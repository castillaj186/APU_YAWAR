package pe.apuyawar.momentocuy.block;

/**
 * Punto de ajuste de densidad para las partículas móviles de las hojas
 * celestiales ({@code MomentoCuyAbetoLeavesBlock},
 * {@code MomentoCuyBlossomLeavesBlock}) — clase común, sin ningún import
 * de {@code apuyawar-lighting}, a propósito: los bloques de hoja se
 * registran incondicionalmente en el inicializador común de Momento Cuy,
 * así que nunca pueden depender de una clase que sólo existe si el motor
 * RGB (opcional, {@code environment: "client"}) está instalado.
 *
 * <p>Si {@code apuyawar-lighting} está presente,
 * {@code pe.apuyawar.momentocuy.client.MomentoCuyLightingBridge} (que SÍ
 * puede importar clases de {@code apuyawar-lighting}, porque sólo se
 * invoca desde el inicializador de CLIENTE, ya protegido con
 * {@code FabricLoader.isModLoaded}) sincroniza
 * {@link #movingParticleDensityMultiplier} cada tick desde
 * {@code LightingConfig#particleDensityMultiplier}. Si no está presente,
 * el valor se queda en 1.0 (densidad normal, sin ningún control externo)
 * — las hojas funcionan igual que si el motor RGB no existiera.</p>
 */
public final class MomentoCuyParticleTuning {
    private static volatile float movingParticleDensityMultiplier = 1.0f;

    private MomentoCuyParticleTuning() {}

    public static float movingParticleDensityMultiplier() {
        return movingParticleDensityMultiplier;
    }

    public static void setMovingParticleDensityMultiplier(float value) {
        movingParticleDensityMultiplier = Math.max(0.0f, Math.min(4.0f, value));
    }
}
