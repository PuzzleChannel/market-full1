package net.sundggs.avecoins.shop;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.Identifier;
import net.minecraft.server.MinecraftServer;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.sundggs.avecoins.config.CraftingConfig;
import net.sundggs.avecoins.config.ModConfigManager;

/** Server-authoritative player marketplace. All prices are VMcoin and payouts go to wallets. */
public final class PlayerMarketService {
    public static final long MAX_WALLET_BALANCE = 1728L;

    public static MarketSnapshot snapshot(ServerPlayer player) {
        PlayerMarketData data = PlayerMarketStore.get();
        MarketSnapshot snapshot = new MarketSnapshot();
        snapshot.viewerId = player.getUUID().toString();
        snapshot.shopColor = ModConfigManager.general().shopColor;
        snapshot.walletBalance = WalletStore.get().balance(player.getUUID(), CraftingConfig.CURRENCY);
        long now = System.currentTimeMillis();
        for (PlayerMarketData.Listing listing : data.listings) {
            if (listing.expiresAt > 0L && listing.expiresAt <= now
                    && !listing.sellerId.equals(snapshot.viewerId)) continue;
            MarketListing entry = new MarketListing();
            entry.id = listing.id;
            entry.sellerId = listing.sellerId;
            entry.sellerName = listing.sellerName;
            entry.itemId = listing.itemId;
            entry.displayName = listing.displayName;
            entry.quantity = listing.quantity;
            entry.unitPrice = listing.unitPrice;
            entry.totalPrice = (int) totalPrice(listing);
            entry.itemData = listing.itemData;
            entry.createdAt = listing.createdAt;
            entry.expiresAt = listing.expiresAt;
            entry.extensionDays = listing.extensionDays;
            entry.deliveryMinutes = estimateDeliveryMinutes(listing.itemId, listing.itemData, (int) totalPrice(listing));
            snapshot.listings.add(entry);
        }
        PlayerMarketData noticeCandidate = data.copy();
        for (PlayerMarketData.Delivery delivery : data.deliveries) {
            if (!delivery.buyerId.equals(snapshot.viewerId)) continue;
            MarketDelivery entry = new MarketDelivery();
            entry.id = delivery.id;
            entry.itemId = delivery.itemId;
            entry.displayName = delivery.displayName;
            entry.itemData = delivery.itemData;
            entry.quantity = delivery.quantity;
            entry.price = delivery.price;
            entry.purchasedAt = delivery.purchasedAt;
            entry.deliverAt = delivery.deliverAt;
            entry.deliveryMinutes = delivery.deliveryMinutes;
            entry.accelerationCost = accelerationCost(delivery.deliveryMinutes);
            snapshot.deliveries.add(entry);
        }
        for (PlayerMarketData.SaleNotice notice : noticeCandidate.saleNotices) {
            if (notice.sellerId.equals(snapshot.viewerId) && !notice.read) {
                MarketSaleNotice visible = new MarketSaleNotice();
                visible.id = notice.id;
                visible.itemName = notice.itemName;
                visible.quantity = notice.quantity;
                visible.price = notice.price;
                visible.soldAt = notice.soldAt;
                snapshot.saleNotices.add(visible);
            }
        }
        return snapshot;
    }

    public static void acknowledgeSaleNotices(ServerPlayer player) {
        PlayerMarketData current = PlayerMarketStore.get();
        PlayerMarketData candidate = current.copy();
        boolean changed = false;
        String viewerId = player.getUUID().toString();
        for (PlayerMarketData.SaleNotice notice : candidate.saleNotices) {
            if (notice.sellerId.equals(viewerId) && !notice.read) {
                notice.read = true;
                changed = true;
            }
        }
        if (changed) PlayerMarketStore.save(candidate);
    }

    public static String snapshotJson(ServerPlayer player) {
        return net.sundggs.avecoins.config.AtomicJsonStore.gson().toJson(snapshot(player));
    }

    public static Result createListing(ServerPlayer player, int inventorySlot, int lotPrice, int quantity) {
        return createListing(player, inventorySlot, lotPrice, quantity, 0);
    }

