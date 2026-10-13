/*
 * Decompiled with CFR 0.152.
 */
package net.sundggs.avecoins.config;

import java.util.Locale;
import java.util.regex.Pattern;
import net.sundggs.avecoins.config.ShopColor;

public final class GeneralConfig {
    public static final int SCHEMA_VERSION = 5;
    private static final Pattern COMMAND = Pattern.compile("[a-z0-9_-]{1,32}");
    public int schemaVersion = 5;
    public boolean shopEnabled = true;
    public boolean automaticCoinConversion = false;
    public String shopName = "AVESHOP";
    public String shopColor = "";
    public String customShopCommand = "";

    public static GeneralConfig defaults() {
        return new GeneralConfig();
    }

    public static GeneralConfig validate(GeneralConfig input) {
        String command;
        GeneralConfig output = new GeneralConfig();
        output.schemaVersion = 5;
        output.shopEnabled = input.shopEnabled;
        output.automaticCoinConversion = input.automaticCoinConversion;
        String shopName = GeneralConfig.migrateDefault(input.shopName, input.schemaVersion, "AVECOINS Shop");
        output.shopName = GeneralConfig.bounded(shopName, "AVESHOP", 48);
        output.shopColor = ShopColor.normalize(input.shopColor);
        String string = command = input.customShopCommand == null ? "" : input.customShopCommand.trim().toLowerCase(Locale.ROOT);
        if (command.startsWith("/")) {
            command = command.substring(1).trim();
        }
        if (!command.isEmpty() && (!COMMAND.matcher(command).matches() || command.equals("avc") || command.equals("avecoins"))) {
            throw new IllegalArgumentException("The custom shop command is invalid or reserved");
        }
        output.customShopCommand = command;
        return output;
    }

    private static String migrateDefault(String value, int schemaVersion, String previousDefault) {
        if (schemaVersion < 4 && value != null && value.trim().equals(previousDefault)) {
            return "AVESHOP";
        }
        return value;
    }

    private static String bounded(String value, String fallback, int maxLength) {
        String normalized;
        String string = normalized = value == null ? fallback : value.trim();
        if (normalized.isEmpty()) {
            normalized = fallback;
        }
        if (normalized.length() > maxLength) {
            throw new IllegalArgumentException("Text exceeds the maximum length of " + maxLength);
        }
        return normalized;
    }
}

