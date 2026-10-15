package pe.apuyawar.lighting.config;

/** Perfiles predefinidos (sección 11 del encargo). Elegir uno sobrescribe
 * los campos ajustables de {@link LightingConfig} con el paquete de valores
 * de abajo; tocar un valor individual después cambia el perfil activo a
 * {@link #CUSTOM} automáticamente.
 *
 * <p><b>Corrección V2 (ver docs/23)</b>: el equipo señaló correctamente
 * que {@code fixedPointDensityMultiplier} (el campo que el renderizador
 * usa de verdad desde la ronda anterior) y {@code maxEffectDistance}
 * (relevante para el costo del escaneo, sección 3) no estaban en este
 * enum ni en {@code applyPreset()} — cambiar a HIGH no los tocaba, dejando
 * valores "antiguos" aunque el perfil dijera HIGH. Ahora los 8 campos que
 * representan calidad/rendimiento están aquí y se aplican todos juntos.</p> */
public enum LightingQuality {
    LOW(0.6f, 0.75f, 0.4f, 0.8f, 0.5f, 24, 24.0f, 14.0f),
    MEDIUM(0.85f, 1.0f, 0.7f, 1.0f, 0.8f, 48, 40.0f, 20.0f),
    HIGH(1.15f, 1.25f, 1.0f, 1.3f, 1.0f, 96, 56.0f, 28.0f),
    CUSTOM(1.0f, 1.0f, 1.0f, 1.0f, 1.0f, 48, 40.0f, 20.0f);

    public final float lightIntensityMultiplier;
    public final float lightRadiusMultiplier;
    public final float bloomIntensity;
    public final float bloomRadiusMultiplier;
    /** Valor histórico, ver {@code LightingConfig#particleDensityMultiplier}
     * — usar {@link #fixedPointDensityMultiplier()} en vez de este campo
     * directamente. */
    public final float particleDensityMultiplier;
    public final int maxActiveLights;
    public final float maxEffectDistance;
    public final float simplifyDetailBeyondDistance;

    LightingQuality(float lightIntensityMultiplier, float lightRadiusMultiplier, float bloomIntensity,
                     float bloomRadiusMultiplier, float particleDensityMultiplier, int maxActiveLights,
                     float maxEffectDistance, float simplifyDetailBeyondDistance) {
        this.lightIntensityMultiplier = lightIntensityMultiplier;
        this.lightRadiusMultiplier = lightRadiusMultiplier;
        this.bloomIntensity = bloomIntensity;
        this.bloomRadiusMultiplier = bloomRadiusMultiplier;
        this.particleDensityMultiplier = particleDensityMultiplier;
        this.maxActiveLights = maxActiveLights;
        this.maxEffectDistance = maxEffectDistance;
        this.simplifyDetailBeyondDistance = simplifyDetailBeyondDistance;
    }

    /** Densidad de puntos fijos del perfil -- mismo valor que
     * {@link #particleDensityMultiplier} (histórico), expuesto con el
     * nombre que realmente usa el renderizador para que
     * {@code applyPreset()} no tenga que leer un campo con un nombre que
     * ya no describe lo que hace. */
    public float fixedPointDensityMultiplier() {
        return particleDensityMultiplier;
    }
}
