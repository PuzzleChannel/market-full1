/*
 * Decompiled with CFR 0.152.
 */
package net.sundggs.avecoins.shop;

import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import net.sundggs.avecoins.config.CraftingConfig;
import net.sundggs.avecoins.shop.CoinExchangeRates;

public final class CoinPaymentPlanner {
    public static PaymentPlan plan(Map<String, Long> totals, Map<String, Long> inventoryBalances, Map<String, Long> walletBalances, CoinExchangeRates rates, boolean automaticConversion) {
        Map<String, Long> initialInventory = CoinPaymentPlanner.balances(inventoryBalances);
        Map<String, Long> initialWallet = CoinPaymentPlanner.balances(walletBalances);
        LinkedHashMap<String, Long> inventory = new LinkedHashMap<String, Long>(initialInventory);
        LinkedHashMap<String, Long> wallet = new LinkedHashMap<String, Long>(initialWallet);
        ArrayList<Conversion> conversions = new ArrayList<Conversion>();
        try {
            for (Map.Entry<String, Long> total : totals.entrySet()) {
                if (rates.isCoin(total.getKey()) || CoinPaymentPlanner.payExact(total.getKey(), CoinPaymentPlanner.required(total), inventory, wallet)) continue;
                return PaymentPlan.failure("insufficient_funds");
            }
            for (int index = CraftingConfig.COINS.size() - 1; index >= 0; --index) {
                String target = CraftingConfig.COINS.get(index);
                long required = CoinPaymentPlanner.required(totals.getOrDefault(target, 0L));
                long paid = CoinPaymentPlanner.take(target, required, inventory);
                long remaining = required - (paid = Math.addExact(paid, CoinPaymentPlanner.take(target, required - paid, wallet)));
                if (remaining == 0L) continue;
                if (!automaticConversion) {
                    return PaymentPlan.failure("insufficient_funds");
                }
                for (int sourceIndex = index + 1; sourceIndex < CraftingConfig.COINS.size() && remaining > 0L; ++sourceIndex) {
                    String source = CraftingConfig.COINS.get(sourceIndex);
                    long sourceValue = rates.valueIn(source, target);
                    remaining = CoinPaymentPlanner.convertAndPay(source, target, sourceValue, remaining, inventory, conversions, PaymentLocation.INVENTORY, rates);
                    remaining = CoinPaymentPlanner.convertAndPay(source, target, sourceValue, remaining, wallet, conversions, PaymentLocation.WALLET, rates);
                }
                if (remaining <= 0L) continue;
                return PaymentPlan.failure("insufficient_funds");
            }
        }
        catch (ArithmeticException exception) {
            return PaymentPlan.failure("currency_value_overflow");
        }
        return PaymentPlan.success(CoinPaymentPlanner.decreases(initialInventory, inventory), CoinPaymentPlanner.decreases(initialWallet, wallet), CoinPaymentPlanner.increases(initialInventory, inventory), CoinPaymentPlanner.increases(initialWallet, wallet), conversions);
    }

    private static long convertAndPay(String source, String target, long sourceValue, long remaining, Map<String, Long> balances, List<Conversion> conversions, PaymentLocation location, CoinExchangeRates rates) {
        long available = balances.getOrDefault(source, 0L);
        if (available == 0L || remaining == 0L || sourceValue <= 0L) {
            return remaining;
        }
        long needed = Math.addExact((remaining - 1L) / sourceValue, 1L);
        long convertedCoins = Math.min(available, needed);
        long convertedValue = Math.multiplyExact(convertedCoins, sourceValue);
        balances.put(source, available - convertedCoins);
        long payment = Math.min(remaining, convertedValue);
        long change = convertedValue - payment;
        for (int tier = CraftingConfig.COINS.indexOf(source) - 1; tier >= CraftingConfig.COINS.indexOf(target); --tier) {
            String denomination = CraftingConfig.COINS.get(tier);
            long unit = rates.valueIn(denomination, target);
            long count = change / unit;
            if (count > 0L) {
                balances.merge(denomination, count, Math::addExact);
            }
            change %= unit;
        }
        conversions.add(new Conversion(source, target, convertedCoins, convertedValue, location));
        return remaining - payment;
    }

