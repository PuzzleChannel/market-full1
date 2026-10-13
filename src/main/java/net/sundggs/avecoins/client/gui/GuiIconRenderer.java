/*
 * Decompiled with CFR 0.152.
 */
package net.sundggs.avecoins.client.gui;

import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.renderer.RenderPipelines;
import net.minecraft.resources.Identifier;

final class GuiIconRenderer {
    static void blit(GuiGraphicsExtractor graphics, Identifier texture, int x, int y, int width, int height, int textureWidth, int textureHeight) {
        graphics.blit(RenderPipelines.GUI_TEXTURED, texture, x, y, 0.0f, 0.0f, width, height, textureWidth, textureHeight, textureWidth, textureHeight);
    }

    private GuiIconRenderer() {
    }
}

