/*
 * Decompiled with CFR 0.152.
 */
package net.sundggs.avecoins;

import com.mojang.logging.LogUtils;
import net.fabricmc.api.ModInitializer;
import net.sundggs.avecoins.command.ModCommands;
import net.sundggs.avecoins.config.ModConfigManager;
import net.sundggs.avecoins.item.ModCreativeModeTabs;
import net.sundggs.avecoins.item.ModItems;
import net.sundggs.avecoins.network.ModNetworking;
import net.sundggs.avecoins.server.ModServerEvents;
import net.sundggs.avecoins.shop.ModMenus;
import org.slf4j.Logger;

public final class AvecoinsMod
implements ModInitializer {
    public static final String MOD_ID = "avecoins";
    public static final Logger LOGGER = LogUtils.getLogger();

    public void onInitialize() {
        ModItems.register();
        ModCreativeModeTabs.register();
        ModMenus.register();
        ModConfigManager.initialize();
        ModNetworking.register();
        ModCommands.register();
        ModServerEvents.register();
    }
}

