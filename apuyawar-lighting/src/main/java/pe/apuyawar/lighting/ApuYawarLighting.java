package pe.apuyawar.lighting;

import net.minecraft.util.Identifier;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Optional;

/**
 * Punto de entrada público del motor: registrar qué bloques emiten luz RGB
 * y con qué {@link EmitterProfile}. Otros módulos (Momento Cuy, y a futuro
 * cristales/lámparas/portales) llaman a {@link #registerBlockEmitter} desde
 * su propio inicializador de CLIENTE — nunca desde código común, porque
 * este motor es puramente visual y no debe tocarse en un servidor
 * dedicado. No se activa RGB automáticamente para ningún bloque: sólo los
 * identificadores que alguien registra explícitamente aquí.
 */
public final class ApuYawarLighting {
    private static final Map<Identifier, EmitterProfile> BLOCK_EMITTERS = new LinkedHashMap<>();

    private ApuYawarLighting() {}

    public static void registerBlockEmitter(Identifier blockId, EmitterProfile profile) {
        BLOCK_EMITTERS.put(blockId, profile);
    }

    public static Optional<EmitterProfile> profileFor(Identifier blockId) {
        return Optional.ofNullable(BLOCK_EMITTERS.get(blockId));
    }

    public static boolean hasAnyEmitters() {
        return !BLOCK_EMITTERS.isEmpty();
    }

    public static Map<Identifier, EmitterProfile> registeredEmitters() {
        return BLOCK_EMITTERS;
    }
}
