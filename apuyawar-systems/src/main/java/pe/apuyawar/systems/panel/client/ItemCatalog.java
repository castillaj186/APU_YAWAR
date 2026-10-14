package pe.apuyawar.systems.panel.client;

import net.minecraft.client.MinecraftClient;
import net.minecraft.item.Item;
import net.minecraft.recipe.CraftingRecipe;

import java.util.List;

/**
 * Catálogo de "Objetos": antes recorría {@code Registries.ITEM} completo
 * (mostraba cualquier ítem registrado, incluidos los que no se pueden obtener
 * de ninguna forma por crafteo). Ahora muestra únicamente objetos con al menos
 * una receta real de mesa de trabajo — ver {@link CraftingRecipeIndex}.
 *
 * <p>Ya no es una lista estática al cargar la clase: las recetas dependen del
 * {@code RecipeManager} del mundo/servidor actual (pueden variar entre
 * servidores o cambiar con un datapack recargado), así que esta clase pasó de
 * ser un {@code static final} a construirse una vez por cada apertura del
 * Panel K, en el constructor de {@link SurvivalPanelScreen} — el mismo patrón
 * de ciclo de vida que ya usa la caché de entidades de {@link MobCellRenderer}
 * (vive y muere con la pantalla, sin necesitar invalidación explícita).
 */
final class ItemCatalog {
    static final int FUTURE_SLOTS = 16;
    private final CraftingRecipeIndex index;

    ItemCatalog() {
        var client = MinecraftClient.getInstance();
        // Defensivo: el Panel K solo se abre tras confirmar con el servidor
        // (PanelOpenRequestPayload/PanelOpenResponsePayload) estando en un
        // mundo, así que client.world no debería ser null aquí — pero si
        // alguna vez lo fuera, un catálogo vacío es preferible a un crash.
        this.index = client.world != null
                ? new CraftingRecipeIndex(client.world.getRecipeManager(), client.world.getRegistryManager())
                : null;
    }

    List<CatalogEntry> entries() { return index != null ? index.entries() : List.of(); }
    List<CraftingRecipe> recipesFor(Item item) { return index != null ? index.recipesFor(item) : List.of(); }
}
