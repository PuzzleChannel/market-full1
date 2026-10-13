/*
 * Decompiled with CFR 0.152.
 */
package net.sundggs.avecoins.recipe;

import java.util.ArrayList;
import java.util.List;

public record RecipeGridPattern(int width, int height, List<String> ingredients) {
    public RecipeGridPattern {
        ingredients = List.copyOf(ingredients);
    }

    public static RecipeGridPattern fromEditorGrid(List<String> grid) {
        if (grid == null || grid.size() != 9) {
            throw new IllegalArgumentException("Recipe grids require exactly nine entries");
        }
        int minRow = 3;
        int maxRow = -1;
        int minColumn = 3;
        int maxColumn = -1;
        for (int index = 0; index < grid.size(); ++index) {
            if (grid.get(index) == null || grid.get(index).isEmpty()) continue;
            int row = index / 3;
            int column = index % 3;
            minRow = Math.min(minRow, row);
            maxRow = Math.max(maxRow, row);
            minColumn = Math.min(minColumn, column);
            maxColumn = Math.max(maxColumn, column);
        }
        if (maxRow < 0) {
            throw new IllegalArgumentException("A custom recipe cannot be empty");
        }
        int width = maxColumn - minColumn + 1;
        int height = maxRow - minRow + 1;
        ArrayList<String> ingredients = new ArrayList<String>(width * height);
        for (int row = minRow; row <= maxRow; ++row) {
            for (int column = minColumn; column <= maxColumn; ++column) {
                String ingredient = grid.get(row * 3 + column);
                ingredients.add(ingredient == null ? "" : ingredient);
            }
        }
        return new RecipeGridPattern(width, height, ingredients);
    }
}

