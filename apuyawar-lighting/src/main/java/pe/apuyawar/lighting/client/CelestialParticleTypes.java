package pe.apuyawar.lighting.client;

import net.fabricmc.fabric.api.client.particle.v1.ParticleFactoryRegistry;
import net.fabricmc.fabric.api.particle.v1.FabricParticleTypes;
import net.minecraft.particle.SimpleParticleType;
import net.minecraft.registry.Registries;
import net.minecraft.registry.Registry;
import net.minecraft.util.Identifier;

/**
 * APU YAWAR Lighting V4, fase 2 ("partículas celestiales
 * personalizadas"): dos tipos nuevos, aparte de la "mota" ya existente
 * ({@code MomentoCuyAbetoLeavesBlock}, que sigue intacta) --
 * {@code CHISPA_*} (núcleo brillante + halo, ver
 * {@link CelestialSparkParticle}) y {@code ESTRELLA_*} (destello breve
 * con la secuencia de brillo animada de vanilla, ver
 * {@link CelestialStarParticle}). Cada una existe en dos paletas --
 * turquesa (Abeto) y rosa/dorado (Cerezo, sección 4.7 del encargo: "no
 * reutilices exactamente el mismo comportamiento del abeto") -- en vez
 * de llevar un color dinámico (un {@link SimpleParticleType} no
 * transporta datos por instancia; cuatro tipos fijos es más simple y
 * transparente que un {@code ParticleEffect} complejo con codec propio
 * para esto).
 *
 * <p>Las texturas se REUTILIZAN de vanilla (sección 4.5 del encargo:
 * "no quiero usar solamente partículas Dust más grandes" -- pero
 * tampoco se dibuja ningún sprite nuevo a mano esta ronda): "chispa"
 * usa {@code minecraft:glow} (el mismo sprite suave de núcleo+halo que
 * ya usa el calamar brillante), "estrella" usa la secuencia
 * {@code minecraft:glitter_0..7} (el mismo destello animado que ya usa
 * el End Rod) -- ver
 * {@code assets/apuyawar_lighting/particles/*.json}.</p>
 */
public final class CelestialParticleTypes {
    private static final String MOD_ID = "apuyawar_lighting";

    public static final SimpleParticleType CHISPA_TURQUESA = FabricParticleTypes.simple();
    public static final SimpleParticleType CHISPA_ROSA = FabricParticleTypes.simple();
    public static final SimpleParticleType ESTRELLA_TURQUESA = FabricParticleTypes.simple();
    public static final SimpleParticleType ESTRELLA_ROSA = FabricParticleTypes.simple();

    private CelestialParticleTypes() {}

    static void register() {
        Registry.register(Registries.PARTICLE_TYPE, Identifier.of(MOD_ID, "chispa_turquesa"), CHISPA_TURQUESA);
        Registry.register(Registries.PARTICLE_TYPE, Identifier.of(MOD_ID, "chispa_rosa"), CHISPA_ROSA);
        Registry.register(Registries.PARTICLE_TYPE, Identifier.of(MOD_ID, "estrella_turquesa"), ESTRELLA_TURQUESA);
        Registry.register(Registries.PARTICLE_TYPE, Identifier.of(MOD_ID, "estrella_rosa"), ESTRELLA_ROSA);

        var registry = ParticleFactoryRegistry.getInstance();
        registry.register(CHISPA_TURQUESA, provider -> new CelestialSparkParticle.Factory(provider, 0x00D9C8));
        registry.register(CHISPA_ROSA, provider -> new CelestialSparkParticle.Factory(provider, 0xFF8FC9));
        registry.register(ESTRELLA_TURQUESA, provider -> new CelestialStarParticle.Factory(provider, 0x00FFF0));
        registry.register(ESTRELLA_ROSA, provider -> new CelestialStarParticle.Factory(provider, 0xFFD659));
    }
}
