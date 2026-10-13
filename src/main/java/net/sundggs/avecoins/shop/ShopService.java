/*
 * Decompiled with CFR 0.152.
 */
package net.sundggs.avecoins.shop;

import java.nio.charset.StandardCharsets;
import java.util.AbstractList;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.function.Function;
import java.util.stream.Collectors;
import net.minecraft.core.HolderLookup;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.Identifier;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.Container;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.ItemLike;
import net.sundggs.avecoins.config.AtomicJsonStore;
import net.sundggs.avecoins.config.ModConfigManager;
import net.sundggs.avecoins.permission.PermissionService;
import net.sundggs.avecoins.shop.CoinExchangeService;
import net.sundggs.avecoins.shop.CoinPaymentPlanner;
import net.sundggs.avecoins.shop.MutableListCopies;
import net.sundggs.avecoins.shop.ShopData;
import net.sundggs.avecoins.shop.ShopItemStacks;
import net.sundggs.avecoins.shop.ShopStore;
import net.sundggs.avecoins.shop.WalletData;
import net.sundggs.avecoins.shop.WalletService;
import net.sundggs.avecoins.shop.WalletStore;

public final class ShopService {
    public static final int MAX_CART_LINES = 64;
    public static final int MAX_QUANTITY_PER_PRODUCT = 1024;
    public static final int HISTORY_PAGE_SIZE = 20;
    private static final Map<UUID, LinkedHashMap<String, Integer>> CARTS = new LinkedHashMap<UUID, LinkedHashMap<String, Integer>>();

    public static ShopSnapshot snapshot(ServerPlayer player, boolean includeAdministration, int historyPage) {
        ShopData data = ShopStore.get();
        ShopSnapshot snapshot = new ShopSnapshot();
        snapshot.revision = data.revision;
        snapshot.enabled = ModConfigManager.general().shopEnabled;
        snapshot.shopName = ModConfigManager.general().shopName;
        snapshot.shopColor = ModConfigManager.general().shopColor;
        snapshot.categories = data.categories.stream().sorted(Comparator.comparingInt(category -> category.sortOrder)).toList();
        snapshot.products = data.products.stream().sorted(Comparator.comparingInt(product -> product.sortOrder)).toList();
        snapshot.cart = new LinkedHashMap<String, Integer>(ShopService.cart(player));
        snapshot.inventoryBalances = WalletService.inventoryBalances(player);
        snapshot.walletBalances = WalletService.walletBalances(player);
        snapshot.balances = WalletService.combinedBalances(player);
        snapshot.paymentQuote = ShopService.paymentQuote(player, data);
        snapshot.administrator = PermissionService.isAdministrator(player);
        snapshot.administrationMode = includeAdministration;
        snapshot.historyPage = Math.max(0, historyPage);
        if (includeAdministration && historyPage >= 0) {
            int total = data.history.size();
            snapshot.historyPages = Math.max(1, (total + 20 - 1) / 20);
            int fromEnd = Math.min(total, snapshot.historyPage * 20);
            int endExclusive = total - fromEnd;
            int startInclusive = Math.max(0, endExclusive - 20);
            ArrayList<ShopData.PurchaseRecord> page = new ArrayList<ShopData.PurchaseRecord>(data.history.subList(startInclusive, endExclusive));
            Collections.reverse(page);
            snapshot.history = page;
        }
        return snapshot;
    }

    public static String snapshotJson(ServerPlayer player, boolean includeAdministration, int historyPage) {
        return AtomicJsonStore.gson().toJson(ShopService.snapshot(player, includeAdministration, historyPage));
    }

    public static OperationResult setCartQuantity(ServerPlayer player, String productId, int quantity) {
        LinkedHashMap<String, Integer> cart = ShopService.cart(player);
        return ShopService.updateCart(cart, ShopStore.get().product(productId), productId, quantity);
    }

    static OperationResult updateCart(LinkedHashMap<String, Integer> cart, ShopData.Product product, String productId, int quantity) {
        if (quantity <= 0) {
            cart.remove(productId);
            return OperationResult.success("cart_updated");
        }
        if (product == null || !product.enabled) {
            return OperationResult.failure("product_unavailable");
        }
        if (quantity > 1024) {
            return OperationResult.failure("quantity_limit");
        }
        if (!cart.containsKey(productId) && cart.size() >= 64) {
            return OperationResult.failure("cart_limit");
        }
        if (product.stock >= 0 && quantity > product.stock) {
            return OperationResult.failure("stock_changed");
        }
        cart.put(productId, quantity);
        return OperationResult.success("cart_updated");
    }

