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
@Mixin(targets = "com.tacz.guns.client.animation.statemachine.GunAnimationStateContext", remap = false)
public abstract class GunAnimationStateContextMixin {
    @WrapOperation(method = {"lambda$hasAmmoToConsume$8"}, require = 0, at = @At(value = "INVOKE",
            target = "Lnet/minecraft/world/entity/Entity;getCapability(Lnet/neoforged/neoforge/capabilities/EntityCapability;Ljava/lang/Object;)Ljava/lang/Object;"))
    private Object gearinventory$ammo(Entity e, EntityCapability<?, ?> cap, Object ctx, Operation<Object> original) {
        return AmmoLookup.wrap(e, original.call(e, cap, ctx));
    }
}