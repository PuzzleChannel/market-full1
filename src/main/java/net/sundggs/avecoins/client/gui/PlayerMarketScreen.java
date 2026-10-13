package net.sundggs.avecoins.client.gui;

import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.Identifier;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.network.chat.Component;
import net.sundggs.avecoins.network.ClientNetworking;
import net.sundggs.avecoins.network.ServerboundAvecoinsPayload;
import net.sundggs.avecoins.shop.PlayerMarketService;
import net.sundggs.avecoins.shop.ShopService;

/** Player-to-player market overview. Listing management is separated into PlayerMarketCreateScreen. */
public final class PlayerMarketScreen extends AvecoinsScreen {
    private static final int LISTINGS_PER_PAGE = 5;
    private final PlayerMarketService.MarketSnapshot snapshot;
    private String resultCode;
    private int page;

    public PlayerMarketScreen(PlayerMarketService.MarketSnapshot snapshot, String resultCode) {
        this(snapshot, resultCode, 0);
    }

    private PlayerMarketScreen(PlayerMarketService.MarketSnapshot snapshot, String resultCode, int page) {
        super(Component.translatable("gui.avecoins.player_market.title"));
        this.snapshot = snapshot;
        this.resultCode = resultCode == null ? "" : resultCode;
        this.page = Math.max(0, page);
        ShopService.ShopSnapshot themeSnapshot = new ShopService.ShopSnapshot();
        themeSnapshot.shopColor = snapshot.shopColor;
        this.useShopTheme(themeSnapshot);
    }

    public PlayerMarketScreen refreshed(PlayerMarketService.MarketSnapshot snapshot, String resultCode) {
        return new PlayerMarketScreen(snapshot, resultCode, this.page);
    }

    @Override
    protected void init() {
        this.layoutPanel(820, 440);
        int headerY = this.panelTop + 7;
        this.addRenderableWidget(new PanelButton(this.panelLeft + 10, headerY, 92, 20,
                Component.translatable("gui.avecoins.player_market.back"), PanelButton.Variant.SECONDARY,
                button -> ClientNetworking.send(new ServerboundAvecoinsPayload("request_shop", "{}"))));

        int closeX = this.panelLeft + this.panelWidth - 28;
        this.addRenderableWidget(new PanelButton(closeX, headerY, 18, 20,
                Component.literal("×"), PanelButton.Variant.DANGER, button -> this.onClose()));

        int refreshX = closeX - 88 - 8;
        this.addRenderableWidget(new PanelButton(refreshX, headerY, 88, 20,
                Component.translatable("gui.avecoins.player_market.refresh"), PanelButton.Variant.HEADER,
                button -> ClientNetworking.send(new ServerboundAvecoinsPayload("request_player_market", "{}"))));

        int createX = refreshX - 154 - 8;
        this.addRenderableWidget(new PanelButton(createX, headerY, 154, 20,
                Component.translatable("gui.avecoins.player_market.list_item"), PanelButton.Variant.SUCCESS,
                button -> this.minecraft.gui.setScreen(new PlayerMarketCreateScreen(this.snapshot, ""))));

        int maxPage = Math.max(0, (this.snapshot.listings.size() - 1) / LISTINGS_PER_PAGE);
        this.page = Math.min(this.page, maxPage);
        int listX = this.panelLeft + 12;
        int rowWidth = this.panelWidth - 24;
        int listTop = this.panelTop + 78;
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
                        String json = "{\\"id\\":\\"" + listing.id + "\\"}";
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
    }

    @Override
    protected void renderPanel(GuiGraphicsExtractor graphics, int mouseX, int mouseY, float partialTick) {
        this.renderHeader(graphics, Component.translatable("gui.avecoins.player_market.subtitle"));
        int listX = this.panelLeft + 12;
        int rowWidth = this.panelWidth - 24;
        int listTop = this.panelTop + 78;
        this.drawSectionTitleAt(graphics, Component.translatable("gui.avecoins.player_market.listings"),
                listX + rowWidth / 2, this.panelTop + 62);

        int start = this.page * LISTINGS_PER_PAGE;
        int end = Math.min(this.snapshot.listings.size(), start + LISTINGS_PER_PAGE);
        if (this.snapshot.listings.isEmpty()) {
            this.drawCenteredWrappedAt(graphics, Component.translatable("gui.avecoins.player_market.empty"),
                    listX + rowWidth / 2, listTop + 30, rowWidth - 48, this.theme().MUTED);
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

            String title = this.font.plainSubstrByWidth(listing.displayName + " ×" + listing.quantity, 480);
            graphics.text(this.font, Component.literal(title), listX + 31, rowY + 8,
                    this.theme().TEXT, !this.theme().custom);
            String seller = listing.sellerName + (listing.sellerId.equals(this.snapshot.viewerId) ? " · Вы" : "");
            String details = seller + "  |  " + listing.unitPrice + " ВМуткоин/шт.  |  Итого: "
                    + ((long) listing.unitPrice * listing.quantity);
            graphics.text(this.font, Component.literal(this.font.plainSubstrByWidth(details, rowWidth - 140)),
                    listX + 31, rowY + 27, this.theme().MUTED, !this.theme().custom);
        }

        int maxPage = Math.max(0, (this.snapshot.listings.size() - 1) / LISTINGS_PER_PAGE);
        if (maxPage > 0) {
            GuiTextLayout.drawCentered(graphics, this.font,
                    Component.translatable("gui.avecoins.shop.page", this.page + 1, maxPage + 1),
                    listX + rowWidth / 2, this.panelTop + 378, this.theme().MUTED, !this.theme().custom);
        }

        graphics.text(this.font, Component.translatable("gui.avecoins.player_market.wallet", this.snapshot.walletBalance),
                this.panelLeft + 14, this.panelTop + 390, this.theme().TEXT, !this.theme().custom);
        if (!this.resultCode.isBlank()) {
            this.renderStatusBoxAt(graphics, this.resultCode, this.panelTop + this.panelHeight - 24);
        }
    }
}
