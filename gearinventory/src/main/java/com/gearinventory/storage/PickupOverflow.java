package com.gearinventory.storage;

import com.gearinventory.config.ServerConfig;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.neoforged.neoforge.common.util.TriState;
import net.neoforged.neoforge.event.entity.player.ItemEntityPickupEvent;

/** Pickup order: backpack upgrades (earlier listeners), vanilla inventory, then armor storage for what does not fit. */
public final class PickupOverflow {
    private PickupOverflow() {}

    public static void onPickup(ItemEntityPickupEvent.Pre event) {
        ItemEntity entity = event.getItemEntity();
        Player player = event.getPlayer();
        ItemStack stack = entity.getItem();
        if (player.level().isClientSide || !ServerConfig.storePickups() || stack.isEmpty() || event.canPickup().isFalse()) return;
        if (entity.hasPickUpDelay() || entity.getTarget() != null && !entity.getTarget().equals(player.getUUID())) return;
        int excess = ServerConfig.pickupToStorageFirst() ? stack.getCount() : stack.getCount() - room(player.getInventory(), stack);
        if (excess <= 0) return;
        ItemStack rest = stack.copyWithCount(excess);
        int moved = excess - insert(player, rest).getCount();
        if (moved == 0) return;
        player.take(entity, moved);
        // Vanilla already holds this exact stack instance; replacing it on the entity would duplicate items.
        stack.shrink(moved);
        if (stack.isEmpty()) {
            event.setCanPickup(TriState.FALSE);
            entity.discard();
        }
    }

    public static int room(Inventory inv, ItemStack stack) {
        int room = 0, max = inv.getMaxStackSize(stack);
        for (int i = 0; i <= 40; i++) {
            if (i >= 36 && i != 40) continue;
            ItemStack in = inv.getItem(i);
            if (in.isEmpty()) room += i < 36 ? max : 0;
            else if (ItemStack.isSameItemSameComponents(in, stack)) room += Math.max(0, max - in.getCount());
        }
        return room;
    }

    /** Returns what did not fit. */
    public static ItemStack insert(Player player, ItemStack stack) {
        ItemStack rest = stack.copy();
        if (!ArmorStorage.mayStore(rest)) return rest;
        for (int pass = 0; pass < 2; pass++) {
            for (EquipmentSlot e : new EquipmentSlot[]{EquipmentSlot.CHEST, EquipmentSlot.LEGS, EquipmentSlot.FEET, EquipmentSlot.HEAD}) {
                ArmorStorageContainer c = new ArmorStorageContainer(player, e);
                for (int i = 0; i < c.size().slots() && !rest.isEmpty(); i++) {
                    ItemStack in = c.getItem(i);
                    if (pass == 1 && in.isEmpty()) c.setItem(i, rest.split(rest.getMaxStackSize()));
                    else if (pass == 0 && !in.isEmpty() && ItemStack.isSameItemSameComponents(in, rest)) {
                        int n = Math.min(rest.getCount(), in.getMaxStackSize() - in.getCount());
                        if (n > 0) {
                            in.grow(n);
                            rest.shrink(n);
                            c.setChanged();
                        }
                    }
                }
            }
        }
        return rest;
    }
}