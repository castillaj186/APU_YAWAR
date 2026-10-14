package pe.apuyawar.systems.panel;

import net.fabricmc.fabric.api.networking.v1.PayloadTypeRegistry;
import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;
import net.minecraft.item.ItemStack;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.text.Text;
import net.minecraft.world.GameMode;
import pe.apuyawar.systems.items.ModItems;
import pe.apuyawar.systems.missions.MissionCatalog;
import pe.apuyawar.systems.missions.MissionState;
import pe.apuyawar.systems.network.AdminAnnouncePayload;
import pe.apuyawar.systems.network.AdminGiveSolPePayload;
import pe.apuyawar.systems.network.AdminRouletteForcePayload;
import pe.apuyawar.systems.network.AdminTeleportToPayload;
import pe.apuyawar.systems.network.MissionsRequestPayload;
import pe.apuyawar.systems.network.MissionsSyncPayload;
import pe.apuyawar.systems.network.PanelOpenRequestPayload;
import pe.apuyawar.systems.network.PanelOpenResponsePayload;
import pe.apuyawar.systems.roulette.RoulettePalette;
import pe.apuyawar.systems.staff.StaffCommands;

/**
 * Lado servidor del Panel K: registra los paquetes C2S y decide, con autoridad real
 * del servidor, qué panel corresponde y si cada acción administrativa está permitida.
 * Un cliente modificado nunca puede otorgarse el panel administrativo ni ejecutar
 * sus acciones solo por abrir una pantalla: cada receptor vuelve a verificar
 * Staff + Creative antes de hacer nada.
 */
public final class PanelEvents {
    private PanelEvents() {}
    public static void initialize() {
        PayloadTypeRegistry.playC2S().register(PanelOpenRequestPayload.ID, PanelOpenRequestPayload.CODEC);
        PayloadTypeRegistry.playC2S().register(MissionsRequestPayload.ID, MissionsRequestPayload.CODEC);
        PayloadTypeRegistry.playC2S().register(AdminAnnouncePayload.ID, AdminAnnouncePayload.CODEC);
        PayloadTypeRegistry.playC2S().register(AdminGiveSolPePayload.ID, AdminGiveSolPePayload.CODEC);
        PayloadTypeRegistry.playC2S().register(AdminTeleportToPayload.ID, AdminTeleportToPayload.CODEC);
        PayloadTypeRegistry.playC2S().register(AdminRouletteForcePayload.ID, AdminRouletteForcePayload.CODEC);

        ServerPlayNetworking.registerGlobalReceiver(PanelOpenRequestPayload.ID, (payload, context) ->
                context.server().execute(() -> {
                    ServerPlayerEntity player = context.player();
                    boolean admin = isAuthorizedAdmin(player);
                    if (ServerPlayNetworking.canSend(player, PanelOpenResponsePayload.ID))
                        ServerPlayNetworking.send(player, new PanelOpenResponsePayload(admin));
                }));

        ServerPlayNetworking.registerGlobalReceiver(MissionsRequestPayload.ID, (payload, context) ->
                context.server().execute(() -> {
                    ServerPlayerEntity player = context.player();
                    MissionState state = MissionState.get(context.server());
                    int count = MissionCatalog.ALL.size();
                    int[] progress = new int[count];
                    boolean[] rewarded = new boolean[count];
                    for (int i = 0; i < count; i++) {
                        var mission = MissionCatalog.ALL.get(i);
                        progress[i] = state.progress(player.getUuid(), mission.id());
                        rewarded[i] = state.isRewarded(player.getUuid(), mission.id());
                    }
                    if (ServerPlayNetworking.canSend(player, MissionsSyncPayload.ID))
                        ServerPlayNetworking.send(player, new MissionsSyncPayload(progress, rewarded));
                }));

        ServerPlayNetworking.registerGlobalReceiver(AdminAnnouncePayload.ID, (payload, context) ->
                context.server().execute(() -> {
                    if (!isAuthorizedAdmin(context.player())) { denied(context.player()); return; }
                    String message = payload.message() == null ? "" : payload.message().strip();
                    if (message.isEmpty() || message.length() > 256) {
                        context.player().sendMessage(Text.literal("APU YAWAR | Mensaje vacío o demasiado largo."), false);
                        return;
                    }
                    context.server().getPlayerManager().broadcast(
                            Text.literal("§6[APU YAWAR] §f" + message), false);
                }));

        ServerPlayNetworking.registerGlobalReceiver(AdminGiveSolPePayload.ID, (payload, context) ->
                context.server().execute(() -> {
                    ServerPlayerEntity admin = context.player();
                    if (!isAuthorizedAdmin(admin)) { denied(admin); return; }
                    int amount = Math.max(1, Math.min(payload.amount(), 2304));
                    ServerPlayerEntity target = context.server().getPlayerManager().getPlayer(payload.targetName());
                    if (target == null) {
                        admin.sendMessage(Text.literal("APU YAWAR | Jugador no conectado: " + payload.targetName()), false);
                        return;
                    }
                    target.giveItemStack(new ItemStack(ModItems.SOL_PE, amount));
                    admin.sendMessage(Text.literal("APU YAWAR | Se entregaron " + amount + " Sol PE a "
                            + target.getName().getString() + "."), false);
                    target.sendMessage(Text.literal("APU YAWAR | Recibiste " + amount + " Sol PE del Staff."), false);
                }));

        ServerPlayNetworking.registerGlobalReceiver(AdminTeleportToPayload.ID, (payload, context) ->
                context.server().execute(() -> {
                    ServerPlayerEntity admin = context.player();
                    if (!isAuthorizedAdmin(admin)) { denied(admin); return; }
                    ServerPlayerEntity target = context.server().getPlayerManager().getPlayer(payload.targetName());
                    if (target == null) {
                        admin.sendMessage(Text.literal("APU YAWAR | Jugador no conectado: " + payload.targetName()), false);
                        return;
                    }
                    admin.teleport(target.getServerWorld(), target.getX(), target.getY(), target.getZ(),
                            target.getYaw(), target.getPitch());
                }));

        ServerPlayNetworking.registerGlobalReceiver(AdminRouletteForcePayload.ID, (payload, context) ->
                context.server().execute(() -> {
                    ServerPlayerEntity admin = context.player();
                    if (!isAuthorizedAdmin(admin)) { denied(admin); return; }
                    int color = payload.colorIndex();
                    if (color < 0 || color >= RoulettePalette.NAMES.length) return;
                    pe.apuyawar.systems.ApuYawarSystems.start(admin.getCommandSource(), color);
                }));
    }

    /** Única definición de "quién ve el panel administrativo": Staff registrado Y en
     * Creative en este momento. Ni el cliente ni el solo hecho de estar en Creative
     * bastan por sí solos. */
    private static boolean isAuthorizedAdmin(ServerPlayerEntity player) {
        return StaffCommands.isStaff(player.getServer(), player.getUuid())
                && player.interactionManager.getGameMode() == GameMode.CREATIVE;
    }
    private static void denied(ServerPlayerEntity player) {
        player.sendMessage(Text.literal("APU YAWAR | Acción no autorizada."), false);
    }
}
