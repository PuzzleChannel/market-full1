package net.sundggs.avecoins.client.gui;

import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.Identifier;
import net.minecraft.world.item.Item;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.input.MouseButtonEvent;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.ItemStack;
import net.sundggs.avecoins.network.ClientNetworking;
import net.sundggs.avecoins.network.ServerboundAvecoinsPayload;
import net.sundggs.avecoins.shop.PlayerMarketService;
import net.sundggs.avecoins.shop.ShopService;

/** Inventory-based player listing editor. The server validates the selected slot before removing anything. */
public final class PlayerMarketCreateScreen extends AvecoinsScreen {
    private static final int INVENTORY_WIDTH = 9 * 18;
    private static final Identifier COIN_ID = Identifier.fromNamespaceAndPath("vmtrade", "vmcoin");

    private final PlayerMarketService.MarketSnapshot snapshot;
    private String resultCode;
    private String savedPrice;
    private String savedQuantity;
    private int selectedSlot;
    private int extraDays;
    private net.minecraft.client.gui.components.EditBox priceBox;
    private net.minecraft.client.gui.components.EditBox quantityBox;
    private boolean confirmingListing;

    public PlayerMarketCreateScreen(PlayerMarketService.MarketSnapshot snapshot, String resultCode) {
        this(snapshot, resultCode, "1", "1", findInitialSlot());
    }

    private PlayerMarketCreateScreen(PlayerMarketService.MarketSnapshot snapshot, String resultCode,
                                     String savedPrice, String savedQuantity, int selectedSlot) {
        super(Component.translatable("gui.avecoins.player_market.create_title"));
        this.snapshot = snapshot;
        this.resultCode = resultCode == null ? "" : resultCode;
        this.savedPrice = savedPrice == null ? "1" : savedPrice;
        this.savedQuantity = savedQuantity == null ? "1" : savedQuantity;
        this.selectedSlot = selectedSlot;
        ShopService.ShopSnapshot themeSnapshot = new ShopService.ShopSnapshot();
        themeSnapshot.shopColor = snapshot.shopColor;
        this.useShopTheme(themeSnapshot);
    }

    private static int findInitialSlot() {
        var player = net.minecraft.client.Minecraft.getInstance().player;
        if (player != null) {
            for (int slot = 0; slot < 36; slot++) {
                if (!player.getInventory().getItem(slot).isEmpty()) return slot;
            }
        }
        return -1;
    }

    public PlayerMarketCreateScreen refreshed(PlayerMarketService.MarketSnapshot snapshot, String resultCode) {
        return new PlayerMarketCreateScreen(snapshot, resultCode,
                this.priceBox == null ? this.savedPrice : this.priceBox.getValue(),
                this.quantityBox == null ? this.savedQuantity : this.quantityBox.getValue(),
                this.selectedSlot);
    }

