/*
 * Decompiled with CFR 0.152.
 */
package net.sundggs.avecoins.client.gui;

import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.navigation.ScreenRectangle;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.client.input.MouseButtonEvent;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.sundggs.avecoins.client.gui.ResponsivePanelLayout;

abstract class ResponsiveContainerScreen<T extends AbstractContainerMenu>
extends AbstractContainerScreen<T> {
    private ResponsivePanelLayout responsiveLayout;

    protected ResponsiveContainerScreen(T menu, Inventory inventory, Component title, int imageWidth, int imageHeight) {
        super(menu, inventory, title, imageWidth, imageHeight);
    }

    protected void init() {
        super.init();
        this.responsiveLayout = ResponsivePanelLayout.fit(this.width, this.height, this.imageWidth, this.imageHeight);
        this.leftPos = this.responsiveLayout.panelLeft();
        this.topPos = this.responsiveLayout.panelTop();
    }

    public final void extractRenderState(GuiGraphicsExtractor graphics, int mouseX, int mouseY, float partialTick) {
        if (net.minecraft.client.Minecraft.getInstance().gui.screen() != this || this.responsiveLayout == null) return;
        this.beforeResponsiveRender();
        graphics.pose().pushMatrix();
        try {
            graphics.pose().translate(this.responsiveLayout.offsetX(), this.responsiveLayout.offsetY());
            graphics.pose().scale(this.responsiveLayout.scale(), this.responsiveLayout.scale());
            int virtualMouseX = this.virtualMouseX(mouseX);
            int virtualMouseY = this.virtualMouseY(mouseY);
            super.extractRenderState(graphics, virtualMouseX, virtualMouseY, partialTick);
        } finally {
            graphics.pose().popMatrix();
        }
    }

    protected void beforeResponsiveRender() {
    }

    public void extractBackground(GuiGraphicsExtractor graphics, int mouseX, int mouseY, float partialTick) {
        if (net.minecraft.client.Minecraft.getInstance().gui.screen() != this || this.responsiveLayout == null) return;
        graphics.pose().pushMatrix();
        try {
            graphics.pose().translate(this.responsiveLayout.offsetX(), this.responsiveLayout.offsetY());
            graphics.pose().scale(this.responsiveLayout.scale(), this.responsiveLayout.scale());
            graphics.fill(0, 0, this.responsiveLayout.virtualWidth(), this.responsiveLayout.virtualHeight(), 0xFF101318);
            this.extractPanelBackground(graphics, partialTick, this.virtualMouseX(mouseX), this.virtualMouseY(mouseY));
        } finally {
            graphics.pose().popMatrix();
        }
    }

    protected abstract void extractPanelBackground(GuiGraphicsExtractor var1, float var2, int var3, int var4);

    public boolean mouseClicked(MouseButtonEvent event, boolean doubled) {
        return super.mouseClicked(this.virtualEvent(event), doubled);
    }

    public boolean mouseReleased(MouseButtonEvent event) {
        return super.mouseReleased(this.virtualEvent(event));
    }

    public boolean mouseDragged(MouseButtonEvent event, double dragX, double dragY) {
        float scale = this.responsiveLayout == null ? 1.0f : this.responsiveLayout.scale();
        return super.mouseDragged(this.virtualEvent(event), dragX / (double)scale, dragY / (double)scale);
    }

    public boolean mouseScrolled(double mouseX, double mouseY, double scrollX, double scrollY) {
        return super.mouseScrolled((double)this.virtualMouseX(mouseX), (double)this.virtualMouseY(mouseY), scrollX, scrollY);
    }

    public ScreenRectangle getRectangle() {
        return this.responsiveLayout == null ? super.getRectangle() : new ScreenRectangle(0, 0, this.responsiveLayout.virtualWidth(), this.responsiveLayout.virtualHeight());
    }

    private int virtualMouseX(double mouseX) {
        return this.responsiveLayout == null ? (int)Math.floor(mouseX) : this.responsiveLayout.virtualMouseX(mouseX);
    }

    private int virtualMouseY(double mouseY) {
        return this.responsiveLayout == null ? (int)Math.floor(mouseY) : this.responsiveLayout.virtualMouseY(mouseY);
    }

    private MouseButtonEvent virtualEvent(MouseButtonEvent event) {
        return new MouseButtonEvent((double)this.virtualMouseX(event.x()), (double)this.virtualMouseY(event.y()), event.buttonInfo());
    }
}

