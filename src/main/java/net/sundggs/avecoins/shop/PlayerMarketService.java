package net.sundggs.avecoins.shop;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.Identifier;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.sundggs.avecoins.config.CraftingConfig;

/** Server-authoritative player marketplace. All prices are VMcoin and payouts go to wallets. */
public final class PlayerMarketService {
    public static final long MAX_WALLET_BALANCE = 1728L;

    public static MarketSnapshot snapshot(ServerPlayer player) {
        PlayerMarketData data = PlayerMarketStore.get();
        MarketSnapshot snapshot = new MarketSnapshot();
        snapshot.viewerId = player.getUUID().toString();
        snapshot.walletBalance = WalletStore.get().balance(player.getUUID(), CraftingConfig.CURRENCY);
        for (PlayerMarketData.Listing listing : data.listings) {
            MarketListing entry = new MarketListing();
            entry.id = listing.id;
            entry.sellerId = listing.sellerId;
            entry.sellerName = listing.sellerName;
            entry.itemId = listing.itemId;
            entry.displayName = listing.displayName;
            entry.quantity = listing.quantity;
            entry.unitPrice = listing.unitPrice;
            entry.createdAt = listing.createdAt;
            snapshot.listings.add(entry);
        }
        return snapshot;
    }

    public static String snapshotJson(ServerPlayer player) {
        return net.sundggs.avecoins.config.AtomicJsonStore.gson().toJson(snapshot(player));
    }

    public static Result createListing(ServerPlayer player, int unitPrice, int quantity) {
        if (unitPrice < 1 || unitPrice > MAX_WALLET_BALANCE || quantity < 1 || quantity > 999) {
            return Result.fail("market_invalid_price_quantity");
        }
        ItemStack held = player.getMainHandItem();
        if (held.isEmpty()) {
            return Result.fail("market_hold_item");
        }
        if (WalletService.isCurrency(held)) {
            return Result.fail("market_currency_forbidden");
        }
        if (quantity > held.getCount()) {
            return Result.fail("market_not_enough_items");
        }
        long total;
        try {
            total = Math.multiplyExact((long) unitPrice, (long) quantity);
        } catch (ArithmeticException exception) {
            return Result.fail("market_invalid_price_quantity");
        }
        if (total > MAX_WALLET_BALANCE) {
            return Result.fail("market_total_too_high");
        }

        String itemData = ShopItemStacks.encode(player.level().registryAccess(), held);
        if (itemData.isBlank() || itemData.getBytes(java.nio.charset.StandardCharsets.UTF_8).length > PlayerMarketData.MAX_ITEM_DATA_BYTES) {
            return Result.fail("market_item_data_too_large");
        }
        String itemId = BuiltInRegistries.ITEM.getKey(held.getItem()).toString();
        PlayerMarketData current = PlayerMarketStore.get();
        if (current.listings.size() >= PlayerMarketData.MAX_LISTINGS) {
            return Result.fail("market_full");
        }

        PlayerMarketData candidate = current.copy();
        PlayerMarketData.Listing listing = new PlayerMarketData.Listing();
        listing.id = UUID.randomUUID().toString();
        listing.sellerId = player.getUUID().toString();
        listing.sellerName = player.getScoreboardName();
        listing.displayName = sanitizeDisplayName(held.getHoverName().getString());
        listing.itemId = itemId;
        listing.itemData = itemData;
        listing.quantity = quantity;
        listing.unitPrice = unitPrice;
        listing.createdAt = System.currentTimeMillis();
        candidate.listings.add(0, listing);

        List<ItemStack> backup = copyInventory(player.getInventory());
        held.shrink(quantity);
        player.getInventory().setChanged();
        if (!PlayerMarketStore.save(candidate)) {
            restoreInventory(player.getInventory(), backup);
            return Result.fail("persistence_error");
        }
        return Result.ok("market_listing_created");
    }