    @Override
    protected void init() {
        this.layoutPanel(820, 440);
        int headerY = this.panelTop + 7;
        this.addRenderableWidget(new PanelButton(this.panelLeft + 10, headerY, 112, 20,
                Component.translatable("gui.avecoins.player_market.back_to_market"), PanelButton.Variant.SECONDARY,
                button -> ClientNetworking.send(new ServerboundAvecoinsPayload("request_player_market", "{}"))));
        this.addRenderableWidget(new PanelButton(this.panelLeft + this.panelWidth - 28, headerY, 18, 20,
                Component.literal("×"), PanelButton.Variant.DANGER, button -> this.onClose()));

        int fieldsX = this.panelLeft + (this.panelWidth - 280) / 2;
        int fieldsY = this.panelTop + 190;
        this.priceBox = new net.minecraft.client.gui.components.EditBox(this.font, fieldsX, fieldsY, 128, 20,
                Component.translatable("gui.avecoins.player_market.price"));
        this.priceBox.setMaxLength(4);
        this.priceBox.setValue(this.savedPrice);
        this.priceBox.setHint(Component.translatable("gui.avecoins.player_market.price_hint"));
        this.addRenderableWidget(this.priceBox);
        this.priceBox.setResponder(value -> {
            this.savedPrice = value;
            if (this.confirmingListing) {
                this.confirmingListing = false;
                this.resultCode = "";
                this.rebuildWidgets();
            }
        });

        this.quantityBox = new net.minecraft.client.gui.components.EditBox(this.font, fieldsX + 152, fieldsY, 128, 20,
                Component.translatable("gui.avecoins.player_market.quantity"));
        this.quantityBox.setMaxLength(3);
        this.quantityBox.setValue(this.savedQuantity);
        this.quantityBox.setHint(Component.translatable("gui.avecoins.player_market.quantity_hint"));
        this.addRenderableWidget(this.quantityBox);
        this.quantityBox.setResponder(value -> {
            this.savedQuantity = value;
            if (this.confirmingListing) {
                this.confirmingListing = false;
                this.resultCode = "";
                this.rebuildWidgets();
            }
        });

        int[] durationOptions = {0, 1, 3, 5};
        String[] durationLabels = {
                "gui.avecoins.player_market.duration_option_one",
                "gui.avecoins.player_market.duration_option_two",
                "gui.avecoins.player_market.duration_option_four",
                "gui.avecoins.player_market.duration_option_six"
        };
        int durationButtonWidth = 88;
        int durationGap = 4;
        int durationRowWidth = durationButtonWidth * durationOptions.length + durationGap * (durationOptions.length - 1);
        int durationStartX = this.panelLeft + (this.panelWidth - durationRowWidth) / 2;
        for (int i = 0; i < durationOptions.length; i++) {
            final int optionExtraDays = durationOptions[i];
            long optionFee = PlayerMarketService.extensionCost(optionExtraDays);
            PanelButton option = new PanelButton(durationStartX + i * (durationButtonWidth + durationGap),
                    this.panelTop + 230, durationButtonWidth, 20,
                    Component.translatable(durationLabels[i]),
                    this.extraDays == optionExtraDays ? PanelButton.Variant.SUCCESS : PanelButton.Variant.SECONDARY,
                    button -> {
                        this.extraDays = optionExtraDays;
                        this.confirmingListing = false;
                        this.resultCode = "";
                        this.rebuildWidgets();
                    }).withTrailingItemAmount(COIN_ID, optionFee);
            Component durationHelp = optionFee > this.snapshot.walletBalance
                    ? Component.translatable("gui.avecoins.player_market.duration_shortfall_tooltip",
                            optionFee - this.snapshot.walletBalance, optionFee, this.snapshot.walletBalance)
                    : Component.translatable("gui.avecoins.player_market.duration_help",
                            optionFee, this.snapshot.walletBalance);
            this.addRenderableWidget(option);
        }

        long extensionFee = PlayerMarketService.extensionCost(this.extraDays);

        int createX = this.panelLeft + (this.panelWidth - 230) / 2;
        PanelButton create = new PanelButton(createX, this.panelTop + 320, 230, 25,
                Component.translatable(this.confirmingListing
                        ? "gui.avecoins.player_market.confirm_listing_button"
                        : "gui.avecoins.player_market.create"), PanelButton.Variant.SUCCESS,
                button -> this.createListing());
        this.addRenderableWidget(create);
        if (this.confirmingListing) {
            PanelButton cancelConfirmation = new PanelButton(createX + 236, this.panelTop + 320, 90, 25,
                    Component.translatable("gui.avecoins.player_market.cancel_confirmation"),
                    PanelButton.Variant.SECONDARY, button -> {
                        this.confirmingListing = false;
                        this.resultCode = "";
                        this.rebuildWidgets();
                    });
            this.addRenderableWidget(cancelConfirmation);
        }
    }

