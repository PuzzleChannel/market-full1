/*
 * Decompiled with CFR 0.152.
 */
package net.sundggs.avecoins.shop;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.LinkOption;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import net.fabricmc.loader.api.FabricLoader;
import net.sundggs.avecoins.AvecoinsMod;
import net.sundggs.avecoins.config.AtomicJsonStore;
import net.sundggs.avecoins.shop.ShopData;

public final class ShopStore {
    private static ShopData current = ShopData.defaults();

    public static synchronized void initialize() {
        ShopStore.migrateLegacyFile();
        if (!ShopStore.recoverPendingTransaction()) {
            throw new IllegalStateException("Could not recover AVECOINS shop configuration");
        }
        Production.CATEGORIES.load();
        Production.PRODUCTS.load();
        Production.HISTORY.load();
        current = ShopStore.combine(Production.CATEGORIES.get(), Production.PRODUCTS.get(), Production.HISTORY.get());
    }

    public static synchronized ShopData get() {
        return current;
    }

    public static synchronized boolean save(ShopData data) {
        long revision;
        ShopData validated;
        if (Files.exists(Production.TRANSACTION_FILE, new LinkOption[0]) && !ShopStore.reload()) {
            return false;
        }
        try {
            validated = ShopData.validate(data);
        }
        catch (RuntimeException exception) {
            AvecoinsMod.LOGGER.warn("Rejected invalid AVECOINS shop configuration: {}", exception.getMessage());
            return false;
        }
        validated.revision = revision = Math.max(ShopStore.current.revision + 1L, validated.revision);
        CategoriesFile categories = new CategoriesFile(revision, validated.categories);
        ProductsFile products = new ProductsFile(revision, validated.products);
        HistoryFile history = new HistoryFile(revision, validated.history);
        if (!Production.TRANSACTION.save(validated)) {
            return false;
        }
        current = validated;
        if (!(Production.CATEGORIES.save(categories) && Production.PRODUCTS.save(products) && Production.HISTORY.save(history))) {
            AvecoinsMod.LOGGER.warn("AVECOINS shop transaction {} remains journaled for recovery", revision);
            return true;
        }
        ShopStore.removeTransactionFile();
        return true;
    }

    public static synchronized boolean reload() {
        if (!ShopStore.recoverPendingTransaction()) {
            return false;
        }
        boolean categoriesLoaded = Production.CATEGORIES.reload();
        boolean productsLoaded = Production.PRODUCTS.reload();
        boolean historyLoaded = Production.HISTORY.reload();
        if (categoriesLoaded && productsLoaded && historyLoaded) {
            try {
                current = ShopStore.combine(Production.CATEGORIES.get(), Production.PRODUCTS.get(), Production.HISTORY.get());
            }
            catch (RuntimeException exception) {
                AvecoinsMod.LOGGER.error("Could not combine AVECOINS shop configuration files", (Throwable)exception);
                return false;
            }
            return true;
        }
        return false;
    }

    private static ShopData combine(CategoriesFile categories, ProductsFile products, HistoryFile history) {
        if (categories.revision != products.revision || products.revision != history.revision) {
            throw new IllegalStateException("Shop files have different revisions");
        }
        ShopData data = new ShopData();
        data.revision = categories.revision;
        data.categories = new ArrayList<ShopData.Category>(categories.categories);
        data.products = new ArrayList<ShopData.Product>(products.products);
        data.history = new ArrayList<ShopData.PurchaseRecord>(history.history);
        return ShopData.validate(data);
    }

    private static void migrateLegacyFile() {
        if (Files.notExists(Production.LEGACY_FILE, new LinkOption[0]) || Files.exists(Production.CATEGORIES.path(), new LinkOption[0]) || Files.exists(Production.PRODUCTS.path(), new LinkOption[0]) || Files.exists(Production.HISTORY.path(), new LinkOption[0])) {
            return;
        }
        try {
            String json = Files.readString(Production.LEGACY_FILE, StandardCharsets.UTF_8);
            AtomicJsonStore.requireStrictJson(json);
            ShopData legacy = ShopData.validate((ShopData)AtomicJsonStore.gson().fromJson(json, ShopData.class));
            if (!(Production.TRANSACTION.save(legacy) && Production.CATEGORIES.save(new CategoriesFile(legacy.revision, legacy.categories)) && Production.PRODUCTS.save(new ProductsFile(legacy.revision, legacy.products)) && Production.HISTORY.save(new HistoryFile(legacy.revision, legacy.history)))) {
                throw new IllegalStateException("Could not save migrated shop files");
            }
            ShopStore.removeTransactionFile();
            Files.deleteIfExists(Production.LEGACY_FILE);
            AvecoinsMod.LOGGER.info("Migrated AVECOINS shop.json to config/avecoins/shop/");
        }
        catch (Exception exception) {
            AvecoinsMod.LOGGER.error("Could not migrate legacy AVECOINS shop.json; keeping the original file", (Throwable)exception);
        }
    }

    private static boolean recoverPendingTransaction() {
        return ShopStore.recoverPendingTransaction(Production.TRANSACTION_FILE, Production.TRANSACTION, Production.CATEGORIES, Production.PRODUCTS, Production.HISTORY);
    }

