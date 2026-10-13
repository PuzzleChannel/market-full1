/*
 * Decompiled with CFR 0.152.
 */
package net.sundggs.avecoins.shop;

import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.regex.Pattern;
import net.sundggs.avecoins.config.CraftingConfig;

public final class ShopData {
    public static final int SCHEMA_VERSION = 3;
    public static final int MAX_CATEGORIES = 64;
    public static final int MAX_PRODUCTS = 512;
    public static final int MAX_HISTORY = 200;
    public static final int MAX_ITEM_DATA = 32768;
    public static final int MAX_TOTAL_ITEM_DATA = 131072;
    private static final Pattern STABLE_ID = Pattern.compile("[a-z0-9_-]{1,48}");
    private static final Pattern RESOURCE_ID = Pattern.compile("[a-z0-9_.-]+:[a-z0-9/._-]+");
    public int schemaVersion = 3;
    public long revision = 0L;
    public List<Category> categories = new ArrayList<Category>();
    public List<Product> products = new ArrayList<Product>();
    public List<PurchaseRecord> history = new ArrayList<PurchaseRecord>();

    public static ShopData defaults() {
        return new ShopData();
    }

    public static ShopData validate(ShopData input) {
        ShopData output = new ShopData();
        output.schemaVersion = 3;
        output.revision = Math.max(0L, input.revision);
        if (input.categories == null || input.categories.size() > 64) {
            throw new IllegalArgumentException("Too many shop categories");
        }
        if (input.products == null || input.products.size() > 512) {
            throw new IllegalArgumentException("Too many shop products");
        }
        LinkedHashSet<String> categoryIds = new LinkedHashSet<String>();
        for (Category source : input.categories) {
            if (source == null) {
                throw new IllegalArgumentException("Shop categories cannot be null");
            }
            Category category = new Category();
            category.id = ShopData.stableId(source.id, "category");
            if (!categoryIds.add(category.id)) {
                throw new IllegalArgumentException("Duplicate category: " + category.id);
            }
            category.name = ShopData.bounded(source.name, category.id, 48);
            category.description = ShopData.bounded(source.description, "", 160);
            category.iconItem = ShopData.itemId(source.iconItem, "minecraft:chest");
            category.sortOrder = ShopData.boundedInt(source.sortOrder, 0, 10000, "category order");
            output.categories.add(category);
        }
        LinkedHashSet<String> productIds = new LinkedHashSet<String>();
        LinkedHashSet<String> sellFingerprints = new LinkedHashSet<String>();
        int totalItemData = 0;
        for (Product source : input.products) {
            if (source == null) {
                throw new IllegalArgumentException("Shop products cannot be null");
            }
            Product product = new Product();
            product.id = ShopData.stableId(source.id, "product");
            if (!productIds.add(product.id)) {
                throw new IllegalArgumentException("Duplicate product: " + product.id);
            }
            product.categoryId = ShopData.stableId(source.categoryId, "category");
            if (!categoryIds.contains(product.categoryId)) {
                throw new IllegalArgumentException("Unknown product category: " + product.categoryId);
            }
            product.name = ShopData.bounded(source.name, product.id, 64);
            product.itemId = ShopData.itemId(source.itemId, null);
            product.itemData = ShopData.bounded(source.itemData, "", 32768);
            int itemDataBytes = product.itemData.getBytes(StandardCharsets.UTF_8).length;
            if (itemDataBytes > 32768) {
                throw new IllegalArgumentException("Product item data exceeds the per-item limit");
            }
            if ((totalItemData = Math.addExact(totalItemData, itemDataBytes)) > 131072) {
                throw new IllegalArgumentException("Shop item data exceeds the catalogue limit");
            }
            product.itemCount = ShopData.boundedInt(source.itemCount, 1, 64, "product item count");
            product.currencyId = CraftingConfig.CURRENCY;
            product.price = ShopData.boundedInt(source.price, 1, 1000000000, "product price");
            product.discountPrice = source.discountPrice;
            if (product.discountPrice != null && (product.discountPrice < 1 || product.discountPrice >= product.price)) {
                throw new IllegalArgumentException("Discount price must be lower than the normal price");
            }
            if (source.stock < -1 || source.stock > 1000000000) {
                throw new IllegalArgumentException("Product stock is out of range");
            }
            product.stock = source.stock;
            product.enabled = source.enabled;
            product.sellEnabled = source.sellEnabled;
            Integer n = product.sellingPrice = product.sellEnabled ? source.sellingPrice : null;
            if (product.sellingPrice != null && (product.sellingPrice < 1 || product.sellingPrice > product.price)) {
                throw new IllegalArgumentException("Selling price must not exceed the normal price");
            }
            if (product.sellEnabled && !sellFingerprints.add(product.itemId + "\u0000" + product.itemData)) {
                throw new IllegalArgumentException("Duplicate sellable product item: " + product.itemId);
            }
            product.sortOrder = ShopData.boundedInt(source.sortOrder, 0, 10000, "product order");
            output.products.add(product);
        }
        if (input.history != null) {
            int start;
            for (int index = start = Math.max(0, input.history.size() - 200); index < input.history.size(); ++index) {
                PurchaseRecord record = input.history.get(index);
                if (record == null) continue;
                output.history.add(ShopData.validateRecord(record));
            }
        }
        return output;
    }

