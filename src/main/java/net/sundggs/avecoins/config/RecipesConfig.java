/*
 * Decompiled with CFR 0.152.
 */
package net.sundggs.avecoins.config;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.regex.Pattern;
import net.sundggs.avecoins.config.CraftingConfig;
import net.sundggs.avecoins.config.RecipeDefinition;

public final class RecipesConfig {
    private static final Pattern RESOURCE_ID = Pattern.compile("[a-z0-9_.-]+:[a-z0-9/._-]+");
    public static final int SCHEMA_VERSION = 2;
    public static final int DEFAULT_DOWNGRADE_COUNT = 4;
    public static final Map<String, String> DOWNGRADE_RESULTS = Map.of();
    public int schemaVersion = 2;
    public Map<String, RecipeDefinition> customRecipes = new LinkedHashMap<String, RecipeDefinition>();
    public Map<String, Integer> downgradeCounts = new LinkedHashMap<String, Integer>();

    public static RecipesConfig defaults() {
        return new RecipesConfig();
    }

    public RecipesConfig copy() {
        RecipesConfig copy = new RecipesConfig();
        copy.schemaVersion = this.schemaVersion;
        this.customRecipes.forEach((id, definition) -> copy.customRecipes.put((String)id, new RecipeDefinition(definition.count, definition.grid)));
        copy.downgradeCounts.putAll(this.downgradeCounts);
        return copy;
    }

    public static RecipesConfig validate(RecipesConfig input) {
        RecipesConfig output = new RecipesConfig();
        if (input.customRecipes != null) {
            for (Map.Entry<String, RecipeDefinition> entry : input.customRecipes.entrySet()) {
                String result = RecipesConfig.safeId(entry.getKey());
                if (!CraftingConfig.MANAGED_RESULTS.contains(result)) {
                    if (CraftingConfig.LEGACY_COINS.contains(result)) {
                        continue;
                    }
                    throw new IllegalArgumentException("Unsupported custom recipe result: " + result);
                }
                RecipeDefinition definition = entry.getValue();
                if (definition == null || definition.grid == null || definition.grid.size() != 9) {
                    throw new IllegalArgumentException("Custom recipes require exactly nine grid entries");
                }
                if (definition.count < 1 || definition.count > 64) {
                    throw new IllegalArgumentException("Recipe result count must be between 1 and 64");
                }
                List<String> grid = definition.grid.stream().map(value -> {
                    String normalized = value == null ? "" : value.trim();
                    return normalized.isEmpty() ? "" : RecipesConfig.safeId(normalized);
                }).toList();
                if (grid.stream().allMatch(String::isEmpty)) {
                    throw new IllegalArgumentException("A custom recipe cannot be empty");
                }
                output.customRecipes.put(result, new RecipeDefinition(definition.count, grid));
            }
        }
        if (input.downgradeCounts != null) {
            for (Map.Entry<String, Integer> entry : input.downgradeCounts.entrySet()) {
                String source = RecipesConfig.safeId(entry.getKey());
                Integer count = entry.getValue();
                if (!DOWNGRADE_RESULTS.containsKey(source)) {
                    if (CraftingConfig.LEGACY_COINS.contains(source)) {
                        continue;
                    }
                    throw new IllegalArgumentException("Invalid downgrade recipe override: " + source);
                }
                if (count == null || count < 1 || count > 64) {
                    throw new IllegalArgumentException("Invalid downgrade recipe override: " + source);
                }
                output.downgradeCounts.put(source, count);
            }
        }
        return output;
    }

    private static String safeId(String value) {
        if (value == null || value.length() > 128 || !RESOURCE_ID.matcher(value).matches()) {
            throw new IllegalArgumentException("Invalid item identifier: " + value);
        }
        return value;
    }
}

