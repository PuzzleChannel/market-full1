/*
 * Decompiled with CFR 0.152.
 */
package net.sundggs.avecoins.client.gui;

import java.util.Comparator;
import java.util.List;
import java.util.Locale;
import java.util.function.Consumer;
import java.util.function.Function;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.ItemLike;
import net.sundggs.avecoins.client.gui.ScreenThemes;

final class AutocompleteField {
    private static final int MAX_SUGGESTIONS = 4;
    private static final int ROW_HEIGHT = 17;
    private final Font font;
    private final EditBox box;
    private final List<String> candidates;
    private final Function<String, ItemStack> iconResolver;
    private final Consumer<String> changed;
    private List<String> suggestions = List.of();

    AutocompleteField(Font font, int x, int y, int width, int maxLength, String value, List<String> candidates, Function<String, ItemStack> iconResolver, Consumer<String> changed) {
        this.font = font;
        this.candidates = candidates;
        this.iconResolver = iconResolver;
        this.changed = changed;
        this.box = new EditBox(font, x, y, width, 20, (Component)Component.empty());
        this.box.setMaxLength(maxLength);
        this.box.setValue(value);
        this.box.setResponder(this::update);
        this.update(value);
    }

    EditBox box() {
        return this.box;
    }

    void setValue(String value) {
        this.box.setValue(value);
        this.suggestions = List.of();
    }

    boolean mouseClicked(double mouseX, double mouseY, int button) {
        if (button != 0 || !this.box.isFocused() || this.suggestions.isEmpty()) {
            return false;
        }
        int top = this.suggestionTop();
        for (int index = 0; index < this.suggestions.size(); ++index) {
            int rowY = top + index * 17;
            if (!(mouseX >= (double)this.box.getX()) || !(mouseX < (double)(this.box.getX() + this.box.getWidth())) || !(mouseY >= (double)rowY) || !(mouseY < (double)(rowY + 17))) continue;
            this.apply(this.suggestions.get(index));
            return true;
        }
        return false;
    }

    boolean keyPressed(int keyCode) {
        if (keyCode == 258 && this.box.isFocused() && !this.suggestions.isEmpty()) {
            this.apply(this.suggestions.getFirst());
            return true;
        }
        return false;
    }

    void render(GuiGraphicsExtractor graphics, int mouseX, int mouseY) {
        if (!this.box.isFocused() || this.suggestions.isEmpty()) {
            return;
        }
        int top = this.suggestionTop();
        graphics.pose().pushMatrix();
        graphics.fill(this.box.getX() - 1, top - 1, this.box.getX() + this.box.getWidth() + 1, top + this.suggestions.size() * 17 + 1, ScreenThemes.current().BORDER);
        for (int index = 0; index < this.suggestions.size(); ++index) {
            String value = this.suggestions.get(index);
            int rowY = top + index * 17;
            boolean hovered = mouseX >= this.box.getX() && mouseX < this.box.getX() + this.box.getWidth() && mouseY >= rowY && mouseY < rowY + 17;
            graphics.fill(this.box.getX(), rowY, this.box.getX() + this.box.getWidth(), rowY + 17, hovered ? ScreenThemes.current().SURFACE_HOVER : ScreenThemes.current().SURFACE);
            if (index + 1 < this.suggestions.size()) {
                graphics.fill(this.box.getX(), rowY + 17 - 1, this.box.getX() + this.box.getWidth(), rowY + 17, ScreenThemes.current().DIVIDER);
            }
            ItemStack icon = this.iconResolver.apply(value);
            int textX = this.box.getX() + 4;
            if (!icon.isEmpty()) {
                graphics.item(icon, this.box.getX() + 1, rowY);
                textX += 18;
            }
            graphics.text(this.font, this.font.plainSubstrByWidth(value, this.box.getX() + this.box.getWidth() - textX - 3), textX, rowY + 4, ScreenThemes.current().TEXT, false);
        }
        graphics.pose().popMatrix();
    }

    static List<String> match(List<String> candidates, String value, int limit) {
        String query;
        block3: {
            block2: {
                query = value.trim().toLowerCase(Locale.ROOT);
                if (query.isEmpty()) break block2;
                if (!candidates.stream().anyMatch(query::equals)) break block3;
            }
            return List.of();
        }
        return candidates.stream().filter(candidate -> candidate.contains(query)).sorted(Comparator.comparing((String candidate) -> !AutocompleteField.preferred(candidate, query)).thenComparing(Function.identity())).limit(limit).toList();
    }

    static ItemStack registeredItem(String value) {
        Identifier id = Identifier.tryParse((String)value);
        if (id == null || !BuiltInRegistries.ITEM.containsKey(id)) {
            return ItemStack.EMPTY;
        }
        return new ItemStack((ItemLike)BuiltInRegistries.ITEM.getValue(id));
    }

    private void update(String value) {
        this.suggestions = AutocompleteField.match(this.candidates, value, 4);
        this.changed.accept(value);
    }

    private void apply(String value) {
        this.box.setValue(value);
        this.suggestions = List.of();
    }

    private int suggestionTop() {
        return this.box.getY() + this.box.getHeight() + 1;
    }

    private static boolean preferred(String value, String query) {
        int separator = value.indexOf(58);
        String path = separator >= 0 ? value.substring(separator + 1) : value;
        return value.startsWith(query) || path.startsWith(query);
    }
}

