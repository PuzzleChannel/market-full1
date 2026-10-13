/*
 * Decompiled with CFR 0.152.
 */
package net.sundggs.avecoins.client.gui;

import net.minecraft.ChatFormatting;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.components.AbstractButton;
import net.minecraft.client.gui.narration.NarrationElementOutput;
import net.minecraft.client.input.InputWithModifiers;
import net.minecraft.client.input.MouseButtonEvent;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.FormattedText;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.resources.Identifier;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.ItemLike;
import net.sundggs.avecoins.client.gui.GuiTextLayout;
import net.sundggs.avecoins.client.gui.ProductDisplayStack;
import net.sundggs.avecoins.client.gui.ScreenThemes;
import net.sundggs.avecoins.shop.ShopData;

final class ProductCardButton
extends AbstractButton {
    static final int HEIGHT = 88;
    private static final int CONTROL_HEIGHT = 14;
    private static final int CONTROL_GAP = 2;
    private static final int[] CONTROL_WIDTHS = new int[]{20, 28, 20};
    private final ShopData.Product product;
    private final ItemStack displayStack;
    private final int quantity;
    private final Runnable add;
    private final Runnable decrement;
    private final Runnable increment;
    private final Runnable remove;

    ProductCardButton(int x, int y, int width, ShopData.Product product, int quantity, Runnable add, Runnable decrement, Runnable increment, Runnable remove) {
        super(x, y, width, 88, (Component)Component.literal((String)product.name));
        this.product = product;
        this.displayStack = ProductDisplayStack.create(product);
        this.quantity = quantity;
        this.add = add;
        this.decrement = decrement;
        this.increment = increment;
        this.remove = remove;
        this.active = product.enabled && product.stock != 0;
    }

    public void onPress(InputWithModifiers input) {
        if (this.active) {
            this.add.run();
        }
    }

    public boolean mouseClicked(MouseButtonEvent event, boolean doubled) {
        double mouseX = event.x();
        double mouseY = event.y();
        if (!this.visible || event.button() != 0 || !this.isMouseOver(mouseX, mouseY)) {
            return false;
        }
        if (this.quantity > 0 && mouseX >= (double)this.removeX() && mouseX < (double)(this.removeX() + 14) && mouseY >= (double)(this.getY() + 3) && mouseY < (double)(this.getY() + 17)) {
            this.remove.run();
            this.playDownSound(Minecraft.getInstance().getSoundManager());
            return true;
        }
        if (this.quantity > 0 && mouseY >= (double)this.controlY() && mouseY < (double)(this.controlY() + 14)) {
            int index = this.controlAt(mouseX);
            if (index == 0) {
                this.decrement.run();
            } else if (index == 2 && this.active) {
                this.increment.run();
            } else if (index != 1) {
                return false;
            }
            this.playDownSound(Minecraft.getInstance().getSoundManager());
            return true;
        }
        if (!this.active) {
            return false;
        }
        this.playDownSound(Minecraft.getInstance().getSoundManager());
        this.onPress((InputWithModifiers)event);
        return true;
    }

    private int controlAt(double mouseX) {
        int cursor = this.controlX();
        for (int index = 0; index < CONTROL_WIDTHS.length; ++index) {
            if (mouseX >= (double)cursor && mouseX < (double)(cursor + CONTROL_WIDTHS[index])) {
                return index;
            }
            cursor += CONTROL_WIDTHS[index] + 2;
        }
        return -1;
    }

    protected void extractContents(GuiGraphicsExtractor graphics, int mouseX, int mouseY, float partialTick) {
        int stockY;
        Minecraft minecraft = Minecraft.getInstance();
        int fill = this.isHoveredOrFocused() && this.active ? ScreenThemes.current().SURFACE_HOVER : ScreenThemes.current().SURFACE;
        int border = this.quantity > 0 ? ScreenThemes.current().SELECTED_ACCENT : ScreenThemes.current().BORDER;
        graphics.fill(this.getX(), this.getY(), this.getX() + this.width, this.getY() + this.height, border);
        graphics.fill(this.getX() + 1, this.getY() + 1, this.getX() + this.width - 1, this.getY() + this.height - 1, fill);
        boolean selected = this.quantity > 0;
        int iconY = selected ? 3 : 9;
        int nameY = selected ? 29 : 37;
        int priceY = selected ? 42 : 51;
        int n = stockY = selected ? 56 : 68;
        if (!this.active) {
            this.renderUnavailable(graphics, this.displayStack, selected, iconY, nameY, mouseX, mouseY);
            return;
        }
        if (!this.displayStack.isEmpty()) {
            graphics.pose().pushMatrix();
            graphics.pose().translate((float)this.getX() + (float)this.width / 2.0f - 12.0f, (float)(this.getY() + iconY));
            graphics.pose().scale(1.5f, 1.5f);
            graphics.item(this.displayStack, 0, 0);
            graphics.pose().popMatrix();
        }
        String name = minecraft.font.plainSubstrByWidth(this.product.name, this.width - 10);
        GuiTextLayout.drawCentered(graphics, minecraft.font, (Component)Component.literal((String)name).withStyle(ChatFormatting.BOLD), this.getX() + this.width / 2, this.getY() + nameY, ScreenThemes.current().TEXT, !ScreenThemes.current().custom);
        this.renderPrice(graphics, priceY);
        MutableComponent stock = this.product.stock < 0 ? Component.translatable((String)"gui.avecoins.shop.stock_unlimited") : Component.translatable((String)"gui.avecoins.shop.stock_level", (Object[])new Object[]{Component.literal((String)(this.product.stock + "x")).withStyle(style -> style.withColor((this.product.stock <= 3 ? ScreenThemes.current().ERROR : (this.product.stock <= 10 ? ScreenThemes.current().SOFT_WARNING : ScreenThemes.current().MUTED)) & 0xFFFFFF))});
        GuiTextLayout.drawCentered(graphics, minecraft.font, (Component)stock, this.getX() + this.width / 2, this.getY() + stockY, ScreenThemes.current().MUTED, !ScreenThemes.current().custom);
        this.renderBadges(graphics);
        if (selected) {
            this.renderQuantityControls(graphics, mouseX, mouseY);
            this.renderRemoveControl(graphics, mouseX, mouseY);
        }
    }

    ItemStack displayStack() {
        return this.displayStack;
    }

    private void renderUnavailable(GuiGraphicsExtractor graphics, ItemStack displayStack, boolean selected, int iconY, int nameY, int mouseX, int mouseY) {
        Minecraft minecraft = Minecraft.getInstance();
        graphics.fill(this.getX() + 1, this.getY() + 1, this.getX() + this.width - 1, this.getY() + this.height - 1, ScreenThemes.current().UNAVAILABLE);
        if (!displayStack.isEmpty()) {
            graphics.pose().pushMatrix();
            graphics.pose().translate((float)this.getX() + (float)this.width / 2.0f - 12.0f, (float)(this.getY() + iconY));
            graphics.pose().scale(1.5f, 1.5f);
            graphics.item(displayStack, 0, 0);
            graphics.pose().popMatrix();
        }
        String name = minecraft.font.plainSubstrByWidth(this.product.name, this.width - 14);
        GuiTextLayout.drawCentered(graphics, minecraft.font, (Component)Component.literal((String)name).withStyle(ChatFormatting.BOLD), this.getX() + this.width / 2, this.getY() + nameY, ScreenThemes.current().TEXT, !ScreenThemes.current().custom);
        int statusY = this.getY() + (selected ? 43 : 53);
        int statusColor = this.product.stock == 0 ? ScreenThemes.current().DANGER : ScreenThemes.current().SURFACE_SELECTED;
        graphics.fill(this.getX() + 12, statusY, this.getX() + this.width - 12, statusY + 16, statusColor);
        GuiTextLayout.drawCentered(graphics, minecraft.font, (Component)Component.translatable((String)(this.product.stock == 0 ? "gui.avecoins.shop.sold_out" : "gui.avecoins.shop.unavailable")).withStyle(ChatFormatting.BOLD), this.getX() + this.width / 2, statusY + 4, ScreenThemes.current().TEXT, !ScreenThemes.current().custom);
        if (selected) {
            this.renderQuantityControls(graphics, mouseX, mouseY);
            this.renderRemoveControl(graphics, mouseX, mouseY);
        }
    }

    private void renderBadges(GuiGraphicsExtractor graphics) {
        int badgeX = this.getX() + 4;
        if (this.product.itemCount > 1) {
            badgeX += this.renderBadge(graphics, this.product.itemCount + "x", badgeX, ScreenThemes.current().SURFACE_SELECTED) + 3;
        }
        if (this.product.discountPrice != null) {
            int percent = this.product.price > 0 ? Math.round((float)(this.product.price - this.product.discountPrice) * 100.0f / (float)this.product.price) : 0;
            this.renderBadge(graphics, percent + "%", badgeX, ScreenThemes.current().SUCCESS_BOX);
        }
    }

    static void renderPreview(GuiGraphicsExtractor graphics, int x, int y, int width, ShopData.Product product, float scale) {
        graphics.pose().pushMatrix();
        graphics.pose().translate((float)x, (float)y);
        graphics.pose().scale(scale, scale);
        ProductCardButton card = new ProductCardButton(0, 0, width, product, 0, () -> {}, () -> {}, () -> {}, () -> {});
        card.extractContents(graphics, -1, -1, 0.0f);
        graphics.pose().popMatrix();
    }

    private int renderBadge(GuiGraphicsExtractor graphics, String label, int x, int color) {
        int badgeWidth = Minecraft.getInstance().font.width(label) + 6;
        graphics.fill(x, this.getY() + 4, x + badgeWidth, this.getY() + 17, color);
        GuiTextLayout.drawCentered(graphics, Minecraft.getInstance().font, label, x + badgeWidth / 2, this.getY() + 7, ScreenThemes.current().TEXT, !ScreenThemes.current().custom);
        return badgeWidth;
    }

    private void renderPrice(GuiGraphicsExtractor graphics, int yOffset) {
        MutableComponent price;
        ItemStack currency;
        Minecraft minecraft = Minecraft.getInstance();
        Identifier currencyId = Identifier.tryParse((String)this.product.currencyId);
        ItemStack itemStack = currency = currencyId != null && BuiltInRegistries.ITEM.containsKey(currencyId) ? new ItemStack((ItemLike)BuiltInRegistries.ITEM.getValue(currencyId)) : ItemStack.EMPTY;
        if (this.product.discountPrice != null) {
            MutableComponent regular = Component.literal((String)Integer.toString(this.product.price)).withStyle(style -> style.withColor(ScreenThemes.current().MUTED & 0xFFFFFF).withStrikethrough(Boolean.valueOf(true)));
            MutableComponent separator = Component.literal((String)"  \u2192  ").withStyle(style -> style.withColor(ScreenThemes.current().MUTED & 0xFFFFFF));
            MutableComponent discounted = Component.literal((String)Integer.toString(this.product.discountPrice)).withStyle(style -> style.withColor(ScreenThemes.current().DISCOUNT & 0xFFFFFF).withBold(Boolean.valueOf(true)));
            price = Component.empty().append((Component)regular).append((Component)separator).append((Component)discounted);
        } else {
            price = Component.literal((String)Integer.toString(this.product.price));
        }
        int textWidth = minecraft.font.width((FormattedText)price);
        int iconWidth = currency.isEmpty() ? 0 : 18;
        int groupX = this.getX() + (this.width - iconWidth - textWidth) / 2;
        if (!currency.isEmpty()) {
            graphics.item(currency, groupX, this.getY() + yOffset - 4);
        }
        graphics.text(minecraft.font, (Component)price, groupX + iconWidth, this.getY() + yOffset, ScreenThemes.current().TEXT, false);
    }

    private void renderQuantityControls(GuiGraphicsExtractor graphics, int mouseX, int mouseY) {
        String[] labels = new String[]{"\u2212", Integer.toString(this.quantity), "+"};
        int cursor = this.controlX();
        for (int index = 0; index < CONTROL_WIDTHS.length; ++index) {
            boolean hovered;
            boolean bl = hovered = mouseX >= cursor && mouseX < cursor + CONTROL_WIDTHS[index] && mouseY >= this.controlY() && mouseY < this.controlY() + 14;
            int background = index == 2 && !this.active ? ScreenThemes.current().SURFACE : (hovered ? ScreenThemes.current().SURFACE_SELECTED : ScreenThemes.current().HEADER_BUTTON);
            graphics.fill(cursor, this.controlY(), cursor + CONTROL_WIDTHS[index], this.controlY() + 14, background);
            int color = index == 2 && !this.active ? ScreenThemes.current().DISABLED : ScreenThemes.current().TEXT;
            GuiTextLayout.drawCentered(graphics, Minecraft.getInstance().font, labels[index], cursor + CONTROL_WIDTHS[index] / 2, this.controlY() + 4, color, !ScreenThemes.current().custom);
            cursor += CONTROL_WIDTHS[index] + 2;
        }
    }

    private void renderRemoveControl(GuiGraphicsExtractor graphics, int mouseX, int mouseY) {
        boolean hovered = mouseX >= this.removeX() && mouseX < this.removeX() + 14 && mouseY >= this.getY() + 3 && mouseY < this.getY() + 17;
        graphics.fill(this.removeX(), this.getY() + 3, this.removeX() + 14, this.getY() + 17, hovered ? ScreenThemes.current().DANGER_HOVER : ScreenThemes.current().DANGER);
        GuiTextLayout.drawCentered(graphics, Minecraft.getInstance().font, "\u00d7", this.removeX() + 7, this.getY() + 6, ScreenThemes.current().TEXT, !ScreenThemes.current().custom);
    }

    private int controlX() {
        int total = 0;
        for (int width : CONTROL_WIDTHS) {
            total += width;
        }
        return this.getX() + (this.getWidth() - (total += 2 * (CONTROL_WIDTHS.length - 1))) / 2;
    }

    private int controlY() {
        return this.getY() + this.getHeight() - 14 - 6;
    }

    private int removeX() {
        return this.getX() + this.getWidth() - 17;
    }

    protected void updateWidgetNarration(NarrationElementOutput output) {
        this.defaultButtonNarrationText(output);
    }
}

