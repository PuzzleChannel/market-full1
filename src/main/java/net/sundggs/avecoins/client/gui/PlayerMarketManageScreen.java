package net.sundggs.avecoins.client.gui;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.Identifier;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.network.chat.Component;
import net.sundggs.avecoins.network.ClientNetworking;
import net.sundggs.avecoins.network.ServerboundAvecoinsPayload;
import net.sundggs.avecoins.shop.PlayerMarketService;
import net.sundggs.avecoins.shop.ShopService;

/** Lists the current player's escrowed items and allows safe price edits or cancellation. */
public final class PlayerMarketManageScreen extends AvecoinsScreen {
    private static final int PAGE_SIZE = 8;
    private static final int COLUMNS = 2;
    private static final int CARD_HEIGHT = 62;
    private static final int CARD_STEP = 66;

    private final PlayerMarketService.MarketSnapshot snapshot;
    private final Map<String, String> priceDrafts;
    private final Map<String, EditBox> priceBoxes = new HashMap<>();
    private String resultCode;
    private int page;

    public PlayerMarketManageScreen(PlayerMarketService.MarketSnapshot snapshot, String resultCode) {
        this(snapshot, resultCode, 0, Map.of());
    }

    private PlayerMarketManageScreen(PlayerMarketService.MarketSnapshot snapshot, String resultCode,
                                     int page, Map<String, String> priceDrafts) {
        super(Component.translatable("gui.avecoins.player_market.manage_title"));
        this.snapshot = snapshot;
        this.resultCode = resultCode == null ? "" : resultCode;
        this.page = Math.max(0, page);
        this.priceDrafts = new HashMap<>(priceDrafts);
        ShopService.ShopSnapshot themeSnapshot = new ShopService.ShopSnapshot();
        themeSnapshot.shopColor = snapshot.shopColor;
        this.useShopTheme(themeSnapshot);
    }

    public PlayerMarketManageScreen refreshed(PlayerMarketService.MarketSnapshot snapshot, String resultCode) {
        Map<String, String> drafts = new HashMap<>();
        this.priceBoxes.forEach((id, box) -> drafts.put(id, box.getValue()));
        return new PlayerMarketManageScreen(snapshot, resultCode, this.page, drafts);
    }

    private List<PlayerMarketService.MarketListing> ownListings() {
        return this.snapshot.listings.stream()
                .filter(listing -> listing.sellerId.equals(this.snapshot.viewerId))
                .toList();
    }

    @Override
    protected void init() {
        this.layoutPanel(820, 440);
        this.priceBoxes.clear();

        int headerY = this.panelTop + 7;
        this.addRenderableWidget(new PanelButton(this.panelLeft + 10, headerY, 112, 20,
                Component.translatable("gui.avecoins.player_market.back_to_market"), PanelButton.Variant.SECONDARY,
                button -> ClientNetworking.send(new ServerboundAvecoinsPayload("request_player_market", "{}"))));
        this.addRenderableWidget(new PanelButton(this.panelLeft + this.panelWidth - 28, headerY, 18, 20,
                Component.literal("×"), PanelButton.Variant.DANGER, button -> this.onClose()));

        List<PlayerMarketService.MarketListing> listings = this.ownListings();
        int maxPage = Math.max(0, (listings.size() - 1) / PAGE_SIZE);
        this.page = Math.min(this.page, maxPage);

        int listX = this.panelLeft + 12;
        int rowWidth = this.panelWidth - 24;
        int cardWidth = (rowWidth - 8) / COLUMNS;
        int listTop = this.panelTop + 68;
        int start = this.page * PAGE_SIZE;
        int end = Math.min(listings.size(), start + PAGE_SIZE);

        for (int index = start; index < end; index++) {
            PlayerMarketService.MarketListing listing = listings.get(index);
            int local = index - start;
            int cardX = listX + (local % COLUMNS) * (cardWidth + 8);
            int cardY = listTop + (local / COLUMNS) * CARD_STEP;

            EditBox priceBox = new EditBox(this.font, cardX + 43, cardY + 37, 70, 18,
                    Component.translatable("gui.avecoins.player_market.price"));
            priceBox.setMaxLength(4);
            priceBox.setValue(this.priceDrafts.getOrDefault(listing.id, Integer.toString(listing.unitPrice)));
            this.priceBoxes.put(listing.id, priceBox);
            this.addRenderableWidget(priceBox);

            this.addRenderableWidget(new PanelButton(cardX + 120, cardY + 37, 83, 18,
                    Component.translatable("gui.avecoins.player_market.save_price"), PanelButton.Variant.SUCCESS,
                    button -> this.savePrice(listing)));
            this.addRenderableWidget(new PanelButton(cardX + 210, cardY + 37, 70, 18,
                    Component.translatable("gui.avecoins.player_market.cancel"), PanelButton.Variant.DANGER,
                    button -> {
                        com.google.gson.JsonObject payload = new com.google.gson.JsonObject();
                        payload.addProperty("id", listing.id);
                        ClientNetworking.send(new ServerboundAvecoinsPayload("market_cancel", payload.toString()));
                    }));
        }

        if (maxPage > 0) {
            int center = this.panelLeft + this.panelWidth / 2;
            int navY = this.panelTop + 348;
            PanelButton previous = new PanelButton(center - 58, navY, 22, 18, Component.literal("‹"),
                    PanelButton.Variant.SECONDARY, button -> {
                        this.page = Math.max(0, this.page - 1);
                        this.rebuildWidgets();
                    });
            previous.active = this.page > 0;
            this.addRenderableWidget(previous);

            PanelButton next = new PanelButton(center + 36, navY, 22, 18, Component.literal("›"),
                    PanelButton.Variant.SECONDARY, button -> {
                        this.page = Math.min(maxPage, this.page + 1);
                        this.rebuildWidgets();
                    });
            next.active = this.page < maxPage;
            this.addRenderableWidget(next);
        }
    }

