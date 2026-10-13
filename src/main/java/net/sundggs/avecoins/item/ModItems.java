/*
 * Decompiled with CFR 0.152.
 */
package net.sundggs.avecoins.item;

import net.minecraft.core.Registry;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.Identifier;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.item.Item;
import net.sundggs.avecoins.AvecoinsMod;

public final class ModItems {
    public static final Item COPPERCOIN = ModItems.register("coppercoin");
    public static final Item IRONCOIN = ModItems.register("ironcoin");
    public static final Item GOLDCOIN = ModItems.register("goldcoin");
    public static final Item DIAMONDCOIN = ModItems.register("diamondcoin");
    public static final Item NETHERITECOIN = ModItems.register("netheritecoin");
    public static final Item GOLDTICKET = ModItems.register("goldticket");
    public static final Item DIAMONDTICKET = ModItems.register("diamondticket");
    public static final Item NETHERITETICKET = ModItems.register("netheriteticket");

    private static Item register(String name) {
        ResourceKey key = ResourceKey.create((ResourceKey)Registries.ITEM, (Identifier)Identifier.fromNamespaceAndPath((String)"avecoins", (String)name));
        return (Item)Registry.register((Registry)BuiltInRegistries.ITEM, (ResourceKey)key, (Object)new Item(new Item.Properties().setId(key)));
    }

    public static void register() {
        AvecoinsMod.LOGGER.info("Registered AVECOINS items");
    }

    private ModItems() {
    }
}

