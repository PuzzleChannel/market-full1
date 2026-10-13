/*
 * Decompiled with CFR 0.152.
 */
package net.sundggs.avecoins.client.gui;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import net.minecraft.ChatFormatting;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.gui.components.Tooltip;
import net.minecraft.client.gui.components.events.GuiEventListener;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.sundggs.avecoins.client.gui.AvecoinsScreen;
import net.sundggs.avecoins.client.gui.DashboardScreen;
import net.sundggs.avecoins.client.gui.GuiPalette;
import net.sundggs.avecoins.client.gui.PanelButton;
import net.sundggs.avecoins.client.gui.ShopSettingsLayout;
import net.sundggs.avecoins.config.AtomicJsonStore;
import net.sundggs.avecoins.config.GeneralConfig;
import net.sundggs.avecoins.config.ShopColor;
import net.sundggs.avecoins.network.ClientNetworking;
import net.sundggs.avecoins.network.ServerboundAvecoinsPayload;

public final class ShopSettingsScreen
extends AvecoinsScreen {
    private final JsonObject dashboardSnapshot;
    private final GeneralConfig config;
    private String resultCode;
    private String commandDraft;
    private String shopNameDraft;
    private EditBox commandBox;
    private EditBox nameBox;
    private EditBox colorBox;
    private PanelButton saveButton;
    private String colorDraft;
    private GuiPalette previewPalette;

    public ShopSettingsScreen(JsonObject dashboardSnapshot) {
        this(dashboardSnapshot, "");
    }

    public ShopSettingsScreen(JsonObject dashboardSnapshot, String resultCode) {
        super((Component)Component.translatable((String)"gui.avecoins.shop_settings.title"));
        this.dashboardSnapshot = dashboardSnapshot;
        this.config = (GeneralConfig)AtomicJsonStore.gson().fromJson((JsonElement)dashboardSnapshot.getAsJsonObject("general"), GeneralConfig.class);
        this.resultCode = resultCode;
        this.commandDraft = ShopSettingsScreen.displayCommand(this.config.customShopCommand);
        this.shopNameDraft = "AVESHOP".equals(this.config.shopName) ? "" : this.config.shopName;
        this.colorDraft = ShopColor.normalize(this.config.shopColor);
        this.previewPalette = GuiPalette.fromHex(this.colorDraft);
    }

    protected void init() {
        this.layoutPanel(440, 532);
        this.addHeaderBackButton(new DashboardScreen(this.dashboardSnapshot, ""));
        int masterWidth = 86;
        this.addRenderableWidget((GuiEventListener)new PanelButton(this.panelLeft + this.panelWidth - masterWidth - 96, this.panelTop + 129, masterWidth, 20, this.masterToggleLabel(), PanelButton.Variant.PRIMARY, this.toggleColor(), button -> {
            this.config.shopEnabled = !this.config.shopEnabled;
            this.rebuildWidgets();
        }));
        this.addRenderableWidget((GuiEventListener)new PanelButton(this.panelLeft + this.panelWidth - masterWidth - 96, this.panelTop + 196, masterWidth, 20, this.conversionToggleLabel(), PanelButton.Variant.PRIMARY, this.conversionToggleColor(), button -> {
            this.config.automaticCoinConversion = !this.config.automaticCoinConversion;
            this.rebuildWidgets();
        }));
        int fieldX = this.panelLeft + 38;
        int fieldWidth = 150;
        this.commandBox = this.field(fieldX, this.panelTop + 274, fieldWidth, 33, this.commandDraft);
        this.commandBox.setResponder(value -> {
            this.commandDraft = value;
        });
        this.nameBox = this.field(fieldX, this.panelTop + 338, fieldWidth, 48, this.shopNameDraft);
        this.nameBox.setResponder(value -> {
            this.shopNameDraft = value;
        });
        this.colorBox = new EditBox(this.font, fieldX, this.panelTop + 402, 84, 20, (Component)Component.translatable((String)"gui.avecoins.shop_settings.color"));
        this.colorBox.setMaxLength(32);
        this.colorBox.setValue(this.colorDraft);
        this.colorBox.setResponder(this::updateColorDraft);
        this.addRenderableWidget((GuiEventListener)this.colorBox);
        PanelButton resetColor = new PanelButton(fieldX + 90, this.panelTop + 402, 60, 20, (Component)Component.translatable((String)"gui.avecoins.shop_settings.color_reset_short"), PanelButton.Variant.SECONDARY, button -> this.colorBox.setValue(""));
        resetColor.setTooltip(Tooltip.create((Component)Component.translatable((String)"gui.avecoins.shop_settings.color_reset")));
        this.addRenderableWidget((GuiEventListener)resetColor);
        this.saveButton = (PanelButton)this.addRenderableWidget((GuiEventListener)new PanelButton(this.panelLeft + (this.panelWidth - 140) / 2, this.panelTop + 464, 140, 22, (Component)Component.translatable((String)"gui.avecoins.save"), PanelButton.Variant.PRIMARY, button -> this.save()));
        this.saveButton.active = ShopColor.isValid(this.colorDraft);
    }

    private void updateColorDraft(String value) {
        this.colorDraft = value;
        boolean valid = ShopColor.isValid(value);
        if (valid) {
            this.previewPalette = GuiPalette.fromHex(value);
        }
        if (this.saveButton != null) {
            this.saveButton.active = valid;
        }
    }

    private EditBox field(int x, int y, int width, int maxLength, String value) {
        EditBox box = new EditBox(this.font, x, y, width, 20, (Component)Component.empty());
        box.setMaxLength(maxLength);
        box.setValue(value);
        this.addRenderableWidget((GuiEventListener)box);
        return box;
    }

    private void save() {
        if (!ShopColor.isValid(this.colorDraft)) {
            return;
        }
        this.config.customShopCommand = ShopSettingsScreen.normalizeCommand(this.commandBox.getValue());
        this.config.shopName = this.nameBox.getValue();
        this.config.shopColor = ShopColor.normalize(this.colorDraft);
        ClientNetworking.send(new ServerboundAvecoinsPayload("save_general", AtomicJsonStore.gson().toJson((Object)this.config)));
    }

    private static String displayCommand(String command) {
        return command == null || command.isBlank() ? "" : "/" + command;
    }

    private static String normalizeCommand(String command) {
        String normalized = command == null ? "" : command.trim();
        return normalized.startsWith("/") ? normalized.substring(1).trim() : normalized;
    }

    @Override
    protected void renderPanel(GuiGraphicsExtractor graphics, int mouseX, int mouseY, float partialTick) {
        this.renderHeader(graphics, null);
        this.drawSectionTitle(graphics, (Component)Component.translatable((String)"gui.avecoins.shop_settings.configuration"), this.panelTop + 47);
        this.drawCenteredWrapped(graphics, (Component)Component.translatable((String)"gui.avecoins.shop_settings.subtitle"), this.panelTop + 62, 220, -6183253);
        this.divider(graphics, this.panelTop + 97, 26);
        int settingsCenterX = this.panelLeft + 140;
        this.drawSectionTitleAt(graphics, (Component)Component.translatable((String)"gui.avecoins.shop_settings.master"), settingsCenterX, this.panelTop + 117);
        this.drawCenteredWrappedAt(graphics, (Component)Component.translatable((String)"gui.avecoins.shop_settings.master_help"), settingsCenterX, this.panelTop + 133, 220, -6183253);
        this.drawSectionTitleAt(graphics, (Component)Component.translatable((String)"gui.avecoins.shop_settings.auto_conversion"), settingsCenterX, this.panelTop + 184);
        this.drawCenteredWrappedAt(graphics, (Component)Component.translatable((String)"gui.avecoins.shop_settings.auto_conversion_help"), settingsCenterX, this.panelTop + 200, 220, -6183253);
        this.divider(graphics, this.panelTop + 239, 26);
        int fieldX = this.panelLeft + 38;
        int descriptionX = this.panelLeft + 225;
        this.fieldLabel(graphics, "/", "gui.avecoins.shop_settings.command", fieldX, this.panelTop + 258);
        this.drawScaledText(graphics, (Component)Component.translatable((String)"gui.avecoins.shop_settings.command_example"), fieldX, this.panelTop + 298, 0.7f, -6183253);
        this.drawScaledWrapped(graphics, (Component)Component.translatable((String)"gui.avecoins.shop_settings.command_note"), descriptionX, this.panelTop + 258, 176, 0.9f, -6183253);
        this.fieldLabel(graphics, "\u270e", "gui.avecoins.shop_settings.name", fieldX, this.panelTop + 322);
        this.drawScaledText(graphics, (Component)Component.translatable((String)"gui.avecoins.shop_settings.name_example"), fieldX, this.panelTop + 362, 0.7f, -6183253);
        this.drawScaledWrapped(graphics, (Component)Component.translatable((String)"gui.avecoins.shop_settings.name_help"), descriptionX, this.panelTop + 322, 176, 0.9f, -6183253);
        this.fieldLabel(graphics, "#", "gui.avecoins.shop_settings.color", fieldX, this.panelTop + 386);
        int descriptionBottom = this.drawScaledWrapped(graphics, (Component)Component.translatable((String)(ShopColor.isValid(this.colorDraft) ? "gui.avecoins.shop_settings.color_help" : "gui.avecoins.shop_settings.color_invalid")), fieldX, this.panelTop + 430, 150, 0.75f, ShopColor.isValid(this.colorDraft) ? -6183253 : -3044212);
        int previewTop = this.panelTop + ShopSettingsLayout.previewTop(descriptionBottom - this.panelTop - 3);
        this.renderColorPreview(graphics, descriptionX, previewTop, 54);
        if (!this.resultCode.isBlank()) {
            this.renderStatusBox(graphics, this.resultCode);
        } else if (this.dashboardSnapshot.get("aliasRestartRequired").getAsBoolean()) {
            this.renderFooterBox(graphics, (Component)Component.translatable((String)"gui.avecoins.shop_settings.restart_pending"), -11844312, -1718678);
        }
    }

    private void renderColorPreview(GuiGraphicsExtractor graphics, int x, int y, int height) {
        GuiPalette palette = this.previewPalette;
        graphics.fill(x, y, x + 176, y + height, palette.BORDER);
        graphics.fill(x + 1, y + 1, x + 175, y + height - 1, palette.PANEL);
        graphics.fill(x + 1, y + 1, x + 175, y + 18, palette.HEADER);
        this.drawScaledText(graphics, (Component)Component.literal((String)this.font.plainSubstrByWidth(this.shopNameDraft.isBlank() ? "AVESHOP" : this.shopNameDraft, 218)), x + 6, y + 5, 0.75f, palette.TEXT);
        graphics.fill(x + 8, y + 23, x + 84, y + 41, palette.SURFACE);
        graphics.fill(x + 92, y + 23, x + 168, y + 41, palette.SURFACE_SELECTED);
        graphics.fill(x + 92, y + 23, x + 94, y + 41, palette.SELECTED_ACCENT);
        this.drawScaledText(graphics, (Component)Component.translatable((String)"gui.avecoins.shop_settings.color_normal"), x + 14, y + 28, 0.75f, palette.TEXT);
        this.drawScaledText(graphics, (Component)Component.translatable((String)"gui.avecoins.shop_settings.color_selected"), x + 100, y + 28, 0.75f, palette.TEXT);
        this.drawScaledText(graphics, (Component)Component.translatable((String)"gui.avecoins.shop_settings.color_sample"), x + 8, y + height - 9, 0.65f, palette.MUTED);
    }

    private void fieldLabel(GuiGraphicsExtractor graphics, String icon, String translationKey, int x, int y) {
        MutableComponent label = Component.literal((String)(icon + " ")).withStyle(ChatFormatting.GRAY).append((Component)Component.translatable((String)translationKey).withStyle(new ChatFormatting[]{ChatFormatting.WHITE, ChatFormatting.BOLD}));
        graphics.text(this.font, (Component)label, x, y, -1710103);
    }

    private Component masterToggleLabel() {
        return Component.literal((String)(this.config.shopEnabled ? "\u2713 " : "\u2715 ")).append((Component)Component.translatable((String)(this.config.shopEnabled ? "gui.avecoins.enabled" : "gui.avecoins.disabled")));
    }

    private int toggleColor() {
        return this.config.shopEnabled ? -6307928 : -3044212;
    }

    private Component conversionToggleLabel() {
        return Component.literal((String)(this.config.automaticCoinConversion ? "\u2713 " : "\u2715 ")).append((Component)Component.translatable((String)(this.config.automaticCoinConversion ? "gui.avecoins.enabled" : "gui.avecoins.disabled")));
    }

    private int conversionToggleColor() {
        return this.config.automaticCoinConversion ? -6307928 : -3044212;
    }

    public void showResult(String code) {
        this.resultCode = code;
    }
}

