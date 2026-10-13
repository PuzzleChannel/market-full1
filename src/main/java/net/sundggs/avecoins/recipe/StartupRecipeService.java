/*
 * Decompiled with CFR 0.152.
 */
package net.sundggs.avecoins.recipe;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.function.Function;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.Identifier;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.MinecraftServer;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStackTemplate;
import net.minecraft.world.item.crafting.CraftingBookCategory;
import net.minecraft.world.item.crafting.CraftingRecipe;
import net.minecraft.world.item.crafting.Ingredient;
import net.minecraft.world.item.crafting.Recipe;
import net.minecraft.world.item.crafting.RecipeHolder;
import net.minecraft.world.item.crafting.RecipeMap;
import net.minecraft.world.item.crafting.ShapedRecipe;
import net.minecraft.world.item.crafting.ShapedRecipePattern;
import net.minecraft.world.item.crafting.ShapelessRecipe;
import net.minecraft.world.level.ItemLike;
import net.sundggs.avecoins.AvecoinsMod;
import net.sundggs.avecoins.config.CraftingConfig;
import net.sundggs.avecoins.config.ModConfigManager;
import net.sundggs.avecoins.config.RecipeDefinition;
import net.sundggs.avecoins.config.RecipesConfig;
import net.sundggs.avecoins.mixin.RecipeManagerAccessor;
import net.sundggs.avecoins.recipe.RecipeGridPattern;
import net.sundggs.avecoins.shop.CoinExchangeService;

public final class StartupRecipeService {
    private static final Set<Identifier> MANAGED_DEFAULT_RECIPE_IDS = Set.of(StartupRecipeService.managedId("coppercoin"), StartupRecipeService.managedId("ironcoin"), StartupRecipeService.managedId("goldcoin"), StartupRecipeService.managedId("diamondcoin"), StartupRecipeService.managedId("netheritecoin"), StartupRecipeService.managedId("goldticket"), StartupRecipeService.managedId("diamondticket"), StartupRecipeService.managedId("netheriteticket"), StartupRecipeService.managedId("ic_to_cc"), StartupRecipeService.managedId("gc_to_ic"), StartupRecipeService.managedId("dc_to_gc"), StartupRecipeService.managedId("nc_to_dc"));
    private static final Set<Identifier> MANAGED_CUSTOM_RECIPE_IDS = Set.of(StartupRecipeService.customId("coppercoin"), StartupRecipeService.customId("ironcoin"), StartupRecipeService.customId("goldcoin"), StartupRecipeService.customId("diamondcoin"), StartupRecipeService.customId("netheritecoin"), StartupRecipeService.customId("goldticket"), StartupRecipeService.customId("diamondticket"), StartupRecipeService.customId("netheriteticket"), StartupRecipeService.customId("vmcoin"));
    private static final Map<Identifier, String> DOWNGRADE_SOURCES = StartupRecipeService.downgradeSources();
    private static MinecraftServer server;
    private static List<RecipeHolder<?>> defaults;

    public static void initialize(MinecraftServer minecraftServer) {
        server = minecraftServer;
        StartupRecipeService.captureDefaults();
        StartupRecipeService.applyAtStartup();
    }

    public static void clear() {
        server = null;
        defaults = List.of();
    }

    private static void captureDefaults() {
        defaults = server.getRecipeManager().getRecipes().stream().filter(holder -> MANAGED_DEFAULT_RECIPE_IDS.contains(holder.id().identifier())).toList();
        AvecoinsMod.LOGGER.info("Loaded {} AVECOINS recipe definitions for this server start", (Object)defaults.size());
    }

    private static boolean applyAtStartup() {
        if (server == null) {
            return false;
        }
        CraftingConfig config = ModConfigManager.crafting();
        RecipesConfig recipes = ModConfigManager.recipes();
        List current = List.copyOf(server.getRecipeManager().getRecipes());
        ArrayList<Object> active = new ArrayList<Object>(current.size() + defaults.size() + recipes.customRecipes.size());
        active.addAll(StartupRecipeService.preserveUnmanaged(current, holder -> holder.id().identifier()));
        RecipeManagerAccessor accessor = (RecipeManagerAccessor)server.getRecipeManager();
        RecipeMap previous = accessor.avecoins$getRecipes();
        boolean replacementInstalled = false;
        try {
            for (RecipeHolder<?> recipeHolder : defaults) {
                String resultId = StartupRecipeService.resultId(recipeHolder);
                if (resultId == null || !config.isEnabled(resultId)) continue;
                String downgradeSource = DOWNGRADE_SOURCES.get(recipeHolder.id().identifier());
                if (downgradeSource != null) {
                    active.add(StartupRecipeService.createDowngradeRecipe(recipeHolder.id(), downgradeSource, resultId, CoinExchangeService.activeRates().downgradeCount(downgradeSource)));
                    continue;
                }
                if (StartupRecipeService.isPrimaryRecipe(recipeHolder, resultId) && recipes.customRecipes.containsKey(resultId)) continue;
                active.add(recipeHolder);
            }
            for (Map.Entry entry : recipes.customRecipes.entrySet()) {
                if (!config.isEnabled((String)entry.getKey())) continue;
                active.add(StartupRecipeService.createCustomRecipe((String)entry.getKey(), (RecipeDefinition)entry.getValue()));
            }
            RecipeMap replacement = RecipeMap.create(active);
            accessor.avecoins$setRecipes(replacement);
            replacementInstalled = true;
            server.getRecipeManager().finalizeRecipeLoading(server.getWorldData().enabledFeatures());
            AvecoinsMod.LOGGER.info("Applied AVECOINS crafting policy during server startup");
            return true;
        }
        catch (RuntimeException exception) {
            if (replacementInstalled) {
                try {
                    accessor.avecoins$setRecipes(previous);
                    server.getRecipeManager().finalizeRecipeLoading(server.getWorldData().enabledFeatures());
                }
                catch (RuntimeException runtimeException) {
                    exception.addSuppressed(runtimeException);
                }
            }
            AvecoinsMod.LOGGER.error("Could not apply the AVECOINS recipe configuration; restored the startup recipe set", (Throwable)exception);
            return false;
        }
    }

