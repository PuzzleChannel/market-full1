package net.sundggs.avecoins.client.gui;

import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
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
import net.sundggs.avecoins.shop.ShopItemStacks;
import net.sundggs.avecoins.shop.ShopService;

/** Lists the current player's escrowed items and allows safe price edits or cancellation. */
public final class PlayerMarketManageScreen extends AvecoinsScreen {
    private static final int PAGE_SIZE = 12;
    private static final int COLUMNS = 4;
    private static final int CARD_HEIGHT = 66;
    private static final int CARD_STEP = 73;
    private static final Identifier COIN_ID = Identifier.fromNamespaceAndPath("vmtrade", "vmcoin");

    private final PlayerMarketService.MarketSnapshot snapshot;
    private final Map<String, String> priceDrafts;
    private final Map<String, EditBox> priceBoxes = new HashMap<>();
    private final Map<String, PanelButton> saveButtons = new HashMap<>();
    private String resultCode;
    private String confirmingListingId;
    private int page;
    private boolean showExpired;
    private String listingStateSignature = "";

    public PlayerMarketManageScreen(PlayerMarketService.MarketSnapshot snapshot, String resultCode) {
        this(snapshot, resultCode, 0, Map.of(), "", false);
    }

    private PlayerMarketManageScreen(PlayerMarketService.MarketSnapshot snapshot, String resultCode,
                                     int page, Map<String, String> priceDrafts, String confirmingListingId, boolean showExpired) {
        super(Component.translatable("gui.avecoins.player_market.manage_title"));
        this.snapshot = snapshot;
        this.resultCode = resultCode == null ? "" : resultCode;
        this.page = Math.max(0, page);
        this.priceDrafts = new HashMap<>(priceDrafts);
        this.confirmingListingId = confirmingListingId == null ? "" : confirmingListingId;
        this.showExpired = showExpired;
        ShopService.ShopSnapshot themeSnapshot = new ShopService.ShopSnapshot();
        themeSnapshot.shopColor = snapshot.shopColor;
        this.useShopTheme(themeSnapshot);
    }

    public PlayerMarketManageScreen refreshed(PlayerMarketService.MarketSnapshot snapshot, String resultCode) {
        Map<String, String> drafts = this.capturePriceDrafts();
        Set<String> activeIds = new HashSet<>();
        for (PlayerMarketService.MarketListing listing : snapshot.listings) {
            activeIds.add(listing.id);
            String draft = drafts.get(listing.id);
            if ("market_listing_updated".equals(resultCode) && draft != null
                    && draft.trim().equals(Integer.toString(listing.totalPrice))) {
                drafts.remove(listing.id);
            }
        }
        drafts.keySet().removeIf(id -> !activeIds.contains(id));

        String confirmId = this.confirmingListingId;
        if (!activeIds.contains(confirmId) || "market_listing_cancelled".equals(resultCode)) {
            confirmId = "";
        }
        return new PlayerMarketManageScreen(snapshot, resultCode, this.page, drafts, confirmId, this.showExpired);
    }

    private Map<String, String> capturePriceDrafts() {
        this.priceBoxes.forEach((id, box) -> this.priceDrafts.put(id, box.getValue()));
        return new HashMap<>(this.priceDrafts);
    }

    private List<PlayerMarketService.MarketListing> allOwnListings() {
        return this.snapshot.listings.stream()
                .filter(listing -> listing.sellerId.equals(this.snapshot.viewerId))
                .toList();
    }

    private List<PlayerMarketService.MarketListing> activeOwnListings() {
        long now = System.currentTimeMillis();
        return this.allOwnListings().stream().filter(listing -> !isExpired(listing, now)).toList();
    }

    private List<PlayerMarketService.MarketListing> expiredOwnListings() {
        long now = System.currentTimeMillis();
        return this.allOwnListings().stream().filter(listing -> isExpired(listing, now)).toList();
    }

    private List<PlayerMarketService.MarketListing> ownListings() {
        long now = System.currentTimeMillis();
        return this.allOwnListings().stream()
                .filter(listing -> isExpired(listing, now) == this.showExpired)
                .toList();
    }

    private static boolean isExpired(PlayerMarketService.MarketListing listing) {
        return isExpired(listing, System.currentTimeMillis());
    }

    private static boolean isExpired(PlayerMarketService.MarketListing listing, long now) {
        return listing.expiresAt > 0L && listing.expiresAt <= now;
    }

