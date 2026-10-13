package net.sundggs.avecoins.client.gui;

import java.util.List;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.Identifier;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.network.chat.Component;
import net.sundggs.avecoins.client.gui.AvecoinsScreen;
import net.sundggs.avecoins.client.gui.PanelButton;
import net.sundggs.avecoins.network.ClientNetworking;
import net.sundggs.avecoins.network.ServerboundAvecoinsPayload;
import net.sundggs.avecoins.shop.PlayerMarketService;
import net.sundggs.avecoins.shop.ShopService;

/** Separate player-to-player market tab. Listing and payment decisions are server-authoritative. */
public final class PlayerMarketScreen extends AvecoinsScreen {
    private static final int LISTINGS_PER_PAGE = 5;
    private final PlayerMarketService.MarketSnapshot snapshot;
    private String resultCode;
    private String savedPrice;
    private String savedQuantity;
    private int page;
    private EditBox priceBox;
    private EditBox quantityBox;

    public PlayerMarketScreen(PlayerMarketService.MarketSnapshot snapshot, String resultCode) {
        this(snapshot, resultCode, "1", "1", 0);
    }

    private PlayerMarketScreen(PlayerMarketService.MarketSnapshot snapshot, String resultCode,
                               String savedPrice, String savedQuantity, int page) {
        super(Component.translatable("gui.avecoins.player_market.title"));
        this.snapshot = snapshot;
        this.resultCode = resultCode == null ? "" : resultCode;
        this.savedPrice = savedPrice == null ? "1" : savedPrice;
        this.savedQuantity = savedQuantity == null ? "1" : savedQuantity;
        this.page = Math.max(0, page);
        ShopService.ShopSnapshot themeSnapshot = new ShopService.ShopSnapshot();
        themeSnapshot.shopColor = snapshot.shopColor;
        this.useShopTheme(themeSnapshot);
    }

    public PlayerMarketScreen refreshed(PlayerMarketService.MarketSnapshot snapshot, String resultCode) {
        return new PlayerMarketScreen(snapshot, resultCode,
                this.priceBox == null ? this.savedPrice : this.priceBox.getValue(),
                this.quantityBox == null ? this.savedQuantity : this.quantityBox.getValue(),
                this.page);
    }

