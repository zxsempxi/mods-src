package com.gearinventory.mixin;

import com.gearinventory.storage.GearGuards;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.RepairItemRecipe;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/** Combining two pieces in a crafting grid builds a new stack and would delete the contents of both. No event exists. */
@Mixin(RepairItemRecipe.class)
public abstract class RepairItemRecipeMixin {
    @Inject(method = "canCombine", at = @At("HEAD"), cancellable = true)
    private static void gearinventory$filled(ItemStack a, ItemStack b, CallbackInfoReturnable<Boolean> cir) {
        if (GearGuards.filled(a) || GearGuards.filled(b)) cir.setReturnValue(false);
    }
}