    private String currentExpirySignature() {
        long now = System.currentTimeMillis();
        StringBuilder signature = new StringBuilder();
        for (PlayerMarketService.MarketListing listing : this.allOwnListings()) {
            signature.append(listing.id).append(':').append(isExpired(listing, now)).append(';');
        }
        return signature.toString();
    }

    @Override
    public void tick() {
        super.tick();
        String signature = this.currentExpirySignature();
        if (!signature.equals(this.listingStateSignature)) {
            this.listingStateSignature = signature;
            if (this.showExpired && this.expiredOwnListings().isEmpty()) this.showExpired = false;
            this.page = 0;
            this.confirmingListingId = "";
            this.rebuildWidgets();
        }
    }

    private static long totalValue(List<PlayerMarketService.MarketListing> listings) {
        long total = 0L;
        for (PlayerMarketService.MarketListing listing : listings) {
            total += (long) listing.totalPrice;
        }
        return total;
    }

    private static long totalQuantity(List<PlayerMarketService.MarketListing> listings) {
        long total = 0L;
        for (PlayerMarketService.MarketListing listing : listings) {
            total += listing.quantity;
        }
        return total;
    }

    @Override
    protected void init() {
        this.layoutPanel(820, 440);
        this.priceBoxes.clear();
        this.saveButtons.clear();

        int headerY = this.panelTop + 7;
        this.addRenderableWidget(new PanelButton(this.panelLeft + 10, headerY, 112, 20,
                Component.translatable("gui.avecoins.player_market.back_to_market"), PanelButton.Variant.SECONDARY,
                button -> ClientNetworking.send(new ServerboundAvecoinsPayload("request_player_market", "{}"))));
        this.addRenderableWidget(new PanelButton(this.panelLeft + this.panelWidth - 28, headerY, 18, 20,
                Component.literal("×"), PanelButton.Variant.DANGER, button -> this.onClose()));

        List<PlayerMarketService.MarketListing> expiredListings = this.expiredOwnListings();
        if (this.showExpired && expiredListings.isEmpty()) this.showExpired = false;
        List<PlayerMarketService.MarketListing> activeListings = this.activeOwnListings();
        expiredListings = this.expiredOwnListings();
        int tabCenterX = this.panelLeft + this.panelWidth / 2;
        int tabY = this.panelTop + 70;
        if (expiredListings.isEmpty()) {
            int tabWidth = 176;
            this.addRenderableWidget(new PanelButton(tabCenterX - tabWidth / 2, tabY, tabWidth, 18,
                    Component.translatable("gui.avecoins.player_market.manage_active_tab", activeListings.size()),
                    PanelButton.Variant.HEADER, button -> {
                        this.capturePriceDrafts();
                        this.showExpired = false;
                        this.page = 0;
                        this.confirmingListingId = "";
                        this.rebuildWidgets();
                    }));
        } else {
            int tabWidth = 160;
            this.addRenderableWidget(new PanelButton(tabCenterX - 164, tabY, tabWidth, 18,
                    Component.translatable("gui.avecoins.player_market.manage_active_tab", activeListings.size()),
                    this.showExpired ? PanelButton.Variant.SECONDARY : PanelButton.Variant.HEADER,
                    button -> {
                        this.capturePriceDrafts();
                        this.showExpired = false;
                        this.page = 0;
                        this.confirmingListingId = "";
                        this.rebuildWidgets();
                    }));
            this.addRenderableWidget(new PanelButton(tabCenterX + 4, tabY, tabWidth, 18,
                    Component.translatable("gui.avecoins.player_market.manage_expired_tab", expiredListings.size()),
                    this.showExpired ? PanelButton.Variant.HEADER : PanelButton.Variant.SECONDARY,
                    button -> {
                        this.capturePriceDrafts();
                        this.showExpired = true;
                        this.page = 0;
                        this.confirmingListingId = "";
                        this.rebuildWidgets();
                    }));
        }

        List<PlayerMarketService.MarketListing> listings = this.ownListings();
        int maxPage = Math.max(0, (listings.size() - 1) / PAGE_SIZE);
        this.page = Math.min(this.page, maxPage);

        int listX = this.panelLeft + 120;
        int rowWidth = this.panelWidth - 132;
        int cardWidth = (rowWidth - (COLUMNS - 1) * 8) / COLUMNS;
        int listTop = this.panelTop + 94;
        int start = this.page * PAGE_SIZE;
        int end = Math.min(listings.size(), start + PAGE_SIZE);

        for (int index = start; index < end; index++) {
            PlayerMarketService.MarketListing listing = listings.get(index);
            int local = index - start;
            int cardX = listX + (local % COLUMNS) * (cardWidth + 8);
            int cardY = listTop + (local / COLUMNS) * CARD_STEP;

            boolean confirming = listing.id.equals(this.confirmingListingId);
            if (confirming) {
                // Use the full control row for a clear two-button confirmation on compact cards.
                this.addRenderableWidget(new PanelButton(cardX + 4, cardY + 47, 78, 16,
                        Component.translatable("gui.avecoins.player_market.confirm_remove"), PanelButton.Variant.DANGER,
                        button -> {
                            this.capturePriceDrafts();
                            com.google.gson.JsonObject payload = new com.google.gson.JsonObject();
                            payload.addProperty("id", listing.id);
                            ClientNetworking.send(new ServerboundAvecoinsPayload("market_cancel", payload.toString()));
                        }));
                this.addRenderableWidget(new PanelButton(cardX + 86, cardY + 47, 76, 16,
                        Component.translatable("gui.avecoins.player_market.keep_listing"), PanelButton.Variant.SECONDARY,
                        button -> {
                            this.capturePriceDrafts();
                            this.confirmingListingId = "";
                            this.rebuildWidgets();
                        }));
            } else {
                EditBox priceBox = new EditBox(this.font, cardX + 4, cardY + 47, 34, 16,
                        Component.translatable("gui.avecoins.player_market.price"));
                priceBox.setMaxLength(4);
                priceBox.active = !isExpired(listing);
                priceBox.setValue(this.priceDrafts.getOrDefault(listing.id, Integer.toString(listing.totalPrice)));
                priceBox.setHint(Component.literal(Integer.toString(listing.totalPrice)));
                this.priceBoxes.put(listing.id, priceBox);
                this.addRenderableWidget(priceBox);

                PanelButton saveButton = new PanelButton(cardX + 42, cardY + 47, 50, 16,
                        Component.translatable("gui.avecoins.player_market.save_price"), PanelButton.Variant.SECONDARY,
                        button -> this.savePrice(listing));
                this.saveButtons.put(listing.id, saveButton);
                this.updateSaveButton(listing, priceBox, saveButton);
                this.addRenderableWidget(saveButton);
                priceBox.setResponder(value -> {
                    this.priceDrafts.put(listing.id, value);
                    this.updateSaveButton(listing, priceBox, saveButton);
                });

                this.addRenderableWidget(new PanelButton(cardX + 96, cardY + 47, 66, 16,
                        Component.translatable(isExpired(listing)
                                ? "gui.avecoins.player_market.manage_return_expired"
                                : "gui.avecoins.player_market.cancel"),
                        PanelButton.Variant.DANGER, button -> this.handleRemoveClick(listing)));
            }
        }

        this.listingStateSignature = this.currentExpirySignature();

        if (maxPage > 0) {
            int center = this.panelLeft + this.panelWidth / 2;
            int navY = this.panelTop + 348;
            PanelButton previous = new PanelButton(center - 58, navY, 22, 18, Component.literal("‹"),
                    PanelButton.Variant.SECONDARY, button -> {
                        this.capturePriceDrafts();
                        this.confirmingListingId = "";
                        this.page = Math.max(0, this.page - 1);
                        this.rebuildWidgets();
                    });
            previous.active = this.page > 0;
            this.addRenderableWidget(previous);

            PanelButton next = new PanelButton(center + 36, navY, 22, 18, Component.literal("›"),
                    PanelButton.Variant.SECONDARY, button -> {
                        this.capturePriceDrafts();
                        this.confirmingListingId = "";
                        this.page = Math.min(maxPage, this.page + 1);
                        this.rebuildWidgets();
                    });
            next.active = this.page < maxPage;
            this.addRenderableWidget(next);
        }
    }

