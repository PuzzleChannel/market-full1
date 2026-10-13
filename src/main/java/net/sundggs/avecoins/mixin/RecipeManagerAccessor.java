/*
 * Decompiled with CFR 0.152.
 */
package net.sundggs.avecoins.mixin;

import net.minecraft.world.item.crafting.RecipeManager;
import net.minecraft.world.item.crafting.RecipeMap;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

@Mixin(value={RecipeManager.class})
public interface RecipeManagerAccessor {
    @Accessor(value="recipes")
    public RecipeMap avecoins$getRecipes();

    @Accessor(value="recipes")
    public void avecoins$setRecipes(RecipeMap var1);
}

