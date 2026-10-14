package pe.apuyawar.systems.panel.client;

/** Un resultado de la búsqueda unificada: un mob hostil o un objeto fabricable.
 * Nunca un espacio "?" de contenido bloqueado — ver {@link SurvivalPanelScreen}. */
sealed interface SearchResult {
    record Mob(MobCatalogEntry entry) implements SearchResult {}
    record Item(CatalogEntry entry) implements SearchResult {}
}