    private void updateSaveButton(PlayerMarketService.MarketListing listing, EditBox box, PanelButton button) {
        String value = box.getValue().trim();
        boolean changed = !value.equals(Integer.toString(listing.totalPrice));
        button.active = changed && !isExpired(listing);
        boolean valid = false;
        try {
            int parsed = Integer.parseInt(value);
            valid = parsed >= 1 && parsed <= PlayerMarketService.MAX_WALLET_BALANCE;
        } catch (NumberFormatException ignored) {
            // Leave the button clickable for a useful validation message.
        }
        button.setVariant(changed && valid && !isExpired(listing) ? PanelButton.Variant.SUCCESS : PanelButton.Variant.SECONDARY);
    }

    private void handleRemoveClick(PlayerMarketService.MarketListing listing) {
        this.capturePriceDrafts();
        if (!listing.id.equals(this.confirmingListingId)) {
            this.confirmingListingId = listing.id;
            this.rebuildWidgets();
            return;
        }
        com.google.gson.JsonObject payload = new com.google.gson.JsonObject();
        payload.addProperty("id", listing.id);
        ClientNetworking.send(new ServerboundAvecoinsPayload("market_cancel", payload.toString()));
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
        if (isExpired(listing)) {
            this.resultCode = "market_listing_expired";
            return;
        }
        if (price < 1 || price > PlayerMarketService.MAX_WALLET_BALANCE) {
            this.resultCode = "market_invalid_price_quantity";
            return;
        }
        if (price == listing.totalPrice) {
            this.priceDrafts.remove(listing.id);
            this.resultCode = "";
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
            int listX = this.panelLeft + 120;
            int rowWidth = this.panelWidth - 132;
            int cardWidth = (rowWidth - (COLUMNS - 1) * 8) / COLUMNS;
            int listTop = this.panelTop + 94;
            int start = this.page * PAGE_SIZE;
            List<PlayerMarketService.MarketListing> listings = this.ownListings();
            int end = Math.min(listings.size(), start + PAGE_SIZE);
            for (int index = start; index < end; index++) {
                int local = index - start;
                int cardX = listX + (local % COLUMNS) * (cardWidth + 8);
                int cardY = listTop + (local / COLUMNS) * CARD_STEP;
                graphics.fill(cardX, cardY, cardX + cardWidth, cardY + CARD_HEIGHT, this.theme().BORDER);
                graphics.fill(cardX + 1, cardY + 1, cardX + cardWidth - 1, cardY + CARD_HEIGHT - 1, this.theme().PANEL);
                graphics.fill(cardX + 1, cardY + 1, cardX + cardWidth - 1, cardY + 3, this.theme().DIVIDER);
            }
        } finally {
            graphics.pose().popMatrix();
        }
    }

