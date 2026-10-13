/*
 * Decompiled with CFR 0.152.
 */
package net.sundggs.avecoins.config;

import java.util.Locale;
import java.util.regex.Pattern;

public final class ShopColor {
    private static final Pattern HEX = Pattern.compile("#?[0-9a-fA-F]{6}");

    public static String normalize(String value) {
        String color;
        String string = color = value == null ? "" : value.trim();
        if (color.isEmpty()) {
            return "";
        }
        if (!HEX.matcher(color).matches()) {
            throw new IllegalArgumentException("Shop color must contain six hexadecimal digits");
        }
        return "#" + color.replace("#", "").toUpperCase(Locale.ROOT);
    }

    public static boolean isValid(String value) {
        try {
            ShopColor.normalize(value);
            return true;
        }
        catch (IllegalArgumentException exception) {
            return false;
        }
    }

    private ShopColor() {
    }
}

