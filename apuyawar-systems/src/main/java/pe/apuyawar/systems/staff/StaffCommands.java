package pe.apuyawar.systems.staff;

import com.mojang.brigadier.exceptions.CommandSyntaxException;
import net.fabricmc.fabric.api.command.v2.CommandRegistrationCallback;
import net.minecraft.command.argument.EntityArgumentType;
import net.minecraft.server.command.ServerCommandSource;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.text.Text;
import net.minecraft.world.GameMode;

import java.util.UUID;

import static net.minecraft.server.command.CommandManager.argument;
import static net.minecraft.server.command.CommandManager.literal;

/**
 * {@code /apuyawar staff dar|quitar|lista|info}. El rango se guarda en
 * {@link StaffState} (servidor, persistente); un cliente no puede falsificarlo.
 *
 * Limitación actual: dar/quitar/info requieren que el jugador objetivo esté
 * conectado (se resuelve su UUID real desde la sesión activa). No hay todavía
 * soporte para asignar el rango a un jugador desconectado por nombre/UUID crudo.
 */
public final class StaffCommands {
    private StaffCommands() {}
    public static void initialize() {
        CommandRegistrationCallback.EVENT.register((dispatcher, access, environment) ->
            dispatcher.register(literal("apuyawar")
                .then(literal("staff")
                    .then(literal("dar")
                        .requires(source -> source.hasPermissionLevel(4))
                        .then(argument("jugador", EntityArgumentType.player())
                            .executes(StaffCommands::grant)))
                    .then(literal("quitar")
                        .requires(source -> source.hasPermissionLevel(4))
                        .then(argument("jugador", EntityArgumentType.player())
                            .executes(StaffCommands::revoke)))
                    .then(literal("lista")
                        .requires(source -> source.hasPermissionLevel(2))
                        .executes(StaffCommands::list))
                    .then(literal("info")
                        .requires(source -> source.hasPermissionLevel(2))
                        .then(argument("jugador", EntityArgumentType.player())
                            .executes(StaffCommands::info))))));
    }
    private static int grant(com.mojang.brigadier.context.CommandContext<ServerCommandSource> context) throws CommandSyntaxException {
        ServerPlayerEntity target = EntityArgumentType.getPlayer(context, "jugador");
        ServerCommandSource source = context.getSource();
        boolean added = StaffState.get(source.getServer()).add(target.getUuid());
        target.changeGameMode(GameMode.CREATIVE);
        if (added) {
            source.sendFeedback(() -> Text.literal("APU YAWAR | " + target.getName().getString()
                    + " ahora es Staff y fue cambiado a Creative."), true);
            target.sendMessage(Text.literal("APU YAWAR | Has sido ascendido a Staff."), false);
        } else {
            source.sendFeedback(() -> Text.literal("APU YAWAR | " + target.getName().getString()
                    + " ya era Staff; se confirmó el cambio a Creative."), true);
        }
        return 1;
    }
    private static int revoke(com.mojang.brigadier.context.CommandContext<ServerCommandSource> context) throws CommandSyntaxException {
        ServerPlayerEntity target = EntityArgumentType.getPlayer(context, "jugador");
        ServerCommandSource source = context.getSource();
        boolean removed = StaffState.get(source.getServer()).remove(target.getUuid());
        target.changeGameMode(GameMode.SURVIVAL);
        if (removed) {
            source.sendFeedback(() -> Text.literal("APU YAWAR | Se revocó el rango de Staff de "
                    + target.getName().getString() + " y volvió a Survival."), true);
            target.sendMessage(Text.literal("APU YAWAR | Tu rango de Staff fue revocado."), false);
        } else {
            source.sendError(Text.literal("APU YAWAR | " + target.getName().getString() + " no era Staff."));
        }
        return removed ? 1 : 0;
    }
    private static int list(com.mojang.brigadier.context.CommandContext<ServerCommandSource> context) {
        ServerCommandSource source = context.getSource();
        var staff = StaffState.get(source.getServer()).all();
        if (staff.isEmpty()) {
            source.sendFeedback(() -> Text.literal("APU YAWAR | No hay Staff registrado."), false);
            return 0;
        }
        StringBuilder names = new StringBuilder();
        for (UUID id : staff) {
            var profile = source.getServer().getUserCache() != null
                    ? source.getServer().getUserCache().getByUuid(id).map(p -> p.getName()).orElse(id.toString())
                    : id.toString();
            if (!names.isEmpty()) names.append(", ");
            names.append(profile);
        }
        source.sendFeedback(() -> Text.literal("APU YAWAR | Staff (" + staff.size() + "): " + names), false);
        return staff.size();
    }
    private static int info(com.mojang.brigadier.context.CommandContext<ServerCommandSource> context) throws CommandSyntaxException {
        ServerPlayerEntity target = EntityArgumentType.getPlayer(context, "jugador");
        ServerCommandSource source = context.getSource();
        boolean isStaff = StaffState.get(source.getServer()).isStaff(target.getUuid());
        source.sendFeedback(() -> Text.literal("APU YAWAR | " + target.getName().getString()
                + " | Staff: " + (isStaff ? "sí" : "no")
                + " | Modo de juego: " + target.interactionManager.getGameMode().getName()), false);
        return 1;
    }
    /** Usado por el sistema de panel para decidir qué pantalla abrir. */
    public static boolean isStaff(net.minecraft.server.MinecraftServer server, UUID uuid) {
        return StaffState.get(server).isStaff(uuid);
    }
}
