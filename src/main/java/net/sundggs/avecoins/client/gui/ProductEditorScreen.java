/*
 * Decompiled with CFR 0.152.
 */
package net.sundggs.avecoins.client.gui;

import java.util.Arrays;
import java.util.List;
import net.minecraft.ChatFormatting;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.gui.components.Tooltip;
import net.minecraft.client.gui.components.events.GuiEventListener;
import net.minecraft.client.input.KeyEvent;
import net.minecraft.client.input.MouseButtonEvent;
import net.minecraft.core.HolderLookup;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.sundggs.avecoins.client.gui.AdminShopScreen;
import net.sundggs.avecoins.client.gui.AutocompleteField;
import net.sundggs.avecoins.client.gui.AvecoinsScreen;
import net.sundggs.avecoins.client.gui.PanelButton;
import net.sundggs.avecoins.client.gui.ProductCardButton;
import net.sundggs.avecoins.config.AtomicJsonStore;
import net.sundggs.avecoins.config.CraftingConfig;
import net.sundggs.avecoins.network.ClientNetworking;
import net.sundggs.avecoins.network.ServerboundAvecoinsPayload;
import net.sundggs.avecoins.shop.ShopData;
import net.sundggs.avecoins.shop.ShopItemStacks;
import net.sundggs.avecoins.shop.ShopService;

