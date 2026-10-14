package pe.apuyawar.systems.panel.client;

import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.gui.widget.TextFieldWidget;
import net.minecraft.client.sound.PositionedSoundInstance;
import net.minecraft.item.ItemStack;
import net.minecraft.recipe.RecipeEntry;
import net.minecraft.recipe.RecipeManager;
import net.minecraft.sound.SoundEvents;
import net.minecraft.text.Text;
import pe.apuyawar.systems.network.MissionsRequestPayload;

import java.util.ArrayList;
import java.util.List;

/**
 * Panel K de supervivencia: fondo {@code panel.png} con los cuatro espacios
 * (Mobs, Objetos, Misiones, Búsqueda) ya recortados en la imagen; los íconos se
 * superponen en esas posiciones. "Misiones" abre una pantalla aparte
 * ({@link MissionsScreen}); las otras tres cambian el contenido de la zona derecha.
 */
public final class SurvivalPanelScreen extends Screen {
    private enum Tab { MOBS, ITEMS, SEARCH }
    private enum SearchMode { RECIPES, HOSTILE_MOBS }
    private Tab tab = Tab.MOBS;
    private SearchMode searchMode = SearchMode.RECIPES;
    // El CellRenderer de mobs (y su caché de entidades de vista previa) vive en
    // esta instancia de pantalla: se descarta junto con ella (al cerrar el panel
    // o al presionar K de nuevo) sin necesitar limpieza explícita — ver
    // MobCellRenderer para el razonamiento completo del ciclo de vida/caché.
    private final CatalogGrid<MobCatalogEntry> mobGrid = new CatalogGrid<>(
            MobCatalog.ENTRIES, MobCatalog.FUTURE_SLOTS, new MobCellRenderer(), e -> e.displayName().getString());
    private final CatalogGrid<CatalogEntry> itemGrid = new CatalogGrid<>(
            ItemCatalog.ENTRIES, ItemCatalog.FUTURE_SLOTS, new ItemCellRenderer(), CatalogEntry::name);
    private TextFieldWidget searchField;
    private List<String> searchResults = List.of();

    public SurvivalPanelScreen() { super(Text.literal("APU YAWAR")); }

    @Override protected void init() {
        int[] box = panelBox();
        var content = PanelLayout.CONTENT_AREA;
        int fx = box[0] + (int) (content.x0() * box[2]);
        int fy = box[1] + (int) (content.y0() * box[3]);
        int fw = (int) (content.width() * box[2]);
        searchField = new TextFieldWidget(this.textRenderer, fx, fy, Math.max(60, fw - 10), 16, Text.literal("Buscar"));
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
     * campo de búsqueda). */
    @Override public void renderBackground(DrawContext context, int mouseX, int mouseY, float delta) {
    }

    @Override public void render(DrawContext context, int mouseX, int mouseY, float delta) {
        // Fondo oscuro plano en vez del mundo difuminado: más legible y evita
        // depender del blur de Minecraft para la nitidez del panel.
        context.fill(0, 0, this.width, this.height, 0xCC000000);
        // Ver ViewportFix: aunque ya no usamos el renderBackground() real (con
        // blur) en ningún punto, se conserva como red de seguridad barata.
        ViewportFix.restoreAfterBackground(this.client);
        int[] box = panelBox();
        // Sobrecarga de 10 argumentos: destino (box[2],box[3]) y región muestreada
        // (PANEL_W,PANEL_H) son parámetros INDEPENDIENTES. La de 8 argumentos usada
        // antes reutiliza el tamaño de destino también como tamaño de región, así
        // que al pedir un destino más chico que la textura recortaba una esquina
        // ampliada en vez de escalar la imagen completa — esa era la causa real del
        // "zoom"/recorte.
        context.drawTexture(PanelTextures.PANEL, box[0], box[1], box[2], box[3], 0f, 0f,
                PanelTextures.PANEL_W, PanelTextures.PANEL_H, PanelTextures.PANEL_W, PanelTextures.PANEL_H);
        drawSlot(context, box, PanelLayout.SLOT_MOBS, PanelTextures.ICON_MOBS, mouseX, mouseY, tab == Tab.MOBS);
        drawSlot(context, box, PanelLayout.SLOT_ITEMS, PanelTextures.ICON_ITEMS, mouseX, mouseY, tab == Tab.ITEMS);
        drawSlot(context, box, PanelLayout.SLOT_MISSIONS, PanelTextures.ICON_MISSIONS, mouseX, mouseY, false);
        drawSlot(context, box, PanelLayout.SLOT_SEARCH, PanelTextures.ICON_SEARCH, mouseX, mouseY, tab == Tab.SEARCH);

        var content = PanelLayout.CONTENT_AREA;
        int fx = box[0] + (int) (content.x0() * box[2]);
        int fy = box[1] + (int) (content.y0() * box[3]);
        int fw = (int) (content.width() * box[2]);
        int fh = (int) (content.height() * box[3]);
        context.enableScissor(fx, fy, fx + fw, fy + fh);
        // Capa oscura semitransparente para que el texto/catálogo se lea sobre la
        // escena de constelaciones (que se mantiene como decoración, no se oculta).
        context.fill(fx - 6, fy - 6, fx + fw + 6, fy + fh + 6, 0x8A000000);
        switch (tab) {
            case MOBS -> {
                context.drawText(this.textRenderer, "Cambios y Mobs", fx, fy, 0xFFFFD700, true);
                mobGrid.render(context, this.textRenderer, fx, fy + 14, fw, fh - 14, mouseX, mouseY);
            }
            case ITEMS -> {
                context.drawText(this.textRenderer, "Objetos", fx, fy, 0xFFFFD700, true);
                itemGrid.render(context, this.textRenderer, fx, fy + 14, fw, fh - 14, mouseX, mouseY);
            }
            case SEARCH -> renderSearch(context, fx, fy, fw, fh);
        }
        context.disableScissor();
        searchField.visible = tab == Tab.SEARCH;
        super.render(context, mouseX, mouseY, delta);
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
        var content = PanelLayout.CONTENT_AREA;
        int fx = box[0] + (int) (content.x0() * box[2]);
        int fy = box[1] + (int) (content.y0() * box[3]) + 14;
        int fw = (int) (content.width() * box[2]);
        int fh = (int) (content.height() * box[3]) - 14;
        if (tab == Tab.MOBS && mobGrid.mouseClicked(fx, fy, fw, fh, mouseX, mouseY)) { click(); return true; }
        if (tab == Tab.ITEMS && itemGrid.mouseClicked(fx, fy, fw, fh, mouseX, mouseY)) { click(); return true; }
        return super.mouseClicked(mouseX, mouseY, button);
    }

