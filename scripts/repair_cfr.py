from pathlib import Path

ROOT = Path("src/main/java")

# CFR emitted casts to raw types that were only necessary for bytecode reconstruction.
# Removing them lets Java 25 infer the real Minecraft/Fabric generic signatures again.
for path in ROOT.rglob("*.java"):
    text = path.read_text(encoding="utf-8")
    text = text.replace("(GuiEventListener)", "")
    text = text.replace("(Object)", "")
    text = text.replace("(Registry)", "")
    text = text.replace("(ResourceKey)", "")
    path.write_text(text, encoding="utf-8")

def patch(relative_path, replacements):
    path = ROOT / relative_path
    text = path.read_text(encoding="utf-8")
    original = text
    for old, new in replacements:
        if old not in text:
            raise SystemExit(f"Expected CFR pattern was not found in {relative_path}: {old[:100]!r}")
        text = text.replace(old, new)
    if text != original:
        path.write_text(text, encoding="utf-8")

patch("net/sundggs/avecoins/config/AtomicJsonStore.java", [
    ("Object candidate = GSON.fromJson(root, this.type);", "T candidate = GSON.fromJson(root, this.type);"),
    ("Object validated;", "T validated;"),
])

patch("net/sundggs/avecoins/config/RecipesConfig.java", [
    ("for (Map.Entry<String, Object> entry : input.customRecipes.entrySet()) {",
     "for (Map.Entry<String, RecipeDefinition> entry : input.customRecipes.entrySet()) {"),
    ("RecipeDefinition definition = (RecipeDefinition)entry.getValue();", "RecipeDefinition definition = entry.getValue();"),
    ("for (Map.Entry<String, Object> entry : input.downgradeCounts.entrySet()) {",
     "for (Map.Entry<String, Integer> entry : input.downgradeCounts.entrySet()) {"),
    ("Integer count = (Integer)entry.getValue();", "Integer count = entry.getValue();"),
])

patch("net/sundggs/avecoins/shop/ModMenus.java", [
    ("new ExtendedMenuType(SellBoxMenu::new,", "new ExtendedMenuType<>(SellBoxMenu::new,"),
    ("new ExtendedMenuType(WalletMenu::new,", "new ExtendedMenuType<>(WalletMenu::new,"),
])

patch("net/sundggs/avecoins/client/gui/AutocompleteField.java", [
    ("Comparator.comparing(candidate -> !AutocompleteField.preferred(candidate, query)).thenComparing(Function.identity())",
     "Comparator.comparing((String candidate) -> !AutocompleteField.preferred(candidate, query)).thenComparing(Function.identity())"),
])

patch("net/sundggs/avecoins/client/gui/AvecoinsScreen.java", [
    ("List lines = this.font.split(", "List<FormattedCharSequence> lines = this.font.split("),
])

patch("net/sundggs/avecoins/client/gui/PurchaseHistoryScreen.java", [
    ("List<Object> lines = record.lines == null ? List.of() : record.lines;",
     "List<ShopData.PurchaseLine> lines = record.lines == null ? List.of() : record.lines;"),
    ("ShopData.PurchaseLine line = (ShopData.PurchaseLine)lines.get(index);",
     "ShopData.PurchaseLine line = lines.get(index);"),
])

patch("net/sundggs/avecoins/client/gui/ShopScreen.java", [
    ("() -> this.setQuantity(product.id, 0)", "() -> this.setQuantity(product2.id, 0)"),
])

# Restore parameterized codec operations, which CFR had emitted as raw DynamicOps.
shop_item_stacks = ROOT / "net/sundggs/avecoins/shop/ShopItemStacks.java"
shop_item_stacks.write_text("""package net.sundggs.avecoins.shop;

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
""", encoding="utf-8")
