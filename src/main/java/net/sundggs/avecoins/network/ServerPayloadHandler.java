/*
 * Decompiled with CFR 0.152.
 */
package net.sundggs.avecoins.network;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.Identifier;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.sundggs.avecoins.AvecoinsMod;
import net.sundggs.avecoins.config.AtomicJsonStore;
import net.sundggs.avecoins.config.CraftingConfig;
import net.sundggs.avecoins.config.GeneralConfig;
import net.sundggs.avecoins.config.ModConfigManager;
import net.sundggs.avecoins.config.RecipeDefinition;
import net.sundggs.avecoins.config.RecipesConfig;
import net.sundggs.avecoins.network.ModNetworking;
import net.sundggs.avecoins.network.ServerboundAvecoinsPayload;
import net.sundggs.avecoins.permission.PermissionService;
import net.sundggs.avecoins.shop.SellBoxMenu;
import net.sundggs.avecoins.shop.ShopData;
import net.sundggs.avecoins.shop.ShopService;
import net.sundggs.avecoins.shop.WalletStore;

public final class ServerPayloadHandler {
    private static final Map<UUID, Long> LAST_ACTION = new HashMap<UUID, Long>();
    private static final Map<UUID, Long> LAST_CHECKOUT = new HashMap<UUID, Long>();
    private static final Map<UUID, Long> LAST_SALE = new HashMap<UUID, Long>();

    public static void handle(ServerboundAvecoinsPayload payload, ServerPlayNetworking.Context context) {
        ServerPlayer player = context.player();
        context.server().execute(() -> ServerPayloadHandler.handleOnServer(payload, player, context.server()));
    }

    private static void handleOnServer(ServerboundAvecoinsPayload payload, ServerPlayer player, MinecraftServer server) {
        long previous;
        long now = System.nanoTime();
        if (now - (previous = LAST_ACTION.getOrDefault(player.getUUID(), 0L).longValue()) < 50000000L) {
            return;
        }
        LAST_ACTION.put(player.getUUID(), now);
        try {
            AtomicJsonStore.requireStrictJson(payload.data());
            ServerPayloadHandler.dispatch(player, server, payload.action(), payload.data());
        }
        catch (SecurityException exception) {
            AvecoinsMod.LOGGER.warn("Denied AVECOINS action '{}' from {}", (Object)payload.action(), (Object)player.getScoreboardName());
            ModNetworking.send(player, "operation_result", ServerPayloadHandler.resultJson(false, "permission_denied"));
        }
        catch (RuntimeException exception) {
            AvecoinsMod.LOGGER.warn("Rejected AVECOINS action '{}' from {}", new Object[]{payload.action(), player.getScoreboardName(), exception});
            ModNetworking.send(player, "operation_result", ServerPayloadHandler.resultJson(false, "invalid_request"));
        }
    }

    public static void removeSession(UUID playerId) {
        LAST_ACTION.remove(playerId);
        LAST_CHECKOUT.remove(playerId);
        LAST_SALE.remove(playerId);
    }

    public static void clearSessions() {
        LAST_ACTION.clear();
        LAST_CHECKOUT.clear();
        LAST_SALE.clear();
    }

