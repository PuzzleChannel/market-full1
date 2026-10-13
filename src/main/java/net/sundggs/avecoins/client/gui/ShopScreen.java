/*
 * Decompiled with CFR 0.152.
 */
package net.sundggs.avecoins.client.gui;

import com.google.gson.JsonObject;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import net.minecraft.ChatFormatting;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.gui.components.PlayerFaceExtractor;
import net.minecraft.client.gui.components.Tooltip;
import net.minecraft.client.gui.components.events.GuiEventListener;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.input.CharacterEvent;
import net.minecraft.client.input.KeyEvent;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.FormattedText;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.resources.Identifier;
import net.minecraft.world.entity.player.PlayerSkin;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.ItemLike;
import net.sundggs.avecoins.client.gui.AvecoinsScreen;
import net.sundggs.avecoins.client.gui.CartQuantityChange;
import net.sundggs.avecoins.client.gui.CartScreen;
import net.sundggs.avecoins.client.gui.CategoryPagination;
import net.sundggs.avecoins.client.gui.GuiIconRenderer;
import net.sundggs.avecoins.client.gui.GuiTextLayout;
import net.sundggs.avecoins.client.gui.PanelButton;
import net.sundggs.avecoins.client.gui.ProductCardButton;
import net.sundggs.avecoins.client.gui.ShiftKeyState;
import net.sundggs.avecoins.network.ClientNetworking;
import net.sundggs.avecoins.network.ServerboundAvecoinsPayload;
import net.sundggs.avecoins.shop.ShopData;
import net.sundggs.avecoins.shop.ShopService;

