package pe.apuyawar.momentocuy;

import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.arguments.IntegerArgumentType;
import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import net.fabricmc.fabric.api.command.v2.CommandRegistrationCallback;
import net.minecraft.server.command.CommandManager;
import net.minecraft.server.command.ServerCommandSource;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.text.Text;
import net.minecraft.util.math.BlockPos;
import pe.apuyawar.momentocuy.dimension.MomentoCuyDimension;
import pe.apuyawar.momentocuy.dimension.MomentoCuyLobby;
import pe.apuyawar.momentocuy.dimension.MomentoCuyTeleport;
import pe.apuyawar.momentocuy.entity.MomentoCuyEntities;
import pe.apuyawar.momentocuy.minigame.Arena;
import pe.apuyawar.momentocuy.minigame.ArenaRegistry;
import pe.apuyawar.momentocuy.minigame.MinigameManager;
import pe.apuyawar.momentocuy.minigame.MinigameMatch;

import java.util.ArrayList;
import java.util.List;

/**
 * {@code /momentocuy ...}. "Acceso normal sin permisos de operador": {@code
 * entrar}/{@code salir} no exigen ningún nivel de permiso (a diferencia del
 * mod antiguo, donde TODO el comando raíz requería nivel 2 — cambio
 * deliberado pedido explícitamente para este mod). Las herramientas
 * administrativas de prueba ({@code lobby}, {@code cuy}, {@code arena},
 * {@code minijuego}) sí requieren nivel 2, nodo por nodo.
 */
public final class MomentoCuyCommands {
    private MomentoCuyCommands() {}

    public static void initialize() {
        CommandRegistrationCallback.EVENT.register((dispatcher, registryAccess, environment) -> register(dispatcher));
    }

    private static void register(CommandDispatcher<ServerCommandSource> dispatcher) {
        dispatcher.register(CommandManager.literal("momentocuy")
                .then(CommandManager.literal("entrar").executes(c -> enter(c.getSource())))
                .then(CommandManager.literal("salir").executes(c -> leave(c.getSource())))
                .then(CommandManager.literal("lobby").requires(s -> s.hasPermissionLevel(2))
                        .then(CommandManager.literal("estado").executes(c -> lobbyStatus(c.getSource())))
                        .then(CommandManager.literal("construir").executes(c -> lobbyBuild(c.getSource()))))
                .then(CommandManager.literal("cuy").requires(s -> s.hasPermissionLevel(2))
                        .then(CommandManager.literal("colocar").executes(c -> placeCuy(c.getSource()))))
                .then(CommandManager.literal("arena").requires(s -> s.hasPermissionLevel(2))
                        .then(CommandManager.literal("registrar")
                                .then(CommandManager.argument("id", StringArgumentType.word())
                                        .then(CommandManager.argument("radio", IntegerArgumentType.integer(1, 200))
                                                .executes(c -> registerArena(c.getSource(), StringArgumentType.getString(c, "id"), IntegerArgumentType.getInteger(c, "radio"))))))
                        .then(CommandManager.literal("checkpoint")
                                .then(CommandManager.argument("id", StringArgumentType.word())
                                        .executes(c -> addCheckpoint(c.getSource(), StringArgumentType.getString(c, "id")))))
                        .then(CommandManager.literal("meta")
                                .then(CommandManager.argument("id", StringArgumentType.word())
                                        .executes(c -> setFinish(c.getSource(), StringArgumentType.getString(c, "id")))))
                        .then(CommandManager.literal("lista").executes(c -> listArenas(c.getSource()))))
                .then(CommandManager.literal("minijuego").requires(s -> s.hasPermissionLevel(2))
                        .then(CommandManager.literal("iniciar")
                                .then(CommandManager.argument("id", StringArgumentType.word())
                                        .executes(c -> startMatch(c.getSource(), StringArgumentType.getString(c, "id")))))
                        .then(CommandManager.literal("finalizar")
                                .then(CommandManager.argument("id", StringArgumentType.word())
                                        .executes(c -> finishMatch(c.getSource(), StringArgumentType.getString(c, "id")))))
                        .then(CommandManager.literal("cancelar")
                                .then(CommandManager.argument("id", StringArgumentType.word())
                                        .executes(c -> cancelMatch(c.getSource(), StringArgumentType.getString(c, "id")))))));
    }

    private static int enter(ServerCommandSource source) throws CommandSyntaxException {
        ServerPlayerEntity player = source.getPlayerOrThrow();
        var result = MomentoCuyTeleport.enter(player);
        source.sendFeedback(() -> result.message, false);
        return result == MomentoCuyTeleport.EnterResult.OK ? 1 : 0;
    }

    private static int leave(ServerCommandSource source) throws CommandSyntaxException {
        ServerPlayerEntity player = source.getPlayerOrThrow();
        var result = MomentoCuyTeleport.leave(player);
        source.sendFeedback(() -> result.message, false);
        return result == MomentoCuyTeleport.LeaveResult.OK ? 1 : 0;
    }

    private static int lobbyStatus(ServerCommandSource source) {
        ServerWorld world = source.getServer().getWorld(MomentoCuyDimension.WORLD);
        boolean loaded = world != null;
        boolean built = loaded && MomentoCuyLobby.exists(world);
        source.sendFeedback(() -> Text.literal("[Momento Cuy] Dimension=" + (loaded ? "CARGADA" : "FALTA")
                + " | Plataforma=" + (built ? "CONSTRUIDA" : "PENDIENTE")), false);
        return loaded ? 1 : 0;
    }

