/*
 * Decompiled with CFR 0.152.
 */
package net.sundggs.avecoins.client.gui;

import com.google.gson.JsonObject;
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
import net.sundggs.avecoins.client.gui.PanelButton;
import net.sundggs.avecoins.client.gui.RecipeEditorScreen;
import net.sundggs.avecoins.config.RecipesConfig;
import net.sundggs.avecoins.network.ClientNetworking;
import net.sundggs.avecoins.network.ServerboundAvecoinsPayload;

public final class DowngradeRecipeScreen
extends AvecoinsScreen {
    private final RecipeEditorScreen parent;
    private final RecipesConfig recipes;
    private final String sourceId;
    private final String resultId;
    private EditBox countBox;
    private String countValue;
    private String resultCode = "";
    private PendingAction pendingAction = PendingAction.NONE;
    private int pendingCount;

    public DowngradeRecipeScreen(RecipeEditorScreen parent, RecipesConfig recipes, String sourceId) {
        super((Component)Component.translatable((String)"gui.avecoins.recipe.downgrade.title"));
        this.parent = parent;
        this.recipes = recipes;
        this.sourceId = sourceId;
        this.resultId = RecipesConfig.DOWNGRADE_RESULTS.get(sourceId);
        if (this.resultId == null) {
            throw new IllegalArgumentException("Coin has no downgrade recipe: " + sourceId);
        }
        this.countValue = Integer.toString(recipes.downgradeCounts.getOrDefault(sourceId, 4));
    }

    protected void init() {
        this.layoutPanel(420, 290);
        this.addHeaderBackButton(this.parent);
        this.countBox = new EditBox(this.font, this.panelLeft + this.panelWidth / 2 - 28, this.panelTop + 170, 56, 20, (Component)Component.translatable((String)"gui.avecoins.recipe.count"));
        this.countBox.setMaxLength(2);
        this.countBox.setValue(this.countValue);
        this.countBox.setResponder(value -> {
            this.countValue = value;
        });
        this.addRenderableWidget(this.countBox);
        this.addRenderableWidget(new PanelButton(this.panelLeft + 55, this.panelTop + 224, 140, 22, (Component)Component.translatable((String)"gui.avecoins.save"), PanelButton.Variant.PRIMARY, button -> this.save()));
        this.addRenderableWidget(new PanelButton(this.panelLeft + 225, this.panelTop + 224, 140, 22, (Component)Component.translatable((String)"gui.avecoins.recipe.restore"), PanelButton.Variant.DANGER, button -> this.restore()));
    }

    private void save() {
        if (this.pendingAction != PendingAction.NONE) {
            return;
        }
        try {
            int count = Integer.parseInt(this.countBox.getValue());
            if (count < 1 || count > 64) {
                throw new IllegalArgumentException();
            }
            this.pendingCount = count;
            this.pendingAction = PendingAction.SAVE;
            this.resultCode = "";
            JsonObject request = new JsonObject();
            request.addProperty("sourceId", this.sourceId);
            request.addProperty("count", (Number)count);
            ClientNetworking.send(new ServerboundAvecoinsPayload("save_downgrade_recipe", request.toString()));
        }
        catch (RuntimeException exception) {
            this.resultCode = "invalid_downgrade_count";
        }
    }

    private void restore() {
        if (this.pendingAction != PendingAction.NONE) {
            return;
        }
        this.pendingAction = PendingAction.RESTORE;
        this.resultCode = "";
        JsonObject request = new JsonObject();
        request.addProperty("sourceId", this.sourceId);
        ClientNetworking.send(new ServerboundAvecoinsPayload("restore_downgrade_recipe", request.toString()));
    }

    public void showResult(String code, boolean success) {
        if (success && this.pendingAction == PendingAction.SAVE) {
            this.recipes.downgradeCounts.put(this.sourceId, this.pendingCount);
            this.countValue = Integer.toString(this.pendingCount);
        } else if (success && this.pendingAction == PendingAction.RESTORE) {
            this.recipes.downgradeCounts.remove(this.sourceId);
            this.countValue = Integer.toString(4);
        }
        this.pendingAction = PendingAction.NONE;
        this.resultCode = code;
        this.rebuildWidgets();
    }

    @Override
    protected void renderPanel(GuiGraphicsExtractor graphics, int mouseX, int mouseY, float partialTick) {
        this.renderHeader(graphics, null);
        this.drawSectionTitle(graphics, (Component)Component.translatable((String)"gui.avecoins.recipe.downgrade.conversion"), this.panelTop + 49);
        this.renderCoin(graphics, this.sourceId, this.panelLeft + 116, this.panelTop + 79);
        graphics.centeredText(this.font, (Component)Component.literal((String)"\u2192").withStyle(ChatFormatting.BOLD), this.panelLeft + this.panelWidth / 2, this.panelTop + 84, -6183253);
        this.renderCoin(graphics, this.resultId, this.panelLeft + 304, this.panelTop + 79);
        this.drawCenteredScaledWrappedAt(graphics, (Component)Component.translatable((String)"gui.avecoins.recipe.downgrade.help"), this.panelLeft + this.panelWidth / 2, this.panelTop + 111, 330, 0.85f, -6183253);
        this.divider(graphics, this.panelTop + 139, 34);
        this.drawSectionTitle(graphics, (Component)Component.translatable((String)"gui.avecoins.recipe.downgrade.output"), this.panelTop + 153);
        this.drawCenteredWrapped(graphics, (Component)Component.translatable((String)"gui.avecoins.recipe.downgrade.count_help"), this.panelTop + 198, 300, -6183253);
        this.renderStatusBox(graphics, this.resultCode);
    }

    private void renderCoin(GuiGraphicsExtractor graphics, String itemId, int centerX, int y) {
        Identifier id = Identifier.parse((String)itemId);
        ItemStack stack = new ItemStack((ItemLike)BuiltInRegistries.ITEM.getValue(id));
        MutableComponent name = stack.getHoverName().copy().withStyle(ChatFormatting.BOLD);
        int width = 20 + this.font.width((FormattedText)name);
        int x = centerX - width / 2;
        graphics.item(stack, x, y);
        graphics.text(this.font, (Component)name, x + 20, y + 4, -1710103, false);
    }

    public void onClose() {
        this.minecraft.gui.setScreen((Screen)this.parent);
    }

    private static enum PendingAction {
        NONE,
        SAVE,
        RESTORE;

    }
}

