/*
 * Decompiled with CFR 0.152.
 */
package net.sundggs.avecoins.client.gui;

import java.util.List;
import java.util.Locale;
import java.util.function.Consumer;
import net.minecraft.ChatFormatting;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.gui.components.events.GuiEventListener;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.input.KeyEvent;
import net.minecraft.client.input.MouseButtonEvent;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.FormattedText;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.resources.Identifier;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.ItemLike;
import net.sundggs.avecoins.client.gui.AutocompleteField;
import net.sundggs.avecoins.client.gui.AvecoinsScreen;
import net.sundggs.avecoins.client.gui.PanelButton;
import net.sundggs.avecoins.client.gui.RecipeEditorScreen;

final class IngredientSelectorScreen
extends AvecoinsScreen {
    private static final int MAX_SUGGESTIONS = 4;
    private static final int SUGGESTION_HEIGHT = 17;
    private final RecipeEditorScreen parent;
    private final Consumer<String> selected;
    private final String initialId;
    private EditBox idBox;
    private ItemStack preview = ItemStack.EMPTY;
    private List<String> allItemIds = List.of();
    private List<String> suggestions = List.of();
    private String resultCode = "";

    IngredientSelectorScreen(RecipeEditorScreen parent, String initialId, Consumer<String> selected) {
        super((Component)Component.translatable((String)"gui.avecoins.recipe.selector.title"));
        this.parent = parent;
        this.initialId = initialId == null ? "" : initialId;
        this.selected = selected;
    }

    protected void init() {
        this.layoutPanel(330, 300);
        this.addHeaderBackButton(this.parent);
        this.allItemIds = BuiltInRegistries.ITEM.keySet().stream().filter(id -> BuiltInRegistries.ITEM.getValue(id) != Items.AIR).map(Identifier::toString).sorted().toList();
        int fieldWidth = 190;
        this.idBox = new EditBox(this.font, this.panelLeft + (this.panelWidth - fieldWidth) / 2, this.panelTop + 127, fieldWidth, 20, (Component)Component.translatable((String)"gui.avecoins.recipe.selector.id"));
        this.idBox.setMaxLength(128);
        this.idBox.setHint((Component)Component.translatable((String)"gui.avecoins.recipe.selector.hint"));
        this.idBox.setValue(this.initialId);
        this.idBox.setResponder(this::updateSearch);
        this.addRenderableWidget((GuiEventListener)this.idBox);
        this.updateSearch(this.initialId);
        this.setInitialFocus((GuiEventListener)this.idBox);
        this.addRenderableWidget((GuiEventListener)new PanelButton(this.panelLeft + (this.panelWidth - 110) / 2, this.panelTop + 238, 110, 22, (Component)Component.translatable((String)"gui.avecoins.recipe.selector.confirm"), PanelButton.Variant.PRIMARY, button -> this.confirmSelection()));
    }

    private void updateSearch(String value) {
        String query = value.trim().toLowerCase(Locale.ROOT);
        this.updatePreview(query);
        if (query.isEmpty() || IngredientSelectorScreen.isRegisteredItem(query)) {
            this.suggestions = List.of();
            return;
        }
        this.suggestions = AutocompleteField.match(this.allItemIds, query, 4);
    }

    private void updatePreview(String value) {
        Identifier id = Identifier.tryParse((String)value);
        if (id == null || !BuiltInRegistries.ITEM.containsKey(id)) {
            this.preview = ItemStack.EMPTY;
            return;
        }
        Item item = (Item)BuiltInRegistries.ITEM.getValue(id);
        this.preview = item == Items.AIR ? ItemStack.EMPTY : new ItemStack((ItemLike)item);
        this.resultCode = "";
    }

    private static boolean isRegisteredItem(String value) {
        Identifier id = Identifier.tryParse((String)value);
        return id != null && BuiltInRegistries.ITEM.containsKey(id) && BuiltInRegistries.ITEM.getValue(id) != Items.AIR;
    }

    private void applySuggestion(String id) {
        this.idBox.setValue(id);
        this.suggestions = List.of();
    }

    private void confirmSelection() {
        if (this.preview.isEmpty()) {
            this.resultCode = "invalid_item_id";
            return;
        }
        Identifier id = BuiltInRegistries.ITEM.getKey((Object)this.preview.getItem());
        this.selected.accept(id.toString());
        this.minecraft.gui.setScreen((Screen)this.parent);
    }

    @Override
    public boolean mouseClicked(MouseButtonEvent event, boolean doubled) {
        double scaledMouseX = this.virtualMouseX(event.x());
        double scaledMouseY = this.virtualMouseY(event.y());
        if (event.button() == 0 && !this.suggestions.isEmpty()) {
            int suggestionY = this.panelTop + 150;
            for (int index = 0; index < this.suggestions.size(); ++index) {
                int rowY = suggestionY + index * 17;
                if (!(scaledMouseX >= (double)this.idBox.getX()) || !(scaledMouseX < (double)(this.idBox.getX() + this.idBox.getWidth())) || !(scaledMouseY >= (double)rowY) || !(scaledMouseY < (double)(rowY + 17))) continue;
                this.applySuggestion(this.suggestions.get(index));
                return true;
            }
        }
        return super.mouseClicked(event, doubled);
    }

    public boolean keyPressed(KeyEvent event) {
        if (event.key() == 258 && this.idBox.isFocused() && !this.suggestions.isEmpty()) {
            this.applySuggestion(this.suggestions.getFirst());
            return true;
        }
        return super.keyPressed(event);
    }

    @Override
    protected void renderPanel(GuiGraphicsExtractor graphics, int mouseX, int mouseY, float partialTick) {
        this.renderHeader(graphics, null);
        this.drawSectionTitle(graphics, (Component)Component.translatable((String)"gui.avecoins.recipe.selector.section"), this.panelTop + 51);
        this.drawCenteredWrapped(graphics, (Component)Component.translatable((String)"gui.avecoins.recipe.selector.subtitle"), this.panelTop + 70, 230, -6183253);
        MutableComponent idLabel = Component.literal((String)"# ").withStyle(ChatFormatting.GRAY).append((Component)Component.translatable((String)"gui.avecoins.recipe.selector.id").withStyle(new ChatFormatting[]{ChatFormatting.WHITE, ChatFormatting.BOLD}));
        graphics.text(this.font, (Component)idLabel, this.idBox.getX(), this.panelTop + 109, -1710103);
        this.divider(graphics, this.panelTop + 163, 32);
        this.drawSectionTitle(graphics, (Component)Component.translatable((String)"gui.avecoins.recipe.selector.preview"), this.panelTop + 181);
        if (!this.preview.isEmpty()) {
            Component previewName = this.preview.getHoverName();
            int groupWidth = 22 + this.font.width((FormattedText)previewName);
            int groupX = this.panelLeft + (this.panelWidth - groupWidth) / 2;
            graphics.item(this.preview, groupX, this.panelTop + 201);
            graphics.text(this.font, previewName, groupX + 22, this.panelTop + 205, -1710103);
        } else {
            graphics.centeredText(this.font, (Component)Component.translatable((String)"gui.avecoins.recipe.selector.no_preview"), this.panelLeft + this.panelWidth / 2, this.panelTop + 205, -6183253);
        }
        this.renderSuggestions(graphics, mouseX, mouseY);
        this.renderStatusBox(graphics, this.resultCode);
    }

    private void renderSuggestions(GuiGraphicsExtractor graphics, int mouseX, int mouseY) {
        int suggestionY = this.panelTop + 150;
        if (this.suggestions.isEmpty()) {
            return;
        }
        graphics.pose().pushMatrix();
        graphics.fill(this.idBox.getX() - 1, suggestionY - 1, this.idBox.getX() + this.idBox.getWidth() + 1, suggestionY + this.suggestions.size() * 17 + 1, -10656661);
        for (int index = 0; index < this.suggestions.size(); ++index) {
            String idValue = this.suggestions.get(index);
            int rowY = suggestionY + index * 17;
            boolean hovered = mouseX >= this.idBox.getX() && mouseX < this.idBox.getX() + this.idBox.getWidth() && mouseY >= rowY && mouseY < rowY + 17;
            graphics.fill(this.idBox.getX(), rowY, this.idBox.getX() + this.idBox.getWidth(), rowY + 17, hovered ? -12959674 : -14670808);
            if (index < this.suggestions.size() - 1) {
                graphics.fill(this.idBox.getX(), rowY + 17 - 1, this.idBox.getX() + this.idBox.getWidth(), rowY + 17, -10656661);
            }
            Identifier id = Identifier.parse((String)idValue);
            graphics.item(new ItemStack((ItemLike)BuiltInRegistries.ITEM.getValue(id)), this.idBox.getX() + 1, rowY);
            String visibleId = this.font.plainSubstrByWidth(idValue, this.idBox.getWidth() - 26);
            graphics.text(this.font, visibleId, this.idBox.getX() + 20, rowY + 4, -1710103);
        }
        graphics.pose().popMatrix();
    }

    public void onClose() {
        this.minecraft.gui.setScreen((Screen)this.parent);
    }
}

