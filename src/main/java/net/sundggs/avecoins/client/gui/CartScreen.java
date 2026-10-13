/*
 * Decompiled with CFR 0.152.
 */
package net.sundggs.avecoins.client.gui;

import com.google.gson.JsonObject;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import net.minecraft.ChatFormatting;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.components.PlayerFaceExtractor;
import net.minecraft.client.gui.components.Tooltip;
import net.minecraft.client.gui.components.events.GuiEventListener;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.resources.Identifier;
import net.minecraft.world.entity.player.PlayerSkin;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.ItemLike;
import net.sundggs.avecoins.client.gui.AvecoinsScreen;
import net.sundggs.avecoins.client.gui.CartQuantityChange;
import net.sundggs.avecoins.client.gui.GuiIconRenderer;
import net.sundggs.avecoins.client.gui.GuiTextLayout;
import net.sundggs.avecoins.client.gui.PanelButton;
import net.sundggs.avecoins.client.gui.PaymentReceipt;
import net.sundggs.avecoins.client.gui.ProductDisplayStack;
import net.sundggs.avecoins.client.gui.PurchaseResultScreen;
import net.sundggs.avecoins.client.gui.ResultFeedback;
import net.sundggs.avecoins.client.gui.ShiftKeyState;
import net.sundggs.avecoins.client.gui.ShopScreen;
import net.sundggs.avecoins.network.ClientNetworking;
import net.sundggs.avecoins.network.ServerboundAvecoinsPayload;
import net.sundggs.avecoins.shop.ShopData;
import net.sundggs.avecoins.shop.ShopService;

