/*
 * Decompiled with CFR 0.152.
 */
package net.sundggs.avecoins.shop;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.UUID;
import net.sundggs.avecoins.config.CraftingConfig;

public final class WalletData {
    public static final int SCHEMA_VERSION = 2;
    public static final int SLOT_COUNT = 27;
    public static final int STACK_SIZE = 64;
    public static final long MAX_BALANCE = 1728L;
    public int schemaVersion = 2;
    public Map<String, Map<String, Long>> wallets = new LinkedHashMap<String, Map<String, Long>>();

    public static WalletData defaults() {
        return new WalletData();
    }

    public static WalletData validate(WalletData input) {
        WalletData output = new WalletData();
        if (input.wallets == null) {
            return output;
        }
        for (Map.Entry<String, Map<String, Long>> wallet : input.wallets.entrySet()) {
            String playerId = UUID.fromString(wallet.getKey()).toString();
            LinkedHashMap<String, Long> balances = new LinkedHashMap<String, Long>();
            if (wallet.getValue() != null) {
                for (Map.Entry<String, Long> balance : wallet.getValue().entrySet()) {
                    String currencyId = balance.getKey();
                    if (CraftingConfig.LEGACY_COINS.contains(currencyId)) {
                        currencyId = CraftingConfig.CURRENCY;
                    }
                    if (!CraftingConfig.MANAGED_RESULTS.contains(currencyId) || balance.getValue() == null || balance.getValue() < 0L || balance.getValue() > 1728L) {
                        throw new IllegalArgumentException("Invalid wallet balance");
                    }
                    if (balance.getValue() <= 0L) continue;
                    balances.merge(currencyId, balance.getValue(), Math::addExact);
                }
            }
            WalletData.ensureFits(balances);
            if (balances.isEmpty()) continue;
            output.wallets.put(playerId, balances);
        }
        return output;
    }

    public WalletData copy() {
        WalletData copy = new WalletData();
        this.wallets.forEach((player, balances) -> copy.wallets.put((String)player, new LinkedHashMap(balances)));
        return copy;
    }

    public Map<String, Long> balances(UUID playerId) {
        Map<String, Long> balances = this.wallets.get(playerId.toString());
        return balances == null ? Map.of() : new LinkedHashMap<String, Long>(balances);
    }

    public long balance(UUID playerId, String currencyId) {
        return this.wallets.getOrDefault(playerId.toString(), Map.of()).getOrDefault(currencyId, 0L);
    }

    public void credit(UUID playerId, String currencyId, long amount) {
        if (amount < 0L) {
            throw new IllegalArgumentException("Wallet credit cannot be negative");
        }
        long updated = Math.addExact(this.balance(playerId, currencyId), amount);
        if (updated > 1728L) {
            throw new IllegalArgumentException("Wallet balance limit exceeded");
        }
        LinkedHashMap<String, Long> balances = new LinkedHashMap<String, Long>(this.balances(playerId));
        if (updated == 0L) {
            balances.remove(currencyId);
        } else {
            balances.put(currencyId, updated);
        }
        this.replaceBalances(playerId, balances);
    }

    public boolean debit(UUID playerId, String currencyId, long amount) {
        if (amount < 0L) {
            throw new IllegalArgumentException("Wallet debit cannot be negative");
        }
        long current = this.balance(playerId, currencyId);
        if (current < amount) {
            return false;
        }
        LinkedHashMap<String, Long> balances = new LinkedHashMap<String, Long>(this.balances(playerId));
        if (current == amount) {
            balances.remove(currencyId);
        } else {
            balances.put(currencyId, current - amount);
        }
        this.replaceBalances(playerId, balances);
        return true;
    }

    public void replaceBalances(UUID playerId, Map<String, Long> replacements) {
        LinkedHashMap<String, Long> balances = new LinkedHashMap<String, Long>();
        replacements.forEach((currencyId, value) -> {
            if (!CraftingConfig.MANAGED_RESULTS.contains(currencyId) || value == null || value < 0L || value > 1728L) {
                throw new IllegalArgumentException("Unsupported wallet balance");
            }
            if (value > 0L) {
                balances.put((String)currencyId, (Long)value);
            }
        });
        WalletData.ensureFits(balances);
        String key = playerId.toString();
        if (balances.isEmpty()) {
            this.wallets.remove(key);
        } else {
            this.wallets.put(key, balances);
        }
    }

    private static void ensureFits(Map<String, Long> balances) {
        long requiredSlots = 0L;
        for (long balance : balances.values()) {
            requiredSlots = Math.addExact(requiredSlots, (balance + 64L - 1L) / 64L);
        }
        if (requiredSlots > 27L) {
            throw new IllegalArgumentException("Wallet slot limit exceeded");
        }
    }
}

