/*
 * Decompiled with CFR 0.152.
 */
package net.sundggs.avecoins.client.gui;

import java.util.LinkedHashMap;
import java.util.Locale;
import java.util.Map;
import net.minecraft.ChatFormatting;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.components.events.GuiEventListener;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.core.HolderLookup;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.resources.Identifier;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.ItemLike;
import net.sundggs.avecoins.client.gui.GuiPalette;
import net.sundggs.avecoins.client.gui.GuiTextLayout;
import net.sundggs.avecoins.client.gui.PanelButton;
import net.sundggs.avecoins.client.gui.ResponsiveContainerScreen;
import net.sundggs.avecoins.client.gui.ShopScreen;
import net.sundggs.avecoins.client.gui.ThemedScreen;
import net.sundggs.avecoins.network.ClientNetworking;
import net.sundggs.avecoins.network.ServerboundAvecoinsPayload;
import net.sundggs.avecoins.shop.SellBoxMenu;
import net.sundggs.avecoins.shop.ShopData;
import net.sundggs.avecoins.shop.ShopItemStacks;
import net.sundggs.avecoins.shop.ShopService;

public final class SellBoxScreen
extends ResponsiveContainerScreen<SellBoxMenu>
implements ThemedScreen {
    private String paletteColor = "";
    private GuiPalette palette = GuiPalette.DEFAULT;
    private static final int BODY_HEIGHT = 312;
    private static final int FOOTER_HEIGHT = 30;
    private static final int DIVIDER_X = 224;
    private static final int LEFT_CENTER = 112;
    private static final int GRID_LEFT = 31;
    private static final int LEFT_CONTENT_OFFSET = 15;
    private static final int RIGHT_LEFT = 245;
    private static final int RIGHT_CENTER = 392;
    private static final int SUMMARY_TITLE_Y = 63;
    private static final int SUMMARY_HELP_Y = 79;
    private static final int SUMMARY_AREA_TOP = 85;
    private static final int SUMMARY_AREA_HEIGHT = 95;
    private static final int SUMMARY_ROW_STEP = 24;
    private static final int SUMMARY_COLUMN_STEP = 154;
    private static final int SUMMARY_CARD_WIDTH = 140;
    private static final int SUMMARY_CARD_HEIGHT = 22;
    private static final int SECTION_DIVIDER_Y = 186;
    private static final int DESTINATION_TITLE_Y = 206;
    private static final int DESTINATION_HELP_Y = 214;
    private static final int DESTINATION_BUTTON_Y = 231;
    private static final int CONFIRM_BUTTON_Y = 268;
    private boolean depositInWallet;
    private String result = "";
    private PanelButton inventoryDestinationButton;
    private PanelButton walletDestinationButton;
    private PanelButton confirmButton;

    @Override
    public GuiPalette theme() {
        String color;
        String string = color = ((SellBoxMenu)this.menu).snapshot().shopColor == null ? "" : ((SellBoxMenu)this.menu).snapshot().shopColor;
        if (!color.equals(this.paletteColor)) {
            this.palette = GuiPalette.fromHex(color);
            this.paletteColor = color;
        }
        return this.palette;
    }

    @Override
    public void applyShopColor(String color) {
        ((SellBoxMenu)this.menu).snapshot().shopColor = color;
    }

    public SellBoxScreen(SellBoxMenu menu, Inventory inventory, Component title) {
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
        this.inventoryDestinationButton = new PanelButton(this.leftPos + 245, this.topPos + 231, 140, 22, (Component)Component.literal((String)"\u25c6 ").append((Component)Component.translatable((String)"gui.avecoins.sell_box.inventory")), PanelButton.Variant.SELECTED, button -> this.selectDestination(false));
        this.walletDestinationButton = new PanelButton(this.leftPos + 245 + 154, this.topPos + 231, 140, 22, (Component)Component.literal((String)"\u00a4 ").append((Component)Component.translatable((String)"gui.avecoins.sell_box.wallet")), PanelButton.Variant.SECONDARY, button -> this.selectDestination(true));
        this.addRenderableWidget(this.inventoryDestinationButton);
        this.addRenderableWidget(this.walletDestinationButton);
        this.confirmButton = new PanelButton(this.leftPos + 392 - 70, this.topPos + 268, 140, 22, (Component)Component.translatable((String)"gui.avecoins.sell_box.confirm"), PanelButton.Variant.SUCCESS, button -> this.confirmSale());
        this.confirmButton.active = !this.totals().isEmpty();
        this.addRenderableWidget(this.confirmButton);
    }

    private void selectDestination(boolean wallet) {
        this.depositInWallet = wallet;
        this.inventoryDestinationButton.setVariant(wallet ? PanelButton.Variant.SECONDARY : PanelButton.Variant.SELECTED);
        this.walletDestinationButton.setVariant(wallet ? PanelButton.Variant.SELECTED : PanelButton.Variant.SECONDARY);
    }

    private void confirmSale() {
        ClientNetworking.send(new ServerboundAvecoinsPayload("confirm_sale", "{\"wallet\":" + this.depositInWallet + "}"));
    }

    private void backToShop() {
        if (this.minecraft.player != null) {
            this.minecraft.player.closeContainer();
            this.minecraft.gui.setScreen((Screen)new ShopScreen(((SellBoxMenu)this.menu).snapshot()));
        }
        ClientNetworking.send(new ServerboundAvecoinsPayload("request_shop", "{}"));
    }

    public void applyResult(ShopService.ShopSnapshot snapshot, String code) {
        ((SellBoxMenu)this.menu).updateSnapshot(snapshot);
        this.result = code == null ? "" : code;
        this.confirmButton.active = !this.totals().isEmpty();
    }

    @Override
    protected void extractPanelBackground(GuiGraphicsExtractor graphics, float partialTick, int mouseX, int mouseY) {
        graphics.fill(this.leftPos, this.topPos, this.leftPos + this.imageWidth, this.topPos + this.imageHeight, this.theme().BORDER);
        graphics.fill(this.leftPos + 1, this.topPos + 1, this.leftPos + this.imageWidth - 1, this.topPos + this.imageHeight - 1, this.theme().PANEL);
        graphics.fill(this.leftPos + 1, this.topPos + 1, this.leftPos + this.imageWidth - 1, this.topPos + 30, this.theme().HEADER);
        graphics.fill(this.leftPos + 1, this.topPos + 312, this.leftPos + this.imageWidth - 1, this.topPos + this.imageHeight - 1, this.theme().HEADER);
        graphics.fill(this.leftPos + 224, this.topPos + 40, this.leftPos + 224 + 1, this.topPos + 312 - 13, this.theme().DIVIDER);
        graphics.fill(this.leftPos + 31, this.topPos + 157 + 15, this.leftPos + 31 + 162, this.topPos + 158 + 15, this.theme().DIVIDER);
        graphics.fill(this.leftPos + 245, this.topPos + 186, this.leftPos + 245 + 294, this.topPos + 186 + 1, this.theme().DIVIDER);
        for (int row = 0; row < 3; ++row) {
            for (int column = 0; column < 9; ++column) {
                this.slotBackground(graphics, 31 + column * 18, 102 + row * 18);
                this.slotBackground(graphics, 31 + column * 18, 202 + row * 18);
            }
        }
        for (int column = 0; column < 9; ++column) {
            this.slotBackground(graphics, 31 + column * 18, 260);
        }
        Map<String, Long> totals = this.totals();
        int cardCount = this.summaryCardCount(totals.size(), this.hasSellableProduct());
        int summaryTop = this.summaryCardsTop(cardCount);
        for (int index = 0; index < cardCount; ++index) {
            int column = index % 2;
            int row = index / 2;
            int x = this.leftPos + 245 + column * 154;
            int y = this.topPos + summaryTop + row * 24;
            graphics.fill(x, y, x + 140, y + 22, (row & 1) == 0 ? this.theme().SURFACE : this.theme().ALTERNATE);
        }
    }

    private void slotBackground(GuiGraphicsExtractor graphics, int x, int y) {
        graphics.fill(this.leftPos + x - 1, this.topPos + y - 1, this.leftPos + x + 17, this.topPos + y + 17, this.theme().BORDER);
        graphics.fill(this.leftPos + x, this.topPos + y, this.leftPos + x + 16, this.topPos + y + 16, this.theme().SURFACE);
    }

    protected void extractLabels(GuiGraphicsExtractor graphics, int mouseX, int mouseY) {
        MutableComponent feedback;
        MutableComponent heading = Component.literal((String)("\u00a4 " + this.title.getString().toUpperCase(Locale.ROOT))).withStyle(ChatFormatting.BOLD);
        GuiTextLayout.drawCentered(graphics, this.font, (Component)heading, this.imageWidth / 2, 10, this.theme().TEXT, !this.theme().custom);
        GuiTextLayout.drawCentered(graphics, this.font, (Component)Component.translatable((String)"gui.avecoins.sell_box.contents").withStyle(ChatFormatting.BOLD), LEFT_CENTER, 65, this.theme().TEXT, !this.theme().custom);
        GuiTextLayout.drawCentered(graphics, this.font, this.playerInventoryTitle, LEFT_CENTER, this.inventoryLabelY, this.theme().MUTED, false);
        Map<String, Long> totals = this.totals();
        boolean hasSellableProduct = this.hasSellableProduct();
        int summaryTop = this.summaryCardsTop(this.summaryCardCount(totals.size(), hasSellableProduct));
        GuiTextLayout.drawCentered(graphics, this.font, (Component)Component.translatable((String)"gui.avecoins.sell_box.summary").withStyle(ChatFormatting.BOLD), RIGHT_CENTER, 63, this.theme().TEXT, !this.theme().custom);
        int index = 0;
        for (Map.Entry<String, Long> total : totals.entrySet()) {
            if (index >= 8) break;
            int column = index % 2;
            int row = index / 2;
            int x = 251 + column * 154;
            int y = summaryTop + 3 + row * 24;
            Identifier id = Identifier.tryParse((String)total.getKey());
            String amount = String.valueOf(total.getValue()) + "x";
            if (id != null && BuiltInRegistries.ITEM.containsKey(id)) {
                ItemStack currency = new ItemStack((ItemLike)BuiltInRegistries.ITEM.getValue(id));
                graphics.item(currency, x, y);
                int nameWidth = Math.max(40, 104 - this.font.width(amount));
                graphics.text(this.font, this.font.plainSubstrByWidth(currency.getHoverName().getString(), nameWidth), x + 20, y + 4, this.theme().TEXT, false);
            }
            graphics.text(this.font, amount, x + 140 - 12 - this.font.width(amount), y + 4, this.theme().SUCCESS, false);
            ++index;
        }
        if (totals.isEmpty()) {
            GuiTextLayout.drawCenteredFitted(graphics, this.font,
                    (Component)Component.translatable((String)"gui.avecoins.sell_box.empty"),
                    RIGHT_CENTER, 128, 270, this.theme().MUTED);
        }
        GuiTextLayout.drawCentered(graphics, this.font, (Component)Component.translatable((String)"gui.avecoins.sell_box.destination").withStyle(ChatFormatting.BOLD),
                RIGHT_CENTER, DESTINATION_TITLE_Y, this.theme().TEXT, !this.theme().custom);
        int remaining = this.remainingItems();
        feedback = !this.result.isBlank() ? Component.translatable((String)("message.avecoins." + this.result))
                : (remaining > 0 ? Component.translatable((String)"gui.avecoins.sell_box.remaining_note") : Component.empty());
        if (!feedback.getString().isBlank()) {
            int color = !this.result.isBlank() ? this.theme().ERROR : this.theme().WARNING;
            GuiTextLayout.drawCentered(graphics, this.font, this.font.plainSubstrByWidth(feedback.getString(), 294), 392, 296, color, !this.theme().custom);
        }
    }

    @Override
    protected void beforeResponsiveRender() {
        this.confirmButton.active = !this.totals().isEmpty();
    }

    private int summaryCardsTop(int totalCount) {
        int rows = (totalCount + 1) / 2;
        if (rows == 0) {
            return SUMMARY_AREA_TOP;
        }
        int cardsHeight = (rows - 1) * SUMMARY_ROW_STEP + SUMMARY_CARD_HEIGHT;
        return SUMMARY_AREA_TOP + (SUMMARY_AREA_HEIGHT - cardsHeight) / 2;
    }

    private int summaryCardCount(int totalCount, boolean hasSellableProduct) {
        if (!hasSellableProduct || totalCount <= 0) {
            return 0;
        }
        return Math.min(8, totalCount);
    }

    private Map<String, Long> totals() {
        LinkedHashMap<String, Integer> offered = new LinkedHashMap<String, Integer>();
        for (int slot = 0; slot < ((SellBoxMenu)this.menu).sellContainer().getContainerSize(); ++slot) {
            ItemStack stack = ((SellBoxMenu)this.menu).sellContainer().getItem(slot);
            ShopData.Product product = this.matchingProduct(stack);
            if (product == null) continue;
            offered.merge(product.id, stack.getCount(), Integer::sum);
        }
        LinkedHashMap<String, Long> totals = new LinkedHashMap<String, Long>();
        for (Map.Entry entry : offered.entrySet()) {
            int quantity;
            ShopData.Product product = this.product((String)entry.getKey());
            if (product == null || (quantity = (Integer)entry.getValue() / product.itemCount) <= 0) continue;
            totals.merge(product.currencyId, (long)product.effectiveSellingPrice() * (long)quantity, Math::addExact);
        }
        return totals;
    }

    private int remainingItems() {
        ShopData.Product product;
        LinkedHashMap<String, Integer> matched = new LinkedHashMap<String, Integer>();
        int remaining = 0;
        for (int slot = 0; slot < ((SellBoxMenu)this.menu).sellContainer().getContainerSize(); ++slot) {
            ItemStack stack = ((SellBoxMenu)this.menu).sellContainer().getItem(slot);
            product = this.matchingProduct(stack);
            if (product == null) {
                remaining += stack.getCount();
                continue;
            }
            matched.merge(product.id, stack.getCount(), Integer::sum);
        }
        for (Map.Entry entry : matched.entrySet()) {
            product = this.product((String)entry.getKey());
            if (product == null) continue;
            remaining += (Integer)entry.getValue() % product.itemCount;
        }
        return remaining;
    }

    private boolean hasSellableProduct() {
        for (int slot = 0; slot < ((SellBoxMenu)this.menu).sellContainer().getContainerSize(); ++slot) {
            if (this.matchingProduct(((SellBoxMenu)this.menu).sellContainer().getItem(slot)) == null) continue;
            return true;
        }
        return false;
    }

    private ShopData.Product product(String id) {
        return ((SellBoxMenu)this.menu).snapshot().products.stream().filter(candidate -> candidate.id.equals(id)).findFirst().orElse(null);
    }

    private ShopData.Product matchingProduct(ItemStack offered) {
        if (offered.isEmpty() || this.minecraft.level == null) {
            return null;
        }
        for (ShopData.Product product : ((SellBoxMenu)this.menu).snapshot().products) {
            Identifier id;
            if (!product.enabled || !product.sellEnabled || (id = Identifier.tryParse((String)product.itemId)) == null || !BuiltInRegistries.ITEM.containsKey(id)) continue;
            Item item = (Item)BuiltInRegistries.ITEM.getValue(id);
            ItemStack expected = product.itemData == null || product.itemData.isBlank() ? new ItemStack((ItemLike)item) : ShopItemStacks.decode((HolderLookup.Provider)this.minecraft.level.registryAccess(), product.itemData).orElse(ItemStack.EMPTY);
            if (expected.isEmpty() || !ItemStack.isSameItemSameComponents((ItemStack)expected, (ItemStack)offered)) continue;
            return product;
        }
        return null;
    }
}

