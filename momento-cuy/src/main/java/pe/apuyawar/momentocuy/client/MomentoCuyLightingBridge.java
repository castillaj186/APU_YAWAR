package pe.apuyawar.momentocuy.client;

import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.minecraft.util.Identifier;
import pe.apuyawar.lighting.ApuYawarLighting;
import pe.apuyawar.lighting.EmitterProfile;
import pe.apuyawar.lighting.client.ApuYawarLightingClient;
import pe.apuyawar.momentocuy.block.MomentoCuyBlocks;
import pe.apuyawar.momentocuy.block.MomentoCuyParticleTuning;

/**
 * Registra las hojas del Abeto y el Cerezo Celestial como emisores RGB de
 * APU YAWAR Lighting. Clase aparte (no inline en {@code MomentoCuyClient})
 * a propósito: {@link MomentoCuyClient#onInitializeClient()} sólo llama a
 * {@link #register()} dentro de un
 * {@code FabricLoader.isModLoaded("apuyawar_lighting")}, así que mientras
 * nada fuera de este archivo referencie {@code pe.apuyawar.lighting.*}, la
 * JVM nunca intenta resolver esas clases si el jar no estuviera presente
 * (no debería ocurrir: se incluye vía jar-in-jar, pero esto protege contra
 * una instalación manual rota).
 */
final class MomentoCuyLightingBridge {
    private MomentoCuyLightingBridge() {}

    static void register() {
        // Abeto Celestial — neón turquesa intenso (ver docs/23). Hojas
        // esmeralda como superficie principal luminosa; sombría aporta
        // contraste con menos prioridad.
        // V4 fase 1: "withEmissiveMask" apunta a la cara "side" REAL de
        // cada bloque (la misma que usa su propio modelo, ver
        // momento-cuy/.../models/block/hojas_pino_esmeralda.json) --
        // EmissiveMaskGenerator deriva de ella una máscara de píxeles
        // claros, no se dibuja ninguna textura nueva a mano.
        ApuYawarLighting.registerBlockEmitter(
                Identifier.of(MomentoCuyBlocks.MOD_ID, "hojas_pino_esmeralda"),
                EmitterProfile.foliage(0x00D9C8, 0.85f, 6.0f, 3)
                        .withSecondary(0x00FFF0)
                        .withAnimated()
                        .withPriority(2)
                        .withEmissiveMask(Identifier.of(MomentoCuyBlocks.MOD_ID, "block/hojas_pino_esmeralda/side")));
        ApuYawarLighting.registerBlockEmitter(
                Identifier.of(MomentoCuyBlocks.MOD_ID, "hojas_pino_sombrio"),
                EmitterProfile.foliage(0x064C4B, 0.45f, 4.0f, 1)
                        .withPriority(1));

        // Cerezo Celestial — más suave y cálido, con destello dorado de
        // Inti ocasional (ver MomentoCuyBlossomLeavesBlock, que ya hace
        // esto por partículas; aquí es el halo/luz localizada).
        ApuYawarLighting.registerBlockEmitter(
                Identifier.of(MomentoCuyBlocks.MOD_ID, "hojas_cerezo_florecido"),
                EmitterProfile.foliage(0xFF70BB, 0.6f, 5.0f, 2)
                        .withSecondary(0xFFD659)
                        .withAnimated()
                        .withPriority(2)
                        .withEmissiveMask(Identifier.of(MomentoCuyBlocks.MOD_ID, "block/hojas_cerezo_florecido/side"))
                        .withWarmPalette());

        // Corrección "Error 6": la densidad de partículas MÓVILES
        // (LightingConfig#particleDensityMultiplier, ajustable por
        // /apuyawarlighting) debe llegar de verdad a
        // MomentoCuyAbetoLeavesBlock/MomentoCuyBlossomLeavesBlock, no sólo
        // existir en el config sin efecto. Se sincroniza cada tick de
        // cliente (no sólo al cargar) para que un cambio en caliente del
        // comando se note sin reiniciar el mundo.
        ClientTickEvents.END_CLIENT_TICK.register(client ->
                MomentoCuyParticleTuning.setMovingParticleDensityMultiplier(
                        ApuYawarLightingClient.config().particleDensityMultiplier));
    }
}
