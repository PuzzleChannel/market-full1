/*
 * Decompiled with CFR 0.152.
 */
package net.sundggs.avecoins.shop;

import com.mojang.brigadier.exceptions.CommandSyntaxException;
import com.mojang.serialization.DynamicOps;
import java.nio.charset.StandardCharsets;
import java.util.Optional;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.NbtOps;
import net.minecraft.nbt.Tag;
import net.minecraft.nbt.TagParser;
import net.minecraft.resources.RegistryOps;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;

public final class ShopItemStacks {
    public static String encode(HolderLookup.Provider registries, ItemStack source) {
        if (source.isEmpty()) {
            return "";
        }
        ItemStack single = source.copyWithCount(1);
        RegistryOps ops = RegistryOps.create((DynamicOps)NbtOps.INSTANCE, (HolderLookup.Provider)registries);
        return ((Tag)ItemStack.CODEC.encodeStart((DynamicOps)ops, (Object)single).getOrThrow()).toString();
    }

    public static Optional<ItemStack> decode(HolderLookup.Provider registries, String encoded) {
        if (encoded == null || encoded.isBlank()) {
            return Optional.empty();
        }
        if (encoded.getBytes(StandardCharsets.UTF_8).length > 32768) {
            return Optional.empty();
        }
        try {
            RegistryOps ops = RegistryOps.create((DynamicOps)NbtOps.INSTANCE, (HolderLookup.Provider)registries);
            CompoundTag tag = TagParser.parseCompoundFully((String)encoded);
            return ItemStack.CODEC.parse((DynamicOps)ops, (Object)tag).result().filter(stack -> !stack.isEmpty()).map(stack -> stack.copyWithCount(1));
        }
        catch (CommandSyntaxException | RuntimeException exception) {
            return Optional.empty();
        }
    }

    public static Optional<ItemStack> decodeExpected(HolderLookup.Provider registries, String encoded, Item expected) {
        return ShopItemStacks.decode(registries, encoded).filter(stack -> stack.is((Object)expected)).map(stack -> stack.copyWithCount(1));
    }

    private ShopItemStacks() {
    }
}

