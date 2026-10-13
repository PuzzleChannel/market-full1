/*
 * Decompiled with CFR 0.152.
 */
package net.sundggs.avecoins.shop;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.Identifier;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.SimpleContainer;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.ItemLike;
import net.sundggs.avecoins.config.CraftingConfig;
import net.sundggs.avecoins.shop.ShopService;
import net.sundggs.avecoins.shop.WalletData;
import net.sundggs.avecoins.shop.WalletStore;

public final class WalletService {
    public static Map<String, Long> walletBalances(ServerPlayer player) {
        return WalletStore.get().balances(player.getUUID());
    }

    public static Map<String, Long> inventoryBalances(ServerPlayer player) {
        LinkedHashMap<String, Long> result = new LinkedHashMap<String, Long>();
        for (String currencyId : CraftingConfig.MANAGED_RESULTS) {
            Item currency = WalletService.resolveCurrency(currencyId);
            long count = currency == null ? 0L : WalletService.countPhysical(player, currency);
            if (count <= 0L) continue;
            result.put(currencyId, count);
        }
        return result;
    }

    public static Map<String, Long> combinedBalances(ServerPlayer player) {
        LinkedHashMap<String, Long> result = new LinkedHashMap<String, Long>(WalletService.inventoryBalances(player));
        WalletService.walletBalances(player).forEach((currency, amount) -> result.merge((String)currency, (Long)amount, Math::addExact));
        return result;
    }

    public static SimpleContainer createContainer(ServerPlayer player) {
        SimpleContainer container = new SimpleContainer(27);
        int slot = 0;
        for (String currencyId : CraftingConfig.MANAGED_RESULTS) {
            int amount;
            Item currency = WalletService.resolveCurrency(currencyId);
            for (long remaining = WalletStore.get().balance(player.getUUID(), currencyId); currency != null && remaining > 0L && slot < container.getContainerSize(); remaining -= (long)amount) {
                amount = (int)Math.min(remaining, (long)currency.getDefaultMaxStackSize());
                container.setItem(slot++, new ItemStack((ItemLike)currency, amount));
            }
        }
        return container;
    }

    public static boolean saveContainer(ServerPlayer player, SimpleContainer container) {
        LinkedHashMap<String, Long> balances = new LinkedHashMap<String, Long>();
        for (int slot = 0; slot < container.getContainerSize(); ++slot) {
            ItemStack stack = container.getItem(slot);
            if (stack.isEmpty()) continue;
            String currencyId = BuiltInRegistries.ITEM.getKey((Object)stack.getItem()).toString();
            if (!CraftingConfig.MANAGED_RESULTS.contains(currencyId)) {
                return false;
            }
            balances.merge(currencyId, Long.valueOf(stack.getCount()), Math::addExact);
        }
        WalletData candidate = WalletStore.get().copy();
        try {
            candidate.replaceBalances(player.getUUID(), balances);
        }
        catch (ArithmeticException | IllegalArgumentException exception) {
            return false;
        }
        return WalletStore.save(candidate);
    }

    public static boolean isCurrency(ItemStack stack) {
        if (stack.isEmpty()) {
            return false;
        }
        return CraftingConfig.MANAGED_RESULTS.contains(BuiltInRegistries.ITEM.getKey((Object)stack.getItem()).toString());
    }

    static Item resolveCurrency(String currencyId) {
        if (!CraftingConfig.MANAGED_RESULTS.contains(currencyId)) {
            return null;
        }
        Identifier id = Identifier.tryParse((String)currencyId);
        return id != null && BuiltInRegistries.ITEM.containsKey(id) ? (Item)BuiltInRegistries.ITEM.getValue(id) : null;
    }

    static long countPhysical(ServerPlayer player, Item item) {
        long total = WalletService.count((List<ItemStack>)player.getInventory().getNonEquipmentItems(), item);
        ItemStack offhand = player.getInventory().getItem(40);
        return total + (long)(offhand.is((Object)item) ? offhand.getCount() : 0);
    }

    private static long count(List<ItemStack> stacks, Item item) {
        long total = 0L;
        for (ItemStack stack : stacks) {
            if (!stack.is((Object)item)) continue;
            total += (long)stack.getCount();
        }
        return total;
    }

    static long removePhysical(ServerPlayer player, Item item, long amount) {
        ItemStack offhand;
        long remaining = ShopService.removeFromStacks((List<ItemStack>)player.getInventory().getNonEquipmentItems(), item, amount);
        if (remaining > 0L && (offhand = player.getInventory().getItem(40)).is((Object)item)) {
            int removed = (int)Math.min(remaining, (long)offhand.getCount());
            offhand.shrink(removed);
            remaining -= (long)removed;
        }
        return remaining;
    }

    private WalletService() {
    }
}

