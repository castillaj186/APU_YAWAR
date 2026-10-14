package pe.apuyawar.momentocuy.dimension;

import net.minecraft.registry.RegistryKey;
import net.minecraft.registry.RegistryKeys;
import net.minecraft.util.Identifier;
import net.minecraft.world.World;

/** Identidad de la dimensión Momento Cuy. Los datos reales (tipo de dimensión,
 * generador, bioma) viven como datapack en {@code data/momento_cuy/...} — esta
 * clase solo expone la {@link RegistryKey} para que el resto del mod (y los
 * comandos) se refieran a la dimensión por un único punto. */
public final class MomentoCuyDimension {
    public static final String MOD_ID = "momento_cuy";
    public static final RegistryKey<World> WORLD = RegistryKey.of(RegistryKeys.WORLD, Identifier.of(MOD_ID, "momento_cuy"));

    private MomentoCuyDimension() {}
}
