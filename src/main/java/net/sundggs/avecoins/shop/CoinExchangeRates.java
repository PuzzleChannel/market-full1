/*
 * Decompiled with CFR 0.152.
 */
package net.sundggs.avecoins.shop;

import java.util.LinkedHashMap;
import java.util.Map;
import net.sundggs.avecoins.config.CraftingConfig;
import net.sundggs.avecoins.config.RecipesConfig;

public final class CoinExchangeRates {
    private final Map<String, Integer> downgradeCounts;

    private CoinExchangeRates(Map<String, Integer> downgradeCounts) {
        this.downgradeCounts = Map.copyOf(downgradeCounts);
    }

    public static CoinExchangeRates from(RecipesConfig config) {
        LinkedHashMap<String, Integer> counts = new LinkedHashMap<String, Integer>();
        for (String source : RecipesConfig.DOWNGRADE_RESULTS.keySet()) {
            counts.put(source, config.downgradeCounts.getOrDefault(source, 4));
        }
        return new CoinExchangeRates(counts);
    }

    public static CoinExchangeRates defaults() {
        return CoinExchangeRates.from(RecipesConfig.defaults());
    }

    public int downgradeCount(String sourceCurrency) {
        return this.downgradeCounts.getOrDefault(sourceCurrency, 0);
    }

    public boolean isCoin(String currency) {
        return CraftingConfig.COINS.contains(currency);
    }

    public long valueIn(String sourceCurrency, String targetCurrency) {
        int source = CraftingConfig.COINS.indexOf(sourceCurrency);
        int target = CraftingConfig.COINS.indexOf(targetCurrency);
        if (source < 0 || target < 0 || source < target) {
            return 0L;
        }
        long value = 1L;
        for (int index = source; index > target; --index) {
            value = Math.multiplyExact(value, this.downgradeCount(CraftingConfig.COINS.get(index)));
        }
        return value;
    }
}

