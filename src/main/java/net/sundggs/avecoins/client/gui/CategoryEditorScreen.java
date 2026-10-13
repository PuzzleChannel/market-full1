/*
 * Decompiled with CFR 0.152.
 */
package net.sundggs.avecoins.client.gui;

import java.util.List;
import net.minecraft.ChatFormatting;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.gui.components.events.GuiEventListener;
import net.minecraft.client.input.KeyEvent;
import net.minecraft.client.input.MouseButtonEvent;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.ItemLike;
import net.sundggs.avecoins.client.gui.AdminShopScreen;
import net.sundggs.avecoins.client.gui.AutocompleteField;
import net.sundggs.avecoins.client.gui.AvecoinsScreen;
import net.sundggs.avecoins.client.gui.PanelButton;
import net.sundggs.avecoins.config.AtomicJsonStore;
import net.sundggs.avecoins.network.ClientNetworking;
import net.sundggs.avecoins.network.ServerboundAvecoinsPayload;
import net.sundggs.avecoins.shop.ShopData;
import net.sundggs.avecoins.shop.ShopService;

public final class CategoryEditorScreen
extends AvecoinsScreen {
    private final AdminShopScreen parent;
    private final ShopData.Category existing;
    private EditBox idBox;
    private EditBox nameBox;
    private EditBox descriptionBox;
    private EditBox iconBox;
    private AutocompleteField iconAutocomplete;
    private String validationKey = "";

    public CategoryEditorScreen(AdminShopScreen parent, ShopService.ShopSnapshot snapshot, ShopData.Category existing) {
        super((Component)Component.translatable((String)(existing == null ? "gui.avecoins.category.new_title" : "gui.avecoins.category.edit_title")));
        this.parent = parent;
        this.useShopTheme(snapshot);
        this.existing = existing;
    }

    public AdminShopScreen refreshedParent(ShopService.ShopSnapshot snapshot, String result) {
        return new AdminShopScreen(snapshot, result, this.parent.selectedCategoryId(), this.parent.categoryPage(), this.parent.productPage());
    }

    protected void init() {
        this.layoutPanel(500, 390);
        this.addHeaderBackButton(this.parent);
        int left = this.panelLeft + 60;
        int right = this.panelLeft + 260;
        int columnWidth = 180;
        this.idBox = this.field(left, this.panelTop + 94, columnWidth, 48, this.existing == null ? "" : this.existing.id);
        if (this.existing != null) {
            this.idBox.setEditable(false);
        }
        this.nameBox = this.field(right, this.panelTop + 94, columnWidth, 48, this.existing == null ? "" : this.existing.name);
        this.descriptionBox = this.field(left, this.panelTop + 148, 380, 160, this.existing == null ? "" : this.existing.description);
        int iconX = this.panelLeft + 115;
        this.iconAutocomplete = new AutocompleteField(this.font, iconX, this.panelTop + 202, columnWidth, 128, this.existing == null ? "" : this.existing.iconItem, CategoryEditorScreen.itemIds(), AutocompleteField::registeredItem, value -> {});
        this.iconBox = this.iconAutocomplete.box();
        this.addRenderableWidget(this.iconBox);
        this.addRenderableWidget(new PanelButton(iconX + columnWidth + 6, this.panelTop + 202, 84, 20, (Component)Component.translatable((String)"gui.avecoins.use_held_item_short"), PanelButton.Variant.HEADER, button -> this.useHeldIcon()));
        this.addRenderableWidget(new PanelButton(this.panelLeft + this.panelWidth / 2 - 74, this.panelTop + 330, 148, 22, (Component)Component.translatable((String)"gui.avecoins.save"), PanelButton.Variant.SELECTED, button -> this.save()));
    }

    private EditBox field(int x, int y, int width, int max, String value) {
        EditBox box = new EditBox(this.font, x, y, width, 20, (Component)Component.empty());
        box.setMaxLength(max);
        box.setValue(value);
        this.addRenderableWidget(box);
        return box;
    }

    private static List<String> itemIds() {
        return BuiltInRegistries.ITEM.keySet().stream().filter(id -> BuiltInRegistries.ITEM.getValue(id) != Items.AIR).map(Identifier::toString).sorted().toList();
    }

    private void useHeldIcon() {
        if (this.minecraft.player != null && !this.minecraft.player.getMainHandItem().isEmpty()) {
            this.iconAutocomplete.setValue(BuiltInRegistries.ITEM.getKey(this.minecraft.player.getMainHandItem().getItem()).toString());
        }
    }

    @Override
    public boolean mouseClicked(MouseButtonEvent event, boolean doubled) {
        if (this.iconAutocomplete.mouseClicked(this.virtualMouseX(event.x()), this.virtualMouseY(event.y()), event.button())) {
            return true;
        }
        return super.mouseClicked(event, doubled);
    }

    public boolean keyPressed(KeyEvent event) {
        if (this.iconAutocomplete.keyPressed(event.key())) {
            return true;
        }
        return super.keyPressed(event);
    }

    private void save() {
        if (!this.idBox.getValue().matches("[a-z0-9_-]{1,48}")) {
            this.validationKey = "gui.avecoins.editor.invalid_id";
            return;
        }
        if (this.nameBox.getValue().isBlank()) {
            this.validationKey = "gui.avecoins.editor.invalid_name";
            return;
        }
        String iconValue = this.iconBox.getValue().isBlank() ? "minecraft:chest" : this.iconBox.getValue();
        Identifier iconId = Identifier.tryParse((String)iconValue);
        if (iconId == null || !BuiltInRegistries.ITEM.containsKey(iconId)) {
            this.validationKey = "gui.avecoins.editor.invalid_item";
            return;
        }
        ShopData.Category category = new ShopData.Category();
        category.id = this.idBox.getValue();
        category.name = this.nameBox.getValue();
        category.description = this.descriptionBox.getValue();
        category.iconItem = iconValue;
        category.sortOrder = this.existing == null ? 0 : this.existing.sortOrder;
        ClientNetworking.send(new ServerboundAvecoinsPayload("save_category", AtomicJsonStore.gson().toJson(category)));
    }

    @Override
    protected void renderPanel(GuiGraphicsExtractor graphics, int mouseX, int mouseY, float partialTick) {
        this.renderHeader(graphics, (Component)Component.translatable((String)"gui.avecoins.category.subtitle"));
        this.divider(graphics, this.panelTop + 68, 28);
        int left = this.panelLeft + 60;
        int right = this.panelLeft + 260;
        int iconX = this.panelLeft + 115;
        this.label(graphics, "#", "gui.avecoins.category.id", left, this.panelTop + 81);
        this.label(graphics, "\u270e", "gui.avecoins.category.name", right, this.panelTop + 81);
        this.label(graphics, "\u2261", "gui.avecoins.category.description", left, this.panelTop + 135);
        this.label(graphics, "\u25c6", "gui.avecoins.category.icon", iconX, this.panelTop + 189);
        this.drawScaledText(graphics, (Component)Component.translatable((String)"gui.avecoins.category.id_example"), left, this.panelTop + 116, 0.8f, this.theme().MUTED);
        this.drawScaledText(graphics, (Component)Component.translatable((String)"gui.avecoins.category.name_example"), right, this.panelTop + 116, 0.8f, this.theme().MUTED);
        this.drawScaledText(graphics, (Component)Component.translatable((String)"gui.avecoins.category.description_example"), left, this.panelTop + 170, 0.8f, this.theme().MUTED);
        this.drawScaledText(graphics, (Component)Component.translatable((String)"gui.avecoins.category.icon_example"), iconX, this.panelTop + 224, 0.8f, this.theme().MUTED);
        this.divider(graphics, this.panelTop + 250, 28);
        this.drawSectionTitle(graphics, (Component)Component.translatable((String)"gui.avecoins.category.preview"), this.panelTop + 270);
        Identifier icon = Identifier.tryParse((String)(this.iconBox.getValue().isBlank() ? "minecraft:chest" : this.iconBox.getValue()));
        String previewName = this.nameBox.getValue().isBlank() ? Component.translatable((String)"gui.avecoins.category.unnamed").getString() : this.nameBox.getValue();
        previewName = this.font.plainSubstrByWidth(previewName, 300);
        int groupWidth = 22 + this.font.width(previewName);
        int groupX = this.panelLeft + (this.panelWidth - groupWidth) / 2;
        if (icon != null && BuiltInRegistries.ITEM.containsKey(icon)) {
            graphics.item(new ItemStack((ItemLike)BuiltInRegistries.ITEM.getValue(icon)), groupX, this.panelTop + 296);
        }
        graphics.text(this.font, previewName, groupX + 22, this.panelTop + 301, this.theme().MUTED, !this.theme().custom);
        if (!this.validationKey.isBlank()) {
            this.renderFooterBox(graphics, (Component)Component.translatable((String)this.validationKey), this.theme().DANGER, this.theme().TEXT);
        }
        this.iconAutocomplete.render(graphics, mouseX, mouseY);
    }

    private void label(GuiGraphicsExtractor graphics, String icon, String key, int x, int y) {
        graphics.text(this.font, (Component)Component.literal((String)(icon + " ")).append((Component)Component.translatable((String)key)).withStyle(ChatFormatting.BOLD), x, y, this.theme().TEXT, !this.theme().custom);
    }
}

