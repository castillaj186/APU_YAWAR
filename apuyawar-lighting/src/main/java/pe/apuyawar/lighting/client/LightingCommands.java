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

    /** Huella de compilación inequívoca (sección "diagnóstico de clases
     * cargadas" del encargo) -- se cambia a mano cada vez que se investiga
     * un problema de clases viejas, para poder confirmar en el log que el
     * .class realmente cargado es el que se acaba de compilar, no una
     * copia cacheada de otra sesión. */
    private static final String BUILD_FINGERPRINT = "V3-classload-diag-a052613+04-namedElements-fix";

    private LightingCommands() {}

    static void register() {
        logClassOrigin();
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
                                .then(setFloatField("puntosfijos", c -> c.fixedPointDensityMultiplier, (c, v) -> c.fixedPointDensityMultiplier = v))
                                .then(setFloatField("particulasmoviles", c -> c.particleDensityMultiplier, (c, v) -> c.particleDensityMultiplier = v))
                                .then(setFloatField("fraccionestrellas", c -> c.celestialStarFraction, (c, v) -> c.celestialStarFraction = v))
                                .then(setFloatField("intensidadmaterial", c -> c.emissiveMaterialIntensity, (c, v) -> c.emissiveMaterialIntensity = v))
                                .then(setFloatField("dia", c -> c.dayIntensityMultiplier, (c, v) -> c.dayIntensityMultiplier = v))
                                .then(setFloatField("noche", c -> c.nightIntensityMultiplier, (c, v) -> c.nightIntensityMultiplier = v))
                                .then(setFloatField("intensidaddecal", c -> c.groundDecalIntensityMultiplier, (c, v) -> c.groundDecalIntensityMultiplier = v))
                                .then(setFloatField("radiofoco", c -> c.groundSpotRadius, (c, v) -> c.groundSpotRadius = v))
                                .then(setFloatField("intensidadfoco", c -> c.groundSpotIntensity, (c, v) -> c.groundSpotIntensity = v))
                                .then(ClientCommandManager.literal("distanciamax")
                                        .then(ClientCommandManager.argument("valor", FloatArgumentType.floatArg(4.0f, 128.0f))
                                                .executes(ctx -> {
                                                    LightingConfig config = ApuYawarLightingClient.config();
                                                    config.maxEffectDistance = FloatArgumentType.getFloat(ctx, "valor");
                                                    config.markCustom();
                                                    config.save();
                                                    feedback(ctx.getSource(), "distanciamax = " + config.maxEffectDistance);
                                                    return 1;
                                                })))
                                .then(ClientCommandManager.literal("distanciasimplificar")
                                        .then(ClientCommandManager.argument("valor", FloatArgumentType.floatArg(2.0f, 128.0f))
                                                .executes(ctx -> {
                                                    LightingConfig config = ApuYawarLightingClient.config();
                                                    config.simplifyDetailBeyondDistance = FloatArgumentType.getFloat(ctx, "valor");
                                                    config.markCustom();
                                                    config.save();
                                                    feedback(ctx.getSource(), "distanciasimplificar = " + config.simplifyDetailBeyondDistance);
                                                    return 1;
                                                })))
                                .then(ClientCommandManager.literal("transicion")
                                        .then(ClientCommandManager.argument("ticks", IntegerArgumentType.integer(1, 200))
                                                .executes(ctx -> {
                                                    LightingConfig config = ApuYawarLightingClient.config();
                                                    config.fadeTransitionTicks = IntegerArgumentType.getInteger(ctx, "ticks");
                                                    config.markCustom();
                                                    config.save();
                                                    feedback(ctx.getSource(), "transicion = " + config.fadeTransitionTicks + " ticks");
                                                    return 1;
                                                })))
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
                        .then(ClientCommandManager.literal("decals")
                                .executes(ctx -> {
                                    LightingConfig config = ApuYawarLightingClient.config();
                                    config.groundDecalsEnabled = !config.groundDecalsEnabled;
                                    config.save();
                                    feedback(ctx.getSource(), "Decals de suelo " + (config.groundDecalsEnabled ? "activados" : "desactivados"));
                                    return 1;
                                }))
                        .then(ClientCommandManager.literal("materialemisivo")
                                .executes(ctx -> {
                                    LightingConfig config = ApuYawarLightingClient.config();
                                    config.emissiveMaterialEnabled = !config.emissiveMaterialEnabled;
                                    config.save();
                                    feedback(ctx.getSource(), "Follaje emisivo real " + (config.emissiveMaterialEnabled ? "activado" : "desactivado"));
                                    return 1;
                                }))
                        .then(ClientCommandManager.literal("bloomreal")
                                .executes(ctx -> {
                                    LightingConfig config = ApuYawarLightingClient.config();
                                    config.realBloomEnabled = !config.realBloomEnabled;
                                    config.save();
                                    feedback(ctx.getSource(), "Bloom REAL de framebuffer " + (config.realBloomEnabled ? "activado" : "desactivado")
                                            + " -- sección V4 fase 3, no verificado visualmente todavía en este entorno; pruébalo con cuidado.");
                                    return 1;
                                }))
                        .then(ClientCommandManager.literal("debug")
                                .then(ClientCommandManager.literal("emisores").executes(LightingCommands::debugEmitters))
                                .then(ClientCommandManager.literal("posiciones").executes(LightingCommands::debugPositions))
                                .then(ClientCommandManager.literal("focos").executes(LightingCommands::debugGroundSpots))
                                .then(ClientCommandManager.literal("materialemisivo").executes(LightingCommands::debugEmissiveMaterial))
                                .then(ClientCommandManager.literal("hojas")
                                        .executes(ctx -> debugRealLeafCounts(ctx, 24))
                                        .then(ClientCommandManager.argument("radio", IntegerArgumentType.integer(4, 80))
                                                .executes(ctx -> debugRealLeafCounts(ctx, IntegerArgumentType.getInteger(ctx, "radio")))))
                                .then(ClientCommandManager.literal("puntos").executes(ctx -> {
                                    LightingConfig config = ApuYawarLightingClient.config();
                                    config.debugHideFixedPoints = !config.debugHideFixedPoints;
                                    feedback(ctx.getSource(), "Puntos/halos de copa " + (config.debugHideFixedPoints ? "OCULTOS (debug)" : "visibles"));
                                    return 1;
                                })))
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
                profile.intensity(), profile.radius(), profile.falloff(), EmitterType.SOLID, 1, 99, true, null, false));
        feedback(ctx.getSource(), "Emisor de prueba colocado en " + pos.toShortString());
        return 1;
    }

    private static int showStatus(com.mojang.brigadier.context.CommandContext<FabricClientCommandSource> ctx) {
        LightingConfig c = ApuYawarLightingClient.config();
        feedback(ctx.getSource(), String.format(java.util.Locale.ROOT,
                "RGB=%s perfil=%s intensidad=%.2f radio=%.2f bloom=%.2f radioBloom=%.2f puntosFijos=%.2f dia=%.2f noche=%.2f maxLuces=%d activos=%d",
                c.rgbEnabled, c.quality, c.lightIntensityMultiplier, c.lightRadiusMultiplier, c.bloomIntensity,
                c.bloomRadiusMultiplier, c.fixedPointDensityMultiplier, c.dayIntensityMultiplier, c.nightIntensityMultiplier,
                c.maxActiveLights, EmitterTracker.active().size()));
        feedback(ctx.getSource(), String.format(java.util.Locale.ROOT,
                "decals=%s intensidadDecal=%.2f radioFoco=%.2f intensidadFoco=%.2f distanciaMax=%.1f simplificarDesde=%.1f transicion=%dticks puntosOcultos(debug)=%s",
                c.groundDecalsEnabled, c.groundDecalIntensityMultiplier, c.groundSpotRadius, c.groundSpotIntensity,
                c.maxEffectDistance, c.simplifyDetailBeyondDistance, c.fadeTransitionTicks, c.debugHideFixedPoints));
        feedback(ctx.getSource(), String.format(java.util.Locale.ROOT,
                "materialEmisivo=%s intensidadMaterial=%.2f particulasMoviles=%.2f fraccionEstrellas=%.2f bloomReal=%s",
                c.emissiveMaterialEnabled, c.emissiveMaterialIntensity, c.particleDensityMultiplier, c.celestialStarFraction,
                c.realBloomEnabled));
        return 1;
    }

    /** V4, fase 1: fuerza la generación de la máscara emisiva de cada
     * perfil registrado que tenga {@code emissiveMaskSource}, sin
     * necesitar encontrar un árbol real en el mundo -- reporta si cada
     * máscara se generó, y cuántos píxeles de la textura base
     * calificaron como emisivos (vía el propio log [DIAG] de
     * {@code EmissiveMaskGenerator}, que ya imprime el identificador
     * resultante). */
    private static int debugEmissiveMaterial(com.mojang.brigadier.context.CommandContext<FabricClientCommandSource> ctx) {
        var client = net.minecraft.client.MinecraftClient.getInstance();
        var registered = pe.apuyawar.lighting.ApuYawarLighting.registeredEmitters();
        int checked = 0;
        int ok = 0;
        for (var entry : registered.entrySet()) {
            EmitterProfile profile = entry.getValue();
            if (!profile.hasEmissiveMask()) continue;
            checked++;
            int rgb = (clampByte01(profile.red()) << 16) | (clampByte01(profile.green()) << 8) | clampByte01(profile.blue());
            var result = EmissiveMaskGenerator.getOrCreate(client.getResourceManager(), profile.emissiveMaskSource(), rgb);
            boolean success = result.isPresent();
            if (success) ok++;
            feedback(ctx.getSource(), entry.getKey() + " -> " + (success ? "OK (" + result.get() + ")" : "FALLÓ (ver log del servidor/cliente)"));
        }
        feedback(ctx.getSource(), checked + " perfiles con máscara emisiva registrados, " + ok + " generados correctamente.");
        return 1;
    }

    private static int clampByte01(float v) {
        return Math.max(0, Math.min(255, (int) (v * 255f)));
    }

    /** Sección 17 del encargo: "mostrar emisores" -- un informe bajo
     * demanda, no un registro continuo en el log. */
    private static int debugEmitters(com.mojang.brigadier.context.CommandContext<FabricClientCommandSource> ctx) {
        var active = EmitterTracker.active();
        var spots = EmitterTracker.groundSpots();
        feedback(ctx.getSource(), "Puntos de copa activos: " + active.size() + " -- focos de suelo: " + spots.size());
        return 1;
    }

    /** Sección 17 del encargo: "mostrar posiciones" -- imprime la
     * coordenada mundial de cada emisor activo, bajo demanda, una sola
     * vez por invocación (no continuamente). */
    private static int debugPositions(com.mojang.brigadier.context.CommandContext<FabricClientCommandSource> ctx) {
        var active = EmitterTracker.active();
        if (active.isEmpty()) {
            feedback(ctx.getSource(), "Sin puntos de copa activos ahora mismo.");
            return 1;
        }
        int shown = 0;
        for (var emitter : active) {
            if (shown >= 10) {
                feedback(ctx.getSource(), "... (" + (active.size() - shown) + " más, no mostrados)");
                break;
            }
            feedback(ctx.getSource(), emitter.pos().toShortString()
                    + " fundido=" + String.format(java.util.Locale.ROOT, "%.2f", emitter.fade()));
            shown++;
        }
        return 1;
    }

    /** V2: "mostrar focos" -- a diferencia de los puntos de copa (uno por
     * hoja), los focos de suelo son como mucho uno por celda de 8×8
     * bloques (ver {@code EmitterTracker}), así que normalmente son muy
     * pocos incluso con un bosque entero a la vista. */
    private static int debugGroundSpots(com.mojang.brigadier.context.CommandContext<FabricClientCommandSource> ctx) {
        var spots = EmitterTracker.groundSpots();
        if (spots.isEmpty()) {
            feedback(ctx.getSource(), "Sin focos de suelo activos ahora mismo.");
            return 1;
        }
        for (var spot : spots) {
            feedback(ctx.getSource(), "ancla=" + spot.anchorPos().toShortString()
                    + " sueloY=" + spot.groundY()
                    + " fundido=" + String.format(java.util.Locale.ROOT, "%.2f", spot.fade()));
        }
        return 1;
    }

    /** "Error 2" del encargo de corrección: distingue el número de
     * emisores realmente registrados en el mundo (hoja por hoja, cubo
     * fijo de escaneo SIN caché -- ver
     * {@link EmitterTracker#debugCountRealEmitters}) del número que
     * {@code EmitterTracker.active()} termina renderizando -- para poder
     * confirmar si una cifra baja viene de "pocas hojas reales", de "el
     * escaneo no las encuentra" o de "el límite de maxluces las recorta". */
    private static int debugRealLeafCounts(com.mojang.brigadier.context.CommandContext<FabricClientCommandSource> ctx, int radius) {
        var client = net.minecraft.client.MinecraftClient.getInstance();
        if (client.world == null || client.player == null) {
            feedback(ctx.getSource(), "Sin mundo cargado.");
            return 1;
        }
        var counts = EmitterTracker.debugCountRealEmitters(client.world, client.player.getBlockPos(), radius);
        if (counts.isEmpty()) {
            feedback(ctx.getSource(), "0 bloques emisores reales en un radio de " + radius + " bloques.");
        } else {
            int total = 0;
            for (var entry : counts.entrySet()) {
                feedback(ctx.getSource(), "  " + entry.getKey() + " = " + entry.getValue());
                total += entry.getValue();
            }
            feedback(ctx.getSource(), "Total hojas emisoras reales (radio " + radius + "): " + total);
        }
        LightingConfig config = ApuYawarLightingClient.config();
        feedback(ctx.getSource(), "Puntos de copa renderizados ahora mismo: " + EmitterTracker.active().size()
                + " (límite maxluces=" + config.maxActiveLights + ", distanciaMax=" + config.maxEffectDistance + ")");
        return 1;
    }

    /** Diagnóstico pedido explícitamente por el equipo tras reportar que
     * los comandos nuevos de la V3 no aparecían en el juego aunque el
     * build fuera exitoso: imprime, de forma inequívoca, de qué JAR o
     * directorio viene de verdad la clase {@code LightingCommands} que
     * esta JVM cargó, y lista TODAS las copias de ese recurso que el
     * classloader puede ver -- si hay más de una, confirma una colisión
     * de classpath, no sólo una caché estática sin usar. */
    private static void logClassOrigin() {
        LOGGER.info("[DIAG] BUILD_FINGERPRINT={}", BUILD_FINGERPRINT);
        logOrigin(LightingCommands.class, "LightingCommands");
        logOrigin(ApuYawarLightingClient.class, "ApuYawarLightingClient");
    }

    private static void logOrigin(Class<?> type, String label) {
        try {
            var codeSource = type.getProtectionDomain().getCodeSource();
            LOGGER.info("[DIAG] {} codeSource={}", label,
                    codeSource != null ? codeSource.getLocation() : "null (bootstrap/sin CodeSource)");
        } catch (Exception e) {
            LOGGER.warn("[DIAG] {} no se pudo leer codeSource: {}", label, e.toString());
        }

        String resourcePath = type.getName().replace('.', '/') + ".class";
        try {
            java.net.URL direct = type.getClassLoader().getResource(resourcePath);
            LOGGER.info("[DIAG] {} getResource({})={}", label, resourcePath, direct);
        } catch (Exception e) {
            LOGGER.warn("[DIAG] {} getResource falló: {}", label, e.toString());
        }

        try {
            var resources = type.getClassLoader().getResources(resourcePath);
            int count = 0;
            while (resources.hasMoreElements()) {
                java.net.URL url = resources.nextElement();
                LOGGER.info("[DIAG] {} getResources()[{}]={}", label, count, url);
                count++;
            }
            if (count > 1) {
                LOGGER.warn("[DIAG] {} tiene {} copias visibles en el classpath -- COLISION real, "
                        + "la que gane depende del orden del classpath, no del commit mas reciente.", label, count);
            } else if (count == 0) {
                LOGGER.warn("[DIAG] {} getResources() no encontro ninguna copia (inesperado)", label);
            }
        } catch (Exception e) {
            LOGGER.warn("[DIAG] {} getResources falló: {}", label, e.toString());
        }
    }

    private static void feedback(FabricClientCommandSource source, String message) {
        source.sendFeedback(Text.literal("[APU YAWAR Lighting] " + message));
    }
}
