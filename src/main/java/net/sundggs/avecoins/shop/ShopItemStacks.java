package net.sundggs.avecoins.shop;

import com.mojang.brigadier.exceptions.CommandSyntaxException;
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
        RegistryOps<Tag> ops = RegistryOps.create(NbtOps.INSTANCE, registries);
        return ItemStack.CODEC.encodeStart(ops, single).getOrThrow().toString();
    }

    public static Optional<ItemStack> decode(HolderLookup.Provider registries, String encoded) {
        if (encoded == null || encoded.isBlank()) {
            return Optional.empty();
        }
        if (encoded.getBytes(StandardCharsets.UTF_8).length > 32768) {
            return Optional.empty();
        }
        try {
            RegistryOps<Tag> ops = RegistryOps.create(NbtOps.INSTANCE, registries);
            CompoundTag tag = TagParser.parseCompoundFully(encoded);
            return ItemStack.CODEC.parse(ops, tag)
                    .result()
                    .filter(stack -> !stack.isEmpty())
                    .map(stack -> stack.copyWithCount(1));
        } catch (CommandSyntaxException | RuntimeException exception) {
            return Optional.empty();
        }
    }

    public static Optional<ItemStack> decodeExpected(HolderLookup.Provider registries, String encoded, Item expected) {
        return ShopItemStacks.decode(registries, encoded)
                .filter(stack -> stack.is(expected))
                .map(stack -> stack.copyWithCount(1));
    }

    private ShopItemStacks() {
    }
}