    public static OperationResult checkout(ServerPlayer player) {
        if (!ModConfigManager.general().shopEnabled) {
            return OperationResult.failure("shop_disabled");
        }
        LinkedHashMap<String, Integer> cart = ShopService.cart(player);
        if (cart.isEmpty()) {
            return OperationResult.failure("cart_empty");
        }
        ShopData current = ShopStore.get();
        ArrayList<ResolvedLine> lines = new ArrayList<ResolvedLine>();
        LinkedHashMap<String, Long> totals = new LinkedHashMap<String, Long>();
        for (Map.Entry<String, Integer> entry : cart.entrySet()) {
            ShopData.Product product = current.product(entry.getKey());
            int quantity = entry.getValue();
            if (quantity < 1 || quantity > 1024) {
                return ShopService.rejected(player, current, lines, totals, "quantity_limit");
            }
            if (product == null || !product.enabled) {
                return ShopService.rejected(player, current, lines, totals, "product_unavailable");
            }
            if (product.stock >= 0 && product.stock < quantity) {
                return ShopService.rejected(player, current, lines, totals, "stock_changed");
            }
            ItemStack productStack = ShopService.resolveProductStack(player, product);
            Item currency = ShopService.resolveItem(product.currencyId);
            if (productStack == null || currency == null) {
                return ShopService.rejected(player, current, lines, totals, "invalid_catalog_item");
            }
            long lineTotal = Math.multiplyExact((long)product.effectivePrice(), quantity);
            totals.merge(product.currencyId, lineTotal, Math::addExact);
            lines.add(new ResolvedLine(product, productStack, currency, quantity, product.effectivePrice(), lineTotal));
        }
        CoinPaymentPlanner.PaymentPlan payment = CoinPaymentPlanner.plan(totals, WalletService.inventoryBalances(player), WalletService.walletBalances(player), CoinExchangeService.activeRates(), ModConfigManager.general().automaticCoinConversion);
        if (!payment.payable) {
            return ShopService.rejected(player, current, lines, totals, payment.failureReason);
        }
        if (!ShopService.canFitAfterPayment(player, lines, payment)) {
            return ShopService.rejected(player, current, lines, totals, "inventory_full");
        }
        Inventory inventory = player.getInventory();
        List<ItemStack> inventoryBackup = ShopService.copyInventory(inventory);
        WalletData walletBackup = WalletStore.get().copy();
        WalletData walletCandidate = walletBackup.copy();
        boolean walletChanged = !payment.walletDebits.isEmpty() || !payment.walletChange.isEmpty();
        try {
            if (!ShopService.applyWalletPayment(walletCandidate, player.getUUID(), payment)) {
                return ShopService.rejected(player, current, lines, totals, "insufficient_funds");
            }
        }
        catch (ArithmeticException | IllegalArgumentException exception) {
            return ShopService.rejected(player, current, lines, totals, "wallet_limit");
        }
        for (Map.Entry<String, Long> entry : payment.inventoryDebits.entrySet()) {
            Item currency = ShopService.resolveItem(entry.getKey());
            if (currency != null && WalletService.removePhysical(player, currency, entry.getValue()) == 0L) continue;
            ShopService.restoreInventory(player, inventoryBackup);
            return ShopService.rejected(player, current, lines, totals, "inventory_changed");
        }
        if (!ShopService.addCurrencyPayments(player, payment.inventoryChange)) {
            ShopService.restoreInventory(player, inventoryBackup);
            return ShopService.rejected(player, current, lines, totals, "inventory_changed");
        }
        for (ResolvedLine resolvedLine : lines) {
            int amount;
            for (int remaining = Math.multiplyExact(resolvedLine.product.itemCount, resolvedLine.quantity); remaining > 0; remaining -= amount) {
                amount = Math.min(remaining, resolvedLine.template.getMaxStackSize());
                ItemStack delivery = resolvedLine.template.copyWithCount(amount);
                if (inventory.add(delivery) && delivery.isEmpty()) continue;
                ShopService.restoreInventory(player, inventoryBackup);
                return ShopService.rejected(player, current, lines, totals, "inventory_changed");
            }
        }
        ShopData candidate = current.copy();
        for (ResolvedLine line : lines) {
            ShopData.Product candidateProduct = candidate.product(line.product.id);
            if (candidateProduct.stock < 0) continue;
            candidateProduct.stock -= line.quantity;
        }
        ++candidate.revision;
        ShopData.PurchaseRecord purchaseRecord = ShopService.createRecord(player, lines, totals, true, "completed", ShopData.TransactionType.PURCHASE, ShopService.paymentDestination(payment));
        purchaseRecord.inventoryDebits.putAll(payment.inventoryDebits);
        purchaseRecord.walletDebits.putAll(payment.walletDebits);
        purchaseRecord.inventoryChange.putAll(payment.inventoryChange);
        purchaseRecord.walletChange.putAll(payment.walletChange);
        for (CoinPaymentPlanner.Conversion conversion : payment.conversions) {
            ShopData.ConversionRecord converted = new ShopData.ConversionRecord();
            converted.sourceCurrency = conversion.sourceCurrency();
            converted.targetCurrency = conversion.targetCurrency();
            converted.sourceAmount = conversion.sourceAmount();
            converted.targetAmount = conversion.targetAmount();
            converted.location = conversion.location().name();
            purchaseRecord.conversions.add(converted);
        }
        candidate.history.add(purchaseRecord);
        ShopService.trimHistory(candidate);
        if (walletChanged && !WalletStore.save(walletCandidate)) {
            ShopService.restoreInventory(player, inventoryBackup);
            return OperationResult.failure("persistence_error");
        }
        if (!ShopStore.save(candidate)) {
            if (walletChanged) {
                WalletStore.save(walletBackup);
            }
            ShopService.restoreInventory(player, inventoryBackup);
            return OperationResult.failure("persistence_error");
        }
        inventory.setChanged();
        player.containerMenu.broadcastChanges();
        cart.clear();
        return OperationResult.success("purchase_completed");
    }

