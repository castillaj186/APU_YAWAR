package pe.apuyawar.systems.items;

import net.fabricmc.fabric.api.itemgroup.v1.ItemGroupEvents;
import net.minecraft.item.Item;
import net.minecraft.item.ItemGroups;
import net.minecraft.registry.Registries;
import net.minecraft.registry.Registry;
import net.minecraft.util.Identifier;

/** Moneda real del servidor; única fuente de verdad del ítem "Sol PE". */
public final class ModItems {
    public static final Item SOL_PE = new Item(new Item.Settings().maxCount(64));
    private ModItems() {}
    public static void initialize() {
        Registry.register(Registries.ITEM, Identifier.of("apuyawar_systems", "sol_pe"), SOL_PE);
        ItemGroupEvents.modifyEntriesEvent(ItemGroups.INGREDIENTS).register(entries -> entries.add(SOL_PE));
    }
}
