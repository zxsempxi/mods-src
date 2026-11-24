package com.gearinventory.mixin;

import com.gearinventory.compat.curios.CuriosCompat;
import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.ItemStack;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;

/** Keeps gliding and wears down the elytra from the slot. No NeoForge event exposes this lookup. */
@Mixin(LivingEntity.class)
public abstract class LivingEntityFlightMixin {
    @WrapOperation(method = "updateFallFlying", at = @At(value = "INVOKE", target = "Lnet/minecraft/world/entity/LivingEntity;getItemBySlot(Lnet/minecraft/world/entity/EquipmentSlot;)Lnet/minecraft/world/item/ItemStack;"))
    private ItemStack gearinventory$elytra(LivingEntity self, EquipmentSlot slot, Operation<ItemStack> original) {
        ItemStack worn = original.call(self, slot);
        return slot == EquipmentSlot.CHEST ? CuriosCompat.flightItem(self, worn) : worn;
    }
}