/*
 * Decompiled with CFR 0.152.
 */
package net.sundggs.avecoins.client;

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.gui.screens.inventory.InventoryScreen;
import net.sundggs.avecoins.client.gui.AdminShopScreen;
import net.sundggs.avecoins.client.gui.CartScreen;
import net.sundggs.avecoins.client.gui.CategoryEditorScreen;
import net.sundggs.avecoins.client.gui.CraftingConfigScreen;
import net.sundggs.avecoins.client.gui.DashboardScreen;
import net.sundggs.avecoins.client.gui.DowngradeRecipeScreen;
import net.sundggs.avecoins.client.gui.ProductEditorScreen;
import net.sundggs.avecoins.client.gui.PurchaseHistoryScreen;
import net.sundggs.avecoins.client.gui.PurchaseResultScreen;
import net.sundggs.avecoins.client.gui.PlayerMarketScreen;
import net.sundggs.avecoins.client.gui.PlayerMarketCreateScreen;
import net.sundggs.avecoins.client.gui.RecipeEditorScreen;
import net.sundggs.avecoins.client.gui.SaleResultScreen;
import net.sundggs.avecoins.client.gui.SellBoxScreen;
import net.sundggs.avecoins.client.gui.ShopScreen;
import net.sundggs.avecoins.client.gui.ShopSettingsScreen;
import net.sundggs.avecoins.client.gui.ThemedScreen;
import net.sundggs.avecoins.client.gui.WalletScreen;
import net.sundggs.avecoins.config.AtomicJsonStore;
import net.sundggs.avecoins.shop.ShopService;
import net.sundggs.avecoins.shop.PlayerMarketService;

