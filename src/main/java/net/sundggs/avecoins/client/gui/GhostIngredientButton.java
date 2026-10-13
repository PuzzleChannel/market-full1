/*
 * Decompiled with CFR 0.152.
 */
package net.sundggs.avecoins.client.gui;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.components.AbstractButton;
import net.minecraft.client.gui.narration.NarrationElementOutput;
import net.minecraft.client.input.InputWithModifiers;
import net.minecraft.client.input.MouseButtonEvent;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.ItemLike;

final class GhostIngredientButton
extends AbstractButton {
    static final int SIZE = 38;
    private final String itemId;
    private final Runnable pressed;
    private final Runnable cleared;

    GhostIngredientButton(int x, int y, String itemId, Runnable pressed, Runnable cleared) {
        super(x, y, 38, 38, (Component)Component.translatable((String)"gui.avecoins.recipe.ghost_slot"));
        this.itemId = itemId == null ? "" : itemId;
        this.pressed = pressed;
        this.cleared = cleared;
    }

    public void onPress(InputWithModifiers input) {
        this.pressed.run();
    }

    public boolean mouseClicked(MouseButtonEvent event, boolean doubled) {
        if (event.button() == 1 && this.active && this.visible && this.isMouseOver(event.x(), event.y())) {
            this.cleared.run();
            return true;
        }
        return super.mouseClicked(event, doubled);
    }

    protected void extractContents(GuiGraphicsExtractor graphics, int mouseX, int mouseY, float partialTick) {
        int color = this.isHoveredOrFocused() ? -11906728 : -14670808;
        graphics.fill(this.getX(), this.getY(), this.getX() + this.width, this.getY() + this.height, -10656661);
        graphics.fill(this.getX() + 2, this.getY() + 2, this.getX() + this.width - 2, this.getY() + this.height - 2, color);
        if (!this.itemId.isEmpty()) {
            Identifier id = Identifier.tryParse((String)this.itemId);
            if (id != null && BuiltInRegistries.ITEM.containsKey(id)) {
                graphics.item(new ItemStack((ItemLike)BuiltInRegistries.ITEM.getValue(id)), this.getX() + 11, this.getY() + 11);
            }
        } else {
            graphics.centeredText(Minecraft.getInstance().font, "\u00b7", this.getX() + this.width / 2, this.getY() + 15, -6183253);
        }
    }

    protected void updateWidgetNarration(NarrationElementOutput output) {
        this.defaultButtonNarrationText(output);
    }
}