public final class CartScreen
extends AvecoinsScreen {
    private static final int ITEMS_PER_PAGE = 5;
    private static final int PANEL_WIDTH = 520;
    private int summaryY;
    private int confirmY;
    private int pagerY;
    private static final int ROW_TOP = 83;
    private static final int ROW_STEP = 34;
    private static final Identifier PRICE_ICON = Identifier.fromNamespaceAndPath((String)"avecoins", (String)"textures/gui/icons/price_tag.png");
    private final ShopService.ShopSnapshot snapshot;
    private int page;
    private String resultCode;
    private boolean resultSuccess;
    private final String shopCategory;
    private final String shopSearch;
    private final int shopPage;
    private final int shopCategoryPage;

    public CartScreen(ShopService.ShopSnapshot snapshot) {
        this(snapshot, 0);
    }

    public CartScreen(ShopService.ShopSnapshot snapshot, int page) {
        this(snapshot, page, "");
    }

    public CartScreen(ShopService.ShopSnapshot snapshot, int page, String resultCode) {
        this(snapshot, page, resultCode, "all", "", 0);
    }

    public CartScreen(ShopService.ShopSnapshot snapshot, int page, String resultCode, String shopCategory, String shopSearch, int shopPage) {
        this(snapshot, page, resultCode, shopCategory, shopSearch, shopPage, 0);
    }

    public CartScreen(ShopService.ShopSnapshot snapshot, int page, String resultCode, String shopCategory, String shopSearch, int shopPage, int shopCategoryPage) {
        super((Component)Component.translatable((String)"gui.avecoins.cart.title"));
        this.snapshot = snapshot;
        this.useShopTheme(snapshot);
        this.page = page;
        this.resultCode = resultCode == null ? "" : resultCode;
        this.resultSuccess = ResultFeedback.isSuccess(this.resultCode);
        this.shopCategory = shopCategory;
        this.shopSearch = shopSearch;
        this.shopPage = shopPage;
        this.shopCategoryPage = shopCategoryPage;
    }

    public int page() {
        return this.page;
    }

    public CartScreen refreshed(ShopService.ShopSnapshot snapshot, String resultCode) {
        return new CartScreen(snapshot, this.page, resultCode, this.shopCategory, this.shopSearch, this.shopPage, this.shopCategoryPage);
    }

    public PurchaseResultScreen purchaseResult(boolean success, String code, ShopService.ShopSnapshot snapshot) {
        return new PurchaseResultScreen(success, code, snapshot, new ShopScreen(snapshot, this.shopCategory, this.shopSearch, this.shopPage, this.shopCategoryPage), this.refreshed(snapshot, ""));
    }

    public void showResult(String resultCode, boolean success) {
        this.resultCode = resultCode == null ? "" : resultCode;
        this.resultSuccess = success;
        this.rebuildWidgets();
    }

    protected void init() {
        int visibleRows = Math.min(5, this.cartLines().size());
        int productsBottom = 83 + Math.max(0, visibleRows - 1) * 34 + 25;
        this.pagerY = productsBottom + 12;
        this.summaryY = visibleRows == 0 ? 126 : (this.cartLines().size() > 5 ? this.pagerY + 18 + 16 : productsBottom + 16);
        this.confirmY = this.summaryY + (this.hasChange() ? 88 : 44);
        this.layoutPanel(520, (visibleRows == 0 ? 166 : this.confirmY + 64) + (this.hasFeedback() ? 24 : 0));
        this.addHeaderBackButton(new ShopScreen(this.snapshot, this.shopCategory, this.shopSearch, this.shopPage, this.shopCategoryPage));
        PanelButton wallet = new PanelButton(this.panelLeft + this.panelWidth - 38, this.panelTop + this.panelHeight - 27, 26, 22, (Component)Component.empty(), PanelButton.Variant.HEADER, button -> ClientNetworking.send(new ServerboundAvecoinsPayload("request_wallet", "{}"))).withIcon(Identifier.fromNamespaceAndPath((String)"avecoins", (String)"textures/gui/icons/coin_wallet.png"), 16);
        wallet.setTooltip(Tooltip.create((Component)Component.translatable((String)"gui.avecoins.wallet.open")));
        this.addRenderableWidget(wallet);
        List<Map.Entry<String, Integer>> lines = this.cartLines();
        int pages = Math.max(1, (lines.size() + 5 - 1) / 5);
        this.page = Math.min(this.page, pages - 1);
        int start = this.page * 5;
        int end = Math.min(lines.size(), start + 5);
        for (int index = start; index < end; ++index) {
            Map.Entry<String, Integer> entry = lines.get(index);
            ShopData.Product product = this.product(entry.getKey());
            if (product == null) continue;
            int rowY = this.panelTop + 83 + (index - start) * 34;
            int quantity = entry.getValue();
            int controlsX = this.panelLeft + this.panelWidth - 134;
            this.addRenderableWidget(new PanelButton(controlsX, rowY, 22, 20, (Component)Component.literal((String)"\u2212"), PanelButton.Variant.SECONDARY, button -> this.changeQuantity(product, quantity, -1)));
            this.addRenderableWidget(new PanelButton(controlsX + 26, rowY, 30, 20, (Component)Component.literal((String)Integer.toString(quantity)), PanelButton.Variant.HEADER, button -> {}));
            this.addRenderableWidget(new PanelButton(controlsX + 60, rowY, 22, 20, (Component)Component.literal((String)"+"), PanelButton.Variant.SECONDARY, button -> this.changeQuantity(product, quantity, 1)));
            this.addRenderableWidget(new PanelButton(controlsX + 86, rowY, 24, 20, (Component)Component.literal((String)"\u00d7"), PanelButton.Variant.DANGER, button -> this.setQuantity(product.id, 0)));
        }
        if (pages > 1) {
            PanelButton previous = new PanelButton(this.panelLeft + this.panelWidth / 2 - 82, this.panelTop + this.pagerY, 34, 18, (Component)Component.literal((String)"\u2039"), PanelButton.Variant.SECONDARY, button -> {
                this.page = Math.max(0, this.page - 1);
                this.rebuildWidgets();
            });
            previous.active = this.page > 0;
            this.addRenderableWidget(previous);
            PanelButton next = new PanelButton(this.panelLeft + this.panelWidth / 2 + 48, this.panelTop + this.pagerY, 34, 18, (Component)Component.literal((String)"\u203a"), PanelButton.Variant.SECONDARY, button -> {
                this.page = Math.min(pages - 1, this.page + 1);
                this.rebuildWidgets();
            });
            next.active = this.page + 1 < pages;
            this.addRenderableWidget(next);
        }
        if (lines.isEmpty()) {
            return;
        }
        PanelButton confirm = new PanelButton(this.panelLeft + this.panelWidth / 2 - 82, this.panelTop + this.confirmY, 164, 22, (Component)Component.translatable((String)"gui.avecoins.cart.confirm"), PanelButton.Variant.SELECTED, button -> ClientNetworking.send(new ServerboundAvecoinsPayload("checkout", "{}")));
        confirm.active = !this.snapshot.cart.isEmpty() && (this.snapshot.paymentQuote == null || this.snapshot.paymentQuote.payable);
        this.addRenderableWidget(confirm);
    }

    @Override
    protected void renderPanel(GuiGraphicsExtractor graphics, int mouseX, int mouseY, float partialTick) {
        this.renderHeader(graphics, (Component)Component.translatable((String)"gui.avecoins.cart.subtitle"));
        this.divider(graphics, this.panelTop + 65, 24);
        List<Map.Entry<String, Integer>> lines = this.cartLines();
        int pages = Math.max(1, (lines.size() + 5 - 1) / 5);
        int start = this.page * 5;
        int end = Math.min(lines.size(), start + 5);
        LinkedHashMap<String, Long> totals = new LinkedHashMap<String, Long>();
        for (Map.Entry<String, Integer> entry : lines) {
            ShopData.Product product = this.product(entry.getKey());
            if (product == null) continue;
            totals.merge(product.currencyId, (long)product.effectivePrice() * (long)entry.getValue().intValue(), Long::sum);
        }
        ItemStack hoveredStack = ItemStack.EMPTY;
        for (int index = start; index < end; ++index) {
            Map.Entry<String, Integer> entry = lines.get(index);
            ShopData.Product product = this.product(entry.getKey());
            if (product == null) continue;
            int rowY = this.panelTop + 83 + (index - start) * 34;
            int controlsX = this.panelLeft + this.panelWidth - 134;
            graphics.fill(this.panelLeft + 24, rowY - 5, controlsX - 6, rowY + 25, (index & 1) == 0 ? this.theme().SURFACE : this.theme().ALTERNATE);
            ItemStack displayStack = ProductDisplayStack.create(product);
            if (!displayStack.isEmpty()) {
                graphics.item(displayStack, this.panelLeft + 30, rowY + 2);
                if (mouseX >= this.panelLeft + 30 && mouseX < this.panelLeft + 46 && mouseY >= rowY + 2 && mouseY < rowY + 18) {
                    hoveredStack = displayStack;
                }
            }
            String productName = this.font.plainSubstrByWidth(product.name, 188);
            graphics.text(this.font, (Component)Component.literal((String)productName).withStyle(ChatFormatting.BOLD), this.panelLeft + 54, rowY, this.theme().TEXT, !this.theme().custom);
            MutableComponent quantity = product.itemCount > 1 ? Component.translatable((String)"gui.avecoins.cart.quantity_delivery", (Object[])new Object[]{entry.getValue(), product.itemCount}) : Component.translatable((String)"gui.avecoins.cart.quantity", (Object[])new Object[]{entry.getValue()});
            graphics.text(this.font, (Component)quantity, this.panelLeft + 54, rowY + 12, this.theme().MUTED, !this.theme().custom);
            this.renderCurrencyAmount(graphics, product.currencyId, (long)product.effectivePrice() * (long)entry.getValue().intValue(), this.panelLeft + 280, rowY + 2);
        }
        if (lines.isEmpty()) {
            GuiTextLayout.drawCentered(graphics, this.font, (Component)Component.translatable((String)"gui.avecoins.cart.empty"), this.panelLeft + this.panelWidth / 2, this.panelTop + 88, this.theme().TEXT, !this.theme().custom);
            this.drawCenteredScaledWrappedAt(graphics, (Component)Component.translatable((String)"gui.avecoins.cart.empty_hint"), this.panelLeft + this.panelWidth / 2, this.panelTop + 108, this.panelWidth - 80, 0.85f, this.theme().MUTED);
        }
        if (pages > 1) {
            GuiTextLayout.drawCentered(graphics, this.font, (Component)Component.translatable((String)"gui.avecoins.shop.page", (Object[])new Object[]{this.page + 1, pages}), this.panelLeft + this.panelWidth / 2, this.panelTop + this.pagerY + 5, this.theme().MUTED, !this.theme().custom);
        }
        if (!lines.isEmpty()) {
            this.divider(graphics, this.panelTop + this.summaryY, 24);
            GuiIconRenderer.blit(graphics, PRICE_ICON, this.panelLeft + 24, this.panelTop + this.summaryY + 17, 14, 14, 32, 32);
            PaymentReceipt.row(graphics, this.font, "gui.avecoins.cart.total", totals, this.panelLeft + 44, this.panelTop + this.summaryY + 16, PaymentReceipt.labelWidth(this.font), this.panelWidth - 68);
            this.renderPaymentQuote(graphics);
        }
        if (!this.resultCode.isBlank()) {
            this.renderFooterBox(graphics, (Component)Component.translatable((String)("message.avecoins." + this.resultCode)), this.resultSuccess ? this.theme().SUCCESS_BOX : this.theme().DANGER, this.theme().TEXT);
        }
        this.renderWalletFooter(graphics);
        if (!hoveredStack.isEmpty()) {
            graphics.setTooltipForNextFrame(this.font, hoveredStack, mouseX, mouseY);
        }
    }

    private void renderPaymentQuote(GuiGraphicsExtractor graphics) {
        ShopService.PaymentQuote quote = this.snapshot.paymentQuote;
        if (quote == null || this.snapshot.cart.isEmpty()) {
            return;
        }
        if (!quote.payable) {
            if (this.resultCode.isBlank()) {
                this.renderFooterBox(graphics, (Component)Component.translatable((String)("message.avecoins." + quote.reason)), this.theme().DANGER, this.theme().TEXT);
            }
            return;
        }
        if (this.hasChange()) {
            GuiIconRenderer.blit(graphics, Identifier.fromNamespaceAndPath((String)"avecoins", (String)"textures/gui/icons/change.png"), this.panelLeft + 24, this.panelTop + this.summaryY + 43, 14, 14, 32, 32);
            PaymentReceipt.row(graphics, this.font, "gui.avecoins.receipt.change", PaymentReceipt.combine(quote.inventoryChange, quote.walletChange), this.panelLeft + 44, this.panelTop + this.summaryY + 42, PaymentReceipt.labelWidth(this.font), this.panelWidth - 68);
            this.drawScaledWrapped(graphics, PaymentReceipt.info("gui.avecoins.receipt.change_help"), this.panelLeft + 44, this.panelTop + this.summaryY + 64, this.panelWidth - 68, 0.75f, this.theme().MUTED);
        }
    }

    private boolean hasFeedback() {
        return !this.resultCode.isBlank() || !this.snapshot.cart.isEmpty() && this.snapshot.paymentQuote != null && !this.snapshot.paymentQuote.payable;
    }

    @Override
    protected int panelFooterHeight() {
        return 32;
    }

    @Override
    protected void renderFooterBox(GuiGraphicsExtractor graphics, Component message, int background, int color) {
        int y = this.panelTop + this.panelHeight - 56;
        graphics.fill(this.panelLeft + 24, y, this.panelLeft + this.panelWidth - 24, y + 18, background);
        this.drawCenteredScaledWrappedAt(graphics, message, this.panelLeft + this.panelWidth / 2, y + 5, this.panelWidth - 60, 0.8f, color);
    }

    private void renderWalletFooter(GuiGraphicsExtractor graphics) {
        int top = this.panelTop + this.panelHeight - 32;
        if (this.minecraft.player != null) {
            PlayerFaceExtractor.extractRenderState((GuiGraphicsExtractor)graphics, (PlayerSkin)this.minecraft.player.getSkin(), (int)(this.panelLeft + 12), (int)(top + 6), (int)20);
            graphics.text(this.font, this.font.plainSubstrByWidth(this.minecraft.player.getName().getString(), 90), this.panelLeft + 40, top + 12, this.theme().TEXT, !this.theme().custom);
        }
        List<String> order = List.of("avecoins:coppercoin", "avecoins:ironcoin", "avecoins:goldcoin", "avecoins:diamondcoin", "avecoins:netheritecoin", "avecoins:goldticket", "avecoins:diamondticket", "avecoins:netheriteticket");
        int required = order.stream().filter(id -> this.snapshot.balances.getOrDefault(id, 0L) > 0L).mapToInt(id -> 30 + this.font.width(Long.toString(this.snapshot.balances.get(id)))).sum();
        float scale = Math.min(1.0f, ((float)this.panelWidth - 190.0f) / (float)Math.max(1, required));
        graphics.pose().pushMatrix();
        graphics.pose().translate((float)(this.panelLeft + this.panelWidth - 46) - (float)required * scale, (float)top + (32.0f - 16.0f * scale) / 2.0f);
        graphics.pose().scale(scale, scale);
        int x = 0;
        for (String id2 : order) {
            long balance = this.snapshot.balances.getOrDefault(id2, 0L);
            if (balance <= 0L) continue;
            x += this.renderCurrencyAmount(graphics, id2, balance, x, 0) + 10;
        }
        graphics.pose().popMatrix();
    }

    private boolean hasChange() {
        ShopService.PaymentQuote quote = this.snapshot.paymentQuote;
        return quote != null && quote.payable && !PaymentReceipt.combine(quote.inventoryChange, quote.walletChange).isEmpty();
    }

    private int renderCurrencyAmount(GuiGraphicsExtractor graphics, String currencyId, long amount, int x, int y) {
        Identifier id = Identifier.tryParse((String)currencyId);
        if (id != null && BuiltInRegistries.ITEM.containsKey(id)) {
            graphics.item(new ItemStack((ItemLike)BuiltInRegistries.ITEM.getValue(id)), x, y);
        }
        String value = Long.toString(amount);
        graphics.text(this.font, value, x + 20, y + 5, this.theme().TEXT, !this.theme().custom);
        return 20 + this.font.width(value);
    }

    private List<Map.Entry<String, Integer>> cartLines() {
        ArrayList<Map.Entry<String, Integer>> result = new ArrayList<Map.Entry<String, Integer>>();
        for (Map.Entry<String, Integer> entry : this.snapshot.cart.entrySet()) {
            if (this.product(entry.getKey()) == null) continue;
            result.add(entry);
        }
        return result;
    }

    private ShopData.Product product(String id) {
        return this.snapshot.products.stream().filter(product -> product.id.equals(id)).findFirst().orElse(null);
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
}

