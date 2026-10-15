package pe.apuyawar.lighting.client;

import com.mojang.brigadier.arguments.FloatArgumentType;
import com.mojang.brigadier.arguments.IntegerArgumentType;
import com.mojang.brigadier.arguments.StringArgumentType;
import net.fabricmc.fabric.api.client.command.v2.ClientCommandManager;
import net.fabricmc.fabric.api.client.command.v2.ClientCommandRegistrationCallback;
import net.fabricmc.fabric.api.client.command.v2.FabricClientCommandSource;
import net.minecraft.text.Text;
import net.minecraft.util.hit.BlockHitResult;
import net.minecraft.util.hit.HitResult;
import net.minecraft.util.math.BlockPos;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import pe.apuyawar.lighting.EmitterProfile;
import pe.apuyawar.lighting.EmitterType;
import pe.apuyawar.lighting.config.LightingConfig;
import pe.apuyawar.lighting.config.LightingQuality;

/**
 * Comando de cliente {@code /apuyawarlighting} — control en caliente de la
 * configuración (sección 11 del encargo: "sin recompilar", "aplicar los
 * cambios sin reiniciar"). No reutiliza ninguna tecla del Panel K ni añade
 * overlays de depuración permanentes: es sólo un comando de chat/consola,
 * igual que cualquier otro comando de cliente de Fabric.
 *
 * <p>Logs temporales de diagnóstico (sección "DIAG" del encargo de
 * corrección): quedan hasta confirmar que el comando aparece de verdad en
 * las sugerencias del chat del equipo, luego se quitan en un commit
 * aparte.</p>
 */
final class LightingCommands {
    private static final Logger LOGGER = LoggerFactory.getLogger("apuyawar-lighting");

    private LightingCommands() {}

    static void register() {
        LOGGER.info("[DIAG] LightingCommands.register() invocado");
        ClientCommandRegistrationCallback.EVENT.register((dispatcher, registryAccess) -> {
            LOGGER.info("[DIAG] ClientCommandRegistrationCallback disparado (dispatcher={})", dispatcher);
            // Comando mínimo de diagnóstico, sin argumentos, para aislar si
            // el problema es el registro en sí o el árbol anidado de
            // /apuyawarlighting -- se quita junto con el resto de logs DIAG.
            dispatcher.register(ClientCommandManager.literal("ayldiag").executes(ctx -> {
                feedback(ctx.getSource(), "ayldiag OK -- el registro de comandos de cliente funciona");
                return 1;
            }));
            LOGGER.info("[DIAG] /ayldiag registrado");

            var node = dispatcher.register(
                ClientCommandManager.literal("apuyawarlighting")
                        .then(ClientCommandManager.literal("toggle").executes(ctx -> {
                            LightingConfig config = ApuYawarLightingClient.config();
                            config.rgbEnabled = !config.rgbEnabled;
                            config.save();
                            feedback(ctx.getSource(), "RGB " + (config.rgbEnabled ? "activado" : "desactivado"));
                            return 1;
                        }))
                        .then(ClientCommandManager.literal("preset")
                                .then(ClientCommandManager.argument("nombre", StringArgumentType.word())
                                        .executes(ctx -> {
                                            String name = StringArgumentType.getString(ctx, "nombre");
                                            LightingConfig config = ApuYawarLightingClient.config();
                                            try {
                                                config.applyPreset(LightingQuality.valueOf(name.toUpperCase(java.util.Locale.ROOT)));
                                                config.save();
                                                feedback(ctx.getSource(), "Perfil aplicado: " + config.quality);
                                            } catch (IllegalArgumentException e) {
                                                ctx.getSource().sendError(Text.literal("Perfiles válidos: low, medium, high, custom"));
                                            }
                                            return 1;
                                        })))
                        .then(ClientCommandManager.literal("set")
                                .then(setFloatField("intensidad", c -> c.lightIntensityMultiplier, (c, v) -> c.lightIntensityMultiplier = v))
                                .then(setFloatField("radio", c -> c.lightRadiusMultiplier, (c, v) -> c.lightRadiusMultiplier = v))
                                .then(setFloatField("bloom", c -> c.bloomIntensity, (c, v) -> c.bloomIntensity = v))
                                .then(setFloatField("radiobloom", c -> c.bloomRadiusMultiplier, (c, v) -> c.bloomRadiusMultiplier = v))
                                .then(setFloatField("particulas", c -> c.particleDensityMultiplier, (c, v) -> c.particleDensityMultiplier = v))
                                .then(setFloatField("dia", c -> c.dayIntensityMultiplier, (c, v) -> c.dayIntensityMultiplier = v))
                                .then(setFloatField("noche", c -> c.nightIntensityMultiplier, (c, v) -> c.nightIntensityMultiplier = v))
                                .then(ClientCommandManager.literal("maxluces")
                                        .then(ClientCommandManager.argument("valor", IntegerArgumentType.integer(1, 512))
                                                .executes(ctx -> {
                                                    LightingConfig config = ApuYawarLightingClient.config();
                                                    config.maxActiveLights = IntegerArgumentType.getInteger(ctx, "valor");
                                                    config.markCustom();
                                                    config.save();
                                                    feedback(ctx.getSource(), "maxluces = " + config.maxActiveLights);
                                                    return 1;
                                                }))))
                        .then(ClientCommandManager.literal("test")
                                .executes(LightingCommands::toggleTestEmitter))
                        .then(ClientCommandManager.literal("estado")
                                .executes(LightingCommands::showStatus)));
            LOGGER.info("[DIAG] /apuyawarlighting registrado, nodo='{}'", node.getName());
        });
    }