    public ShopData copy() {
        ShopData copy = new ShopData();
        copy.schemaVersion = this.schemaVersion;
        copy.revision = this.revision;
        this.categories.forEach(value -> copy.categories.add(ShopData.copyCategory(value)));
        this.products.forEach(value -> copy.products.add(ShopData.copyProduct(value)));
        this.history.forEach(value -> copy.history.add(ShopData.copyRecord(value)));
        return copy;
    }

    public Product product(String id) {
        return this.products.stream().filter(product -> product.id.equals(id)).findFirst().orElse(null);
    }

    private static PurchaseRecord validateRecord(PurchaseRecord source) {
        PurchaseRecord output = new PurchaseRecord();
        output.transactionId = ShopData.bounded(source.transactionId, "unknown", 64);
        output.timestamp = Math.max(0L, source.timestamp);
        output.playerUuid = ShopData.bounded(source.playerUuid, "unknown", 64);
        output.playerName = ShopData.bounded(source.playerName, "unknown", 32);
        output.success = source.success;
        output.reason = ShopData.bounded(source.reason, "", 160);
        output.type = source.type == null ? TransactionType.PURCHASE : source.type;
        output.paymentDestination = source.paymentDestination == null ? PaymentDestination.INVENTORY : source.paymentDestination;
        output.lines = new ArrayList<PurchaseLine>();
        if (source.lines != null) {
            for (PurchaseLine purchaseLine : source.lines.stream().limit(64L).toList()) {
                if (purchaseLine == null) continue;
                PurchaseLine line = new PurchaseLine();
                line.productId = ShopData.bounded(purchaseLine.productId, "unknown", 48);
                line.productName = ShopData.bounded(purchaseLine.productName, "unknown", 64);
                line.itemId = ShopData.itemId(purchaseLine.itemId, "minecraft:air");
                line.quantity = ShopData.boundedInt(purchaseLine.quantity, 0, 4096, "history quantity");
                line.itemCount = ShopData.boundedInt(purchaseLine.itemCount, 1, 64, "history item count");
                line.currencyId = CraftingConfig.normalizeCurrencyId(purchaseLine.currencyId);
                line.unitPrice = ShopData.boundedInt(purchaseLine.unitPrice, 0, 1000000000, "history unit price");
                if (purchaseLine.lineTotal < 0L) {
                    throw new IllegalArgumentException("History total cannot be negative");
                }
                line.lineTotal = purchaseLine.lineTotal;
                output.lines.add(line);
            }
        }
        output.totals = new LinkedHashMap<String, Long>();
        if (source.totals != null) {
            for (Map.Entry entry : source.totals.entrySet()) {
                String currency = CraftingConfig.normalizeCurrencyId((String)entry.getKey());
                if (entry.getValue() == null || (Long)entry.getValue() < 0L) {
                    throw new IllegalArgumentException("Invalid history currency total");
                }
                output.totals.merge(currency, (Long)entry.getValue(), Math::addExact);
            }
        }
        output.inventoryDebits = ShopData.validateCurrencyAmounts(source.inventoryDebits, "inventory debit");
        output.walletDebits = ShopData.validateCurrencyAmounts(source.walletDebits, "wallet debit");
        output.inventoryChange = ShopData.validateCurrencyAmounts(source.inventoryChange, "inventory change");
        output.walletChange = ShopData.validateCurrencyAmounts(source.walletChange, "wallet change");
        output.conversions = new ArrayList<ConversionRecord>();
        if (source.conversions != null) {
            for (ConversionRecord conversionRecord : source.conversions.stream().limit(64L).toList()) {
                if (conversionRecord == null) continue;
                ConversionRecord conversion = new ConversionRecord();
                conversion.sourceCurrency = ShopData.coinId(conversionRecord.sourceCurrency);
                conversion.targetCurrency = ShopData.coinId(conversionRecord.targetCurrency);
                if (conversionRecord.sourceAmount < 0L || conversionRecord.targetAmount < 0L) {
                    throw new IllegalArgumentException("History conversion amounts cannot be negative");
                }
                conversion.sourceAmount = conversionRecord.sourceAmount;
                conversion.targetAmount = conversionRecord.targetAmount;
                conversion.location = ShopData.bounded(conversionRecord.location, "INVENTORY", 16);
                if (!conversion.location.equals("INVENTORY") && !conversion.location.equals("WALLET")) {
                    throw new IllegalArgumentException("Invalid history conversion location");
                }
                output.conversions.add(conversion);
            }
        }
        return output;
    }

