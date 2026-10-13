package net.sundggs.avecoins.client.gui;

import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.input.CharacterEvent;
import net.minecraft.client.input.KeyEvent;
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

/** Компактный рынок игроков с поиском по объявлениям. */
public final class PlayerMarketScreen extends AvecoinsScreen {
    private static final int LISTINGS_PER_PAGE = 12;
    private static final int COLUMNS = 4;
    private static final int CARD_HEIGHT = 80;
    private static final int CARD_STEP = 86;
    private static final Identifier WALLET_ICON = Identifier.fromNamespaceAndPath("avecoins", "textures/gui/icons/coin_wallet.png");
    private static final String[] FILTER_KEYS = {
            "gui.avecoins.player_market.filter_all",
            "gui.avecoins.player_market.filter_affordable",
            "gui.avecoins.player_market.filter_mine",
            "gui.avecoins.player_market.filter_others"
    };
    private static final String[] SORT_KEYS = {
            "gui.avecoins.player_market.sort_newest",
            "gui.avecoins.player_market.sort_cheapest",
            "gui.avecoins.player_market.sort_expensive",
            "gui.avecoins.player_market.sort_quantity",
            "gui.avecoins.player_market.sort_unit"
    };

    private final PlayerMarketService.MarketSnapshot snapshot;
    private final Map<String, ItemStack> previewCache = new HashMap<>();
    private int filterMode;
    private int sortMode;
    private String resultCode;
    private String search;
    private int page;
    private EditBox searchBox;
    private List<PlayerMarketService.MarketListing> visibleListings = List.of();

    public PlayerMarketScreen(PlayerMarketService.MarketSnapshot snapshot, String resultCode) {
        this(snapshot, resultCode, "", 0, 0, 0);
    }

    private PlayerMarketScreen(PlayerMarketService.MarketSnapshot snapshot, String resultCode, String search,
                                int page, int filterMode, int sortMode) {
        super(Component.translatable("gui.avecoins.player_market.title"));
        this.snapshot = snapshot;
        this.resultCode = resultCode == null ? "" : resultCode;
        this.search = search == null ? "" : search;
        this.page = Math.max(0, page);
        this.filterMode = Math.floorMod(filterMode, FILTER_KEYS.length);
        this.sortMode = Math.floorMod(sortMode, SORT_KEYS.length);
        ShopService.ShopSnapshot themeSnapshot = new ShopService.ShopSnapshot();
        themeSnapshot.shopColor = snapshot.shopColor;
        this.useShopTheme(themeSnapshot);
    }

    public PlayerMarketScreen refreshed(PlayerMarketService.MarketSnapshot snapshot, String resultCode) {
        return new PlayerMarketScreen(snapshot, resultCode,
                this.searchBox == null ? this.search : this.searchBox.getValue(),
                this.page, this.filterMode, this.sortMode);
    }

