/*
 * Decompiled with CFR 0.152.
 */
package net.sundggs.avecoins.client.gui;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.Map;
import net.minecraft.ChatFormatting;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.FormattedText;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.resources.Identifier;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.ItemLike;
import net.sundggs.avecoins.client.gui.GuiIconRenderer;
import net.sundggs.avecoins.client.gui.ScreenThemes;
import net.sundggs.avecoins.config.CraftingConfig;

final class PaymentReceipt {
    static Component info(String translationKey) {
        return Component.empty().append((Component)Component.literal((String)"[i]").withStyle(style -> style.withBold(Boolean.valueOf(true)).withColor(ScreenThemes.current().SOFT_WARNING & 0xFFFFFF))).append((Component)Component.literal((String)" ")).append((Component)Component.translatable((String)translationKey));
    }

    static Map<String, Long> combine(Map<String, Long> first, Map<String, Long> second) {
        LinkedHashMap<String, Long> result = new LinkedHashMap<String, Long>();
        if (first != null) {
            first.forEach((id, count) -> result.merge((String)id, (Long)count, Long::sum));
        }
        if (second != null) {
            second.forEach((id, count) -> result.merge((String)id, (Long)count, Long::sum));
        }
        return result;
    }

    static void row(GuiGraphicsExtractor graphics, Font font, String label, Map<String, Long> amounts, int x, int y, int labelWidth, int width) {
        MutableComponent heading = Component.translatable((String)label).withStyle(ChatFormatting.BOLD);
        graphics.pose().pushMatrix();
        graphics.pose().translate((float)x, (float)(y + 5));
        float labelScale = Math.min(1.0f, ((float)labelWidth - 8.0f) / (float)Math.max(1, font.width((FormattedText)heading)));
        graphics.pose().scale(labelScale, labelScale);
        graphics.text(font, (Component)heading, 0, 0, ScreenThemes.current().TEXT, false);
        graphics.pose().popMatrix();
        LinkedHashMap<String, Long> ordered = new LinkedHashMap<String, Long>();
        for (String id2 : CraftingConfig.COINS.reversed()) {
            if (amounts.getOrDefault(id2, 0L) <= 0L) continue;
            ordered.put(id2, amounts.get(id2));
        }
        amounts.forEach((id, count) -> {
            if (count > 0L) {
                ordered.putIfAbsent((String)id, (Long)count);
            }
        });
        int requiredWidth = ordered.values().stream().mapToInt(count -> 28 + font.width(count.toString())).sum();
        float scale = Math.min(1.0f, (float)(width - labelWidth) / (float)Math.max(1, requiredWidth));
        graphics.pose().pushMatrix();
        graphics.pose().translate((float)(x + labelWidth), (float)y);
        graphics.pose().scale(scale, scale);
        int cursor = 0;
        for (Map.Entry entry : ordered.entrySet()) {
            Identifier id3 = Identifier.tryParse((String)((String)entry.getKey()));
            if (id3 != null && BuiltInRegistries.ITEM.containsKey(id3)) {
                graphics.item(new ItemStack((ItemLike)BuiltInRegistries.ITEM.getValue(id3)), cursor, 0);
            }
            graphics.text(font, ((Long)entry.getValue()).toString(), cursor + 20, 5, ScreenThemes.current().TEXT, false);
            cursor += 28 + font.width(((Long)entry.getValue()).toString());
        }
        if (ordered.isEmpty()) {
            graphics.text(font, "\u2014", 0, 5, ScreenThemes.current().MUTED, false);
        }
        graphics.pose().popMatrix();
    }

    static void change(GuiGraphicsExtractor graphics, Font font, Map<String, Long> paid, Map<String, Long> returned, int x, int y, int width) {
        GuiIconRenderer.blit(graphics, Identifier.fromNamespaceAndPath((String)"avecoins", (String)"textures/gui/icons/change.png"), x, y + 1, 14, 14, 32, 32);
        graphics.text(font, (Component)Component.translatable((String)"gui.avecoins.receipt.change").withStyle(ChatFormatting.BOLD), x + 20, y + 5, ScreenThemes.current().TEXT, false);
        int labelWidth = 20 + PaymentReceipt.labelWidth(font);
        int needed = PaymentReceipt.amountWidth(font, paid) + 22 + PaymentReceipt.amountWidth(font, returned);
        float scale = Math.min(1.0f, (float)(width - labelWidth) / (float)Math.max(1, needed));
        graphics.pose().pushMatrix();
        graphics.pose().translate((float)(x + labelWidth), (float)y);
        graphics.pose().scale(scale, scale);
        int cursor = PaymentReceipt.amounts(graphics, font, paid, 0);
        graphics.text(font, "\u2192", cursor + 3, 5, ScreenThemes.current().MUTED, false);
        PaymentReceipt.amounts(graphics, font, returned, cursor + 22);
        graphics.pose().popMatrix();
    }

    static int labelWidth(Font font) {
        return 8 + Math.max(font.width((FormattedText)Component.translatable((String)"gui.avecoins.cart.total").withStyle(ChatFormatting.BOLD)), font.width((FormattedText)Component.translatable((String)"gui.avecoins.receipt.change").withStyle(ChatFormatting.BOLD)));
    }

    private static int amountWidth(Font font, Map<String, Long> amounts) {
        return amounts.values().stream().filter(value -> value > 0L).mapToInt(value -> 28 + font.width(value.toString())).sum();
    }

    private static int amounts(GuiGraphicsExtractor graphics, Font font, Map<String, Long> amounts, int cursor) {
        ArrayList<String> ids = new ArrayList<String>(CraftingConfig.COINS.reversed());
        ids.addAll(CraftingConfig.TICKETS);
        for (String currency : ids) {
            long value = amounts.getOrDefault(currency, 0L);
            if (value <= 0L) continue;
            Identifier id = Identifier.parse((String)currency);
            graphics.item(new ItemStack((ItemLike)BuiltInRegistries.ITEM.getValue(id)), cursor, 0);
            graphics.text(font, Long.toString(value), cursor + 20, 5, ScreenThemes.current().TEXT, false);
            cursor += 28 + font.width(Long.toString(value));
        }
        return cursor;
    }

    private PaymentReceipt() {
    }
}

