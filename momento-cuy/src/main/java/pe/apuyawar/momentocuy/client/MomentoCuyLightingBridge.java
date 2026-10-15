package pe.apuyawar.momentocuy.client;

import net.minecraft.util.Identifier;
import pe.apuyawar.lighting.ApuYawarLighting;
import pe.apuyawar.lighting.EmitterProfile;
import pe.apuyawar.momentocuy.block.MomentoCuyBlocks;

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
        ApuYawarLighting.registerBlockEmitter(
                Identifier.of(MomentoCuyBlocks.MOD_ID, "hojas_pino_esmeralda"),
                EmitterProfile.foliage(0x00D9C8, 0.85f, 6.0f, 3)
                        .withSecondary(0x00FFF0)
                        .withAnimated()
                        .withPriority(2));
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
                        .withPriority(2));
    }
}
