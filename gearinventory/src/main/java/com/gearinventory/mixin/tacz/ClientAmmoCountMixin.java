package com.gearinventory.mixin.tacz;

import com.gearinventory.compat.tacz.AmmoLookup;
import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.item.ItemStack;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Pseudo;
import org.spongepowered.asm.mixin.injection.At;

/** Optional: adds pocket ammo to the number on the gun HUD. */
@Pseudo
@Mixin(targets = "com.tacz.guns.client.gameplay.ClientAmmoCount", remap = false)
public abstract class ClientAmmoCountMixin {
    @WrapOperation(method = "get", require = 0, at = @At(value = "INVOKE", target = "Lnet/minecraft/world/entity/player/Inventory;getContainerSize()I"))
    private static int gearinventory$size(Inventory inv, Operation<Integer> original) {
        return original.call(inv) + AmmoLookup.EXTRA;
    }

    @WrapOperation(method = "get", require = 0, at = @At(value = "INVOKE", target = "Lnet/minecraft/world/entity/player/Inventory;getItem(I)Lnet/minecraft/world/item/ItemStack;"))
    private static ItemStack gearinventory$item(Inventory inv, int i, Operation<ItemStack> original) {
        int n = i - inv.getContainerSize();
        return n < 0 ? original.call(inv, i) : AmmoLookup.extra(inv.player, n);
    }
}