    private static boolean payExact(String currency, long amount, Map<String, Long> inventory, Map<String, Long> wallet) {
        long paid = CoinPaymentPlanner.take(currency, amount, inventory);
        return (paid = Math.addExact(paid, CoinPaymentPlanner.take(currency, amount - paid, wallet))) == amount;
    }

    private static long take(String currency, long amount, Map<String, Long> balances) {
        if (amount <= 0L) {
            return 0L;
        }
        long available = balances.getOrDefault(currency, 0L);
        long taken = Math.min(amount, available);
        if (taken > 0L) {
            balances.put(currency, available - taken);
        }
        return taken;
    }

    private static long required(Map.Entry<String, Long> total) {
        return CoinPaymentPlanner.required(total.getValue());
    }

    private static long required(Long amount) {
        if (amount == null || amount < 0L) {
            throw new IllegalArgumentException("Payment totals cannot be negative");
        }
        return amount;
    }

    private static Map<String, Long> balances(Map<String, Long> source) {
        LinkedHashMap<String, Long> copy = new LinkedHashMap<String, Long>();
        source.forEach((currency, amount) -> {
            if (amount == null || amount < 0L) {
                throw new IllegalArgumentException("Balances cannot be negative");
            }
            if (amount > 0L) {
                copy.put((String)currency, (Long)amount);
            }
        });
        return copy;
    }

    private static Map<String, Long> decreases(Map<String, Long> initial, Map<String, Long> result) {
        LinkedHashMap<String, Long> values = new LinkedHashMap<String, Long>();
        initial.forEach((currency, amount) -> {
            long difference = amount - result.getOrDefault(currency, 0L);
            if (difference > 0L) {
                values.put((String)currency, difference);
            }
        });
        return values;
    }

    private static Map<String, Long> increases(Map<String, Long> initial, Map<String, Long> result) {
        LinkedHashMap<String, Long> values = new LinkedHashMap<String, Long>();
        result.forEach((currency, amount) -> {
            long difference = amount - initial.getOrDefault(currency, 0L);
            if (difference > 0L) {
                values.put((String)currency, difference);
            }
        });
        return values;
    }

    private CoinPaymentPlanner() {
    }

    public static final class PaymentPlan {
        public final boolean payable;
        public final String failureReason;
        public final Map<String, Long> inventoryDebits;
        public final Map<String, Long> walletDebits;
        public final Map<String, Long> inventoryChange;
        public final Map<String, Long> walletChange;
        public final List<Conversion> conversions;

        private PaymentPlan(boolean payable, String failureReason, Map<String, Long> inventoryDebits, Map<String, Long> walletDebits, Map<String, Long> inventoryChange, Map<String, Long> walletChange, List<Conversion> conversions) {
            this.payable = payable;
            this.failureReason = failureReason;
            this.inventoryDebits = PaymentPlan.immutable(inventoryDebits);
            this.walletDebits = PaymentPlan.immutable(walletDebits);
            this.inventoryChange = PaymentPlan.immutable(inventoryChange);
            this.walletChange = PaymentPlan.immutable(walletChange);
            this.conversions = List.copyOf(conversions);
        }

        public static PaymentPlan success(Map<String, Long> inventoryDebits, Map<String, Long> walletDebits, Map<String, Long> inventoryChange, Map<String, Long> walletChange, List<Conversion> conversions) {
            return new PaymentPlan(true, "", inventoryDebits, walletDebits, inventoryChange, walletChange, conversions);
        }

        public static PaymentPlan failure(String reason) {
            return new PaymentPlan(false, reason, Map.of(), Map.of(), Map.of(), Map.of(), List.of());
        }

        private static Map<String, Long> immutable(Map<String, Long> values) {
            return Collections.unmodifiableMap(new LinkedHashMap<String, Long>(values));
        }
    }

    public static enum PaymentLocation {
        INVENTORY,
        WALLET;

    }

    public record Conversion(String sourceCurrency, String targetCurrency, long sourceAmount, long targetAmount, PaymentLocation location) {
    }
}

