/*
 * Decompiled with CFR 0.152.
 */
package net.sundggs.avecoins.client.gui;

import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.components.events.GuiEventListener;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
import net.sundggs.avecoins.client.gui.AvecoinsScreen;
import net.sundggs.avecoins.client.gui.CartScreen;
import net.sundggs.avecoins.client.gui.GuiTextLayout;
import net.sundggs.avecoins.client.gui.PanelButton;
import net.sundggs.avecoins.client.gui.ShopScreen;
import net.sundggs.avecoins.shop.ShopService;

public final class PurchaseResultScreen
extends AvecoinsScreen {
    private final boolean success;
    private final String code;
    private final ShopService.ShopSnapshot snapshot;
    private final Screen shopDestination;
    private final Screen cartDestination;

    public PurchaseResultScreen(boolean success, String code, ShopService.ShopSnapshot snapshot) {
        this(success, code, snapshot, new ShopScreen(snapshot), new CartScreen(snapshot));
    }

    public PurchaseResultScreen(boolean success, String code, ShopService.ShopSnapshot snapshot, Screen shopDestination, Screen cartDestination) {
        super((Component)Component.translatable((String)(success ? "gui.avecoins.purchase.success_title" : "gui.avecoins.purchase.rejected_title")));
        this.success = success;
        this.code = code;
        this.snapshot = snapshot;
        this.useShopTheme(snapshot);
        this.shopDestination = shopDestination;
        this.cartDestination = cartDestination;
    }

    protected void init() {
        this.layoutPanel(370, 220);
        this.retainThemeDestination(this.shopDestination);
        this.retainThemeDestination(this.cartDestination);
        int buttonWidth = 128;
        int gap = 18;
        int buttonLeft = this.panelLeft + (this.panelWidth - buttonWidth * 2 - gap) / 2;
        this.addRenderableWidget(new PanelButton(buttonLeft, this.panelTop + 152, buttonWidth, 22, (Component)Component.translatable((String)(this.success ? "gui.avecoins.purchase.back_shop" : "gui.avecoins.purchase.back_cart")), PanelButton.Variant.SECONDARY, button -> this.minecraft.gui.setScreen(this.success ? this.shopDestination : this.cartDestination)));
        this.addRenderableWidget(new PanelButton(buttonLeft + buttonWidth + gap, this.panelTop + 152, buttonWidth, 22, (Component)Component.translatable((String)"gui.avecoins.close"), PanelButton.Variant.DANGER, button -> this.onClose()));
    }

    @Override
    protected void renderPanel(GuiGraphicsExtractor graphics, int mouseX, int mouseY, float partialTick) {
        this.renderHeader(graphics, null);
        int boxX = this.panelLeft + 28;
        int boxY = this.panelTop + 58;
        graphics.fill(boxX, boxY, this.panelLeft + this.panelWidth - 28, boxY + 72, this.success ? this.theme().SUCCESS_BOX : this.theme().DANGER);
        GuiTextLayout.drawCentered(graphics, this.font, (Component)Component.literal((String)(this.success ? "\u2713" : "\u00d7")), this.panelLeft + this.panelWidth / 2, boxY + 12, this.theme().TEXT, !this.theme().custom);
        this.drawCenteredWrappedAt(graphics, (Component)Component.translatable((String)("message.avecoins." + this.code)), this.panelLeft + this.panelWidth / 2, boxY + 34, this.panelWidth - 86, this.theme().TEXT);
    }
}

