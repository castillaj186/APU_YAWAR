package pe.apuyawar.lighting;

/** Clasifica un {@link EmitterProfile} para que el agrupador de emisores
 * (ver {@code client.EmitterSampler}) pueda decidir cuántos puntos de luz
 * generar por bloque y cómo distribuirlos, sin que cada tipo nuevo de
 * decoración tenga que escribir su propia lógica de agrupación. */
public enum EmitterType {
    /** Hoja/follaje: varios puntos pequeños y deterministas por posición
     * de bloque, nunca uno por cara. */
    FOLIAGE,
    /** Bloque sólido emisor (cristal, lámpara, portal): un único punto
     * centrado. */
    SOLID,
    /** Partícula/efecto puntual de corta vida, no asociado a un bloque
     * fijo (reservado para uso futuro). */
    TRANSIENT
}
