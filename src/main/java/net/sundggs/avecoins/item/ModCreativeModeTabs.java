/*
 * Decompiled with CFR 0.152.
 */
package net.sundggs.avecoins.item;

import net.fabricmc.fabric.api.creativetab.v1.FabricCreativeModeTab;
import net.minecraft.core.Registry;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.item.CreativeModeTab;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.ItemLike;
import net.sundggs.avecoins.AvecoinsMod;
import net.sundggs.avecoins.item.ModItems;

public final class ModCreativeModeTabs {
    private static final ResourceKey<CreativeModeTab> TAB_KEY = ResourceKey.create(BuiltInRegistries.CREATIVE_MODE_TAB.key(), (Identifier)Identifier.fromNamespaceAndPath((String)"avecoins", (String)"avecoins_items_tab"));
    public static final CreativeModeTab AVECOINS_ITEMS_TAB = FabricCreativeModeTab.builder().icon(() -> new ItemStack((ItemLike)ModItems.GOLDTICKET)).title((Component)Component.translatable((String)"creativetab.avecoinsmod.items")).displayItems((parameters, output) -> {
        output.accept((ItemLike)ModItems.GOLDTICKET);
        output.accept((ItemLike)ModItems.DIAMONDTICKET);
        output.accept((ItemLike)ModItems.NETHERITETICKET);
    }).build();

    public static void register() {
        Registry.register(BuiltInRegistries.CREATIVE_MODE_TAB, TAB_KEY, AVECOINS_ITEMS_TAB);
        AvecoinsMod.LOGGER.info("Registered AVECOINS creative tab");
    }

    private ModCreativeModeTabs() {
    }
}

