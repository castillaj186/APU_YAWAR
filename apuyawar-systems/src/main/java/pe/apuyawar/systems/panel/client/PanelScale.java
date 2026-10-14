package pe.apuyawar.systems.panel.client;

/**
 * Antes, {@code panelBox()} calculaba un factor de escala continuo arbitrario
 * (ej. 0.4387...) para que el panel ocupara el 92% de la pantalla. Un ratio
 * así no tiene ninguna relación con la cuadrícula de texels de origen: cada
 * fila/columna de destino termina promediando una cantidad distinta de
 * texels de origen, lo que produce un patrón de muestreo irregular (se
 * percibe como "sucio"/blando) incluso con filtrado bilineal correcto.
 *
 * {@link #snap(float)} redondea ese factor HACIA ABAJO al ratio limpio 1/n
 * más cercano (o 1.0 si cabe a tamaño nativo), para que el muestreo sea
 * regular y predecible en vez de depender de un porcentaje arbitrario de
 * pantalla.
 *
 * Reporte de PR #1: en GUI pequeñas (ej. 480x253 lógico, escala 4), el ideal
 * real (≈0.247) cae justo debajo de 1/4 y {@link #snap} lo redondea hasta
 * 1/5 (0.20), desaprovechando espacio y encogiendo el panel y sus 4 íconos
 * más de lo necesario. {@link #resolve(float)} añade una vía de comparación
 * en vivo: con {@link #rawMode} encendido (tecla F7, ver
 * {@link SurvivalPanelScreen}), devuelve el {@code ideal} SIN redondear, para
 * que el usuario pueda confirmar en el mismo cliente, con las mismas texturas
 * y el mismo filtro, si aprovechar mejor la resolución devuelve la nitidez
 * sin reintroducir el muestreo irregular que motivó {@link #snap} en primer
 * lugar. Apagado por defecto: el comportamiento ya validado (snap a 1/n) se
 * mantiene salvo que el usuario encienda la prueba explícitamente.
 */
final class PanelScale {
    private PanelScale() {}
    /** F7 en {@link SurvivalPanelScreen}: ver javadoc de la clase. */
    static boolean rawMode = false;
    static float snap(float ideal) {
        if (ideal >= 1f) return 1f;
        if (ideal <= 0f) return 1f;
        int n = (int) Math.ceil(1f / ideal);
        return 1f / n;
    }
    /** Factor de escala real a aplicar: {@link #snap(float)} salvo que
     * {@link #rawMode} esté activo, en cuyo caso devuelve {@code ideal} tal
     * cual. Usado por {@link SurvivalPanelScreen#panelBox()} y
     * {@link MissionsScreen#panelBox()} para que ambas pantallas respondan
     * igual a la prueba A/B. */
    static float resolve(float ideal) {
        if (ideal >= 1f) return 1f;
        if (ideal <= 0f) return 1f;
        return rawMode ? ideal : snap(ideal);
    }
}
