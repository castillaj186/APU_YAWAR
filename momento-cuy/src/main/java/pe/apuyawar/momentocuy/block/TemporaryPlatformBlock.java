package pe.apuyawar.momentocuy.block;

import net.minecraft.block.AbstractBlock;
import net.minecraft.block.Block;
import net.minecraft.block.BlockState;
import net.minecraft.block.Blocks;
import net.minecraft.entity.Entity;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.random.Random;
import net.minecraft.world.World;

/**
 * Plataforma temporal: se desvanece unos segundos después de que algo se
 * pare sobre ella (vanilla ya hace exactamente esto con la nieve en polvo o el
 * hielo escarchado; aquí se usa el mismo mecanismo real, {@code
 * WorldAccess.scheduleBlockTick}, confirmado por bytecode en
 * {@code FarmlandBlock}). Pensado para minijuegos de plataformas que
 * desaparecen — la REGLA de cuándo usarla la decide cada arena, este bloque
 * solo implementa el comportamiento físico del bloque en sí.
 */
public final class TemporaryPlatformBlock extends Block {
    private static final int DISAPPEAR_DELAY_TICKS = 20; // 1 segundo tras pisarla

    public TemporaryPlatformBlock(AbstractBlock.Settings settings) {
        super(settings);
    }

    @Override
    public void onLandedUpon(World world, BlockState state, BlockPos pos, Entity entity, float fallDistance) {
        super.onLandedUpon(world, state, pos, entity, fallDistance);
        if (world instanceof ServerWorld serverWorld && serverWorld.getBlockState(pos).isOf(this)) {
            serverWorld.scheduleBlockTick(pos, this, DISAPPEAR_DELAY_TICKS);
        }
    }

    @Override
    protected void scheduledTick(BlockState state, ServerWorld world, BlockPos pos, Random random) {
        // Vuelve a aire solo si sigue siendo esta plataforma (pudo haber sido
        // reemplazada o reconstruida por el constructor mientras tanto).
        if (world.getBlockState(pos).isOf(this)) {
            world.setBlockState(pos, Blocks.AIR.getDefaultState(), Block.NOTIFY_ALL);
        }
    }
}