    private static void dispatch(ServerPlayer player, MinecraftServer server, String action, String data) {
        switch (action) {
            case "request_dashboard": {
                ServerPayloadHandler.object(data, new String[0]);
                ServerPayloadHandler.require(player, "avecoins.gui");
                ModNetworking.openDashboard(player, "");
                break;
            }
            case "reload_config": {
                ServerPayloadHandler.object(data, new String[0]);
                ServerPayloadHandler.require(player, "avecoins.config.reload");
                boolean loaded = ModConfigManager.reload() && WalletStore.reload();
                ModNetworking.broadcastShopState(server);
                ModNetworking.openDashboard(player, loaded ? "configuration_reloaded" : "persistence_error");
                break;
            }
            case "request_shop": {
                ServerPayloadHandler.object(data, new String[0]);
                ServerPayloadHandler.require(player, "avecoins.shop.use");
                if (!ModConfigManager.general().shopEnabled) {
                    ModNetworking.send(player, "operation_result", ServerPayloadHandler.resultJson(false, "shop_disabled"));
                    break;
                }
                ModNetworking.openShop(player, false, 0);
                break;
            }
            case "request_wallet": {
                ServerPayloadHandler.object(data, new String[0]);
                ServerPayloadHandler.require(player, "avecoins.shop.use");
                if (!ModConfigManager.general().shopEnabled) {
                    ModNetworking.send(player, "operation_result", ServerPayloadHandler.resultJson(false, "shop_disabled"));
                    break;
                }
                ModNetworking.openWallet(player);
                break;
            }
            case "request_sell_box": {
                ServerPayloadHandler.object(data, new String[0]);
                ServerPayloadHandler.require(player, "avecoins.shop.use");
                if (!ModConfigManager.general().shopEnabled) {
                    ModNetworking.send(player, "operation_result", ServerPayloadHandler.resultJson(false, "shop_disabled"));
                    break;
                }
                ModNetworking.openSellBox(player);
                break;
            }
            case "confirm_sale": {
                ServerPayloadHandler.require(player, "avecoins.shop.use");
                long saleNow = System.nanoTime();
                long previousSale = LAST_SALE.getOrDefault(player.getUUID(), 0L);
                if (saleNow - previousSale < 750000000L) {
                    ModNetworking.syncSale(player, ShopService.OperationResult.failure("rate_limited"));
                    return;
                }
                LAST_SALE.put(player.getUUID(), saleNow);
                AbstractContainerMenu abstractContainerMenu = player.containerMenu;
                if (!(abstractContainerMenu instanceof SellBoxMenu)) {
                    ModNetworking.syncSale(player, ShopService.OperationResult.failure("invalid_request"));
                    return;
                }
                SellBoxMenu menu = (SellBoxMenu)abstractContainerMenu;
                boolean wallet = ServerPayloadHandler.booleanField(ServerPayloadHandler.object(data, "wallet"), "wallet", false);
                ShopService.OperationResult result = ShopService.sell(player, menu.sellContainer(), wallet);
                menu.broadcastChanges();
                ModNetworking.syncSale(player, result);
                break;
            }
            case "request_shop_admin": {
                ServerPayloadHandler.requireAdministrator(player);
                JsonObject json = ServerPayloadHandler.object(data, "history", "page");
                int historyPage = ServerPayloadHandler.booleanField(json, "history", false) ? ServerPayloadHandler.intField(json, "page", 0, 49) : -1;
                ModNetworking.openShop(player, true, historyPage);
                break;
            }
            case "save_general": {
                ServerPayloadHandler.require(player, "avecoins.config.shop");
                GeneralConfig candidate = GeneralConfig.validate(AtomicJsonStore.parseStrict(data, GeneralConfig.class));
                boolean saved = ModConfigManager.generalStore().save(candidate);
                if (saved) {
                    ModNetworking.broadcastShopState(server);
                }
                ModNetworking.openConfiguration(player, "shop", saved ? "settings_saved" : "persistence_error");
                break;
            }
            case "save_crafting_coins": 
            case "save_crafting_tickets": {
                CraftingConfig candidate = CraftingConfig.validate(AtomicJsonStore.parseStrict(data, CraftingConfig.class));
                ServerPayloadHandler.requireCraftingPermission(player, candidate);
                boolean saved = ModConfigManager.craftingStore().save(candidate);
                ModNetworking.openConfiguration(player, action.equals("save_crafting_coins") ? "coins" : "tickets", saved ? "crafting_restart_required" : "persistence_error");
                break;
            }
            case "save_custom_recipe": {
                JsonObject json = ServerPayloadHandler.object(data, "resultId", "recipe");
                String resultId = ServerPayloadHandler.managedResultId(json);
                ServerPayloadHandler.requireRecipePermission(player, resultId);
                RecipeDefinition definition = AtomicJsonStore.parseStrict(json.get("recipe").toString(), RecipeDefinition.class);
                ServerPayloadHandler.validateRecipeItems(definition);
                RecipesConfig candidate = ModConfigManager.recipes().copy();
                candidate.customRecipes.put(resultId, definition);
                candidate = RecipesConfig.validate(candidate);
                boolean saved = ServerPayloadHandler.saveRecipesForNextStart(candidate);
                ModNetworking.send(player, "operation_result", ServerPayloadHandler.resultJson(saved, saved ? "crafting_restart_required" : "persistence_error"));
                break;
            }
            case "restore_custom_recipe": {
                JsonObject json = ServerPayloadHandler.object(data, "resultId");
                String resultId = ServerPayloadHandler.managedResultId(json);
                ServerPayloadHandler.requireRecipePermission(player, resultId);
                RecipesConfig candidate = ModConfigManager.recipes().copy();
                candidate.customRecipes.remove(resultId);
                candidate = RecipesConfig.validate(candidate);
                boolean saved = ServerPayloadHandler.saveRecipesForNextStart(candidate);
                ModNetworking.send(player, "operation_result", ServerPayloadHandler.resultJson(saved, saved ? "crafting_restart_required" : "persistence_error"));
                break;
            }
            case "save_downgrade_recipe": {
                JsonObject json = ServerPayloadHandler.object(data, "sourceId", "count");
                String sourceId = ServerPayloadHandler.managedDowngradeSource(json);
                ServerPayloadHandler.require(player, "avecoins.config.coins");
                RecipesConfig candidate = ModConfigManager.recipes().copy();
                candidate.downgradeCounts.put(sourceId, ServerPayloadHandler.intField(json, "count", 1, 64));
                candidate = RecipesConfig.validate(candidate);
                boolean saved = ServerPayloadHandler.saveRecipesForNextStart(candidate);
                ModNetworking.send(player, "operation_result", ServerPayloadHandler.resultJson(saved, saved ? "crafting_restart_required" : "persistence_error"));
                break;
            }
            case "restore_downgrade_recipe": {
                JsonObject json = ServerPayloadHandler.object(data, "sourceId");
                String sourceId = ServerPayloadHandler.managedDowngradeSource(json);
                ServerPayloadHandler.require(player, "avecoins.config.coins");
                RecipesConfig candidate = ModConfigManager.recipes().copy();
                candidate.downgradeCounts.remove(sourceId);
                candidate = RecipesConfig.validate(candidate);
                boolean saved = ServerPayloadHandler.saveRecipesForNextStart(candidate);
                ModNetworking.send(player, "operation_result", ServerPayloadHandler.resultJson(saved, saved ? "crafting_restart_required" : "persistence_error"));
                break;
            }
            case "cart_quantity": {
                ServerPayloadHandler.require(player, "avecoins.shop.use");
                JsonObject json = ServerPayloadHandler.object(data, "productId", "quantity");
                ShopService.OperationResult result = ShopService.setCartQuantity(player, ServerPayloadHandler.stringField(json, "productId", 48), ServerPayloadHandler.intField(json, "quantity", 0, 1024));
                ModNetworking.syncShop(player, false, 0, result.code());
                break;
            }
            case "checkout": {
                ServerPayloadHandler.object(data, new String[0]);
                ServerPayloadHandler.require(player, "avecoins.shop.use");
                long now = System.nanoTime();
                long previousCheckout = LAST_CHECKOUT.getOrDefault(player.getUUID(), 0L);
                if (now - previousCheckout < 750000000L) {
                    JsonObject response = new JsonObject();
                    response.addProperty("success", Boolean.valueOf(false));
                    response.addProperty("code", "rate_limited");
                    response.add("snapshot", AtomicJsonStore.gson().toJsonTree((Object)ShopService.snapshot(player, false, 0)));
                    ModNetworking.send(player, "purchase_result", AtomicJsonStore.gson().toJson((JsonElement)response));
                    return;
                }
                LAST_CHECKOUT.put(player.getUUID(), now);
                ShopService.OperationResult result = ShopService.checkout(player);
                JsonObject response = new JsonObject();
                response.addProperty("success", Boolean.valueOf(result.success()));
                response.addProperty("code", result.code());
                response.add("snapshot", AtomicJsonStore.gson().toJsonTree((Object)ShopService.snapshot(player, false, 0)));
                ModNetworking.send(player, "purchase_result", AtomicJsonStore.gson().toJson((JsonElement)response));
                break;
            }
            case "save_category": {
                ServerPayloadHandler.requireAdministrator(player);
                ShopData.Category category = AtomicJsonStore.parseStrict(data, ShopData.Category.class);
                ShopService.OperationResult result = ShopService.saveCategory(category);
                ModNetworking.syncShop(player, true, -1, result.code());
                break;
            }
            case "move_category": {
                ServerPayloadHandler.requireAdministrator(player);
                JsonObject json = ServerPayloadHandler.object(data, "id", "direction");
                ShopService.OperationResult result = ShopService.moveCategory(ServerPayloadHandler.stringField(json, "id", 48), ServerPayloadHandler.intField(json, "direction", -1, 1));
                ModNetworking.syncShop(player, true, -1, result.code());
                break;
            }
            case "delete_category": {
                ServerPayloadHandler.requireAdministrator(player);
                ShopService.OperationResult result = ShopService.deleteCategory(ServerPayloadHandler.stringField(ServerPayloadHandler.object(data, "id"), "id", 48));
                ModNetworking.syncShop(player, true, -1, result.code());
                break;
            }
            case "save_product": {
                ServerPayloadHandler.requireAdministrator(player);
                ShopData.Product product = AtomicJsonStore.parseStrict(data, ShopData.Product.class);
                ShopService.OperationResult result = ShopService.saveProduct(player, product);
                ModNetworking.syncShop(player, true, -1, result.code());
                break;
            }
            case "move_product": {
                ServerPayloadHandler.requireAdministrator(player);
                JsonObject json = ServerPayloadHandler.object(data, "id", "direction");
                ShopService.OperationResult result = ShopService.moveProduct(ServerPayloadHandler.stringField(json, "id", 48), ServerPayloadHandler.intField(json, "direction", -1, 1));
                ModNetworking.syncShop(player, true, -1, result.code());
                break;
            }
            case "delete_product": {
                ServerPayloadHandler.requireAdministrator(player);
                ShopService.OperationResult result = ShopService.deleteProduct(ServerPayloadHandler.stringField(ServerPayloadHandler.object(data, "id"), "id", 48));
                ModNetworking.syncShop(player, true, -1, result.code());
                break;
            }
            default: {
                throw new IllegalArgumentException("Unknown action");
            }
        }
    }