    static boolean recoverPendingTransaction(Path transactionFile, AtomicJsonStore<ShopData> transaction, AtomicJsonStore<CategoriesFile> categories, AtomicJsonStore<ProductsFile> products, AtomicJsonStore<HistoryFile> history) {
        if (Files.notExists(transactionFile, new LinkOption[0])) {
            return true;
        }
        try {
            if (!transaction.reload()) {
                return false;
            }
            ShopData pending = transaction.get();
            long revision = pending.revision;
            if (!(categories.save(new CategoriesFile(revision, pending.categories)) && products.save(new ProductsFile(revision, pending.products)) && history.save(new HistoryFile(revision, pending.history)))) {
                return false;
            }
            ShopStore.removeTransactionFile(transactionFile);
            AvecoinsMod.LOGGER.info("Recovered an interrupted AVECOINS shop save");
            return true;
        }
        catch (RuntimeException exception) {
            AvecoinsMod.LOGGER.error("Could not recover the pending AVECOINS shop transaction", (Throwable)exception);
            return false;
        }
    }

    private static void removeTransactionFile() {
        ShopStore.removeTransactionFile(Production.TRANSACTION_FILE);
    }

    private static void removeTransactionFile(Path transactionFile) {
        try {
            Files.deleteIfExists(transactionFile);
        }
        catch (IOException exception) {
            AvecoinsMod.LOGGER.warn("Could not remove completed AVECOINS shop transaction file {}", transactionFile, exception);
        }
    }

    private ShopStore() {
    }

    private static final class Production {
        private static final Path ROOT = FabricLoader.getInstance().getConfigDir().resolve("avecoins").toAbsolutePath().normalize();
        private static final Path SHOP_DIRECTORY = ROOT.resolve("shop");
        private static final Path LEGACY_FILE = ROOT.resolve("shop.json");
        private static final Path TRANSACTION_FILE = SHOP_DIRECTORY.resolve(".transaction.json");
        private static final AtomicJsonStore<CategoriesFile> CATEGORIES = new AtomicJsonStore<CategoriesFile>(SHOP_DIRECTORY.resolve("categories.json"), CategoriesFile.class, CategoriesFile::defaults, CategoriesFile::validate);
        private static final AtomicJsonStore<ProductsFile> PRODUCTS = new AtomicJsonStore<ProductsFile>(SHOP_DIRECTORY.resolve("products.json"), ProductsFile.class, ProductsFile::defaults, ProductsFile::validate);
        private static final AtomicJsonStore<HistoryFile> HISTORY = new AtomicJsonStore<HistoryFile>(SHOP_DIRECTORY.resolve("history.json"), HistoryFile.class, HistoryFile::defaults, HistoryFile::validate);
        private static final AtomicJsonStore<ShopData> TRANSACTION = new AtomicJsonStore<ShopData>(TRANSACTION_FILE, ShopData.class, ShopData::defaults, ShopData::validate);

        private Production() {
        }
    }

    public static final class CategoriesFile {
        public int schemaVersion = 3;
        public long revision;
        public List<ShopData.Category> categories = new ArrayList<ShopData.Category>();

        public CategoriesFile() {
        }

        CategoriesFile(long revision, List<ShopData.Category> categories) {
            this.revision = revision;
            this.categories = new ArrayList<ShopData.Category>(categories);
        }

        static CategoriesFile defaults() {
            return new CategoriesFile();
        }

        static CategoriesFile validate(CategoriesFile input) {
            CategoriesFile output = new CategoriesFile();
            output.revision = Math.max(0L, input.revision);
            ShopData data = ShopData.defaults();
            data.categories = input.categories == null ? null : new ArrayList<ShopData.Category>(input.categories);
            output.categories = ShopData.validate((ShopData)data).categories;
            return output;
        }
    }

    public static final class ProductsFile {
        public int schemaVersion = 3;
        public long revision;
        public List<ShopData.Product> products = new ArrayList<ShopData.Product>();

        public ProductsFile() {
        }

        ProductsFile(long revision, List<ShopData.Product> products) {
            this.revision = revision;
            this.products = new ArrayList<ShopData.Product>(products);
        }

        static ProductsFile defaults() {
            return new ProductsFile();
        }

        static ProductsFile validate(ProductsFile input) {
            ProductsFile output = new ProductsFile();
            output.revision = Math.max(0L, input.revision);
            List<ShopData.Product> list = output.products = input.products == null ? new ArrayList<ShopData.Product>() : new ArrayList<ShopData.Product>(input.products);
            if (output.products.size() > 512) {
                throw new IllegalArgumentException("Too many shop products");
            }
            return output;
        }
    }

    public static final class HistoryFile {
        public int schemaVersion = 3;
        public long revision;
        public List<ShopData.PurchaseRecord> history = new ArrayList<ShopData.PurchaseRecord>();

        public HistoryFile() {
        }

        HistoryFile(long revision, List<ShopData.PurchaseRecord> history) {
            this.revision = revision;
            this.history = new ArrayList<ShopData.PurchaseRecord>(history);
        }

        static HistoryFile defaults() {
            return new HistoryFile();
        }

        static HistoryFile validate(HistoryFile input) {
            HistoryFile output = new HistoryFile();
            output.revision = Math.max(0L, input.revision);
            ShopData data = ShopData.defaults();
            data.history = input.history == null ? new ArrayList<ShopData.PurchaseRecord>() : new ArrayList<ShopData.PurchaseRecord>(input.history);
            output.history = ShopData.validate((ShopData)data).history;
            return output;
        }
    }
}

