/*
 * Decompiled with CFR 0.152.
 */
package net.sundggs.avecoins.client.gui;

import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.FormattedText;

final class GuiTextLayout {
    static void drawCentered(GuiGraphicsExtractor graphics, Font font, Component text, int centerX, int y, int color, boolean shadow) {
        graphics.text(font, text, centerX - font.width((FormattedText)text) / 2, y, color, shadow);
    }

    static void drawCentered(GuiGraphicsExtractor graphics, Font font, String text, int centerX, int y, int color, boolean shadow) {
        graphics.text(font, text, centerX - font.width(text) / 2, y, color, shadow);
    }

    static void drawCenteredFitted(GuiGraphicsExtractor graphics, Font font, Component text, int centerX, int y, int maxWidth, int color) {
        int textWidth = font.width((FormattedText)text);
        float scale = textWidth <= maxWidth ? 1.0f : (float)maxWidth / (float)textWidth;
        graphics.pose().pushMatrix();
        graphics.pose().scale(scale, scale);
        graphics.text(font, text, Math.round((float)centerX / scale - (float)textWidth / 2.0f), Math.round((float)y / scale), color, false);
        graphics.pose().popMatrix();
    }

    private GuiTextLayout() {
    }
}

