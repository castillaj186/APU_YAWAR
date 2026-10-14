package pe.apuyawar.systems.panel.client;

import net.minecraft.client.font.TextRenderer;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.item.ItemStack;
import net.minecraft.recipe.CraftingRecipe;
import net.minecraft.recipe.Ingredient;
import net.minecraft.recipe.ShapedRecipe;
import net.minecraft.registry.RegistryWrapper;

import java.util.List;

/**
 * Visualizador de UNA receta de mesa de trabajo: ingredientes reales en su
 * disposición real (incluidos los espacios vacíos), una flecha y el resultado.
 * Utilidad sin estado (todo el estado — qué objeto, qué receta alternativa —
 * vive en {@link SurvivalPanelScreen}) para que la misma implementación sirva
 * desde la pestaña Objetos y desde Búsqueda, tal como se pidió. Es solo una
 * consulta visual: nunca llama a {@code craft(...)} ni modifica ningún
 * inventario — ningún material se consume realmente.
 */
final class CraftingRecipeView {
    private static final int SLOT = 18, SLOT_GAP = 2;
    private static final int ARROW_W = 26;
    private static final int RESULT_SLOT = 24;
    private static final int NAME_Y = 0, NAV_Y = 12, GRID_Y = 26;

    private CraftingRecipeView() {}

    /** Ancho total que ocupa el visualizador (para centrarlo si hay espacio de sobra). */
    static int contentWidth() { return gridWidth(3) + ARROW_W + RESULT_SLOT; }

    private static int gridWidth(int cols) { return cols * SLOT + (cols - 1) * SLOT_GAP; }
    private static int gridHeight(int rows) { return rows * SLOT + (rows - 1) * SLOT_GAP; }

    /** Rectángulo del botón "< Volver" (misma posición para Objetos y Búsqueda). */
    static Rect backButton(int x, int y) { return new Rect(x, y + NAV_Y, 54, 10); }
    /** Rectángulo de "<" para ir a la receta alternativa anterior; vacío (ancho 0) si no aplica. */
    static Rect prevButton(int x, int y, int w, int recipeCount) {
        return recipeCount > 1 ? new Rect(x + w - 58, y + NAV_Y, 10, 10) : Rect.EMPTY;
    }
    /** Rectángulo de ">" para ir a la receta alternativa siguiente; vacío si no aplica. */
    static Rect nextButton(int x, int y, int w, int recipeCount) {
        return recipeCount > 1 ? new Rect(x + w - 14, y + NAV_Y, 10, 10) : Rect.EMPTY;
    }

    record Rect(int x, int y, int w, int h) {
        static final Rect EMPTY = new Rect(0, 0, 0, 0);
        boolean contains(double mx, double my) {
            return w > 0 && h > 0 && mx >= x && mx < x + w && my >= y && my < y + h;
        }
    }

