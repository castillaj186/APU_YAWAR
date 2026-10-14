package pe.apuyawar.systems.panel.client;

import net.minecraft.entity.EntityType;
import net.minecraft.item.ItemStack;
import net.minecraft.item.SpawnEggItem;

import java.util.ArrayList;
import java.util.List;

/**
 * Catálogo de "Cambios y Mobs": criaturas normales de Vanilla (toda la que tenga
 * huevo de generación — eso ya excluye naturalmente al Ender Dragon y al Wither,
 * que no tienen huevo). El Ender Dragon y el Wither se excluyen también de forma
 * explícita por si algún mod les agrega uno.
 *
 * El contenido futuro (desarrollado por el resto del equipo) se representa con
 * espacios "?" vacíos — ver {@link #FUTURE_SLOTS} — sin inventar ningún registro.
 */
final class MobCatalog {
    /** Cuántos espacios "?" de contenido futuro mostrar al final de la cuadrícula. */
    static final int FUTURE_SLOTS = 16;
    static final List<CatalogEntry> ENTRIES = build();
    private MobCatalog() {}
    private static List<CatalogEntry> build() {
        List<CatalogEntry> list = new ArrayList<>();
        for (SpawnEggItem egg : SpawnEggItem.getAll()) {
            EntityType<?> type = egg.getEntityType(new ItemStack(egg));
            if (type == EntityType.ENDER_DRAGON || type == EntityType.WITHER) continue;
            ItemStack stack = new ItemStack(egg);
            list.add(new CatalogEntry(stack, stack.getName().getString()));
        }
        list.sort((a, b) -> a.name().compareToIgnoreCase(b.name()));
        return list;
    }
}
