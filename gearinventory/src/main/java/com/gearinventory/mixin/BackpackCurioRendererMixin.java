package com.gearinventory.mixin;

import com.gearinventory.config.ClientConfig;
import com.llamalad7.mixinextras.sugar.Local;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Pseudo;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import top.theillusivec4.curios.api.SlotContext;

/** Optional: hides the Sophisticated Backpacks model while gliding. Skipped when that mod is absent. */
@Pseudo
@Mixin(targets = "net.p3pp3rf1y.sophisticatedbackpacks.compat.curios.BackpackCurioRenderer", remap = false)
public abstract class BackpackCurioRendererMixin {
    @Inject(method = "render", at = @At("HEAD"), cancellable = true, require = 0)
    private void gearinventory$hide(CallbackInfo ci, @Local(argsOnly = true) SlotContext ctx) {
        if (ClientConfig.HIDE_BACKPACK_IN_FLIGHT.get() && ctx.entity().isFallFlying()) ci.cancel();
    }
}