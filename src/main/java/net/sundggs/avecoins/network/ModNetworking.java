/*
 * Decompiled with CFR 0.152.
 */
package net.sundggs.avecoins.network;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import java.nio.charset.StandardCharsets;
import net.fabricmc.fabric.api.menu.v1.ExtendedMenuProvider;
import net.fabricmc.fabric.api.networking.v1.PayloadTypeRegistry;
import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;
import net.minecraft.network.chat.Component;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.MenuProvider;
import net.minecraft.world.SimpleContainer;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.sundggs.avecoins.AvecoinsMod;
import net.sundggs.avecoins.command.ModCommands;
import net.sundggs.avecoins.config.AtomicJsonStore;
import net.sundggs.avecoins.config.ModConfigManager;
import net.sundggs.avecoins.message.ModMessages;
import net.sundggs.avecoins.network.ClientboundAvecoinsPayload;
import net.sundggs.avecoins.network.ServerPayloadHandler;
import net.sundggs.avecoins.network.ServerboundAvecoinsPayload;
import net.sundggs.avecoins.permission.PermissionService;
import net.sundggs.avecoins.shop.SellBoxMenu;
import net.sundggs.avecoins.shop.ShopService;
import net.sundggs.avecoins.shop.PlayerMarketService;
import net.sundggs.avecoins.shop.WalletMenu;
import net.sundggs.avecoins.shop.WalletService;

public final class ModNetworking {
    public static final String PROTOCOL_VERSION = "2.1";
    private static final int STRING_LENGTH_PREFIX_HEADROOM = 10;

    public static void register() {
        PayloadTypeRegistry.clientboundPlay().registerLarge(ClientboundAvecoinsPayload.TYPE, ClientboundAvecoinsPayload.STREAM_CODEC, 2097226);
        PayloadTypeRegistry.serverboundPlay().registerLarge(ServerboundAvecoinsPayload.TYPE, ServerboundAvecoinsPayload.STREAM_CODEC, 65610);
        ServerPlayNetworking.registerGlobalReceiver(ServerboundAvecoinsPayload.TYPE, ServerPayloadHandler::handle);
    }

    public static void openDashboard(ServerPlayer player, String resultCode) {
        ModNetworking.send(player, "open_dashboard", AtomicJsonStore.gson().toJson((JsonElement)ModNetworking.dashboardData(player, resultCode)));
    }

    public static void openConfiguration(ServerPlayer player, String target, String resultCode) {
        JsonObject root = ModNetworking.dashboardData(player, resultCode);
        root.addProperty("target", target);
        ModNetworking.send(player, "open_configuration", AtomicJsonStore.gson().toJson((JsonElement)root));
    }

    private static JsonObject dashboardData(ServerPlayer player, String resultCode) {
        JsonObject root = new JsonObject();
        root.add("general", AtomicJsonStore.gson().toJsonTree(ModConfigManager.general()));
        root.add("crafting", AtomicJsonStore.gson().toJsonTree(ModConfigManager.crafting()));
        root.add("recipes", AtomicJsonStore.gson().toJsonTree(ModConfigManager.recipes()));
        root.addProperty("result", resultCode);
        root.addProperty("activeCustomAlias", ModCommands.activeCustomAlias());
        root.addProperty("aliasRestartRequired", Boolean.valueOf(!ModCommands.activeCustomAlias().equals(ModConfigManager.general().customShopCommand)));
        root.addProperty("canConfigureCoins", Boolean.valueOf(PermissionService.has(player, "avecoins.config.coins")));
        root.addProperty("canConfigureTickets", Boolean.valueOf(PermissionService.has(player, "avecoins.config.tickets")));
        root.addProperty("canConfigureShop", Boolean.valueOf(PermissionService.has(player, "avecoins.config.shop")));
        root.addProperty("canReloadConfig", Boolean.valueOf(PermissionService.has(player, "avecoins.config.reload")));
        return root;
    }

    public static void openShop(ServerPlayer player, boolean administration, int historyPage) {
        boolean allowedAdmin;
        boolean bl = allowedAdmin = administration && PermissionService.isAdministrator(player);
        String action = allowedAdmin ? (historyPage >= 0 ? "open_shop_history" : "open_shop_admin") : "open_shop";
        ModNetworking.send(player, action, ShopService.snapshotJson(player, allowedAdmin, historyPage));
    }

