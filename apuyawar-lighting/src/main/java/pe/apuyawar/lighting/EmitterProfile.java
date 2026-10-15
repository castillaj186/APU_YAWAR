package pe.apuyawar.lighting;

import net.minecraft.util.Identifier;

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
 * @param emissiveMaskSource ruta de la textura REAL (la cara "side" del
 *        bloque, tal como la referencia {@code momento_cuy} la registra)
 *        a partir de la cual {@code client.EmissiveMaskGenerator} deriva
 *        una máscara de píxeles emisivos (V4, fase 1) -- {@code null} si
 *        este perfil no tiene follaje emisivo real, sólo los puntos/halos
 *        de siempre.
 * @param warmPalette      V4 fase 2: si las partículas celestiales
 *        (chispas/estrellas, ver {@code client.CelestialParticleSpawner})
 *        de este perfil usan la paleta cálida (rosa/dorado, Cerezo
 *        Celestial) en vez de la fría (turquesa/cian, Abeto Celestial) --
 *        "no reutilices exactamente el mismo comportamiento del abeto"
 *        (sección 4.7 del encargo V4).
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
        boolean animated,
        Identifier emissiveMaskSource,
        boolean warmPalette
) {
    public static EmitterProfile foliage(int rgb, float intensity, float radius, int pointsPerBlock) {
        return new EmitterProfile(
                ((rgb >> 16) & 0xFF) / 255f, ((rgb >> 8) & 0xFF) / 255f, (rgb & 0xFF) / 255f,
                null, null, null,
                intensity, radius, 2.0f, EmitterType.FOLIAGE, pointsPerBlock, 0, false, null, false);
    }

    public EmitterProfile withSecondary(int rgb) {
        return new EmitterProfile(red, green, blue,
                ((rgb >> 16) & 0xFF) / 255f, ((rgb >> 8) & 0xFF) / 255f, (rgb & 0xFF) / 255f,
                intensity, radius, falloff, type, pointsPerBlock, priority, animated, emissiveMaskSource, warmPalette);
    }

    public EmitterProfile withAnimated() {
        return new EmitterProfile(red, green, blue, secondaryRed, secondaryGreen, secondaryBlue,
                intensity, radius, falloff, type, pointsPerBlock, priority, true, emissiveMaskSource, warmPalette);
    }

    public EmitterProfile withPriority(int newPriority) {
        return new EmitterProfile(red, green, blue, secondaryRed, secondaryGreen, secondaryBlue,
                intensity, radius, falloff, type, pointsPerBlock, newPriority, animated, emissiveMaskSource, warmPalette);
    }

    /** V4 fase 1: asocia este perfil a una textura real de bloque de la
     * que derivar una máscara de píxeles emisivos -- ver
     * {@code client.EmissiveMaskGenerator}. */
    public EmitterProfile withEmissiveMask(Identifier sourceTexture) {
        return new EmitterProfile(red, green, blue, secondaryRed, secondaryGreen, secondaryBlue,
                intensity, radius, falloff, type, pointsPerBlock, priority, animated, sourceTexture, warmPalette);
    }

    /** V4 fase 2: marca este perfil para usar la paleta cálida de
     * partículas celestiales (rosa/dorado) en vez de la fría
     * (turquesa/cian). */
    public EmitterProfile withWarmPalette() {
        return new EmitterProfile(red, green, blue, secondaryRed, secondaryGreen, secondaryBlue,
                intensity, radius, falloff, type, pointsPerBlock, priority, animated, emissiveMaskSource, true);
    }

    public boolean hasSecondary() {
        return secondaryRed != null;
    }

    public boolean hasEmissiveMask() {
        return emissiveMaskSource != null;
    }
}