    public static OperationResult saveCategory(ShopData.Category category) {
        if (ShopService.resolveItem(category.iconItem) == null) {
            return OperationResult.failure("invalid_catalog_item");
        }
        ShopData candidate = ShopStore.get().copy();
        ShopData.Category previous = candidate.categories.stream().filter(existing -> existing.id.equals(category.id)).findFirst().orElse(null);
        category.sortOrder = previous == null ? ShopService.appendCategoryOrder(candidate) : previous.sortOrder;
        candidate.categories.removeIf(existing -> existing.id.equals(category.id));
        candidate.categories.add(category);
        ++candidate.revision;
        return ShopStore.save(candidate) ? OperationResult.success("category_saved") : OperationResult.failure("persistence_error");
    }

    public static OperationResult deleteCategory(String categoryId) {
        ShopData candidate = ShopStore.get().copy();
        if (candidate.products.stream().anyMatch(product -> product.categoryId.equals(categoryId))) {
            return OperationResult.failure("category_not_empty");
        }
        boolean removed = candidate.categories.removeIf(category -> category.id.equals(categoryId));
        if (!removed) {
            return OperationResult.failure("category_missing");
        }
        ++candidate.revision;
        return ShopStore.save(candidate) ? OperationResult.success("category_deleted") : OperationResult.failure("persistence_error");
    }

    public static OperationResult saveProduct(ServerPlayer player, ShopData.Product product) {
        ShopData.Product previous = ShopStore.get().product(product.id);
        if (!ShopService.makeItemDataAuthoritative(player, product, previous)) {
            return OperationResult.failure("invalid_catalog_item");
        }
        ItemStack productStack = ShopService.resolveProductStack(player, product);
        if (productStack == null) {
            return OperationResult.failure("invalid_catalog_item");
        }
        ShopData candidate = ShopStore.get().copy();
        if (candidate.categories.stream().noneMatch(category -> category.id.equals(product.categoryId))) {
            return OperationResult.failure("category_missing");
        }
        if (product.sellEnabled) {
            for (ShopData.Product other : candidate.products) {
                ItemStack otherStack;
                if (other.id.equals(product.id) || !other.sellEnabled || (otherStack = ShopService.resolveProductStack(player, other)) == null || !ItemStack.isSameItemSameComponents((ItemStack)productStack, (ItemStack)otherStack)) continue;
                return OperationResult.failure("duplicate_sell_product");
            }
        }
        product.sortOrder = previous == null || !previous.categoryId.equals(product.categoryId) ? ShopService.appendProductOrder(candidate, product.categoryId) : previous.sortOrder;
        candidate.products.removeIf(existing -> existing.id.equals(product.id));
        candidate.products.add(product);
        ++candidate.revision;
        return ShopStore.save(candidate) ? OperationResult.success("product_saved") : OperationResult.failure("persistence_error");
    }

