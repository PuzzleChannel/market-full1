/*
 * Decompiled with CFR 0.152.
 */
package net.sundggs.avecoins.client.gui;

import com.google.gson.JsonObject;
import java.util.List;
import net.minecraft.ChatFormatting;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.components.events.GuiEventListener;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.resources.Identifier;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.ItemLike;
import net.sundggs.avecoins.client.gui.AvecoinsScreen;
import net.sundggs.avecoins.client.gui.CategoryEditorScreen;
import net.sundggs.avecoins.client.gui.ConfirmActionScreen;
import net.sundggs.avecoins.client.gui.GuiTextLayout;
import net.sundggs.avecoins.client.gui.PanelButton;
import net.sundggs.avecoins.client.gui.ProductDisplayStack;
import net.sundggs.avecoins.client.gui.ProductEditorScreen;
import net.sundggs.avecoins.client.gui.ShopScreen;
import net.sundggs.avecoins.network.ClientNetworking;
import net.sundggs.avecoins.network.ServerboundAvecoinsPayload;
import net.sundggs.avecoins.shop.ShopData;
import net.sundggs.avecoins.shop.ShopService;

public final class AdminShopScreen
extends AvecoinsScreen {
    private static final int PANEL_WIDTH = 800;
    private static final int PANEL_HEIGHT = 460;
    private static final int SIDEBAR_WIDTH = 180;
    private static final int CATEGORIES_PER_PAGE = 10;
    private static final int PRODUCTS_PER_PAGE = 8;
    private static final int CATEGORY_COLUMN_HEIGHT = 355;
    private static final int CATEGORY_COLUMN_TOP_OFFSET = 52;
    private final ShopService.ShopSnapshot snapshot;
    private final String result;
    private String selectedCategory;
    private int categoryPage;
    private int productPage;

    public AdminShopScreen(ShopService.ShopSnapshot snapshot, String result) {
        this(snapshot, result, snapshot.categories.isEmpty() ? "" : snapshot.categories.getFirst().id, 0, 0);
    }

    public AdminShopScreen(ShopService.ShopSnapshot snapshot, String result, String selectedCategory) {
        this(snapshot, result, selectedCategory, 0, 0);
    }

    public AdminShopScreen(ShopService.ShopSnapshot snapshot, String result, String selectedCategory, int categoryPage, int productPage) {
        super((Component)Component.translatable((String)"gui.avecoins.shop_admin.title"));
        this.snapshot = snapshot;
        this.useShopTheme(snapshot);
        String string = this.result = result == null ? "" : result;
        this.selectedCategory = snapshot.categories.stream().anyMatch(category -> category.id.equals(selectedCategory)) ? selectedCategory : (snapshot.categories.isEmpty() ? "" : snapshot.categories.getFirst().id);
        this.categoryPage = Math.max(0, categoryPage);
        this.productPage = Math.max(0, productPage);
    }

    public String selectedCategoryId() {
        return this.selectedCategory;
    }

    public int categoryPage() {
        return this.categoryPage;
    }

    public int productPage() {
        return this.productPage;
    }

    protected void init() {
        ShopData.Category selected;
        this.layoutPanel(800, 460);
        this.addHeaderBackButton(new ShopScreen(this.snapshot));
        this.addRenderableWidget(new PanelButton(this.panelLeft + this.panelWidth - 108, this.panelTop + 6, 100, 18, (Component)Component.translatable((String)"gui.avecoins.shop_admin.history"), PanelButton.Variant.HEADER, button -> ClientNetworking.send(new ServerboundAvecoinsPayload("request_shop_admin", "{\"history\":true,\"page\":0}"))));
        int left = this.panelLeft + 12;
        this.addRenderableWidget(new PanelButton(left, this.panelTop + 52 + 40, 156, 22, (Component)Component.translatable((String)"gui.avecoins.shop_admin.new_category"), PanelButton.Variant.SELECTED, button -> this.minecraft.gui.setScreen((Screen)new CategoryEditorScreen(this, this.snapshot, null))));
        int categoryPages = Math.max(1, (this.snapshot.categories.size() + 10 - 1) / 10);
        this.categoryPage = Math.min(this.categoryPage, categoryPages - 1);
        int categoryStart = this.categoryPage * 10;
        int categoryEnd = Math.min(this.snapshot.categories.size(), categoryStart + 10);
        for (int index = categoryStart; index < categoryEnd; ++index) {
            ShopData.Category category = this.snapshot.categories.get(index);
            int y = this.panelTop + 52 + 68 + (index - categoryStart) * 25;
            String categoryName = this.font.plainSubstrByWidth(category.name, 80);
            this.addRenderableWidget(new PanelButton(left, y, 112, 21, (Component)Component.literal((String)"   ").append(categoryName), category.id.equals(this.selectedCategory) ? PanelButton.Variant.SELECTED : PanelButton.Variant.SECONDARY, button -> {
                this.selectedCategory = category.id;
                this.productPage = 0;
                this.rebuildWidgets();
            }));
            this.addRenderableWidget(new PanelButton(left + 116, y, 20, 21, (Component)Component.literal((String)"\u2191"), PanelButton.Variant.SECONDARY, button -> this.move("move_category", category.id, -1)));
            this.addRenderableWidget(new PanelButton(left + 140, y, 20, 21, (Component)Component.literal((String)"\u2193"), PanelButton.Variant.SECONDARY, button -> this.move("move_category", category.id, 1)));
        }
        if (categoryPages > 1) {
            this.addPageButtons(this.panelLeft + 90, this.panelTop + 52 + 329, this.categoryPage, categoryPages, true);
        }
        if ((selected = this.selectedCategory()) != null) {
            int actionY = this.panelTop + 40;
            this.addRenderableWidget(new PanelButton(this.panelLeft + this.panelWidth - 276, actionY, 58, 20, (Component)Component.translatable((String)"gui.avecoins.edit"), PanelButton.Variant.HEADER, button -> this.minecraft.gui.setScreen((Screen)new CategoryEditorScreen(this, this.snapshot, selected))));
            this.addRenderableWidget(new PanelButton(this.panelLeft + this.panelWidth - 212, actionY, 62, 20, (Component)Component.translatable((String)"gui.avecoins.delete"), PanelButton.Variant.DANGER, button -> this.confirmDeleteCategory(selected)));
            this.addRenderableWidget(new PanelButton(this.panelLeft + this.panelWidth - 142, actionY, 124, 20, (Component)Component.translatable((String)"gui.avecoins.shop_admin.new_product"), PanelButton.Variant.SELECTED, button -> this.minecraft.gui.setScreen((Screen)new ProductEditorScreen(this, this.snapshot, selected.id, null))));
            List<ShopData.Product> allProducts = this.products(selected.id);
            int productPages = Math.max(1, (allProducts.size() + 8 - 1) / 8);
            this.productPage = Math.min(this.productPage, productPages - 1);
            int productStart = this.productPage * 8;
            int productEnd = Math.min(allProducts.size(), productStart + 8);
            for (int index = productStart; index < productEnd; ++index) {
                ShopData.Product product = allProducts.get(index);
                int rowY = this.panelTop + 105 + (index - productStart) * 34;
                int controlsX = this.panelLeft + this.panelWidth - 136;
                this.addRenderableWidget(new PanelButton(controlsX, rowY, 20, 20, (Component)Component.literal((String)"\u2191"), PanelButton.Variant.SECONDARY, button -> this.move("move_product", product.id, -1)));
                this.addRenderableWidget(new PanelButton(controlsX + 24, rowY, 20, 20, (Component)Component.literal((String)"\u2193"), PanelButton.Variant.SECONDARY, button -> this.move("move_product", product.id, 1)));
                this.addRenderableWidget(new PanelButton(controlsX + 48, rowY, 42, 20, (Component)Component.translatable((String)"gui.avecoins.edit"), PanelButton.Variant.SECONDARY, button -> this.minecraft.gui.setScreen((Screen)new ProductEditorScreen(this, this.snapshot, selected.id, product))));
                this.addRenderableWidget(new PanelButton(controlsX + 94, rowY, 20, 20, (Component)Component.literal((String)"\u00d7"), PanelButton.Variant.DANGER, button -> this.confirmDeleteProduct(product)));
            }
            if (productPages > 1) {
                int contentCenter = this.panelLeft + 180 + (this.panelWidth - 180) / 2;
                this.addPageButtons(contentCenter, this.panelTop + 386, this.productPage, productPages, false);
            }
        }
    }

    private void addPageButtons(int centerX, int y, int currentPage, int pages, boolean categories) {
        PanelButton previous = new PanelButton(centerX - 70, y, 28, 18, (Component)Component.literal((String)"\u2039"), PanelButton.Variant.SECONDARY, button -> {
            if (categories) {
                this.categoryPage = Math.max(0, this.categoryPage - 1);
            } else {
                this.productPage = Math.max(0, this.productPage - 1);
            }
            this.rebuildWidgets();
        });
        previous.active = currentPage > 0;
        this.addRenderableWidget(previous);
        PanelButton next = new PanelButton(centerX + 42, y, 28, 18, (Component)Component.literal((String)"\u203a"), PanelButton.Variant.SECONDARY, button -> {
            if (categories) {
                this.categoryPage = Math.min(pages - 1, this.categoryPage + 1);
            } else {
                this.productPage = Math.min(pages - 1, this.productPage + 1);
            }
            this.rebuildWidgets();
        });
        next.active = currentPage + 1 < pages;
        this.addRenderableWidget(next);
    }

    private void move(String action, String id, int direction) {
        JsonObject request = new JsonObject();
        request.addProperty("id", id);
        request.addProperty("direction", (Number)direction);
        ClientNetworking.send(new ServerboundAvecoinsPayload(action, request.toString()));
    }

    private void confirmDeleteCategory(ShopData.Category category) {
        this.minecraft.gui.setScreen((Screen)new ConfirmActionScreen(this, (Component)Component.translatable((String)"gui.avecoins.confirm.delete_category", (Object[])new Object[]{category.name}), () -> {
            JsonObject request = new JsonObject();
            request.addProperty("id", category.id);
            ClientNetworking.send(new ServerboundAvecoinsPayload("delete_category", request.toString()));
        }));
    }

    private void confirmDeleteProduct(ShopData.Product product) {
        this.minecraft.gui.setScreen((Screen)new ConfirmActionScreen(this, (Component)Component.translatable((String)"gui.avecoins.confirm.delete_product", (Object[])new Object[]{product.name}), () -> {
            JsonObject request = new JsonObject();
            request.addProperty("id", product.id);
            ClientNetworking.send(new ServerboundAvecoinsPayload("delete_product", request.toString()));
        }));
    }

    @Override
    protected void renderPanel(GuiGraphicsExtractor graphics, int mouseX, int mouseY, float partialTick) {
        this.renderHeader(graphics, null);
        graphics.fill(this.panelLeft + 180, this.panelTop + 30, this.panelLeft + 180 + 1, this.panelTop + this.panelHeight - 30, this.theme().DIVIDER);
        MutableComponent categoriesTitle = Component.literal((String)"\u2630 ").append((Component)Component.translatable((String)"gui.avecoins.shop.categories")).withStyle(ChatFormatting.BOLD);
        GuiTextLayout.drawCentered(graphics, this.font, (Component)categoriesTitle, this.panelLeft + 90, this.panelTop + 52, this.theme().TEXT, !this.theme().custom);
        this.drawCenteredScaledWrappedAt(graphics, (Component)Component.translatable((String)"gui.avecoins.shop_admin.categories_help"), this.panelLeft + 90, this.panelTop + 52 + 15, 152, 0.8f, this.theme().MUTED);
        this.renderCategoryIcons(graphics);
        this.renderPageLabel(graphics, this.panelLeft + 90, this.panelTop + 52 + 334, this.categoryPage, Math.max(1, (this.snapshot.categories.size() + 10 - 1) / 10));
        ShopData.Category selected = this.selectedCategory();
        if (selected == null) {
            GuiTextLayout.drawCentered(graphics, this.font, (Component)Component.translatable((String)"gui.avecoins.shop_admin.no_categories"), this.panelLeft + 180 + (this.panelWidth - 180) / 2, this.panelTop + 190, this.theme().MUTED, !this.theme().custom);
        } else {
            int contentLeft = this.panelLeft + 180 + 14;
            this.renderItem(graphics, selected.iconItem, contentLeft, this.panelTop + 40);
            String selectedName = this.font.plainSubstrByWidth(selected.name, 270);
            graphics.text(this.font, (Component)Component.literal((String)selectedName).withStyle(ChatFormatting.BOLD), contentLeft + 22, this.panelTop + 42, this.theme().TEXT, !this.theme().custom);
            graphics.text(this.font, this.font.plainSubstrByWidth(selected.description, 270), contentLeft + 22, this.panelTop + 54, this.theme().MUTED, !this.theme().custom);
            graphics.fill(contentLeft, this.panelTop + 72, this.panelLeft + this.panelWidth - 18, this.panelTop + 73, this.theme().DIVIDER);
            graphics.text(this.font, (Component)Component.translatable((String)"gui.avecoins.shop_admin.products").withStyle(ChatFormatting.BOLD), contentLeft, this.panelTop + 84, this.theme().TEXT, !this.theme().custom);
            List<ShopData.Product> allProducts = this.products(selected.id);
            int productPages = Math.max(1, (allProducts.size() + 8 - 1) / 8);
            int productStart = this.productPage * 8;
            int productEnd = Math.min(allProducts.size(), productStart + 8);
            for (int index = productStart; index < productEnd; ++index) {
                ShopData.Product product = allProducts.get(index);
                int rowY = this.panelTop + 105 + (index - productStart) * 34;
                graphics.fill(contentLeft, rowY - 4, this.panelLeft + this.panelWidth - 142, rowY + 24, (index & 1) == 0 ? this.theme().SURFACE : this.theme().ALTERNATE);
                ItemStack displayStack = ProductDisplayStack.create(product);
                if (!displayStack.isEmpty()) {
                    graphics.item(displayStack, contentLeft + 6, rowY + 1);
                }
                String productName = this.font.plainSubstrByWidth(product.name, 168);
                graphics.text(this.font, (Component)Component.literal((String)productName).withStyle(ChatFormatting.BOLD), contentLeft + 30, rowY, product.enabled ? this.theme().TEXT : this.theme().DISABLED, !this.theme().custom);
                graphics.text(this.font, (Component)Component.translatable((String)(product.enabled ? "gui.avecoins.enabled" : "gui.avecoins.disabled")), contentLeft + 30, rowY + 11, product.enabled ? this.theme().SUCCESS : this.theme().DISABLED, !this.theme().custom);
                this.renderCurrency(graphics, product, contentLeft + 220, rowY + 1);
                MutableComponent stock = product.stock < 0 ? Component.translatable((String)"gui.avecoins.shop.stock_unlimited") : Component.translatable((String)"gui.avecoins.shop.stock", (Object[])new Object[]{product.stock});
                graphics.text(this.font, (Component)stock, contentLeft + 296, rowY + 5, this.theme().MUTED, !this.theme().custom);
            }
            if (allProducts.isEmpty()) {
                GuiTextLayout.drawCentered(graphics, this.font, (Component)Component.translatable((String)"gui.avecoins.shop_admin.no_products"), this.panelLeft + 180 + (this.panelWidth - 180) / 2, this.panelTop + 180, this.theme().MUTED, !this.theme().custom);
            }
            this.renderPageLabel(graphics, this.panelLeft + 180 + (this.panelWidth - 180) / 2, this.panelTop + 391, this.productPage, productPages);
        }
        if (!this.result.isEmpty()) {
            this.renderStatusBox(graphics, this.result);
        }
    }

    private void renderPageLabel(GuiGraphicsExtractor graphics, int centerX, int y, int currentPage, int pages) {
        if (pages > 1) {
            GuiTextLayout.drawCentered(graphics, this.font, (Component)Component.translatable((String)"gui.avecoins.shop.page", (Object[])new Object[]{currentPage + 1, pages}), centerX, y, this.theme().MUTED, !this.theme().custom);
        }
    }

    private void renderCategoryIcons(GuiGraphicsExtractor graphics) {
        int start = this.categoryPage * 10;
        int end = Math.min(this.snapshot.categories.size(), start + 10);
        for (int index = start; index < end; ++index) {
            this.renderItem(graphics, this.snapshot.categories.get((int)index).iconItem, this.panelLeft + 19, this.panelTop + 52 + 71 + (index - start) * 25);
        }
    }

    private void renderCurrency(GuiGraphicsExtractor graphics, ShopData.Product product, int x, int y) {
        this.renderItem(graphics, product.currencyId, x, y);
        graphics.text(this.font, Integer.toString(product.effectivePrice()), x + 20, y + 5, product.discountPrice == null ? this.theme().TEXT : this.theme().DISCOUNT, !this.theme().custom);
    }

    private void renderItem(GuiGraphicsExtractor graphics, String itemId, int x, int y) {
        Identifier id = Identifier.tryParse((String)itemId);
        if (id != null && BuiltInRegistries.ITEM.containsKey(id)) {
            graphics.item(new ItemStack((ItemLike)BuiltInRegistries.ITEM.getValue(id)), x, y);
        }
    }

    private List<ShopData.Product> products(String categoryId) {
        return this.snapshot.products.stream().filter(product -> product.categoryId.equals(categoryId)).toList();
    }

    private ShopData.Category selectedCategory() {
        return this.snapshot.categories.stream().filter(category -> category.id.equals(this.selectedCategory)).findFirst().orElse(null);
    }
}

