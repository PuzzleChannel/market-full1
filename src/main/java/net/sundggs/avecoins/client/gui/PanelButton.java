/*
 * Decompiled with CFR 0.152.
 */
package net.sundggs.avecoins.client.gui;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.components.Button;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.FormattedText;
import net.minecraft.resources.Identifier;
import net.sundggs.avecoins.client.gui.GuiIconRenderer;
import net.sundggs.avecoins.client.gui.GuiPalette;
import net.sundggs.avecoins.client.gui.GuiTextLayout;
import net.sundggs.avecoins.client.gui.ScreenThemes;

public final class PanelButton
extends Button {
    private Variant variant;
    private int textColor;
    private int textRole;
    private boolean visuallyDisabled;
    private Identifier icon;
    private int iconSize;
    private int iconTextureSize = 32;
    private int iconGap = 5;

    public PanelButton(int x, int y, int width, int height, Component message, Variant variant, Button.OnPress onPress) {
        this(x, y, width, height, message, variant, -1710103, onPress);
        this.textRole = 0;
    }

    public PanelButton(int x, int y, int width, int height, Component message, Variant variant, int textColor, Button.OnPress onPress) {
        super(x, y, width, height, message, onPress, DEFAULT_NARRATION);
        this.variant = variant;
        this.setTextColor(textColor);
    }

    protected void extractContents(GuiGraphicsExtractor graphics, int mouseX, int mouseY, float partialTick) {
        int color;
        int background = !this.active || this.visuallyDisabled ? ScreenThemes.current().SURFACE : (this.variant == Variant.SUCCESS ? (this.isHoveredOrFocused() ? ScreenThemes.current().SUCCESS_BOX_HOVER : ScreenThemes.current().SUCCESS_BOX) : (this.variant == Variant.DANGER ? (this.isHoveredOrFocused() ? ScreenThemes.current().DANGER_HOVER : ScreenThemes.current().DANGER) : (this.variant == Variant.HEADER ? (this.isHoveredOrFocused() ? ScreenThemes.current().HEADER_BUTTON_HOVER : ScreenThemes.current().HEADER_BUTTON) : (this.variant == Variant.SELECTED ? (this.isHoveredOrFocused() ? ScreenThemes.current().HEADER_BUTTON_HOVER : ScreenThemes.current().SURFACE_SELECTED) : (this.variant == Variant.PRIMARY && this.isHoveredOrFocused() ? ScreenThemes.current().SURFACE_SELECTED : (this.isHoveredOrFocused() ? ScreenThemes.current().SURFACE_HOVER : ScreenThemes.current().SURFACE))))));
        graphics.fill(this.getX(), this.getY(), this.getX() + this.getWidth(), this.getY() + this.getHeight(), background);
        if (this.variant == Variant.SELECTED && this.active && !this.visuallyDisabled) {
            graphics.fill(this.getX(), this.getY(), this.getX() + 2, this.getY() + this.getHeight(), ScreenThemes.current().SELECTED_ACCENT);
        }
        GuiPalette palette = ScreenThemes.current();
        int preferred = this.textRole < 0 ? this.textColor : palette.roleColor(this.textRole);
        int n = color = this.active && !this.visuallyDisabled ? palette.textOn(preferred, background) : palette.DISABLED;
        if (palette.custom && this.active && !this.visuallyDisabled && this.isHoveredOrFocused()) {
            graphics.fill(this.getX(), this.getY(), this.getX() + this.getWidth(), this.getY() + 1, palette.BORDER);
            graphics.fill(this.getX(), this.getY() + this.getHeight() - 1, this.getX() + this.getWidth(), this.getY() + this.getHeight(), palette.BORDER);
            graphics.fill(this.getX(), this.getY(), this.getX() + 1, this.getY() + this.getHeight(), palette.BORDER);
            graphics.fill(this.getX() + this.getWidth() - 1, this.getY(), this.getX() + this.getWidth(), this.getY() + this.getHeight(), palette.BORDER);
        }
        if (this.icon == null) {
            GuiTextLayout.drawCentered(graphics, Minecraft.getInstance().font, this.getMessage(), this.getX() + this.getWidth() / 2, this.getY() + (this.getHeight() - 8) / 2, color, !ScreenThemes.current().custom);
        } else {
            int textWidth = Minecraft.getInstance().font.width((FormattedText)this.getMessage());
            int groupWidth = this.iconSize + (textWidth > 0 ? this.iconGap + textWidth : 0);
            int groupX = this.getX() + (this.getWidth() - groupWidth) / 2;
            int iconY = this.getY() + (this.getHeight() - this.iconSize) / 2;
            GuiIconRenderer.blit(graphics, this.icon, groupX, iconY, this.iconSize, this.iconSize, this.iconTextureSize, this.iconTextureSize);
            if (textWidth > 0) {
                graphics.text(Minecraft.getInstance().font, this.getMessage(), groupX + this.iconSize + this.iconGap, this.getY() + (this.getHeight() - 8) / 2, color, false);
            }
        }
    }

    public PanelButton setVisuallyDisabled(boolean visuallyDisabled) {
        this.visuallyDisabled = visuallyDisabled;
        return this;
    }

    public PanelButton withIcon(Identifier icon, int iconSize) {
        this.icon = icon;
        this.iconSize = iconSize;
        return this;
    }

    public PanelButton setTextColor(int textColor) {
        this.textColor = textColor;
        this.textRole = GuiPalette.DEFAULT.textRole(textColor);
        if (this.textRole < 0) {
            this.textRole = ScreenThemes.current().textRole(textColor);
        }
        return this;
    }

    public PanelButton setVariant(Variant variant) {
        this.variant = variant;
        return this;
    }

    public static enum Variant {
        PRIMARY,
        SECONDARY,
        SUCCESS,
        DANGER,
        HEADER,
        SELECTED;

    }
}

