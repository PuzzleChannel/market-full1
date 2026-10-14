package net.sundggs.avecoins.shop;

import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.UUID;
import net.minecraft.resources.Identifier;

/** Persistent escrow records for the player-to-player market. */
public final class PlayerMarketData {
    public static final int SCHEMA_VERSION = 1;
    public static final int MAX_LISTINGS = 40;
    public static final int MAX_ITEM_DATA_BYTES = 16384;

    public int schemaVersion = SCHEMA_VERSION;
    public long revision;
    public List<Listing> listings = new ArrayList<>();
    public List<SaleNotice> saleNotices = new ArrayList<>();

    public static PlayerMarketData defaults() {
        return new PlayerMarketData();
    }

    public static PlayerMarketData validate(PlayerMarketData input) {
        if (input == null) {
            throw new IllegalArgumentException("Market data is missing");
        }
        if (input.listings == null || input.listings.size() > MAX_LISTINGS) {
            throw new IllegalArgumentException("Invalid market listing count");
        }

        PlayerMarketData output = new PlayerMarketData();
        output.revision = Math.max(0L, input.revision);
        Set<String> ids = new HashSet<>();
        for (Listing source : input.listings) {
            if (source == null) {
                throw new IllegalArgumentException("Null market listing");
            }
            Listing listing = new Listing();
            listing.id = canonicalUuid(source.id, "listing id");
            if (!ids.add(listing.id)) {
                throw new IllegalArgumentException("Duplicate market listing id");
            }
            listing.sellerId = canonicalUuid(source.sellerId, "seller id");
            listing.sellerName = bounded(source.sellerName, 32, "seller name");
            listing.displayName = bounded(source.displayName, 128, "display name");
            listing.itemId = bounded(source.itemId, 128, "item id");
            Identifier parsedItemId = Identifier.tryParse(listing.itemId);
            if (parsedItemId == null || !parsedItemId.toString().equals(listing.itemId)) {
                throw new IllegalArgumentException("Invalid item id");
            }
            if (source.itemData == null || source.itemData.isBlank()
                    || source.itemData.getBytes(StandardCharsets.UTF_8).length > MAX_ITEM_DATA_BYTES) {
                throw new IllegalArgumentException("Invalid serialized item");
            }
            listing.itemData = source.itemData;
            if (source.quantity < 1 || source.quantity > 999) {
                throw new IllegalArgumentException("Invalid market quantity");
            }
            if (source.unitPrice < 1 || source.unitPrice > 1728) {
                throw new IllegalArgumentException("Invalid market price");
            }
            if (source.totalPrice < 0 || source.totalPrice > 1728) {
                throw new IllegalArgumentException("Invalid market total price");
            }
            long total;
            if (source.totalPrice > 0) {
                total = source.totalPrice;
            } else {
                // A missing totalPrice means this is a legacy per-item-price listing.
                try {
                    total = Math.multiplyExact((long) source.quantity, (long) source.unitPrice);
                } catch (ArithmeticException exception) {
                    throw new IllegalArgumentException("Market total overflow", exception);
                }
            }
            if (total < 1L || total > 1728L) {
                throw new IllegalArgumentException("Market listing total exceeds wallet limit");
            }
            listing.quantity = source.quantity;
            listing.unitPrice = source.unitPrice;
            listing.totalPrice = source.totalPrice;
            listing.createdAt = Math.max(0L, source.createdAt);
            listing.expiresAt = source.expiresAt > 0L ? source.expiresAt : (listing.createdAt > 0L ? listing.createdAt + 86400000L : 0L);
            listing.extensionDays = Math.max(0, Math.min(10, source.extensionDays));
            output.listings.add(listing);
        }
        if (input.saleNotices != null) {
            int start = Math.max(0, input.saleNotices.size() - 100);
            for (int i = start; i < input.saleNotices.size(); i++) {
                SaleNotice source = input.saleNotices.get(i);
                if (source == null || source.sellerId == null || source.itemName == null) continue;
                try {
                    SaleNotice notice = new SaleNotice();
                    notice.id = canonicalUuid(source.id, "sale notice id");
                    notice.sellerId = canonicalUuid(source.sellerId, "sale notice seller id");
                    notice.itemName = bounded(source.itemName, 128, "sale item name");
                    notice.quantity = Math.max(1, Math.min(999, source.quantity));
                    notice.price = Math.max(1, Math.min(1728, source.price));
                    notice.soldAt = Math.max(0L, source.soldAt);
                    notice.read = source.read;
                    output.saleNotices.add(notice);
                } catch (IllegalArgumentException ignored) {
                    // Discard only the malformed notice; preserve valid market listings.
                }
            }
        }
        return output;
    }

    public PlayerMarketData copy() {
        PlayerMarketData copy = new PlayerMarketData();
        copy.revision = this.revision;
        for (Listing source : this.listings) {
            Listing listing = new Listing();
            listing.id = source.id;
            listing.sellerId = source.sellerId;
            listing.sellerName = source.sellerName;
            listing.displayName = source.displayName;
            listing.itemId = source.itemId;
            listing.itemData = source.itemData;
            listing.quantity = source.quantity;
            listing.unitPrice = source.unitPrice;
            listing.totalPrice = source.totalPrice;
            listing.createdAt = source.createdAt;
            listing.expiresAt = source.expiresAt;
            listing.extensionDays = source.extensionDays;
            copy.listings.add(listing);
        }
        for (SaleNotice source : this.saleNotices) {
            SaleNotice notice = new SaleNotice();
            notice.id = source.id;
            notice.sellerId = source.sellerId;
            notice.itemName = source.itemName;
            notice.quantity = source.quantity;
            notice.price = source.price;
            notice.soldAt = source.soldAt;
            notice.read = source.read;
            copy.saleNotices.add(notice);
        }
        return copy;
    }

    public static final class SaleNotice {
        public String id = "";
        public String sellerId = "";
        public String itemName = "";
        public int quantity = 1;
        public int price = 1;
        public long soldAt;
        public boolean read;
    }

    private static String canonicalUuid(String value, String label) {
        try {
            return UUID.fromString(value).toString();
        } catch (RuntimeException exception) {
            throw new IllegalArgumentException("Invalid " + label, exception);
        }
    }

    private static String bounded(String value, int max, String label) {
        String normalized = value == null ? "" : value.trim();
        if (normalized.isBlank() || normalized.length() > max || normalized.indexOf('\n') >= 0 || normalized.indexOf('\r') >= 0) {
            throw new IllegalArgumentException("Invalid " + label);
        }
        return normalized;
    }

    public static final class Listing {
        public String id = "";
        public String sellerId = "";
        public String sellerName = "";
        public String displayName = "";
        public String itemId = "minecraft:stone";
        /** Encodes one item with its data/components; quantity is stored separately. */
        public String itemData = "";
        public int quantity = 1;
        /** Legacy per-item price. Zero totalPrice means this field still defines the price. */
        public int unitPrice = 1;
        /** Fixed price for the entire listing; zero is reserved for backward-compatible legacy entries. */
        public int totalPrice;
        public long createdAt;
        /** Expiration timestamp; first 24 hours are free. */
        public long expiresAt;
        /** Paid extra days beyond the free first day. */
        public int extensionDays;
    }
}
