package pe.apuyawar.systems.panel.client;

import net.minecraft.item.ItemStack;

/** Una entrada visual de catálogo (mob u objeto), representada por el ícono de un ítem real. */
record CatalogEntry(ItemStack icon, String name) {}
