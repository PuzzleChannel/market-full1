/*
 * Decompiled with CFR 0.152.
 */
package net.sundggs.avecoins.client.gui;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.screens.Screen;
import net.sundggs.avecoins.client.gui.GuiPalette;
import net.sundggs.avecoins.client.gui.ThemedScreen;

final class ScreenThemes {
    static GuiPalette of(Screen screen) {
        GuiPalette guiPalette;
        if (screen instanceof ThemedScreen) {
            ThemedScreen themed = (ThemedScreen)screen;
            guiPalette = themed.theme();
        } else {
            guiPalette = GuiPalette.DEFAULT;
        }
        return guiPalette;
    }

    static GuiPalette current() {
        return ScreenThemes.of(Minecraft.getInstance().gui.screen());
    }

    private ScreenThemes() {
    }
}

