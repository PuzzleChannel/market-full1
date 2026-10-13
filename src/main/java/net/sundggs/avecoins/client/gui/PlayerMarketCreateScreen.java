package net.sundggs.avecoins.client.gui;

import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.input.MouseButtonEvent;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.ItemStack;
import net.sundggs.avecoins.network.ClientNetworking;
import net.sundggs.avecoins.network.ServerboundAvecoinsPayload;
import net.sundggs.avecoins.shop.PlayerMarketService;
import net.sundggs.avecoins.shop.ShopService;

/** Inventory-based player listing editor. Items are only removed after server-side validation. */
public final class PlayerMarketCreateScreen extends AvecoinsScreen {
    private final PlayerMarketService.MarketSnapshot snapshot;
    private String resultCode;
    private String savedPrice;
    private String savedQuantity;
    private int selectedSlot;
    private net.minecraft.client.gui.components.EditBox priceBox;
    private net.minecraft.client.gui.components.EditBox quantityBox;

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

        int formX = this.panelLeft + 242;
        this.priceBox = new net.minecraft.client.gui.components.EditBox(this.font, formX, this.panelTop + 174, 244, 20,
                Component.translatable("gui.avecoins.player_market.price"));
        this.priceBox.setMaxLength(6);
        this.priceBox.setValue(this.savedPrice);
        this.priceBox.setHint(Component.literal("Например, 12"));
        this.addRenderableWidget(this.priceBox);

        this.quantityBox = new net.minecraft.client.gui.components.EditBox(this.font, formX, this.panelTop + 222, 244, 20,
                Component.translatable("gui.avecoins.player_market.quantity"));
        this.quantityBox.setMaxLength(3);
        this.quantityBox.setValue(this.savedQuantity);
        this.quantityBox.setHint(Component.literal("Например, 16"));
        this.addRenderableWidget(this.quantityBox);

