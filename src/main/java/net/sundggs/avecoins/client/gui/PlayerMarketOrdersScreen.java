package net.sundggs.avecoins.client.gui;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphicsExtractor;
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

/** Lists purchased player-market items while they wait and allows manual collection. */
public final class PlayerMarketOrdersScreen extends AvecoinsScreen {
    private static final int PAGE_SIZE = 9;
    private static final int COLUMNS = 3;
    private static final int CARD_HEIGHT = 78;
    private static final int CARD_STEP = 84;
    private static final Identifier COIN_ID = Identifier.fromNamespaceAndPath("vmtrade", "vmcoin");

    private final PlayerMarketService.MarketSnapshot snapshot;
    private final Map<String, ItemStack> previewCache = new HashMap<>();
    private String resultCode;
    private int page;
    private String confirmingAccelerationId = "";
    private String lastReadinessSignature = "";

    public PlayerMarketOrdersScreen(PlayerMarketService.MarketSnapshot snapshot, String resultCode) {
        super(Component.translatable("gui.avecoins.player_market.orders_title"));
        this.snapshot = snapshot;
        this.resultCode = resultCode == null ? "" : resultCode;
        ShopService.ShopSnapshot themeSnapshot = new ShopService.ShopSnapshot();
        themeSnapshot.shopColor = snapshot.shopColor;
        this.useShopTheme(themeSnapshot);
    }

    public PlayerMarketOrdersScreen refreshed(PlayerMarketService.MarketSnapshot snapshot, String resultCode) {
        PlayerMarketOrdersScreen refreshed = new PlayerMarketOrdersScreen(snapshot, resultCode);
        refreshed.page = this.page;
        return refreshed;
    }

