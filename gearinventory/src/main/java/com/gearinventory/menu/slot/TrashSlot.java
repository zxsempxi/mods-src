package com.gearinventory.menu.slot;

import com.gearinventory.trash.TrashContainer;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;

/** Placing is handled by the menu (the new item replaces the old one), so vanilla swap/merge/drag never touch it. */
public class TrashSlot extends Slot {
    public TrashSlot(TrashContainer c, int x, int y) {
        super(c, 0, x, y);
    }

    @Override
    public boolean mayPlace(ItemStack stack) {
        return false;
    }
}