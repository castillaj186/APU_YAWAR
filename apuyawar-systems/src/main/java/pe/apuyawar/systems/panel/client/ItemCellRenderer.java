package pe.apuyawar.systems.panel.client;

import net.minecraft.client.font.TextRenderer;
import net.minecraft.client.gui.DrawContext;

/**
 * Estrategia de renderizado de celda para el catálogo de Objetos: el ícono real
 * del {@link CatalogEntry}, agrandado por un factor entero. Extraída, sin cambiar
 * su comportamiento, de la antigua {@code CatalogGrid} no genérica.
 */
final class ItemCellRenderer implements CatalogGrid.CellRenderer<CatalogEntry> {
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

    @Override public void render(DrawContext ctx, TextRenderer tr, CatalogEntry entry, int cx, int cy, int cellW, int cellH, boolean hover, boolean selected) {
        // El renderizador de ítems ya usa el mismo MatrixStack de este DrawContext,
        // así que empujar una escala ENTERA antes de dibujar lo agranda sin
        // deformarlo ni introducir aliasing (ver comentario de ICON_SCALE).
        ctx.getMatrices().push();
        int pad = (cellW - 16 * ICON_SCALE) / 2;
        ctx.getMatrices().translate(cx + pad, cy + pad, 0);
        ctx.getMatrices().scale(ICON_SCALE, ICON_SCALE, 1f);
        ctx.drawItem(entry.icon(), 0, 0);
        ctx.getMatrices().pop();
    }
}