    private void createListing() {
        this.savedPrice = this.priceBox.getValue();
        this.savedQuantity = this.quantityBox.getValue();
        if (this.minecraft.player == null || this.selectedSlot < 0 || this.selectedSlot >= 36) {
            this.resultCode = "market_select_item";
            this.rebuildWidgets();
            return;
        }

        ItemStack selected = this.minecraft.player.getInventory().getItem(this.selectedSlot);
        if (selected.isEmpty()) {
            this.resultCode = "market_select_item";
            this.rebuildWidgets();
            return;
        }

        try {
            int price = Integer.parseInt(this.savedPrice.trim());
            int quantity = Integer.parseInt(this.savedQuantity.trim());
            if (price < 1 || price > 1728 || quantity < 1 || quantity > 999) {
                this.resultCode = "market_invalid_price_quantity";
                this.rebuildWidgets();
                return;
            }
            if (quantity > selected.getCount()) {
                this.resultCode = "market_not_enough_items";
                this.rebuildWidgets();
                return;
            }
            long extensionFee = PlayerMarketService.extensionCost(this.extraDays);
            if (extensionFee > this.snapshot.walletBalance) {
                this.confirmingListing = false;
                this.resultCode = "market_extension_insufficient";
                this.rebuildWidgets();
                return;
            }
            if (!this.confirmingListing) {
                this.confirmingListing = true;
                this.resultCode = "market_confirm_listing";
                this.rebuildWidgets();
                return;
            }
            // Price is the total amount for the selected quantity, so a stack of 64 can cost 1 VMcoin.
            com.google.gson.JsonObject payload = new com.google.gson.JsonObject();
            payload.addProperty("slot", this.selectedSlot);
            payload.addProperty("price", price);
            payload.addProperty("quantity", quantity);
            payload.addProperty("extraDays", this.extraDays);
            ClientNetworking.send(new ServerboundAvecoinsPayload("market_create_listing", payload.toString()));
        } catch (NumberFormatException exception) {
            this.confirmingListing = false;
            this.resultCode = "market_invalid_price_quantity";
            this.rebuildWidgets();
        }
    }

    private static int parsePositiveOrDefault(String value, int fallback) {
        try {
            int parsed = Integer.parseInt(value == null ? "" : value.trim());
            return parsed > 0 ? parsed : fallback;
        } catch (NumberFormatException ignored) {
            return fallback;
        }
    }

    @Override
    public boolean mouseClicked(MouseButtonEvent event, boolean doubled) {
        if (event.buttonInfo().button() == 0) {
            int mouseX = this.virtualMouseX(event.x());
            int mouseY = this.virtualMouseY(event.y());
            int inventoryX = this.inventoryX();
            int mainTop = this.mainInventoryTop();
            int hotbarTop = this.hotbarTop();
            if (mouseX >= inventoryX && mouseX < inventoryX + INVENTORY_WIDTH) {
                int column = (mouseX - inventoryX) / 18;
                if (mouseY >= mainTop && mouseY < mainTop + 3 * 18) {
                    int row = (mouseY - mainTop) / 18;
                    int slot = 9 + row * 9 + column;
                    if (this.minecraft.player != null && !this.minecraft.player.getInventory().getItem(slot).isEmpty()) {
                        this.selectedSlot = slot;
                        this.confirmingListing = false;
                        this.resultCode = "";
                        return true;
                    }
                } else if (mouseY >= hotbarTop && mouseY < hotbarTop + 18) {
                    int slot = column;
                    if (this.minecraft.player != null && !this.minecraft.player.getInventory().getItem(slot).isEmpty()) {
                        this.selectedSlot = slot;
                        this.confirmingListing = false;
                        this.resultCode = "";
                        return true;
                    }
                }
            }
        }
        return super.mouseClicked(event, doubled);
    }

    private int inventoryX() {
        return this.panelLeft + (this.panelWidth - INVENTORY_WIDTH) / 2;
    }

    private int mainInventoryTop() {
        return this.panelTop + 86;
    }

    private int hotbarTop() {
        return this.panelTop + 142;
    }