    /** extraDays maps to fixed listing options: 0, 1, 3, or 5 days beyond the free first day. */
    public static Result createListing(ServerPlayer player, int inventorySlot, int lotPrice, int quantity, int extraDays) {
        if (lotPrice < 1 || lotPrice > MAX_WALLET_BALANCE || quantity < 1 || quantity > 999
                || !(extraDays == 0 || extraDays == 1 || extraDays == 3 || extraDays == 5)) {
            return Result.fail("market_invalid_price_quantity");
        }
        if (inventorySlot < 0 || inventorySlot >= 36) {
            return Result.fail("market_select_item");
        }
        ItemStack held = player.getInventory().getItem(inventorySlot);
        if (held.isEmpty()) {
            return Result.fail("market_hold_item");
        }
        if (WalletService.isCurrency(held)) {
            return Result.fail("market_currency_forbidden");
        }
        if (quantity > held.getCount()) {
            return Result.fail("market_not_enough_items");
        }
        // The entered price is the total for this entire lot, not the per-item price.

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
        listing.unitPrice = Math.max(1, lotPrice / quantity);
        listing.totalPrice = lotPrice;
        listing.createdAt = System.currentTimeMillis();
        listing.extensionDays = extraDays;
        listing.expiresAt = listing.createdAt + 86400000L + (long) extraDays * 86400000L;
        candidate.listings.add(0, listing);

        long extensionCost = extensionCost(extraDays);
        WalletData walletBackup = WalletStore.get().copy();
        WalletData walletCandidate = walletBackup.copy();
        if (extensionCost > 0L && !walletCandidate.debit(player.getUUID(), CraftingConfig.CURRENCY, extensionCost)) {
            return Result.fail("market_extension_insufficient");
        }

        List<ItemStack> backup = copyInventory(player.getInventory());
        held.shrink(quantity);
        player.getInventory().setChanged();
        if (!PlayerMarketStore.save(candidate)) {
            restoreInventory(player.getInventory(), backup);
            return Result.fail("persistence_error");
        }
        if (extensionCost > 0L && !WalletStore.save(walletCandidate)) {
            PlayerMarketStore.save(current);
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
        if (listing.expiresAt > 0L && listing.expiresAt <= System.currentTimeMillis()) {
            return Result.fail("market_listing_expired");
        }
        if (listing.sellerId.equals(buyer.getUUID().toString())) {
            return Result.fail("market_cannot_buy_own");
        }
        long total = totalPrice(listing);
        if (total < 1 || total > MAX_WALLET_BALANCE) {
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
        if (current.deliveries.size() >= 500) {
            return Result.fail("market_delivery_queue_full");
        }

        WalletData walletBackup = WalletStore.get().copy();
        WalletData walletCandidate = walletBackup.copy();
        try {
            if (!walletCandidate.debit(buyer.getUUID(), CraftingConfig.CURRENCY, total)) {
                return Result.fail("market_wallet_insufficient");
            }
            walletCandidate.credit(UUID.fromString(listing.sellerId), CraftingConfig.CURRENCY, total);
        } catch (ArithmeticException | IllegalArgumentException exception) {
            return Result.fail("market_seller_wallet_limit");
        }

        PlayerMarketData marketCandidate = current.copy();
        marketCandidate.listings.removeIf(entry -> entry.id.equals(listing.id));
        int deliveryMinutes = estimateDeliveryMinutes(listing.itemId, listing.itemData, (int) total);
        PlayerMarketData.Delivery delivery = new PlayerMarketData.Delivery();
        delivery.id = UUID.randomUUID().toString();
        delivery.buyerId = buyer.getUUID().toString();
        delivery.sellerId = listing.sellerId;
        delivery.itemId = listing.itemId;
        delivery.displayName = listing.displayName;
        delivery.itemData = listing.itemData;
        delivery.quantity = listing.quantity;
        delivery.price = (int) total;
        delivery.purchasedAt = System.currentTimeMillis();
        delivery.deliveryMinutes = deliveryMinutes;
        delivery.deliverAt = delivery.purchasedAt + deliveryMinutes * 60_000L;
        marketCandidate.deliveries.add(delivery);
        PlayerMarketData.SaleNotice notice = new PlayerMarketData.SaleNotice();
        notice.id = UUID.randomUUID().toString();
        notice.sellerId = listing.sellerId;
        notice.itemName = listing.displayName;
        notice.quantity = listing.quantity;
        notice.price = (int) total;
        notice.soldAt = System.currentTimeMillis();
        marketCandidate.saleNotices.add(notice);
        if (marketCandidate.saleNotices.size() > 100) {
            marketCandidate.saleNotices.remove(0);
        }
        if (!PlayerMarketStore.save(marketCandidate)) {
            return Result.fail("persistence_error");
        }
        if (!WalletStore.save(walletCandidate)) {
            // If the wallet write fails, put the escrowed listing back. Failure to do
            // so can only lose the listing; it cannot create a duplicate item.
            PlayerMarketStore.save(current);
            return Result.fail("persistence_error");
        }
        return Result.ok("market_purchase_queued");
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
        restoreInventory(seller.getInventory(), inventoryBackup);

        PlayerMarketData candidate = current.copy();
        candidate.listings.removeIf(entry -> entry.id.equals(listing.id));
        if (!PlayerMarketStore.save(candidate)) {
            return Result.fail("persistence_error");
        }
        if (!insertCompletely(seller.getInventory(), template, listing.quantity)) {
            // Keep the item in escrow if delivery unexpectedly fails.
            PlayerMarketStore.save(current);
            restoreInventory(seller.getInventory(), inventoryBackup);
            return Result.fail("market_inventory_full");
        }
        seller.getInventory().setChanged();
        return Result.ok("market_listing_cancelled");
    }

    public static Result updatePrice(ServerPlayer seller, String listingId, int lotPrice) {
        if (lotPrice < 1 || lotPrice > MAX_WALLET_BALANCE) {
            return Result.fail("market_invalid_price_quantity");
        }
        PlayerMarketData current = PlayerMarketStore.get();
        PlayerMarketData.Listing listing = find(current, listingId);
        if (listing == null) {
            return Result.fail("market_listing_missing");
        }
        if (!listing.sellerId.equals(seller.getUUID().toString())) {
            return Result.fail("permission_denied");
        }

        long total = lotPrice;
        if (total > MAX_WALLET_BALANCE) {
            return Result.fail("market_total_too_high");
        }

        PlayerMarketData candidate = current.copy();
        PlayerMarketData.Listing editable = find(candidate, listingId);
        if (editable == null) {
            return Result.fail("market_listing_missing");
        }
        editable.unitPrice = Math.max(1, lotPrice / editable.quantity);
        editable.totalPrice = lotPrice;
        if (!PlayerMarketStore.save(candidate)) {
            return Result.fail("persistence_error");
        }
        return Result.ok("market_listing_updated");
    }

    public static int estimateDeliveryMinutes(String itemId, String itemData, int price) {
        String path = itemId == null ? "" : itemId.toLowerCase(java.util.Locale.ROOT);
        String encoded = itemData == null ? "" : itemData.toLowerCase(java.util.Locale.ROOT);
        int base;
        if (path.contains("elytra")) {
            base = 30;
        } else if ((path.contains("sword") || path.contains("pickaxe") || path.contains("axe") || path.contains("shovel")
                || path.contains("hoe") || path.contains("chestplate") || path.contains("helmet") || path.contains("leggings")
                || path.contains("boots")) && encoded.contains("enchantments")) {
            base = 30;
        } else if (path.contains("netherite") && (path.contains("sword") || path.contains("pickaxe")
                || path.contains("axe") || path.contains("shovel") || path.contains("hoe") || path.contains("chestplate"))) {
            base = 24;
        } else if (path.contains("enchanted_book")) {
            base = encoded.contains("mending") || encoded.contains("silk_touch") || encoded.contains("fortune")
                    || encoded.contains("sharpness") || encoded.contains("protection") || encoded.contains("efficiency")
                    || encoded.contains("power") || encoded.contains("unbreaking") || encoded.contains("looting") ? 30 : 18;
        } else if (path.contains("lingering_potion")) {
            base = 30;
        } else if (path.contains("splash_potion") || path.contains("tipped_arrow")) {
            base = encoded.contains("strong") || encoded.contains("long_") ? 30 : 18;
        } else if (path.contains("potion")) {
            base = encoded.contains("strong") || encoded.contains("long_") ? 22 : 16;
        } else if (path.contains("golden_carrot")) {
            base = 10;
        } else if (path.contains("golden_apple") || path.contains("totem") || path.contains("nether_star")
                || path.contains("beacon") || path.contains("shulker_box") || path.contains("trident")
                || path.contains("mace") || path.contains("heavy_core") || path.contains("dragon_egg")) {
            base = 20;
        } else if (encoded.contains("enchantments") || encoded.contains("stored_enchantments")) {
            base = 27;
        } else if (path.contains("diamond") || path.contains("emerald") || path.contains("ancient_debris")
                || path.contains("netherite") || path.contains("amethyst") || path.contains("echo_shard")
                || path.contains("music_disc") || path.contains("saddle") || path.contains("name_tag")) {
            base = 6;
        } else if (path.contains("sword") || path.contains("pickaxe") || path.contains("axe") || path.contains("shovel")
                || path.contains("hoe") || path.contains("bow") || path.contains("crossbow") || path.contains("armor")
                || path.contains("helmet") || path.contains("chestplate") || path.contains("leggings") || path.contains("boots")) {
            base = 8;
        } else if (path.contains("apple") || path.contains("bread") || path.contains("beef") || path.contains("porkchop")
                || path.contains("chicken") || path.contains("mutton") || path.contains("rabbit") || path.contains("cod")
                || path.contains("salmon") || path.contains("potato") || path.contains("carrot") || path.contains("cookie")
                || path.contains("melon_slice") || path.contains("berries") || path.contains("stew") || path.contains("soup")) {
            base = 2;
        } else if (path.contains("stone") || path.contains("dirt") || path.contains("sand") || path.contains("gravel")
                || path.contains("cobblestone") || path.contains("planks") || path.contains("log") || path.contains("wool")
                || path.contains("glass") || path.contains("brick") || path.contains("concrete") || path.contains("terracotta")
                || path.contains("torch") || path.contains("stick") || path.contains("string") || path.contains("coal")) {
            base = 1;
        } else {
            base = 4;
        }
        int priceExtra = price >= 100 ? 10 : price >= 50 ? 7 : price >= 25 ? 4 : price >= 10 ? 2 : price >= 4 ? 1 : 0;
        return Math.max(1, Math.min(30, base + priceExtra));
    }

    /**
     * Kept as a no-op for compatibility: orders must be collected manually from the
     * Active Orders screen and must never be granted automatically by a timer.
     */
    public static void processDeliveries(MinecraftServer server) {
        // Delivery timers only change an order's readiness; they do not grant items.
    }

    public static int accelerationCost(int deliveryMinutes) {
        int duration = Math.max(1, Math.min(30, deliveryMinutes));
        return 1 + ((duration - 1) * 4) / 29;
    }

    public static Result accelerateDelivery(ServerPlayer buyer, String deliveryId) {
        PlayerMarketData current = PlayerMarketStore.get();
        PlayerMarketData.Delivery found = null;
        String buyerId = buyer.getUUID().toString();
        for (PlayerMarketData.Delivery delivery : current.deliveries) {
            if (delivery.id.equals(deliveryId)) {
                found = delivery;
                break;
            }
        }
        if (found == null) return Result.fail("market_delivery_missing");
        if (!found.buyerId.equals(buyerId)) return Result.fail("permission_denied");
        long now = System.currentTimeMillis();
        if (found.deliverAt <= now) return Result.fail("market_delivery_already_ready");

        int cost = accelerationCost(found.deliveryMinutes);
        WalletData walletCurrent = WalletStore.get().copy();
        WalletData walletCandidate = walletCurrent.copy();
        if (!walletCandidate.debit(buyer.getUUID(), CraftingConfig.CURRENCY, cost)) {
            return Result.fail("market_delivery_acceleration_insufficient");
        }

        PlayerMarketData marketCandidate = current.copy();
        for (PlayerMarketData.Delivery delivery : marketCandidate.deliveries) {
            if (delivery.id.equals(deliveryId)) {
                delivery.deliverAt = now;
                break;
            }
        }
        if (!PlayerMarketStore.save(marketCandidate)) return Result.fail("persistence_error");
        if (!WalletStore.save(walletCandidate)) {
            PlayerMarketStore.save(current);
            return Result.fail("persistence_error");
        }
        return Result.ok("market_delivery_accelerated");
    }

    public static Result claimDelivery(ServerPlayer buyer, String deliveryId) {
        PlayerMarketData current = PlayerMarketStore.get();
        PlayerMarketData.Delivery delivery = null;
        String buyerId = buyer.getUUID().toString();
        for (PlayerMarketData.Delivery entry : current.deliveries) {
            if (entry.id.equals(deliveryId)) {
                delivery = entry;
                break;
            }
        }
        if (delivery == null) return Result.fail("market_delivery_missing");
        if (!delivery.buyerId.equals(buyerId)) return Result.fail("permission_denied");
        if (delivery.deliverAt > System.currentTimeMillis()) return Result.fail("market_delivery_not_ready");

        Identifier itemId = Identifier.tryParse(delivery.itemId);
        if (itemId == null || !BuiltInRegistries.ITEM.containsKey(itemId)) {
            return Result.fail("market_item_unavailable");
        }
        Item expected = (Item) BuiltInRegistries.ITEM.getValue(itemId);
        ItemStack template = ShopItemStacks.decodeExpected(buyer.level().registryAccess(), delivery.itemData, expected)
                .orElse(ItemStack.EMPTY);
        if (template.isEmpty()) return Result.fail("market_item_unavailable");

        // Attempt to place the complete quantity and roll back if all items do not fit.
        // This handles both empty slots and compatible partially-filled stacks.
        List<ItemStack> inventoryBackup = copyInventory(buyer.getInventory());
        if (!insertCompletely(buyer.getInventory(), template, delivery.quantity)) {
            restoreInventory(buyer.getInventory(), inventoryBackup);
            return Result.fail("market_delivery_inventory_full");
        }

        PlayerMarketData marketCandidate = current.copy();
        marketCandidate.deliveries.removeIf(entry -> entry.id.equals(deliveryId));
        if (!PlayerMarketStore.save(marketCandidate)) {
            restoreInventory(buyer.getInventory(), inventoryBackup);
            return Result.fail("persistence_error");
        }
        buyer.getInventory().setChanged();
        return Result.ok("market_delivery_claimed");
    }

    public static long extensionCost(int extraDays) {
        return switch (extraDays) {
            case 0 -> 0L;
            case 1 -> 2L;
            case 3 -> 4L;
            case 5 -> 6L;
            default -> Long.MAX_VALUE;
        };
    }

    private static long totalPrice(PlayerMarketData.Listing listing) {
        return listing.totalPrice > 0 ? listing.totalPrice : (long) listing.unitPrice * listing.quantity;
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
        public String shopColor = "";
        public long walletBalance;
        public List<MarketListing> listings = new ArrayList<>();
        public List<MarketSaleNotice> saleNotices = new ArrayList<>();
        public List<MarketDelivery> deliveries = new ArrayList<>();
    }

    public static final class MarketSaleNotice {
        public String id = "";
        public String itemName = "";
        public int quantity;
        public int price;
        public long soldAt;
    }

    public static final class MarketDelivery {
        public String id = "";
        public String itemId = "";
        public String displayName = "";
        public String itemData = "";
        public int quantity;
        public int price;
        public long purchasedAt;
        public long deliverAt;
        public int deliveryMinutes;
        public int accelerationCost;
    }

    public static final class MarketListing {
        public String id = "";
        public String sellerId = "";
        public String sellerName = "";
        public String itemId = "";
        public String displayName = "";
        public int quantity;
        /** Server-calculated price for the full lot, including migrated legacy listings. */
        public int totalPrice;
        /** Retained for compatibility with older saved listings. */
        public int unitPrice;
        public String itemData = "";
        public long createdAt;
        public long expiresAt;
        public int extensionDays;
        public int deliveryMinutes;
    }

    public record Result(boolean success, String code) {
        public static Result ok(String code) { return new Result(true, code); }
        public static Result fail(String code) { return new Result(false, code); }
    }

    private PlayerMarketService() {
    }
}
