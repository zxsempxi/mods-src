package com.gearinventory.mixin.tacz;

import com.gearinventory.compat.tacz.AmmoLookup;
import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.neoforged.neoforge.capabilities.EntityCapability;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Pseudo;
import org.spongepowered.asm.mixin.injection.At;

/** Optional: TaCZ has no API to add ammo sources. Skipped when TaCZ is absent or changed. */
@Pseudo
@Mixin(targets = "com.tacz.guns.api.item.gun.AbstractGunItem", remap = false)
public abstract class AbstractGunItemMixin {
    @WrapOperation(method = {"canReload", "hasInventoryAmmo"}, require = 0, at = @At(value = "INVOKE",
            target = "Lnet/minecraft/world/entity/LivingEntity;getCapability(Lnet/neoforged/neoforge/capabilities/EntityCapability;Ljava/lang/Object;)Ljava/lang/Object;"))
    private Object gearinventory$ammo(LivingEntity e, EntityCapability<?, ?> cap, Object ctx, Operation<Object> original) {
        return AmmoLookup.wrap(e, original.call(e, cap, ctx));
    }

    @org.spongepowered.asm.mixin.injection.Inject(method = "findAndExtractInventoryAmmo", at = @At("RETURN"), require = 0)
    private void gearinventory$flush(net.neoforged.neoforge.items.IItemHandler handler, net.minecraft.world.item.ItemStack gun, int need, org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable<Integer> cir) {
        if (handler instanceof AmmoLookup.Handler h) h.flush();
    }}