    /** Dibuja el visualizador completo: cabecera (Volver + navegación entre
     * recetas alternativas), nombre del objeto, y la cuadrícula real de
     * ingredientes → flecha → resultado. {@code recipeIndex}/{@code recipeCount}
     * solo controlan el texto/visibilidad de los botones de navegación; avanzar
     * entre recetas lo decide quien llama (ver {@code SurvivalPanelScreen}). */
    static void render(DrawContext ctx, TextRenderer tr, RegistryWrapper.WrapperLookup registries,
                        CraftingRecipe recipe, int recipeIndex, int recipeCount,
                        String resultName, int x, int y, int w, int mouseX, int mouseY) {
        boolean backHover = backButton(x, y).contains(mouseX, mouseY);
        ctx.drawText(tr, "« Volver", x, y + NAV_Y, backHover ? 0xFFFFFFFF : 0xFFAAAAAA, false);
        if (recipeCount > 1) {
            String counter = "Receta " + (recipeIndex + 1) + "/" + recipeCount;
            ctx.drawText(tr, counter, x + w - 58 - tr.getWidth(counter) - 4, y + NAV_Y, 0xFFCCCCCC, false);
            Rect prev = prevButton(x, y, w, recipeCount), next = nextButton(x, y, w, recipeCount);
            ctx.drawText(tr, "<", prev.x(), prev.y(), prev.contains(mouseX, mouseY) ? 0xFFFFFFFF : 0xFFAAAAAA, false);
            ctx.drawText(tr, ">", next.x(), next.y(), next.contains(mouseX, mouseY) ? 0xFFFFFFFF : 0xFFAAAAAA, false);
        }
        ctx.drawText(tr, resultName, x, y + NAME_Y, 0xFFFFD700, true);

        int gridY = y + GRID_Y;
        int cols, rows;
        List<Ingredient> ingredients;
        if (recipe instanceof ShapedRecipe shaped) {
            cols = shaped.getWidth();
            rows = shaped.getHeight();
            ingredients = shaped.getIngredients();
        } else {
            // Sin forma: no hay una posición "real" por ingrediente, así que se
            // empaquetan en filas de hasta 3 — nunca se inventa una forma que la
            // receta no tiene, solo se eligen filas/columnas para mostrarlos todos.
            ingredients = recipe.getIngredients();
            cols = Math.max(1, Math.min(3, ingredients.size()));
            rows = Math.max(1, (ingredients.size() + cols - 1) / cols);
        }
        int gw = gridWidth(cols), gh = gridHeight(rows);
        int gridX = x;
        for (int row = 0; row < rows; row++) {
            for (int col = 0; col < cols; col++) {
                int i = row * cols + col;
                int sx = gridX + col * (SLOT + SLOT_GAP);
                int sy = gridY + row * (SLOT + SLOT_GAP);
                ctx.fill(sx, sy, sx + SLOT, sy + SLOT, 0x50000000);
                drawSlotBorder(ctx, sx, sy, SLOT, 0x60FFFFFF);
                if (i >= ingredients.size()) continue;
                Ingredient ingredient = ingredients.get(i);
                if (ingredient.isEmpty()) continue; // espacio vacío real de la receta
                ItemStack[] variants = ingredient.getMatchingStacks();
                if (variants.length == 0) continue;
                ctx.drawItem(variants[0], sx + 1, sy + 1);
                if (variants.length > 1) {
                    // No se representa como si solo aceptara un ítem: se marca que
                    // hay más de una variante válida para este espacio.
                    ctx.drawText(tr, "+" + (variants.length - 1), sx + SLOT - 8, sy + SLOT - 8, 0xFFFFFF55, true);
                }
            }
        }

        int arrowX = gridX + gw + (ARROW_W - tr.getWidth("→")) / 2;
        ctx.drawText(tr, "→", arrowX, gridY + gh / 2 - 4, 0xFFFFD700, false);

        int resultX = gridX + gw + ARROW_W;
        int resultY = gridY + gh / 2 - RESULT_SLOT / 2;
        ItemStack result = recipe.getResult(registries);
        ctx.fill(resultX, resultY, resultX + RESULT_SLOT, resultY + RESULT_SLOT, 0x50000000);
        drawSlotBorder(ctx, resultX, resultY, RESULT_SLOT, 0xFFFFD700);
        ctx.getMatrices().push();
        int pad = (RESULT_SLOT - 16) / 2;
        ctx.getMatrices().translate(resultX + pad, resultY + pad, 0);
        ctx.drawItem(result, 0, 0);
        ctx.getMatrices().pop();
        if (result.getCount() > 1) {
            ctx.drawText(tr, "x" + result.getCount(), resultX + RESULT_SLOT - 10, resultY + RESULT_SLOT - 9, 0xFFFFFFFF, true);
        }
    }

    private static void drawSlotBorder(DrawContext ctx, int x, int y, int size, int color) {
        ctx.fill(x, y, x + size, y + 1, color);
        ctx.fill(x, y + size - 1, x + size, y + size, color);
        ctx.fill(x, y, x + 1, y + size, color);
        ctx.fill(x + size - 1, y, x + size, y + size, color);
    }
}