    public static OperationResult sell(ServerPlayer player, Container sellBox, boolean depositInWallet) {
        if (!ModConfigManager.general().shopEnabled) {
            return OperationResult.failure("shop_disabled");
        }
        ShopData current = ShopStore.get();
        LinkedHashMap<String, MatchedSale> matches = new LinkedHashMap<String, MatchedSale>();
        for (int slot = 0; slot < sellBox.getContainerSize(); ++slot) {
            ItemStack offered = sellBox.getItem(slot);
            if (offered.isEmpty()) continue;
            Object match = null;
            ItemStack template = ItemStack.EMPTY;
            for (ShopData.Product product : current.products) {
                ItemStack candidate;
                if (!product.enabled || !product.sellEnabled || (candidate = ShopService.resolveProductStack(player, product)) == null || !ItemStack.isSameItemSameComponents((ItemStack)candidate, (ItemStack)offered)) continue;
                if (match != null) {
                    return OperationResult.failure("duplicate_sell_product");
                }
                match = product;
                template = candidate;
            }
            if (match == null) continue;
            MatchedSale sale = (MatchedSale)matches.get(((ShopData.Product)match).id);
            if (sale == null) {
                sale = new MatchedSale((ShopData.Product)match, template);
                matches.put(((ShopData.Product)match).id, sale);
            }
            sale.offered = Math.addExact(sale.offered, offered.getCount());
        }
        ArrayList<ResolvedLine> lines = new ArrayList<ResolvedLine>();
        LinkedHashMap<String, Long> totals = new LinkedHashMap<String, Long>();
        for (MatchedSale match : matches.values()) {
            int quantity = match.offered / match.product.itemCount;
            if (quantity <= 0) continue;
            if (match.product.stock >= 0 && (long)match.product.stock + (long)quantity > 1000000000L) {
                return OperationResult.failure("stock_limit");
            }
            int unitPrice = match.product.effectiveSellingPrice();
            long lineTotal = Math.multiplyExact((long)unitPrice, quantity);
            Item currency2 = ShopService.resolveItem(match.product.currencyId);
            if (currency2 == null) {
                return OperationResult.failure("invalid_catalog_item");
            }
            totals.merge(match.product.currencyId, lineTotal, Math::addExact);
            lines.add(new ResolvedLine(match.product, match.template, currency2, quantity, unitPrice, lineTotal));
        }
        if (lines.isEmpty()) {
            return OperationResult.failure("nothing_to_sell");
        }
        if (!depositInWallet && !ShopService.canFitCurrencyPayments(player, totals)) {
            return OperationResult.failure("sale_payment_inventory_full");
        }
        List<ItemStack> boxBackup = ShopService.copyContainer(sellBox);
        List<ItemStack> inventoryBackup = ShopService.copyInventory(player.getInventory());
        WalletData walletBackup = WalletStore.get().copy();
        WalletData walletCandidate = walletBackup.copy();
        for (ResolvedLine line : lines) {
            int remaining = Math.multiplyExact(line.product.itemCount, line.quantity);
            for (int slot = 0; slot < sellBox.getContainerSize() && remaining > 0; ++slot) {
                ItemStack offered = sellBox.getItem(slot);
                if (offered.isEmpty() || !ItemStack.isSameItemSameComponents((ItemStack)line.template, (ItemStack)offered)) continue;
                int removed = Math.min(remaining, offered.getCount());
                offered.shrink(removed);
                remaining -= removed;
                sellBox.setChanged();
            }
        }
        if (depositInWallet) {
            try {
                totals.forEach((currency, amount) -> walletCandidate.credit(player.getUUID(), (String)currency, (long)amount));
            }
            catch (ArithmeticException | IllegalArgumentException exception) {
                ShopService.restoreContainer(sellBox, boxBackup);
                return OperationResult.failure("wallet_limit");
            }
            if (!WalletStore.save(walletCandidate)) {
                ShopService.restoreContainer(sellBox, boxBackup);
                return OperationResult.failure("persistence_error");
            }
        } else if (!ShopService.addCurrencyPayments(player, totals)) {
            ShopService.restoreContainer(sellBox, boxBackup);
            ShopService.restoreInventory(player, inventoryBackup);
            return OperationResult.failure("sale_payment_inventory_full");
        }
        ShopData candidate = current.copy();
        for (ResolvedLine line : lines) {
            ShopData.Product product = candidate.product(line.product.id);
            if (product.stock < 0) continue;
            product.stock += line.quantity;
        }
        ++candidate.revision;
        candidate.history.add(ShopService.createRecord(player, lines, totals, true, "sale_completed", ShopData.TransactionType.SALE, depositInWallet ? ShopData.PaymentDestination.WALLET : ShopData.PaymentDestination.INVENTORY));
        ShopService.trimHistory(candidate);
        if (!ShopStore.save(candidate)) {
            if (depositInWallet) {
                WalletStore.save(walletBackup);
            }
            ShopService.restoreContainer(sellBox, boxBackup);
            ShopService.restoreInventory(player, inventoryBackup);
            return OperationResult.failure("persistence_error");
        }
        player.getInventory().setChanged();
        player.containerMenu.broadcastChanges();
        return OperationResult.success(depositInWallet ? "sale_completed_wallet" : "sale_completed_inventory");
    }

    public static OperationResult moveCategory(String categoryId, int direction) {
        ShopData candidate = ShopStore.get().copy();
        ArrayList<ShopData.Category> ordered = new ArrayList<ShopData.Category>(candidate.categories);
        ordered.sort(Comparator.comparingInt(value -> value.sortOrder));
        int index = ShopService.indexOfCategory(ordered, categoryId);
        int destination = index + Integer.signum(direction);
        if (index < 0 || destination < 0 || destination >= ordered.size()) {
            return OperationResult.failure("order_limit");
        }
        ShopData.Category moved = (ShopData.Category)ordered.remove(index);
        ordered.add(destination, moved);
        for (int position = 0; position < ordered.size(); ++position) {
            ((ShopData.Category)ordered.get((int)position)).sortOrder = (position + 1) * 10;
        }
        ++candidate.revision;
        return ShopStore.save(candidate) ? OperationResult.success("order_updated") : OperationResult.failure("persistence_error");
    }

