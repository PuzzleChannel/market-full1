/*
 * Decompiled with CFR 0.152.
 */
package net.sundggs.avecoins.client.gui;

import com.google.gson.JsonObject;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import net.minecraft.ChatFormatting;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.gui.components.events.GuiEventListener;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.FormattedText;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.resources.Identifier;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.ItemLike;
import net.sundggs.avecoins.client.gui.AvecoinsScreen;
import net.sundggs.avecoins.client.gui.CraftingConfigScreen;
import net.sundggs.avecoins.client.gui.DowngradeRecipeScreen;
import net.sundggs.avecoins.client.gui.GhostIngredientButton;
import net.sundggs.avecoins.client.gui.IngredientSelectorScreen;
import net.sundggs.avecoins.client.gui.PanelButton;
import net.sundggs.avecoins.config.AtomicJsonStore;
import net.sundggs.avecoins.config.RecipeDefinition;
import net.sundggs.avecoins.config.RecipesConfig;
import net.sundggs.avecoins.network.ClientNetworking;
import net.sundggs.avecoins.network.ServerboundAvecoinsPayload;

public final class RecipeEditorScreen
extends AvecoinsScreen {
    private static final int CONTENT_PADDING = 35;
    private static final int INFO_WIDTH = 200;
    private static final int COLUMN_GAP = 50;
    private static final int SLOT_STEP = 42;
    private static final int GRID_SIZE = 122;
    private static final int INFO_ITEM_Y = 74;
    private static final int INFO_COUNT_LABEL_Y = 105;
    private static final int INFO_COUNT_BOX_Y = 122;
    private static final int INFO_HELP_Y = 149;
    private static final int INFO_DIVIDER_Y = 186;
    private static final int INFO_SAVE_Y = 205;
    private static final int INFO_RESTORE_Y = 235;
    private static final int INFO_RESTORE_HELP_Y = 262;
    private static final int DOWNGRADE_GRID_HELP_Y = 60;
    private static final int DOWNGRADE_GRID_TOP = 95;
    private static final int DOWNGRADE_CLEAR_Y = 229;
    private static final int DOWNGRADE_EDIT_Y = 261;
    private static final int DOWNGRADE_HELP_Y = 287;
    private static final int STANDARD_GRID_HELP_Y = 77;
    private static final int STANDARD_GRID_TOP = 116;
    private static final int STANDARD_CLEAR_Y = 254;
    private final CraftingConfigScreen parent;
    private final RecipesConfig recipes;
    private final String resultId;
    private final List<String> grid;
    private EditBox countBox;
    private String countValue;
    private String resultCode = "";
    private PendingAction pendingAction = PendingAction.NONE;
    private RecipeDefinition pendingRecipe;

    public RecipeEditorScreen(CraftingConfigScreen parent, RecipesConfig recipes, String resultId) {
        super((Component)Component.translatable((String)"gui.avecoins.recipe.editor.title"));
        this.parent = parent;
        this.recipes = recipes;
        this.resultId = resultId;
        RecipeDefinition existing = recipes.customRecipes.get(resultId);
        this.grid = new ArrayList<String>(existing == null ? Collections.nCopies(9, "") : existing.grid);
        this.countValue = Integer.toString(existing == null ? 1 : existing.count);
    }

    protected void init() {
        this.layoutPanel(470, 360);
        this.addHeaderBackButton(this.parent);
        int infoX = this.panelLeft + 35;
        int infoCenterX = infoX + 100;
        int gridLeft = infoX + 200 + 50;
        int gridTop = this.panelTop + (this.hasDowngrade() ? 95 : 116);
        for (int index = 0; index < 9; ++index) {
            int slot = index;
            this.addRenderableWidget(new GhostIngredientButton(gridLeft + index % 3 * 42, gridTop + index / 3 * 42, this.grid.get(index), () -> this.openIngredientSelector(slot), () -> this.clearSlot(slot)));
        }
        this.countBox = new EditBox(this.font, infoCenterX - 26, this.panelTop + 122, 52, 20, (Component)Component.translatable((String)"gui.avecoins.recipe.count"));
        this.countBox.setMaxLength(2);
        this.countBox.setValue(this.countValue);
        this.countBox.setResponder(value -> {
            this.countValue = value;
        });
        this.addRenderableWidget(this.countBox);
        this.addRenderableWidget(new PanelButton(infoCenterX - 80, this.panelTop + 205, 160, 20, (Component)Component.translatable((String)"gui.avecoins.save"), PanelButton.Variant.PRIMARY, button -> this.saveRecipe()));
        this.addRenderableWidget(new PanelButton(infoCenterX - 80, this.panelTop + 235, 160, 20, (Component)Component.translatable((String)"gui.avecoins.recipe.restore"), PanelButton.Variant.DANGER, button -> this.restoreRecipe()));
        this.addRenderableWidget(new PanelButton(gridLeft + -7, this.panelTop + (this.hasDowngrade() ? 229 : 254), 136, 22, (Component)Component.translatable((String)"gui.avecoins.recipe.clear_grid"), PanelButton.Variant.SECONDARY, button -> {
            Collections.fill(this.grid, "");
            this.resultCode = "recipe_grid_cleared";
            this.rebuildWidgets();
        }));
        if (this.hasDowngrade()) {
            this.addRenderableWidget(new PanelButton(gridLeft + -7, this.panelTop + 261, 136, 20, (Component)Component.translatable((String)"gui.avecoins.recipe.downgrade.edit"), PanelButton.Variant.SECONDARY, button -> this.minecraft.gui.setScreen((Screen)new DowngradeRecipeScreen(this, this.recipes, this.resultId))));
        }
    }

    private void openIngredientSelector(int slot) {
        this.countValue = this.countBox.getValue();
        this.resultCode = "";
        this.minecraft.gui.setScreen((Screen)new IngredientSelectorScreen(this, this.grid.get(slot), value -> this.grid.set(slot, (String)value)));
    }

    private void clearSlot(int slot) {
        this.grid.set(slot, "");
        this.resultCode = "recipe_slot_cleared";
        this.rebuildWidgets();
    }

    private void saveRecipe() {
        if (this.pendingAction != PendingAction.NONE) {
            return;
        }
        try {
            int count = Integer.parseInt(this.countBox.getValue());
            if (count < 1 || count > 64 || this.grid.stream().allMatch(String::isEmpty)) {
                throw new IllegalArgumentException();
            }
            this.pendingRecipe = new RecipeDefinition(count, this.grid);
            this.pendingAction = PendingAction.SAVE;
            this.resultCode = "";
            JsonObject request = new JsonObject();
            request.addProperty("resultId", this.resultId);
            request.add("recipe", AtomicJsonStore.gson().toJsonTree(this.pendingRecipe));
            ClientNetworking.send(new ServerboundAvecoinsPayload("save_custom_recipe", request.toString()));
        }
        catch (RuntimeException exception) {
            this.resultCode = "invalid_recipe";
        }
    }

    private void restoreRecipe() {
        if (this.pendingAction != PendingAction.NONE) {
            return;
        }
        this.pendingAction = PendingAction.RESTORE;
        this.resultCode = "";
        JsonObject request = new JsonObject();
        request.addProperty("resultId", this.resultId);
        ClientNetworking.send(new ServerboundAvecoinsPayload("restore_custom_recipe", request.toString()));
    }

    public void showResult(String code, boolean success) {
        if (success && this.pendingAction == PendingAction.SAVE && this.pendingRecipe != null) {
            this.recipes.customRecipes.put(this.resultId, this.pendingRecipe);
            this.countValue = Integer.toString(this.pendingRecipe.count);
        } else if (success && this.pendingAction == PendingAction.RESTORE) {
            this.recipes.customRecipes.remove(this.resultId);
            Collections.fill(this.grid, "");
            this.countValue = "1";
        }
        this.pendingAction = PendingAction.NONE;
        this.pendingRecipe = null;
        this.resultCode = code;
        this.rebuildWidgets();
    }

    @Override
    protected void renderPanel(GuiGraphicsExtractor graphics, int mouseX, int mouseY, float partialTick) {
        this.renderHeader(graphics, null);
        int infoX = this.panelLeft + 35;
        int infoCenterX = infoX + 100;
        int gridLeft = infoX + 200 + 50;
        int gridCenterX = gridLeft + 61;
        this.drawCenteredScaledWrappedAt(graphics, (Component)Component.translatable((String)"gui.avecoins.recipe.editor.help"), gridCenterX, this.panelTop + (this.hasDowngrade() ? 60 : 77), 160, 0.8f, -6183253);
        Identifier id = Identifier.parse((String)this.resultId);
        ItemStack resultStack = new ItemStack((ItemLike)BuiltInRegistries.ITEM.getValue(id));
        MutableComponent itemName = Component.translatable((String)("item." + id.getNamespace() + "." + id.getPath())).withStyle(ChatFormatting.BOLD);
        int itemGroupWidth = 22 + this.font.width((FormattedText)itemName);
        int itemX = infoCenterX - itemGroupWidth / 2;
        graphics.item(resultStack, itemX, this.panelTop + 74);
        graphics.text(this.font, (Component)itemName, itemX + 22, this.panelTop + 74 + 4, -1710103);
        MutableComponent countLabel = Component.literal((String)"# ").withStyle(ChatFormatting.GRAY).append((Component)Component.translatable((String)"gui.avecoins.recipe.count").withStyle(new ChatFormatting[]{ChatFormatting.WHITE, ChatFormatting.BOLD})).append((Component)Component.literal((String)":").withStyle(new ChatFormatting[]{ChatFormatting.WHITE, ChatFormatting.BOLD}));
        graphics.centeredText(this.font, (Component)countLabel, infoCenterX, this.panelTop + 105, -1710103);
        this.drawCenteredScaledWrappedAt(graphics, (Component)Component.translatable((String)"gui.avecoins.recipe.count_help"), infoCenterX, this.panelTop + 149, 170, 1.0f, -6183253);
        graphics.fill(infoX + 10, this.panelTop + 186, infoX + 200 - 10, this.panelTop + 186 + 1, -12959932);
        this.drawCenteredScaledWrappedAt(graphics, (Component)Component.translatable((String)"gui.avecoins.recipe.restore_help"), infoCenterX, this.panelTop + 262, 170, 0.8f, -6183253);
        int columnDividerX = this.panelLeft + this.panelWidth / 2 + 5;
        graphics.fill(columnDividerX, this.panelTop + 52, columnDividerX + 1, this.panelTop + 308, -12959932);
        if (this.hasDowngrade()) {
            this.drawCenteredScaledWrappedAt(graphics, (Component)Component.translatable((String)"gui.avecoins.recipe.downgrade.link_help"), gridCenterX, this.panelTop + 287, 160, 0.75f, -6183253);
        }
        this.renderStatusBox(graphics, this.resultCode);
    }

    public void onClose() {
        this.minecraft.gui.setScreen((Screen)this.parent);
    }

    private boolean hasDowngrade() {
        return RecipesConfig.DOWNGRADE_RESULTS.containsKey(this.resultId);
    }

    private static enum PendingAction {
        NONE,
        SAVE,
        RESTORE;

    }
}