    public static void openPlayerMarket(ServerPlayer player) {
        ModNetworking.send(player, "open_player_market", PlayerMarketService.snapshotJson(player));
    }

    public static void openPlayerMarketManage(ServerPlayer player) {
        ModNetworking.send(player, "open_player_market_manage", PlayerMarketService.snapshotJson(player));
    }

    public static void syncPlayerMarket(ServerPlayer player, PlayerMarketService.Result result) {
        JsonObject response = new JsonObject();
        response.addProperty("success", result.success());
        response.addProperty("result", result.code());
        response.add("snapshot", AtomicJsonStore.gson().toJsonTree(PlayerMarketService.snapshot(player)));
        ModNetworking.send(player, "player_market_update", AtomicJsonStore.gson().toJson(response));
    }

    public static void openWallet(final ServerPlayer player) {
        final ShopService.ShopSnapshot snapshot = ShopService.snapshot(player, false, 0);
        final String json = AtomicJsonStore.gson().toJson(snapshot);
        player.openMenu((MenuProvider)new ExtendedMenuProvider<String>(){

            public String getScreenOpeningData(ServerPlayer ignored) {
                return json;
            }

            public Component getDisplayName() {
                return Component.translatable((String)"gui.avecoins.wallet.title");
            }

            public AbstractContainerMenu createMenu(int containerId, Inventory inventory, Player ignored) {
                return new WalletMenu(containerId, inventory, WalletService.createContainer(player), snapshot);
            }
        });
    }

    public static void openSellBox(ServerPlayer player) {
        final ShopService.ShopSnapshot snapshot = ShopService.snapshot(player, false, 0);
        final String json = AtomicJsonStore.gson().toJson(snapshot);
        player.openMenu((MenuProvider)new ExtendedMenuProvider<String>(){

            public String getScreenOpeningData(ServerPlayer ignored) {
                return json;
            }

            public Component getDisplayName() {
                return Component.translatable((String)"gui.avecoins.sell_box.title");
            }

            public AbstractContainerMenu createMenu(int containerId, Inventory inventory, Player ignored) {
                return new SellBoxMenu(containerId, inventory, new SimpleContainer(27), snapshot);
            }
        });
    }

    public static void syncSale(ServerPlayer player, ShopService.OperationResult result) {
        JsonObject response = new JsonObject();
        response.addProperty("success", Boolean.valueOf(result.success()));
        response.addProperty("code", result.code());
        response.add("snapshot", AtomicJsonStore.gson().toJsonTree(ShopService.snapshot(player, false, 0)));
        ModNetworking.send(player, "sale_result", AtomicJsonStore.gson().toJson((JsonElement)response));
    }

    public static void syncShop(ServerPlayer player, boolean administration, int historyPage, String result) {
        JsonObject response = new JsonObject();
        response.addProperty("result", result);
        response.add("snapshot", AtomicJsonStore.gson().toJsonTree(ShopService.snapshot(player, administration && PermissionService.isAdministrator(player), historyPage)));
        ModNetworking.send(player, "shop_sync", AtomicJsonStore.gson().toJson((JsonElement)response));
    }

    public static void send(ServerPlayer player, String action, String data) {
        int payloadBytes = data.getBytes(StandardCharsets.UTF_8).length;
        if (payloadBytes > 0x200000) {
            AvecoinsMod.LOGGER.error("Refused oversized AVECOINS payload '{}' ({} bytes)", action, payloadBytes);
            ModMessages.send(player, "message.avecoins.payload_too_large", new Object[0]);
            return;
        }
        ServerPlayNetworking.send((ServerPlayer)player, (CustomPacketPayload)new ClientboundAvecoinsPayload(action, data));
    }

    public static void broadcastShopState(MinecraftServer server) {
        JsonObject state = new JsonObject();
        state.addProperty("enabled", Boolean.valueOf(ModConfigManager.general().shopEnabled));
        state.addProperty("shopColor", ModConfigManager.general().shopColor);
        for (ServerPlayer player : server.getPlayerList().getPlayers()) {
            ModNetworking.send(player, "shop_state", state.toString());
        }
    }

    private ModNetworking() {
    }
}

