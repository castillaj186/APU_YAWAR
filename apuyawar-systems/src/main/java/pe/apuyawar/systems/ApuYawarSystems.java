package pe.apuyawar.systems;

import net.fabricmc.api.ModInitializer;
import net.fabricmc.fabric.api.command.v2.CommandRegistrationCallback;
import net.fabricmc.fabric.api.networking.v1.PayloadTypeRegistry;
import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;
import net.minecraft.server.command.CommandManager;
import net.minecraft.server.command.ServerCommandSource;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.text.Text;
import com.mojang.brigadier.arguments.StringArgumentType;
import pe.apuyawar.systems.network.RouletteStartPayload;
import pe.apuyawar.systems.network.IntiStartPayload;
import pe.apuyawar.systems.network.PanelOpenResponsePayload;
import pe.apuyawar.systems.network.MissionsSyncPayload;
import pe.apuyawar.systems.roulette.RoulettePalette;
import java.util.concurrent.ThreadLocalRandom;

/** Punto de entrada del mod técnico. No reutiliza clases ni identificadores antiguos. */
public final class ApuYawarSystems implements ModInitializer {
    public static final String MOD_ID = "apuyawar_systems";

    @Override public void onInitialize() {
        pe.apuyawar.systems.items.ModItems.initialize();
        pe.apuyawar.systems.staff.StaffCommands.initialize();
        pe.apuyawar.systems.panel.PanelEvents.initialize();
        pe.apuyawar.systems.events.JudgmentEvents.initialize();
        pe.apuyawar.systems.events.JudgmentCommands.initialize();
        PayloadTypeRegistry.playS2C().register(RouletteStartPayload.ID, RouletteStartPayload.CODEC);
        PayloadTypeRegistry.playS2C().register(IntiStartPayload.ID, IntiStartPayload.CODEC);
        PayloadTypeRegistry.playS2C().register(PanelOpenResponsePayload.ID, PanelOpenResponsePayload.CODEC);
        PayloadTypeRegistry.playS2C().register(MissionsSyncPayload.ID, MissionsSyncPayload.CODEC);
        pe.apuyawar.systems.missions.MissionEvents.initialize();
        CommandRegistrationCallback.EVENT.register((dispatcher, registryAccess, environment) ->
            dispatcher.register(CommandManager.literal("apuyawar_ruleta")
                .requires(source -> source.hasPermissionLevel(2))
                .then(CommandManager.literal("tirar")
                    .executes(context -> start(context.getSource(), ThreadLocalRandom.current().nextInt(8))))
                .then(CommandManager.literal("forzar")
                    .then(CommandManager.argument("color", StringArgumentType.word())
                        .executes(context -> {
                            int color = RoulettePalette.indexOf(StringArgumentType.getString(context, "color"));
                            if (color < 0) {
                                context.getSource().sendError(Text.literal("Color no válido. Usa: " + String.join(", ", RoulettePalette.NAMES)));
                                return 0;
                            }
                            return start(context.getSource(), color);
                        }))))) ;
    }

    public static int start(ServerCommandSource source, int color) {
        var service=pe.apuyawar.systems.events.JudgmentService.get(source.getServer());
        if(service.busy(color)){source.sendError(Text.literal("APU YAWAR | Ya existe un evento activo de ese color."));return 0;}
        java.util.List<java.util.UUID> recipients=new java.util.ArrayList<>();
        for(ServerPlayerEntity player:source.getServer().getPlayerManager().getPlayerList())
            if(ServerPlayNetworking.canSend(player,RouletteStartPayload.ID))recipients.add(player.getUuid());
        if(recipients.isEmpty()){source.sendError(Text.literal("APU YAWAR | No hay clientes compatibles conectados."));return 0;}
        long sequence=service.queue(color,recipients);
        int delivered = 0;
        for (ServerPlayerEntity player : source.getServer().getPlayerManager().getPlayerList()) {
            if (ServerPlayNetworking.canSend(player, RouletteStartPayload.ID)) {
                ServerPlayNetworking.send(player, new RouletteStartPayload(color,sequence));
                delivered++;
            }
        }
        final int recipientCount = delivered;
        source.sendFeedback(() -> Text.literal("APU YAWAR | Resultado: " + RoulettePalette.NAMES[color]
            + " | Clientes compatibles: " + recipientCount), true);
        return 1;
    }
}