    public static OperationResult moveProduct(String productId, int direction) {
        ShopData candidate = ShopStore.get().copy();
        ShopData.Product product = candidate.product(productId);
        if (product == null) {
            return OperationResult.failure("product_missing");
        }
        List ordered = candidate.products.stream().filter(value -> value.categoryId.equals(product.categoryId)).sorted(Comparator.comparingInt(value -> value.sortOrder)).collect(Collectors.toCollection(ArrayList::new));
        int index = ShopService.indexOfProduct(ordered, productId);
        int destination = index + Integer.signum(direction);
        if (index < 0 || destination < 0 || destination >= ordered.size()) {
            return OperationResult.failure("order_limit");
        }
        ShopData.Product moved = (ShopData.Product)ordered.remove(index);
        ordered.add(destination, moved);
        for (int position = 0; position < ordered.size(); ++position) {
            ((ShopData.Product)ordered.get((int)position)).sortOrder = (position + 1) * 10;
        }
        ++candidate.revision;
        return ShopStore.save(candidate) ? OperationResult.success("order_updated") : OperationResult.failure("persistence_error");
    }

    public static OperationResult deleteProduct(String productId) {
        ShopData candidate = ShopStore.get().copy();
        boolean removed = candidate.products.removeIf(product -> product.id.equals(productId));
        if (!removed) {
            return OperationResult.failure("product_missing");
        }
        ++candidate.revision;
        if (!ShopStore.save(candidate)) {
            return OperationResult.failure("persistence_error");
        }
        CARTS.values().forEach(cart -> cart.remove(productId));
        return OperationResult.success("product_deleted");
    }

    public static void removeSession(UUID playerId) {
        CARTS.remove(playerId);
    }

    public static void clearSessions() {
        CARTS.clear();
    }

    private static PaymentQuote paymentQuote(ServerPlayer player, ShopData data) {
        PaymentQuote quote;
        block11: {
            quote = new PaymentQuote();
            LinkedHashMap<String, Long> totals = new LinkedHashMap<String, Long>();
            ArrayList<ResolvedLine> lines = new ArrayList<ResolvedLine>();
            try {
                for (Map.Entry<String, Integer> entry : ShopService.cart(player).entrySet()) {
                    ShopData.Product product = data.product(entry.getKey());
                    if (product == null || !product.enabled || entry.getValue() <= 0) {
                        quote.payable = false;
                        quote.reason = "product_unavailable";
                        return quote;
                    }
                    if (product.stock >= 0 && product.stock < entry.getValue()) {
                        quote.payable = false;
                        quote.reason = "stock_changed";
                        return quote;
                    }
                    ItemStack productStack = ShopService.resolveProductStack(player, product);
                    Item currency = ShopService.resolveItem(product.currencyId);
                    if (productStack == null || currency == null) {
                        quote.payable = false;
                        quote.reason = "invalid_catalog_item";
                        return quote;
                    }
                    long lineTotal = Math.multiplyExact((long)product.effectivePrice(), (int)entry.getValue());
                    totals.merge(product.currencyId, lineTotal, Math::addExact);
                    lines.add(new ResolvedLine(product, productStack, currency, entry.getValue(), product.effectivePrice(), lineTotal));
                }
                CoinPaymentPlanner.PaymentPlan plan = CoinPaymentPlanner.plan(totals, WalletService.inventoryBalances(player), WalletService.walletBalances(player), CoinExchangeService.activeRates(), ModConfigManager.general().automaticCoinConversion);
                quote.payable = plan.payable;
                quote.reason = plan.failureReason;
                quote.inventoryDebits.putAll(plan.inventoryDebits);
                quote.walletDebits.putAll(plan.walletDebits);
                quote.inventoryChange.putAll(plan.inventoryChange);
                quote.walletChange.putAll(plan.walletChange);
                quote.conversions = plan.conversions;
                if (!plan.payable) break block11;
                WalletData wallet = WalletStore.get().copy();
                try {
                    if (!ShopService.applyWalletPayment(wallet, player.getUUID(), plan)) {
                        quote.payable = false;
                        quote.reason = "insufficient_funds";
                    } else if (!ShopService.canFitAfterPayment(player, lines, plan)) {
                        quote.payable = false;
                        quote.reason = "inventory_full";
                    }
                }
                catch (ArithmeticException | IllegalArgumentException exception) {
                    quote.payable = false;
                    quote.reason = "wallet_limit";
                }
            }
            catch (ArithmeticException exception) {
                quote.payable = false;
                quote.reason = "currency_value_overflow";
            }
        }
        return quote;
    }

