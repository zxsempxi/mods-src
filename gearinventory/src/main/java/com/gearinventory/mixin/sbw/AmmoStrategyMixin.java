package com.gearinventory.mixin.sbw;

import com.gearinventory.compat.tacz.AmmoLookup;
import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.player.Player;
import net.neoforged.neoforge.capabilities.EntityCapability;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Pseudo;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Coerce;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/** Optional: Superb Warfare counts and takes ammo from the pockets too. Used for both server reload and client HUD. */
@Pseudo
@Mixin(targets = {"com.atsuishio.superbwarfare.data.gun.ammo_consumer_strategy.ItemAmmoStrategy",
        "com.atsuishio.superbwarfare.data.gun.ammo_consumer_strategy.PlayerAmmoStrategy"}, remap = false)
public abstract class AmmoStrategyMixin {
    @WrapOperation(method = {"consume(Lcom/atsuishio/superbwarfare/data/gun/GunData;Lcom/atsuishio/superbwarfare/data/gun/AmmoConsumer;Lnet/minecraft/world/entity/Entity;I)I",
            "count(Lcom/atsuishio/superbwarfare/data/gun/GunData;Lcom/atsuishio/superbwarfare/data/gun/AmmoConsumer;Lnet/minecraft/world/entity/Entity;)I"},
            require = 0, at = @At(value = "INVOKE", target = "Lnet/minecraft/world/entity/Entity;getCapability(Lnet/neoforged/neoforge/capabilities/EntityCapability;)Ljava/lang/Object;"))
    private Object gearinventory$ammo(Entity e, EntityCapability<?, ?> cap, Operation<Object> original) {
        return AmmoLookup.wrap(e, original.call(e, cap));
    }

    /** Ammo boxes are changed in place. */
    @Inject(method = "consume(Lcom/atsuishio/superbwarfare/data/gun/GunData;Lcom/atsuishio/superbwarfare/data/gun/AmmoConsumer;Lnet/minecraft/world/entity/Entity;I)I", at = @At("RETURN"), require = 0)
    private void gearinventory$flush(@Coerce Object data, @Coerce Object consumer, Entity e, int amount, CallbackInfoReturnable<Integer> cir) {
        if (e instanceof Player p) AmmoLookup.flush(p);
    }
}