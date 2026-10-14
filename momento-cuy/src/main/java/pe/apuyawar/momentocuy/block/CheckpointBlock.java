package pe.apuyawar.momentocuy.block;

import net.minecraft.block.AbstractBlock;
import net.minecraft.block.Block;
import net.minecraft.block.BlockState;
import net.minecraft.entity.Entity;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;

/** Bloque de checkpoint: avisa a {@link MinigameTriggers} cuando algo se para
 * encima. No guarda ningún progreso por sí mismo — eso es responsabilidad del
 * motor de minijuegos que escuche el evento. */
public final class CheckpointBlock extends Block {
    public CheckpointBlock(AbstractBlock.Settings settings) {
        super(settings);
    }

    @Override
    public void onLandedUpon(World world, BlockState state, BlockPos pos, Entity entity, float fallDistance) {
        super.onLandedUpon(world, state, pos, entity, fallDistance);
        if (world instanceof ServerWorld serverWorld) {
            MinigameTriggers.fire(serverWorld, pos, entity, MinigameTriggers.Kind.CHECKPOINT);
        }
    }
}
