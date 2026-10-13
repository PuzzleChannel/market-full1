/*
 * Decompiled with CFR 0.152.
 */
package net.sundggs.avecoins.shop;

import net.fabricmc.fabric.api.menu.v1.ExtendedMenuType;
import net.minecraft.core.Registry;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.resources.Identifier;
import net.minecraft.world.inventory.MenuType;
import net.sundggs.avecoins.shop.SellBoxMenu;
import net.sundggs.avecoins.shop.WalletMenu;

public final class ModMenus {
    public static final MenuType<SellBoxMenu> SELL_BOX = new ExtendedMenuType(SellBoxMenu::new, ByteBufCodecs.stringUtf8((int)0x200000));
    public static final MenuType<WalletMenu> WALLET = new ExtendedMenuType(WalletMenu::new, ByteBufCodecs.stringUtf8((int)0x200000));

    public static void register() {
        Registry.register((Registry)BuiltInRegistries.MENU, (Identifier)Identifier.fromNamespaceAndPath((String)"avecoins", (String)"sell_box"), SELL_BOX);
        Registry.register((Registry)BuiltInRegistries.MENU, (Identifier)Identifier.fromNamespaceAndPath((String)"avecoins", (String)"wallet"), WALLET);
    }

    private ModMenus() {
    }
}

