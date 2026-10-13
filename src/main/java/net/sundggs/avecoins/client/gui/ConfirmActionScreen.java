/*
 * Decompiled with CFR 0.152.
 */
package net.sundggs.avecoins.client.gui;

import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.components.events.GuiEventListener;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
import net.sundggs.avecoins.client.gui.AvecoinsScreen;
import net.sundggs.avecoins.client.gui.GuiPalette;
import net.sundggs.avecoins.client.gui.GuiTextLayout;
import net.sundggs.avecoins.client.gui.PanelButton;
import net.sundggs.avecoins.client.gui.ScreenThemes;
import net.sundggs.avecoins.client.gui.ThemedScreen;

public final class ConfirmActionScreen
extends AvecoinsScreen {
    private final Screen parent;
    private final Component message;
    private final Runnable confirmed;

    public ConfirmActionScreen(Screen parent, Component message, Runnable confirmed) {
        super((Component)Component.translatable((String)"gui.avecoins.confirm.title"));
        this.parent = parent;
        this.message = message;
        this.confirmed = confirmed;
    }

    @Override
    public GuiPalette theme() {
        return ScreenThemes.of(this.parent);
    }

    @Override
    public void applyShopColor(String color) {
        Screen screen = this.parent;
        if (screen instanceof ThemedScreen) {
            ThemedScreen themed = (ThemedScreen)screen;
            themed.applyShopColor(color);
        }
    }

    protected void init() {
        this.layoutPanel(420, 240);
        this.addRenderableWidget(new PanelButton(this.panelLeft + 34, this.panelTop + 172, 140, 22, (Component)Component.translatable((String)"gui.avecoins.cancel"), PanelButton.Variant.SECONDARY, button -> this.minecraft.gui.setScreen(this.parent)));
        this.addRenderableWidget(new PanelButton(this.panelLeft + this.panelWidth - 174, this.panelTop + 172, 140, 22, (Component)Component.translatable((String)"gui.avecoins.confirm"), PanelButton.Variant.DANGER, button -> this.confirmed.run()));
    }

    @Override
    protected void renderPanel(GuiGraphicsExtractor graphics, int mouseX, int mouseY, float partialTick) {
        this.renderHeader(graphics, null);
        this.divider(graphics, this.panelTop + 48);
        graphics.fill(this.panelLeft + 30, this.panelTop + 68, this.panelLeft + this.panelWidth - 30, this.panelTop + 145, this.theme().SURFACE);
        GuiTextLayout.drawCentered(graphics, this.font, (Component)Component.literal((String)"!"), this.panelLeft + this.panelWidth / 2, this.panelTop + 82, this.theme().WARNING, !this.theme().custom);
        this.drawCenteredWrappedAt(graphics, this.message, this.panelLeft + this.panelWidth / 2, this.panelTop + 103, this.panelWidth - 92, this.theme().TEXT);
    }

    public void onClose() {
        this.minecraft.gui.setScreen(this.parent);
    }
}