        this.addRenderableWidget(new PanelButton(formX, this.panelTop + 278, 244, 25,
                Component.translatable("gui.avecoins.player_market.create"), PanelButton.Variant.SUCCESS,
                button -> this.createListing()));
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
            if ((long) price * quantity > 1728L) {
                this.resultCode = "market_total_too_high";
                this.rebuildWidgets();
                return;
            }
            ClientNetworking.send(new ServerboundAvecoinsPayload("market_create_listing",
                    "{\\"slot\\":" + this.selectedSlot + ",\\"price\\":" + price + ",\\"quantity\\":" + quantity + "}"));
        } catch (NumberFormatException exception) {
            this.resultCode = "market_invalid_price_quantity";
            this.rebuildWidgets();
        }
    }

    @Override
    public boolean mouseClicked(MouseButtonEvent event, boolean doubled) {
        if (event.buttonInfo().button() == 0) {
            int mouseX = this.virtualMouseX(event.x());
            int mouseY = this.virtualMouseY(event.y());
            int inventoryX = this.panelLeft + 24;
            int mainTop = this.panelTop + 126;
            int hotbarTop = this.panelTop + 190;
            if (mouseX >= inventoryX && mouseX < inventoryX + 9 * 18) {
                int column = (mouseX - inventoryX) / 18;
                if (mouseY >= mainTop && mouseY < mainTop + 3 * 18) {
                    int row = (mouseY - mainTop) / 18;
                    int slot = 9 + row * 9 + column;
                    if (this.minecraft.player != null && !this.minecraft.player.getInventory().getItem(slot).isEmpty()) {
                        this.selectedSlot = slot;
                        return true;
                    }
                } else if (mouseY >= hotbarTop && mouseY < hotbarTop + 18) {
                    int slot = column;
                    if (this.minecraft.player != null && !this.minecraft.player.getInventory().getItem(slot).isEmpty()) {
                        this.selectedSlot = slot;
                        return true;
                    }
                }
            }
        }
        return super.mouseClicked(event, doubled);
    }

    @Override
    protected void renderPanel(GuiGraphicsExtractor graphics, int mouseX, int mouseY, float partialTick) {
        this.renderHeader(graphics, Component.translatable("gui.avecoins.player_market.create_subtitle"));
        this.drawSectionTitleAt(graphics, Component.translatable("gui.avecoins.player_market.inventory"),
                this.panelLeft + 105, this.panelTop + 72);
        this.drawCenteredWrappedAt(graphics, Component.translatable("gui.avecoins.player_market.inventory_help"),
                this.panelLeft + 105, this.panelTop + 88, 190, this.theme().MUTED);

        int inventoryX = this.panelLeft + 24;
        int mainTop = this.panelTop + 126;
        int hotbarTop = this.panelTop + 190;
        for (int row = 0; row < 3; row++) {
            for (int column = 0; column < 9; column++) {
                int slot = 9 + row * 9 + column;
                drawInventorySlot(graphics, slot, inventoryX + column * 18, mainTop);
                drawInventorySlot(graphics, slot, inventoryX + column * 18, mainTop + row * 18);
            }
        }
        this.drawSectionTitleAt(graphics, Component.translatable("gui.avecoins.player_market.hotbar"),
                this.panelLeft + 105, this.panelTop + 174);
        for (int column = 0; column < 9; column++) {
            drawInventorySlot(graphics, column, inventoryX + column * 18, hotbarTop);
        }

        int dividerX = this.panelLeft + 210;
        graphics.fill(dividerX, this.panelTop + 78, dividerX + 1, this.panelTop + 360, this.theme().DIVIDER);

        int formX = this.panelLeft + 242;
        this.drawSectionTitleAt(graphics, Component.translatable("gui.avecoins.player_market.selected_item"),
                formX + 122, this.panelTop + 72);
        graphics.fill(formX, this.panelTop + 88, formX + 376, this.panelTop + 143, this.theme().BORDER);
        graphics.fill(formX + 1, this.panelTop + 89, formX + 375, this.panelTop + 142, this.theme().SURFACE);

        ItemStack selected = this.minecraft.player == null || this.selectedSlot < 0
                ? ItemStack.EMPTY : this.minecraft.player.getInventory().getItem(this.selectedSlot);
        if (!selected.isEmpty()) {
            graphics.item(selected, formX + 9, this.panelTop + 105);
            String name = this.font.plainSubstrByWidth(selected.getHoverName().getString(), 300);
            graphics.text(this.font, Component.literal(name), formX + 31, this.panelTop + 102,
                    this.theme().TEXT, !this.theme().custom);
            graphics.text(this.font, Component.translatable("gui.avecoins.player_market.selected_count", selected.getCount()),
                    formX + 31, this.panelTop + 119, this.theme().MUTED, !this.theme().custom);
        } else {
            this.drawWrapped(graphics, Component.translatable("gui.avecoins.player_market.no_selected_item"),
                    formX + 10, this.panelTop + 108, 350, this.theme().MUTED);
        }

        graphics.text(this.font, Component.translatable("gui.avecoins.player_market.price"),
                formX, this.panelTop + 158, this.theme().TEXT, !this.theme().custom);
        graphics.text(this.font, Component.translatable("gui.avecoins.player_market.quantity"),
                formX, this.panelTop + 206, this.theme().TEXT, !this.theme().custom);
        graphics.text(this.font, Component.translatable("gui.avecoins.player_market.wallet", this.snapshot.walletBalance),
                formX, this.panelTop + 255, this.theme().TEXT, !this.theme().custom);
        this.drawWrapped(graphics, Component.translatable("gui.avecoins.player_market.limit"),
                formX, this.panelTop + 314, 370, this.theme().MUTED);

        if (!this.resultCode.isBlank()) {
            this.renderStatusBoxAt(graphics, this.resultCode, this.panelTop + this.panelHeight - 24);
        }
    }

    private void drawInventorySlot(GuiGraphicsExtractor graphics, int slot, int x, int y) {
        boolean selected = slot == this.selectedSlot;
        graphics.fill(x - 1, y - 1, x + 17, y + 17, selected ? this.theme().SUCCESS : this.theme().BORDER);
        graphics.fill(x, y, x + 16, y + 16, selected ? this.theme().SURFACE_SELECTED : this.theme().SURFACE);
        if (this.minecraft.player != null) {
            ItemStack stack = this.minecraft.player.getInventory().getItem(slot);
            if (!stack.isEmpty()) graphics.item(stack, x, y);
        }
    }
}
