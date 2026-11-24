package com.gearinventory.mixin;

import com.gearinventory.storage.GearGuards;
import net.minecraft.world.item.crafting.RecipeInput;
import net.minecraft.world.item.crafting.RecipeManager;
import net.minecraft.world.item.crafting.RecipeType;
import net.minecraft.world.level.Level;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import java.util.List;
import java.util.Optional;

/** Filled gear is never smelted: one place covers furnaces, hoppers feeding them and modded machines using vanilla lookups. */
@Mixin(RecipeManager.class)
public abstract class RecipeManagerMixin {
    @Inject(method = "getRecipeFor(Lnet/minecraft/world/item/crafting/RecipeType;Lnet/minecraft/world/item/crafting/RecipeInput;Lnet/minecraft/world/level/Level;Lnet/minecraft/world/item/crafting/RecipeHolder;)Ljava/util/Optional;",
            at = @At("HEAD"), cancellable = true)
    private void gearinventory$one(RecipeType<?> type, RecipeInput input, Level level, net.minecraft.world.item.crafting.RecipeHolder<?> last,
                                   CallbackInfoReturnable<Optional<?>> cir) {
        if (GearGuards.blocksProcessing(input)) cir.setReturnValue(Optional.empty());
    }

    @Inject(method = "getRecipesFor", at = @At("HEAD"), cancellable = true)
    private void gearinventory$all(RecipeType<?> type, RecipeInput input, Level level, CallbackInfoReturnable<List<?>> cir) {
        if (GearGuards.blocksProcessing(input)) cir.setReturnValue(List.of());
    }
}