    public static Result purchase(ServerPlayer buyer, String listingId) {
        PlayerMarketData current = PlayerMarketStore.get();
        PlayerMarketData.Listing listing = find(current, listingId);
        if (listing == null) {
            return Result.fail("market_listing_missing");
        }
        if (listing.sellerId.equals(buyer.getUUID().toString())) {
            return Result.fail("market_cannot_buy_own");
        }
        long total;
        try {
            total = Math.multiplyExact((long) listing.unitPrice, (long) listing.quantity);
        } catch (ArithmeticException exception) {
            return Result.fail("market_invalid_price_quantity");
        }

        Identifier expectedId = Identifier.tryParse(listing.itemId);
        if (expectedId == null || !BuiltInRegistries.ITEM.containsKey(expectedId)) {
            return Result.fail("market_item_unavailable");
        }
        Item expectedItem = (Item) BuiltInRegistries.ITEM.getValue(expectedId);
        ItemStack template = ShopItemStacks.decodeExpected(buyer.level().registryAccess(), listing.itemData, expectedItem)
                .orElse(ItemStack.EMPTY);
        if (template.isEmpty()) {
            return Result.fail("market_item_unavailable");
        }
        ItemStack delivery = template.copyWithCount(listing.quantity);
        List<ItemStack> inventoryBackup = copyInventory(buyer.getInventory());
        if (!insertCompletely(buyer.getInventory(), template, listing.quantity)) {
            restoreInventory(buyer.getInventory(), inventoryBackup);
            return Result.fail("market_inventory_full");
        }

        WalletData walletBackup = WalletStore.get().copy();
        WalletData walletCandidate = walletBackup.copy();
        try {
            if (!walletCandidate.debit(buyer.getUUID(), CraftingConfig.CURRENCY, total)) {
                restoreInventory(buyer.getInventory(), inventoryBackup);
                return Result.fail("market_wallet_insufficient");
            }
            walletCandidate.credit(UUID.fromString(listing.sellerId), CraftingConfig.CURRENCY, total);
        } catch (ArithmeticException | IllegalArgumentException exception) {
            restoreInventory(buyer.getInventory(), inventoryBackup);
            return Result.fail("market_seller_wallet_limit");
        }

        PlayerMarketData marketCandidate = current.copy();
        marketCandidate.listings.removeIf(entry -> entry.id.equals(listing.id));
        if (!WalletStore.save(walletCandidate)) {
            restoreInventory(buyer.getInventory(), inventoryBackup);
            return Result.fail("persistence_error");
        }
        if (!PlayerMarketStore.save(marketCandidate)) {
            WalletStore.save(walletBackup);
            restoreInventory(buyer.getInventory(), inventoryBackup);
            return Result.fail("persistence_error");
        }
        buyer.getInventory().setChanged();
        return Result.ok("market_purchase_completed");
    }

    public static Result cancel(ServerPlayer seller, String listingId) {
        PlayerMarketData current = PlayerMarketStore.get();
        PlayerMarketData.Listing listing = find(current, listingId);
        if (listing == null) {
            return Result.fail("market_listing_missing");
        }
        if (!listing.sellerId.equals(seller.getUUID().toString())) {
            return Result.fail("permission_denied");
        }
        Identifier expectedId = Identifier.tryParse(listing.itemId);
        if (expectedId == null || !BuiltInRegistries.ITEM.containsKey(expectedId)) {
            return Result.fail("market_item_unavailable");
        }
        Item expectedItem = (Item) BuiltInRegistries.ITEM.getValue(expectedId);
        ItemStack template = ShopItemStacks.decodeExpected(seller.level().registryAccess(), listing.itemData, expectedItem)
                .orElse(ItemStack.EMPTY);
        if (template.isEmpty()) {
            return Result.fail("market_item_unavailable");
        }

        List<ItemStack> inventoryBackup = copyInventory(seller.getInventory());
        if (!insertCompletely(seller.getInventory(), template, listing.quantity)) {
            restoreInventory(seller.getInventory(), inventoryBackup);
            return Result.fail("market_inventory_full");
        }
        PlayerMarketData candidate = current.copy();
        candidate.listings.removeIf(entry -> entry.id.equals(listing.id));
        if (!PlayerMarketStore.save(candidate)) {
            restoreInventory(seller.getInventory(), inventoryBackup);
            return Result.fail("persistence_error");
        }
        seller.getInventory().setChanged();
        return Result.ok("market_listing_cancelled");
    }

    private static PlayerMarketData.Listing find(PlayerMarketData data, String listingId) {
        if (listingId == null || listingId.length() > 40) return null;
        return data.listings.stream().filter(entry -> entry.id.equals(listingId)).findFirst().orElse(null);
    }

    private static String sanitizeDisplayName(String source) {
        String normalized = source == null ? "" : source.replace('\n', ' ').replace('\r', ' ').trim();
        if (normalized.isBlank()) return "Item";
        return normalized.length() > 128 ? normalized.substring(0, 128) : normalized;
    }

    private static List<ItemStack> copyInventory(Inventory inventory) {
        ArrayList<ItemStack> backup = new ArrayList<>(inventory.getContainerSize());
        for (int slot = 0; slot < inventory.getContainerSize(); slot++) {
            backup.add(inventory.getItem(slot).copy());
        }
        return backup;
    }

    private static void restoreInventory(Inventory inventory, List<ItemStack> backup) {
        for (int slot = 0; slot < backup.size(); slot++) {
            inventory.setItem(slot, backup.get(slot).copy());
        }
        inventory.setChanged();
    }

    private static boolean insertCompletely(Inventory inventory, ItemStack template, int amount) {
        int remaining = amount;
        int max = Math.max(1, template.getMaxStackSize());
        while (remaining > 0) {
            int part = Math.min(remaining, max);
            ItemStack stack = template.copyWithCount(part);
            if (!inventory.add(stack) || !stack.isEmpty()) {
                return false;
            }
            remaining -= part;
        }
        return true;
    }

    public static final class MarketSnapshot {
        public String viewerId = "";
        public long walletBalance;
        public List<MarketListing> listings = new ArrayList<>();
    }

    public static final class MarketListing {
        public String id = "";
        public String sellerId = "";
        public String sellerName = "";
        public String itemId = "";
        public String displayName = "";
        public int quantity;
        public int unitPrice;
        public long createdAt;
    }

    public record Result(boolean success, String code) {
        static Result ok(String code) { return new Result(true, code); }
        static Result fail(String code) { return new Result(false, code); }
    }

    private PlayerMarketService() {
    }
}
