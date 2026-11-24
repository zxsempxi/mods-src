package com.gearinventory.menu.slot;

import com.gearinventory.storage.ArmorStorage;
import com.gearinventory.storage.ArmorStorageContainer;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;

public class ArmorGridSlot extends Slot {
    public final ArmorStorageContainer storage;
    /** Client only: false while scrolled out of the column. */
    public boolean visible = true;

    public ArmorGridSlot(ArmorStorageContainer storage, int index) {
        super(storage, index, -10000, -10000);
        this.storage = storage;
    }

    public boolean inRange() {
        return getContainerSlot() < storage.size().slots();
    }

    @Override
    public boolean isActive() {
        return inRange() && visible;
    }

    @Override
    public boolean mayPlace(ItemStack stack) {
        return inRange() && ArmorStorage.mayStore(stack);
    }

    @Override
    public boolean mayPickup(Player player) {
        return inRange();
    }
}