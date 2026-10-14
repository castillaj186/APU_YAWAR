package pe.apuyawar.systems.panel.client;

import net.minecraft.client.font.TextRenderer;
import net.minecraft.client.gui.DrawContext;

import java.util.List;
import java.util.function.Function;

/**
 * Cuadrícula reutilizable de catálogo: contenido real (vía {@link CellRenderer},
 * una estrategia específica por tipo de contenido) + espacios "?" de contenido
 * bloqueado/futuro, con scroll, hover y selección. Genérica sobre el tipo de
 * entrada para que Objetos (ítems 2D) y Mobs (entidades 3D) compartan la misma
 * gestión de layout/scroll/hover/selección sin compartir el renderizado de cada
 * celda, que es fundamentalmente distinto entre ambos — ver {@link ItemCellRenderer}
 * y {@link MobCellRenderer}.
 */
final class CatalogGrid<T> {
    private static final int CELL = 40;
    private static final int INFO_HEIGHT = 12;
    private final List<T> entries;
    private final int futureSlots;
    private final CellRenderer<T> cellRenderer;
    private final Function<T, String> nameExtractor;
    private int scroll = 0;
    private int selected = -1;

    /** Dibuja únicamente el CONTENIDO de una celda normal (ya desbloqueada) de
     * tamaño {@code cellW x cellH} con esquina superior izquierda en
     * {@code (cx,cy)}. El fondo, el hover y el borde de selección ya los dibuja
     * {@link CatalogGrid#render}; cada estrategia solo dibuja su ícono/entidad. */
    @FunctionalInterface
    interface CellRenderer<T> {
        void render(DrawContext ctx, TextRenderer tr, T entry, int cx, int cy, int cellW, int cellH, boolean hover, boolean selected);
    }

    CatalogGrid(List<T> entries, int futureSlots, CellRenderer<T> cellRenderer, Function<T, String> nameExtractor) {
        this.entries = entries;
        this.futureSlots = futureSlots;
        this.cellRenderer = cellRenderer;
        this.nameExtractor = nameExtractor;
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
                boolean isSelected = index == selected;
                ctx.fill(cx, cy, cx + cellW, cy + cellH, 0x55000000);
                if (isSelected) drawOutline(ctx, cx, cy, cellW, cellH, 0xFFFFD700);
                else if (hover) drawOutline(ctx, cx, cy, cellW, cellH, 0x90FFFFFF);
                if (index < entries.size()) {
                    cellRenderer.render(ctx, tr, entries.get(index), cx, cy, cellW, cellH, hover, isSelected);
                } else {
                    // Contenido futuro no desbloqueado: símbolo "?" dibujado por código,
                    // nunca el modelo/nombre de ninguna criatura real — no debe confundirse
                    // con una entidad que falló al renderizarse (eso lo maneja cada
                    // CellRenderer con su propio fallback, no este símbolo).
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
            ctx.drawText(tr, nameExtractor.apply(entries.get(selected)), x, infoY, 0xFFFFFFFF, true);
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
