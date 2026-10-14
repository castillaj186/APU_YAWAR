package pe.apuyawar.systems.panel.client;

import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.gui.widget.TextFieldWidget;
import net.minecraft.client.sound.PositionedSoundInstance;
import net.minecraft.item.Item;
import net.minecraft.recipe.CraftingRecipe;
import net.minecraft.sound.SoundEvents;
import net.minecraft.text.Text;
import pe.apuyawar.systems.network.MissionsRequestPayload;

import java.text.Normalizer;
import java.util.ArrayList;
import java.util.List;
import java.util.function.IntConsumer;

/**
 * Panel K de supervivencia: fondo {@code panel.png} con los cuatro espacios
 * (Mobs, Objetos, Misiones, Búsqueda) ya recortados en la imagen; los íconos se
 * superponen en esas posiciones. "Misiones" abre una pantalla aparte
 * ({@link MissionsScreen}); las otras tres cambian el contenido de la zona derecha.
 *
 * <p>Mobs muestra solo enemigos hostiles ({@link MobCatalog#ENTRIES}); Objetos
 * muestra solo objetos fabricables ({@link ItemCatalog}, construido una vez
 * por apertura de panel desde el {@code RecipeManager} real del mundo actual);
 * Búsqueda es una sola barra que encuentra ambos tipos a la vez
 * ({@link SearchResult}). Seleccionar un objeto fabricable (desde Objetos o
 * desde Búsqueda) abre un visualizador de receta compartido
 * ({@link CraftingRecipeView}); seleccionar un mob desde Búsqueda abre una
 * ficha básica (reutilizando el mismo renderizador 3D de la pestaña Mobs).
 */
public final class SurvivalPanelScreen extends Screen {
    private enum Tab { MOBS, ITEMS, SEARCH }
    private Tab tab = Tab.MOBS;

    // Renderizadores de celda compartidos entre las 3 cuadrículas (Mobs, Objetos
    // y Búsqueda) para que la búsqueda reutilice exactamente el mismo dibujado
    // -- y, en el caso de los mobs, la misma caché de entidades de vista previa
    // -- en vez de crear una segunda implementación.
    private final ItemCellRenderer itemCellRenderer = new ItemCellRenderer();
    private final MobCellRenderer mobCellRenderer = new MobCellRenderer();
    // Construido una vez por apertura del Panel K desde el RecipeManager real
    // del mundo actual -- ver la documentación de la clase.
    private final ItemCatalog itemCatalog = new ItemCatalog();

    private final CatalogGrid<MobCatalogEntry> mobGrid = new CatalogGrid<>(
            MobCatalog.ENTRIES, MobCatalog.FUTURE_SLOTS, mobCellRenderer, e -> e.displayName().getString());
    private final CatalogGrid<CatalogEntry> itemGrid = new CatalogGrid<>(
            itemCatalog.entries(), ItemCatalog.FUTURE_SLOTS, itemCellRenderer, CatalogEntry::name);
    // Sin espacios "?": una búsqueda sin resultados simplemente no muestra nada
    // (no hay un conjunto fijo de "futuros resultados de búsqueda" que bloquear).
    private final CatalogGrid<SearchResult> searchGrid = new CatalogGrid<>(
            List.of(), 0, new SearchResultCellRenderer(itemCellRenderer, mobCellRenderer),
            r -> switch (r) {
                case SearchResult.Item i -> i.entry().name();
                case SearchResult.Mob m -> m.entry().displayName().getString();
            });
    private TextFieldWidget searchField;

    // Vista de detalle de receta abierta desde Objetos (independiente de la de
    // Búsqueda: volver desde una no afecta a la otra, y cada una recuerda su
    // propio objeto/índice de receta alternativa mientras el panel esté abierto).
    private Item itemsDetailItem; private String itemsDetailName; private int itemsDetailIndex;
    // Detalle abierto desde Búsqueda: o un objeto (receta) o un mob (ficha), nunca ambos.
    private Item searchDetailItem; private String searchDetailName; private int searchDetailIndex;
    private MobCatalogEntry searchDetailMob;

    private static final int TITLE_PAD_TOP = 6, TITLE_PAD_X = 2, TITLE_HEIGHT = 10, GAP_AFTER_TITLE = 6;
    private static final int SEARCH_FIELD_HEIGHT = 16, GAP_AFTER_SEARCH = 6;
    private static final int BACK_W = 54, BACK_H = 10;