    private static com.mojang.brigadier.builder.LiteralArgumentBuilder<FabricClientCommandSource> setFloatField(
            String name, java.util.function.Function<LightingConfig, Float> getter,
            java.util.function.BiConsumer<LightingConfig, Float> setter) {
        return ClientCommandManager.<FabricClientCommandSource>literal(name)
                .then(ClientCommandManager.argument("valor", FloatArgumentType.floatArg(0.0f, 10.0f))
                        .executes(ctx -> {
                            LightingConfig config = ApuYawarLightingClient.config();
                            float value = FloatArgumentType.getFloat(ctx, "valor");
                            setter.accept(config, value);
                            config.markCustom();
                            config.save();
                            feedback(ctx.getSource(), name + " = " + value);
                            return 1;
                        }));
    }

    private static int toggleTestEmitter(com.mojang.brigadier.context.CommandContext<FabricClientCommandSource> ctx) {
        if (EmitterTracker.hasTestEmitter()) {
            EmitterTracker.clearTestEmitter();
            feedback(ctx.getSource(), "Emisor de prueba retirado");
            return 1;
        }
        var player = ctx.getSource().getPlayer();
        HitResult target = player.raycast(12.0, 1.0f, false);
        BlockPos pos = target instanceof BlockHitResult blockHit
                ? blockHit.getBlockPos().offset(blockHit.getSide())
                : player.getBlockPos().up(2);
        int color = ApuYawarLightingClient.config().testEmitterColor;
        EmitterProfile profile = EmitterProfile.foliage(color, 1.0f, 6.0f, 1).withAnimated();
        // El emisor de prueba no es realmente "foliage" (es un único punto
        // fijo), pero EmitterProfile.foliage() ya da el color/radio/puntos
        // correctos para un único punto -- y EmitterTracker.active() lo
        // añade siempre, sin pasar por el escaneo de bloques.
        EmitterTracker.setTestEmitter(pos, new EmitterProfile(
                profile.red(), profile.green(), profile.blue(), null, null, null,
                profile.intensity(), profile.radius(), profile.falloff(), EmitterType.SOLID, 1, 99, true));
        feedback(ctx.getSource(), "Emisor de prueba colocado en " + pos.toShortString());
        return 1;
    }

    private static int showStatus(com.mojang.brigadier.context.CommandContext<FabricClientCommandSource> ctx) {
        LightingConfig c = ApuYawarLightingClient.config();
        feedback(ctx.getSource(), String.format(java.util.Locale.ROOT,
                "RGB=%s perfil=%s intensidad=%.2f radio=%.2f bloom=%.2f radioBloom=%.2f particulas=%.2f dia=%.2f noche=%.2f maxLuces=%d activos=%d",
                c.rgbEnabled, c.quality, c.lightIntensityMultiplier, c.lightRadiusMultiplier, c.bloomIntensity,
                c.bloomRadiusMultiplier, c.particleDensityMultiplier, c.dayIntensityMultiplier, c.nightIntensityMultiplier,
                c.maxActiveLights, EmitterTracker.active().size()));
        return 1;
    }

    private static void feedback(FabricClientCommandSource source, String message) {
        source.sendFeedback(Text.literal("[APU YAWAR Lighting] " + message));
    }
}
