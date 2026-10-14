package pe.apuyawar.systems.panel.client;

import net.minecraft.client.font.TextRenderer;
import net.minecraft.client.gui.DrawContext;

/** Dibuja cada celda de la búsqueda unificada delegando en el renderizador real
 * ya existente de cada tipo de contenido — {@link ItemCellRenderer} para
 * objetos, {@link MobCellRenderer} para mobs — sin reimplementar ningún
 * dibujado nuevo; solo decide cuál de los dos usar según el tipo de resultado.
 * Comparte las MISMAS instancias que usan las pestañas Objetos y Mobs (pasadas
 * por {@link SurvivalPanelScreen}), para que la búsqueda reutilice la caché de
 * entidades de vista previa en vez de crear una segunda. */
final class SearchResultCellRenderer implements CatalogGrid.CellRenderer<SearchResult> {
    private final ItemCellRenderer itemRenderer;
    private final MobCellRenderer mobRenderer;

    SearchResultCellRenderer(ItemCellRenderer itemRenderer, MobCellRenderer mobRenderer) {
        this.itemRenderer = itemRenderer;
        this.mobRenderer = mobRenderer;
    }

    @Override public void render(DrawContext ctx, TextRenderer tr, SearchResult entry, int cx, int cy, int cellW, int cellH, boolean hover, boolean selected) {
        switch (entry) {
            case SearchResult.Item i -> itemRenderer.render(ctx, tr, i.entry(), cx, cy, cellW, cellH, hover, selected);
            case SearchResult.Mob m -> mobRenderer.render(ctx, tr, m.entry(), cx, cy, cellW, cellH, hover, selected);
        }
    }
}