    private static int lobbyBuild(ServerCommandSource source) {
        ServerWorld world = source.getServer().getWorld(MomentoCuyDimension.WORLD);
        if (world == null) {
            source.sendFeedback(() -> Text.literal("[Momento Cuy] La dimension no esta cargada."), false);
            return 0;
        }
        boolean built = MomentoCuyLobby.ensure(world);
        source.sendFeedback(() -> Text.literal(built ? "[Momento Cuy] Plataforma construida." : "[Momento Cuy] Ya existia; no se reconstruyo."), false);
        return 1;
    }

    private static int placeCuy(ServerCommandSource source) throws CommandSyntaxException {
        ServerPlayerEntity player = source.getPlayerOrThrow();
        if (!(player.getWorld() instanceof ServerWorld world)) return 0;
        var cuy = MomentoCuyEntities.CUY_JARDINERO.create(world);
        if (cuy == null) return 0;
        cuy.refreshPositionAndAngles(player.getX(), player.getY(), player.getZ(), player.getYaw(), 0);
        world.spawnEntity(cuy);
        source.sendFeedback(() -> Text.literal("[Momento Cuy] Cuy Jardinero colocado."), false);
        return 1;
    }

    private static int registerArena(ServerCommandSource source, String id, int radius) throws CommandSyntaxException {
        ServerPlayerEntity player = source.getPlayerOrThrow();
        BlockPos center = player.getBlockPos();
        BlockPos corner1 = center.add(-radius, -radius, -radius);
        BlockPos corner2 = center.add(radius, radius, radius);
        ArenaRegistry.register(new Arena(id, corner1, corner2, center, new ArrayList<>(), null));
        source.sendFeedback(() -> Text.literal("[Momento Cuy] Arena '" + id + "' registrada: " + corner1 + " a " + corner2 + ", inicio en " + center), false);
        return 1;
    }

    private static int addCheckpoint(ServerCommandSource source, String id) throws CommandSyntaxException {
        ServerPlayerEntity player = source.getPlayerOrThrow();
        var existing = ArenaRegistry.get(id);
        if (existing.isEmpty()) {
            source.sendFeedback(() -> Text.literal("[Momento Cuy] No existe la arena '" + id + "'."), false);
            return 0;
        }
        Arena arena = existing.get();
        List<BlockPos> checkpoints = new ArrayList<>(arena.checkpoints());
        checkpoints.add(player.getBlockPos());
        ArenaRegistry.register(new Arena(arena.id(), arena.corner1(), arena.corner2(), arena.startPoint(), checkpoints, arena.finishPoint()));
        source.sendFeedback(() -> Text.literal("[Momento Cuy] Checkpoint #" + checkpoints.size() + " agregado a '" + id + "'."), false);
        return 1;
    }

    private static int setFinish(ServerCommandSource source, String id) throws CommandSyntaxException {
        ServerPlayerEntity player = source.getPlayerOrThrow();
        var existing = ArenaRegistry.get(id);
        if (existing.isEmpty()) {
            source.sendFeedback(() -> Text.literal("[Momento Cuy] No existe la arena '" + id + "'."), false);
            return 0;
        }
        Arena arena = existing.get();
        ArenaRegistry.register(new Arena(arena.id(), arena.corner1(), arena.corner2(), arena.startPoint(), arena.checkpoints(), player.getBlockPos()));
        source.sendFeedback(() -> Text.literal("[Momento Cuy] Meta de '" + id + "' fijada en " + player.getBlockPos()), false);
        return 1;
    }

    private static int listArenas(ServerCommandSource source) {
        var arenas = ArenaRegistry.all();
        source.sendFeedback(() -> Text.literal("[Momento Cuy] " + arenas.size() + " arena(s) registrada(s) esta sesion: "
                + arenas.stream().map(Arena::id).reduce((a, b) -> a + ", " + b).orElse("ninguna")), false);
        return arenas.size();
    }

    private static int startMatch(ServerCommandSource source, String id) throws CommandSyntaxException {
        ServerPlayerEntity player = source.getPlayerOrThrow();
        var arena = ArenaRegistry.get(id);
        if (arena.isEmpty()) {
            source.sendFeedback(() -> Text.literal("[Momento Cuy] No existe la arena '" + id + "'."), false);
            return 0;
        }
        MinigameMatch match = MinigameManager.startMatch(arena.get());
        match.join(player);
        match.start(source.getServer());
        source.sendFeedback(() -> Text.literal("[Momento Cuy] Partida iniciada en '" + id + "'."), false);
        return 1;
    }

    private static int finishMatch(ServerCommandSource source, String id) {
        MinigameMatch match = MinigameManager.activeMatch(id);
        if (match == null) return 0;
        match.finish(source.getServer());
        source.sendFeedback(() -> Text.literal("[Momento Cuy] Partida '" + id + "' finalizada; participantes devueltos a la plataforma."), false);
        return 1;
    }

    private static int cancelMatch(ServerCommandSource source, String id) {
        MinigameMatch match = MinigameManager.activeMatch(id);
        if (match == null) return 0;
        match.cancel(source.getServer());
        source.sendFeedback(() -> Text.literal("[Momento Cuy] Partida '" + id + "' cancelada; participantes devueltos a la plataforma."), false);
        return 1;
    }
}