    static boolean applyWalletPayment(WalletData wallet, UUID playerId, CoinPaymentPlanner.PaymentPlan payment) {
        for (Map.Entry<String, Long> debit : payment.walletDebits.entrySet()) {
            if (wallet.debit(playerId, debit.getKey(), debit.getValue())) continue;
            return false;
        }
        for (Map.Entry<String, Long> change : payment.walletChange.entrySet()) {
            wallet.credit(playerId, change.getKey(), change.getValue());
        }
        return true;
    }

    private static ShopData.PaymentDestination paymentDestination(CoinPaymentPlanner.PaymentPlan payment) {
        boolean wallet;
        boolean inventory = !payment.inventoryDebits.isEmpty();
        boolean bl = wallet = !payment.walletDebits.isEmpty();
        if (inventory && wallet) {
            return ShopData.PaymentDestination.MIXED;
        }
        return wallet ? ShopData.PaymentDestination.WALLET : ShopData.PaymentDestination.INVENTORY;
    }

    private static OperationResult rejected(ServerPlayer player, ShopData current, List<ResolvedLine> lines, Map<String, Long> totals, String reason) {
        ShopData candidate = current.copy();
        candidate.history.add(ShopService.createRecord(player, lines, totals, false, reason, ShopData.TransactionType.PURCHASE, ShopData.PaymentDestination.INVENTORY));
        ShopService.trimHistory(candidate);
        ShopStore.save(candidate);
        return OperationResult.failure(reason);
    }

    private static ShopData.PurchaseRecord createRecord(ServerPlayer player, List<ResolvedLine> lines, Map<String, Long> totals, boolean success, String reason, ShopData.TransactionType type, ShopData.PaymentDestination destination) {
        ShopData.PurchaseRecord record = new ShopData.PurchaseRecord();
        record.transactionId = UUID.randomUUID().toString();
        record.timestamp = System.currentTimeMillis();
        record.playerUuid = player.getUUID().toString();
        record.playerName = player.getScoreboardName();
        record.success = success;
        record.reason = reason;
        record.type = type;
        record.paymentDestination = destination;
        record.totals = new LinkedHashMap<String, Long>(totals);
        for (ResolvedLine resolved : lines) {
            ShopData.PurchaseLine line = new ShopData.PurchaseLine();
            line.productId = resolved.product.id;
            line.productName = resolved.product.name;
            line.itemId = resolved.product.itemId;
            line.quantity = resolved.quantity;
            line.itemCount = resolved.product.itemCount;
            line.currencyId = resolved.product.currencyId;
            line.unitPrice = resolved.unitPrice;
            line.lineTotal = resolved.lineTotal;
            record.lines.add(line);
        }
        return record;
    }

    private static void trimHistory(ShopData data) {
        while (data.history.size() > 200) {
            data.history.removeFirst();
        }
    }

    private static LinkedHashMap<String, Integer> cart(ServerPlayer player) {
        return CARTS.computeIfAbsent(player.getUUID(), ignored -> new LinkedHashMap());
    }

    private static Item resolveItem(String idValue) {
        Identifier id = Identifier.tryParse((String)idValue);
        if (id == null || !BuiltInRegistries.ITEM.containsKey(id)) {
            return null;
        }
        return (Item)BuiltInRegistries.ITEM.getValue(id);
    }

    private static ItemStack resolveProductStack(ServerPlayer player, ShopData.Product product) {
        Item expected = ShopService.resolveItem(product.itemId);
        if (expected == null) {
            return null;
        }
        if (product.itemData == null || product.itemData.isBlank()) {
            return new ItemStack((ItemLike)expected);
        }
        return ShopItemStacks.decodeExpected((HolderLookup.Provider)player.registryAccess(), product.itemData, expected).orElse(null);
    }

    private static boolean makeItemDataAuthoritative(ServerPlayer player, ShopData.Product product, ShopData.Product previous) {
        Identifier heldId;
        if (product.itemData == null || product.itemData.isBlank()) {
            product.itemData = "";
            return true;
        }
        if (previous != null && product.itemId.equals(previous.itemId) && product.itemData.equals(previous.itemData)) {
            return true;
        }
        ItemStack held = player.getMainHandItem();
        Identifier identifier = heldId = held.isEmpty() ? null : BuiltInRegistries.ITEM.getKey(held.getItem());
        if (heldId == null || !heldId.toString().equals(product.itemId)) {
            return false;
        }
        try {
            String encoded = ShopItemStacks.encode((HolderLookup.Provider)player.registryAccess(), held);
            if (encoded.isBlank() || encoded.getBytes(StandardCharsets.UTF_8).length > 32768) {
                return false;
            }
            product.itemData = encoded;
            return true;
        }
        catch (RuntimeException exception) {
            return false;
        }
    }

