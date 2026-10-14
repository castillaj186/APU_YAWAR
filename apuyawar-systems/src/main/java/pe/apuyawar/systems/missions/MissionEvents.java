package pe.apuyawar.systems.missions;

import net.fabricmc.fabric.api.networking.v1.ServerPlayConnectionEvents;
import net.minecraft.item.ItemStack;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.text.Text;
import pe.apuyawar.systems.items.ModItems;

/** Progreso y recompensas reales de misiones; servidor-autoritativo, sin duplicados. */
public final class MissionEvents {
    private MissionEvents() {}
    public static void initialize() {
        ServerPlayConnectionEvents.JOIN.register((handler, sender, server) -> {
            ServerPlayerEntity player = handler.getPlayer();
            MissionState state = MissionState.get(server);
            for (MissionDefinition mission : MissionCatalog.ALL) {
                int value = state.increment(player.getUuid(), mission.id(), 1);
                tryReward(player, state, mission, value);
            }
        });
    }
    private static void tryReward(ServerPlayerEntity player, MissionState state, MissionDefinition mission, int progress) {
        if (progress < mission.required()) return;
        if (state.isRewarded(player.getUuid(), mission.id())) return;
        state.markRewarded(player.getUuid(), mission.id());
        player.giveItemStack(new ItemStack(ModItems.SOL_PE, mission.rewardSolPe()));
        player.sendMessage(Text.literal("APU YAWAR | Misión completada: " + mission.name()
                + " (+" + mission.rewardSolPe() + " Sol PE)"), false);
    }
}