    @Override
    protected void renderPanel(GuiGraphicsExtractor graphics, int mouseX, int mouseY, float partialTick) {
        this.renderHeader(graphics, Component.translatable("gui.avecoins.player_market.create_subtitle"));
        this.drawSectionTitleAt(graphics, Component.translatable("gui.avecoins.player_market.inventory"),
                this.panelLeft + this.panelWidth / 2, this.panelTop + 68);

        int inventoryX = this.inventoryX();
        int mainTop = this.mainInventoryTop();
        int hotbarTop = this.hotbarTop();

        for (int row = 0; row < 3; row++) {
            for (int column = 0; column < 9; column++) {
                int slot = 9 + row * 9 + column;
                drawInventorySlot(graphics, slot, inventoryX + column * 18, mainTop + row * 18);
            }
        }
        for (int column = 0; column < 9; column++) {
            drawInventorySlot(graphics, column, inventoryX + column * 18, hotbarTop);
        }

        int fieldsX = this.panelLeft + (this.panelWidth - 280) / 2;
        Component priceLabel = Component.translatable("gui.avecoins.player_market.price");
        int priceLabelWidth = this.font.width(priceLabel);
        boolean hasCoin = BuiltInRegistries.ITEM.containsKey(COIN_ID);
        int priceGroupWidth = priceLabelWidth + (hasCoin ? 20 : 0);
        int priceLabelX = fieldsX + (128 - priceGroupWidth) / 2;
        graphics.text(this.font, priceLabel, priceLabelX, this.panelTop + 176,
                this.theme().TEXT, !this.theme().custom);
        if (hasCoin) {
            graphics.item(new ItemStack((Item) BuiltInRegistries.ITEM.getValue(COIN_ID)),
                    priceLabelX + priceLabelWidth + 4, this.panelTop + 172);
        }
        GuiTextLayout.drawCentered(graphics, this.font, Component.translatable("gui.avecoins.player_market.quantity"),
                fieldsX + 152 + 64, this.panelTop + 176, this.theme().TEXT, !this.theme().custom);

        this.drawSectionTitleAt(graphics, Component.translatable("gui.avecoins.player_market.duration_heading"),
                this.panelLeft + this.panelWidth / 2, this.panelTop + 218);
        this.renderVmcoinBalance(graphics, this.panelLeft + this.panelWidth / 2, this.panelTop + 260);
        long extensionFee = PlayerMarketService.extensionCost(this.extraDays);
        int feeColor = extensionFee > this.snapshot.walletBalance ? this.theme().DANGER : this.theme().SUCCESS;
        this.renderDurationFeeSummary(graphics, this.panelLeft + this.panelWidth / 2,
                this.panelTop + 281, extensionFee, Math.max(0L, this.snapshot.walletBalance - extensionFee),
                feeColor);

        this.drawCenteredWrappedAt(graphics, Component.translatable("gui.avecoins.player_market.limit"),
                this.panelLeft + this.panelWidth / 2, this.panelTop + 356, 640, this.theme().MUTED);

        for (int slot = 0; slot < 36; slot++) {
            int x;
            int y;
            if (slot < 9) {
                x = inventoryX + slot * 18;
                y = hotbarTop;
            } else {
                int local = slot - 9;
                x = inventoryX + local % 9 * 18;
                y = mainTop + local / 9 * 18;
            }
            if (mouseX >= x && mouseX < x + 16 && mouseY >= y && mouseY < y + 16
                    && this.minecraft.player != null) {
                ItemStack hovered = this.minecraft.player.getInventory().getItem(slot);
                if (!hovered.isEmpty()) {
                    graphics.setTooltipForNextFrame(this.font, hovered, mouseX, mouseY);
                }
                break;
            }
        }

        if (!this.resultCode.isBlank()) {
            this.renderStatusBoxAt(graphics, this.resultCode, this.panelTop + this.panelHeight - 24);
        }
    }

    private String durationLabel() {
        long cost = PlayerMarketService.extensionCost(this.extraDays);
        return Component.translatable("gui.avecoins.player_market.duration", this.extraDays + 1, cost).getString();
    }