    @Override public boolean mouseScrolled(double mouseX, double mouseY, double horizontalAmount, double verticalAmount) {
        if (tab == Tab.MOBS) mobGrid.mouseScrolled(verticalAmount);
        if (tab == Tab.ITEMS) itemGrid.mouseScrolled(verticalAmount);
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

    private void onSearchChanged(String query) {
        searchResults = new ArrayList<>();
        if (query == null || query.isBlank() || this.client == null || this.client.world == null) return;
        String needle = query.toLowerCase();
        if (searchMode == SearchMode.RECIPES) {
            RecipeManager recipes = this.client.world.getRecipeManager();
            for (RecipeEntry<?> entry : recipes.values()) {
                ItemStack result = entry.value().getResult(this.client.world.getRegistryManager());
                if (result.isEmpty()) continue;
                String name = result.getName().getString();
                if (name.toLowerCase().contains(needle)) {
                    StringBuilder line = new StringBuilder(name).append(" <- ");
                    var it = entry.value().getIngredients().iterator();
                    boolean any = false;
                    while (it.hasNext()) {
                        var ingredient = it.next();
                        var matches = ingredient.getMatchingStacks();
                        if (matches.length > 0) { line.append(matches[0].getName().getString()); any = true; }
                        if (it.hasNext()) line.append(", ");
                    }
                    if (!any) continue;
                    searchResults.add(line.toString());
                    if (searchResults.size() >= 40) break;
                }
            }
        } else {
            for (MobCatalogEntry entry : MobCatalog.ENTRIES) {
                String name = entry.displayName().getString();
                if (name.toLowerCase().contains(needle)) searchResults.add(name);
            }
        }
    }

    private void renderSearch(DrawContext ctx, int fx, int fy, int fw, int fh) {
        ctx.drawText(this.textRenderer, "Búsqueda", fx, fy, 0xFFFFD700, true);
        int modeY = fy + 22;
        boolean recipesActive = searchMode == SearchMode.RECIPES;
        ctx.drawText(this.textRenderer, (recipesActive ? "§e" : "§7") + "[Recetas]", fx, modeY, 0xFFFFFFFF, false);
        ctx.drawText(this.textRenderer, (!recipesActive ? "§e" : "§7") + "[Mobs hostiles]", fx + 70, modeY, 0xFFFFFFFF, false);
        int listY = fy + 40;
        if (searchResults.isEmpty()) {
            ctx.drawText(this.textRenderer, "Escribe para buscar...", fx, listY, 0xFF888888, false);
        }
        for (int i = 0; i < searchResults.size() && (listY + i * 10) < fy + fh; i++) {
            ctx.drawText(this.textRenderer, searchResults.get(i), fx, listY + i * 10, 0xFFCCCCCC, false);
        }
    }

    @Override public boolean shouldPause() { return false; }
}