    private static void require(ServerPlayer player, String node) {
        if (!PermissionService.has(player, node)) {
            throw new SecurityException("Permission denied");
        }
    }

    private static void requireAdministrator(ServerPlayer player) {
        if (!PermissionService.isAdministrator(player)) {
            throw new SecurityException("Permission denied");
        }
    }

    private static void requireCraftingPermission(ServerPlayer player, CraftingConfig candidate) {
        boolean ticketsChanged;
        CraftingConfig current = ModConfigManager.crafting();
        boolean coinsChanged = current.coinCraftingEnabled != candidate.coinCraftingEnabled || !current.coins.equals(candidate.coins);
        boolean bl = ticketsChanged = current.ticketCraftingEnabled != candidate.ticketCraftingEnabled || !current.tickets.equals(candidate.tickets);
        if (coinsChanged && !PermissionService.has(player, "avecoins.config.coins")) {
            throw new SecurityException("Coin configuration permission denied");
        }
        if (ticketsChanged && !PermissionService.has(player, "avecoins.config.tickets")) {
            throw new SecurityException("Ticket configuration permission denied");
        }
    }

    private static void requireRecipePermission(ServerPlayer player, String resultId) {
        ServerPayloadHandler.require(player, CraftingConfig.COINS.contains(resultId) ? "avecoins.config.coins" : "avecoins.config.tickets");
    }

