package pe.apuyawar.systems.panel.client;

/**
 * Posición de los 4 espacios de botones de {@code panel.png}, como fracciones del
 * tamaño del panel (0..1). Estos valores NO son estimados a ojo: se midieron
 * programáticamente escaneando los píxeles reales de {@code panel.png}
 * (1672x941) con System.Drawing, buscando el rectángulo oscuro interior de cada
 * espacio (contraste contra el marco dorado), validado en 3 columnas distintas
 * (x=130,160,180,220) y 4 filas distintas (una por slot).
 *
 * Medición real en píxeles de imagen (x0=111, x1=249 para los cuatro; cada slot
 * tiene su propia altura — el cuarto es realmente más bajo que los otros tres,
 * confirmado, no es un error de medición):
 *   slot 1: y 106-240   slot 2: y 319-452   slot 3: y 530-661   slot 4: y 730-843
 */
final class PanelLayout {
    record Slot(double x0, double y0, double x1, double y1) {
        double width() { return x1 - x0; }
        double height() { return y1 - y0; }
    }
    private static Slot px(int x0, int y0, int x1, int y1) {
        return new Slot(x0 / (double) PanelTextures.PANEL_W, y0 / (double) PanelTextures.PANEL_H,
                         x1 / (double) PanelTextures.PANEL_W, y1 / (double) PanelTextures.PANEL_H);
    }
    static final Slot SLOT_MOBS     = px(111, 106, 249, 240);
    static final Slot SLOT_ITEMS    = px(111, 319, 249, 452);
    static final Slot SLOT_MISSIONS = px(111, 530, 249, 661);
    static final Slot SLOT_SEARCH   = px(111, 730, 249, 843);
    /** Zona derecha del panel (la escena andina) donde se dibuja el contenido de cada pestaña. */
    static final Slot CONTENT_AREA  = new Slot(0.195, 0.07, 0.97, 0.93);
    private PanelLayout() {}
}