    private void renderVmcoinBalance(GuiGraphicsExtractor graphics, int centerX, int iconY) {
        Component label = Component.translatable("gui.avecoins.player_market.wallet_label");
        String amount = Long.toString(this.snapshot.walletBalance);
        Identifier currencyId = Identifier.tryParse("vmtrade:vmcoin");
        boolean hasIcon = currencyId != null && BuiltInRegistries.ITEM.containsKey(currencyId);
        int iconSpace = hasIcon ? 20 : 0;
        int totalWidth = this.font.width(label) + 8 + iconSpace + this.font.width(amount);
        int x = centerX - totalWidth / 2;
        graphics.text(this.font, label, x, iconY + 4, this.theme().TEXT, !this.theme().custom);
        x += this.font.width(label) + 8;
        if (hasIcon) {
            graphics.item(new ItemStack((Item) BuiltInRegistries.ITEM.getValue(currencyId)), x, iconY);
            x += 20;
        }
        graphics.text(this.font, Component.literal(amount), x, iconY + 4, this.theme().TEXT, !this.theme().custom);
    }

    private void renderDurationFeeSummary(GuiGraphicsExtractor graphics, int centerX, int y,
                                          long fee, long balanceAfterPayment, int color) {
        Component feeLabel = Component.translatable("gui.avecoins.player_market.duration_fee_label");
        Component balanceLabel = Component.translatable("gui.avecoins.player_market.balance_after_fee_label");
        String feeAmount = Long.toString(fee);
        String balanceAmount = Long.toString(balanceAfterPayment);
        boolean hasCoin = BuiltInRegistries.ITEM.containsKey(COIN_ID);
        int iconWidth = hasCoin ? 19 : 0;
        String separator = "  ·  ";
        int feeWidth = this.font.width(feeLabel) + 5 + this.font.width(feeAmount) + iconWidth;
        int balanceWidth = this.font.width(balanceLabel) + 5 + this.font.width(balanceAmount) + iconWidth;
        int x = centerX - (feeWidth + this.font.width(separator) + balanceWidth) / 2;

        graphics.text(this.font, feeLabel, x, y, color, !this.theme().custom);
        x += this.font.width(feeLabel) + 5;
        x = this.drawCoinAmount(graphics, feeAmount, x, y, color, hasCoin);
        graphics.text(this.font, Component.literal(separator), x, y, this.theme().MUTED, !this.theme().custom);
        x += this.font.width(separator);
        graphics.text(this.font, balanceLabel, x, y, color, !this.theme().custom);
        x += this.font.width(balanceLabel) + 5;
        this.drawCoinAmount(graphics, balanceAmount, x, y, color, hasCoin);
    }

    private int drawCoinAmount(GuiGraphicsExtractor graphics, String amount, int x, int y,
                               int color, boolean hasCoin) {
        graphics.text(this.font, Component.literal(amount), x, y, color, !this.theme().custom);
        x += this.font.width(amount) + 3;
        if (hasCoin) {
            graphics.item(new ItemStack((Item) BuiltInRegistries.ITEM.getValue(COIN_ID)), x, y - 4);
            x += 16;
        }
        return x;
    }

    private void drawInventorySlot(GuiGraphicsExtractor graphics, int slot, int x, int y) {
        boolean selected = slot == this.selectedSlot;
        graphics.fill(x - 1, y - 1, x + 17, y + 17, selected ? this.theme().SUCCESS : this.theme().BORDER);
        graphics.fill(x, y, x + 16, y + 16, selected ? this.theme().SURFACE_SELECTED : this.theme().SURFACE);
        if (this.minecraft.player != null) {
            ItemStack stack = this.minecraft.player.getInventory().getItem(slot);
            if (!stack.isEmpty()) {
                graphics.item(stack, x, y);
                if (stack.getCount() > 1) {
                    String count = Integer.toString(stack.getCount());
                    graphics.text(this.font, Component.literal(count),
                            x + 17 - this.font.width(count), y + 8, 0xFFFFFFFF, true);
                }
            }
        }
    }
}