public final class ProductEditorScreen
extends AvecoinsScreen {
    private final AdminShopScreen parent;
    private final ShopService.ShopSnapshot snapshot;
    private final ShopData.Product existing;
    private EditBox idBox;
    private EditBox nameBox;
    private EditBox itemBox;
    private EditBox itemCountBox;
    private EditBox currencyBox;
    private EditBox priceBox;
    private EditBox stockBox;
    private EditBox sellingPriceBox;
    private AutocompleteField itemAutocomplete;
    private AutocompleteField currencyAutocomplete;
    private AutocompleteField categoryAutocomplete;
    private PanelButton discountButton;
    private PanelButton statusButton;
    private PanelButton sellButton;
    private boolean enabled;
    private boolean sellEnabled;
    private String capturedItemId;
    private String capturedItemData;
    private String selectedCategoryId;
    private String validationKey = "";
    private int discountPercent;
    private static final int[] DISCOUNT_OPTIONS = new int[]{5, 10, 20, 30, 40, 50, 60, 70, 80, 90};

    public ProductEditorScreen(AdminShopScreen parent, ShopService.ShopSnapshot snapshot, String categoryId, ShopData.Product existing) {
        super((Component)Component.translatable((String)(existing == null ? "gui.avecoins.product.new_title" : "gui.avecoins.product.edit_title")));
        this.parent = parent;
        this.snapshot = snapshot;
        this.useShopTheme(snapshot);
        this.existing = existing;
        this.enabled = existing == null || existing.enabled;
        this.sellEnabled = existing != null && existing.sellEnabled;
        this.capturedItemId = existing == null ? "" : existing.itemId;
        this.capturedItemData = existing == null ? "" : existing.itemData;
        this.selectedCategoryId = existing == null ? categoryId : existing.categoryId;
        this.discountPercent = ProductEditorScreen.nearestDiscountPercent(existing);
    }

    public AdminShopScreen refreshedParent(ShopService.ShopSnapshot snapshot, String result) {
        return new AdminShopScreen(snapshot, result, this.parent.selectedCategoryId(), this.parent.categoryPage(), this.parent.productPage());
    }

    protected void init() {
        this.layoutPanel(700, 400);
        this.addHeaderBackButton(this.parent);
        int left = this.panelLeft + 40;
        int right = this.panelLeft + 365;
        int paymentLeft = this.panelLeft + 40;
        int priceLeft = this.panelLeft + 358;
        int discountLeft = this.panelLeft + 418;
        int sellingLeft = this.panelLeft + 520;
        int allowSellingLeft = this.panelLeft + 580;
        this.idBox = this.field(left, this.panelTop + 92, 296, 48, this.existing == null ? "" : this.existing.id);
        if (this.existing != null) {
            this.idBox.setEditable(false);
        }
        this.nameBox = this.field(right, this.panelTop + 92, 295, 64, this.existing == null ? "" : this.existing.name);
        this.itemAutocomplete = new AutocompleteField(this.font, left, this.panelTop + 142, 200, 128, this.existing == null ? "" : this.existing.itemId, ProductEditorScreen.itemIds(), AutocompleteField::registeredItem, value -> {
            if (!value.equals(this.capturedItemId)) {
                this.capturedItemData = "";
            }
        });
        this.itemBox = this.itemAutocomplete.box();
        this.addRenderableWidget((GuiEventListener)this.itemBox);
        PanelButton useProductInHand = new PanelButton(left + 206, this.panelTop + 142, 90, 20, (Component)Component.translatable((String)"gui.avecoins.use_held_item_short"), PanelButton.Variant.HEADER, button -> this.setProductFromHand());
        useProductInHand.setTooltip(Tooltip.create((Component)Component.translatable((String)"gui.avecoins.product.item_data_help")));
        this.addRenderableWidget((GuiEventListener)useProductInHand);
        this.categoryAutocomplete = new AutocompleteField(this.font, right, this.panelTop + 142, 145, 64, this.selectedCategoryName(), this.categoryNames(), value -> ItemStack.EMPTY, value -> {
            this.selectedCategoryId = this.categoryIdForName((String)value);
        });
        this.addRenderableWidget((GuiEventListener)this.categoryAutocomplete.box());
        this.itemCountBox = this.field(right + 155, this.panelTop + 142, 62, 2, this.existing == null ? "" : Integer.toString(this.existing.itemCount));
        this.stockBox = this.field(right + 225, this.panelTop + 142, 62, 11, this.existing == null ? "" : Integer.toString(this.existing.stock));
        this.currencyAutocomplete = new AutocompleteField(this.font, paymentLeft, this.panelTop + 218, 200, 128, this.existing == null ? CraftingConfig.CURRENCY : this.existing.currencyId, CraftingConfig.COINS, AutocompleteField::registeredItem, value -> {
            if (CraftingConfig.COINS.contains(value)) {
                this.validationKey = "";
            }
        });
        this.currencyBox = this.currencyAutocomplete.box();
        this.addRenderableWidget((GuiEventListener)this.currencyBox);
        this.addRenderableWidget((GuiEventListener)new PanelButton(paymentLeft + 206, this.panelTop + 218, 90, 20, (Component)Component.translatable((String)"gui.avecoins.use_held_item_short"), PanelButton.Variant.HEADER, button -> this.setCurrencyFromHand()));
        this.priceBox = this.field(priceLeft, this.panelTop + 218, 52, 10, this.existing == null ? "" : Integer.toString(this.existing.price));
        this.discountButton = new PanelButton(discountLeft, this.panelTop + 218, 82, 20, (Component)Component.literal((String)this.discountLabel()), this.discountPercent == 0 ? PanelButton.Variant.SECONDARY : PanelButton.Variant.SELECTED, this.discountPercent == 0 ? -6183253 : -6307928, button -> this.cycleDiscount());
        this.addRenderableWidget((GuiEventListener)this.discountButton);
        this.sellingPriceBox = this.field(sellingLeft, this.panelTop + 218, 52, 10, !this.sellEnabled || this.existing == null || this.existing.sellingPrice == null ? "" : Integer.toString(this.existing.sellingPrice));
        this.sellingPriceBox.setEditable(this.sellEnabled);
        this.sellingPriceBox.active = this.sellEnabled;
        this.sellButton = new PanelButton(allowSellingLeft, this.panelTop + 218, 82, 20, (Component)Component.translatable((String)(this.sellEnabled ? "gui.avecoins.enabled" : "gui.avecoins.disabled")), this.sellEnabled ? PanelButton.Variant.SELECTED : PanelButton.Variant.SECONDARY, this.sellEnabled ? -6307928 : -6183253, button -> this.toggleSelling());
        this.addRenderableWidget((GuiEventListener)this.sellButton);
        this.statusButton = new PanelButton(this.panelLeft + 120, this.panelTop + 305, 150, 20, (Component)Component.translatable((String)(this.enabled ? "gui.avecoins.enabled" : "gui.avecoins.disabled")), PanelButton.Variant.SELECTED, this.enabled ? -6307928 : -3044212, button -> this.toggleEnabled());
        this.addRenderableWidget((GuiEventListener)this.statusButton);
        this.addRenderableWidget((GuiEventListener)new PanelButton(this.panelLeft + 432, this.panelTop + 305, 148, 22, (Component)Component.translatable((String)"gui.avecoins.save"), PanelButton.Variant.SELECTED, button -> this.save()));
    }

    private EditBox field(int x, int y, int width, int max, String value) {
        EditBox box = new EditBox(this.font, x, y, width, 20, (Component)Component.empty());
        box.setMaxLength(max);
        box.setValue(value);
        this.addRenderableWidget((GuiEventListener)box);
        return box;
    }

    private void toggleEnabled() {
        this.enabled = !this.enabled;
        this.statusButton.setMessage((Component)Component.translatable((String)(this.enabled ? "gui.avecoins.enabled" : "gui.avecoins.disabled")));
        this.statusButton.setTextColor(this.enabled ? -6307928 : -3044212);
    }

    private void toggleSelling() {
        boolean bl = this.sellEnabled = !this.sellEnabled;
        if (!this.sellEnabled) {
            this.sellingPriceBox.setValue("");
        }
        this.sellingPriceBox.setEditable(this.sellEnabled);
        this.sellingPriceBox.active = this.sellEnabled;
        this.sellButton.setMessage((Component)Component.translatable((String)(this.sellEnabled ? "gui.avecoins.enabled" : "gui.avecoins.disabled")));
        this.sellButton.setTextColor(this.sellEnabled ? -6307928 : -6183253);
        this.sellButton.setVariant(this.sellEnabled ? PanelButton.Variant.SELECTED : PanelButton.Variant.SECONDARY);
    }

    private void setCurrencyFromHand() {
        if (this.minecraft.player == null || this.minecraft.player.getMainHandItem().isEmpty()) {
            return;
        }
        String currencyId = BuiltInRegistries.ITEM.getKey((Object)this.minecraft.player.getMainHandItem().getItem()).toString();
        if (!CraftingConfig.COINS.contains(currencyId)) {
            this.validationKey = "gui.avecoins.editor.currency_hand_required";
            return;
        }
        this.validationKey = "";
        this.currencyAutocomplete.setValue(currencyId);
    }

    private void setProductFromHand() {
        if (this.minecraft.level == null || this.minecraft.player == null || this.minecraft.player.getMainHandItem().isEmpty()) {
            return;
        }
        this.capturedItemId = BuiltInRegistries.ITEM.getKey((Object)this.minecraft.player.getMainHandItem().getItem()).toString();
        this.capturedItemData = ShopItemStacks.encode((HolderLookup.Provider)this.minecraft.level.registryAccess(), this.minecraft.player.getMainHandItem());
        this.itemAutocomplete.setValue(this.capturedItemId);
    }

    private static List<String> itemIds() {
        return BuiltInRegistries.ITEM.keySet().stream().filter(id -> BuiltInRegistries.ITEM.getValue(id) != Items.AIR).map(Identifier::toString).sorted().toList();
    }

    private List<String> categoryNames() {
        return this.snapshot.categories.stream().map(category -> category.name).filter(name -> name != null && !name.isBlank()).distinct().sorted().toList();
    }

    private String categoryIdForName(String name) {
        return this.snapshot.categories.stream().filter(category -> category.name.equals(name)).map(category -> category.id).findFirst().orElse("");
    }

    private static int nearestDiscountPercent(ShopData.Product product) {
        if (product == null || product.discountPrice == null || product.price <= 0) {
            return 0;
        }
        int percent = Math.round((float)(product.price - product.discountPrice) * 100.0f / (float)product.price);
        int best = 0;
        int distance = Integer.MAX_VALUE;
        for (int option : DISCOUNT_OPTIONS) {
            if (Math.abs(option - percent) >= distance) continue;
            best = option;
            distance = Math.abs(option - percent);
        }
        return best;
    }

    private String discountLabel() {
        return this.discountPercent == 0 ? Component.translatable((String)"gui.avecoins.disabled").getString() : this.discountPercent + "%";
    }

    private void cycleDiscount() {
        int index;
        this.discountPercent = this.discountPercent == 0 ? DISCOUNT_OPTIONS[0] : ((index = Arrays.binarySearch(DISCOUNT_OPTIONS, this.discountPercent)) < 0 || index + 1 >= DISCOUNT_OPTIONS.length ? 0 : DISCOUNT_OPTIONS[index + 1]);
        this.discountButton.setMessage((Component)Component.literal((String)this.discountLabel()));
        this.discountButton.setTextColor(this.discountPercent == 0 ? -6183253 : -6307928);
        this.discountButton.setVariant(this.discountPercent == 0 ? PanelButton.Variant.SECONDARY : PanelButton.Variant.SELECTED);
    }

    private String selectedCategoryName() {
        String name = this.snapshot.categories.stream().filter(category -> category.id.equals(this.selectedCategoryId)).map(category -> category.name).findFirst().orElseGet(() -> Component.translatable((String)"gui.avecoins.product.select_category").getString());
        return name;
    }

    @Override
    public boolean mouseClicked(MouseButtonEvent event, boolean doubled) {
        double y;
        double x = this.virtualMouseX(event.x());
        if (this.itemAutocomplete.mouseClicked(x, y = (double)this.virtualMouseY(event.y()), event.button()) || this.currencyAutocomplete.mouseClicked(x, y, event.button()) || this.categoryAutocomplete.mouseClicked(x, y, event.button())) {
            return true;
        }
        return super.mouseClicked(event, doubled);
    }

    public boolean keyPressed(KeyEvent event) {
        if (this.itemAutocomplete.keyPressed(event.key()) || this.currencyAutocomplete.keyPressed(event.key()) || this.categoryAutocomplete.keyPressed(event.key())) {
            return true;
        }
        return super.keyPressed(event);
    }

    private void save() {
        if (!this.idBox.getValue().matches("[a-z0-9_-]{1,48}")) {
            this.validationKey = "gui.avecoins.editor.invalid_id";
            return;
        }
        if (this.nameBox.getValue().isBlank()) {
            this.validationKey = "gui.avecoins.editor.invalid_name";
            return;
        }
        Identifier itemId = Identifier.tryParse((String)this.itemBox.getValue());
        if (itemId == null || !BuiltInRegistries.ITEM.containsKey(itemId)) {
            this.validationKey = "gui.avecoins.editor.invalid_item";
            return;
        }
        Identifier currencyId = Identifier.tryParse((String)this.currencyBox.getValue());
        if (currencyId == null || !BuiltInRegistries.ITEM.containsKey(currencyId) || !CraftingConfig.COINS.contains(this.currencyBox.getValue())) {
            this.validationKey = "gui.avecoins.editor.invalid_currency";
            return;
        }
        if (this.snapshot.categories.stream().noneMatch(category -> category.id.equals(this.selectedCategoryId))) {
            this.validationKey = "gui.avecoins.editor.invalid_category";
            return;
        }
        ShopData.Product product = new ShopData.Product();
        product.id = this.idBox.getValue();
        product.categoryId = this.selectedCategoryId;
        product.name = this.nameBox.getValue();
        product.itemId = this.itemBox.getValue();
        product.itemData = this.capturedItemData;
        product.currencyId = CraftingConfig.CURRENCY;
        product.enabled = this.enabled;
        product.sellEnabled = this.sellEnabled;
        product.sortOrder = this.existing == null ? 0 : this.existing.sortOrder;
        try {
            product.itemCount = Integer.parseInt(this.itemCountBox.getValue());
            product.price = Integer.parseInt(this.priceBox.getValue());
            product.discountPrice = this.discountedPrice(product.price);
            product.stock = Integer.parseInt(this.stockBox.getValue());
            product.sellingPrice = !this.sellEnabled || this.sellingPriceBox.getValue().isBlank() ? null : Integer.valueOf(Integer.parseInt(this.sellingPriceBox.getValue()));
        }
        catch (NumberFormatException exception) {
            this.validationKey = "gui.avecoins.editor.invalid_numbers";
            return;
        }
        if (product.itemCount < 1 || product.itemCount > 64 || product.price < 1 || product.price > 1000000000 || product.stock < -1 || product.stock > 1000000000 || product.sellingPrice != null && (product.sellingPrice < 1 || product.sellingPrice > product.price) || product.discountPrice != null && (product.discountPrice < 1 || product.discountPrice >= product.price)) {
            this.validationKey = "gui.avecoins.editor.invalid_numbers";
            return;
        }
        ClientNetworking.send(new ServerboundAvecoinsPayload("save_product", AtomicJsonStore.gson().toJson((Object)product)));
    }

    @Override
    protected void renderPanel(GuiGraphicsExtractor graphics, int mouseX, int mouseY, float partialTick) {
        this.renderHeader(graphics, (Component)Component.translatable((String)"gui.avecoins.product.subtitle"));
        this.divider(graphics, this.panelTop + 68, 24);
        int left = this.panelLeft + 40;
        int right = this.panelLeft + 365;
        int paymentLeft = this.panelLeft + 40;
        int priceLeft = this.panelLeft + 358;
        int discountLeft = this.panelLeft + 418;
        int sellingLeft = this.panelLeft + 520;
        int allowSellingLeft = this.panelLeft + 580;
        this.label(graphics, "#", "gui.avecoins.product.id", left, this.panelTop + 79);
        this.label(graphics, "\u270e", "gui.avecoins.product.name", right, this.panelTop + 79);
        this.label(graphics, "\u25c6", "gui.avecoins.product.item", left, this.panelTop + 129);
        this.label(graphics, "\u2630", "gui.avecoins.product.category", right, this.panelTop + 129);
        this.label(graphics, "\u00d7", "gui.avecoins.product.item_count", right + 155, this.panelTop + 129);
        this.label(graphics, "\u221e", "gui.avecoins.product.stock", right + 225, this.panelTop + 129);
        this.example(graphics, "gui.avecoins.product.id_example", left, this.panelTop + 114);
        this.example(graphics, "gui.avecoins.product.name_example", right, this.panelTop + 114);
        this.example(graphics, "gui.avecoins.product.item_example", left, this.panelTop + 164);
        this.example(graphics, "gui.avecoins.product.category_example", right, this.panelTop + 164);
        this.example(graphics, "gui.avecoins.product.count_example", right + 155, this.panelTop + 164);
        this.drawScaledText(graphics, (Component)Component.translatable((String)"gui.avecoins.product.stock_example"), right + 225, this.panelTop + 164, 0.65f, this.theme().MUTED);
        this.divider(graphics, this.panelTop + 188, 24);
        this.label(graphics, "\u25c9", "gui.avecoins.product.currency", paymentLeft, this.panelTop + 205);
        this.label(graphics, "\u00a4", "gui.avecoins.product.price", priceLeft, this.panelTop + 205);
        this.label(graphics, "%", "gui.avecoins.product.discount_percent", discountLeft, this.panelTop + 205);
        graphics.fill(this.panelLeft + 510, this.panelTop + 203, this.panelLeft + 511, this.panelTop + 249, this.theme().DIVIDER);
        this.label(graphics, "\u00a4", "gui.avecoins.product.selling_price", sellingLeft, this.panelTop + 205);
        this.label(graphics, "\u2194", "gui.avecoins.product.allow_selling", allowSellingLeft, this.panelTop + 205);
        this.example(graphics, "gui.avecoins.product.currency_example", paymentLeft, this.panelTop + 240);
        this.drawScaledText(graphics, (Component)Component.translatable((String)"gui.avecoins.product.price_example"), priceLeft, this.panelTop + 240, 0.7f, this.theme().MUTED);
        this.drawScaledText(graphics, (Component)Component.translatable((String)"gui.avecoins.product.discount_optional"), discountLeft, this.panelTop + 240, 0.7f, this.theme().MUTED);
        this.drawScaledText(graphics, (Component)Component.translatable((String)"gui.avecoins.product.selling_price_optional"), sellingLeft, this.panelTop + 240, 0.65f, this.theme().MUTED);
        this.divider(graphics, this.panelTop + 264, 24);
        this.label(graphics, "\u2713", "gui.avecoins.product.status", left, this.panelTop + 284);
        this.drawSectionTitle(graphics, (Component)Component.translatable((String)"gui.avecoins.product.preview"), this.panelTop + 278);
        ProductCardButton.renderPreview(graphics, Math.round((float)this.panelLeft + (float)this.panelWidth / 2.0f - 55.1f), this.panelTop + 294, 190, this.previewProduct(), 0.58f);
        if (!this.validationKey.isBlank()) {
            this.renderFooterBox(graphics, (Component)Component.translatable((String)this.validationKey), this.theme().DANGER, this.theme().TEXT);
        }
        this.itemAutocomplete.render(graphics, mouseX, mouseY);
        this.currencyAutocomplete.render(graphics, mouseX, mouseY);
        this.categoryAutocomplete.render(graphics, mouseX, mouseY);
    }

    private String selectedCategoryDescription() {
        return this.snapshot.categories.stream().filter(category -> category.id.equals(this.selectedCategoryId)).map(category -> category.description == null || category.description.isBlank() ? category.name : category.description).findFirst().orElse("");
    }

    private ShopData.Product previewProduct() {
        ShopData.Product preview = new ShopData.Product();
        preview.name = this.nameBox.getValue().isBlank() ? Component.translatable((String)"gui.avecoins.product.unnamed").getString() : this.nameBox.getValue();
        preview.itemId = this.itemBox.getValue();
        preview.itemData = this.itemBox.getValue().equals(this.capturedItemId) ? this.capturedItemData : "";
        preview.currencyId = CraftingConfig.CURRENCY;
        preview.itemCount = this.parsed(this.itemCountBox, 1);
        preview.price = this.parsed(this.priceBox, 1);
        preview.discountPrice = this.discountedPrice(preview.price);
        preview.stock = this.parsed(this.stockBox, -1);
        preview.enabled = this.enabled;
        preview.sellEnabled = this.sellEnabled;
        preview.sellingPrice = this.sellingPriceBox == null || this.sellingPriceBox.getValue().isBlank() ? null : Integer.valueOf(this.parsed(this.sellingPriceBox, 1));
        return preview;
    }

    private int parsed(EditBox box, int fallback) {
        try {
            return Integer.parseInt(box.getValue());
        }
        catch (NumberFormatException ignored) {
            return fallback;
        }
    }

    private Integer discountedPrice(int price) {
        if (this.discountPercent == 0 || price <= 1) {
            return null;
        }
        int value = Math.max(1, Math.round((float)(price * (100 - this.discountPercent)) / 100.0f));
        return value < price ? Integer.valueOf(value) : null;
    }

    private void example(GuiGraphicsExtractor graphics, String key, int x, int y) {
        this.drawScaledText(graphics, (Component)Component.translatable((String)key), x, y, 0.8f, this.theme().MUTED);
    }

    private void label(GuiGraphicsExtractor graphics, String icon, String key, int x, int y) {
        if (key.equals("gui.avecoins.product.status")) {
            this.drawCenteredScaledWrappedAt(graphics, (Component)Component.translatable((String)"gui.avecoins.product.status_help"), this.panelLeft + 195, this.panelTop + 290, 180, 0.8f, this.theme().MUTED);
            return;
        }
        graphics.text(this.font, (Component)Component.literal((String)(icon + " ")).append((Component)Component.translatable((String)key)).withStyle(ChatFormatting.BOLD), x, y, this.theme().TEXT, !this.theme().custom);
    }
}

