/*
 * Decompiled with CFR 0.152.
 */
package net.sundggs.avecoins.shop;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.Identifier;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.SimpleContainer;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.ItemLike;
import net.sundggs.avecoins.config.CraftingConfig;
import net.sundggs.avecoins.shop.ShopService;
import net.sundggs.avecoins.shop.WalletData;
import net.sundggs.avecoins.shop.WalletStore;

public final class WalletService {
    /**
     * Converts obsolete AVECOINS coin stacks already in a player's inventory
     * into the server's single VMcoin currency. The conversion is atomic: if
     * any stack cannot be inserted, the original inventory is restored.
     */
    public static boolean migrateLegacyCoins(ServerPlayer player) {
        if (player == null) {
            return false;
        }
        Item currency = WalletService.resolveCurrency(CraftingConfig.CURRENCY);
        if (currency == null) {
            return false;
        }
        Inventory inventory = player.getInventory();
        ArrayList<ItemStack> backup = new ArrayList<ItemStack>(inventory.getContainerSize());
        for (int slot = 0; slot < inventory.getContainerSize(); ++slot) {
            backup.add(inventory.getItem(slot).copy());
        }
        boolean changed = false;
        for (int slot = 0; slot < inventory.getContainerSize(); ++slot) {
            ItemStack oldStack = inventory.getItem(slot);
            if (oldStack.isEmpty()) {
                continue;
            }
            String itemId = BuiltInRegistries.ITEM.getKey((Object)oldStack.getItem()).toString();
            if (!CraftingConfig.LEGACY_COINS.contains(itemId)) {
                continue;
            }
            int remaining = oldStack.getCount();
            oldStack.shrink(remaining);
            changed = true;
            while (remaining > 0) {
                int amount = Math.min(remaining, currency.getDefaultMaxStackSize());
                ItemStack replacement = new ItemStack((ItemLike)currency, amount);
                if (!inventory.add(replacement) || !replacement.isEmpty()) {
                    for (int restore = 0; restore < backup.size(); ++restore) {
                        inventory.setItem(restore, backup.get(restore).copy());
                    }
                    inventory.setChanged();
                    return false;
                }
                remaining -= amount;
            }
        }
        if (changed) {
            inventory.setChanged();
            player.containerMenu.broadcastChanges();
        }
        return changed;
    }

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