    private void savePrice(PlayerMarketService.MarketListing listing) {
        EditBox box = this.priceBoxes.get(listing.id);
        if (box == null) return;
        String raw = box.getValue().trim();
        int price;
        try {
            price = Integer.parseInt(raw);
        } catch (NumberFormatException exception) {
            this.resultCode = "market_invalid_price_quantity";
            return;
        }
        if (price < 1 || price > PlayerMarketService.MAX_WALLET_BALANCE) {
            this.resultCode = "market_invalid_price_quantity";
            return;
        }
        if ((long) price * listing.quantity > PlayerMarketService.MAX_WALLET_BALANCE) {
            this.resultCode = "market_total_too_high";
            return;
        }
        com.google.gson.JsonObject payload = new com.google.gson.JsonObject();
        payload.addProperty("id", listing.id);
        payload.addProperty("price", price);
        ClientNetworking.send(new ServerboundAvecoinsPayload("market_update_listing", payload.toString()));
    }

    @Override
    public void extractBackground(GuiGraphicsExtractor graphics, int mouseX, int mouseY, float partialTick) {
        super.extractBackground(graphics, mouseX, mouseY, partialTick);
        if (Minecraft.getInstance().gui.screen() != this || this.responsiveLayout == null) return;
        graphics.pose().pushMatrix();
        try {
            graphics.pose().translate(this.responsiveLayout.offsetX(), this.responsiveLayout.offsetY());
            graphics.pose().scale(this.responsiveLayout.scale(), this.responsiveLayout.scale());
            int listX = this.panelLeft + 12;
            int rowWidth = this.panelWidth - 24;
            int cardWidth = (rowWidth - 8) / COLUMNS;
            int listTop = this.panelTop + 68;
            int start = this.page * PAGE_SIZE;
            List<PlayerMarketService.MarketListing> listings = this.ownListings();
            int end = Math.min(listings.size(), start + PAGE_SIZE);
            for (int index = start; index < end; index++) {
                int local = index - start;
                int cardX = listX + (local % COLUMNS) * (cardWidth + 8);
                int cardY = listTop + (local / COLUMNS) * CARD_STEP;
                graphics.fill(cardX, cardY, cardX + cardWidth, cardY + CARD_HEIGHT, this.theme().BORDER);
                graphics.fill(cardX + 1, cardY + 1, cardX + cardWidth - 1, cardY + CARD_HEIGHT - 1, this.theme().PANEL);
            }
        } finally {
            graphics.pose().popMatrix();
        }
    }

    @Override
    protected void renderPanel(GuiGraphicsExtractor graphics, int mouseX, int mouseY, float partialTick) {
        this.renderHeader(graphics, null);
        this.drawSectionTitleAt(graphics, Component.translatable("gui.avecoins.player_market.my_listings"),
                this.panelLeft + this.panelWidth / 2, this.panelTop + 51);

        List<PlayerMarketService.MarketListing> listings = this.ownListings();
        int listX = this.panelLeft + 12;
        int rowWidth = this.panelWidth - 24;
        int cardWidth = (rowWidth - 8) / COLUMNS;
        int listTop = this.panelTop + 68;
        int start = this.page * PAGE_SIZE;
        int end = Math.min(listings.size(), start + PAGE_SIZE);

        if (listings.isEmpty()) {
            this.drawCenteredWrappedAt(graphics, Component.translatable("gui.avecoins.player_market.manage_empty"),
                    this.panelLeft + this.panelWidth / 2, listTop + 70, rowWidth - 60, this.theme().MUTED);
        }

        for (int index = start; index < end; index++) {
            PlayerMarketService.MarketListing listing = listings.get(index);
            int local = index - start;
            int cardX = listX + (local % COLUMNS) * (cardWidth + 8);
            int cardY = listTop + (local / COLUMNS) * CARD_STEP;

            Identifier itemId = Identifier.tryParse(listing.itemId);
            if (itemId != null && BuiltInRegistries.ITEM.containsKey(itemId)) {
                Item item = (Item) BuiltInRegistries.ITEM.getValue(itemId);
                graphics.item(new ItemStack(item), cardX + 8, cardY + 8);
            }
            String title = listing.displayName;
            graphics.text(this.font, Component.literal(this.font.plainSubstrByWidth(title, cardWidth - 44)),
                    cardX + 30, cardY + 8, this.theme().TEXT, !this.theme().custom);
            graphics.text(this.font,
                    Component.translatable("gui.avecoins.player_market.selected_count", listing.quantity),
                    cardX + 30, cardY + 24, this.theme().MUTED, !this.theme().custom);
            graphics.text(this.font, Component.translatable("gui.avecoins.player_market.price_short"),
                    cardX + 8, cardY + 42, this.theme().TEXT, !this.theme().custom);
        }

        int maxPage = Math.max(0, (listings.size() - 1) / PAGE_SIZE);
        if (maxPage > 0) {
            GuiTextLayout.drawCentered(graphics, this.font,
                    Component.translatable("gui.avecoins.shop.page", this.page + 1, maxPage + 1),
                    this.panelLeft + this.panelWidth / 2, this.panelTop + 352, this.theme().MUTED, !this.theme().custom);
        }
        this.renderVmcoinBalance(graphics, this.panelLeft + this.panelWidth / 2, this.panelTop + 383);
        if (!this.resultCode.isBlank()) {
            this.renderStatusBoxAt(graphics, this.resultCode, this.panelTop + this.panelHeight - 24);
        }
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
}