    @Override
    protected void init() {
        this.layoutPanel(820, 440);
        int headerY = this.panelTop + 7;
        int closeX = this.panelLeft + this.panelWidth - 28;
        this.addRenderableWidget(new PanelButton(closeX, headerY, 18, 20,
                Component.literal("×"), PanelButton.Variant.DANGER, button -> this.onClose()));

        int myListingsX = closeX - 92 - 8;
        this.addRenderableWidget(new PanelButton(myListingsX, headerY, 92, 20,
                Component.translatable("gui.avecoins.player_market.my_listings"), PanelButton.Variant.HEADER,
                button -> ClientNetworking.send(new ServerboundAvecoinsPayload("request_player_market_manage", "{}"))));

        int createX = myListingsX - 142 - 8;
        this.addRenderableWidget(new PanelButton(createX, headerY, 142, 20,
                Component.translatable("gui.avecoins.player_market.list_item"), PanelButton.Variant.SUCCESS,
                button -> this.minecraft.gui.setScreen(new PlayerMarketCreateScreen(this.snapshot, ""))));

        int walletX = this.panelLeft + this.panelWidth - 12 - 26;
        int walletY = this.panelTop + this.panelHeight - 30;
        PanelButton wallet = new PanelButton(walletX, walletY, 26, 22, Component.empty(),
                PanelButton.Variant.HEADER,
                button -> ClientNetworking.send(new ServerboundAvecoinsPayload("request_wallet", "{}")))
                .withIcon(WALLET_ICON, 16);
        wallet.setTooltip(net.minecraft.client.gui.components.Tooltip.create(
                Component.translatable("gui.avecoins.wallet.open")));
        this.addRenderableWidget(wallet);

        int controlsWidth = 150 + 8 + 300 + 8 + 150;
        int controlsLeft = this.panelLeft + (this.panelWidth - controlsWidth) / 2;
        this.addRenderableWidget(new PanelButton(controlsLeft, this.panelTop + 42, 150, 20,
                Component.translatable("gui.avecoins.player_market.filter",
                        Component.translatable(FILTER_KEYS[this.filterMode])),
                PanelButton.Variant.SECONDARY, button -> {
                    this.filterMode = (this.filterMode + 1) % FILTER_KEYS.length;
                    this.page = 0;
                    this.rebuildWidgets();
                }));

        this.searchBox = new EditBox(this.font, controlsLeft + 158,
                this.panelTop + 42, 300, 20, Component.translatable("gui.avecoins.player_market.search"));
        this.searchBox.setMaxLength(64);
        this.searchBox.setValue(this.search);
        this.searchBox.setHint(Component.translatable("gui.avecoins.player_market.search"));
        this.addRenderableWidget(this.searchBox);

        this.addRenderableWidget(new PanelButton(controlsLeft + 466, this.panelTop + 42, 150, 20,
                Component.translatable("gui.avecoins.player_market.sort",
                        Component.translatable(SORT_KEYS[this.sortMode])),
                PanelButton.Variant.SECONDARY, button -> {
                    this.sortMode = (this.sortMode + 1) % SORT_KEYS.length;
                    this.page = 0;
                    this.rebuildWidgets();
                }));

        this.visibleListings = this.filteredListings();
        int maxPage = Math.max(0, (this.visibleListings.size() - 1) / LISTINGS_PER_PAGE);
        this.page = Math.min(this.page, maxPage);

        int listX = this.panelLeft + 12;
        int rowWidth = this.panelWidth - 24;
        int cardWidth = (rowWidth - (COLUMNS - 1) * 8) / COLUMNS;
        int listTop = this.panelTop + 80;
        int start = this.page * LISTINGS_PER_PAGE;
        int end = Math.min(this.visibleListings.size(), start + LISTINGS_PER_PAGE);

        for (int index = start; index < end; index++) {
            PlayerMarketService.MarketListing listing = this.visibleListings.get(index);
            int local = index - start;
            int cardX = listX + (local % COLUMNS) * (cardWidth + 8);
            int cardY = listTop + (local / COLUMNS) * CARD_STEP;
            boolean ownListing = listing.sellerId.equals(this.snapshot.viewerId);
            long total = lotPrice(listing);

            PanelButton action = new PanelButton(cardX + cardWidth - 62, cardY + 59, 56, 18,
                    Component.translatable(ownListing ? "gui.avecoins.player_market.cancel" : "gui.avecoins.player_market.buy"),
                    ownListing ? PanelButton.Variant.DANGER : PanelButton.Variant.SUCCESS,
                    button -> {
                        com.google.gson.JsonObject payload = new com.google.gson.JsonObject();
                        payload.addProperty("id", listing.id);
                        ClientNetworking.send(new ServerboundAvecoinsPayload(
                                ownListing ? "market_cancel" : "market_buy", payload.toString()));
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
            int navY = this.panelTop + 347;
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

    private List<PlayerMarketService.MarketListing> filteredListings() {
        String query = this.search == null ? "" : this.search.trim().toLowerCase(Locale.ROOT);
        return this.snapshot.listings.stream()
                .filter(listing -> {
                    boolean own = listing.sellerId.equals(this.snapshot.viewerId);
                    long total = lotPrice(listing);
                    boolean matchesFilter = switch (this.filterMode) {
                        case 1 -> !own && total <= this.snapshot.walletBalance;
                        case 2 -> own;
                        case 3 -> !own;
                        default -> true;
                    };
                    boolean matchesSearch = query.isEmpty()
                            || (listing.displayName != null && listing.displayName.toLowerCase(Locale.ROOT).contains(query))
                            || (listing.itemId != null && listing.itemId.toLowerCase(Locale.ROOT).contains(query))
                            || (listing.sellerName != null && listing.sellerName.toLowerCase(Locale.ROOT).contains(query));
                    return matchesFilter && matchesSearch;
                })
                .sorted(this.listingComparator())
                .toList();
    }

    private Comparator<PlayerMarketService.MarketListing> listingComparator() {
        Comparator<PlayerMarketService.MarketListing> newest =
                Comparator.comparingLong((PlayerMarketService.MarketListing listing) -> listing.createdAt).reversed();
        return switch (this.sortMode) {
            case 1 -> Comparator.comparingLong(PlayerMarketScreen::lotPrice).thenComparing(newest);
            case 2 -> Comparator.comparingLong(PlayerMarketScreen::lotPrice).reversed().thenComparing(newest);
            case 3 -> Comparator.comparingInt((PlayerMarketService.MarketListing listing) -> listing.quantity)
                    .reversed().thenComparing(newest);
            case 4 -> (first, second) -> {
                long firstCross = lotPrice(first) * Math.max(1, second.quantity);
                long secondCross = lotPrice(second) * Math.max(1, first.quantity);
                int compare = Long.compare(firstCross, secondCross);
                return compare != 0 ? compare : Long.compare(second.createdAt, first.createdAt);
            };
            default -> newest;
        };
    }

    private static long lotPrice(PlayerMarketService.MarketListing listing) {
        return listing.totalPrice > 0 ? listing.totalPrice : (long) listing.unitPrice * listing.quantity;
    }

    private ItemStack previewStack(PlayerMarketService.MarketListing listing) {
        ItemStack cached = this.previewCache.get(listing.id);
        if (cached != null) return cached.copy();
        Identifier itemId = Identifier.tryParse(listing.itemId);
        if (itemId == null || !BuiltInRegistries.ITEM.containsKey(itemId)) return ItemStack.EMPTY;
        Item item = (Item) BuiltInRegistries.ITEM.getValue(itemId);
        ItemStack stack = ItemStack.EMPTY;
        if (this.minecraft.level != null && listing.itemData != null && !listing.itemData.isBlank()) {
            stack = ShopItemStacks.decodeExpected(this.minecraft.level.registryAccess(), listing.itemData, item)
                    .orElse(ItemStack.EMPTY);
        }
        if (stack.isEmpty()) stack = new ItemStack(item);
        stack = stack.copyWithCount(Math.min(Math.max(1, listing.quantity), stack.getMaxStackSize()));
        this.previewCache.put(listing.id, stack.copy());
        return stack;
    }

    public boolean charTyped(CharacterEvent event) {
        boolean handled = super.charTyped(event);
        this.refreshSearchIfChanged();
        return handled;
    }

    public boolean keyPressed(KeyEvent event) {
        boolean handled = super.keyPressed(event);
        this.refreshSearchIfChanged();
        return handled;
    }

    private void refreshSearchIfChanged() {
        if (this.searchBox == null || this.search.equals(this.searchBox.getValue())) return;
        this.search = this.searchBox.getValue();
        this.page = 0;
        this.rebuildWidgets();
        this.searchBox.setFocused(true);
        this.searchBox.moveCursorToEnd(false);
        this.setFocused(this.searchBox);
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
            int cardWidth = (rowWidth - (COLUMNS - 1) * 8) / COLUMNS;
            int listTop = this.panelTop + 80;
            int start = this.page * LISTINGS_PER_PAGE;
            int end = Math.min(this.visibleListings.size(), start + LISTINGS_PER_PAGE);
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
        this.drawSectionTitleAt(graphics, Component.translatable("gui.avecoins.player_market.listings"),
                this.panelLeft + this.panelWidth / 2, this.panelTop + 68);

        int listX = this.panelLeft + 12;
        int rowWidth = this.panelWidth - 24;
        int cardWidth = (rowWidth - (COLUMNS - 1) * 8) / COLUMNS;
        int listTop = this.panelTop + 80;
        int start = this.page * LISTINGS_PER_PAGE;
        int end = Math.min(this.visibleListings.size(), start + LISTINGS_PER_PAGE);

        if (this.visibleListings.isEmpty()) {
            Component empty = this.search == null || this.search.isBlank()
                    ? Component.translatable("gui.avecoins.player_market.empty")
                    : Component.translatable("gui.avecoins.player_market.search_empty");
            this.drawCenteredWrappedAt(graphics, empty, this.panelLeft + this.panelWidth / 2,
                    listTop + 64, rowWidth - 60, this.theme().MUTED);
        }

        for (int index = start; index < end; index++) {
            PlayerMarketService.MarketListing listing = this.visibleListings.get(index);
            int local = index - start;
            int cardX = listX + (local % COLUMNS) * (cardWidth + 8);
            int cardY = listTop + (local / COLUMNS) * CARD_STEP;
            boolean ownListing = listing.sellerId.equals(this.snapshot.viewerId);
            long total = lotPrice(listing);

            ItemStack preview = this.previewStack(listing);
            if (!preview.isEmpty()) {
                graphics.item(preview, cardX + 7, cardY + 8);
                if (mouseX >= cardX + 6 && mouseX < cardX + 28
                        && mouseY >= cardY + 7 && mouseY < cardY + 30) {
                    graphics.setTooltipForNextFrame(this.font, preview, mouseX, mouseY);
                }
            }

            String displayName = preview.isEmpty() ? listing.displayName : preview.getHoverName().getString();
            String title = displayName + (listing.quantity > 1 ? " ×" + listing.quantity : "");
            graphics.text(this.font, Component.literal(this.font.plainSubstrByWidth(title, cardWidth - 34)),
                    cardX + 29, cardY + 8, this.theme().TEXT, !this.theme().custom);

            String seller = listing.sellerName + (ownListing ? " · Вы" : "");
            graphics.text(this.font, Component.literal(this.font.plainSubstrByWidth(seller, cardWidth - 16)),
                    cardX + 8, cardY + 28, this.theme().MUTED, !this.theme().custom);

            Component priceLabel = Component.translatable("gui.avecoins.player_market.total_price");
            graphics.text(this.font, priceLabel, cardX + 8, cardY + 44,
                    this.theme().TEXT, !this.theme().custom);
            int priceX = cardX + 8 + this.font.width(priceLabel) + 4;
            String priceText = Long.toString(total);
            graphics.text(this.font, Component.literal(priceText), priceX, cardY + 44,
                    this.theme().TEXT, !this.theme().custom);
            Identifier currencyId = Identifier.tryParse("vmtrade:vmcoin");
            if (currencyId != null && BuiltInRegistries.ITEM.containsKey(currencyId)) {
                graphics.item(new ItemStack((Item) BuiltInRegistries.ITEM.getValue(currencyId)),
                        priceX + this.font.width(priceText) + 3, cardY + 41);
            }
        }

        int maxPage = Math.max(0, (this.visibleListings.size() - 1) / LISTINGS_PER_PAGE);
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
