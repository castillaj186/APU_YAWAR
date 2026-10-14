package pe.apuyawar.momentocuy.block;

import net.minecraft.block.AbstractBlock;
import net.minecraft.block.Block;
import net.minecraft.entity.Entity;
import net.minecraft.util.math.Vec3d;
import net.minecraft.world.BlockView;

/**
 * Bloque rebotador: mismo mecanismo real que {@code SlimeBlock}
 * ({@code AbstractBlock.onEntityLand}, confirmado por bytecode), con un
 * impulso vertical mayor para un salto más "de minijuego". No niega el daño
 * de caída al aterrizar encima en movimiento normal (igual que el slime).
 */
public final class BouncePadBlock extends Block {
    private static final double BOUNCE_MULTIPLIER = -1.4;
    private static final double MIN_UPWARD_VELOCITY = 0.35;

    public BouncePadBlock(AbstractBlock.Settings settings) {
        super(settings);
    }

    @Override
    public void onEntityLand(BlockView world, Entity entity) {
        if (entity.bypassesLandingEffects()) {
            super.onEntityLand(world, entity);
            return;
        }
        Vec3d velocity = entity.getVelocity();
        if (velocity.y < 0.0) {
            double bounced = velocity.y * BOUNCE_MULTIPLIER;
            entity.setVelocity(velocity.x, Math.max(bounced, MIN_UPWARD_VELOCITY), velocity.z);
            entity.velocityModified = true;
        }
    }
}
