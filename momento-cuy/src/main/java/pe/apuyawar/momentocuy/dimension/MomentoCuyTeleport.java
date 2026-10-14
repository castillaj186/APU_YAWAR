package pe.apuyawar.momentocuy.dimension;

import net.minecraft.registry.RegistryKey;
import net.minecraft.registry.RegistryKeys;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.sound.SoundCategory;
import net.minecraft.sound.SoundEvents;
import net.minecraft.text.Text;
import net.minecraft.util.Identifier;
import net.minecraft.world.World;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Entrada/salida de Momento Cuy, compartida por {@code MomentoCuyCommands} y
 * {@code MomentoCuyPortalBlock} — una sola implementación, no dos caminos que
 * puedan desincronizarse. "Controlada por el servidor": el cliente nunca
 * decide el destino, solo pide entrar/salir y el servidor resuelve con sus
 * propias reglas (si existe la dimensión, si hay suelo seguro, etc.).
 */
public final class MomentoCuyTeleport {
    private static final Logger LOGGER = LoggerFactory.getLogger("momento-cuy");

    private MomentoCuyTeleport() {}

    public static EnterResult enter(ServerPlayerEntity player) {
        MinecraftServer server = player.getServer();
        ServerWorld to = server.getWorld(MomentoCuyDimension.WORLD);
        if (to == null) return EnterResult.DIMENSION_NOT_LOADED;
        if (player.getServerWorld().getRegistryKey().equals(MomentoCuyDimension.WORLD)) return EnterResult.ALREADY_THERE;

        MomentoCuyLobby.ensure(to);
        if (!MomentoCuyLobby.hasSafeGroundAtEntry(to)) {
            // Proteccion explicita contra caer al vacio al aparecer: si el
            // suelo de la plataforma no esta donde deberia, se aborta en vez
            // de teletransportar a ciegas.
            LOGGER.error("[MomentoCuy] Suelo de entrada ausente; se cancela el teletransporte de {}", player.getGameProfile().getName());
            return EnterResult.UNSAFE_DESTINATION;
        }

        MomentoCuyReturnPoints.remember(server, player);
        player.teleport(to, MomentoCuyLobby.ENTRY_X, MomentoCuyLobby.ENTRY_Y, MomentoCuyLobby.ENTRY_Z, 180.0F, 0.0F);
        to.playSound(null, player.getBlockPos(), SoundEvents.BLOCK_AMETHYST_BLOCK_CHIME, SoundCategory.AMBIENT, 0.8F, 1.0F);
        return EnterResult.OK;
    }

    public static LeaveResult leave(ServerPlayerEntity player) {
        if (!player.getServerWorld().getRegistryKey().equals(MomentoCuyDimension.WORLD)) return LeaveResult.NOT_THERE;
        MinecraftServer server = player.getServer();
        MomentoCuyReturnPoints.Point point = MomentoCuyReturnPoints.get(player.getUuid());
        ServerWorld destination;
        double x, y, z;
        float yaw, pitch;
        if (point == null) {
            // Sin punto de retorno guardado (ej. se unio ya dentro con un
            // teletransporte de operador): vuelve al spawn del Overworld en
            // vez de dejarlo varado, en vez de negarse a actuar.
            destination = server.getOverworld();
            var spawn = destination.getSpawnPos();
            x = spawn.getX() + 0.5; y = spawn.getY(); z = spawn.getZ() + 0.5; yaw = 0; pitch = 0;
        } else {
            RegistryKey<World> key = RegistryKey.of(RegistryKeys.WORLD, Identifier.of(point.dimension()));
            ServerWorld world = server.getWorld(key);
            if (world == null) {
                destination = server.getOverworld();
                var spawn = destination.getSpawnPos();
                x = spawn.getX() + 0.5; y = spawn.getY(); z = spawn.getZ() + 0.5; yaw = 0; pitch = 0;
            } else {
                destination = world;
                x = point.x(); y = point.y(); z = point.z(); yaw = point.yaw(); pitch = point.pitch();
            }
        }
        player.teleport(destination, x, y, z, yaw, pitch);
        MomentoCuyReturnPoints.forget(server, player.getUuid());
        return LeaveResult.OK;
    }

    public enum EnterResult {
        OK(Text.literal("Bienvenido a Momento Cuy.")),
        ALREADY_THERE(Text.literal("Ya estas en Momento Cuy.")),
        DIMENSION_NOT_LOADED(Text.literal("La dimension Momento Cuy no esta cargada.")),
        UNSAFE_DESTINATION(Text.literal("No se pudo entrar: la plataforma de llegada no esta en un estado seguro."));
        public final Text message;
        EnterResult(Text message) { this.message = message; }
    }

    public enum LeaveResult {
        OK(Text.literal("Has vuelto a tu lugar original.")),
        NOT_THERE(Text.literal("No estas en Momento Cuy."));
        public final Text message;
        LeaveResult(Text message) { this.message = message; }
    }
}
