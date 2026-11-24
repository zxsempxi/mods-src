package com.claude.armorrigs;

import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.item.ItemStack;

/** Base inventory: hotbar plus the left 3x3 block of the main grid. */
public final class BaseSlots {
    private BaseSlots() {}

    public static boolean locked(Inventory inv, int index) {
        if (inv.player.getAbilities().instabuild) return false;
        return index >= 9 && index < 36 && index % 9 >= 3;
    }

    /** Inserts into open slots only (selected, offhand stack, then the rest). Shrinks stack, true if anything moved. */
    public static boolean insert(Inventory inv, ItemStack stack) {
        if (stack.isEmpty()) return false;
        int before = stack.getCount();
        if (stack.isStackable()) {
            merge(inv, inv.selected, stack);
            merge(inv, Inventory.SLOT_OFFHAND, stack);
            for (int i = 0; i < 36; i++) merge(inv, i, stack);
        }
        for (int i = 0; i < 36 && !stack.isEmpty(); i++) {
            if (!locked(inv, i) && inv.getItem(i).isEmpty()) {
                ItemStack put = stack.split(Math.min(stack.getCount(), inv.getMaxStackSize(stack)));
                put.setPopTime(5);
                inv.setItem(i, put);
            }
        }
        return stack.getCount() < before;
    }

    private static void merge(Inventory inv, int i, ItemStack stack) {
        if (stack.isEmpty() || locked(inv, i)) return;
        ItemStack in = inv.getItem(i);
        if (in.isEmpty() || !ItemStack.isSameItemSameComponents(in, stack)) return;
        int n = Math.min(stack.getCount(), inv.getMaxStackSize(in) - in.getCount());
        if (n <= 0) return;
        in.grow(n);
        in.setPopTime(5);
        stack.shrink(n);
    }
}