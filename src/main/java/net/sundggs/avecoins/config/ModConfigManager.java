/*
 * Decompiled with CFR 0.152.
 */
package net.sundggs.avecoins.config;

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.google.gson.reflect.TypeToken;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.LinkOption;
import java.nio.file.Path;
import java.util.Map;
import net.fabricmc.loader.api.FabricLoader;
import net.sundggs.avecoins.AvecoinsMod;
import net.sundggs.avecoins.config.AtomicJsonStore;
import net.sundggs.avecoins.config.CraftingConfig;
import net.sundggs.avecoins.config.GeneralConfig;
import net.sundggs.avecoins.config.RecipeDefinition;
import net.sundggs.avecoins.config.RecipesConfig;
import net.sundggs.avecoins.shop.ShopStore;

public final class ModConfigManager {
    private static final Path CONFIG_DIRECTORY = FabricLoader.getInstance().getConfigDir().resolve("avecoins").toAbsolutePath().normalize();
    private static final AtomicJsonStore<GeneralConfig> GENERAL = new AtomicJsonStore<GeneralConfig>(CONFIG_DIRECTORY.resolve("settings.json"), GeneralConfig.class, GeneralConfig::defaults, GeneralConfig::validate);
    private static final AtomicJsonStore<CraftingConfig> CRAFTING = new AtomicJsonStore<CraftingConfig>(CONFIG_DIRECTORY.resolve("crafting.json"), CraftingConfig.class, CraftingConfig::defaults, CraftingConfig::validate);
    private static final AtomicJsonStore<RecipesConfig> RECIPES = new AtomicJsonStore<RecipesConfig>(CONFIG_DIRECTORY.resolve("recipes.json"), RecipesConfig.class, RecipesConfig::defaults, RecipesConfig::validate);

    public static void initialize() {
        GENERAL.load();
        boolean canRewriteCrafting = ModConfigManager.migrateLegacyCraftingRecipes();
        CRAFTING.load();
        RECIPES.load();
        if (canRewriteCrafting) {
            CRAFTING.save(CRAFTING.get());
        }
        ShopStore.initialize();
    }

    public static boolean reload() {
        boolean generalLoaded = GENERAL.reload();
        boolean craftingLoaded = CRAFTING.reload();
        boolean recipesLoaded = RECIPES.reload();
        boolean shopLoaded = ShopStore.reload();
        return generalLoaded && craftingLoaded && recipesLoaded && shopLoaded;
    }

    public static AtomicJsonStore<GeneralConfig> generalStore() {
        return GENERAL;
    }

    public static AtomicJsonStore<CraftingConfig> craftingStore() {
        return CRAFTING;
    }

    public static AtomicJsonStore<RecipesConfig> recipesStore() {
        return RECIPES;
    }

    public static GeneralConfig general() {
        return GENERAL.get();
    }

    public static CraftingConfig crafting() {
        return CRAFTING.get();
    }

    public static RecipesConfig recipes() {
        return RECIPES.get();
    }

    private static boolean migrateLegacyCraftingRecipes() {
        Path craftingFile = CONFIG_DIRECTORY.resolve("crafting.json");
        Path recipesFile = CONFIG_DIRECTORY.resolve("recipes.json");
        if (Files.notExists(craftingFile, new LinkOption[0]) || Files.exists(recipesFile, new LinkOption[0])) {
            return true;
        }
        try {
            String json = Files.readString(craftingFile, StandardCharsets.UTF_8);
            AtomicJsonStore.requireStrictJson(json);
            JsonObject root = JsonParser.parseString((String)json).getAsJsonObject();
            if (!root.has("customRecipes")) {
                return true;
            }
            RecipesConfig recipes = new RecipesConfig();
            recipes.customRecipes = (Map)AtomicJsonStore.gson().fromJson(root.get("customRecipes"), new TypeToken<Map<String, RecipeDefinition>>(){}.getType());
            if (RECIPES.save(RecipesConfig.validate(recipes))) {
                AvecoinsMod.LOGGER.info("Migrated AVECOINS custom recipes to recipes.json");
                return true;
            }
            return false;
        }
        catch (Exception exception) {
            AvecoinsMod.LOGGER.error("Could not migrate legacy AVECOINS custom recipes", (Throwable)exception);
            return false;
        }
    }

    private ModConfigManager() {
    }
}