    private static boolean canFitAfterPayment(ServerPlayer player, List<ResolvedLine> lines, CoinPaymentPlanner.PaymentPlan payment) {
        List<ItemStack> deliveries = lines.stream().map(line -> line.template.copyWithCount(Math.multiplyExact(line.product.itemCount, line.quantity))).toList();
        return ShopService.canFitAfterPayment((List<ItemStack>)player.getInventory().getNonEquipmentItems(), payment, deliveries, ShopService::resolveItem);
    }

    static boolean canFitAfterPayment(List<ItemStack> inventory, CoinPaymentPlanner.PaymentPlan payment, List<ItemStack> deliveries, Function<String, Item> currencies) {
        long remaining;
        Item currency;
        List<ItemStack> simulated = ShopService.copyStacks(inventory);
        block0: for (Map.Entry<String, Long> debit : payment.inventoryDebits.entrySet()) {
            currency = currencies.apply(debit.getKey());
            remaining = debit.getValue();
            for (ItemStack stack : simulated) {
                if (remaining == 0L) continue block0;
                if (!stack.is(currency)) continue;
                int removed = (int)Math.min(remaining, (long)stack.getCount());
                stack.shrink(removed);
                remaining -= (long)removed;
            }
        }
        for (Map.Entry<String, Long> change : payment.inventoryChange.entrySet()) {
            int amount;
            currency = currencies.apply(change.getKey());
            for (remaining = change.getValue().longValue(); remaining > 0L; remaining -= (long)amount) {
                amount = (int)Math.min(remaining, (long)currency.getDefaultMaxStackSize());
                if (ShopService.simulateAdd(simulated, new ItemStack((ItemLike)currency, amount))) continue;
                return false;
            }
        }
        for (ItemStack delivery : deliveries) {
            int amount;
            for (int remaining2 = delivery.getCount(); remaining2 > 0; remaining2 -= amount) {
                amount = Math.min(remaining2, delivery.getMaxStackSize());
                if (ShopService.simulateAdd(simulated, delivery.copyWithCount(amount))) continue;
                return false;
            }
        }
        return true;
    }

    private static boolean simulateAdd(List<ItemStack> inventory, ItemStack incoming) {
        for (ItemStack existing : inventory) {
            if (!ItemStack.isSameItemSameComponents((ItemStack)existing, (ItemStack)incoming) || existing.getCount() >= existing.getMaxStackSize()) continue;
            int moved = Math.min(incoming.getCount(), existing.getMaxStackSize() - existing.getCount());
            existing.grow(moved);
            incoming.shrink(moved);
            if (!incoming.isEmpty()) continue;
            return true;
        }
        for (int index = 0; index < inventory.size(); ++index) {
            if (!inventory.get(index).isEmpty()) continue;
            inventory.set(index, incoming.copy());
            incoming.setCount(0);
            return true;
        }
        return incoming.isEmpty();
    }

    private static boolean canFitCurrencyPayments(ServerPlayer player, Map<String, Long> totals) {
        List<ItemStack> simulated = ShopService.copyStacks((List<ItemStack>)player.getInventory().getNonEquipmentItems());
        for (Map.Entry<String, Long> total : totals.entrySet()) {
            int amount;
            Item currency = ShopService.resolveItem(total.getKey());
            if (currency == null) {
                return false;
            }
            for (long remaining = total.getValue().longValue(); remaining > 0L; remaining -= (long)amount) {
                amount = (int)Math.min(remaining, (long)currency.getDefaultMaxStackSize());
                if (ShopService.simulateAdd(simulated, new ItemStack((ItemLike)currency, amount))) continue;
                return false;
            }
        }
        return true;
    }

    private static boolean addCurrencyPayments(ServerPlayer player, Map<String, Long> totals) {
        for (Map.Entry<String, Long> total : totals.entrySet()) {
            int amount;
            Item currency = ShopService.resolveItem(total.getKey());
            if (currency == null) {
                return false;
            }
            for (long remaining = total.getValue().longValue(); remaining > 0L; remaining -= (long)amount) {
                amount = (int)Math.min(remaining, (long)currency.getDefaultMaxStackSize());
                ItemStack stack = new ItemStack((ItemLike)currency, amount);
                if (player.getInventory().add(stack) && stack.isEmpty()) continue;
                return false;
            }
        }
        return true;
    }

    private static List<ItemStack> copyContainer(Container container) {
        ArrayList<ItemStack> copy = new ArrayList<ItemStack>(container.getContainerSize());
        for (int slot = 0; slot < container.getContainerSize(); ++slot) {
            copy.add(container.getItem(slot).copy());
        }
        return copy;
    }

    private static void restoreContainer(Container container, List<ItemStack> backup) {
        for (int slot = 0; slot < backup.size(); ++slot) {
            container.setItem(slot, backup.get(slot).copy());
        }
        container.setChanged();
    }