    @Override
    protected void init() {
        this.layoutPanel(820, 440);
        int headerY = this.panelTop + 7;
        this.addRenderableWidget(new PanelButton(this.panelLeft + 10, headerY, 92, 20,
                Component.translatable("gui.avecoins.player_market.back"), PanelButton.Variant.SECONDARY,
                button -> ClientNetworking.send(new ServerboundAvecoinsPayload("request_shop", "{}"))));
        this.addRenderableWidget(new PanelButton(this.panelLeft + this.panelWidth - 112, headerY, 100, 20,
                Component.translatable("gui.avecoins.player_market.refresh"), PanelButton.Variant.HEADER,
                button -> ClientNetworking.send(new ServerboundAvecoinsPayload("request_player_market", "{}"))));
        this.addRenderableWidget(new PanelButton(this.panelLeft + this.panelWidth - 28, headerY, 18, 20,
                Component.literal("×"), PanelButton.Variant.DANGER, button -> this.onClose()));

        int maxPage = Math.max(0, (this.snapshot.listings.size() - 1) / LISTINGS_PER_PAGE);
        this.page = Math.min(this.page, maxPage);
        int listX = this.panelLeft + 12;
        int rowWidth = 512;
        int listTop = this.panelTop + 80;
        int start = this.page * LISTINGS_PER_PAGE;
        int end = Math.min(this.snapshot.listings.size(), start + LISTINGS_PER_PAGE);
        for (int index = start; index < end; index++) {
            PlayerMarketService.MarketListing listing = this.snapshot.listings.get(index);
            int rowY = listTop + (index - start) * 58;
            boolean ownListing = listing.sellerId.equals(this.snapshot.viewerId);
            long total = (long) listing.quantity * (long) listing.unitPrice;
            PanelButton action = new PanelButton(listX + rowWidth - 92, rowY + 16, 84, 20,
                    Component.translatable(ownListing ? "gui.avecoins.player_market.cancel" : "gui.avecoins.player_market.buy"),
                    ownListing ? PanelButton.Variant.DANGER : PanelButton.Variant.SUCCESS,
                    button -> {
                        String json = "{\"id\":\"" + listing.id + "\"}";
                        ClientNetworking.send(new ServerboundAvecoinsPayload(ownListing ? "market_cancel" : "market_buy", json));
                    });
            if (!ownListing && this.snapshot.walletBalance < total) {
                action.active = false;
                action.setTooltip(net.minecraft.client.gui.components.Tooltip.create(
                        Component.translatable("gui.avecoins.player_market.need_money")));
            }
            this.addRenderableWidget(action);
        }

        if (maxPage > 0) {
            int center = listX + rowWidth / 2;
            int navY = this.panelTop + 374;
            PanelButton previous = new PanelButton(center - 60, navY, 24, 18, Component.literal("‹"),
                    PanelButton.Variant.SECONDARY, button -> {
                        this.page = Math.max(0, this.page - 1);
                        this.rebuildWidgets();
                    });
            previous.active = this.page > 0;
            this.addRenderableWidget(previous);
            PanelButton next = new PanelButton(center + 36, navY, 24, 18, Component.literal("›"),
                    PanelButton.Variant.SECONDARY, button -> {
                        this.page = Math.min(maxPage, this.page + 1);
                        this.rebuildWidgets();
                    });
            next.active = this.page < maxPage;
            this.addRenderableWidget(next);
        }

        int formX = this.panelLeft + 542;
        this.priceBox = new EditBox(this.font, formX, this.panelTop + 148, 246, 20,
                Component.translatable("gui.avecoins.player_market.price"));
        this.priceBox.setMaxLength(6);
        this.priceBox.setValue(this.savedPrice);
        this.priceBox.setHint(Component.literal("Например, 12"));
        this.addRenderableWidget(this.priceBox);

        this.quantityBox = new EditBox(this.font, formX, this.panelTop + 204, 246, 20,
                Component.translatable("gui.avecoins.player_market.quantity"));
        this.quantityBox.setMaxLength(3);
        this.quantityBox.setValue(this.savedQuantity);
        this.quantityBox.setHint(Component.literal("Например, 16"));
        this.addRenderableWidget(this.quantityBox);

        this.addRenderableWidget(new PanelButton(formX, this.panelTop + 250, 246, 24,
                Component.translatable("gui.avecoins.player_market.create"), PanelButton.Variant.SUCCESS,
                button -> this.createListing()));
    }

    private void createListing() {
        this.savedPrice = this.priceBox.getValue();
        this.savedQuantity = this.quantityBox.getValue();
        try {
            int price = Integer.parseInt(this.savedPrice.trim());
            int quantity = Integer.parseInt(this.savedQuantity.trim());
            if (price < 1 || price > 1728 || quantity < 1 || quantity > 999) {
                this.resultCode = "market_invalid_price_quantity";
                this.rebuildWidgets();
                return;
            }
            ClientNetworking.send(new ServerboundAvecoinsPayload("market_create_listing",
                    "{\"price\":" + price + ",\"quantity\":" + quantity + "}"));
        } catch (NumberFormatException exception) {
            this.resultCode = "market_invalid_price_quantity";
            this.rebuildWidgets();
        }
    }

