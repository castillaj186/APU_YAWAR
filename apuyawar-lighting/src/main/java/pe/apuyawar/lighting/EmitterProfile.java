package pe.apuyawar.lighting;

/**
 * Descripción, por tipo de bloque, de cómo debe verse su luz RGB — nunca se
 * crea un emisor por instancia de bloque sin que alguien lo pida: esta
 * clase es sólo la "receta" que {@code client.EmitterSampler} usa para
 * generar un número fijo y determinista de puntos de luz por posición de
 * bloque visible.
 *
 * @param red,green,blue  color del emisor, 0-1.
 * @param secondaryRed,secondaryGreen,secondaryBlue color secundario
 *        opcional (p. ej. los destellos dorados del cerezo); si es
 *        {@code null} sólo se usa el color principal.
 * @param intensity       brillo relativo base, 0-1 (se multiplica por la
 *        configuración global y por el multiplicador día/noche).
 * @param radius           radio de influencia en bloques del halo/decal de
 *        suelo.
 * @param falloff          exponente de atenuación (1 = lineal, 2 = cuadrática
 *        — más alto, caída más abrupta cerca del borde).
 * @param type              ver {@link EmitterType}.
 * @param pointsPerBlock   cuántos puntos de luz como máximo genera un solo
 *        bloque de este tipo (ver sección "no una luz GPU por hoja" del
 *        encargo) — determinista por posición, no aleatorio por frame.
 * @param priority          mayor = se mantiene activo primero si se supera
 *        el máximo global de luces.
 * @param animated          si la intensidad debe oscilar lentamente
 *        (parpadeo tipo vela, no parpadeo brusco).
 */
public record EmitterProfile(
        float red, float green, float blue,
        Float secondaryRed, Float secondaryGreen, Float secondaryBlue,
        float intensity,
        float radius,
        float falloff,
        EmitterType type,
        int pointsPerBlock,
        int priority,
        boolean animated
) {
    public static EmitterProfile foliage(int rgb, float intensity, float radius, int pointsPerBlock) {
        return new EmitterProfile(
                ((rgb >> 16) & 0xFF) / 255f, ((rgb >> 8) & 0xFF) / 255f, (rgb & 0xFF) / 255f,
                null, null, null,
                intensity, radius, 2.0f, EmitterType.FOLIAGE, pointsPerBlock, 0, false);
    }

    public EmitterProfile withSecondary(int rgb) {
        return new EmitterProfile(red, green, blue,
                ((rgb >> 16) & 0xFF) / 255f, ((rgb >> 8) & 0xFF) / 255f, (rgb & 0xFF) / 255f,
                intensity, radius, falloff, type, pointsPerBlock, priority, animated);
    }

    public EmitterProfile withAnimated() {
        return new EmitterProfile(red, green, blue, secondaryRed, secondaryGreen, secondaryBlue,
                intensity, radius, falloff, type, pointsPerBlock, priority, true);
    }

    public EmitterProfile withPriority(int newPriority) {
        return new EmitterProfile(red, green, blue, secondaryRed, secondaryGreen, secondaryBlue,
                intensity, radius, falloff, type, pointsPerBlock, newPriority, animated);
    }

    public boolean hasSecondary() {
        return secondaryRed != null;
    }
}
