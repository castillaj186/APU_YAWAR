package pe.apuyawar.momentocuy.block;

import net.minecraft.block.AbstractBlock;
import net.minecraft.block.Block;
import net.minecraft.block.BlockState;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.util.ActionResult;
import net.minecraft.util.hit.BlockHitResult;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;
import pe.apuyawar.momentocuy.dimension.MomentoCuyTeleport;

/**
 * Portal del Momento Cuy: un teletransportador por interacción, no un marco
 * multibloque al estilo del portal del Nether (eso habría sido inventar una
 * mecánica de detección de estructura que no se pidió). Clic derecho dentro
 * de la dimensión devuelve al jugador a donde estaba; clic derecho fuera lo
 * lleva a Momento Cuy — misma lógica exacta que los comandos
 * {@code /momentocuy entrar}/{@code salir}, vía {@link MomentoCuyTeleport},
 * para que nunca queden desincronizados.
 */
public final class MomentoCuyPortalBlock extends Block {
    public MomentoCuyPortalBlock(AbstractBlock.Settings settings) {
        super(settings);
    }

    @Override
    protected ActionResult onUse(BlockState state, World world, BlockPos pos, PlayerEntity player, BlockHitResult hit) {
        if (world.isClient || !(player instanceof ServerPlayerEntity serverPlayer)) return ActionResult.SUCCESS;
        boolean inMomentoCuy = player.getWorld().getRegistryKey().equals(pe.apuyawar.momentocuy.dimension.MomentoCuyDimension.WORLD);
        if (inMomentoCuy) {
            var result = MomentoCuyTeleport.leave(serverPlayer);
            serverPlayer.sendMessage(result.message, true);
        } else {
            var result = MomentoCuyTeleport.enter(serverPlayer);
            serverPlayer.sendMessage(result.message, true);
        }
        return ActionResult.SUCCESS;
    }
}
