/*
 * Decompiled with CFR 0.152.
 */
package net.sundggs.avecoins.message;

import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.server.level.ServerPlayer;

public final class ModMessages {
    private static MutableComponent prefix() {
        return Component.literal((String)"[").withStyle(ChatFormatting.GRAY).append((Component)Component.literal((String)"AVECOINS").withStyle(new ChatFormatting[]{ChatFormatting.WHITE, ChatFormatting.BOLD})).append((Component)Component.literal((String)"] - ").withStyle(ChatFormatting.GRAY));
    }

    public static Component translated(String key, Object ... arguments) {
        return ModMessages.prefix().append((Component)Component.translatable((String)key, (Object[])arguments).withStyle(ChatFormatting.WHITE));
    }

    public static void send(ServerPlayer player, String key, Object ... arguments) {
        player.sendSystemMessage(ModMessages.translated(key, arguments));
    }

    private ModMessages() {
    }
}

