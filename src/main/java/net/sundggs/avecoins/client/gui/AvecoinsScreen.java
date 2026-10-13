/*
 * Decompiled with CFR 0.152.
 */
package net.sundggs.avecoins.client.gui;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import net.minecraft.ChatFormatting;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.components.events.GuiEventListener;
import net.minecraft.client.gui.navigation.ScreenRectangle;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.input.MouseButtonEvent;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.FormattedText;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.util.FormattedCharSequence;
import net.sundggs.avecoins.client.gui.GuiPalette;
import net.sundggs.avecoins.client.gui.GuiTextLayout;
import net.sundggs.avecoins.client.gui.PanelButton;
import net.sundggs.avecoins.client.gui.ResponsivePanelLayout;
import net.sundggs.avecoins.client.gui.ResultFeedback;
import net.sundggs.avecoins.client.gui.ThemedScreen;
import net.sundggs.avecoins.shop.ShopService;

public abstract class AvecoinsScreen
extends Screen
implements ThemedScreen {
    protected static final int HEADER_HEIGHT = 30;
    protected static final int FOOTER_HEIGHT = 30;
    protected int panelLeft;
    protected int panelTop;
    protected int panelWidth;
    protected int panelHeight;
    protected ResponsivePanelLayout responsiveLayout;
    private ShopService.ShopSnapshot themeSnapshot;
    private String paletteColor = "";
    private GuiPalette palette = GuiPalette.DEFAULT;
    private final List<Screen> themeDestinations = new ArrayList<Screen>();

    protected final void useShopTheme(ShopService.ShopSnapshot snapshot) {
        this.themeSnapshot = snapshot;
    }

    protected final void retainThemeDestination(Screen destination) {
        this.themeDestinations.add(destination);
    }

    @Override
    public GuiPalette theme() {
        String color;
        String string = color = this.themeSnapshot == null || this.themeSnapshot.shopColor == null ? "" : this.themeSnapshot.shopColor;
        if (!color.equals(this.paletteColor)) {
            this.palette = GuiPalette.fromHex(color);
            this.paletteColor = color;
        }
        return this.palette;
    }

    @Override
    public void applyShopColor(String color) {
        if (this.themeSnapshot != null) {
            this.themeSnapshot.shopColor = color;
        }
        for (Screen destination : this.themeDestinations) {
            if (!(destination instanceof ThemedScreen)) continue;
            ThemedScreen themed = (ThemedScreen)destination;
            themed.applyShopColor(color);
        }
    }

    protected AvecoinsScreen(Component title) {
        super(title);
    }

    protected void layoutPanel(int preferredWidth, int preferredHeight) {
        this.themeDestinations.clear();
        this.panelWidth = preferredWidth;
        this.panelHeight = preferredHeight;
        this.responsiveLayout = ResponsivePanelLayout.fit(this.width, this.height, this.panelWidth, this.panelHeight);
        this.panelLeft = this.responsiveLayout.panelLeft();
        this.panelTop = this.responsiveLayout.panelTop();
    }

    public final void extractRenderState(GuiGraphicsExtractor graphics, int mouseX, int mouseY, float partialTick) {
        if (Minecraft.getInstance().gui.screen() != this || this.responsiveLayout == null) return;
        graphics.pose().pushMatrix();
        try {
            graphics.pose().translate(this.responsiveLayout.offsetX(), this.responsiveLayout.offsetY());
            graphics.pose().scale(this.responsiveLayout.scale(), this.responsiveLayout.scale());
            int virtualMouseX = this.virtualMouseX(mouseX);
            int virtualMouseY = this.virtualMouseY(mouseY);
            super.extractRenderState(graphics, virtualMouseX, virtualMouseY, partialTick);
            this.renderPanel(graphics, virtualMouseX, virtualMouseY, partialTick);
        } finally {
            graphics.pose().popMatrix();
        }
    }

    protected abstract void renderPanel(GuiGraphicsExtractor var1, int var2, int var3, float var4);

    public boolean mouseClicked(MouseButtonEvent event, boolean doubled) {
        return super.mouseClicked(this.virtualEvent(event), doubled);
    }

    public boolean mouseReleased(MouseButtonEvent event) {
        return super.mouseReleased(this.virtualEvent(event));
    }

    public boolean mouseDragged(MouseButtonEvent event, double dragX, double dragY) {
        return super.mouseDragged(this.virtualEvent(event), dragX / (double)this.responsiveLayout.scale(), dragY / (double)this.responsiveLayout.scale());
    }

    public boolean mouseScrolled(double mouseX, double mouseY, double scrollX, double scrollY) {
        return super.mouseScrolled((double)this.virtualMouseX(mouseX), (double)this.virtualMouseY(mouseY), scrollX, scrollY);
    }

    public ScreenRectangle getRectangle() {
        if (this.responsiveLayout == null) {
            return super.getRectangle();
        }
        return new ScreenRectangle(0, 0, this.responsiveLayout.virtualWidth(), this.responsiveLayout.virtualHeight());
    }

    protected final int virtualMouseX(double mouseX) {
        if (this.responsiveLayout == null) {
            return (int)Math.floor(mouseX);
        }
        return this.responsiveLayout.virtualMouseX(mouseX);
    }

    protected final int virtualMouseY(double mouseY) {
        if (this.responsiveLayout == null) {
            return (int)Math.floor(mouseY);
        }
        return this.responsiveLayout.virtualMouseY(mouseY);
    }

    private MouseButtonEvent virtualEvent(MouseButtonEvent event) {
        return new MouseButtonEvent((double)this.virtualMouseX(event.x()), (double)this.virtualMouseY(event.y()), event.buttonInfo());
    }

    public void extractBackground(GuiGraphicsExtractor graphics, int mouseX, int mouseY, float partialTick) {
        if (Minecraft.getInstance().gui.screen() != this || this.responsiveLayout == null) return;
        graphics.pose().pushMatrix();
        try {
            graphics.pose().translate(this.responsiveLayout.offsetX(), this.responsiveLayout.offsetY());
            graphics.pose().scale(this.responsiveLayout.scale(), this.responsiveLayout.scale());
            graphics.fill(0, 0, this.responsiveLayout.virtualWidth(), this.responsiveLayout.virtualHeight(), 0xFF101318);
            graphics.fill(this.panelLeft, this.panelTop, this.panelLeft + this.panelWidth, this.panelTop + this.panelHeight, this.theme().BORDER);
            graphics.fill(this.panelLeft + 1, this.panelTop + 1, this.panelLeft + this.panelWidth - 1, this.panelTop + this.panelHeight - 1, this.theme().PANEL);
            graphics.fill(this.panelLeft + 1, this.panelTop + 1, this.panelLeft + this.panelWidth - 1, this.panelTop + this.panelHeaderHeight(), this.theme().HEADER);
            graphics.fill(this.panelLeft + 1, this.panelTop + this.panelHeight - this.panelFooterHeight(), this.panelLeft + this.panelWidth - 1, this.panelTop + this.panelHeight - 1, this.theme().HEADER);
        } finally {
            graphics.pose().popMatrix();
        }
    }

    protected int panelHeaderHeight() {
        return 30;
    }

    protected int panelFooterHeight() {
        return 30;
    }

    protected void renderHeader(GuiGraphicsExtractor graphics, Component subtitle) {
        MutableComponent heading = Component.literal((String)("\u00a4 " + this.title.getString().toUpperCase(Locale.ROOT))).withStyle(ChatFormatting.BOLD);
        GuiTextLayout.drawCentered(graphics, this.font, (Component)heading, this.panelLeft + this.panelWidth / 2, this.panelTop + 10, this.theme().TEXT, !this.theme().custom);
        if (subtitle != null) {
            this.drawCenteredWrapped(graphics, subtitle, this.panelTop + 45, this.panelWidth - 52, this.theme().MUTED);
        }
    }

    protected void addHeaderBackButton(Screen destination) {
        this.themeDestinations.add(destination);
        this.addRenderableWidget(new PanelButton(this.panelLeft + 8, this.panelTop + 6, 64, 18, (Component)Component.literal((String)"< ").append((Component)Component.translatable((String)"gui.avecoins.back")), PanelButton.Variant.DANGER, button -> this.minecraft.gui.setScreen(destination)));
    }

    protected int drawCenteredWrapped(GuiGraphicsExtractor graphics, Component text, int y, int maxWidth, int color) {
        return this.drawCenteredWrappedAt(graphics, text, this.panelLeft + this.panelWidth / 2, y, maxWidth, color);
    }

    protected int drawCenteredWrappedAt(GuiGraphicsExtractor graphics, Component text, int centerX, int y, int maxWidth, int color) {
        List<FormattedCharSequence> lines = this.font.split((FormattedText)text, maxWidth);
        int lineY = y;
        for (FormattedCharSequence line : lines) {
            graphics.text(this.font, line, centerX - this.font.width(line) / 2, lineY, color, !this.theme().custom);
            lineY += 10;
        }
        return lineY;
    }

    protected int drawWrapped(GuiGraphicsExtractor graphics, Component text, int x, int y, int maxWidth, int color) {
        List<FormattedCharSequence> lines = this.font.split((FormattedText)text, maxWidth);
        int lineY = y;
        for (FormattedCharSequence line : lines) {
            graphics.text(this.font, line, x, lineY, color, !this.theme().custom);
            lineY += 10;
        }
        return lineY;
    }

    protected void drawSectionTitle(GuiGraphicsExtractor graphics, Component text, int y) {
        this.drawSectionTitleAt(graphics, text, this.panelLeft + this.panelWidth / 2, y);
    }

    protected void drawSectionTitleAt(GuiGraphicsExtractor graphics, Component text, int centerX, int y) {
        GuiTextLayout.drawCentered(graphics, this.font, (Component)Component.literal((String)text.getString().toUpperCase(Locale.ROOT)).withStyle(ChatFormatting.BOLD), centerX, y, this.theme().TEXT, !this.theme().custom);
    }

    protected void drawScaledText(GuiGraphicsExtractor graphics, Component text, int x, int y, float scale, int color) {
        graphics.pose().pushMatrix();
        graphics.pose().scale(scale, scale);
        graphics.text(this.font, text, Math.round((float)x / scale), Math.round((float)y / scale), color, false);
        graphics.pose().popMatrix();
    }

    protected int drawScaledWrapped(GuiGraphicsExtractor graphics, Component text, int x, int y, int maxWidth, float scale, int color) {
        List<FormattedCharSequence> lines = this.font.split((FormattedText)text, Math.round((float)maxWidth / scale));
        int lineY = y;
        for (FormattedCharSequence line : lines) {
            graphics.pose().pushMatrix();
            graphics.pose().scale(scale, scale);
            graphics.text(this.font, line, Math.round((float)x / scale), Math.round((float)lineY / scale), color, false);
            graphics.pose().popMatrix();
            lineY += Math.round(10.0f * scale);
        }
        return lineY;
    }

    protected int drawCenteredScaledWrappedAt(GuiGraphicsExtractor graphics, Component text, int centerX, int y, int maxWidth, float scale, int color) {
        List<FormattedCharSequence> lines = this.font.split((FormattedText)text, Math.round((float)maxWidth / scale));
        int lineY = y;
        for (FormattedCharSequence line : lines) {
            int lineWidth = this.font.width(line);
            graphics.pose().pushMatrix();
            graphics.pose().scale(scale, scale);
            graphics.text(this.font, line, Math.round((float)centerX / scale - (float)lineWidth / 2.0f), Math.round((float)lineY / scale), color, false);
            graphics.pose().popMatrix();
            lineY += Math.round(10.0f * scale);
        }
        return lineY;
    }

    protected void renderStatusBox(GuiGraphicsExtractor graphics, String resultCode) {
        this.renderStatusBoxAt(graphics, resultCode, this.panelTop + this.panelHeight - 24);
    }

    protected void renderStatusBoxAt(GuiGraphicsExtractor graphics, String resultCode, int boxY) {
        if (resultCode == null || resultCode.isBlank()) {
            return;
        }
        int boxWidth = this.panelWidth - 40;
        int boxX = this.panelLeft + (this.panelWidth - boxWidth) / 2;
        boolean success = ResultFeedback.isSuccess(resultCode);
        graphics.fill(boxX, boxY, boxX + boxWidth, boxY + 18, success ? this.theme().SUCCESS_BOX : this.theme().DANGER);
        this.drawCenteredFitted(graphics, (Component)Component.translatable((String)("message.avecoins." + resultCode)), this.panelLeft + this.panelWidth / 2, boxY + 5, boxWidth - 12, this.theme().TEXT);
    }

    protected void renderFooterBox(GuiGraphicsExtractor graphics, Component message, int background, int color) {
        int boxWidth = this.panelWidth - 40;
        int boxX = this.panelLeft + (this.panelWidth - boxWidth) / 2;
        int boxY = this.panelTop + this.panelHeight - 24;
        graphics.fill(boxX, boxY, boxX + boxWidth, boxY + 18, background);
        this.drawCenteredFitted(graphics, message, this.panelLeft + this.panelWidth / 2, boxY + 5, boxWidth - 12, color);
    }

    private void drawCenteredFitted(GuiGraphicsExtractor graphics, Component text, int centerX, int y, int maxWidth, int color) {
        int textWidth = this.font.width((FormattedText)text);
        float scale = textWidth <= maxWidth ? 1.0f : Math.max(0.7f, (float)maxWidth / (float)textWidth);
        graphics.pose().pushMatrix();
        graphics.pose().scale(scale, scale);
        graphics.text(this.font, text, Math.round((float)centerX / scale - (float)textWidth / 2.0f), Math.round((float)y / scale), color, false);
        graphics.pose().popMatrix();
    }

    protected void divider(GuiGraphicsExtractor graphics, int y) {
        this.divider(graphics, y, 18);
    }

    protected void divider(GuiGraphicsExtractor graphics, int y, int inset) {
        graphics.fill(this.panelLeft + inset, y, this.panelLeft + this.panelWidth - inset, y + 1, this.theme().DIVIDER);
    }
}