    /** Posiciones ya resueltas en píxeles de pantalla del área de contenido
     * (zona derecha del panel, la escena andina) para el frame actual: título,
     * barra de búsqueda (solo aplica/ocupa espacio en la pestaña Búsqueda) y el
     * cuerpo (cuadrícula o vista de detalle). Corrige el problema reportado de
     * títulos pegados al borde superior / solapados con el buscador: antes el
     * título se dibujaba exactamente en {@code fy} y el cuerpo en {@code fy+14}
     * sin ningún margen; ahora hay un margen superior, una separación clara
     * tras el título, y (solo en Búsqueda) una separación adicional tras la
     * barra de texto antes de empezar la cuadrícula de resultados. */
    private record ContentLayout(int fx, int fy, int fw, int fh, int titleY, int searchFieldY, int bodyY, int bodyH) {}

    private ContentLayout layout(int[] box) {
        var content = PanelLayout.CONTENT_AREA;
        int fx = box[0] + (int) (content.x0() * box[2]);
        int fy = box[1] + (int) (content.y0() * box[3]);
        int fw = (int) (content.width() * box[2]);
        int fh = (int) (content.height() * box[3]);
        int titleY = fy + TITLE_PAD_TOP;
        int searchFieldY = titleY + TITLE_HEIGHT + GAP_AFTER_TITLE;
        int bodyY = (tab == Tab.SEARCH) ? searchFieldY + SEARCH_FIELD_HEIGHT + GAP_AFTER_SEARCH : searchFieldY;
        int bodyH = Math.max(0, fh - (bodyY - fy));
        return new ContentLayout(fx, fy, fw, fh, titleY, searchFieldY, bodyY, bodyH);
    }

    public SurvivalPanelScreen() { super(Text.literal("APU YAWAR")); }

    @Override protected void init() {
        ContentLayout l = layout(panelBox());
        searchField = new TextFieldWidget(this.textRenderer, l.fx() + TITLE_PAD_X, l.searchFieldY(),
                Math.max(60, l.fw() - TITLE_PAD_X * 2), SEARCH_FIELD_HEIGHT, Text.literal("Buscar"));
        searchField.setChangedListener(this::onSearchChanged);
        addDrawableChild(searchField);
    }

    /** Devuelve {x, y, width, height} del panel ya escalado y centrado en pantalla.
     * El ratio ideal (92% de pantalla) se ajusta con {@link PanelScale#snap}
     * a la fracción limpia 1/n más cercana en vez de usarse tal cual: un
     * factor arbitrario como 0.4387 produce un muestreo irregular de texels
     * incluso con filtrado bilineal; 1/n es predecible. */
    private int[] panelBox() {
        float ideal = Math.min(this.width * 0.92f / PanelTextures.PANEL_W, this.height * 0.92f / PanelTextures.PANEL_H);
        float scale = PanelScale.snap(ideal);
        int w = Math.round(PanelTextures.PANEL_W * scale), h = Math.round(PanelTextures.PANEL_H * scale);
        int x = (this.width - w) / 2, y = (this.height - h) / 2;
        return new int[]{x, y, w, h};
    }

    /** {@code Screen.render()} (invocado vía {@code super.render()} más abajo)
     * vuelve a llamar a {@code renderBackground()} internamente antes de pintar
     * los widgets — eso aplicaba una segunda pasada de blur encima del panel ya
     * dibujado, la causa real del desenfoque reportado. Como el fondo real de
     * este panel es el {@code fill()} oscuro dibujado directamente en
     * {@link #render}, el {@code renderBackground()} con blur de Minecraft no
     * se necesita nunca aquí; se sobrescribe como no-op para bloquear esa
     * llamada oculta sin afectar el resto de {@code super.render()} (botones,
     * campo de búsqueda). NO TOCAR: nitidez ya confirmada visualmente. */
    @Override public void renderBackground(DrawContext context, int mouseX, int mouseY, float delta) {
    }

