/*
 * Decompiled with CFR 0.152.
 */
package net.sundggs.avecoins.client.gui;

import com.google.gson.JsonObject;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.components.events.GuiEventListener;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
import net.sundggs.avecoins.client.gui.AvecoinsScreen;
import net.sundggs.avecoins.client.gui.CraftingConfigScreen;
import net.sundggs.avecoins.client.gui.PanelButton;
import net.sundggs.avecoins.client.gui.ShopSettingsScreen;
import net.sundggs.avecoins.network.ClientNetworking;
import net.sundggs.avecoins.network.ServerboundAvecoinsPayload;

public final class DashboardScreen
extends AvecoinsScreen {
    private final JsonObject snapshot;
    private String resultCode;

    public DashboardScreen(JsonObject snapshot) {
        this(snapshot, snapshot.get("result").getAsString());
    }

    public DashboardScreen(JsonObject snapshot, String resultCode) {
        super((Component)Component.translatable((String)"gui.avecoins.dashboard.title"));
        this.snapshot = snapshot;
        this.resultCode = resultCode;
    }

    protected void init() {
        this.layoutPanel(250, 348);
        int buttonWidth = 136;
        int x = this.panelLeft + (this.panelWidth - buttonWidth) / 2;
        int y = this.panelTop + 105;
        PanelButton coins = new PanelButton(x, y, buttonWidth, 24, (Component)Component.translatable((String)"gui.avecoins.dashboard.coins"), PanelButton.Variant.PRIMARY, button -> this.minecraft.gui.setScreen((Screen)new CraftingConfigScreen(this.snapshot, CraftingConfigScreen.Mode.COINS)));
        coins.active = this.snapshot.get("canConfigureCoins").getAsBoolean();
        this.addRenderableWidget((GuiEventListener)coins);
        PanelButton tickets = new PanelButton(x, y + 30, buttonWidth, 24, (Component)Component.translatable((String)"gui.avecoins.dashboard.tickets"), PanelButton.Variant.PRIMARY, button -> this.minecraft.gui.setScreen((Screen)new CraftingConfigScreen(this.snapshot, CraftingConfigScreen.Mode.TICKETS)));
        tickets.active = this.snapshot.get("canConfigureTickets").getAsBoolean();
        this.addRenderableWidget((GuiEventListener)tickets);
        PanelButton shop = new PanelButton(x, y + 60, buttonWidth, 24, (Component)Component.translatable((String)"gui.avecoins.dashboard.shop"), PanelButton.Variant.PRIMARY, button -> this.minecraft.gui.setScreen((Screen)new ShopSettingsScreen(this.snapshot)));
        shop.active = this.snapshot.get("canConfigureShop").getAsBoolean();
        this.addRenderableWidget((GuiEventListener)shop);
        PanelButton reload = new PanelButton(x, this.panelTop + 278, buttonWidth, 24, (Component)Component.translatable((String)"gui.avecoins.dashboard.reload"), PanelButton.Variant.PRIMARY, button -> ClientNetworking.send(new ServerboundAvecoinsPayload("reload_config", "{}")));
        reload.active = this.snapshot.get("canReloadConfig").getAsBoolean();
        this.addRenderableWidget((GuiEventListener)reload);
    }

    @Override
    protected void renderPanel(GuiGraphicsExtractor graphics, int mouseX, int mouseY, float partialTick) {
        this.renderHeader(graphics, null);
        this.drawSectionTitle(graphics, (Component)Component.translatable((String)"gui.avecoins.dashboard.options"), this.panelTop + 51);
        this.drawCenteredWrapped(graphics, (Component)Component.translatable((String)"gui.avecoins.dashboard.subtitle"), this.panelTop + 70, 176, -6183253);
        this.divider(graphics, this.panelTop + 207, 34);
        this.drawSectionTitle(graphics, (Component)Component.translatable((String)"gui.avecoins.dashboard.configuration"), this.panelTop + 224);
        this.drawCenteredWrapped(graphics, (Component)Component.translatable((String)"gui.avecoins.dashboard.configuration_help"), this.panelTop + 243, 176, -6183253);
        this.renderStatusBox(graphics, this.resultCode);
    }

    public void showResult(String code) {
        this.resultCode = code;
    }
}

