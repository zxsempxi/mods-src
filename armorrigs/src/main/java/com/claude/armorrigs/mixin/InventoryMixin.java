package com.claude.armorrigs.mixin;

import com.claude.armorrigs.BaseSlots;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.item.ItemStack;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(value = Inventory.class, priority = 500)
public abstract class InventoryMixin {
    @Inject(method = "getFreeSlot", at = @At("RETURN"), cancellable = true)
    private void armorrigs$freeSlot(CallbackInfoReturnable<Integer> cir) {
        Inventory self = (Inventory) (Object) this;
        int found = cir.getReturnValue();
        if (found < 0 || !BaseSlots.locked(self, found)) return;
        for (int i = found; i < self.items.size(); i++) {
            if (self.items.get(i).isEmpty() && !BaseSlots.locked(self, i)) {
                cir.setReturnValue(i);
                return;
            }
        }
        cir.setReturnValue(-1);
    }

    @Inject(method = "getSlotWithRemainingSpace", at = @At("RETURN"), cancellable = true)
    private void armorrigs$stackSlot(ItemStack stack, CallbackInfoReturnable<Integer> cir) {
        Inventory self = (Inventory) (Object) this;
        int found = cir.getReturnValue();
        if (found < 0 || !BaseSlots.locked(self, found)) return;
        for (int i = found + 1; i < self.items.size(); i++) {
            ItemStack in = self.items.get(i);
            if (!BaseSlots.locked(self, i) && !in.isEmpty() && in.isStackable()
                    && in.getCount() < self.getMaxStackSize(in) && ItemStack.isSameItemSameComponents(in, stack)) {
                cir.setReturnValue(i);
                return;
            }
        }
        cir.setReturnValue(-1);
    }

    // Runs before Grid Inventory, which cancels add() at HEAD and knows nothing about locked slots.
    @Inject(method = "add(Lnet/minecraft/world/item/ItemStack;)Z", at = @At("HEAD"), cancellable = true)
    private void armorrigs$add(ItemStack stack, CallbackInfoReturnable<Boolean> cir) {
        Inventory self = (Inventory) (Object) this;
        if (!self.player.getAbilities().instabuild) cir.setReturnValue(BaseSlots.insert(self, stack));
    }

    @Inject(method = "add(ILnet/minecraft/world/item/ItemStack;)Z", at = @At("HEAD"), cancellable = true)
    private void armorrigs$addAt(int slot, ItemStack stack, CallbackInfoReturnable<Boolean> cir) {
        Inventory self = (Inventory) (Object) this;
        if (self.player.getAbilities().instabuild) return;
        if (slot < 0 || BaseSlots.locked(self, slot)) cir.setReturnValue(BaseSlots.insert(self, stack));
    }
}