public final class ClientScreenDispatcher {
    public static void handle(String action, String data) {
        if (action.equals("open_dashboard")) {
            JsonObject snapshot = JsonParser.parseString((String)data).getAsJsonObject();
            Minecraft.getInstance().gui.setScreen((Screen)new DashboardScreen(snapshot));
        } else if (action.equals("open_configuration")) {
            JsonObject snapshot = JsonParser.parseString((String)data).getAsJsonObject();
            String result = snapshot.get("result").getAsString();
            String target = snapshot.get("target").getAsString();
            if (target.equals("coins")) {
                Minecraft.getInstance().gui.setScreen((Screen)new CraftingConfigScreen(snapshot, CraftingConfigScreen.Mode.COINS, result));
            } else if (target.equals("tickets")) {
                Minecraft.getInstance().gui.setScreen((Screen)new CraftingConfigScreen(snapshot, CraftingConfigScreen.Mode.TICKETS, result));
            } else if (target.equals("shop")) {
                Minecraft.getInstance().gui.setScreen((Screen)new ShopSettingsScreen(snapshot, result));
            }
        } else if (action.equals("operation_result")) {
            JsonObject response = JsonParser.parseString((String)data).getAsJsonObject();
            String result = response.get("code").getAsString();
            boolean success = response.get("success").getAsBoolean();
            Screen screen = Minecraft.getInstance().gui.screen();
            if (screen instanceof DashboardScreen) {
                DashboardScreen dashboardScreen = (DashboardScreen)screen;
                dashboardScreen.showResult(result);
            } else {
                screen = Minecraft.getInstance().gui.screen();
                if (screen instanceof CraftingConfigScreen) {
                    CraftingConfigScreen craftingScreen = (CraftingConfigScreen)screen;
                    craftingScreen.showResult(result);
                } else {
                    screen = Minecraft.getInstance().gui.screen();
                    if (screen instanceof ShopSettingsScreen) {
                        ShopSettingsScreen settingsScreen = (ShopSettingsScreen)screen;
                        settingsScreen.showResult(result);
                    } else {
                        screen = Minecraft.getInstance().gui.screen();
                        if (screen instanceof RecipeEditorScreen) {
                            RecipeEditorScreen recipeEditorScreen = (RecipeEditorScreen)screen;
                            recipeEditorScreen.showResult(result, success);
                        } else {
                            screen = Minecraft.getInstance().gui.screen();
                            if (screen instanceof DowngradeRecipeScreen) {
                                DowngradeRecipeScreen downgradeRecipeScreen = (DowngradeRecipeScreen)screen;
                                downgradeRecipeScreen.showResult(result, success);
                            } else {
                                screen = Minecraft.getInstance().gui.screen();
                                if (screen instanceof CartScreen) {
                                    CartScreen cartScreen = (CartScreen)screen;
                                    cartScreen.showResult(result, success);
                                }
                            }
                        }
                    }
                }
            }
        } else if (action.equals("open_shop")) {
            ShopService.ShopSnapshot snapshot = (ShopService.ShopSnapshot)AtomicJsonStore.gson().fromJson(data, ShopService.ShopSnapshot.class);
            Minecraft.getInstance().gui.setScreen((Screen)new ShopScreen(snapshot));
        } else if (action.equals("open_shop_admin")) {
            ShopService.ShopSnapshot snapshot = (ShopService.ShopSnapshot)AtomicJsonStore.gson().fromJson(data, ShopService.ShopSnapshot.class);
            Minecraft.getInstance().gui.setScreen((Screen)new AdminShopScreen(snapshot, ""));
        } else if (action.equals("open_shop_history")) {
            ShopService.ShopSnapshot snapshot = (ShopService.ShopSnapshot)AtomicJsonStore.gson().fromJson(data, ShopService.ShopSnapshot.class);
            Screen success = Minecraft.getInstance().gui.screen();
            if (success instanceof PurchaseHistoryScreen) {
                PurchaseHistoryScreen historyScreen = (PurchaseHistoryScreen)success;
                Minecraft.getInstance().gui.setScreen((Screen)historyScreen.refreshed(snapshot));
            } else {
                Minecraft.getInstance().gui.setScreen((Screen)new PurchaseHistoryScreen(snapshot));
            }
        } else if (action.equals("shop_sync")) {
            JsonObject response = JsonParser.parseString((String)data).getAsJsonObject();
            ShopService.ShopSnapshot snapshot = (ShopService.ShopSnapshot)AtomicJsonStore.gson().fromJson(response.get("snapshot"), ShopService.ShopSnapshot.class);
            String result = response.get("result").getAsString();
            Screen screen = Minecraft.getInstance().gui.screen();
            if (screen instanceof CartScreen) {
                CartScreen cartScreen = (CartScreen)screen;
                Minecraft.getInstance().gui.setScreen((Screen)cartScreen.refreshed(snapshot, result));
            } else {
                screen = Minecraft.getInstance().gui.screen();
                if (screen instanceof AdminShopScreen) {
                    AdminShopScreen adminScreen = (AdminShopScreen)screen;
                    Minecraft.getInstance().gui.setScreen((Screen)new AdminShopScreen(snapshot, result, adminScreen.selectedCategoryId(), adminScreen.categoryPage(), adminScreen.productPage()));
                } else {
                    screen = Minecraft.getInstance().gui.screen();
                    if (screen instanceof CategoryEditorScreen) {
                        CategoryEditorScreen categoryEditor = (CategoryEditorScreen)screen;
                        Minecraft.getInstance().gui.setScreen((Screen)categoryEditor.refreshedParent(snapshot, result));
                    } else {
                        screen = Minecraft.getInstance().gui.screen();
                        if (screen instanceof ProductEditorScreen) {
                            ProductEditorScreen productEditor = (ProductEditorScreen)screen;
                            Minecraft.getInstance().gui.setScreen((Screen)productEditor.refreshedParent(snapshot, result));
                        } else {
                            screen = Minecraft.getInstance().gui.screen();
                            if (screen instanceof ShopScreen) {
                                ShopScreen shopScreen = (ShopScreen)screen;
                                Minecraft.getInstance().gui.setScreen((Screen)shopScreen.refreshed(snapshot));
                            } else {
                                Minecraft.getInstance().gui.setScreen((Screen)new ShopScreen(snapshot));
                            }
                        }
                    }
                }
            }
        } else if (action.equals("purchase_result")) {
            JsonObject response = JsonParser.parseString((String)data).getAsJsonObject();
            ShopService.ShopSnapshot snapshot = (ShopService.ShopSnapshot)AtomicJsonStore.gson().fromJson(response.get("snapshot"), ShopService.ShopSnapshot.class);
            boolean success = response.get("success").getAsBoolean();
            String code = response.get("code").getAsString();
            Screen screen = Minecraft.getInstance().gui.screen();
            if (screen instanceof CartScreen) {
                CartScreen cartScreen = (CartScreen)screen;
                Minecraft.getInstance().gui.setScreen((Screen)cartScreen.purchaseResult(success, code, snapshot));
            } else {
                Minecraft.getInstance().gui.setScreen((Screen)new PurchaseResultScreen(success, code, snapshot));
            }
        } else if (action.equals("sale_result")) {
            JsonObject response = JsonParser.parseString((String)data).getAsJsonObject();
            ShopService.ShopSnapshot snapshot = (ShopService.ShopSnapshot)AtomicJsonStore.gson().fromJson(response.get("snapshot"), ShopService.ShopSnapshot.class);
            boolean success = response.get("success").getAsBoolean();
            String code = response.get("code").getAsString();
            Screen screen = Minecraft.getInstance().gui.screen();
            if (screen instanceof SellBoxScreen) {
                SellBoxScreen sellBoxScreen = (SellBoxScreen)screen;
                if (success) {
                    if (Minecraft.getInstance().player != null) {
                        Minecraft.getInstance().player.closeContainer();
                    }
                    Minecraft.getInstance().gui.setScreen((Screen)new SaleResultScreen(code, snapshot));
                } else {
                    sellBoxScreen.applyResult(snapshot, code);
                }
            } else if (success) {
                Minecraft.getInstance().gui.setScreen((Screen)new SaleResultScreen(code, snapshot));
            }
        } else if (action.equals("open_player_market")) {
            PlayerMarketService.MarketSnapshot snapshot = (PlayerMarketService.MarketSnapshot)AtomicJsonStore.gson().fromJson(data, PlayerMarketService.MarketSnapshot.class);
            Minecraft.getInstance().gui.setScreen((Screen)new PlayerMarketScreen(snapshot, ""));
        } else if (action.equals("player_market_update")) {
            JsonObject response = JsonParser.parseString((String)data).getAsJsonObject();
            PlayerMarketService.MarketSnapshot snapshot = (PlayerMarketService.MarketSnapshot)AtomicJsonStore.gson().fromJson(response.get("snapshot"), PlayerMarketService.MarketSnapshot.class);
            String result = response.get("result").getAsString();
            Screen screen = Minecraft.getInstance().gui.screen();
            boolean success = response.get("success").getAsBoolean();
            if (screen instanceof PlayerMarketCreateScreen) {
                PlayerMarketCreateScreen createScreen = (PlayerMarketCreateScreen)screen;
                if (success) {
                    Minecraft.getInstance().gui.setScreen((Screen)new PlayerMarketScreen(snapshot, result));
                } else {
                    Minecraft.getInstance().gui.setScreen((Screen)createScreen.refreshed(snapshot, result));
                }
            } else if (screen instanceof PlayerMarketScreen) {
                PlayerMarketScreen marketScreen = (PlayerMarketScreen)screen;
                Minecraft.getInstance().gui.setScreen((Screen)marketScreen.refreshed(snapshot, result));
            } else {
                Minecraft.getInstance().gui.setScreen((Screen)new PlayerMarketScreen(snapshot, result));
            }
        } else if (action.equals("shop_state")) {
            Screen screen;
            JsonObject state = JsonParser.parseString((String)data).getAsJsonObject();
            boolean enabled = state.get("enabled").getAsBoolean();
            if (state.has("shopColor") && (screen = Minecraft.getInstance().gui.screen()) instanceof ThemedScreen) {
                ThemedScreen themed = (ThemedScreen)screen;
                themed.applyShopColor(state.get("shopColor").getAsString());
            }
            if ((screen = Minecraft.getInstance().gui.screen()) instanceof InventoryScreen) {
                InventoryScreen inventoryScreen = (InventoryScreen)screen;
                inventoryScreen.resize(inventoryScreen.width, inventoryScreen.height);
            }
            if (!enabled && (Minecraft.getInstance().gui.screen() instanceof ShopScreen || Minecraft.getInstance().gui.screen() instanceof CartScreen || Minecraft.getInstance().gui.screen() instanceof AdminShopScreen || Minecraft.getInstance().gui.screen() instanceof PurchaseHistoryScreen || Minecraft.getInstance().gui.screen() instanceof WalletScreen || Minecraft.getInstance().gui.screen() instanceof SellBoxScreen || Minecraft.getInstance().gui.screen() instanceof SaleResultScreen || Minecraft.getInstance().gui.screen() instanceof PlayerMarketScreen || Minecraft.getInstance().gui.screen() instanceof PlayerMarketCreateScreen)) {
                Minecraft.getInstance().gui.setScreen(null);
            }
        }
    }

    private ClientScreenDispatcher() {
    }
}

