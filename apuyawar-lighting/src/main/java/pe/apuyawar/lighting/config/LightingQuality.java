package pe.apuyawar.lighting.config;

/** Perfiles predefinidos (sección 11 del encargo). Elegir uno sobrescribe
 * los campos ajustables de {@link LightingConfig} con el paquete de valores
 * de abajo; tocar un valor individual después cambia el perfil activo a
 * {@link #CUSTOM} automáticamente. */
public enum LightingQuality {
    LOW(0.6f, 0.75f, 0.4f, 0.8f, 0.5f, 24),
    MEDIUM(0.85f, 1.0f, 0.7f, 1.0f, 0.8f, 48),
    HIGH(1.15f, 1.25f, 1.0f, 1.3f, 1.0f, 96),
    CUSTOM(1.0f, 1.0f, 1.0f, 1.0f, 1.0f, 48);

    public final float lightIntensityMultiplier;
    public final float lightRadiusMultiplier;
    public final float bloomIntensity;
    public final float bloomRadiusMultiplier;
    public final float particleDensityMultiplier;
    public final int maxActiveLights;

    LightingQuality(float lightIntensityMultiplier, float lightRadiusMultiplier, float bloomIntensity,
                     float bloomRadiusMultiplier, float particleDensityMultiplier, int maxActiveLights) {
        this.lightIntensityMultiplier = lightIntensityMultiplier;
        this.lightRadiusMultiplier = lightRadiusMultiplier;
        this.bloomIntensity = bloomIntensity;
        this.bloomRadiusMultiplier = bloomRadiusMultiplier;
        this.particleDensityMultiplier = particleDensityMultiplier;
        this.maxActiveLights = maxActiveLights;
    }
}
