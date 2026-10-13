/*
 * Decompiled with CFR 0.152.
 */
package net.sundggs.avecoins.client.gui;

final class ShopSettingsLayout {
    static final int WIDTH = 440;
    static final int HEIGHT = 532;
    static final int COLOR_TOP = 386;
    static final int PREVIEW_HEIGHT = 54;
    static final int SAVE_TOP = 464;
    static final int SAVE_HEIGHT = 22;

    static int previewTop(int blockBottom) {
        return 386 + (blockBottom - 386 - 54) / 2;
    }

    private ShopSettingsLayout() {
    }
}