    @Override
    protected void renderPanel(GuiGraphicsExtractor graphics, int mouseX, int mouseY, float partialTick) {
        this.renderHeader(graphics, Component.translatable("gui.avecoins.player_market.subtitle"));
        int listX = this.panelLeft + 12;
        int rowWidth = 512;
        int listTop = this.panelTop + 80;
        this.drawSectionTitleAt(graphics, Component.translatable("gui.avecoins.player_market.listings"),
                listX + rowWidth / 2, this.panelTop + 64);
        this.drawSectionTitleAt(graphics, Component.translatable("gui.avecoins.player_market.new_listing"),
                this.panelLeft + 665, this.panelTop + 64);

        int start = this.page * LISTINGS_PER_PAGE;
        int end = Math.min(this.snapshot.listings.size(), start + LISTINGS_PER_PAGE);
        if (this.snapshot.listings.isEmpty()) {
            this.drawCenteredWrappedAt(graphics, Component.translatable("gui.avecoins.player_market.empty"),
                    listX + rowWidth / 2, listTop + 28, rowWidth - 32, this.theme().MUTED);
        }
        for (int index = start; index < end; index++) {
            PlayerMarketService.MarketListing listing = this.snapshot.listings.get(index);
            int rowY = listTop + (index - start) * 58;
            graphics.fill(listX, rowY, listX + rowWidth - 98, rowY + 52, this.theme().BORDER);
            graphics.fill(listX + 1, rowY + 1, listX + rowWidth - 99, rowY + 51, this.theme().PANEL);
            Identifier itemId = Identifier.tryParse(listing.itemId);
            if (itemId != null && BuiltInRegistries.ITEM.containsKey(itemId)) {
                Item item = (Item) BuiltInRegistries.ITEM.getValue(itemId);
                graphics.item(new ItemStack(item), listX + 8, rowY + 9);
            }
            String title = this.font.plainSubstrByWidth(listing.displayName + " ×" + listing.quantity, 270);
            graphics.text(this.font, Component.literal(title), listX + 31, rowY + 8,
                    this.theme().TEXT, !this.theme().custom);
            String seller = listing.sellerName + (listing.sellerId.equals(this.snapshot.viewerId) ? " · Вы" : "");
            String details = seller + "  |  " + listing.unitPrice + " ВМуткоин/шт.  |  Итого: "
                    + ((long) listing.unitPrice * listing.quantity);
            graphics.text(this.font, Component.literal(this.font.plainSubstrByWidth(details, 380)),
                    listX + 31, rowY + 26, this.theme().MUTED, !this.theme().custom);
        }

        int formX = this.panelLeft + 542;
        int formRight = this.panelLeft + this.panelWidth - 12;
        graphics.fill(formX - 9, this.panelTop + 78, formX - 8, this.panelTop + 360, this.theme().DIVIDER);
        this.drawWrapped(graphics, Component.translatable("gui.avecoins.player_market.hold_item"),
                formX, this.panelTop + 91, 240, this.theme().MUTED);
        graphics.text(this.font, Component.translatable("gui.avecoins.player_market.price"),
                formX, this.panelTop + 134, this.theme().TEXT, !this.theme().custom);
        graphics.text(this.font, Component.translatable("gui.avecoins.player_market.quantity"),
                formX, this.panelTop + 190, this.theme().TEXT, !this.theme().custom);
        this.drawWrapped(graphics, Component.translatable("gui.avecoins.player_market.limit"),
                formX, this.panelTop + 286, 242, this.theme().MUTED);
        graphics.text(this.font, Component.translatable("gui.avecoins.player_market.wallet", this.snapshot.walletBalance),
                formX, this.panelTop + 327, this.theme().TEXT, !this.theme().custom);

        int maxPage = Math.max(0, (this.snapshot.listings.size() - 1) / LISTINGS_PER_PAGE);
        if (maxPage > 0) {
            GuiTextLayout.drawCentered(graphics, this.font,
                    Component.translatable("gui.avecoins.shop.page", this.page + 1, maxPage + 1),
                    listX + rowWidth / 2, this.panelTop + 378, this.theme().MUTED, !this.theme().custom);
        }
        if (!this.resultCode.isBlank()) {
            this.renderStatusBoxAt(graphics, this.resultCode, this.panelTop + this.panelHeight - 24);
        }
    }
}