    private static Map<String, Long> validateCurrencyAmounts(Map<String, Long> source, String label) {
        LinkedHashMap<String, Long> output = new LinkedHashMap<String, Long>();
        if (source == null) {
            return output;
        }
        for (Map.Entry<String, Long> entry : source.entrySet()) {
            String currency = CraftingConfig.normalizeCurrencyId(entry.getKey());
            if (entry.getValue() == null || entry.getValue() < 0L) {
                throw new IllegalArgumentException("Invalid history " + label);
            }
            if (entry.getValue() <= 0L) continue;
            output.merge(currency, entry.getValue(), Math::addExact);
        }
        return output;
    }

    private static String coinId(String value) {
        return CraftingConfig.normalizeCurrencyId(value);
    }

    private static Category copyCategory(Category source) {
        Category copy = new Category();
        copy.id = source.id;
        copy.name = source.name;
        copy.description = source.description;
        copy.iconItem = source.iconItem;
        copy.sortOrder = source.sortOrder;
        return copy;
    }

    private static Product copyProduct(Product source) {
        Product copy = new Product();
        copy.id = source.id;
        copy.categoryId = source.categoryId;
        copy.name = source.name;
        copy.itemId = source.itemId;
        copy.itemData = source.itemData;
        copy.itemCount = source.itemCount;
        copy.currencyId = source.currencyId;
        copy.price = source.price;
        copy.discountPrice = source.discountPrice;
        copy.stock = source.stock;
        copy.enabled = source.enabled;
        copy.sellEnabled = source.sellEnabled;
        copy.sellingPrice = source.sellingPrice;
        copy.sortOrder = source.sortOrder;
        return copy;
    }

    private static PurchaseRecord copyRecord(PurchaseRecord source) {
        PurchaseRecord copy = new PurchaseRecord();
        copy.transactionId = source.transactionId;
        copy.timestamp = source.timestamp;
        copy.playerUuid = source.playerUuid;
        copy.playerName = source.playerName;
        copy.success = source.success;
        copy.reason = source.reason;
        copy.type = source.type;
        copy.paymentDestination = source.paymentDestination;
        copy.totals = new LinkedHashMap<String, Long>(source.totals);
        copy.inventoryDebits = new LinkedHashMap<String, Long>(source.inventoryDebits);
        copy.walletDebits = new LinkedHashMap<String, Long>(source.walletDebits);
        copy.inventoryChange = new LinkedHashMap<String, Long>(source.inventoryChange);
        copy.walletChange = new LinkedHashMap<String, Long>(source.walletChange);
        source.conversions.forEach(value -> copy.conversions.add(ShopData.copyConversion(value)));
        source.lines.forEach(value -> copy.lines.add(ShopData.copyLine(value)));
        return copy;
    }

    private static ConversionRecord copyConversion(ConversionRecord source) {
        ConversionRecord copy = new ConversionRecord();
        copy.sourceCurrency = source.sourceCurrency;
        copy.targetCurrency = source.targetCurrency;
        copy.sourceAmount = source.sourceAmount;
        copy.targetAmount = source.targetAmount;
        copy.location = source.location;
        return copy;
    }