    @Override public void render(DrawContext context, int mouseX, int mouseY, float delta) {
        // Fondo oscuro plano en vez del mundo difuminado: más legible y evita
        // depender del blur de Minecraft para la nitidez del panel. NO TOCAR.
        context.fill(0, 0, this.width, this.height, 0xCC000000);
        // Ver ViewportFix: aunque ya no usamos el renderBackground() real (con
        // blur) en ningún punto, se conserva como red de seguridad barata. NO TOCAR.
        ViewportFix.restoreAfterBackground(this.client);
        int[] box = panelBox();
        // Sobrecarga de 10 argumentos: destino (box[2],box[3]) y región muestreada
        // (PANEL_W,PANEL_H) son parámetros INDEPENDIENTES — NO TOCAR (ver docs 12-15). */
        context.drawTexture(PanelTextures.PANEL, box[0], box[1], box[2], box[3], 0f, 0f,
                PanelTextures.PANEL_W, PanelTextures.PANEL_H, PanelTextures.PANEL_W, PanelTextures.PANEL_H);
        drawSlot(context, box, PanelLayout.SLOT_MOBS, PanelTextures.ICON_MOBS, mouseX, mouseY, tab == Tab.MOBS);
        drawSlot(context, box, PanelLayout.SLOT_ITEMS, PanelTextures.ICON_ITEMS, mouseX, mouseY, tab == Tab.ITEMS);
        drawSlot(context, box, PanelLayout.SLOT_MISSIONS, PanelTextures.ICON_MISSIONS, mouseX, mouseY, false);
        drawSlot(context, box, PanelLayout.SLOT_SEARCH, PanelTextures.ICON_SEARCH, mouseX, mouseY, tab == Tab.SEARCH);

        ContentLayout l = layout(box);
        context.enableScissor(l.fx(), l.fy(), l.fx() + l.fw(), l.fy() + l.fh());
        // Capa oscura semitransparente para que el texto/catálogo se lea sobre la
        // escena de constelaciones (que se mantiene como decoración, no se oculta).
        context.fill(l.fx() - 6, l.fy() - 6, l.fx() + l.fw() + 6, l.fy() + l.fh() + 6, 0x8A000000);
        switch (tab) {
            case MOBS -> renderMobsTab(context, l, mouseX, mouseY);
            case ITEMS -> renderItemsTab(context, l, mouseX, mouseY);
            case SEARCH -> renderSearchTab(context, l, mouseX, mouseY);
        }
        context.disableScissor();
        searchField.visible = tab == Tab.SEARCH;
        super.render(context, mouseX, mouseY, delta);
    }

    private void renderMobsTab(DrawContext ctx, ContentLayout l, int mouseX, int mouseY) {
        ctx.drawText(this.textRenderer, "Cambios y Mobs", l.fx() + TITLE_PAD_X, l.titleY(), 0xFFFFD700, true);
        mobGrid.render(ctx, this.textRenderer, l.fx(), l.bodyY(), l.fw(), l.bodyH(), mouseX, mouseY);
    }

    private void renderItemsTab(DrawContext ctx, ContentLayout l, int mouseX, int mouseY) {
        ctx.drawText(this.textRenderer, "Objetos", l.fx() + TITLE_PAD_X, l.titleY(), 0xFFFFD700, true);
        if (itemsDetailItem == null) {
            itemGrid.render(ctx, this.textRenderer, l.fx(), l.bodyY(), l.fw(), l.bodyH(), mouseX, mouseY);
            return;
        }
        List<CraftingRecipe> recipes = itemCatalog.recipesFor(itemsDetailItem);
        if (recipes.isEmpty() || this.client == null || this.client.world == null) { itemsDetailItem = null; return; }
        if (itemsDetailIndex >= recipes.size()) itemsDetailIndex = 0;
        CraftingRecipeView.render(ctx, this.textRenderer, this.client.world.getRegistryManager(),
                recipes.get(itemsDetailIndex), itemsDetailIndex, recipes.size(), itemsDetailName,
                l.fx(), l.bodyY(), l.fw(), mouseX, mouseY);
    }

    private void renderSearchTab(DrawContext ctx, ContentLayout l, int mouseX, int mouseY) {
        ctx.drawText(this.textRenderer, "Búsqueda", l.fx() + TITLE_PAD_X, l.titleY(), 0xFFFFD700, true);
        if (searchDetailMob != null) {
            renderMobDetail(ctx, l, searchDetailMob, mouseX, mouseY);
        } else if (searchDetailItem != null) {
            List<CraftingRecipe> recipes = itemCatalog.recipesFor(searchDetailItem);
            if (recipes.isEmpty() || this.client == null || this.client.world == null) { searchDetailItem = null; return; }
            if (searchDetailIndex >= recipes.size()) searchDetailIndex = 0;
            CraftingRecipeView.render(ctx, this.textRenderer, this.client.world.getRegistryManager(),
                    recipes.get(searchDetailIndex), searchDetailIndex, recipes.size(), searchDetailName,
                    l.fx(), l.bodyY(), l.fw(), mouseX, mouseY);
        } else {
            searchGrid.render(ctx, this.textRenderer, l.fx(), l.bodyY(), l.fw(), l.bodyH(), mouseX, mouseY);
        }
    }

