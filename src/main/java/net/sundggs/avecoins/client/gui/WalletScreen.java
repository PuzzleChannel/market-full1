/*
 * Decompiled with CFR 0.152.
 */
package net.sundggs.avecoins.client.gui;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import net.minecraft.ChatFormatting;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.components.events.GuiEventListener;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.resources.Identifier;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.ItemLike;
import net.sundggs.avecoins.client.gui.GuiPalette;
import net.sundggs.avecoins.client.gui.GuiTextLayout;
import net.sundggs.avecoins.client.gui.PanelButton;
import net.sundggs.avecoins.client.gui.ResponsiveContainerScreen;
import net.sundggs.avecoins.client.gui.ThemedScreen;
import net.sundggs.avecoins.network.ClientNetworking;
import net.sundggs.avecoins.network.ServerboundAvecoinsPayload;
import net.sundggs.avecoins.shop.WalletMenu;

public final class WalletScreen
extends ResponsiveContainerScreen<WalletMenu>
implements ThemedScreen {
    private String paletteColor = "";
    private GuiPalette palette = GuiPalette.DEFAULT;
    private static final int BODY_HEIGHT = 312;
    private static final int FOOTER_HEIGHT = 30;
    private static final int DIVIDER_X = 224;
    private static final int LEFT_CENTER = 112;
    private static final int GRID_LEFT = 31;
    private static final int RIGHT_LEFT = 245;
    private static final int RIGHT_CENTER = 392;
    private static final int SUMMARY_COLUMN_STEP = 154;
    private static final int SUMMARY_CARD_WIDTH = 140;
    private static final List<String> CURRENCIES = List.of("vmtrade:vmcoin", "avecoins:goldticket", "avecoins:diamondticket", "avecoins:netheriteticket");

    @Override
    public GuiPalette theme() {
        String color;
        String string = color = ((WalletMenu)this.menu).snapshot().shopColor == null ? "" : ((WalletMenu)this.menu).snapshot().shopColor;
        if (!color.equals(this.paletteColor)) {
            this.palette = GuiPalette.fromHex(color);
            this.paletteColor = color;
        }
        return this.palette;
    }

    @Override
    public void applyShopColor(String color) {
        ((WalletMenu)this.menu).snapshot().shopColor = color;
    }

    public WalletScreen(WalletMenu menu, Inventory inventory, Component title) {
        super(menu, inventory, title, 560, 342);
        this.titleLabelX = 0;
        this.titleLabelY = 0;
        this.inventoryLabelX = 31;
        this.inventoryLabelY = 189;
    }

    @Override
    protected void init() {
        super.init();
        this.addRenderableWidget(new PanelButton(this.leftPos + 8, this.topPos + 6, 64, 18, (Component)Component.literal((String)"< ").append((Component)Component.translatable((String)"gui.avecoins.back")), PanelButton.Variant.DANGER, button -> this.backToShop()));
    }

    private void backToShop() {
        if (this.minecraft.player != null) {
            this.minecraft.player.closeContainer();
            ClientNetworking.send(new ServerboundAvecoinsPayload("request_player_market", "{}"));
        }
    }

    @Override
    protected void extractPanelBackground(GuiGraphicsExtractor graphics, float partialTick, int mouseX, int mouseY) {
        int column;
        graphics.fill(this.leftPos, this.topPos, this.leftPos + this.imageWidth, this.topPos + this.imageHeight, this.theme().BORDER);
        graphics.fill(this.leftPos + 1, this.topPos + 1, this.leftPos + this.imageWidth - 1, this.topPos + this.imageHeight - 1, this.theme().PANEL);
        graphics.fill(this.leftPos + 1, this.topPos + 1, this.leftPos + this.imageWidth - 1, this.topPos + 30, this.theme().HEADER);
        graphics.fill(this.leftPos + 1, this.topPos + 312, this.leftPos + this.imageWidth - 1, this.topPos + this.imageHeight - 1, this.theme().HEADER);
        graphics.fill(this.leftPos + 224, this.topPos + 40, this.leftPos + 224 + 1, this.topPos + 312 - 13, this.theme().DIVIDER);
        graphics.fill(this.leftPos + 31, this.topPos + 172, this.leftPos + 31 + 162, this.topPos + 173, this.theme().DIVIDER);
        for (int row = 0; row < 3; ++row) {
            for (int column2 = 0; column2 < 9; ++column2) {
                this.slotBackground(graphics, 31 + column2 * 18, 102 + row * 18);
                this.slotBackground(graphics, 31 + column2 * 18, 202 + row * 18);
            }
        }
        for (column = 0; column < 9; ++column) {
            this.slotBackground(graphics, 31 + column * 18, 260);
        }
        for (int row = 0; row < CURRENCIES.size(); ++row) {
            int x = this.leftPos + 322;
            int y = this.topPos + 126 + row * 34;
            graphics.fill(x, y, x + 140, y + 28, (row & 1) == 0 ? this.theme().SURFACE : this.theme().ALTERNATE);
        }
    }

    private void slotBackground(GuiGraphicsExtractor graphics, int x, int y) {
        graphics.fill(this.leftPos + x - 1, this.topPos + y - 1, this.leftPos + x + 17, this.topPos + y + 17, this.theme().BORDER);
        graphics.fill(this.leftPos + x, this.topPos + y, this.leftPos + x + 16, this.topPos + y + 16, this.theme().SURFACE);
    }

    protected void extractLabels(GuiGraphicsExtractor graphics, int mouseX, int mouseY) {
        MutableComponent heading = Component.literal((String)("\u00a4 " + this.title.getString().toUpperCase(Locale.ROOT))).withStyle(ChatFormatting.BOLD);
        GuiTextLayout.drawCentered(graphics, this.font, (Component)heading, this.imageWidth / 2, 10, this.theme().TEXT, !this.theme().custom);
        GuiTextLayout.drawCentered(graphics, this.font, (Component)Component.translatable((String)"gui.avecoins.wallet.storage").withStyle(ChatFormatting.BOLD), LEFT_CENTER, 76, this.theme().TEXT, !this.theme().custom);
        GuiTextLayout.drawCentered(graphics, this.font, this.playerInventoryTitle, LEFT_CENTER, this.inventoryLabelY, this.theme().MUTED, false);
        GuiTextLayout.drawCentered(graphics, this.font, (Component)Component.translatable((String)"gui.avecoins.wallet.summary").withStyle(ChatFormatting.BOLD), 392, 85, this.theme().TEXT, !this.theme().custom);
        GuiTextLayout.drawCentered(graphics, this.font, (Component)Component.translatable((String)"gui.avecoins.wallet.summary_help"), 392, 101, this.theme().MUTED, !this.theme().custom);
        Map<String, Long> totals = this.walletTotals();
        for (int index = 0; index < CURRENCIES.size(); ++index) {
            String currency = CURRENCIES.get(index);
            Identifier id = Identifier.tryParse((String)currency);
            if (id == null || !BuiltInRegistries.ITEM.containsKey(id)) continue;
            int x = 323;
            int y = 132 + index * 34;
            ItemStack stack = new ItemStack((ItemLike)BuiltInRegistries.ITEM.getValue(id));
            graphics.item(stack, x, y);
            graphics.text(this.font, this.font.plainSubstrByWidth(stack.getHoverName().getString(), 104), x + 20, y - 2, this.theme().TEXT, false);
            graphics.text(this.font, (Component)Component.literal((String)("\u00d7 " + String.valueOf(totals.getOrDefault(currency, 0L)))), x + 20, y + 9, this.theme().MUTED, false);
        }
    }

    private Map<String, Long> walletTotals() {
        LinkedHashMap<String, Long> totals = new LinkedHashMap<String, Long>();
        for (int slot = 0; slot < 27; ++slot) {
            ItemStack stack = ((WalletMenu)this.menu).walletContainer().getItem(slot);
            if (stack.isEmpty()) continue;
            totals.merge(BuiltInRegistries.ITEM.getKey(stack.getItem()).toString(), Long.valueOf(stack.getCount()), Long::sum);
        }
        return totals;
    }
}

