package pe.apuyawar.momentocuy.block;

import net.minecraft.block.AbstractBlock;
import net.minecraft.block.Block;
import net.minecraft.block.BlockState;
import net.minecraft.entity.Entity;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;

/** Bloque de meta: avisa a {@link MinigameTriggers} cuando algo llega. El
 * motor de minijuegos decide si eso termina una partida real. */
public final class FinishLineBlock extends Block {
    public FinishLineBlock(AbstractBlock.Settings settings) {
        super(settings);
    }

    @Override
    public void onLandedUpon(World world, BlockState state, BlockPos pos, Entity entity, float fallDistance) {
        super.onLandedUpon(world, state, pos, entity, fallDistance);
        if (world instanceof ServerWorld serverWorld) {
            MinigameTriggers.fire(serverWorld, pos, entity, MinigameTriggers.Kind.FINISH);
        }
    }
}
