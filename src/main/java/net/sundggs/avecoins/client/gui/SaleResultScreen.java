/*
 * Decompiled with CFR 0.152.
 */
package net.sundggs.avecoins.client.gui;

import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.components.events.GuiEventListener;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
import net.sundggs.avecoins.client.gui.AvecoinsScreen;
import net.sundggs.avecoins.client.gui.GuiTextLayout;
import net.sundggs.avecoins.client.gui.PanelButton;
import net.sundggs.avecoins.client.gui.ShopScreen;
import net.sundggs.avecoins.network.ClientNetworking;
import net.sundggs.avecoins.network.ServerboundAvecoinsPayload;
import net.sundggs.avecoins.shop.ShopService;

public final class SaleResultScreen
extends AvecoinsScreen {
    private final String code;
    private final ShopService.ShopSnapshot snapshot;

    public SaleResultScreen(String code, ShopService.ShopSnapshot snapshot) {
        super((Component)Component.translatable((String)"gui.avecoins.sale.success_title"));
        this.code = code;
        this.snapshot = snapshot;
        this.useShopTheme(snapshot);
    }

    protected void init() {
        this.layoutPanel(370, 220);
        int buttonWidth = 128;
        int gap = 18;
        int buttonLeft = this.panelLeft + (this.panelWidth - buttonWidth * 2 - gap) / 2;
        this.addRenderableWidget(new PanelButton(buttonLeft, this.panelTop + 152, buttonWidth, 22, (Component)Component.translatable((String)"gui.avecoins.sale.back_shop"), PanelButton.Variant.SECONDARY, button -> this.backToShop()));
        this.addRenderableWidget(new PanelButton(buttonLeft + buttonWidth + gap, this.panelTop + 152, buttonWidth, 22, (Component)Component.translatable((String)"gui.avecoins.close"), PanelButton.Variant.DANGER, button -> this.onClose()));
    }

    private void backToShop() {
        this.minecraft.gui.setScreen((Screen)new ShopScreen(this.snapshot));
        ClientNetworking.send(new ServerboundAvecoinsPayload("request_shop", "{}"));
    }

    @Override
    protected void renderPanel(GuiGraphicsExtractor graphics, int mouseX, int mouseY, float partialTick) {
        this.renderHeader(graphics, null);
        int boxX = this.panelLeft + 28;
        int boxY = this.panelTop + 58;
        graphics.fill(boxX, boxY, this.panelLeft + this.panelWidth - 28, boxY + 72, this.theme().SUCCESS_BOX);
        GuiTextLayout.drawCentered(graphics, this.font, (Component)Component.literal((String)"\u2713"), this.panelLeft + this.panelWidth / 2, boxY + 12, this.theme().TEXT, !this.theme().custom);
        this.drawCenteredWrappedAt(graphics, (Component)Component.translatable((String)("message.avecoins." + this.code)), this.panelLeft + this.panelWidth / 2, boxY + 34, this.panelWidth - 86, this.theme().TEXT);
    }
}

