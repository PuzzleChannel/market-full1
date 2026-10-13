/*
 * Decompiled with CFR 0.152.
 */
package net.sundggs.avecoins.client;

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import net.minecraft.client.Minecraft;
import net.sundggs.avecoins.AvecoinsMod;
import net.sundggs.avecoins.client.ClientScreenDispatcher;
import net.sundggs.avecoins.config.AtomicJsonStore;
import net.sundggs.avecoins.network.ClientboundAvecoinsPayload;

public final class ClientPayloadRouter {
    private static volatile boolean shopEnabled;

    public static void handle(ClientboundAvecoinsPayload payload) {
        Minecraft.getInstance().execute(() -> {
            try {
                AtomicJsonStore.requireStrictJson(payload.data());
                if (payload.action().equals("open_shop") || payload.action().equals("open_shop_admin") || payload.action().equals("open_shop_history")) {
                    JsonObject object = JsonParser.parseString((String)payload.data()).getAsJsonObject();
                    shopEnabled = object.get("enabled").getAsBoolean();
                } else if (payload.action().equals("shop_sync") || payload.action().equals("purchase_result") || payload.action().equals("sale_result")) {
                    JsonObject response = JsonParser.parseString((String)payload.data()).getAsJsonObject();
                    JsonObject snapshot = response.getAsJsonObject("snapshot");
                    shopEnabled = snapshot.get("enabled").getAsBoolean();
                } else if (payload.action().equals("shop_state")) {
                    shopEnabled = JsonParser.parseString((String)payload.data()).getAsJsonObject().get("enabled").getAsBoolean();
                }
                ClientScreenDispatcher.handle(payload.action(), payload.data());
            }
            catch (RuntimeException exception) {
                AvecoinsMod.LOGGER.error("Could not handle client payload {}", payload.action(), exception);
            }
        });
    }

    public static boolean isShopEnabled() {
        return shopEnabled;
    }

    private ClientPayloadRouter() {
    }
}