    private static String managedResultId(JsonObject json) {
        String resultId = ServerPayloadHandler.stringField(json, "resultId", 128);
        if (!CraftingConfig.MANAGED_RESULTS.contains(resultId)) {
            throw new IllegalArgumentException("Unsupported recipe result");
        }
        return resultId;
    }

    private static String managedDowngradeSource(JsonObject json) {
        String sourceId = ServerPayloadHandler.stringField(json, "sourceId", 128);
        if (!RecipesConfig.DOWNGRADE_RESULTS.containsKey(sourceId)) {
            throw new IllegalArgumentException("Unsupported downgrade recipe source");
        }
        return sourceId;
    }

    private static boolean saveRecipesForNextStart(RecipesConfig candidate) {
        return ModConfigManager.recipesStore().save(candidate);
    }

    private static void validateRecipeItems(RecipeDefinition definition) {
        if (definition.grid == null) {
            throw new IllegalArgumentException("Recipe grid is missing");
        }
        for (String value : definition.grid) {
            Identifier id;
            if (value == null || value.isBlank() || (id = Identifier.tryParse((String)value)) != null && BuiltInRegistries.ITEM.containsKey(id)) continue;
            throw new IllegalArgumentException("Unknown recipe ingredient");
        }
    }

    private static JsonObject object(String data, String ... allowedFields) {
        JsonObject object = JsonParser.parseString((String)data).getAsJsonObject();
        Set<String> allowed = Set.of(allowedFields);
        if (!allowed.containsAll(object.keySet())) {
            throw new IllegalArgumentException("Unexpected request field");
        }
        return object;
    }

