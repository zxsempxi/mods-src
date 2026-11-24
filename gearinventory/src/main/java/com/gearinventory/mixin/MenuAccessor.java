package com.gearinventory.mixin;

import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Invoker;

/** addSlot and moveItemStackTo are protected; vanilla menus are not ours to subclass. */
@Mixin(AbstractContainerMenu.class)
public interface MenuAccessor {
    @Invoker("addSlot")
    Slot gearinventory$addSlot(Slot slot);

    @Invoker("moveItemStackTo")
    boolean gearinventory$move(ItemStack stack, int from, int to, boolean reverse);
}