    static long removeFromStacks(List<ItemStack> stacks, Item item, long amount) {
        long remaining = amount;
        for (ItemStack stack : stacks) {
            if (remaining == 0L) break;
            if (!stack.is(item)) continue;
            int removed = (int)Math.min(remaining, (long)stack.getCount());
            stack.shrink(removed);
            remaining -= (long)removed;
        }
        return remaining;
    }

    static List<ItemStack> copyStacks(List<ItemStack> stacks) {
        return MutableListCopies.copy(stacks, ItemStack::copy);
    }

    private static List<ItemStack> copyInventory(Inventory inventory) {
        return MutableListCopies.copy(ShopService.inventoryView(inventory), ItemStack::copy);
    }

    private static int indexOfCategory(List<ShopData.Category> categories, String id) {
        for (int index = 0; index < categories.size(); ++index) {
            if (!categories.get((int)index).id.equals(id)) continue;
            return index;
        }
        return -1;
    }

    private static int indexOfProduct(List<ShopData.Product> products, String id) {
        for (int index = 0; index < products.size(); ++index) {
            if (!products.get((int)index).id.equals(id)) continue;
            return index;
        }
        return -1;
    }

    private static int appendCategoryOrder(ShopData data) {
        ArrayList<ShopData.Category> ordered = new ArrayList<ShopData.Category>(data.categories);
        ordered.sort(Comparator.comparingInt(value -> value.sortOrder));
        for (int index = 0; index < ordered.size(); ++index) {
            ((ShopData.Category)ordered.get((int)index)).sortOrder = (index + 1) * 10;
        }
        return (ordered.size() + 1) * 10;
    }

    private static int appendProductOrder(ShopData data, String categoryId) {
        List ordered = data.products.stream().filter(value -> value.categoryId.equals(categoryId)).sorted(Comparator.comparingInt(value -> value.sortOrder)).collect(Collectors.toCollection(ArrayList::new));
        for (int index = 0; index < ordered.size(); ++index) {
            ((ShopData.Product)ordered.get((int)index)).sortOrder = (index + 1) * 10;
        }
        return (ordered.size() + 1) * 10;
    }

    private static void restoreInventory(ServerPlayer player, List<ItemStack> backup) {
        Inventory inventory = player.getInventory();
        MutableListCopies.restore(ShopService.inventoryView(inventory), backup, ItemStack::copy);
        inventory.setChanged();
        player.containerMenu.broadcastChanges();
    }

    private static List<ItemStack> inventoryView(final Inventory inventory) {
        return new AbstractList<ItemStack>(){

            @Override
            public ItemStack get(int index) {
                return inventory.getItem(index);
            }

            @Override
            public ItemStack set(int index, ItemStack value) {
                ItemStack previous = inventory.getItem(index);
                inventory.setItem(index, value);
                return previous;
            }

            @Override
            public int size() {
                return inventory.getContainerSize();
            }
        };
    }

    private ShopService() {
    }

    public static final class ShopSnapshot {
        public long revision;
        public boolean enabled;
        public String shopName;
        public String shopColor = "";
        public boolean administrator;
        public boolean administrationMode;
        public List<ShopData.Category> categories = List.of();
        public List<ShopData.Product> products = List.of();
        public Map<String, Integer> cart = new LinkedHashMap<String, Integer>();
        public Map<String, Long> balances = new LinkedHashMap<String, Long>();
        public Map<String, Long> inventoryBalances = new LinkedHashMap<String, Long>();
        public Map<String, Long> walletBalances = new LinkedHashMap<String, Long>();
        public PaymentQuote paymentQuote = new PaymentQuote();
        public int historyPage;
        public int historyPages = 1;
        public List<ShopData.PurchaseRecord> history = List.of();
    }

    public static final class PaymentQuote {
        public boolean payable = true;
        public String reason = "";
        public Map<String, Long> inventoryDebits = new LinkedHashMap<String, Long>();
        public Map<String, Long> walletDebits = new LinkedHashMap<String, Long>();
        public Map<String, Long> inventoryChange = new LinkedHashMap<String, Long>();
        public Map<String, Long> walletChange = new LinkedHashMap<String, Long>();
        public List<CoinPaymentPlanner.Conversion> conversions = List.of();
    }

    public record OperationResult(boolean success, String code) {
        public static OperationResult success(String code) {
            return new OperationResult(true, code);
        }

        public static OperationResult failure(String code) {
            return new OperationResult(false, code);
        }
    }

    private record ResolvedLine(ShopData.Product product, ItemStack template, Item currency, int quantity, int unitPrice, long lineTotal) {
    }

    private static final class MatchedSale {
        private final ShopData.Product product;
        private final ItemStack template;
        private int offered;

        private MatchedSale(ShopData.Product product, ItemStack template) {
            this.product = product;
            this.template = template;
        }
    }
}

