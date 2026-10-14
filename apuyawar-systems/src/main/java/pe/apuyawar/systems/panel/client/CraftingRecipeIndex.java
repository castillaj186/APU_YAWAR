package pe.apuyawar.systems.panel.client;

import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.recipe.CraftingRecipe;
import net.minecraft.recipe.RecipeEntry;
import net.minecraft.recipe.RecipeManager;
import net.minecraft.recipe.RecipeType;
import net.minecraft.recipe.ShapedRecipe;
import net.minecraft.recipe.ShapelessRecipe;
import net.minecraft.registry.RegistryWrapper;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Índice de recetas de mesa de trabajo (con forma y sin forma, 2×2 y 3×3)
 * agrupadas por objeto resultante, construido desde el {@link RecipeManager}
 * REAL del mundo/servidor actual en el momento de crearse — nunca como lista
 * estática al cargar esta clase, porque las recetas pueden variar según el
 * servidor o una recarga de datapacks. {@link ItemCatalog} crea una instancia
 * nueva cada vez que se abre el Panel K (ver su documentación), por lo que
 * siempre refleja las recetas realmente sincronizadas en ese momento.
 *
 * <p>Deliberadamente solo {@link ShapedRecipe} y {@link ShapelessRecipe} —
 * exactamente "con forma" / "sin forma" de mesa de trabajo, tal como se pidió.
 * {@code RecipeType.CRAFTING} también incluye recetas "especiales" (teñido de
 * armadura/estandartes, clonado de libros/mapas, estrellas de fuegos
 * artificiales, etc.): son {@link CraftingRecipe} igual que las anteriores,
 * pero no tienen una cuadrícula fija de ingredientes que mostrar honestamente
 * en un visualizador de 3×3 — se excluyen a propósito, no por descuido.
 * Fundición, cocción, ahumado, fogata, corte de piedra, herrería, pociones y
 * comercio no son {@code RecipeType.CRAFTING} en absoluto, así que ya quedan
 * fuera de {@code recipeManager.listAllOfType(RecipeType.CRAFTING)} sin
 * necesitar un filtro adicional.
 */
final class CraftingRecipeIndex {
    private final List<CatalogEntry> entries;
    private final Map<Item, List<CraftingRecipe>> recipesByResult;

    CraftingRecipeIndex(RecipeManager recipeManager, RegistryWrapper.WrapperLookup registries) {
        Map<Item, List<CraftingRecipe>> byResult = new LinkedHashMap<>();
        Map<Item, ItemStack> iconByResult = new LinkedHashMap<>();
        for (RecipeEntry<CraftingRecipe> entry : recipeManager.listAllOfType(RecipeType.CRAFTING)) {
            CraftingRecipe recipe = entry.value();
            if (!(recipe instanceof ShapedRecipe) && !(recipe instanceof ShapelessRecipe)) continue;
            ItemStack result = recipe.getResult(registries);
            if (result.isEmpty()) continue;
            Item item = result.getItem();
            byResult.computeIfAbsent(item, i -> new ArrayList<>()).add(recipe);
            // El ícono representativo del catálogo es el resultado de la PRIMERA
            // receta encontrada para ese objeto; las variantes/alternativas se
            // consultan completas con recipesFor(item), no se pierden.
            iconByResult.putIfAbsent(item, result);
        }
        List<CatalogEntry> list = new ArrayList<>();
        for (var e : iconByResult.entrySet()) {
            ItemStack icon = e.getValue();
            list.add(new CatalogEntry(icon, icon.getName().getString()));
        }
        list.sort((a, b) -> a.name().compareToIgnoreCase(b.name()));
        this.entries = List.copyOf(list);
        this.recipesByResult = Map.copyOf(byResult);
    }

    /** Un {@link CatalogEntry} por objeto fabricable (no por receta — un objeto
     * con varias recetas alternativas aparece una sola vez), listo para
     * {@link CatalogGrid}/{@link ItemCellRenderer} sin ningún cambio. */
    List<CatalogEntry> entries() { return entries; }

    /** Todas las recetas válidas que producen este objeto (shaped + shapeless),
     * para navegar entre variantes en {@link CraftingRecipeView}. Lista vacía
     * si el objeto no es fabricable (no debería ocurrir para nada presente en
     * {@link #entries()}, pero nunca lanza por un objeto desconocido). */
    List<CraftingRecipe> recipesFor(Item item) { return recipesByResult.getOrDefault(item, List.of()); }
}