    @Override
    protected void init() {
        this.layoutPanel(820, 440);
        int headerY = this.panelTop + 7;
        this.addRenderableWidget(new PanelButton(this.panelLeft + 10, headerY, 120, 20,
                Component.translatable("gui.avecoins.player_market.orders_back"), PanelButton.Variant.SECONDARY,
                button -> ClientNetworking.send(new ServerboundAvecoinsPayload("request_player_market", "{}"))));
        this.addRenderableWidget(new PanelButton(this.panelLeft + this.panelWidth - 28, headerY, 18, 20,
                Component.literal("×"), PanelButton.Variant.DANGER, button -> this.onClose()));

        List<PlayerMarketService.MarketDelivery> orders = this.snapshot.deliveries;
        int maxPage = Math.max(0, (orders.size() - 1) / PAGE_SIZE);
        this.page = Math.min(this.page, maxPage);
        int listX = this.panelLeft + 10;
        int rowWidth = this.panelWidth - 20;
        int cardWidth = (rowWidth - (COLUMNS - 1) * 8) / COLUMNS;
        int listTop = this.panelTop + 64;
        int start = this.page * PAGE_SIZE;
        int end = Math.min(orders.size(), start + PAGE_SIZE);

        for (int index = start; index < end; index++) {
            PlayerMarketService.MarketDelivery order = orders.get(index);
            int local = index - start;
            int cardX = listX + (local % COLUMNS) * (cardWidth + 8);
            int cardY = listTop + (local / COLUMNS) * CARD_STEP;
            boolean ready = order.deliverAt <= System.currentTimeMillis();
            boolean confirming = order.id.equals(this.confirmingAccelerationId);
            if (ready) {
                PanelButton collect = new PanelButton(cardX + cardWidth - 108, cardY + 56, 100, 18,
                        Component.translatable("gui.avecoins.player_market.orders_claim"),
                        PanelButton.Variant.SUCCESS, button -> {
                            com.google.gson.JsonObject payload = new com.google.gson.JsonObject();
                            payload.addProperty("id", order.id);
                            ClientNetworking.send(new ServerboundAvecoinsPayload("market_claim_delivery", payload.toString()));
                        });
                collect.setTooltip(net.minecraft.client.gui.components.Tooltip.create(
                        Component.translatable("gui.avecoins.player_market.claim_tooltip")));
                this.addRenderableWidget(collect);
            } else if (confirming) {
                PanelButton confirm = new PanelButton(cardX + cardWidth - 154, cardY + 56, 104, 18,
                        Component.translatable("gui.avecoins.player_market.confirm_acceleration_button", order.accelerationCost),
                        PanelButton.Variant.SUCCESS, button -> {
                            if (this.snapshot.walletBalance < order.accelerationCost) {
                                this.confirmingAccelerationId = "";
                                this.resultCode = "market_delivery_acceleration_insufficient";
                                this.rebuildWidgets();
                                return;
                            }
                            this.confirmingAccelerationId = "";
                            this.resultCode = "";
                            com.google.gson.JsonObject payload = new com.google.gson.JsonObject();
                            payload.addProperty("id", order.id);
                            ClientNetworking.send(new ServerboundAvecoinsPayload("market_accelerate_delivery", payload.toString()));
                        });
                confirm.setTooltip(net.minecraft.client.gui.components.Tooltip.create(
                        Component.translatable("gui.avecoins.player_market.acceleration_tooltip",
                                order.accelerationCost, this.snapshot.walletBalance)));
                PanelButton cancel = new PanelButton(cardX + cardWidth - 46, cardY + 56, 38, 18,
                        Component.translatable("gui.avecoins.player_market.cancel_confirmation"),
                        PanelButton.Variant.SECONDARY, button -> {
                            this.confirmingAccelerationId = "";
                            this.resultCode = "";
                            this.rebuildWidgets();
                        });
                this.addRenderableWidget(confirm);
                this.addRenderableWidget(cancel);
            } else {
                PanelButton accelerate = new PanelButton(cardX + cardWidth - 108, cardY + 56, 100, 18,
                        Component.translatable("gui.avecoins.player_market.orders_accelerate", order.accelerationCost),
                        PanelButton.Variant.SECONDARY, button -> {
                            if (this.snapshot.walletBalance < order.accelerationCost) {
                                this.confirmingAccelerationId = "";
                                this.resultCode = "market_delivery_acceleration_insufficient";
                                this.rebuildWidgets();
                                return;
                            }
                            this.confirmingAccelerationId = order.id;
                            this.resultCode = "market_confirm_acceleration";
                            this.rebuildWidgets();
                        });
                if (this.snapshot.walletBalance < order.accelerationCost) {
                    long shortfall = order.accelerationCost - this.snapshot.walletBalance;
                    accelerate.setTooltip(net.minecraft.client.gui.components.Tooltip.create(
                            Component.translatable("gui.avecoins.player_market.acceleration_shortfall_tooltip",
                                    shortfall, order.accelerationCost, this.snapshot.walletBalance)));
                } else {
                    accelerate.setTooltip(net.minecraft.client.gui.components.Tooltip.create(
                            Component.translatable("gui.avecoins.player_market.acceleration_tooltip",
                                    order.accelerationCost, this.snapshot.walletBalance)));
                }
                this.addRenderableWidget(accelerate);
            }
        }

        if (maxPage > 0) {
            int center = this.panelLeft + this.panelWidth / 2;
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
        this.lastReadinessSignature = this.readinessSignature();
    }

    @Override
    public void tick() {
        super.tick();
        String signature = this.readinessSignature();
        if (!signature.equals(this.lastReadinessSignature)) {
            this.lastReadinessSignature = signature;
            this.rebuildWidgets();
        }
    }

    private String readinessSignature() {
        long now = System.currentTimeMillis();
        StringBuilder signature = new StringBuilder();
        for (PlayerMarketService.MarketDelivery order : this.snapshot.deliveries) {
            signature.append(order.id).append(order.deliverAt <= now ? 'R' : 'W').append(';');
        }
        return signature.toString();
    }

    private ItemStack previewStack(PlayerMarketService.MarketDelivery order) {
        ItemStack cached = this.previewCache.get(order.id);
        if (cached != null) return cached.copy();
        Identifier id = Identifier.tryParse(order.itemId);
        if (id == null || !BuiltInRegistries.ITEM.containsKey(id)) return ItemStack.EMPTY;
        Item item = (Item) BuiltInRegistries.ITEM.getValue(id);
        ItemStack stack = ItemStack.EMPTY;
        if (this.minecraft.level != null && order.itemData != null && !order.itemData.isBlank()) {
            stack = ShopItemStacks.decodeExpected(this.minecraft.level.registryAccess(), order.itemData, item)
                    .orElse(ItemStack.EMPTY);
        }
        if (stack.isEmpty()) stack = new ItemStack(item);
        stack = stack.copyWithCount(Math.min(Math.max(1, order.quantity), stack.getMaxStackSize()));
        this.previewCache.put(order.id, stack.copy());
        return stack;
    }

    @Override
    public void extractBackground(GuiGraphicsExtractor graphics, int mouseX, int mouseY, float partialTick) {
        super.extractBackground(graphics, mouseX, mouseY, partialTick);
        if (Minecraft.getInstance().gui.screen() != this || this.responsiveLayout == null) return;
        graphics.pose().pushMatrix();
        try {
            graphics.pose().translate(this.responsiveLayout.offsetX(), this.responsiveLayout.offsetY());
            graphics.pose().scale(this.responsiveLayout.scale(), this.responsiveLayout.scale());
            int listX = this.panelLeft + 10;
            int rowWidth = this.panelWidth - 20;
            int cardWidth = (rowWidth - (COLUMNS - 1) * 8) / COLUMNS;
            int listTop = this.panelTop + 64;
            int start = this.page * PAGE_SIZE;
            int end = Math.min(this.snapshot.deliveries.size(), start + PAGE_SIZE);
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
        this.drawSectionTitleAt(graphics, Component.translatable("gui.avecoins.player_market.orders_title"),
                this.panelLeft + this.panelWidth / 2, this.panelTop + 43);

        List<PlayerMarketService.MarketDelivery> orders = this.snapshot.deliveries;
        int listX = this.panelLeft + 10;
        int rowWidth = this.panelWidth - 20;
        int cardWidth = (rowWidth - (COLUMNS - 1) * 8) / COLUMNS;
        int listTop = this.panelTop + 64;
        int start = this.page * PAGE_SIZE;
        int end = Math.min(orders.size(), start + PAGE_SIZE);

        if (orders.isEmpty()) {
            this.drawCenteredWrappedAt(graphics, Component.translatable("gui.avecoins.player_market.orders_empty"),
                    this.panelLeft + this.panelWidth / 2, listTop + 70, rowWidth - 60, this.theme().MUTED);
        }

        long now = System.currentTimeMillis();
        for (int index = start; index < end; index++) {
            PlayerMarketService.MarketDelivery order = orders.get(index);
            int local = index - start;
            int cardX = listX + (local % COLUMNS) * (cardWidth + 8);
            int cardY = listTop + (local / COLUMNS) * CARD_STEP;
            ItemStack preview = this.previewStack(order);
            if (!preview.isEmpty()) {
                graphics.item(preview, cardX + 7, cardY + 8);
                if (mouseX >= cardX + 6 && mouseX < cardX + 28
                        && mouseY >= cardY + 7 && mouseY < cardY + 30) {
                    graphics.setTooltipForNextFrame(this.font, preview, mouseX, mouseY);
                }
            }
            String title = (preview.isEmpty() ? order.displayName : preview.getHoverName().getString())
                    + (order.quantity > 1 ? " ×" + order.quantity : "");
            graphics.text(this.font, Component.literal(this.font.plainSubstrByWidth(title, cardWidth - 42)),
                    cardX + 31, cardY + 8, this.theme().TEXT, !this.theme().custom);
            graphics.text(this.font,
                    Component.translatable("gui.avecoins.player_market.orders_price", order.price),
                    cardX + 8, cardY + 34, this.theme().TEXT, !this.theme().custom);

            if (order.deliverAt <= now) {
                graphics.text(this.font, Component.translatable("gui.avecoins.player_market.orders_ready"),
                        cardX + 8, cardY + 46, this.theme().SUCCESS, !this.theme().custom);
            } else {
                long left = order.deliverAt - now;
                long minutes = left / 60000L;
                long seconds = (left % 60000L + 999L) / 1000L;
                if (seconds >= 60L) { minutes++; seconds = 0L; }
                String remaining = minutes + ":" + String.format(java.util.Locale.ROOT, "%02d", seconds);
                graphics.text(this.font,
                        Component.translatable("gui.avecoins.player_market.orders_waiting", remaining),
                        cardX + 8, cardY + 46, this.theme().MUTED, !this.theme().custom);
            }
            if (COIN_ID != null && BuiltInRegistries.ITEM.containsKey(COIN_ID)) {
                graphics.item(new ItemStack((Item) BuiltInRegistries.ITEM.getValue(COIN_ID)),
                        cardX + cardWidth - 22, cardY + 31);
            }
        }

        int maxPage = Math.max(0, (orders.size() - 1) / PAGE_SIZE);
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
        boolean hasIcon = COIN_ID != null && BuiltInRegistries.ITEM.containsKey(COIN_ID);
        int iconSpace = hasIcon ? 20 : 0;
        int totalWidth = this.font.width(label) + 8 + iconSpace + this.font.width(amount);
        int x = centerX - totalWidth / 2;
        graphics.text(this.font, label, x, iconY + 4, this.theme().TEXT, !this.theme().custom);
        x += this.font.width(label) + 8;
        if (hasIcon) {
            graphics.item(new ItemStack((Item) BuiltInRegistries.ITEM.getValue(COIN_ID)), x, iconY);
            x += 20;
        }
        graphics.text(this.font, Component.literal(amount), x, iconY + 4, this.theme().TEXT, !this.theme().custom);
    }
}
