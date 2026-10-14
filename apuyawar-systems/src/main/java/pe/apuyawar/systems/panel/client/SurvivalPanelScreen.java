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
import org.lwjgl.opengl.GL11;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
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
    private boolean suppressBackgroundDuringWidgetRender = false;

    private static final Logger LOGGER = LoggerFactory.getLogger("apuyawar-panel-k");
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
    /** F10: prueba A/B oculta que reemplaza {@code renderBackground()} (y por
     * tanto el blur de fondo) por un simple {@code fill()} oscuro, para
     * comparar en vivo si el blur en sí es parte de la causa del desenfoque,
     * independientemente de {@link ViewportFix}. Apagado por defecto. */
    private static boolean skipBlurBackground = false;
    /** F12: usa {@link PanelTextures#DEBUG_PANEL_NEAREST} (mismo bitmap que
     * {@link PanelTextures#PANEL}, solo con {@code blur:false}) para el fondo
     * del panel REAL a tamaño completo, en vez del recorte pequeño de
     * diagnóstico — prueba controlada de nearest vs bilineal sin tocar el
     * master de producción. Apagado por defecto. (No se usa F11: coincide
     * con el atajo de pantalla completa de Minecraft, que se procesa en un
     * callback aparte y no se puede interceptar desde Screen.keyPressed.) */
    private static boolean useNearestPanelDebug = false;
    /** F7: ver {@link PanelScale#rawMode} (javadoc completo en
     * {@code PanelScale}) — comparación A/B pedida en la revisión de la
     * PR #1: con esto encendido, {@code panelBox()} usa el factor ideal SIN
     * redondear a 1/n (en vez de {@code PanelScale.snap}), porque en GUI
     * pequeñas (ej. 480x253, escala 4) el redondeo hacia abajo desaprovechaba
     * resolución disponible (ideal≈0.247 quedaba en 0.20, no en 0.25).
     * Afecta también a {@link MissionsScreen} (mismo {@code PanelScale}
     * compartido). Apagado por defecto. El estado real vive en
     * {@link PanelScale#rawMode} (no un campo propio) para que ambas
     * pantallas lean el mismo valor. */

    // Constantes de diseño del overlay F8 (compartidas entre render() y
    // renderDiagnostic()). Revisadas en la PR #1: rowH bajó de 56 a 46 y el
    // tamaño de los íconos D1/D2 de 44 a 34 para liberar espacio vertical, y
    // las muestras G-antes/G-después se movieron a una tercera columna propia
    // (antes estaban fijas en y=300, fuera de una GUI de 480x253 de altura).
    // Con estos valores el overlay completo cabe en 444x244 lógicos: se
    // verificó a mano contra 480x253 (el caso reportado) con margen; en GUI
    // más pequeñas que eso seguiría pudiendo recortarse, pero no hay reporte
    // de ese caso todavía.
    private static final int DX = 12, DY = 12, COL_W = 170, ROW_H = 46;
    private static final int DIAG_ICON_SIZE = 34;
    private static final int G_SIZE = 80;
    private static final int GX = DX + COL_W * 2 + 8;
    private static final int GY_ANTES = DY + 10;
    private static final int GY_DESPUES = GY_ANTES + G_SIZE + 24;

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
     * El ratio ideal (92% de pantalla) se resuelve con {@link PanelScale#resolve}:
     * por defecto, a la fracción limpia 1/n más cercana ({@link PanelScale#snap}),
     * porque un factor arbitrario como 0.4387 produce un muestreo irregular de
     * texels incluso con filtrado bilineal; con {@link PanelScale#rawMode} (F7)
     * encendido, usa {@code ideal} tal cual, para comparar en vivo si eso
     * recupera nitidez en GUI pequeñas donde 1/n desaprovecha resolución. */
    private int[] panelBox() {
        float ideal = Math.min(this.width * 0.92f / PanelTextures.PANEL_W, this.height * 0.92f / PanelTextures.PANEL_H);
        float scale = PanelScale.resolve(ideal);
        int w = Math.round(PanelTextures.PANEL_W * scale), h = Math.round(PanelTextures.PANEL_H * scale);
        int x = (this.width - w) / 2, y = (this.height - h) / 2;
        return new int[]{x, y, w, h};
    }

    /** Screen.render() vuelve a llamar a renderBackground() antes de pintar widgets.
     * Evitamos aplicar blur sobre nuestro panel ya dibujado; el primer fondo
     * se pinta normalmente al comienzo de render(). */
    @Override public void renderBackground(DrawContext context, int mouseX, int mouseY, float delta) {
        if (!suppressBackgroundDuringWidgetRender) {
            super.renderBackground(context, mouseX, mouseY, delta);
        }
    }
    @Override public void render(DrawContext context, int mouseX, int mouseY, float delta) {
        logViewport("A-entrada (antes de renderBackground)");
        if (skipBlurBackground) {
            // F10, prueba A/B: evita renderBackground() (y por tanto el blur
            // de fondo) por completo, para aislar si el blur EN SÍ influye en
            // el desenfoque del panel, independientemente de ViewportFix.
            context.fill(0, 0, this.width, this.height, 0xCC000000);
        } else {
            this.renderBackground(context, mouseX, mouseY, delta);
        }
        // Ver ViewportFix: el blur de fondo deja el viewport de GL en el
        // tamaño de su propio framebuffer interno, no en el de la ventana.
        // Sin este reinicio, todo lo dibujado a continuación (panel, íconos,
        // catálogos) se renderiza con un viewport incorrecto y se ve borroso.
        // Se mantiene también cuando skipBlurBackground está activo: si no
        // hubo blur, esta llamada no tiene nada que corregir (el viewport ya
        // estaba bien) y no debería cambiar nada — eso en sí mismo es parte
        // de la prueba.
        ViewportFix.restoreAfterBackground(this.client);
        logViewport("B-tras renderBackground + ViewportFix");
        int[] box = panelBox();
        // Sobrecarga de 10 argumentos: destino (box[2],box[3]) y región muestreada
        // (PANEL_W,PANEL_H) son parámetros INDEPENDIENTES. La de 8 argumentos usada
        // antes reutiliza el tamaño de destino también como tamaño de región, así
        // que al pedir un destino más chico que la textura recortaba una esquina
        // ampliada en vez de escalar la imagen completa — esa era la causa real del
        // "zoom"/recorte.
        // F12: en producción siempre PanelTextures.PANEL (blur:true); el
        // interruptor solo cambia qué identificador se usa, nunca el .mcmeta
        // real ni el archivo master.
        var panelTexture = useNearestPanelDebug ? PanelTextures.DEBUG_PANEL_NEAREST : PanelTextures.PANEL;
        context.drawTexture(panelTexture, box[0], box[1], box[2], box[3], 0f, 0f,
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
        // G-antes: mismo elemento grande (recorte detallado de panel.png) que
        // G-después en renderDiagnostic, dibujado aquí ANTES de super.render(),
        // en posición separada, mismo tamaño y mismo filtro — comparación
        // directa pedida explícitamente para descartar que super.render() (u
        // otra operación entre medio) afecte lo ya dibujado.
        // Posición/tamaño corregidos tras la revisión de la PR #1: la anterior
        // (12,300) a 110px quedaba fuera de pantalla en una GUI de 480x253
        // (ver GX/GY/G_SIZE más abajo, columna 3 junto al resto del overlay).
        if (diagnosticMode) drawPanelSample(context, GX, GY_ANTES, G_SIZE, PanelTextures.PANEL, "G-antes");
        searchField.visible = tab == Tab.SEARCH;
        suppressBackgroundDuringWidgetRender = true;
        try {
            super.render(context, mouseX, mouseY, delta);
        } finally {
            suppressBackgroundDuringWidgetRender = false;
        }
        logViewport("C-tras super.render() (antes del diagnóstico F8)");
        // F8 sigue existiendo como herramienta de desarrollo oculta (sin ningún
        // indicio visible en la vista normal, por pedido explícito del usuario).
        if (diagnosticMode) renderDiagnostic(context, box);
    }

    /** Registra el viewport REAL de OpenGL (no el valor que creemos haber
     * pedido) junto con las dimensiones de ventana/framebuffer, para
     * verificar con datos — no solo con la lectura de bytecode — si
     * {@link ViewportFix} efectivamente deja el viewport esperado en cada
     * punto del frame. Solo registra mientras F8 está activo, para no llenar
     * el log durante el juego normal. */
    private void logViewport(String label) {
        if (!diagnosticMode || this.client == null) return;
        int[] vp = new int[4];
        GL11.glGetIntegerv(GL11.GL_VIEWPORT, vp);
        var window = this.client.getWindow();
        LOGGER.info("[PanelK] {} -> GL_VIEWPORT=({},{},{},{}) screen={}x{} (logico) fb={}x{} scaleFactor={}",
                label, vp[0], vp[1], vp[2], vp[3], this.width, this.height,
                window.getFramebufferWidth(), window.getFramebufferHeight(), window.getScaleFactor());
    }

    /** Dibuja un recorte de 220x220 texels del borde superior de
     * {@code panel.png} (zona con detalle: filigrana dorada + parte del
     * resplandor central), escalado al mismo destino en todas las llamadas,
     * para comparar el mismo contenido antes/después de distintos puntos del
     * render. Usa la textura de producción salvo que se pase la variante de
     * depuración explícitamente. */
    private void drawPanelSample(DrawContext context, int x, int y, int destSize, net.minecraft.util.Identifier tex, String label) {
        context.fill(x - 2, y - 12, x + destSize + 2, y + destSize + 2, 0xE0000000);
        context.drawText(this.textRenderer, label, x, y - 10, 0xFFFFFF00, false);
        context.drawTexture(tex, x, y, destSize, destSize, 700f, 0f, 220, 220,
                PanelTextures.PANEL_W, PanelTextures.PANEL_H);
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
     * <li>G-antes/G-después: mismo recorte grande de {@code panel.png},
     *     dibujado una vez en {@link #render} ANTES de {@code super.render()}
     *     y otra vez aquí DESPUÉS, en una tercera columna propia (antes fijo
     *     en y=300, lo que lo dejaba fuera de una GUI de 480x253 de alto —
     *     corregido en la revisión de la PR #1 a {@link #GX}/{@link #GY_ANTES}/
     *     {@link #GY_DESPUES}, dentro del área visible).</li>
     * </ul>
     * Debajo de las pruebas, una línea de texto muestra los valores reales de
     * {@code panelBox()} (ideal sin redondear, snap a 1/n, y el que está en
     * uso según {@link PanelScale#rawMode}/F7) — la comparación numérica en
     * vivo pedida en la PR #1, además de la visual. */
    private void renderDiagnostic(DrawContext context, int[] box) {
        int bgRight = Math.max(DX + COL_W * 2 + 4, GX + G_SIZE + 4);
        int statusY0 = DY + ROW_H * 4 + 2;
        int bgBottom = statusY0 + 3 * 12 + 10;
        context.fill(DX - 4, DY - 4, bgRight + 4, bgBottom, 0xE8000000);
        context.drawText(this.textRenderer, "F9 ViewportFix: " + (ViewportFix.enabled ? "ON (corregido)" : "OFF (defecto original)"),
                DX, statusY0, ViewportFix.enabled ? 0xFF55FF55 : 0xFFFF5555, false);
        context.drawText(this.textRenderer, "F10 sin blur de fondo: " + (skipBlurBackground ? "ON (fill plano)" : "OFF (renderBackground normal)"),
                DX, statusY0 + 12, skipBlurBackground ? 0xFF55FF55 : 0xFFAAAAAA, false);
        context.drawText(this.textRenderer, "F12 panel nearest: " + (useNearestPanelDebug ? "ON (debug)" : "OFF (bilineal producción)"),
                DX, statusY0 + 24, useNearestPanelDebug ? 0xFF55FF55 : 0xFFAAAAAA, false);
        float ideal = Math.min(this.width * 0.92f / PanelTextures.PANEL_W, this.height * 0.92f / PanelTextures.PANEL_H);
        float snapped = PanelScale.snap(ideal);
        float used = box[2] / (float) PanelTextures.PANEL_W;
        context.drawText(this.textRenderer, String.format(
                "F7 escala sin redondear: %s (ideal=%.3f snap=%.3f usado=%.3f box=%dx%d gui=%dx%d)",
                PanelScale.rawMode ? "ON" : "OFF", ideal, snapped, used, box[2], box[3], this.width, this.height),
                DX, statusY0 + 36, PanelScale.rawMode ? 0xFF55FF55 : 0xFFAAAAAA, false);

        // G-después: MISMO recorte grande de panel.png que G-antes (dibujado
        // en render() antes de super.render()), misma textura y tamaño, en
        // otra posición — comparación directa antes/después de super.render().
        drawPanelSample(context, GX, GY_DESPUES, G_SIZE, PanelTextures.PANEL, "G-despues");

        // Fila 1: A (cuadrícula fill) | C (texto nativo)
        context.drawText(this.textRenderer, "A: fill (control)", DX, DY, 0xFFFFFF00, false);
        int gx = DX, gy = DY + 10, cell = 2;
        for (int yy = 0; yy < 16; yy++)
            for (int xx = 0; xx < 16; xx++)
                context.fill(gx + xx * cell, gy + yy * cell, gx + xx * cell + cell, gy + yy * cell + cell,
                        (xx + yy) % 2 == 0 ? 0xFFFFFFFF : 0xFF000000);
        context.drawText(this.textRenderer, "C: texto nativo", DX + COL_W, DY, 0xFFFFFF00, false);
        context.drawText(this.textRenderer, "Aa0 1234", DX + COL_W, DY + 12, 0xFFFFFFFF, false);
        // G-antes ya se dibujó completo (imagen + etiqueta) en render(), antes
        // de super.render() — no se repite aquí, solo G-después (más abajo).

        // Fila 2: B1 (panel.png a escala EN VIVO) | B2 (panel.png 1:1)
        int row2 = DY + ROW_H;
        context.drawText(this.textRenderer, "B1: panel escala en vivo", DX, row2, 0xFFFFFF00, false);
        float liveScale = box[2] / (float) PanelTextures.PANEL_W;
        int liveSize = Math.max(8, Math.round(64 * liveScale));
        context.drawTexture(PanelTextures.PANEL, DX, row2 + 10, liveSize, liveSize, 300f, 0f,
                64, 64, PanelTextures.PANEL_W, PanelTextures.PANEL_H);
        context.drawText(this.textRenderer, "B2: panel.png 1:1", DX + COL_W, row2, 0xFFFFFF00, false);
        context.drawTexture(PanelTextures.PANEL, DX + COL_W, row2 + 10, 32, 32, 300f, 0f,
                32, 32, PanelTextures.PANEL_W, PanelTextures.PANEL_H);

        // Fila 3: D1 (icon_mobs bilineal, producción) | D2 (mismo bitmap, nearest)
        int row3 = DY + ROW_H * 2;
        context.drawText(this.textRenderer, "D1: icon bilineal (actual)", DX, row3, 0xFFFFFF00, false);
        context.drawTexture(PanelTextures.ICON_MOBS, DX, row3 + 10, DIAG_ICON_SIZE, DIAG_ICON_SIZE, 0f, 0f,
                PanelTextures.ICON_SIZE, PanelTextures.ICON_SIZE, PanelTextures.ICON_SIZE, PanelTextures.ICON_SIZE);
        context.drawText(this.textRenderer, "D2: mismo PNG, nearest", DX + COL_W, row3, 0xFFFFFF00, false);
        context.drawTexture(PanelTextures.DEBUG_ICON_NEAREST, DX + COL_W, row3 + 10, DIAG_ICON_SIZE, DIAG_ICON_SIZE, 0f, 0f,
                PanelTextures.ICON_SIZE, PanelTextures.ICON_SIZE, PanelTextures.ICON_SIZE, PanelTextures.ICON_SIZE);

        // Fila 4: E1 (ítem vanilla nativo 16px) | E2 (mismo ítem, escala x2 entera de CatalogGrid)
        int row4 = DY + ROW_H * 3;
        ItemStack vanillaProbe = net.minecraft.item.Items.ZOMBIE_SPAWN_EGG.getDefaultStack();
        context.drawText(this.textRenderer, "E1: huevo vanilla nativo", DX, row4, 0xFFFFFF00, false);
        context.drawItem(vanillaProbe, DX, row4 + 10);
        context.drawText(this.textRenderer, "E2: mismo huevo, x2 entero", DX + COL_W, row4, 0xFFFFFF00, false);
        context.getMatrices().push();
        context.getMatrices().translate(DX + COL_W, row4 + 10, 0);
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
        // F10: ver skipBlurBackground.
        if (keyCode == org.lwjgl.glfw.GLFW.GLFW_KEY_F10) { skipBlurBackground = !skipBlurBackground; return true; }
        // F7: ver PanelScale.rawMode.
        if (keyCode == org.lwjgl.glfw.GLFW.GLFW_KEY_F7) { PanelScale.rawMode = !PanelScale.rawMode; return true; }
        // F12 (no F11: coincide con pantalla completa, atajo global de
        // Minecraft que no pasa por Screen.keyPressed y se activaría igual).
        if (keyCode == org.lwjgl.glfw.GLFW.GLFW_KEY_F12) { useNearestPanelDebug = !useNearestPanelDebug; return true; }
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
