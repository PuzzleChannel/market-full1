/*
 * Decompiled with CFR 0.152.
 */
package net.sundggs.avecoins.client.gui;

import java.time.Instant;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Map;
import net.minecraft.ChatFormatting;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.components.events.GuiEventListener;
import net.minecraft.client.input.MouseButtonEvent;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.FormattedText;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.resources.Identifier;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.ItemLike;
import net.sundggs.avecoins.client.gui.AdminShopScreen;
import net.sundggs.avecoins.client.gui.AvecoinsScreen;
import net.sundggs.avecoins.client.gui.GuiTextLayout;
import net.sundggs.avecoins.client.gui.PanelButton;
import net.sundggs.avecoins.client.gui.PaymentReceipt;
import net.sundggs.avecoins.network.ClientNetworking;
import net.sundggs.avecoins.network.ServerboundAvecoinsPayload;
import net.sundggs.avecoins.shop.ShopData;
import net.sundggs.avecoins.shop.ShopService;

public final class PurchaseHistoryScreen
extends AvecoinsScreen {
    private static final int PANEL_WIDTH = 650;
    private static final int PANEL_HEIGHT = 400;
    private static final int LIST_WIDTH = 260;
    private static final int LIST_TOP = 96;
    private static final int LIST_ROW_HEIGHT = 24;
    private static final int TRANSACTIONS_PER_VIEW = 10;
    private static final int DETAIL_LINES_PER_PAGE = 3;
    private static final DateTimeFormatter LIST_DATE = DateTimeFormatter.ofPattern("MM-dd HH:mm").withZone(ZoneId.systemDefault());
    private static final DateTimeFormatter DETAIL_DATE = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm").withZone(ZoneId.systemDefault());
    private final ShopService.ShopSnapshot snapshot;
    private final String selectedCategory;
    private final int categoryPage;
    private final int productPage;
    private int selectedIndex;
    private int detailPage;
    private int listPage;

    public PurchaseHistoryScreen(ShopService.ShopSnapshot snapshot) {
        this(snapshot, snapshot.categories.isEmpty() ? "" : snapshot.categories.getFirst().id, 0, 0);
    }

    public PurchaseHistoryScreen(ShopService.ShopSnapshot snapshot, String selectedCategory, int categoryPage, int productPage) {
        this(snapshot, selectedCategory, categoryPage, productPage, 0);
    }

    private PurchaseHistoryScreen(ShopService.ShopSnapshot snapshot, String selectedCategory, int categoryPage, int productPage, int listPage) {
        super((Component)Component.translatable((String)"gui.avecoins.history.title"));
        this.snapshot = snapshot;
        this.useShopTheme(snapshot);
        this.selectedCategory = selectedCategory;
        this.categoryPage = categoryPage;
        this.productPage = productPage;
        this.listPage = listPage;
    }

    public PurchaseHistoryScreen refreshed(ShopService.ShopSnapshot snapshot) {
        return new PurchaseHistoryScreen(snapshot, this.selectedCategory, this.categoryPage, this.productPage, this.listPage);
    }

    protected void init() {
        this.layoutPanel(650, 400);
        this.addHeaderBackButton(new AdminShopScreen(this.snapshot, "", this.selectedCategory, this.categoryPage, this.productPage));
        int localPages = this.transactionPages();
        this.listPage = Math.min(this.listPage, localPages - 1);
        int firstVisible = this.listPage * 10;
        this.selectedIndex = this.snapshot.history.isEmpty() ? -1 : Math.max(firstVisible, Math.min(this.selectedIndex, this.snapshot.history.size() - 1));
        int listCenter = this.panelLeft + 130;
        Component transactionPager = this.transactionPagerText();
        int pagerWidth = Math.min(170, Math.round((float)this.font.width((FormattedText)transactionPager) * 0.8f));
        int pagerGap = 10;
        PanelButton previous = new PanelButton(listCenter - pagerWidth / 2 - pagerGap - 30, this.panelTop + 344, 30, 18, (Component)Component.literal((String)"\u2039"), PanelButton.Variant.SECONDARY, button -> this.previousTransactions());
        previous.active = this.listPage > 0 || this.snapshot.historyPage > 0;
        this.addRenderableWidget((GuiEventListener)previous);
        PanelButton next = new PanelButton(listCenter + pagerWidth / 2 + pagerGap, this.panelTop + 344, 30, 18, (Component)Component.literal((String)"\u203a"), PanelButton.Variant.SECONDARY, button -> this.nextTransactions());
        next.active = this.listPage + 1 < localPages || this.snapshot.historyPage + 1 < this.snapshot.historyPages;
        this.addRenderableWidget((GuiEventListener)next);
        ShopData.PurchaseRecord selected = this.selectedRecord();
        if (selected != null) {
            int pages = PurchaseHistoryScreen.detailPages(selected);
            this.detailPage = Math.min(this.detailPage, pages - 1);
            if (pages > 1) {
                int detailCenter = this.panelLeft + 260 + (this.panelWidth - 260) / 2;
                PanelButton linePrevious = new PanelButton(detailCenter - 72, this.panelTop + this.detailPagerY(selected), 30, 18, (Component)Component.literal((String)"\u2039"), PanelButton.Variant.SECONDARY, button -> {
                    this.detailPage = Math.max(0, this.detailPage - 1);
                    this.rebuildWidgets();
                });
                linePrevious.active = this.detailPage > 0;
                this.addRenderableWidget((GuiEventListener)linePrevious);
                PanelButton lineNext = new PanelButton(detailCenter + 42, this.panelTop + this.detailPagerY(selected), 30, 18, (Component)Component.literal((String)"\u203a"), PanelButton.Variant.SECONDARY, button -> {
                    this.detailPage = Math.min(pages - 1, this.detailPage + 1);
                    this.rebuildWidgets();
                });
                lineNext.active = this.detailPage + 1 < pages;
                this.addRenderableWidget((GuiEventListener)lineNext);
            }
        }
    }

    private void requestPage(int page, int targetListPage) {
        this.listPage = Math.max(0, targetListPage);
        ClientNetworking.send(new ServerboundAvecoinsPayload("request_shop_admin", "{\"history\":true,\"page\":" + page + "}"));
    }

    private void previousTransactions() {
        if (this.listPage > 0) {
            --this.listPage;
            this.selectedIndex = this.listPage * 10;
            this.detailPage = 0;
            this.rebuildWidgets();
        } else if (this.snapshot.historyPage > 0) {
            this.requestPage(this.snapshot.historyPage - 1, 1);
        }
    }

    private void nextTransactions() {
        if (this.listPage + 1 < this.transactionPages()) {
            ++this.listPage;
            this.selectedIndex = this.listPage * 10;
            this.detailPage = 0;
            this.rebuildWidgets();
        } else if (this.snapshot.historyPage + 1 < this.snapshot.historyPages) {
            this.requestPage(this.snapshot.historyPage + 1, 0);
        }
    }

    @Override
    public boolean mouseClicked(MouseButtonEvent event, boolean doubled) {
        double x = this.virtualMouseX(event.x());
        double y = this.virtualMouseY(event.y());
        if (event.button() == 0 && x >= (double)(this.panelLeft + 14) && x < (double)(this.panelLeft + 260 - 10)) {
            int localIndex = (int)((y - (double)(this.panelTop + 96)) / 24.0);
            int index = this.listPage * 10 + localIndex;
            if (y >= (double)(this.panelTop + 96) && localIndex >= 0 && localIndex < 10 && index < this.snapshot.history.size()) {
                this.selectedIndex = index;
                this.detailPage = 0;
                this.rebuildWidgets();
                return true;
            }
        }
        return super.mouseClicked(event, doubled);
    }

    @Override
    protected void renderPanel(GuiGraphicsExtractor graphics, int mouseX, int mouseY, float partialTick) {
        this.renderHeader(graphics, (Component)Component.translatable((String)"gui.avecoins.history.subtitle"));
        this.divider(graphics, this.panelTop + 66, 20);
        graphics.fill(this.panelLeft + 260, this.panelTop + 76, this.panelLeft + 260 + 1, this.panelTop + this.panelHeight - 30 - 10, this.theme().DIVIDER);
        this.drawSectionTitleAt(graphics, (Component)Component.translatable((String)"gui.avecoins.history.transactions"), this.panelLeft + 130, this.panelTop + 78);
        this.drawSectionTitleAt(graphics, (Component)Component.translatable((String)"gui.avecoins.history.transaction_detail"), this.panelLeft + 260 + (this.panelWidth - 260) / 2, this.panelTop + 78);
        this.renderTransactionList(graphics, mouseX, mouseY);
        this.renderDetail(graphics, mouseX, mouseY);
        this.drawCenteredScaledWrappedAt(graphics, this.transactionPagerText(), this.panelLeft + 130, this.panelTop + 349, 210, 0.8f, this.theme().MUTED);
    }

    private void renderTransactionList(GuiGraphicsExtractor graphics, int mouseX, int mouseY) {
        if (this.snapshot.history.isEmpty()) {
            this.drawCenteredWrappedAt(graphics, (Component)Component.translatable((String)"gui.avecoins.history.empty"), this.panelLeft + 130, this.panelTop + 170, 210, this.theme().MUTED);
            return;
        }
        int start = this.listPage * 10;
        int end = Math.min(this.snapshot.history.size(), start + 10);
        for (int index = start; index < end; ++index) {
            boolean hovered;
            ShopData.PurchaseRecord record = this.snapshot.history.get(index);
            int y = this.panelTop + 96 + (index - start) * 24;
            boolean selected = index == this.selectedIndex;
            boolean bl = hovered = mouseX >= this.panelLeft + 14 && mouseX < this.panelLeft + 260 - 10 && mouseY >= y && mouseY < y + 24;
            int background = selected ? this.theme().SURFACE_SELECTED : (hovered ? this.theme().SURFACE_HOVER : ((index & 1) == 0 ? this.theme().SURFACE : this.theme().ALTERNATE));
            graphics.fill(this.panelLeft + 14, y, this.panelLeft + 260 - 10, y + 24 - 1, background);
            if (selected) {
                graphics.fill(this.panelLeft + 14, y, this.panelLeft + 17, y + 24 - 1, record.success ? this.theme().SUCCESS : this.theme().ERROR);
            }
            graphics.text(this.font, LIST_DATE.format(Instant.ofEpochMilli(record.timestamp)), this.panelLeft + 22, y + 4, this.theme().MUTED, false);
            graphics.text(this.font, this.font.plainSubstrByWidth(record.playerName, 72), this.panelLeft + 101, y + 4, this.theme().TEXT, false);
            MutableComponent state = Component.translatable((String)(record.success ? "gui.avecoins.history.approved" : "gui.avecoins.history.rejected"));
            graphics.text(this.font, (Component)state, this.panelLeft + 178, y + 4, record.success ? this.theme().SUCCESS : this.theme().ERROR, false);
            int items = record.lines == null ? 0 : record.lines.stream().mapToInt(line -> Math.max(1, line.quantity) * Math.max(1, line.itemCount)).sum();
            graphics.text(this.font, (Component)Component.translatable((String)(record.type == ShopData.TransactionType.SALE ? "gui.avecoins.history.sale" : "gui.avecoins.history.purchase")), this.panelLeft + 22, y + 14, this.theme().DISCOUNT, false);
            graphics.text(this.font, (Component)Component.translatable((String)"gui.avecoins.history.items", (Object[])new Object[]{items}), this.panelLeft + 101, y + 14, this.theme().MUTED, false);
        }
    }

    private void renderDetail(GuiGraphicsExtractor graphics, int mouseX, int mouseY) {
        int detailPages;
        ShopData.PurchaseRecord record = this.selectedRecord();
        if (record == null) {
            return;
        }
        int left = this.panelLeft + 260 + 16;
        int right = this.panelLeft + this.panelWidth - 16;
        graphics.fill(left, this.panelTop + 96, right, this.panelTop + this.detailCardBottom(record), record.success ? this.theme().HISTORY_SUCCESS : this.theme().HISTORY_ERROR);
        graphics.text(this.font, (Component)Component.literal((String)record.playerName).withStyle(ChatFormatting.BOLD), left + 10, this.panelTop + 103, this.theme().TEXT, false);
        graphics.text(this.font, DETAIL_DATE.format(Instant.ofEpochMilli(record.timestamp)), left + 10, this.panelTop + 116, this.theme().MUTED, false);
        MutableComponent kind = Component.translatable((String)(record.type == ShopData.TransactionType.SALE ? "gui.avecoins.history.sale_destination" : "gui.avecoins.history.purchase_destination"), (Object[])new Object[]{Component.translatable((String)(record.paymentDestination == ShopData.PaymentDestination.WALLET ? "gui.avecoins.history.wallet" : (record.paymentDestination == ShopData.PaymentDestination.MIXED ? "gui.avecoins.history.mixed" : "gui.avecoins.history.inventory")))});
        graphics.text(this.font, (Component)kind, left + 10, this.panelTop + 130, this.theme().MUTED, false);
        MutableComponent status = Component.translatable((String)(record.success ? "gui.avecoins.history.approved" : "gui.avecoins.history.rejected"));
        graphics.text(this.font, (Component)status, right - this.font.width((FormattedText)status) - 10, this.panelTop + 103, record.success ? this.theme().SUCCESS : this.theme().ERROR, false);
        Map<String, Long> returned = PaymentReceipt.combine(record.inventoryChange, record.walletChange);
        if (record.success && !returned.isEmpty()) {
            PaymentReceipt.change(graphics, this.font, PaymentReceipt.combine(record.inventoryDebits, record.walletDebits), returned, left + 10, this.panelTop + 153, right - left - 20);
            this.drawScaledWrapped(graphics, PaymentReceipt.info("gui.avecoins.receipt.change_history_help"), left + 10, this.panelTop + 177, right - left - 20, 0.75f, this.theme().MUTED);
        } else {
            this.drawScaledWrapped(graphics, (Component)Component.translatable((String)("message.avecoins." + record.reason)), left + 10, this.panelTop + 151, right - left - 20, 0.8f, this.theme().MUTED);
        }
        graphics.text(this.font, (Component)Component.translatable((String)"gui.avecoins.history.products").withStyle(ChatFormatting.BOLD), left, this.panelTop + this.detailCardBottom(record) + 12, this.theme().TEXT, false);
        graphics.fill(left, this.panelTop + this.detailCardBottom(record) + 25, right, this.panelTop + this.detailCardBottom(record) + 26, this.theme().DIVIDER);
        List<Object> lines = record.lines == null ? List.of() : record.lines;
        int start = this.detailPage * 3;
        int end = Math.min(lines.size(), start + 3);
        ItemStack hoveredStack = ItemStack.EMPTY;
        for (int index = start; index < end; ++index) {
            ShopData.PurchaseLine line = (ShopData.PurchaseLine)lines.get(index);
            int y = this.panelTop + this.detailCardBottom(record) + 33 + (index - start) * 27;
            graphics.fill(left, y - 3, right, y + 21, (index & 1) == 0 ? this.theme().SURFACE : this.theme().ALTERNATE);
            ItemStack stack = PurchaseHistoryScreen.item(line.itemId);
            if (!stack.isEmpty()) {
                graphics.item(stack, left + 6, y + 1);
                if (mouseX >= left + 6 && mouseX < left + 22 && mouseY >= y + 1 && mouseY < y + 17) {
                    hoveredStack = stack;
                }
            }
            graphics.text(this.font, (Component)Component.literal((String)this.font.plainSubstrByWidth(line.productName, 185)).withStyle(ChatFormatting.BOLD), left + 29, y, this.theme().TEXT, false);
            int delivered = Math.max(1, line.quantity) * Math.max(1, line.itemCount);
            graphics.text(this.font, (Component)Component.translatable((String)(record.type == ShopData.TransactionType.SALE ? "gui.avecoins.history.sold" : "gui.avecoins.history.delivered"), (Object[])new Object[]{delivered}), left + 29, y + 11, this.theme().MUTED, false);
            this.renderCurrency(graphics, line.currencyId, line.lineTotal, right - 70, y + 1);
        }
        if (lines.isEmpty()) {
            GuiTextLayout.drawCentered(graphics, this.font, (Component)Component.translatable((String)"gui.avecoins.history.no_products"), left + (right - left) / 2, this.panelTop + this.detailCardBottom(record) + 48, this.theme().MUTED, !this.theme().custom);
        }
        if ((detailPages = PurchaseHistoryScreen.detailPages(record)) > 1) {
            GuiTextLayout.drawCentered(graphics, this.font, (Component)Component.translatable((String)"gui.avecoins.history.page", (Object[])new Object[]{this.detailPage + 1, detailPages}), left + (right - left) / 2, this.panelTop + this.detailPagerY(record) + 5, this.theme().MUTED, !this.theme().custom);
        }
        graphics.fill(left, this.panelTop + 344, right, this.panelTop + 345, this.theme().DIVIDER);
        PaymentReceipt.row(graphics, this.font, "gui.avecoins.history.total", record.totals, left + 10, this.panelTop + 348, PaymentReceipt.labelWidth(this.font), right - left - 20);
        if (!hoveredStack.isEmpty()) {
            graphics.setTooltipForNextFrame(this.font, hoveredStack, mouseX, mouseY);
        }
    }

    private int detailCardBottom(ShopData.PurchaseRecord record) {
        boolean change = record.success && !PaymentReceipt.combine(record.inventoryChange, record.walletChange).isEmpty();
        Component description = change ? PaymentReceipt.info("gui.avecoins.receipt.change_history_help") : Component.translatable((String)("message.avecoins." + record.reason));
        float scale = change ? 0.75f : 0.8f;
        int textWidth = 338;
        int lines = this.font.split((FormattedText)description, (int)((float)textWidth / scale)).size();
        return (change ? 177 : 151) + Math.max(1, lines) * Math.round(10.0f * scale) + 10;
    }

    private int detailPagerY(ShopData.PurchaseRecord record) {
        return this.detailCardBottom(record) + 33 + 75 + 12;
    }

    private int renderCurrency(GuiGraphicsExtractor graphics, String currencyId, long amount, int x, int y) {
        ItemStack currency = PurchaseHistoryScreen.item(currencyId);
        if (!currency.isEmpty()) {
            graphics.item(currency, x, y);
        }
        String value = Long.toString(amount);
        graphics.text(this.font, value, x + 19, y + 5, this.theme().TEXT, false);
        return 19 + this.font.width(value);
    }

    private static ItemStack item(String idValue) {
        Identifier id = Identifier.tryParse((String)idValue);
        return id != null && BuiltInRegistries.ITEM.containsKey(id) ? new ItemStack((ItemLike)BuiltInRegistries.ITEM.getValue(id)) : ItemStack.EMPTY;
    }

    private ShopData.PurchaseRecord selectedRecord() {
        return this.selectedIndex >= 0 && this.selectedIndex < this.snapshot.history.size() ? this.snapshot.history.get(this.selectedIndex) : null;
    }

    private static int detailPages(ShopData.PurchaseRecord record) {
        int size = record.lines == null ? 0 : record.lines.size();
        return Math.max(1, (size + 3 - 1) / 3);
    }

    private int transactionPages() {
        return Math.max(1, (this.snapshot.history.size() + 10 - 1) / 10);
    }

    private Component transactionPagerText() {
        return Component.translatable((String)"gui.avecoins.history.view", (Object[])new Object[]{this.listPage + 1, this.transactionPages(), this.snapshot.historyPage + 1, Math.max(1, this.snapshot.historyPages)});
    }
}

