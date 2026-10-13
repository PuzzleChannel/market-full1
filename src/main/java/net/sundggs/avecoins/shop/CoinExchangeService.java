/*
 * Decompiled with CFR 0.152.
 */
package net.sundggs.avecoins.shop;

import net.sundggs.avecoins.config.ModConfigManager;
import net.sundggs.avecoins.shop.CoinExchangeRates;

public final class CoinExchangeService {
    private static volatile CoinExchangeRates activeRates = CoinExchangeRates.defaults();

    public static void initialize() {
        activeRates = CoinExchangeRates.from(ModConfigManager.recipes());
    }

    public static CoinExchangeRates activeRates() {
        return activeRates;
    }

    public static void clear() {
        activeRates = CoinExchangeRates.defaults();
    }

    private CoinExchangeService() {
    }
}

