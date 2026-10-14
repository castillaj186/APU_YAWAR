package pe.apuyawar.momentocuy.block;

import net.minecraft.block.AbstractBlock;
import net.minecraft.block.Block;
import net.minecraft.block.BlockState;
import net.minecraft.entity.Entity;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;

/** Baldosa activadora genérica: avisa a {@link MinigameTriggers} cuando algo
 * la pisa. Qué hace eso (abrir una puerta, soltar un mob, iniciar un
 * cronómetro...) lo decide cada minijuego al escuchar el evento — este bloque
 * no implementa ninguna regla concreta todavía. */
public final class TriggerTileBlock extends Block {
    public TriggerTileBlock(AbstractBlock.Settings settings) {
        super(settings);
    }

    @Override
    public void onLandedUpon(World world, BlockState state, BlockPos pos, Entity entity, float fallDistance) {
        super.onLandedUpon(world, state, pos, entity, fallDistance);
        if (world instanceof ServerWorld serverWorld) {
            MinigameTriggers.fire(serverWorld, pos, entity, MinigameTriggers.Kind.TRIGGER_TILE);
        }
    }
}
