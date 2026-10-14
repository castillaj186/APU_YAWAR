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
    private final CatalogGrid mobGrid = new CatalogGrid(MobCatalog.ENTRIES, MobCatalog.FUTURE_SLOTS);
    private final CatalogGrid itemGrid = new CatalogGrid(ItemCatalog.ENTRIES, ItemCatalog.FUTURE_SLOTS);
    private TextFieldWidget searchField;
    private List<String> searchResults = List.of();
    /** F8: herramienta de desarrollo oculta, apagada por defecto y sin ningún
     * indicio visible en la vista normal. Ver {@link #renderDiagnostic}. */
    private static boolean diagnosticMode = false;

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

    @Override public void render(DrawContext context, int mouseX, int mouseY, float delta) {
        this.renderBackground(context, mouseX, mouseY, delta);
        // Ver ViewportFix: el blur de fondo deja el viewport de GL en el
        // tamaño de su propio framebuffer interno, no en el de la ventana.
        // Sin este reinicio, todo lo dibujado a continuación (panel, íconos,
        // catálogos) se renderiza con un viewport incorrecto y se ve borroso.
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
        // F8 sigue existiendo como herramienta de desarrollo oculta (sin ningún
        // indicio visible en la vista normal, por pedido explícito del usuario).
        if (diagnosticMode) renderDiagnostic(context, box);
    }

    /** Pantalla de comparación de nitidez (F8, oculta, apagada por defecto).
     * Cada prueba se dibuja en su propia celda fija sin superposición:
     * <ul>
     * <li>A: cuadrícula dibujada con {@code fill} (nunca puede verse borrosa,
     *     no es una textura — control de referencia).</li>
     * <li>C: texto nativo, al lado de A.</li>
     * <li>B1: recorte de {@code panel.png} al factor de escala EN VIVO que usa
     *     el panel real en esta pantalla (box[2]/PANEL_W) — lo que el usuario
     *     realmente ve.</li>
     * <li>B2: el mismo recorte a 1:1 (destino = región, sin ningún escalado) —
     *     si B1 se ve mal pero B2 bien, el problema es el escalado en vivo, no
     *     el contenido del PNG.</li>
     * <li>D1/D2: el MISMO bitmap ({@code icon_mobs.png}) al mismo tamaño de
     *     destino, D1 con el filtro de producción (bilineal) y D2 con nearest
     *     ({@link PanelTextures#DEBUG_ICON_NEAREST}) — aísla el efecto del
     *     filtro de la escala y del contenido.</li>
     * <li>E1/E2: un ítem 100% vanilla (huevo de zombie) dibujado nativo a 16px
     *     sin ninguna escala nuestra, contra el mismo ítem con la escala
     *     entera x2 que usa {@link CatalogGrid} — si E1 se ve nítido y E2
     *     también, el entero resolvió el problema reportado con los huevos de
     *     aparición; si E2 sigue blando, el problema no era el factor
     *     fraccionario.</li>
     * </ul> */
    private void renderDiagnostic(DrawContext context, int[] box) {
        int dx = 12, dy = 12, colW = 170, rowH = 56;
        context.fill(dx - 4, dy - 4, dx + colW * 2 + 4, dy + rowH * 4 + 14, 0xE8000000);
        context.drawText(this.textRenderer, "F9 ViewportFix: " + (ViewportFix.enabled ? "ON (corregido)" : "OFF (defecto original)"),
                dx, dy + rowH * 4 + 2, ViewportFix.enabled ? 0xFF55FF55 : 0xFFFF5555, false);

        // Fila 1: A (cuadrícula fill) | C (texto nativo)
        context.drawText(this.textRenderer, "A: fill (control)", dx, dy, 0xFFFFFF00, false);
        int gx = dx, gy = dy + 10, cell = 2;
        for (int yy = 0; yy < 16; yy++)
            for (int xx = 0; xx < 16; xx++)
                context.fill(gx + xx * cell, gy + yy * cell, gx + xx * cell + cell, gy + yy * cell + cell,
                        (xx + yy) % 2 == 0 ? 0xFFFFFFFF : 0xFF000000);
        context.drawText(this.textRenderer, "C: texto nativo", dx + colW, dy, 0xFFFFFF00, false);
        context.drawText(this.textRenderer, "Aa0 1234", dx + colW, dy + 12, 0xFFFFFFFF, false);

        // Fila 2: B1 (panel.png a escala EN VIVO) | B2 (panel.png 1:1)
        int row2 = dy + rowH;
        context.drawText(this.textRenderer, "B1: panel escala en vivo", dx, row2, 0xFFFFFF00, false);
        float liveScale = box[2] / (float) PanelTextures.PANEL_W;
        int liveSize = Math.max(8, Math.round(64 * liveScale));
        context.drawTexture(PanelTextures.PANEL, dx, row2 + 10, liveSize, liveSize, 300f, 0f,
                64, 64, PanelTextures.PANEL_W, PanelTextures.PANEL_H);
        context.drawText(this.textRenderer, "B2: panel.png 1:1", dx + colW, row2, 0xFFFFFF00, false);
        context.drawTexture(PanelTextures.PANEL, dx + colW, row2 + 10, 32, 32, 300f, 0f,
                32, 32, PanelTextures.PANEL_W, PanelTextures.PANEL_H);

        // Fila 3: D1 (icon_mobs bilineal, producción) | D2 (mismo bitmap, nearest)
        int row3 = dy + rowH * 2;
        int iconSize = 44;
        context.drawText(this.textRenderer, "D1: icon bilineal (actual)", dx, row3, 0xFFFFFF00, false);
        context.drawTexture(PanelTextures.ICON_MOBS, dx, row3 + 10, iconSize, iconSize, 0f, 0f,
                PanelTextures.ICON_SIZE, PanelTextures.ICON_SIZE, PanelTextures.ICON_SIZE, PanelTextures.ICON_SIZE);
        context.drawText(this.textRenderer, "D2: mismo PNG, nearest", dx + colW, row3, 0xFFFFFF00, false);
        context.drawTexture(PanelTextures.DEBUG_ICON_NEAREST, dx + colW, row3 + 10, iconSize, iconSize, 0f, 0f,
                PanelTextures.ICON_SIZE, PanelTextures.ICON_SIZE, PanelTextures.ICON_SIZE, PanelTextures.ICON_SIZE);

        // Fila 4: E1 (ítem vanilla nativo 16px) | E2 (mismo ítem, escala x2 entera de CatalogGrid)
        int row4 = dy + rowH * 3;
        ItemStack vanillaProbe = net.minecraft.item.Items.ZOMBIE_SPAWN_EGG.getDefaultStack();
        context.drawText(this.textRenderer, "E1: huevo vanilla nativo", dx, row4, 0xFFFFFF00, false);
        context.drawItem(vanillaProbe, dx, row4 + 10);
        context.drawText(this.textRenderer, "E2: mismo huevo, x2 entero", dx + colW, row4, 0xFFFFFF00, false);
        context.getMatrices().push();
        context.getMatrices().translate(dx + colW, row4 + 10, 0);
        context.getMatrices().scale(2f, 2f, 1f);
        context.drawItem(vanillaProbe, 0, 0);
        context.getMatrices().pop();
    }

    @Override public boolean keyPressed(int keyCode, int scanCode, int modifiers) {
        if (keyCode == org.lwjgl.glfw.GLFW.GLFW_KEY_F8) { diagnosticMode = !diagnosticMode; return true; }
        // F9: apaga/enciende ViewportFix para comparar en vivo, en la misma
        // sesión, el panel CON y SIN la corrección del viewport. Herramienta
        // de verificación oculta, sin indicio en la vista normal.
        if (keyCode == org.lwjgl.glfw.GLFW.GLFW_KEY_F9) { ViewportFix.enabled = !ViewportFix.enabled; return true; }
        return super.keyPressed(keyCode, scanCode, modifiers);
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
            for (CatalogEntry entry : MobCatalog.ENTRIES) {
                if (entry.name().toLowerCase().contains(needle)) searchResults.add(entry.name());
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
