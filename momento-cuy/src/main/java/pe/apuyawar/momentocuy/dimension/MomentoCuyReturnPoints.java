package pe.apuyawar.momentocuy.dimension;

import net.minecraft.server.MinecraftServer;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.util.WorldSavePath;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.InputStream;
import java.io.OutputStream;
import java.nio.file.AtomicMoveNotSupportedException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.HashMap;
import java.util.Map;
import java.util.Properties;
import java.util.UUID;

/**
 * Dónde estaba cada jugador antes de entrar a Momento Cuy, para poder
 * devolverlo exactamente ahí al salir — persistido en disco (no solo en
 * memoria) para sobrevivir un reinicio o una caída del servidor mientras
 * alguien seguía dentro. Adaptado del patrón ya probado de
 * {@code ReturnPoints} del mod antiguo (escritura atómica: archivo temporal +
 * renombrado, para no corromper el registro si el servidor se detiene a mitad
 * de una escritura).
 */
public final class MomentoCuyReturnPoints {
    private static final Logger LOGGER = LoggerFactory.getLogger("momento-cuy");
    private static final Map<UUID, Point> POINTS = new HashMap<>();

    private MomentoCuyReturnPoints() {}

    private static Path file(MinecraftServer server) {
        return server.getSavePath(WorldSavePath.ROOT).resolve("momento_cuy").resolve("return_points.properties");
    }

    public static void load(MinecraftServer server) {
        POINTS.clear();
        Path path = file(server);
        if (Files.exists(path)) {
            Properties p = new Properties();
            try (InputStream in = Files.newInputStream(path)) {
                p.load(in);
                for (String key : p.stringPropertyNames()) {
                    try {
                        POINTS.put(UUID.fromString(key), Point.parse(p.getProperty(key)));
                    } catch (RuntimeException e) {
                        LOGGER.warn("[MomentoCuy] Punto de retorno invalido para {}", key);
                    }
                }
                LOGGER.info("[MomentoCuy] {} puntos de retorno recuperados", POINTS.size());
            } catch (Exception e) {
                LOGGER.error("[MomentoCuy] No se pudo leer puntos de retorno", e);
            }
        }
    }

    public static void save(MinecraftServer server) {
        Path path = file(server);
        Path temp = path.resolveSibling(path.getFileName() + ".tmp");
        Properties p = new Properties();
        for (Map.Entry<UUID, Point> entry : POINTS.entrySet()) {
            p.setProperty(entry.getKey().toString(), entry.getValue().serialize());
        }
        try {
            Files.createDirectories(path.getParent());
            try (OutputStream out = Files.newOutputStream(temp)) {
                p.store(out, "MOMENTO CUY - lugares de retorno desde la dimension Momento Cuy");
            }
            try {
                Files.move(temp, path, StandardCopyOption.REPLACE_EXISTING, StandardCopyOption.ATOMIC_MOVE);
            } catch (AtomicMoveNotSupportedException e) {
                Files.move(temp, path, StandardCopyOption.REPLACE_EXISTING);
            }
        } catch (Exception e) {
            LOGGER.error("[MomentoCuy] No se pudo guardar puntos de retorno", e);
        }
    }

    public static void remember(MinecraftServer server, ServerPlayerEntity player) {
        POINTS.put(player.getUuid(), new Point(
                player.getServerWorld().getRegistryKey().getValue().toString(),
                player.getX(), player.getY(), player.getZ(), player.getYaw(), player.getPitch()));
        save(server);
    }

    public static Point get(UUID uuid) { return POINTS.get(uuid); }

    public static void forget(MinecraftServer server, UUID uuid) {
        POINTS.remove(uuid);
        save(server);
    }

    public static boolean has(UUID uuid) { return POINTS.containsKey(uuid); }

    public record Point(String dimension, double x, double y, double z, float yaw, float pitch) {
        String serialize() { return dimension + "|" + x + "|" + y + "|" + z + "|" + yaw + "|" + pitch; }

        static Point parse(String s) {
            String[] a = s.split("\\|", -1);
            if (a.length != 6) throw new IllegalArgumentException("Registro de retorno incompleto");
            return new Point(a[0], Double.parseDouble(a[1]), Double.parseDouble(a[2]), Double.parseDouble(a[3]),
                    Float.parseFloat(a[4]), Float.parseFloat(a[5]));
        }
    }
}
