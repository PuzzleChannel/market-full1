/*
 * Decompiled with CFR 0.152.
 */
package net.sundggs.avecoins.client.gui;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import java.util.List;
import java.util.Map;
import net.minecraft.ChatFormatting;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.components.events.GuiEventListener;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.ItemLike;
import net.sundggs.avecoins.client.gui.AvecoinsScreen;
import net.sundggs.avecoins.client.gui.DashboardScreen;
import net.sundggs.avecoins.client.gui.PanelButton;
import net.sundggs.avecoins.client.gui.RecipeEditorScreen;
import net.sundggs.avecoins.config.AtomicJsonStore;
import net.sundggs.avecoins.config.CraftingConfig;
import net.sundggs.avecoins.config.RecipesConfig;
import net.sundggs.avecoins.network.ClientNetworking;
import net.sundggs.avecoins.network.ServerboundAvecoinsPayload;

public final class CraftingConfigScreen
extends AvecoinsScreen {
    private final JsonObject dashboardSnapshot;
    private final Mode mode;
    private final CraftingConfig config;
    private final RecipesConfig recipes;
    private String resultCode;

    public CraftingConfigScreen(JsonObject dashboardSnapshot, Mode mode) {
        this(dashboardSnapshot, mode, "");
    }

    public CraftingConfigScreen(JsonObject dashboardSnapshot, Mode mode, String resultCode) {
        this(dashboardSnapshot, mode, (CraftingConfig)AtomicJsonStore.gson().fromJson((JsonElement)dashboardSnapshot.getAsJsonObject("crafting"), CraftingConfig.class), (RecipesConfig)AtomicJsonStore.gson().fromJson((JsonElement)dashboardSnapshot.getAsJsonObject("recipes"), RecipesConfig.class), resultCode);
    }

    CraftingConfigScreen(JsonObject dashboardSnapshot, Mode mode, CraftingConfig config, RecipesConfig recipes, String resultCode) {
        super((Component)Component.translatable((String)(mode == Mode.COINS ? "gui.avecoins.coins.title" : "gui.avecoins.tickets.title")));
        this.dashboardSnapshot = dashboardSnapshot;
        this.mode = mode;
        this.config = config;
        this.recipes = recipes;
        this.resultCode = resultCode;
    }

    protected void init() {
        List<String> items = this.items();
        this.layoutPanel(410, 266 + items.size() * 34);
        this.addHeaderBackButton(new DashboardScreen(this.dashboardSnapshot, ""));
        int masterWidth = 86;
        this.addRenderableWidget(new PanelButton(this.panelLeft + this.panelWidth - masterWidth - 62, this.panelTop + 127, masterWidth, 20, CraftingConfigScreen.toggleLabel(this.masterEnabled()), PanelButton.Variant.PRIMARY, CraftingConfigScreen.toggleColor(this.masterEnabled()), button -> {
            this.setMaster(!this.masterEnabled());
            this.resultCode = "";
            this.rebuildWidgets();
        }));
        int rowY = this.panelTop + 180;
        int toggleX = this.panelLeft + this.panelWidth - 108;
        int editX = toggleX - 92;
        Map<String, Boolean> toggles = this.toggles();
        boolean individualControlsEnabled = this.masterEnabled();
        for (String itemId : items) {
            int currentY = rowY;
            PanelButton editButton = new PanelButton(editX, currentY, 86, 20, (Component)Component.translatable((String)"gui.avecoins.recipe.edit"), PanelButton.Variant.SECONDARY, button -> this.editRecipe(itemId));
            this.addRenderableWidget(editButton);
            boolean enabled = toggles.getOrDefault(itemId, true);
            PanelButton toggleButton = new PanelButton(toggleX, currentY, 80, 20, CraftingConfigScreen.toggleLabel(enabled), PanelButton.Variant.PRIMARY, CraftingConfigScreen.toggleColor(enabled), button -> this.toggleRecipe(itemId));
            toggleButton.setVisuallyDisabled(!individualControlsEnabled);
            this.addRenderableWidget(toggleButton);
            rowY += 34;
        }
        int saveY = rowY + 16;
        this.addRenderableWidget(new PanelButton(this.panelLeft + (this.panelWidth - 140) / 2, saveY, 140, 22, (Component)Component.translatable((String)"gui.avecoins.save"), PanelButton.Variant.PRIMARY, button -> ClientNetworking.send(new ServerboundAvecoinsPayload(this.mode == Mode.COINS ? "save_crafting_coins" : "save_crafting_tickets", AtomicJsonStore.gson().toJson(this.config)))));
    }

    @Override
    protected void renderPanel(GuiGraphicsExtractor graphics, int mouseX, int mouseY, float partialTick) {
        this.renderHeader(graphics, null);
        this.drawSectionTitle(graphics, (Component)Component.translatable((String)"gui.avecoins.crafting.configuration"), this.panelTop + 47);
        this.drawCenteredWrapped(graphics, (Component)Component.translatable((String)(this.mode == Mode.COINS ? "gui.avecoins.coins.subtitle" : "gui.avecoins.tickets.subtitle")), this.panelTop + 62, 220, -6183253);
        this.divider(graphics, this.panelTop + 97, 26);
        int craftingCenterX = this.panelLeft + 142;
        this.drawSectionTitleAt(graphics, (Component)Component.translatable((String)"gui.avecoins.crafting.master"), craftingCenterX, this.panelTop + 116);
        this.drawCenteredWrappedAt(graphics, (Component)Component.translatable((String)(this.mode == Mode.COINS ? "gui.avecoins.coins.master_help" : "gui.avecoins.tickets.master_help")), craftingCenterX, this.panelTop + 132, 230, -6183253);
        this.divider(graphics, this.panelTop + 166, 26);
        int rowY = this.panelTop + 180;
        for (String itemId : this.items()) {
            Identifier id = Identifier.parse((String)itemId);
            graphics.item(new ItemStack((ItemLike)BuiltInRegistries.ITEM.getValue(id)), this.panelLeft + 28, rowY + 1);
            graphics.text(this.font, (Component)Component.translatable((String)("item." + id.getNamespace() + "." + id.getPath())).withStyle(ChatFormatting.BOLD), this.panelLeft + 52, rowY + 1, -1710103);
            boolean custom = this.recipes.customRecipes.containsKey(itemId);
            this.drawScaledText(graphics, (Component)Component.translatable((String)(custom ? "gui.avecoins.recipe.custom" : "gui.avecoins.recipe.default")), this.panelLeft + 52, rowY + 14, 0.75f, custom ? -6307928 : -6183253);
            rowY += 34;
        }
        this.renderStatusBox(graphics, this.resultCode);
    }

    private List<String> items() {
        return this.mode == Mode.COINS ? CraftingConfig.COINS : CraftingConfig.TICKETS;
    }

    private Map<String, Boolean> toggles() {
        return this.mode == Mode.COINS ? this.config.coins : this.config.tickets;
    }

    private boolean masterEnabled() {
        return this.mode == Mode.COINS ? this.config.coinCraftingEnabled : this.config.ticketCraftingEnabled;
    }

    private void setMaster(boolean value) {
        if (this.mode == Mode.COINS) {
            this.config.coinCraftingEnabled = value;
        } else {
            this.config.ticketCraftingEnabled = value;
        }
    }

    private void editRecipe(String itemId) {
        this.resultCode = "";
        this.minecraft.gui.setScreen((Screen)new RecipeEditorScreen(this, this.recipes, itemId));
    }

    private void toggleRecipe(String itemId) {
        if (!this.masterEnabled()) {
            this.resultCode = "crafting_master_toggle_disabled";
            return;
        }
        Map<String, Boolean> toggles = this.toggles();
        toggles.put(itemId, !toggles.getOrDefault(itemId, true));
        this.resultCode = "";
        this.rebuildWidgets();
    }

    private static Component toggleLabel(boolean enabled) {
        return Component.literal((String)(enabled ? "\u2713 " : "\u2715 ")).append((Component)Component.translatable((String)(enabled ? "gui.avecoins.enabled" : "gui.avecoins.disabled")));
    }

    private static int toggleColor(boolean enabled) {
        return enabled ? -6307928 : -3044212;
    }

    public void showResult(String code) {
        this.resultCode = code;
    }

    public static enum Mode {
        COINS,
        TICKETS;

    }
}