    static boolean occupiesManagedSlot(Identifier recipeId) {
        return MANAGED_DEFAULT_RECIPE_IDS.contains(recipeId) || MANAGED_CUSTOM_RECIPE_IDS.contains(recipeId);
    }

    static <T> List<T> preserveUnmanaged(List<T> recipes, Function<T, Identifier> idResolver) {
        return recipes.stream().filter(recipe -> !StartupRecipeService.occupiesManagedSlot((Identifier)idResolver.apply(recipe))).toList();
    }

    private static String resultId(RecipeHolder<?> holder) {
        Identifier recipeId = holder.id().identifier();
        String path = recipeId.getPath();
        if (List.of("ic_to_cc", "gc_to_ic", "dc_to_gc", "nc_to_dc").contains(path)
            || CraftingConfig.LEGACY_COINS.contains("avecoins:" + path)) {
            return null;
        }
        return MANAGED_DEFAULT_RECIPE_IDS.contains(recipeId)
            ? Identifier.fromNamespaceAndPath((String)"avecoins", (String)path).toString()
            : null;
    }

    private static RecipeHolder<?> createCustomRecipe(String resultId, RecipeDefinition definition) {
        Identifier resultLocation = StartupRecipeService.requireItem(resultId);
        Item resultItem = (Item)BuiltInRegistries.ITEM.getValue(resultLocation);
        RecipeGridPattern normalized = RecipeGridPattern.fromEditorGrid(definition.grid);
        ArrayList ingredients = new ArrayList(normalized.width() * normalized.height());
        for (int index = 0; index < normalized.ingredients().size(); ++index) {
            String ingredientId = normalized.ingredients().get(index);
            if (ingredientId.isEmpty()) {
                ingredients.add(Optional.empty());
                continue;
            }
            Item ingredient = (Item)BuiltInRegistries.ITEM.getValue(StartupRecipeService.requireItem(ingredientId));
            ingredients.add(Optional.of(Ingredient.of((ItemLike)ingredient)));
        }
        ShapedRecipePattern pattern = new ShapedRecipePattern(normalized.width(), normalized.height(), ingredients, Optional.empty());
        ShapedRecipe recipe = new ShapedRecipe(new Recipe.CommonInfo(true), new CraftingRecipe.CraftingBookInfo(CraftingBookCategory.MISC, "avecoins_custom"), pattern, new ItemStackTemplate(resultItem, definition.count));
        Identifier recipeId = Identifier.fromNamespaceAndPath((String)"avecoins", (String)("custom/" + resultLocation.getPath()));
        ResourceKey recipeKey = ResourceKey.create((ResourceKey)Registries.RECIPE, (Identifier)recipeId);
        return new RecipeHolder(recipeKey, (Recipe)recipe);
    }

    private static RecipeHolder<?> createDowngradeRecipe(ResourceKey<Recipe<?>> recipeKey, String sourceId, String resultId, int count) {
        Item sourceItem = (Item)BuiltInRegistries.ITEM.getValue(StartupRecipeService.requireItem(sourceId));
        Item resultItem = (Item)BuiltInRegistries.ITEM.getValue(StartupRecipeService.requireItem(resultId));
        ShapelessRecipe recipe = new ShapelessRecipe(new Recipe.CommonInfo(true), new CraftingRecipe.CraftingBookInfo(CraftingBookCategory.MISC, "avecoins_downgrade"), new ItemStackTemplate(resultItem, count), List.of(Ingredient.of((ItemLike)sourceItem)));
        return new RecipeHolder(recipeKey, (Recipe)recipe);
    }

    private static boolean isPrimaryRecipe(RecipeHolder<?> holder, String resultId) {
        return holder.id().identifier().equals((Object)Identifier.tryParse((String)resultId));
    }

    private static Map<Identifier, String> downgradeSources() {
        return Map.of();
    }

    private static Identifier requireItem(String value) {
        Identifier id = Identifier.tryParse((String)value);
        if (id == null || !BuiltInRegistries.ITEM.containsKey(id)) {
            throw new IllegalArgumentException("Unknown item: " + value);
        }
        return id;
    }

    private static Identifier managedId(String path) {
        return Identifier.fromNamespaceAndPath((String)"avecoins", (String)path);
    }

    private static Identifier customId(String path) {
        return Identifier.fromNamespaceAndPath((String)"avecoins", (String)("custom/" + path));
    }

    private StartupRecipeService() {
    }

    static {
        defaults = List.of();
    }
}

