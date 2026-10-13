/*
 * Decompiled with CFR 0.152.
 */
package net.sundggs.avecoins;

import com.mojang.blaze3d.platform.InputConstants;
import com.mojang.blaze3d.platform.Window;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.item.v1.ItemTooltipCallback;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;
import net.fabricmc.fabric.api.client.screen.v1.ScreenEvents;
import net.fabricmc.fabric.api.client.screen.v1.Screens;
import net.minecraft.ChatFormatting;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.screens.MenuScreens;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.gui.screens.inventory.InventoryScreen;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.FormattedText;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.network.chat.Style;
import net.minecraft.resources.Identifier;
import net.minecraft.world.item.ItemStack;
import net.sundggs.avecoins.client.ClientPayloadRouter;
import net.sundggs.avecoins.client.gui.SellBoxScreen;
import net.sundggs.avecoins.client.gui.ShopTabButton;
import net.sundggs.avecoins.client.gui.WalletScreen;
import net.sundggs.avecoins.network.ClientNetworking;
import net.sundggs.avecoins.network.ClientboundAvecoinsPayload;
import net.sundggs.avecoins.network.ServerboundAvecoinsPayload;
import net.sundggs.avecoins.shop.ModMenus;

public final class AvecoinsClient
implements ClientModInitializer {
    private static final Set<String> TOOLTIP_ITEMS = Set.of("coppercoin", "ironcoin", "goldcoin", "diamondcoin", "netheritecoin", "goldticket", "diamondticket", "netheriteticket");
    private static final int TOOLTIP_WIDTH = 200;
    private static final Map<String, ChatFormatting> ITEM_COLORS = Map.of("coppercoin", ChatFormatting.GOLD, "ironcoin", ChatFormatting.GRAY, "goldcoin", ChatFormatting.YELLOW, "diamondcoin", ChatFormatting.AQUA, "netheritecoin", ChatFormatting.DARK_GRAY, "goldticket", ChatFormatting.YELLOW, "diamondticket", ChatFormatting.AQUA, "netheriteticket", ChatFormatting.DARK_GRAY);
    private static final Map<String, UpgradePath> UPGRADE_PATHS = Map.of("coppercoin", new UpgradePath("copper", ChatFormatting.GOLD, "iron", ChatFormatting.GRAY), "ironcoin", new UpgradePath("iron", ChatFormatting.GRAY, "gold", ChatFormatting.YELLOW), "goldcoin", new UpgradePath("gold", ChatFormatting.YELLOW, "diamond", ChatFormatting.AQUA), "diamondcoin", new UpgradePath("diamond", ChatFormatting.AQUA, "netherite", ChatFormatting.DARK_GRAY));

    public void onInitializeClient() {
        MenuScreens.register(ModMenus.SELL_BOX, SellBoxScreen::new);
        MenuScreens.register(ModMenus.WALLET, WalletScreen::new);
        ItemTooltipCallback.EVENT.register((stack, context, flag, lines) -> this.appendTooltip(stack, lines));
        ScreenEvents.AFTER_INIT.register(this::addInventoryShopTab);
        ClientPlayNetworking.registerGlobalReceiver(ClientboundAvecoinsPayload.TYPE, (payload, context) -> ClientPayloadRouter.handle(payload));
    }

    private void appendTooltip(ItemStack stack, List<Component> tooltip) {
        Identifier itemId = BuiltInRegistries.ITEM.getKey((Object)stack.getItem());
        if (!itemId.getNamespace().equals("avecoins") || !TOOLTIP_ITEMS.contains(itemId.getPath())) {
            return;
        }
        String itemKey = itemId.getPath();
        tooltip.add((Component)Component.empty());
        if (AvecoinsClient.isShiftDown()) {
            AvecoinsClient.appendDetails(tooltip, itemKey);
        } else {
            MutableComponent highlight = Component.translatable((String)("tooltip.avecoins." + itemKey + ".description.highlight")).withStyle(ITEM_COLORS.get(itemKey));
            AvecoinsClient.appendWrapped(tooltip, (Component)Component.translatable((String)("tooltip.avecoins." + itemKey + ".description"), (Object[])new Object[]{highlight}).withStyle(ChatFormatting.WHITE), 200);
            tooltip.add((Component)Component.empty());
            AvecoinsClient.appendWrapped(tooltip, (Component)Component.translatable((String)"tooltip.avecoins.more_info", (Object[])new Object[]{Component.literal((String)"SHIFT").withStyle(new ChatFormatting[]{ChatFormatting.YELLOW, ChatFormatting.BOLD})}).withStyle(ChatFormatting.GRAY), 200);
        }
    }

    private static boolean isShiftDown() {
        Minecraft minecraft = Minecraft.getInstance();
        return InputConstants.isKeyDown((Window)minecraft.getWindow(), (int)340) || InputConstants.isKeyDown((Window)minecraft.getWindow(), (int)344);
    }

    private static void appendDetails(List<Component> tooltip, String itemKey) {
        UpgradePath path = UPGRADE_PATHS.get(itemKey);
        if (path != null) {
            MutableComponent tip = Component.translatable((String)"tooltip.avecoins.tip").withStyle(new ChatFormatting[]{ChatFormatting.GRAY, ChatFormatting.BOLD}).append((Component)Component.literal((String)" ")).append((Component)Component.translatable((String)"tooltip.avecoins.coin.upgrade_tip").withStyle(style -> style.withColor(ChatFormatting.WHITE).withBold(Boolean.valueOf(false))));
            AvecoinsClient.appendWrapped(tooltip, (Component)tip, 200);
            tooltip.add((Component)Component.empty());
            AvecoinsClient.appendWrapped(tooltip, (Component)Component.translatable((String)"tooltip.avecoins.coin.path", (Object[])new Object[]{AvecoinsClient.styledMaterial(path.fromMaterial(), path.fromColor()), AvecoinsClient.styledMaterial(path.toMaterial(), path.toColor())}).withStyle(ChatFormatting.WHITE), 200);
            return;
        }
        MutableComponent highlight = Component.translatable((String)("tooltip.avecoins." + itemKey + ".details.highlight")).withStyle(ITEM_COLORS.get(itemKey));
        MutableComponent details = Component.translatable((String)("tooltip.avecoins." + itemKey + ".details"), (Object[])new Object[]{highlight}).withStyle(style -> style.withColor(ChatFormatting.WHITE).withBold(Boolean.valueOf(false)));
        if (itemKey.equals("netheritecoin") || itemKey.endsWith("ticket")) {
            details = Component.translatable((String)"tooltip.avecoins.tip").withStyle(new ChatFormatting[]{ChatFormatting.GRAY, ChatFormatting.BOLD}).append((Component)Component.literal((String)" ")).append((Component)details);
        }
        AvecoinsClient.appendWrapped(tooltip, (Component)details, 200);
    }

    private static MutableComponent styledMaterial(String material, ChatFormatting color) {
        return Component.literal((String)"\u00a9 ").withStyle(color).append((Component)Component.translatable((String)("tooltip.avecoins.material." + material)).withStyle(color));
    }

    private static void appendWrapped(List<Component> tooltip, Component text, int width) {
        Minecraft minecraft = Minecraft.getInstance();
        int availableWidth = Math.max(1, minecraft.getWindow().getGuiScaledWidth() - 12);
        minecraft.font.getSplitter().splitLines((FormattedText)text, Math.min(width, availableWidth), Style.EMPTY, (line, continuation) -> tooltip.add(AvecoinsClient.copyFormatted(line)));
    }

    private static Component copyFormatted(FormattedText source) {
        MutableComponent result = Component.empty();
        source.visit((style, text) -> {
            result.append((Component)Component.literal((String)text).setStyle(style));
            return Optional.empty();
        }, Style.EMPTY);
        return result;
    }

    private void addInventoryShopTab(Minecraft minecraft, Screen screen, int scaledWidth, int scaledHeight) {
        if (!(screen instanceof InventoryScreen) || !ClientPayloadRouter.isShopEnabled()) {
            return;
        }
        int inventoryLeft = (scaledWidth - 176) / 2;
        int inventoryTop = (scaledHeight - 166) / 2;
        Screens.getWidgets((Screen)screen).add(new ShopTabButton(inventoryLeft - 24, inventoryTop + 8, () -> ClientNetworking.send(new ServerboundAvecoinsPayload("request_shop", "{}"))));
    }

    private record UpgradePath(String fromMaterial, ChatFormatting fromColor, String toMaterial, ChatFormatting toColor) {
    }
}

