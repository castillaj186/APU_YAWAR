package pe.apuyawar.momentocuy.block;

import net.minecraft.block.AbstractBlock;
import net.minecraft.block.BlockState;
import net.minecraft.block.CherryLeavesBlock;
import net.minecraft.particle.DustParticleEffect;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.random.Random;
import net.minecraft.world.World;
import org.joml.Vector3f;

/**
 * Hojas florecidas del Cerezo Celestial: extiende {@link CherryLeavesBlock}
 * real de vanilla (no {@code LeavesBlock} simple) para heredar gratis sus
 * pétalos cayendo ({@code ParticleTypes.CHERRY_LEAVES}, vía
 * {@code super.randomDisplayTick}) y el goteo de lluvia; además añade un
 * aura mágica propia de destellos rosados/dorados. Es, a propósito, la
 * única de las 3 hojas de cerezo con este extra — las otras dos
 * ({@code hojas_cerezo_rosa_claro}/{@code hojas_cerezo_rosa_intenso}) usan
 * el {@code CherryLeavesBlock} de vanilla directamente, sin aura ni luz,
 * para no saturar el árbol de efectos ni volver luminosas todas las hojas
 * rosadas.
 */
public final class MomentoCuyBlossomLeavesBlock extends CherryLeavesBlock {
    private static final DustParticleEffect ROSA = new DustParticleEffect(new Vector3f(0.95f, 0.55f, 0.78f), 0.9f);
    private static final DustParticleEffect DORADO_INTI = new DustParticleEffect(new Vector3f(1.0f, 0.84f, 0.35f), 0.8f);

    public MomentoCuyBlossomLeavesBlock(AbstractBlock.Settings settings) {
        super(settings);
    }

    @Override
    public void randomDisplayTick(BlockState state, World world, BlockPos pos, Random random) {
        super.randomDisplayTick(state, world, pos, random);
        // Más presencia de noche, como pide el encargo, sin parpadeo violento:
        // sólo cambia la probabilidad de aparición, nunca la luz del bloque.
        int chance = world.isNight() ? 6 : 14;
        if (random.nextInt(chance) != 0) return;
        double x = pos.getX() + random.nextDouble();
        double y = pos.getY() + random.nextDouble() * 0.5 + 0.25;
        double z = pos.getZ() + random.nextDouble();
        DustParticleEffect effect = random.nextInt(3) == 0 ? DORADO_INTI : ROSA;
        world.addParticle(effect, x, y, z, 0.0, 0.01, 0.0);
    }
}