public final class ShopScreen
extends AvecoinsScreen {
    private static final int PRODUCTS_PER_PAGE = 12;
    private static final int CATEGORIES_PER_PAGE = 10;
    private static final int GRID_COLUMNS = 4;
    private static final int SHOP_WIDTH = 800;
    private static final int SHOP_HEIGHT = 430;
    private static final int SHOP_HEADER_HEIGHT = 40;
    private static final int SHOP_FOOTER_HEIGHT = 32;
    private static final int CATEGORY_WIDTH = 140;
    private static final int PRODUCT_GAP = 8;
    private static final int PRODUCT_ROW_STEP = 92;
    private static final int PRODUCT_TOP = 84;
    private static final int PAGINATION_Y_OFFSET = 370;
    private static final int CATEGORY_PAGINATION_Y_OFFSET = 366;
    private static final int WALLET_BUTTON_WIDTH = 26;
    private static final Identifier SEARCH_ICON = Identifier.fromNamespaceAndPath((String)"avecoins", (String)"textures/gui/icons/search.png");
    private static final Identifier CART_ICON = Identifier.fromNamespaceAndPath((String)"avecoins", (String)"textures/gui/icons/register.png");
    private static final Identifier SELL_ICON = Identifier.fromNamespaceAndPath((String)"avecoins", (String)"textures/gui/icons/price_tag.png");
    private static final Identifier WALLET_ICON = Identifier.fromNamespaceAndPath((String)"avecoins", (String)"textures/gui/icons/coin_wallet.png");
    private static final Identifier ADMIN_ICON = Identifier.fromNamespaceAndPath((String)"avecoins", (String)"textures/gui/icons/admin_button.png");
    private static final Identifier ALL_CATEGORY_ICON = Identifier.withDefaultNamespace((String)"chest");
    private static final List<String> BALANCE_ORDER = List.of("vmtrade:vmcoin", "avecoins:coppercoin", "avecoins:ironcoin", "avecoins:goldcoin", "avecoins:diamondcoin", "avecoins:netheritecoin", "avecoins:goldticket", "avecoins:diamondticket", "avecoins:netheriteticket");
    private final ShopService.ShopSnapshot snapshot;
    private String selectedCategory = "all";
    private String search = "";
    private int page;
    private int categoryPage;
    private int categoryPages = 1;
    private int totalPages = 1;
    private EditBox searchBox;
    private List<ShopData.Product> visibleProducts = List.of();
    private final List<ProductCardButton> productCards = new ArrayList<ProductCardButton>();

    public ShopScreen(ShopService.ShopSnapshot snapshot) {
        this(snapshot, "all", "", 0, 0);
    }

    public ShopScreen(ShopService.ShopSnapshot snapshot, String selectedCategory, String search, int page) {
        this(snapshot, selectedCategory, search, page, ShopScreen.initialCategoryPage(snapshot, selectedCategory));
    }

    public ShopScreen(ShopService.ShopSnapshot snapshot, String selectedCategory, String search, int page, int categoryPage) {
        super((Component)Component.literal((String)snapshot.shopName));
        this.snapshot = snapshot;
        this.useShopTheme(snapshot);
        String requestedCategory = selectedCategory == null ? "all" : selectedCategory;
        this.selectedCategory = requestedCategory.equals("all") || snapshot.categories.stream().anyMatch(category -> category.id.equals(requestedCategory)) ? requestedCategory : "all";
        this.search = search == null ? "" : search;
        this.page = Math.max(0, page);
        this.categoryPage = Math.max(0, categoryPage);
    }

    public ShopScreen refreshed(ShopService.ShopSnapshot snapshot) {
        return new ShopScreen(snapshot, this.selectedCategory, this.search, this.page, this.categoryPage);
    }

    protected void init() {
        this.productCards.clear();
        this.layoutPanel(800, 430);
        int buttonY = this.panelTop + 9;
        int closeX = this.panelLeft + this.panelWidth - 28;
        this.addRenderableWidget(new PanelButton(closeX, buttonY, 18, 22, (Component)Component.literal((String)"\u00d7"), PanelButton.Variant.DANGER, button -> this.onClose()));
        int cartCount = this.snapshot.cart.values().stream().mapToInt(Integer::intValue).sum();
        int cartX = closeX - 120;
        this.addRenderableWidget(new PanelButton(cartX, buttonY, 112, 22, (Component)Component.translatable((String)"gui.avecoins.shop.cart", (Object[])new Object[]{cartCount}), PanelButton.Variant.HEADER, button -> this.minecraft.gui.setScreen((Screen)new CartScreen(this.snapshot, 0, "", this.selectedCategory, this.search, this.page, this.categoryPage))).withIcon(CART_ICON, 14));
        PanelButton sell = new PanelButton(cartX - 122, buttonY, 112, 22, (Component)Component.translatable((String)"gui.avecoins.shop.sell_box"), PanelButton.Variant.HEADER, button -> ClientNetworking.send(new ServerboundAvecoinsPayload("request_sell_box", "{}"))).withIcon(SELL_ICON, 14);
        sell.active = this.snapshot.products.stream().anyMatch(product -> product.enabled && product.sellEnabled);
        sell.setTooltip(Tooltip.create((Component)Component.translatable((String)"gui.avecoins.shop.sell_box_help")));
        this.addRenderableWidget(sell);
        if (this.snapshot.administrator) {
            int adminX = this.panelLeft + 14 + this.font.width((FormattedText)this.displayedShopName()) + 12;
            PanelButton administration = new PanelButton(adminX, buttonY, 28, 22, (Component)Component.empty(), PanelButton.Variant.HEADER, button -> ClientNetworking.send(new ServerboundAvecoinsPayload("request_shop_admin", "{\"history\":false,\"page\":0}"))).withIcon(ADMIN_ICON, 16);
            administration.setTooltip(Tooltip.create((Component)Component.translatable((String)"gui.avecoins.shop.admin")));
            this.addRenderableWidget(administration);
        }
        if (this.minecraft.player != null) {
            int footerTop = this.panelTop + this.panelHeight - 32;
            PanelButton playerMarket = new PanelButton(this.panelLeft + (this.panelWidth - 148) / 2, footerTop + 5, 148, 22,
                    (Component)Component.translatable("gui.avecoins.player_market.tab"), PanelButton.Variant.HEADER,
                    button -> ClientNetworking.send(new ServerboundAvecoinsPayload("request_player_market", "{}")));
            playerMarket.setTooltip(Tooltip.create((Component)Component.translatable("gui.avecoins.player_market.tab_help")));
            this.addRenderableWidget(playerMarket);
            int walletX = this.panelLeft + this.panelWidth - 12 - 26;
            PanelButton wallet = new PanelButton(walletX, footerTop + 5, 26, 22, (Component)Component.empty(), PanelButton.Variant.HEADER, button -> ClientNetworking.send(new ServerboundAvecoinsPayload("request_wallet", "{}"))).withIcon(WALLET_ICON, 16);
            wallet.setTooltip(Tooltip.create((Component)Component.translatable((String)"gui.avecoins.wallet.open")));
            this.addRenderableWidget(wallet);
        }
        int searchWidth = 240;
        this.searchBox = new EditBox(this.font, this.panelLeft + (this.panelWidth - searchWidth) / 2, buttonY, searchWidth, 22, (Component)Component.translatable((String)"gui.avecoins.shop.search"));
        this.searchBox.setMaxLength(64);
        this.searchBox.setValue(this.search);
        this.searchBox.setHint((Component)Component.translatable((String)"gui.avecoins.shop.search"));
        this.addRenderableWidget(this.searchBox);
        int categoryX = this.panelLeft + 12;
        int categoryY = this.panelTop + 68;
        this.addCategoryButton(categoryX, categoryY, "all", (Component)Component.translatable((String)"gui.avecoins.shop.all"));
        this.categoryPages = CategoryPagination.pageCount(this.snapshot.categories.size(), 10);
        this.categoryPage = CategoryPagination.clampPage(this.categoryPage, this.snapshot.categories.size(), 10);
        int categoryStart = CategoryPagination.startIndex(this.categoryPage, this.snapshot.categories.size(), 10);
        int categoryEnd = CategoryPagination.endIndex(this.categoryPage, this.snapshot.categories.size(), 10);
        for (int index = categoryStart; index < categoryEnd; ++index) {
            ShopData.Category category = this.snapshot.categories.get(index);
            int y = categoryY + 26 + (index - categoryStart) * 26;
            this.addCategoryButton(categoryX, y, category.id, (Component)Component.literal((String)this.font.plainSubstrByWidth(category.name, 84)));
        }
        if (this.categoryPages > 1) {
            this.addCategoryPageButtons();
        }
        List<ShopData.Product> visible = this.filteredProducts();
        this.visibleProducts = visible;
        int contentLeft = this.panelLeft + 140 + 16;
        int contentTop = this.panelTop + 84;
        int contentWidth = this.panelWidth - 140 - 28;
        int productWidth = (contentWidth - 24) / 4;
        int maxPage = Math.max(0, (visible.size() - 1) / 12);
        this.totalPages = maxPage + 1;
        this.page = Math.min(this.page, maxPage);
        int start = this.page * 12;
        int end = Math.min(visible.size(), start + 12);
        for (int index = start; index < end; ++index) {
            ShopData.Product product2 = visible.get(index);
            int local = index - start;
            int x = contentLeft + local % 4 * (productWidth + 8);
            int y = contentTop + local / 4 * 92;
            int quantity = this.snapshot.cart.getOrDefault(product2.id, 0);
            ProductCardButton card = new ProductCardButton(x, y, productWidth, product2, quantity, () -> this.changeQuantity(product2, quantity, 1), () -> this.changeQuantity(product2, quantity, -1), () -> this.changeQuantity(product2, quantity, 1), () -> this.setQuantity(product2.id, 0));
            this.productCards.add(card);
            this.addRenderableWidget(card);
        }
        if (maxPage > 0) {
            int navY = this.panelTop + 370;
            int contentCenter = contentLeft + contentWidth / 2;
            this.addRenderableWidget(new PanelButton(contentCenter - 88, navY, 36, 18, (Component)Component.literal((String)"\u2039"), PanelButton.Variant.SECONDARY, button -> {
                this.page = Math.max(0, this.page - 1);
                this.rebuildWidgets();
            }));
            this.addRenderableWidget(new PanelButton(contentCenter + 52, navY, 36, 18, (Component)Component.literal((String)"\u203a"), PanelButton.Variant.SECONDARY, button -> {
                this.page = Math.min(maxPage, this.page + 1);
                this.rebuildWidgets();
            }));
        }
    }

    private void addCategoryButton(int x, int y, String id, Component name) {
        MutableComponent label = Component.literal((String)"   ").append(name);
        this.addRenderableWidget(new PanelButton(x, y, 116, 22, (Component)label, this.selectedCategory.equals(id) ? PanelButton.Variant.SELECTED : PanelButton.Variant.SECONDARY, button -> {
            this.selectedCategory = id;
            this.page = 0;
            this.rebuildWidgets();
        }));
    }

    private void addCategoryPageButtons() {
        int centerX = this.panelLeft + 70;
        int y = this.panelTop + 366;
        PanelButton previous = new PanelButton(centerX - 60, y, 22, 18, (Component)Component.literal((String)"\u2039"), PanelButton.Variant.SECONDARY, button -> this.changeCategoryPage(this.categoryPage - 1));
        previous.active = this.categoryPage > 0;
        this.addRenderableWidget(previous);
        PanelButton next = new PanelButton(centerX + 38, y, 22, 18, (Component)Component.literal((String)"\u203a"), PanelButton.Variant.SECONDARY, button -> this.changeCategoryPage(this.categoryPage + 1));
        next.active = this.categoryPage + 1 < this.categoryPages;
        this.addRenderableWidget(next);
    }

    private boolean changeCategoryPage(int requestedPage) {
        int nextPage = CategoryPagination.clampPage(requestedPage, this.snapshot.categories.size(), 10);
        if (nextPage == this.categoryPage) {
            return false;
        }
        this.categoryPage = nextPage;
        this.rebuildWidgets();
        return true;
    }

    private List<ShopData.Product> filteredProducts() {
        String normalizedSearch = this.search.toLowerCase(Locale.ROOT).trim();
        ArrayList<ShopData.Product> result = new ArrayList<ShopData.Product>();
        for (ShopData.Product product : this.snapshot.products) {
            if (!this.selectedCategory.equals("all") && !product.categoryId.equals(this.selectedCategory) || !normalizedSearch.isEmpty() && !product.name.toLowerCase(Locale.ROOT).contains(normalizedSearch) && !product.itemId.toLowerCase(Locale.ROOT).contains(normalizedSearch)) continue;
            result.add(product);
        }
        return result;
    }

    private void setQuantity(String productId, int quantity) {
        JsonObject request = new JsonObject();
        request.addProperty("productId", productId);
        request.addProperty("quantity", (Number)quantity);
        ClientNetworking.send(new ServerboundAvecoinsPayload("cart_quantity", request.toString()));
    }

    private void changeQuantity(ShopData.Product product, int quantity, int direction) {
        int updated = CartQuantityChange.adjust(quantity, direction, ShiftKeyState.isDown(), product.stock);
        if (updated != quantity) {
            this.setQuantity(product.id, updated);
        }
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

    @Override
    public boolean mouseScrolled(double mouseX, double mouseY, double scrollX, double scrollY) {
        boolean overCategories;
        int x = this.virtualMouseX(mouseX);
        int y = this.virtualMouseY(mouseY);
        boolean bl = overCategories = x >= this.panelLeft && x < this.panelLeft + 140 && y >= this.panelTop + 40 && y < this.panelTop + this.panelHeight - 32;
        if (overCategories && scrollY != 0.0 && this.categoryPages > 1) {
            int direction;
            int n = direction = scrollY > 0.0 ? -1 : 1;
            if (this.changeCategoryPage(this.categoryPage + direction)) {
                return true;
            }
        }
        return super.mouseScrolled(mouseX, mouseY, scrollX, scrollY);
    }

    private void refreshSearchIfChanged() {
        if (this.searchBox == null || this.search.equals(this.searchBox.getValue())) {
            return;
        }
        this.search = this.searchBox.getValue();
        this.page = 0;
        this.rebuildWidgets();
        this.searchBox.setFocused(true);
        this.searchBox.moveCursorToEnd(false);
        this.setFocused(this.searchBox);
    }

    @Override
    protected int panelHeaderHeight() {
        return 40;
    }

    @Override
    protected int panelFooterHeight() {
        return 32;
    }

    @Override
    protected void renderPanel(GuiGraphicsExtractor graphics, int mouseX, int mouseY, float partialTick) {
        Component shopName = this.displayedShopName();
        graphics.text(this.font, shopName, this.panelLeft + 14, this.panelTop + 16, this.theme().TEXT, false);
        GuiIconRenderer.blit(graphics, SEARCH_ICON, this.searchBox.getX() - 20, this.searchBox.getY() + 3, 16, 16, 32, 32);
        graphics.fill(this.panelLeft + 140, this.panelTop + 40, this.panelLeft + 140 + 1, this.panelTop + this.panelHeight - 32, this.theme().DIVIDER);
        MutableComponent categoryHeading = Component.literal((String)"\u2630 ").append((Component)Component.translatable((String)"gui.avecoins.shop.categories")).withStyle(ChatFormatting.BOLD);
        GuiTextLayout.drawCentered(graphics, this.font, (Component)categoryHeading, this.panelLeft + 70, this.panelTop + 51, this.theme().TEXT, !this.theme().custom);
        this.renderCategoryIcons(graphics);
        if (this.categoryPages > 1) {
            GuiTextLayout.drawCentered(graphics, this.font, (Component)Component.translatable((String)"gui.avecoins.shop.page", (Object[])new Object[]{this.categoryPage + 1, this.categoryPages}), this.panelLeft + 70, this.panelTop + 366 + 5, this.theme().MUTED, !this.theme().custom);
        }
        this.renderCategoryContext(graphics);
        this.renderFooter(graphics);
        if (this.totalPages > 1) {
            int contentLeft = this.panelLeft + 140 + 16;
            int contentWidth = this.panelWidth - 140 - 28;
            GuiTextLayout.drawCentered(graphics, this.font, (Component)Component.translatable((String)"gui.avecoins.shop.page", (Object[])new Object[]{this.page + 1, this.totalPages}), contentLeft + contentWidth / 2, this.panelTop + 370 + 5, this.theme().MUTED, !this.theme().custom);
        }
        if (this.filteredProducts().isEmpty()) {
            GuiTextLayout.drawCentered(graphics, this.font, (Component)Component.translatable((String)"gui.avecoins.shop.empty"), this.panelLeft + 140 + (this.panelWidth - 140) / 2, this.panelTop + this.panelHeight / 2, this.theme().MUTED, !this.theme().custom);
        }
        this.renderHoveredProductTooltip(graphics, mouseX, mouseY);
    }

    private void renderHoveredProductTooltip(GuiGraphicsExtractor graphics, int mouseX, int mouseY) {
        List<ShopData.Product> visible = this.visibleProducts;
        int start = this.page * 12;
        int end = Math.min(visible.size(), start + 12);
        int contentLeft = this.panelLeft + 140 + 16;
        int contentWidth = this.panelWidth - 140 - 28;
        int productWidth = (contentWidth - 24) / 4;
        for (int index = start; index < end; ++index) {
            int iconY;
            ShopData.Product product = visible.get(index);
            int local = index - start;
            int x = contentLeft + local % 4 * (productWidth + 8);
            int y = this.panelTop + 84 + local / 4 * 92;
            int n = iconY = this.snapshot.cart.getOrDefault(product.id, 0) > 0 ? 3 : 9;
            if (mouseX < x + productWidth / 2 - 12 || mouseX >= x + productWidth / 2 + 12 || mouseY < y + iconY || mouseY >= y + iconY + 24) continue;
            ItemStack stack = this.productCards.stream().filter(card -> card.getX() == x && card.getY() == y).map(ProductCardButton::displayStack).findFirst().orElse(ItemStack.EMPTY);
            if (!stack.isEmpty()) {
                graphics.setTooltipForNextFrame(this.font, stack, mouseX, mouseY);
            }
            return;
        }
    }

    private void renderCategoryIcons(GuiGraphicsExtractor graphics) {
        int x = this.panelLeft + 19;
        this.renderItemIcon(graphics, ALL_CATEGORY_ICON, x, this.panelTop + 71);
        int start = CategoryPagination.startIndex(this.categoryPage, this.snapshot.categories.size(), 10);
        int end = CategoryPagination.endIndex(this.categoryPage, this.snapshot.categories.size(), 10);
        for (int index = start; index < end; ++index) {
            ShopData.Category category = this.snapshot.categories.get(index);
            int y = this.panelTop + 97 + (index - start) * 26;
            Identifier icon = Identifier.tryParse((String)category.iconItem);
            this.renderItemIcon(graphics, icon, x, y);
        }
    }

    private void renderCategoryContext(GuiGraphicsExtractor graphics) {
        int contentLeft = this.panelLeft + 140 + 16;
        ShopData.Category category = this.selectedCategoryData();
        Identifier icon = category == null ? ALL_CATEGORY_ICON : Identifier.tryParse((String)category.iconItem);
        this.renderItemIcon(graphics, icon, contentLeft, this.panelTop + 47);
        MutableComponent name = category == null ? Component.translatable((String)"gui.avecoins.shop.all_products") : Component.literal((String)category.name);
        graphics.text(this.font, (Component)name.copy().withStyle(ChatFormatting.BOLD), contentLeft + 22, this.panelTop + 50, this.theme().TEXT, false);
        MutableComponent description = category == null ? Component.translatable((String)"gui.avecoins.shop.all_description") : Component.literal((String)category.description);
        graphics.text(this.font, this.font.plainSubstrByWidth(description.getString(), this.panelWidth - 140 - 56), contentLeft + 22, this.panelTop + 62, this.theme().MUTED, !this.theme().custom);
        graphics.fill(contentLeft, this.panelTop + 75, this.panelLeft + this.panelWidth - 12, this.panelTop + 76, this.theme().DIVIDER);
    }

    private ShopData.Category selectedCategoryData() {
        if (this.selectedCategory.equals("all")) {
            return null;
        }
        return this.snapshot.categories.stream().filter(category -> category.id.equals(this.selectedCategory)).findFirst().orElse(null);
    }

    private void renderItemIcon(GuiGraphicsExtractor graphics, Identifier id, int x, int y) {
        if (id != null && BuiltInRegistries.ITEM.containsKey(id)) {
            graphics.item(new ItemStack((ItemLike)BuiltInRegistries.ITEM.getValue(id)), x, y);
        }
    }

    private void renderFooter(GuiGraphicsExtractor graphics) {
        int top = this.panelTop + this.panelHeight - 32;
        if (this.minecraft.player != null) {
            PlayerFaceExtractor.extractRenderState((GuiGraphicsExtractor)graphics, (PlayerSkin)this.minecraft.player.getSkin(), (int)(this.panelLeft + 12), (int)(top + 6), (int)20);
            graphics.text(this.font, this.minecraft.player.getName(), this.panelLeft + 40, top + 12, this.theme().TEXT, !this.theme().custom);
        }
        int x = this.panelLeft + this.panelWidth - 12 - 26 - 8;
        for (int index = BALANCE_ORDER.size() - 1; index >= 0; --index) {
            Identifier id;
            String currencyId = BALANCE_ORDER.get(index);
            Long balance = this.snapshot.balances.get(currencyId);
            if (balance == null || balance <= 0L || (id = Identifier.tryParse((String)currencyId)) == null || !BuiltInRegistries.ITEM.containsKey(id)) continue;
            String amount = Long.toString(balance);
            int textWidth = this.font.width(amount);
            graphics.text(this.font, amount, x -= textWidth, top + 12, this.theme().TEXT, !this.theme().custom);
            graphics.item(new ItemStack((ItemLike)BuiltInRegistries.ITEM.getValue(id)), x -= 20, top + 8);
            x -= 10;
        }
    }

    private Component displayedShopName() {
        int titleWidth = this.snapshot.administrator ? 178 : 230;
        return Component.literal((String)this.font.plainSubstrByWidth(this.title.getString(), titleWidth)).withStyle(ChatFormatting.BOLD);
    }

    private static int initialCategoryPage(ShopService.ShopSnapshot snapshot, String selectedCategory) {
        if (selectedCategory == null || selectedCategory.equals("all")) {
            return 0;
        }
        for (int index = 0; index < snapshot.categories.size(); ++index) {
            if (!snapshot.categories.get((int)index).id.equals(selectedCategory)) continue;
            return CategoryPagination.pageForIndex(index, snapshot.categories.size(), 10);
        }
        return 0;
    }
}

