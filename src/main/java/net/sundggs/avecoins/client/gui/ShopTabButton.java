/*
 * Decompiled with CFR 0.152.
 */
package net.sundggs.avecoins.client.gui;

import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.components.AbstractButton;
import net.minecraft.client.gui.narration.NarrationElementOutput;
import net.minecraft.client.input.InputWithModifiers;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.sundggs.avecoins.client.gui.GuiIconRenderer;

public final class ShopTabButton
extends AbstractButton {
    private static final Identifier ICON = Identifier.fromNamespaceAndPath((String)"avecoins", (String)"textures/gui/icons/shop.png");
    private final Runnable pressed;

    public ShopTabButton(int x, int y, Runnable pressed) {
        super(x, y, 24, 28, (Component)Component.translatable((String)"gui.avecoins.shop.open"));
        this.pressed = pressed;
    }

    public void onPress(InputWithModifiers input) {
        this.pressed.run();
    }

    protected void extractContents(GuiGraphicsExtractor graphics, int mouseX, int mouseY, float partialTick) {
        int fill = this.isHoveredOrFocused() ? -12959674 : -14670808;
        graphics.fill(this.getX(), this.getY(), this.getX() + this.width, this.getY() + this.height, -10656661);
        graphics.fill(this.getX() + 1, this.getY() + 1, this.getX() + this.width, this.getY() + this.height - 1, fill);
        GuiIconRenderer.blit(graphics, ICON, this.getX() + 4, this.getY() + 6, 16, 16, 32, 32);
    }

    protected void updateWidgetNarration(NarrationElementOutput output) {
        this.defaultButtonNarrationText(output);
    }
}

