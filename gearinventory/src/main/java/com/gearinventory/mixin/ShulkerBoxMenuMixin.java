package com.gearinventory.mixin;

import com.gearinventory.menu.LootModeSlots;
import net.minecraft.world.Container;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.ShulkerBoxMenu;
import net.minecraft.world.inventory.MenuType;
import net.minecraft.world.item.ItemStack;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/** Loot mode: extra slots at the end plus shift-click that knows about them. There is no event for either. */
@Mixin(ShulkerBoxMenu.class)
public abstract class ShulkerBoxMenuMixin {
    @Inject(method = "<init>(ILnet/minecraft/world/entity/player/Inventory;Lnet/minecraft/world/Container;)V", at = @At("TAIL"))
    private void gearinventory$add(int id, Inventory inv, Container c, CallbackInfo ci) {
        LootModeSlots.add((AbstractContainerMenu) (Object) this, ShulkerBoxMenu.class, inv);
    }

    @Inject(method = "quickMoveStack", at = @At("HEAD"), cancellable = true)
    private void gearinventory$move(Player player, int index, CallbackInfoReturnable<ItemStack> cir) {
        AbstractContainerMenu self = (AbstractContainerMenu) (Object) this;
        if (LootModeSlots.layout(self) != null) cir.setReturnValue(LootModeSlots.quickMove(self, player, index));
    }
}