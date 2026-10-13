/*
 * Decompiled with CFR 0.152.
 */
package net.sundggs.avecoins.client.gui;

import java.util.LinkedHashMap;
import java.util.Map;
import net.minecraft.client.Minecraft;
import net.minecraft.core.HolderLookup;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.Identifier;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.ItemLike;
import net.sundggs.avecoins.shop.ShopData;
import net.sundggs.avecoins.shop.ShopItemStacks;

final class ProductDisplayStack {
    private static final int CACHE_LIMIT = 256;
    private static final Map<Key, ItemStack> CACHE = new LinkedHashMap<Key, ItemStack>(32, 0.75f, true){

        @Override
        protected boolean removeEldestEntry(Map.Entry<Key, ItemStack> eldest) {
            return this.size() > 256;
        }
    };
    private static Object cachedLevel;

    static ItemStack create(ShopData.Product product) {
        Minecraft minecraft = Minecraft.getInstance();
        if (cachedLevel != minecraft.level) {
            CACHE.clear();
            cachedLevel = minecraft.level;
        }
        Key key = new Key(product.itemId, product.itemData, product.itemCount);
        return CACHE.computeIfAbsent(key, ignored -> ProductDisplayStack.decode(product, minecraft));
    }

    private static ItemStack decode(ShopData.Product product, Minecraft minecraft) {
        Identifier id = Identifier.tryParse((String)product.itemId);
        if (id == null || !BuiltInRegistries.ITEM.containsKey(id)) {
            return ItemStack.EMPTY;
        }
        Item expected = (Item)BuiltInRegistries.ITEM.getValue(id);
        if (minecraft.level != null && product.itemData != null && !product.itemData.isBlank()) {
            return ShopItemStacks.decodeExpected((HolderLookup.Provider)minecraft.level.registryAccess(), product.itemData, expected).map(stack -> stack.copyWithCount(product.itemCount)).orElse(ItemStack.EMPTY);
        }
        return new ItemStack((ItemLike)expected, product.itemCount);
    }

    private ProductDisplayStack() {
    }

    private record Key(String itemId, String itemData, int itemCount) {
    }
}

