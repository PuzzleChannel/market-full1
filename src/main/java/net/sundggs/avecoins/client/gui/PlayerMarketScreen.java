package net.sundggs.avecoins.client.gui;

import java.util.ArrayList;
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
import net.minecraft.world.item.BlockItem;
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
    private static final int CARD_HEIGHT = 66;
    private static final int CARD_STEP = 73;
    private static final Identifier WALLET_ICON = Identifier.fromNamespaceAndPath("avecoins", "textures/gui/icons/coin_wallet.png");
    private static final Identifier COIN_ID = Identifier.fromNamespaceAndPath("vmtrade", "vmcoin");
    private static final String[] FILTER_KEYS = {
            "gui.avecoins.player_market.filter_all",
            "gui.avecoins.player_market.filter_mine",
            "gui.avecoins.player_market.filter_others"
    };
    private static final String[] CATEGORY_KEYS = {
            "gui.avecoins.player_market.category_all",
            "gui.avecoins.player_market.category_blocks",
            "gui.avecoins.player_market.category_tools",
            "gui.avecoins.player_market.category_combat",
            "gui.avecoins.player_market.category_materials",
            "gui.avecoins.player_market.category_food",
            "gui.avecoins.player_market.category_potions",
            "gui.avecoins.player_market.category_redstone",
            "gui.avecoins.player_market.category_decoration",
            "gui.avecoins.player_market.category_other"
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
    private int categoryMode;
    private String resultCode;
    private String search;
    private int page;
    private EditBox searchBox;
    private List<PlayerMarketService.MarketListing> visibleListings = List.of();
    private final List<PanelButton> listingControls = new ArrayList<>();
    private String confirmingListingId = "";

    public PlayerMarketScreen(PlayerMarketService.MarketSnapshot snapshot, String resultCode) {
        this(snapshot, resultCode, "", 0, 0, 0, 0);
    }

    private PlayerMarketScreen(PlayerMarketService.MarketSnapshot snapshot, String resultCode, String search,
                                int page, int filterMode, int sortMode, int categoryMode) {
        super(Component.translatable("gui.avecoins.player_market.title"));
        this.snapshot = snapshot;
        this.resultCode = resultCode == null ? "" : resultCode;
        this.search = search == null ? "" : search;
        this.page = Math.max(0, page);
        this.filterMode = Math.floorMod(filterMode, FILTER_KEYS.length);
        this.sortMode = Math.floorMod(sortMode, SORT_KEYS.length);
        this.categoryMode = Math.floorMod(categoryMode, CATEGORY_KEYS.length);
        ShopService.ShopSnapshot themeSnapshot = new ShopService.ShopSnapshot();
        themeSnapshot.shopColor = snapshot.shopColor;
        this.useShopTheme(themeSnapshot);
    }

    public PlayerMarketScreen refreshed(PlayerMarketService.MarketSnapshot snapshot, String resultCode) {
        PlayerMarketScreen refreshed = new PlayerMarketScreen(snapshot, resultCode,
                this.searchBox == null ? this.search : this.searchBox.getValue(),
                this.page, this.filterMode, this.sortMode, this.categoryMode);
        refreshed.confirmingListingId = this.confirmingListingId;
        return refreshed;
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

        int ordersX = myListingsX - 118 - 8;
        this.addRenderableWidget(new PanelButton(ordersX, headerY, 118, 20,
                Component.translatable("gui.avecoins.player_market.orders"), PanelButton.Variant.HEADER,
                button -> ClientNetworking.send(new ServerboundAvecoinsPayload("request_player_market_orders", "{}"))));

        int createX = ordersX - 142 - 8;
        this.addRenderableWidget(new PanelButton(createX, headerY, 142, 20,
                Component.translatable("gui.avecoins.player_market.list_item"), PanelButton.Variant.SUCCESS,
                button -> this.minecraft.gui.setScreen(new PlayerMarketCreateScreen(this.snapshot, ""))));

        int walletX = this.panelLeft + this.panelWidth - 12 - 26;
        int walletY = this.panelTop + this.panelHeight - 56;
        PanelButton wallet = new PanelButton(walletX, walletY, 26, 22, Component.empty(),
                PanelButton.Variant.HEADER,
                button -> ClientNetworking.send(new ServerboundAvecoinsPayload("request_wallet", "{}")))
                .withIcon(WALLET_ICON, 16);
        this.addRenderableWidget(wallet);

        int controlsWidth = 150 + 8 + 300 + 8 + 150;
        int listingColumnLeft = this.panelLeft + 120;
        int listingColumnWidth = this.panelWidth - 132;
        int controlsLeft = listingColumnLeft + (listingColumnWidth - controlsWidth) / 2;
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

        int categoryX = this.panelLeft + 10;
        int categoryY = this.panelTop + 86;
        for (int i = 0; i < CATEGORY_KEYS.length; i++) {
            final int selectedCategory = i;
            this.addRenderableWidget(new PanelButton(categoryX, categoryY + i * 24, 100, 21,
                    Component.translatable(CATEGORY_KEYS[i]),
                    i == this.categoryMode ? PanelButton.Variant.HEADER : PanelButton.Variant.SECONDARY,
                    button -> {
                        this.categoryMode = selectedCategory;
                        this.page = 0;
                        this.rebuildWidgets();
                    }));
        }


        this.rebuildListingControls();

    }


    private void rebuildListingControls() {
        for (PanelButton control : this.listingControls) this.removeWidget(control);
        this.listingControls.clear();

        this.visibleListings = this.filteredListings();
        int maxPage = Math.max(0, (this.visibleListings.size() - 1) / LISTINGS_PER_PAGE);
        this.page = Math.min(this.page, maxPage);

        int listX = this.panelLeft + 120;
        int rowWidth = this.panelWidth - 132;
        int cardWidth = (rowWidth - (COLUMNS - 1) * 8) / COLUMNS;
        int listTop = this.panelTop + 100;
        int start = this.page * LISTINGS_PER_PAGE;
        int end = Math.min(this.visibleListings.size(), start + LISTINGS_PER_PAGE);

        for (int index = start; index < end; index++) {
            PlayerMarketService.MarketListing listing = this.visibleListings.get(index);
            int local = index - start;
            int cardX = listX + (local % COLUMNS) * (cardWidth + 8);
            int cardY = listTop + (local / COLUMNS) * CARD_STEP;
            boolean ownListing = listing.sellerId.equals(this.snapshot.viewerId);
            long total = lotPrice(listing);
            boolean confirming = listing.id.equals(this.confirmingListingId);

            if (ownListing) {
                // A seller can only remove their own listing; never attach a purchase action to it.
                PanelButton remove = new PanelButton(cardX + cardWidth - 23, cardY + 4, 18, 18,
                        Component.literal("×"), PanelButton.Variant.DANGER,
                        button -> {
                            com.google.gson.JsonObject payload = new com.google.gson.JsonObject();
                            payload.addProperty("id", listing.id);
                            ClientNetworking.send(new ServerboundAvecoinsPayload("market_cancel", payload.toString()));
                        });
                this.listingControls.add(this.addRenderableWidget(remove));
            } else if (confirming) {
                PanelButton confirm = new PanelButton(cardX + cardWidth - 104, cardY + CARD_HEIGHT - 21, 100, 18,
                        Component.translatable("gui.avecoins.player_market.confirm_purchase_button"),
                        PanelButton.Variant.SUCCESS, button -> {
                            if (this.snapshot.walletBalance < total) {
                                this.confirmingListingId = "";
                                this.resultCode = "market_wallet_insufficient";
                                this.rebuildListingControls();
                                return;
                            }
                            this.confirmingListingId = "";
                            this.resultCode = "";
                            com.google.gson.JsonObject payload = new com.google.gson.JsonObject();
                            payload.addProperty("id", listing.id);
                            ClientNetworking.send(new ServerboundAvecoinsPayload("market_buy", payload.toString()));
                        }).withTrailingItemAmount(COIN_ID, total);
                confirm.setTooltip(net.minecraft.client.gui.components.Tooltip.create(
                        Component.translatable("gui.avecoins.player_market.purchase_tooltip",
                                total, this.snapshot.walletBalance, listing.deliveryMinutes)));
                PanelButton cancel = new PanelButton(cardX + cardWidth - 160, cardY + CARD_HEIGHT - 21, 52, 18,
                        Component.translatable("gui.avecoins.player_market.cancel_confirmation"),
                        PanelButton.Variant.SECONDARY, button -> {
                            this.confirmingListingId = "";
                            this.resultCode = "";
                            this.rebuildListingControls();
                        });
                this.listingControls.add(this.addRenderableWidget(cancel));
                this.listingControls.add(this.addRenderableWidget(confirm));
            } else {
                PanelButton buy = new PanelButton(cardX + cardWidth - 104, cardY + CARD_HEIGHT - 21, 100, 18,
                        Component.translatable("gui.avecoins.player_market.buy"),
                        PanelButton.Variant.SUCCESS, button -> {
                            if (this.snapshot.walletBalance < total) {
                                this.confirmingListingId = "";
                                this.resultCode = "market_wallet_insufficient";
                                this.rebuildListingControls();
                                return;
                            }
                            this.confirmingListingId = listing.id;
                            this.resultCode = "market_confirm_purchase";
                            this.rebuildListingControls();
                        }).withTrailingItemAmount(COIN_ID, total);
                if (this.snapshot.walletBalance < total) {
                    long shortfall = total - this.snapshot.walletBalance;
                    buy.setTooltip(net.minecraft.client.gui.components.Tooltip.create(
                            Component.translatable("gui.avecoins.player_market.purchase_shortfall_tooltip",
                                    shortfall, total, this.snapshot.walletBalance)));
                } else {
                    buy.setTooltip(net.minecraft.client.gui.components.Tooltip.create(
                            Component.translatable("gui.avecoins.player_market.purchase_tooltip",
                                    total, this.snapshot.walletBalance, listing.deliveryMinutes)));
                }
                this.listingControls.add(this.addRenderableWidget(buy));
            }
        }

        if (maxPage > 0) {
            int center = listX + rowWidth / 2;
            int navY = this.panelTop + 347;
            PanelButton previous = new PanelButton(center - 60, navY, 24, 18, Component.literal("‹"),
                    PanelButton.Variant.SECONDARY, button -> {
                        this.page = Math.max(0, this.page - 1);
                        this.rebuildListingControls();
                    });
            previous.active = this.page > 0;
            this.listingControls.add(this.addRenderableWidget(previous));

            PanelButton next = new PanelButton(center + 36, navY, 24, 18, Component.literal("›"),
                    PanelButton.Variant.SECONDARY, button -> {
                        this.page = Math.min(maxPage, this.page + 1);
                        this.rebuildListingControls();
                    });
            next.active = this.page < maxPage;
            this.listingControls.add(this.addRenderableWidget(next));
        }
    }

    private List<PlayerMarketService.MarketListing> filteredListings() {
        String query = this.search == null ? "" : this.search.trim().toLowerCase(Locale.ROOT);
        return this.snapshot.listings.stream()
                .filter(listing -> {
                    boolean own = listing.sellerId.equals(this.snapshot.viewerId);
                    boolean matchesFilter = switch (this.filterMode) {
                        case 1 -> own;
                        case 2 -> !own;
                        default -> true;
                    };
                    boolean matchesCategory = matchesCategory(listing);
                    boolean matchesSearch = query.isEmpty()
                            || (listing.displayName != null && listing.displayName.toLowerCase(Locale.ROOT).contains(query))
                            || (listing.itemId != null && listing.itemId.toLowerCase(Locale.ROOT).contains(query))
                            || (listing.sellerName != null && listing.sellerName.toLowerCase(Locale.ROOT).contains(query));
                    return matchesFilter && matchesCategory && matchesSearch;
                })
                .sorted(this.listingComparator())
                .toList();
    }


    private boolean matchesCategory(PlayerMarketService.MarketListing listing) {
        if (this.categoryMode == 0) return true;
        Identifier id = Identifier.tryParse(listing.itemId);
        if (id == null || !BuiltInRegistries.ITEM.containsKey(id)) return this.categoryMode == 9;
        Item item = (Item) BuiltInRegistries.ITEM.getValue(id);
        String path = id.getPath();
        int category;
        if (path.contains("sword") || path.contains("bow") || path.contains("crossbow") || path.contains("trident")
                || path.contains("shield") || path.contains("mace") || path.contains("helmet") || path.contains("chestplate")
                || path.contains("leggings") || path.contains("boots") || path.contains("horse_armor") || path.contains("wolf_armor")
                || path.equals("arrow") || path.endsWith("_arrow") || path.contains("spear")) category = 3;
        else if (path.contains("pickaxe") || path.contains("shovel") || path.contains("hoe") || path.contains("fishing_rod")
                || path.contains("shears") || path.contains("flint_and_steel") || path.contains("brush") || path.contains("spyglass")
                || path.contains("compass") || path.contains("clock") || path.contains("lead") || path.contains("name_tag")
                || path.contains("bucket") || path.contains("carrot_on_a_stick") || path.contains("warped_fungus_on_a_stick")
                || path.contains("elytra")) category = 2;
        else if (path.contains("potion") || path.contains("tipped_arrow") || path.contains("brewing") || path.contains("experience_bottle")
                || path.contains("enchanted_book") || path.contains("book") || path.contains("totem") || path.contains("end_crystal")) category = 6;
        else if (path.contains("redstone") || path.contains("repeater") || path.contains("comparator") || path.contains("observer")
                || path.contains("piston") || path.contains("lever") || path.contains("button") || path.contains("pressure_plate")
                || path.contains("tripwire") || path.contains("target") || path.contains("daylight_detector") || path.contains("dispenser")
                || path.contains("dropper") || path.contains("hopper") || path.contains("rail") || path.contains("minecart")) category = 7;
        else if (path.contains("boat") || path.contains("raft") || path.equals("saddle")) category = 2;
        else if (path.contains("apple") || path.contains("bread") || path.contains("beef") || path.contains("porkchop")
                || path.contains("chicken") || path.contains("mutton") || path.contains("rabbit") || path.contains("cod")
                || path.contains("salmon") || path.contains("tropical_fish") || path.contains("pufferfish") || path.contains("stew")
                || path.contains("soup") || path.contains("carrot") || path.contains("potato") || path.contains("beetroot")
                || path.contains("melon_slice") || path.contains("cookie") || path.contains("cake") || path.contains("pie")
                || path.contains("berries") || path.contains("chorus_fruit") || path.contains("golden_carrot")
                || path.contains("golden_apple") || path.contains("honey_bottle") || path.equals("honeycomb")) category = 5;
        else if (!(item instanceof BlockItem) && (path.contains("ingot") || path.contains("nugget") || path.contains("raw_") || path.contains("diamond")
                || path.contains("emerald") || path.contains("lapis") || path.contains("quartz") || path.contains("amethyst")
                || path.contains("coal") || path.contains("charcoal") || path.contains("flint") || path.contains("string")
                || path.contains("leather") || path.contains("feather") || path.contains("bone") || path.contains("gunpowder")
                || path.contains("paper") || path.contains("slime_ball") || path.contains("prismarine_shard")
                || path.contains("phantom_membrane") || path.contains("scute") || path.contains("shell") || path.contains("echo_shard")
                || path.contains("disc_fragment") || path.contains("shard"))) category = 4;
        else if (path.contains("banner") || path.contains("painting") || path.contains("item_frame") || path.contains("flower")
                || path.contains("tulip") || path.contains("dye") || path.contains("bed") || path.contains("candle")
                || path.contains("sign") || path.contains("pot") || path.contains("head") || path.contains("skull")
                || path.contains("armor_stand") || path.contains("lantern") || path.contains("torch") || path.contains("carpet")
                || path.contains("music_disc") || path.contains("coral") || path.contains("sapling") || path.contains("leaves")
                || path.contains("hanging_sign")) category = 8;
        else if (item instanceof BlockItem) category = 1;
        else category = 9;
        return this.categoryMode == category;
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
        this.rebuildListingControls();
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
            int listTop = this.panelTop + 100;
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
        this.drawSectionTitleAt(graphics, Component.translatable("gui.avecoins.player_market.title"),
                this.panelLeft + 75, this.panelTop + 10);
        int noticeCount = Math.min(3, this.snapshot.saleNotices.size());
        int noticeGap = 6;
        int noticeWidth = (this.panelWidth - 132 - (noticeCount - 1) * noticeGap) / Math.max(1, noticeCount);
        for (int i = 0; i < noticeCount; i++) {
            PlayerMarketService.MarketSaleNotice notice = this.snapshot.saleNotices.get(i);
            int noticeX = this.panelLeft + 120 + i * (noticeWidth + noticeGap);
            int noticeY = this.panelTop + 62;
            graphics.fill(noticeX, noticeY, noticeX + noticeWidth, noticeY + 20, this.theme().SUCCESS);
            graphics.fill(noticeX + 1, noticeY + 1, noticeX + noticeWidth - 1, noticeY + 19, this.theme().SURFACE);
            String itemLabel = notice.itemName + (notice.quantity > 1 ? " ×" + notice.quantity : "");
            String line = this.font.plainSubstrByWidth(itemLabel, Math.max(20, noticeWidth - 60))
                    + " · " + notice.price + " ВМ";
            graphics.text(this.font, Component.literal(this.font.plainSubstrByWidth(line, noticeWidth - 8)),
                    noticeX + 4, noticeY + 6, this.theme().TEXT, !this.theme().custom);
        }
        this.drawSectionTitleAt(graphics, Component.translatable("gui.avecoins.player_market.listings"),
                this.panelLeft + 120 + (this.panelWidth - 132) / 2, this.panelTop + 88);

        int listX = this.panelLeft + 120;
        int rowWidth = this.panelWidth - 132;
        int cardWidth = (rowWidth - (COLUMNS - 1) * 8) / COLUMNS;
        int listTop = this.panelTop + 100;
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
            graphics.text(this.font,
                    Component.literal(this.font.plainSubstrByWidth(title, cardWidth - (ownListing ? 52 : 34))),
                    cardX + 29, cardY + 8, this.theme().TEXT, !this.theme().custom);

            String seller = listing.sellerName + (ownListing ? " · Вы" : "");
            graphics.text(this.font, Component.literal(this.font.plainSubstrByWidth(seller, cardWidth - 16)),
                    cardX + 8, cardY + 27, this.theme().MUTED, !this.theme().custom);
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
