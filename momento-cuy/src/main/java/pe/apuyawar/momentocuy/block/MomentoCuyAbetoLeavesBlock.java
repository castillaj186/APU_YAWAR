package pe.apuyawar.momentocuy.block;

import net.minecraft.block.AbstractBlock;
import net.minecraft.block.BlockState;
import net.minecraft.block.LeavesBlock;
import net.minecraft.particle.DustParticleEffect;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.random.Random;
import net.minecraft.world.World;
import org.joml.Vector3f;

/**
 * Hojas del Abeto Celestial: {@link LeavesBlock} real de vanilla (colisión,
 * capa de render, persistencia/distancia al tronco y descomposición natural
 * iguales a cualquier hoja vanilla — ver {@code MomentoCuyBlocks} para el
 * {@code .ticksRandomly()} que activa esa descomposición), con un
 * {@code randomDisplayTick} propio que añade chispas turquesas/celestes
 * "de luciérnaga" alrededor de las ramas. Usada para las dos variedades
 * (esmeralda y sombría) — las partículas son iguales en ambas; sólo
 * {@code hojas_pino_esmeralda} recibe además luminancia en
 * {@code MomentoCuyBlocks.baseSettings}, para no iluminar todo el bosque.
 */
public final class MomentoCuyAbetoLeavesBlock extends LeavesBlock {
    private static final DustParticleEffect TURQUESA = new DustParticleEffect(new Vector3f(0.25f, 0.85f, 0.82f), 0.8f);
    private static final DustParticleEffect CELESTE = new DustParticleEffect(new Vector3f(0.42f, 0.70f, 1.0f), 0.7f);

    public MomentoCuyAbetoLeavesBlock(AbstractBlock.Settings settings) {
        super(settings);
    }

    @Override
    public void randomDisplayTick(BlockState state, World world, BlockPos pos, Random random) {
        super.randomDisplayTick(state, world, pos, random);
        int chance = world.isNight() ? 8 : 20;
        if (random.nextInt(chance) != 0) return;
        double x = pos.getX() + random.nextDouble();
        double y = pos.getY() + random.nextDouble() * 0.6 + 0.1;
        double z = pos.getZ() + random.nextDouble();
        // Deriva vertical lenta, como una luciérnaga suspendida en vez de caer.
        double driftY = (random.nextDouble() - 0.3) * 0.01;
        DustParticleEffect effect = random.nextBoolean() ? TURQUESA : CELESTE;
        world.addParticle(effect, x, y, z, 0.0, driftY, 0.0);
    }
}