    private static PurchaseLine copyLine(PurchaseLine source) {
        PurchaseLine copy = new PurchaseLine();
        copy.productId = source.productId;
        copy.productName = source.productName;
        copy.itemId = source.itemId;
        copy.quantity = source.quantity;
        copy.itemCount = source.itemCount;
        copy.currencyId = source.currencyId;
        copy.unitPrice = source.unitPrice;
        copy.lineTotal = source.lineTotal;
        return copy;
    }

    private static String stableId(String value, String label) {
        String normalized;
        String string = normalized = value == null ? "" : value.trim().toLowerCase(Locale.ROOT);
        if (!STABLE_ID.matcher(normalized).matches()) {
            throw new IllegalArgumentException("Invalid " + label + " id: " + normalized);
        }
        return normalized;
    }

    private static String itemId(String value, String fallback) {
        String normalized;
        String string = normalized = value == null || value.isBlank() ? fallback : value.trim();
        if (normalized == null || normalized.length() > 128 || !RESOURCE_ID.matcher(normalized).matches()) {
            throw new IllegalArgumentException("Invalid item id: " + normalized);
        }
        return normalized;
    }

    private static String bounded(String value, String fallback, int max) {
        String normalized;
        String string = normalized = value == null ? fallback : value.trim();
        if (normalized == null || normalized.length() > max) {
            throw new IllegalArgumentException("Text exceeds maximum length " + max);
        }
        return normalized;
    }

    private static int boundedInt(int value, int min, int max, String label) {
        if (value < min || value > max) {
            throw new IllegalArgumentException(label + " is out of range");
        }
        return value;
    }

    public static final class Category {
        public String id = "";
        public String name = "";
        public String description = "";
        public String iconItem = "minecraft:chest";
        public int sortOrder = 0;
    }

    public static final class Product {
        public String id = "";
        public String categoryId = "";
        public String name = "";
        public String itemId = "minecraft:stone";
        public String itemData = "";
        public int itemCount = 1;
        public String currencyId = CraftingConfig.CURRENCY;
        public int price = 1;
        public Integer discountPrice = null;
        public int stock = -1;
        public boolean enabled = true;
        public boolean sellEnabled = false;
        public Integer sellingPrice = null;
        public int sortOrder = 0;

        public int effectivePrice() {
            return this.discountPrice == null ? this.price : this.discountPrice;
        }

        public int effectiveSellingPrice() {
            return this.sellingPrice == null ? this.effectivePrice() : this.sellingPrice.intValue();
        }
    }

    public static final class PurchaseRecord {
        public String transactionId = "";
        public long timestamp = 0L;
        public String playerUuid = "";
        public String playerName = "";
        public boolean success = false;
        public String reason = "";
        public TransactionType type = TransactionType.PURCHASE;
        public PaymentDestination paymentDestination = PaymentDestination.INVENTORY;
        public List<PurchaseLine> lines = new ArrayList<PurchaseLine>();
        public Map<String, Long> totals = new LinkedHashMap<String, Long>();
        public Map<String, Long> inventoryDebits = new LinkedHashMap<String, Long>();
        public Map<String, Long> walletDebits = new LinkedHashMap<String, Long>();
        public Map<String, Long> inventoryChange = new LinkedHashMap<String, Long>();
        public Map<String, Long> walletChange = new LinkedHashMap<String, Long>();
        public List<ConversionRecord> conversions = new ArrayList<ConversionRecord>();
    }

    public static enum TransactionType {
        PURCHASE,
        SALE;

    }

    public static enum PaymentDestination {
        INVENTORY,
        WALLET,
        MIXED;

    }

    public static final class PurchaseLine {
        public String productId = "";
        public String productName = "";
        public String itemId = "";
        public int quantity = 0;
        public int itemCount = 1;
        public String currencyId = "";
        public int unitPrice = 0;
        public long lineTotal = 0L;
    }

    public static final class ConversionRecord {
        public String sourceCurrency = "";
        public String targetCurrency = "";
        public long sourceAmount = 0L;
        public long targetAmount = 0L;
        public String location = "INVENTORY";
    }
}

