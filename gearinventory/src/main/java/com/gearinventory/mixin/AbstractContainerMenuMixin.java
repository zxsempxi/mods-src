package com.gearinventory.mixin;

import com.gearinventory.menu.GearInventoryMenu;
import com.gearinventory.menu.LootModeSlots;
import com.gearinventory.menu.slot.TrashSlot;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.ClickType;
import net.minecraft.world.item.ItemStack;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/** Remembers the loot-mode slots of a vanilla menu and gives its trash slot the replace-on-click behaviour. */
@Mixin(AbstractContainerMenu.class)
public abstract class AbstractContainerMenuMixin implements LootModeSlots.Holder {
    @Unique
    private LootModeSlots.Layout gearinventory$layout;

    @Override
    public LootModeSlots.Layout gearinventory$layout() {
        return gearinventory$layout;
    }

    @Override
    public void gearinventory$layout(LootModeSlots.Layout layout) {
        gearinventory$layout = layout;
    }

    @Inject(method = "clicked", at = @At("HEAD"), cancellable = true)
    private void gearinventory$trash(int index, int button, ClickType type, Player player, CallbackInfo ci) {
        if (gearinventory$layout == null || type != ClickType.PICKUP) return;
        AbstractContainerMenu self = (AbstractContainerMenu) (Object) this;
        if (index < 0 || index >= self.slots.size() || !(self.slots.get(index) instanceof TrashSlot) || self.getCarried().isEmpty()) return;
        if (GearInventoryMenu.trashable(self.getCarried())) {
            self.slots.get(index).set(self.getCarried());
            self.setCarried(ItemStack.EMPTY);
        }
        ci.cancel();
    }
}