    @Override
    protected void renderPanel(GuiGraphicsExtractor graphics, int mouseX, int mouseY, float partialTick) {
        this.renderHeader(graphics, null);
        List<PlayerMarketService.MarketListing> listings = this.ownListings();
        Component summary = Component.translatable("gui.avecoins.player_market.manage_summary",
                listings.size(), totalQuantity(listings));
        GuiTextLayout.drawCenteredFitted(graphics, this.font, summary,
                this.panelLeft + this.panelWidth / 2, this.panelTop + 42, this.panelWidth - 36, this.theme().MUTED);
        this.renderTotalValue(graphics, this.panelLeft + this.panelWidth / 2, this.panelTop + 57,
                totalValue(listings));

        int listX = this.panelLeft + 120;
        int rowWidth = this.panelWidth - 132;
        int cardWidth = (rowWidth - (COLUMNS - 1) * 8) / COLUMNS;
        int listTop = this.panelTop + 94;
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

            ItemStack preview = this.previewStack(listing);
            int cardCenterX = cardX + cardWidth / 2;
            if (!preview.isEmpty()) {
                graphics.item(preview, cardCenterX - 8, cardY + 2);
            }
            String displayName = preview.isEmpty() ? listing.displayName : preview.getHoverName().getString();
            String title = displayName + (listing.quantity > 1 ? " х" + listing.quantity : "");
            Component titleText = Component.literal(this.font.plainSubstrByWidth(title, cardWidth - 8));
            GuiTextLayout.drawCentered(graphics, this.font, titleText,
                    cardCenterX, cardY + 20, this.theme().TEXT, !this.theme().custom);
            this.renderListingPrice(graphics, listing.totalPrice, cardCenterX, cardY + 32);
            if (listing.expiresAt > 0L) {
                long remaining = listing.expiresAt - System.currentTimeMillis();
                int remainingColor = remaining <= 0L ? this.theme().DANGER : this.theme().MUTED;
                GuiTextLayout.drawCentered(graphics, this.font, this.remainingTimeLabel(listing.expiresAt),
                        cardCenterX, cardY + 41, remainingColor, !this.theme().custom);
            }
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

    
    private Component remainingTimeLabel(long expiresAt) {
        long remaining = expiresAt - System.currentTimeMillis();
        if (remaining <= 0L) {
            return Component.translatable("gui.avecoins.player_market.listing_expired");
        }
        long minutes = Math.max(1L, (remaining + 59_999L) / 60_000L);
        if (minutes >= 1_440L) {
            return Component.translatable("gui.avecoins.player_market.listing_remaining_days",
                    minutes / 1_440L, (minutes % 1_440L) / 60L);
        }
        if (minutes >= 60L) {
            return Component.translatable("gui.avecoins.player_market.listing_remaining_hours",
                    minutes / 60L, minutes % 60L);
        }
        return Component.translatable("gui.avecoins.player_market.listing_remaining_minutes", minutes);
    }

    private void renderTotalValue(GuiGraphicsExtractor graphics, int centerX, int y, long value) {
        Component label = Component.translatable("gui.avecoins.player_market.manage_total_value");
        this.drawCurrencyLine(graphics, label, Long.toString(value), centerX, y, this.theme().TEXT);
    }

    private void renderListingPrice(GuiGraphicsExtractor graphics, long value, int centerX, int y) {
        Component label = Component.translatable("gui.avecoins.player_market.current_price");
        String amount = Long.toString(value);
        boolean hasIcon = BuiltInRegistries.ITEM.containsKey(COIN_ID);
        int groupWidth = this.font.width(label) + 5 + this.font.width(amount) + (hasIcon ? 19 : 0);
        int x = centerX - groupWidth / 2;
        graphics.text(this.font, label, x, y, this.theme().TEXT, !this.theme().custom);
        int amountX = x + this.font.width(label) + 5;
        graphics.text(this.font, Component.literal(amount), amountX, y, this.theme().TEXT, !this.theme().custom);
        if (hasIcon) {
            graphics.item(new ItemStack((Item) BuiltInRegistries.ITEM.getValue(COIN_ID)),
                    amountX + this.font.width(amount) + 3, y - 4);
        }
    }

    private void drawCurrencyLine(GuiGraphicsExtractor graphics, Component label, String amount,
                                  int centerX, int y, int color) {
        boolean hasIcon = BuiltInRegistries.ITEM.containsKey(COIN_ID);
        int width = this.font.width(label) + 6 + this.font.width(amount) + (hasIcon ? 20 : 0);
        int x = centerX - width / 2;
        graphics.text(this.font, label, x, y, color, false);
        x += this.font.width(label) + 6;
        graphics.text(this.font, Component.literal(amount), x, y, color, false);
        x += this.font.width(amount) + 3;
        if (hasIcon) graphics.item(new ItemStack((Item) BuiltInRegistries.ITEM.getValue(COIN_ID)), x, y - 4);
    }

    private ItemStack previewStack(PlayerMarketService.MarketListing listing) {
        Identifier itemId = Identifier.tryParse(listing.itemId);
        if (itemId == null || !BuiltInRegistries.ITEM.containsKey(itemId)) {
            return ItemStack.EMPTY;
        }
        Item item = (Item) BuiltInRegistries.ITEM.getValue(itemId);
        if (this.minecraft.level != null && listing.itemData != null && !listing.itemData.isBlank()) {
            ItemStack decoded = ShopItemStacks.decodeExpected(this.minecraft.level.registryAccess(),
                    listing.itemData, item).orElse(ItemStack.EMPTY);
            if (!decoded.isEmpty()) {
                return decoded.copyWithCount(Math.min(listing.quantity, decoded.getMaxStackSize()));
            }
        }
        return new ItemStack(item);
    }

    private void renderVmcoinBalance(GuiGraphicsExtractor graphics, int centerX, int iconY) {
        Component label = Component.translatable("gui.avecoins.player_market.wallet_label");
        String amount = Long.toString(this.snapshot.walletBalance);
        Identifier currencyId = COIN_ID;
        boolean hasIcon = BuiltInRegistries.ITEM.containsKey(currencyId);
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