    private static int integerField(String data, String field, int min, int max) {
        return ServerPayloadHandler.intField(ServerPayloadHandler.object(data, new String[0]), field, min, max);
    }

    private static int intField(JsonObject object, String field, int min, int max) {
        if (!(object.has(field) && object.get(field).isJsonPrimitive() && object.getAsJsonPrimitive(field).isNumber())) {
            throw new IllegalArgumentException("Expected numeric request field");
        }
        int value = object.get(field).getAsInt();
        if (value < min || value > max) {
            throw new IllegalArgumentException("Integer out of range");
        }
        return value;
    }

    private static boolean booleanField(JsonObject object, String field, boolean fallback) {
        if (!object.has(field)) {
            return fallback;
        }
        if (!object.get(field).isJsonPrimitive() || !object.getAsJsonPrimitive(field).isBoolean()) {
            throw new IllegalArgumentException("Expected boolean request field");
        }
        return object.get(field).getAsBoolean();
    }

    private static String stringField(JsonObject object, String field, int max) {
        if (!(object.has(field) && object.get(field).isJsonPrimitive() && object.getAsJsonPrimitive(field).isString())) {
            throw new IllegalArgumentException("Expected text request field");
        }
        String value = object.get(field).getAsString();
        if (value.isBlank() || value.length() > max) {
            throw new IllegalArgumentException("Invalid text field");
        }
        return value;
    }

    private static String resultJson(boolean success, String code) {
        JsonObject result = new JsonObject();
        result.addProperty("success", Boolean.valueOf(success));
        result.addProperty("code", code);
        return AtomicJsonStore.gson().toJson((JsonElement)result);
    }

    private ServerPayloadHandler() {
    }
}

