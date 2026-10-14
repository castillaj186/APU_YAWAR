package pe.apuyawar.systems.panel.client;

/**
 * {@code panelBox()} calculaba originalmente un factor de escala continuo
 * arbitrario (ej. 0.4387...) para que el panel ocupara el 92% de la pantalla.
 * Un ratio así no tiene ninguna relación con la cuadrícula de texels de
 * origen: cada fila/columna de destino termina promediando una cantidad
 * distinta de texels de origen, lo que produce un patrón de muestreo
 * irregular (se percibe como "sucio"/blando) incluso con filtrado bilineal
 * correcto.
 *
 * {@link #snap(float)} redondea ese factor HACIA ABAJO al ratio limpio 1/n
 * más cercano (o 1.0 si cabe a tamaño nativo), para que el muestreo sea
 * regular y predecible en vez de depender de un porcentaje arbitrario de
 * pantalla. Se probó también dejar el factor sin redondear (más grande en
 * GUI pequeñas) y se confirmó visualmente que el tamaño redondeado a 1/n se
 * prefiere; este es el comportamiento permanente.
 */
final class PanelScale {
    private PanelScale() {}
    static float snap(float ideal) {
        if (ideal >= 1f) return 1f;
        if (ideal <= 0f) return 1f;
        int n = (int) Math.ceil(1f / ideal);
        return 1f / n;
    }
}
