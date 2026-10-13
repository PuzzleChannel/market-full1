/*
 * Decompiled with CFR 0.152.
 */
package net.sundggs.avecoins.client.gui;

public record ResponsivePanelLayout(float scale, int virtualWidth, int virtualHeight, float offsetX, float offsetY, int panelLeft, int panelTop) {
    private static final int HORIZONTAL_MARGIN = 8;
    private static final int VERTICAL_MARGIN = 8;

    public static ResponsivePanelLayout fit(int screenWidth, int screenHeight, int panelWidth, int panelHeight) {
        if (screenWidth <= 0 || screenHeight <= 0 || panelWidth <= 0 || panelHeight <= 0) {
            throw new IllegalArgumentException("Responsive GUI dimensions must be positive");
        }
        int requiredWidth = panelWidth + 8;
        int requiredHeight = panelHeight + VERTICAL_MARGIN;
        float scale = Math.min(1.0f, Math.min((float)screenWidth / (float)requiredWidth, (float)screenHeight / (float)requiredHeight));
        int virtualWidth = Math.max(requiredWidth, (int)Math.floor((float)screenWidth / scale));
        int virtualHeight = Math.max(requiredHeight, (int)Math.floor((float)screenHeight / scale));
        float offsetX = ((float)screenWidth - (float)virtualWidth * scale) / 2.0f;
        float offsetY = ((float)screenHeight - (float)virtualHeight * scale) / 2.0f;
        int panelLeft = (virtualWidth - panelWidth) / 2;
        int panelTop = Math.max(4, (virtualHeight - panelHeight) / 2);
        return new ResponsivePanelLayout(scale, virtualWidth, virtualHeight, offsetX, offsetY, panelLeft, panelTop);
    }

    public int virtualMouseX(double mouseX) {
        return (int)Math.floor((mouseX - (double)this.offsetX) / (double)this.scale + 1.0E-4);
    }

    public int virtualMouseY(double mouseY) {
        return (int)Math.floor((mouseY - (double)this.offsetY) / (double)this.scale + 1.0E-4);
    }
}

