package pe.apuyawar.momentocuy.block;

import net.minecraft.block.SaplingGenerator;
import net.minecraft.registry.RegistryKey;
import net.minecraft.registry.RegistryKeys;
import net.minecraft.util.Identifier;
import net.minecraft.world.gen.feature.ConfiguredFeature;

import java.util.Optional;

/**
 * Árboles personalizados de Momento Cuy: cada uno es, igual que los árboles
 * de Minecraft, una {@link ConfiguredFeature} de tipo {@code minecraft:tree}
 * definida como dato (JSON) en
 * {@code data/momento_cuy/worldgen/configured_feature/}, referenciada por
 * clave desde un {@link SaplingGenerator} — la misma arquitectura que usa
 * vanilla ({@code SaplingGenerator.CHERRY}, {@code SaplingGenerator.SPRUCE},
 * etc. son instancias concretas de esta clase final, no subclases propias).
 * No se registra ningún {@code placed_feature}: sólo queremos que el brote
 * pueda convertirse en árbol, no generación de bosques en el chunk.
 */
public final class MomentoCuyTrees {
    public static final RegistryKey<ConfiguredFeature<?, ?>> CEREZO_CELESTIAL = RegistryKey.of(
            RegistryKeys.CONFIGURED_FEATURE, Identifier.of(MomentoCuyBlocks.MOD_ID, "cerezo_celestial"));
    public static final RegistryKey<ConfiguredFeature<?, ?>> ABETO_CELESTIAL = RegistryKey.of(
            RegistryKeys.CONFIGURED_FEATURE, Identifier.of(MomentoCuyBlocks.MOD_ID, "abeto_celestial"));

    // Firma real (confirmada por bytecode, no la de 3 Optional que parecía a
    // primera vista): (id, megaVariant, regularVariant, beesVariant). No
    // queremos variante "mega" (árbol 2x2) ni de abejas.
    public static final SaplingGenerator CEREZO_GENERATOR =
            new SaplingGenerator("cerezo_celestial", Optional.empty(), Optional.of(CEREZO_CELESTIAL), Optional.empty());
    public static final SaplingGenerator ABETO_GENERATOR =
            new SaplingGenerator("abeto_celestial", Optional.empty(), Optional.of(ABETO_CELESTIAL), Optional.empty());

    private MomentoCuyTrees() {}
}
