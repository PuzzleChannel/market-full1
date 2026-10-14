/*
 * Decompiled with CFR 0.152.
 */
package net.sundggs.avecoins.server;

import com.google.gson.JsonObject;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerLifecycleEvents;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerTickEvents;
import net.fabricmc.fabric.api.networking.v1.ServerPlayConnectionEvents;
import net.sundggs.avecoins.config.ModConfigManager;
import net.sundggs.avecoins.network.ModNetworking;
import net.sundggs.avecoins.network.ServerPayloadHandler;
import net.sundggs.avecoins.recipe.StartupRecipeService;
import net.sundggs.avecoins.shop.CoinExchangeService;
import net.sundggs.avecoins.shop.ShopService;
import net.sundggs.avecoins.shop.WalletStore;
import net.sundggs.avecoins.shop.PlayerMarketStore;
import net.sundggs.avecoins.shop.PlayerMarketService;
import net.sundggs.avecoins.shop.WalletService;

public final class ModServerEvents {
    private static int deliveryTicks;

    public static void register() {
        ServerLifecycleEvents.SERVER_STARTED.register(server -> {
            WalletStore.initialize(server);
            PlayerMarketStore.initialize(server);
            CoinExchangeService.initialize();
            StartupRecipeService.initialize(server);
        });
        ServerLifecycleEvents.SERVER_STOPPING.register(server -> {
            StartupRecipeService.clear();
            CoinExchangeService.clear();
            ShopService.clearSessions();
            ServerPayloadHandler.clearSessions();
            WalletStore.clear();
            PlayerMarketStore.clear();
        });
        ServerTickEvents.END_SERVER_TICK.register(server -> {
            if (++deliveryTicks >= 20) {
                deliveryTicks = 0;
                PlayerMarketService.processDeliveries(server);
            }
        });
        ServerPlayConnectionEvents.DISCONNECT.register((handler, server) -> {
            ShopService.removeSession(handler.player.getUUID());
            ServerPayloadHandler.removeSession(handler.player.getUUID());
        });
        ServerPlayConnectionEvents.JOIN.register((handler, sender, server) -> {
            WalletService.migrateLegacyCoins(handler.player);
            JsonObject state = new JsonObject();
            state.addProperty("enabled", Boolean.valueOf(ModConfigManager.general().shopEnabled));
            ModNetworking.send(handler.player, "shop_state", state.toString());
        });
    }

    private ModServerEvents() {
    }
}