    /** Ficha básica de un mob (desde Búsqueda): solo nombre y categoría reales
     * -- no se inventan drops, estadísticas ni habilidades que no existan
     * todavía -- más el mismo modelo 3D de la pestaña Mobs, agrandado. */
    private void renderMobDetail(DrawContext ctx, ContentLayout l, MobCatalogEntry mob, int mouseX, int mouseY) {
        boolean backHover = isBackHovered(l, mouseX, mouseY);
        ctx.drawText(this.textRenderer, "« Volver", l.fx(), l.bodyY(), backHover ? 0xFFFFFFFF : 0xFFAAAAAA, false);
        ctx.drawText(this.textRenderer, mob.displayName().getString(), l.fx(), l.bodyY() + 12, 0xFFFFD700, true);
        ctx.drawText(this.textRenderer, "Categoría: Enemigo hostil", l.fx(), l.bodyY() + 24, 0xFFAAAAAA, false);
        int top = l.bodyY() + 38;
        int size = Math.max(24, Math.min(l.fw(), l.bodyY() + l.bodyH() - top));
        int mx = l.fx() + (l.fw() - size) / 2;
        mobCellRenderer.render(ctx, this.textRenderer, mob, mx, top, size, size, false, false);
    }

    private boolean isBackHovered(ContentLayout l, double mouseX, double mouseY) {
        return mouseX >= l.fx() && mouseX < l.fx() + BACK_W && mouseY >= l.bodyY() && mouseY < l.bodyY() + BACK_H;
    }

    /** Maneja el clic sobre "« Volver" / "<" / ">" de un visualizador de receta
     * abierto (Objetos o Búsqueda, misma lógica reutilizada para ambos). */
    private boolean handleRecipeDetailClick(ContentLayout l, double mouseX, double mouseY,
                                             int currentIndex, int recipeCount, IntConsumer setIndex, Runnable clear) {
        int x = l.fx(), y = l.bodyY(), w = l.fw();
        if (CraftingRecipeView.backButton(x, y).contains(mouseX, mouseY)) { click(); clear.run(); return true; }
        if (recipeCount > 1) {
            if (CraftingRecipeView.prevButton(x, y, w, recipeCount).contains(mouseX, mouseY)) {
                click(); setIndex.accept((currentIndex - 1 + recipeCount) % recipeCount); return true;
            }
            if (CraftingRecipeView.nextButton(x, y, w, recipeCount).contains(mouseX, mouseY)) {
                click(); setIndex.accept((currentIndex + 1) % recipeCount); return true;
            }
        }
        return false;
    }

    private void drawSlot(DrawContext ctx, int[] box, PanelLayout.Slot slot, net.minecraft.util.Identifier icon,
                           int mouseX, int mouseY, boolean active) {
        int x = box[0] + (int) (slot.x0() * box[2]);
        int y = box[1] + (int) (slot.y0() * box[3]);
        int w = (int) (slot.width() * box[2]);
        int h = (int) (slot.height() * box[3]);
        // Margen para que el ícono no toque el borde dorado del espacio oscuro.
        int size = (int) (Math.min(w, h) * 0.82);
        int ix = x + (w - size) / 2, iy = y + (h - size) / 2;
        ctx.drawTexture(icon, ix, iy, size, size, 0f, 0f,
                PanelTextures.ICON_SIZE, PanelTextures.ICON_SIZE, PanelTextures.ICON_SIZE, PanelTextures.ICON_SIZE);
        boolean hover = mouseX >= x && mouseX < x + w && mouseY >= y && mouseY < y + h;
        if (active) ctx.fill(x, y, x + w, y + h, 0x3055FFAA);
        else if (hover) ctx.fill(x, y, x + w, y + h, 0x30FFFFFF);
    }

