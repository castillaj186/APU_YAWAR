package pe.apuyawar.systems.panel.client;

import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.gui.widget.ButtonWidget;
import net.minecraft.text.Text;
import pe.apuyawar.systems.missions.MissionCatalog;
import pe.apuyawar.systems.missions.MissionDefinition;

/**
 * Pantalla independiente de Misiones (botón Pergamino). Reutiliza el mismo fondo
 * {@code panel.png} que el panel principal para mantener la identidad visual de
 * APU YAWAR en vez de texto plano sobre el mundo. Lista a la izquierda, detalles
 * a la derecha; el progreso real llega del servidor vía {@code MissionsSyncPayload}
 * y se aplica con {@link #applySync}.
 */
public final class MissionsScreen extends Screen {
    private int[] progress = new int[MissionCatalog.ALL.size()];
    private boolean[] rewarded = new boolean[MissionCatalog.ALL.size()];
    private int selected = 0;
    private int listX, listY, listW, listH, detailX, detailY, detailW;

    public MissionsScreen() { super(Text.literal("Misiones - APU YAWAR")); }

    /** Llamado por el receptor de red cuando llega el progreso real del servidor. */
    public void applySync(int[] progress, boolean[] rewarded) {
        this.progress = progress;
        this.rewarded = rewarded;
    }

    /** Mismo ratio limpio 1/n que {@link SurvivalPanelScreen#panelBox()}, para
     * que ambas pantallas muestren el panel con el mismo factor de escala y
     * evitar un muestreo fraccionario arbitrario de texels. */
    private int[] panelBox() {
        float ideal = Math.min(this.width * 0.92f / PanelTextures.PANEL_W, this.height * 0.92f / PanelTextures.PANEL_H);
        float scale = PanelScale.snap(ideal);
        int w = Math.round(PanelTextures.PANEL_W * scale), h = Math.round(PanelTextures.PANEL_H * scale);
        int x = (this.width - w) / 2, y = (this.height - h) / 2;
        return new int[]{x, y, w, h};
    }

    @Override protected void init() {
        int[] box = panelBox();
        var content = PanelLayout.CONTENT_AREA;
        int fx = box[0] + (int) (content.x0() * box[2]);
        int fy = box[1] + (int) (content.y0() * box[3]);
        int fw = (int) (content.width() * box[2]);
        int fh = (int) (content.height() * box[3]);
        listX = fx + 6; listY = fy + 24; listW = Math.min(150, fw / 3); listH = fh - 50;
        detailX = listX + listW + 14; detailY = fy + 24; detailW = fw - listW - 26;
        addDrawableChild(ButtonWidget.builder(Text.literal("< Volver al Panel"), b -> {
            if (this.client != null) this.client.setScreen(new SurvivalPanelScreen());
        }).dimensions(fx, fy + fh - 20, 110, 18).build());
    }

    /** {@code Screen.render()} vuelve a llamar a {@code renderBackground()}
     * internamente antes de pintar los widgets (botón "Volver"); eso aplicaba
     * una segunda pasada de blur sobre el panel ya dibujado. El fondo real de
     * esta pantalla es el {@code fill()} oscuro de {@link #render}, así que se
     * sobrescribe como no-op para bloquear esa llamada oculta. */
    @Override public void renderBackground(DrawContext context, int mouseX, int mouseY, float delta) {
    }
    @Override public void render(DrawContext context, int mouseX, int mouseY, float delta) {
        context.fill(0, 0, this.width, this.height, 0xCC000000);
        ViewportFix.restoreAfterBackground(this.client);
        int[] box = panelBox();
        context.drawTexture(PanelTextures.PANEL, box[0], box[1], box[2], box[3], 0f, 0f,
                PanelTextures.PANEL_W, PanelTextures.PANEL_H, PanelTextures.PANEL_W, PanelTextures.PANEL_H);
        var content = PanelLayout.CONTENT_AREA;
        int fx = box[0] + (int) (content.x0() * box[2]);
        int fy = box[1] + (int) (content.y0() * box[3]);
        int fw = (int) (content.width() * box[2]);
        int fh = (int) (content.height() * box[3]);
        context.enableScissor(fx, fy, fx + fw, fy + fh);
        context.fill(fx - 6, fy - 6, fx + fw + 6, fy + fh + 6, 0x8A000000);
        context.drawText(this.textRenderer, "Misiones de APU YAWAR", fx + 6, fy + 6, 0xFFFFD700, true);

        var all = MissionCatalog.ALL;
        int rowH = 18;
        for (int i = 0; i < all.size(); i++) {
            MissionDefinition mission = all.get(i);
            boolean complete = progress[i] >= mission.required();
            int rowY = listY + i * rowH;
            boolean hover = mouseX >= listX && mouseX < listX + listW && mouseY >= rowY && mouseY < rowY + rowH;
            if (i == selected) context.fill(listX - 2, rowY - 1, listX + listW, rowY + rowH - 2, 0x5055FFAA);
            else if (hover) context.fill(listX - 2, rowY - 1, listX + listW, rowY + rowH - 2, 0x30FFFFFF);
            int color = complete ? 0xFF55FF55 : 0xFFEEEEEE;
            context.drawText(this.textRenderer, (complete ? "[Completa] " : "[En progreso] ") + mission.name(),
                    listX, rowY + 4, color, false);
        }

        if (selected >= 0 && selected < all.size()) {
            MissionDefinition mission = all.get(selected);
            boolean complete = progress[selected] >= mission.required();
            context.fill(detailX - 6, detailY - 6, detailX + detailW, detailY + 84, 0x60000000);
            context.drawText(this.textRenderer, mission.name(), detailX, detailY, 0xFFFFD700, true);
            context.drawText(this.textRenderer, mission.description(), detailX, detailY + 16, 0xFFFFFFFF, false);
            context.drawText(this.textRenderer, "Progreso: " + Math.min(progress[selected], mission.required())
                    + " / " + mission.required(), detailX, detailY + 32, 0xFFCCCCCC, false);
            context.drawText(this.textRenderer, "Recompensa: " + mission.rewardSolPe() + " Sol PE", detailX, detailY + 48, 0xFFFFD700, false);
            String state = !complete ? "Pendiente" : (rewarded[selected] ? "Completada y recompensada" : "Completada");
            context.drawText(this.textRenderer, "Estado: " + state, detailX, detailY + 64, complete ? 0xFF55FF55 : 0xFFAAAAAA, false);
        }
        context.disableScissor();
        super.render(context, mouseX, mouseY, delta);
    }

    @Override public boolean mouseClicked(double mouseX, double mouseY, int button) {
        var all = MissionCatalog.ALL;
        int rowH = 18;
        for (int i = 0; i < all.size(); i++) {
            int rowY = listY + i * rowH;
            if (mouseX >= listX && mouseX < listX + listW && mouseY >= rowY && mouseY < rowY + rowH) {
                selected = i;
                return true;
            }
        }
        return super.mouseClicked(mouseX, mouseY, button);
    }

    @Override public boolean shouldPause() { return false; }
}
