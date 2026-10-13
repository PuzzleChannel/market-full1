/*
 * Decompiled with CFR 0.152.
 */
package net.sundggs.avecoins.client.gui;

import com.mojang.blaze3d.platform.InputConstants;
import com.mojang.blaze3d.platform.Window;
import net.minecraft.client.Minecraft;

final class ShiftKeyState {
    private ShiftKeyState() {
    }

    static boolean isDown() {
        Minecraft minecraft = Minecraft.getInstance();
        return InputConstants.isKeyDown((Window)minecraft.getWindow(), (int)340) || InputConstants.isKeyDown((Window)minecraft.getWindow(), (int)344);
    }
}