    @Override public boolean mouseClicked(double mouseX, double mouseY, int button) {
        int[] box = panelBox();
        if (inSlot(box, PanelLayout.SLOT_MOBS, mouseX, mouseY)) { select(Tab.MOBS); return true; }
        if (inSlot(box, PanelLayout.SLOT_ITEMS, mouseX, mouseY)) { select(Tab.ITEMS); return true; }
        if (inSlot(box, PanelLayout.SLOT_SEARCH, mouseX, mouseY)) { select(Tab.SEARCH); return true; }
        if (inSlot(box, PanelLayout.SLOT_MISSIONS, mouseX, mouseY)) { openMissions(); return true; }
        ContentLayout l = layout(box);
        switch (tab) {
            case MOBS -> {
                if (mobGrid.mouseClicked(l.fx(), l.bodyY(), l.fw(), l.bodyH(), mouseX, mouseY)) { click(); return true; }
            }
            case ITEMS -> {
                if (itemsDetailItem != null) {
                    if (handleRecipeDetailClick(l, mouseX, mouseY, itemsDetailIndex,
                            itemCatalog.recipesFor(itemsDetailItem).size(),
                            i -> itemsDetailIndex = i, () -> itemsDetailItem = null)) return true;
                } else if (itemGrid.mouseClicked(l.fx(), l.bodyY(), l.fw(), l.bodyH(), mouseX, mouseY)) {
                    click();
                    CatalogEntry picked = itemGrid.selectedEntry();
                    if (picked != null) { itemsDetailItem = picked.icon().getItem(); itemsDetailName = picked.name(); itemsDetailIndex = 0; }
                    return true;
                }
            }
            case SEARCH -> {
                if (searchDetailMob != null) {
                    if (isBackHovered(l, mouseX, mouseY)) { click(); searchDetailMob = null; return true; }
                } else if (searchDetailItem != null) {
                    if (handleRecipeDetailClick(l, mouseX, mouseY, searchDetailIndex,
                            itemCatalog.recipesFor(searchDetailItem).size(),
                            i -> searchDetailIndex = i, () -> searchDetailItem = null)) return true;
                } else if (searchGrid.mouseClicked(l.fx(), l.bodyY(), l.fw(), l.bodyH(), mouseX, mouseY)) {
                    click();
                    SearchResult picked = searchGrid.selectedEntry();
                    if (picked instanceof SearchResult.Mob m) {
                        searchDetailMob = m.entry();
                    } else if (picked instanceof SearchResult.Item i) {
                        searchDetailItem = i.entry().icon().getItem();
                        searchDetailName = i.entry().name();
                        searchDetailIndex = 0;
                    }
                    return true;
                }
            }
        }
        return super.mouseClicked(mouseX, mouseY, button);
    }

    @Override public boolean mouseScrolled(double mouseX, double mouseY, double horizontalAmount, double verticalAmount) {
        if (tab == Tab.MOBS) mobGrid.mouseScrolled(verticalAmount);
        else if (tab == Tab.ITEMS && itemsDetailItem == null) itemGrid.mouseScrolled(verticalAmount);
        else if (tab == Tab.SEARCH && searchDetailItem == null && searchDetailMob == null) searchGrid.mouseScrolled(verticalAmount);
        return super.mouseScrolled(mouseX, mouseY, horizontalAmount, verticalAmount);
    }

    private boolean inSlot(int[] box, PanelLayout.Slot slot, double mouseX, double mouseY) {
        int x = box[0] + (int) (slot.x0() * box[2]), y = box[1] + (int) (slot.y0() * box[3]);
        int w = (int) (slot.width() * box[2]), h = (int) (slot.height() * box[3]);
        return mouseX >= x && mouseX < x + w && mouseY >= y && mouseY < y + h;
    }
    private void select(Tab newTab) { if (tab != newTab) { tab = newTab; click(); } }
    private void click() {
        if (this.client != null && this.client.getSoundManager() != null)
            this.client.getSoundManager().play(PositionedSoundInstance.master(SoundEvents.UI_BUTTON_CLICK.value(), 1.0f));
    }
    private void openMissions() {
        click();
        if (this.client == null) return;
        this.client.setScreen(new MissionsScreen());
        if (ClientPlayNetworking.canSend(MissionsRequestPayload.ID)) ClientPlayNetworking.send(new MissionsRequestPayload());
    }

    /** Búsqueda unificada: mobs hostiles + objetos fabricables a la vez, sin
     * que el jugador elija categoría. Insensible a mayúsculas/minúsculas y
     * tildes (normalización NFD + eliminación de marcas combinantes — una
     * forma estándar y razonable de "ignorar acentos" sin inventar un
     * algoritmo de coincidencia difusa). Nunca incluye espacios "?" de
     * contenido bloqueado: solo entradas reales de {@link MobCatalog#ENTRIES}
     * y {@link ItemCatalog#entries()}. */
    private void onSearchChanged(String query) {
        if (query == null || query.isBlank()) { searchGrid.setEntries(List.of()); return; }
        String needle = normalize(query);
        List<SearchResult> results = new ArrayList<>();
        for (MobCatalogEntry mob : MobCatalog.ENTRIES) {
            if (normalize(mob.displayName().getString()).contains(needle)) results.add(new SearchResult.Mob(mob));
        }
        for (CatalogEntry item : itemCatalog.entries()) {
            if (normalize(item.name()).contains(needle)) results.add(new SearchResult.Item(item));
        }
        searchGrid.setEntries(results);
    }

    private static String normalize(String s) {
        return Normalizer.normalize(s.toLowerCase(), Normalizer.Form.NFD).replaceAll("\\p{M}", "");
    }

    @Override public boolean shouldPause() { return false; }
}
