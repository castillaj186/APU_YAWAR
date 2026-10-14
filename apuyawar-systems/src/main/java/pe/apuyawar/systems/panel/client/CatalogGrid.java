package pe.apuyawar.systems.panel.client;

import net.minecraft.client.font.TextRenderer;
import net.minecraft.client.gui.DrawContext;

import java.util.List;

/**
 * Cuadrícula reutilizable de catálogo: íconos reales + espacios "?" de contenido
 * bloqueado/futuro, con scroll y selección. Usada por las pestañas de Mobs y
 * Objetos para no duplicar la misma lógica de cuadrícula dos veces.
 */
final class CatalogGrid {
    private static final int CELL = 40;
    /** Entero a propósito. El atlas de ítems vanilla se muestrea con
     * GL_NEAREST (por eso los ítems se ven pixelados/nítidos en cualquier
     * inventario); con un factor NO entero (antes 1.7f) cada píxel de
     * destino cae en un texel distinto de forma irregular — un patrón de
     * aliasing que se percibe como "sucio"/borroso aunque no haya ningún
     * blur real. Con un entero, cada texel se repite un número exacto de
     * veces y el resultado es tan nítido como el ítem nativo, solo más
     * grande (confirmado: los huevos de aparición vanilla, que no usan
     * ninguna textura nuestra, se veían borrosos solo aquí, con el 1.7f). */
    private static final int ICON_SCALE = 2;
    private static final int INFO_HEIGHT = 12;
    private final List<CatalogEntry> entries;
    private final int futureSlots;
    private int scroll = 0;
    private int selected = -1;
    CatalogGrid(List<CatalogEntry> entries, int futureSlots) {
        this.entries = entries;
        this.futureSlots = futureSlots;
    }
    private int totalSlots() { return entries.size() + futureSlots; }
    void render(DrawContext ctx, TextRenderer tr, int x, int y, int w, int h, int mouseX, int mouseY) {
        int cols = Math.max(1, w / CELL);
        int rows = Math.max(1, (h - INFO_HEIGHT) / CELL);
        int totalRows = (totalSlots() + cols - 1) / cols;
        int maxScroll = Math.max(0, totalRows - rows);
        if (scroll > maxScroll) scroll = maxScroll;
        for (int row = 0; row < rows; row++) {
            for (int col = 0; col < cols; col++) {
                int index = (row + scroll) * cols + col;
                if (index >= totalSlots()) continue;
                int cx = x + col * CELL, cy = y + row * CELL;
                int cellW = CELL - 4, cellH = CELL - 4;
                boolean hover = mouseX >= cx && mouseX < cx + cellW && mouseY >= cy && mouseY < cy + cellH;
                ctx.fill(cx, cy, cx + cellW, cy + cellH, 0x55000000);
                if (index == selected) drawOutline(ctx, cx, cy, cellW, cellH, 0xFFFFD700);
                else if (hover) drawOutline(ctx, cx, cy, cellW, cellH, 0x90FFFFFF);
                if (index < entries.size()) {
                    // Ícono real agrandado: el renderizador de ítems ya usa el mismo
                    // MatrixStack de este DrawContext, así que empujar una escala
                    // ENTERA antes de dibujar lo agranda sin deformarlo ni introducir
                    // aliasing (ver comentario de ICON_SCALE).
                    ctx.getMatrices().push();
                    int pad = (cellW - 16 * ICON_SCALE) / 2;
                    ctx.getMatrices().translate(cx + pad, cy + pad, 0);
                    ctx.getMatrices().scale(ICON_SCALE, ICON_SCALE, 1f);
                    ctx.drawItem(entries.get(index).icon(), 0, 0);
                    ctx.getMatrices().pop();
                } else {
                    ctx.getMatrices().push();
                    ctx.getMatrices().translate(cx + cellW / 2f - 3, cy + cellH / 2f - 4, 0);
                    ctx.getMatrices().scale(1.6f, 1.6f, 1f);
                    ctx.drawText(tr, "?", 0, 0, 0x90FFFFFF, false);
                    ctx.getMatrices().pop();
                }
            }
        }
        int infoY = y + rows * CELL + 2;
        if (selected >= 0 && selected < entries.size()) {
            ctx.drawText(tr, entries.get(selected).name(), x, infoY, 0xFFFFFFFF, true);
        } else if (selected >= entries.size() && selected < totalSlots()) {
            ctx.drawText(tr, "??? (contenido no desbloqueado)", x, infoY, 0xFFAAAAAA, true);
        }
    }
    private void drawOutline(DrawContext ctx, int x, int y, int w, int h, int color) {
        ctx.fill(x, y, x + w, y + 1, color);
        ctx.fill(x, y + h - 1, x + w, y + h, color);
        ctx.fill(x, y, x + 1, y + h, color);
        ctx.fill(x + w - 1, y, x + w, y + h, color);
    }
    boolean mouseClicked(int x, int y, int w, int h, double mouseX, double mouseY) {
        if (mouseX < x || mouseY < y || mouseX >= x + w || mouseY >= y + h) return false;
        int cols = Math.max(1, w / CELL);
        int col = (int) ((mouseX - x) / CELL);
        int row = (int) ((mouseY - y) / CELL);
        int index = (row + scroll) * cols + col;
        if (index < 0 || index >= totalSlots()) return false;
        selected = index;
        return true;
    }
    boolean mouseScrolled(double amount) {
        int before = scroll;
        scroll = Math.max(0, scroll - (int) Math.signum(amount));
        return scroll != before;
    }
}
