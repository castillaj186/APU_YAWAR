package pe.apuyawar.systems.panel.client;

import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import net.minecraft.registry.Registries;

import java.util.ArrayList;
import java.util.List;

/**
 * Catálogo de "Objetos": todos los ítems registrados (Vanilla + los que agregue
 * APU YAWAR, como Sol PE). No existe todavía ninguna regla real de progresión para
 * bloquear ítems individualmente (se confirmó que no hay sistema previo) — por
 * ahora se muestran todos como disponibles, más unos espacios "?" de contenido
 * futuro, listos para conectar una regla de desbloqueo real más adelante.
 */
final class ItemCatalog {
    static final int FUTURE_SLOTS = 16;
    static final List<CatalogEntry> ENTRIES = build();
    private ItemCatalog() {}
    private static List<CatalogEntry> build() {
        List<CatalogEntry> list = new ArrayList<>();
        for (Item item : Registries.ITEM) {
            if (item == Items.AIR) continue;
            ItemStack stack = new ItemStack(item);
            list.add(new CatalogEntry(stack, stack.getName().getString()));
        }
        list.sort((a, b) -> a.name().compareToIgnoreCase(b.name()));
        return list;
    }
}
