package pe.apuyawar.world;

import net.fabricmc.api.ModInitializer;
import net.minecraft.item.Item;
import net.minecraft.registry.Registries;
import net.minecraft.registry.Registry;
import net.minecraft.util.Identifier;

/**
 * Módulo de contenido ORIGINAL, empezando con un objeto de prueba.
 * Los cuys, herramientas, biomas, minerales y dimensiones se crearán por etapas.
 */
public final class ApuYawarWorld implements ModInitializer {
    public static final String MOD_ID="apuyawar_world";
    public static final Item FRAGMENTO_APU = Registry.register(
        Registries.ITEM, Identifier.of(MOD_ID,"fragmento_apu"),new Item(new Item.Settings()));
    @Override public void onInitialize() {
        // La inicialización estática registra el objeto en el registro de Minecraft.
    }
}
