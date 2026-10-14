package pe.apuyawar.systems.events;

import net.fabricmc.fabric.api.event.lifecycle.v1.*;
import net.fabricmc.fabric.api.entity.event.v1.ServerLivingEntityEvents;
import net.fabricmc.fabric.api.event.player.*;
import net.fabricmc.fabric.api.networking.v1.*;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.util.ActionResult;
import pe.apuyawar.systems.network.RouletteFinishedPayload;
import pe.apuyawar.systems.network.IntiFinishedPayload;

public final class JudgmentEvents {
    private JudgmentEvents(){}
    public static void initialize(){
        PayloadTypeRegistry.playC2S().register(RouletteFinishedPayload.ID,RouletteFinishedPayload.CODEC);
        PayloadTypeRegistry.playC2S().register(IntiFinishedPayload.ID,IntiFinishedPayload.CODEC);
        ServerPlayNetworking.registerGlobalReceiver(RouletteFinishedPayload.ID,(payload,context)->context.server().execute(()->JudgmentService.get(context.server()).acknowledge(context.player(),payload.sequence())));
        ServerPlayNetworking.registerGlobalReceiver(IntiFinishedPayload.ID,(payload,context)->context.server().execute(()->JudgmentService.get(context.server()).acknowledgeInti(context.player(),payload.sequence())));
        ServerLifecycleEvents.SERVER_STARTED.register(server->JudgmentService.get(server).difficulties.recoverWorldRules());
        ServerLifecycleEvents.SERVER_STOPPING.register(JudgmentService::shutdown);
        ServerTickEvents.END_SERVER_TICK.register(server->JudgmentService.get(server).tick());
        ServerEntityEvents.ENTITY_LOAD.register((entity,world)->JudgmentService.get(world.getServer()).loaded(entity));
        ServerLivingEntityEvents.AFTER_DEATH.register((entity,source)->{if(entity instanceof ServerPlayerEntity player)JudgmentService.get(player.getServer()).challenges.fail(player.getUuid());});
        ServerLivingEntityEvents.AFTER_DAMAGE.register((entity,source,base,taken,blocked)->{if(entity.getWorld() instanceof ServerWorld world)JudgmentService.get(world.getServer()).difficulties.damage(entity,source,taken,blocked);});
        ServerPlayConnectionEvents.JOIN.register((handler,sender,server)->{var service=JudgmentService.get(server);service.challenges.join(handler.player);if(!service.pending())service.difficulties.announce(handler.player);});
        ServerPlayConnectionEvents.DISCONNECT.register((handler,server)->JudgmentService.get(server).challenges.fail(handler.player.getUuid()));
        UseBlockCallback.EVENT.register((player,world,hand,hit)->player instanceof ServerPlayerEntity p?JudgmentService.get(p.getServer()).difficulties.useBlock(p,hand,hit.getBlockPos(),hit.getSide()):ActionResult.PASS);
        UseEntityCallback.EVENT.register((player,world,hand,entity,hit)->player instanceof ServerPlayerEntity p?JudgmentService.get(p.getServer()).difficulties.useEntity(p,entity,hand):ActionResult.PASS);
        PlayerBlockBreakEvents.AFTER.register((world,player,pos,state,entity)->{if(player instanceof ServerPlayerEntity p)JudgmentService.get(p.getServer()).difficulties.broken(p,state);});
    }
}
