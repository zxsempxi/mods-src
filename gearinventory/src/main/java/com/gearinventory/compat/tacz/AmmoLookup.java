package com.gearinventory.compat.tacz;

import com.gearinventory.storage.ArmorStorage;
import com.gearinventory.storage.ArmorStorageContainer;
import com.gearinventory.storage.StorageSize;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.neoforged.neoforge.items.IItemHandler;

import java.util.Map;

/** Shows TaCZ the vanilla inventory first and armor storage after it. Used only inside TaCZ, other mods see no change. */
public final class AmmoLookup {
    public static final int EXTRA = 4 * StorageSize.MAX_SLOTS;
    private static final EquipmentSlot[] ORDER = {EquipmentSlot.CHEST, EquipmentSlot.LEGS, EquipmentSlot.FEET, EquipmentSlot.HEAD};
    private static final Map<Player, ArmorStorageContainer[]> VIEWS = new com.google.common.collect.MapMaker().weakKeys().makeMap();

    private AmmoLookup() {}

    /** The containers cache their decoded contents, so keep one set per player object. Entity.equals compares ids, which the client and server player share in singleplayer, so keys are compared by identity. */
    private static synchronized ArmorStorageContainer[] view(Player p) {
        return VIEWS.computeIfAbsent(p, k -> {
            ArmorStorageContainer[] c = new ArmorStorageContainer[4];
            for (int i = 0; i < 4; i++) c[i] = new ArmorStorageContainer(k, ORDER[i]);
            return c;
        });
    }

    public static ItemStack extra(Player p, int index) {
        return index < 0 || index >= EXTRA ? ItemStack.EMPTY
                : view(p)[index / StorageSize.MAX_SLOTS].getItem(index % StorageSize.MAX_SLOTS);
    }

    public static Object wrap(Entity entity, Object original) {
        return entity instanceof Player p && original instanceof IItemHandler h ? new Handler(p, h) : original;
    }

    public static void flush(Player p) {
        for (ArmorStorageContainer c : view(p)) flush(c);
    }

    /** Ammo boxes are changed in place, so write every storage back, also one that has just become empty. */
    private static void flush(ArmorStorageContainer c) {
        c.isEmpty();
        if (c.armor().has(net.minecraft.core.component.DataComponents.CONTAINER)) c.setChanged();
    }

    public static final class Handler implements IItemHandler {
        private final IItemHandler base;
        private final ArmorStorageContainer[] storages;

        Handler(Player p, IItemHandler base) {
            this.base = base;
            this.storages = view(p);
        }

        private ArmorStorageContainer at(int extra) {
            return storages[extra / StorageSize.MAX_SLOTS];
        }

        public void flush() {
            for (ArmorStorageContainer c : storages) AmmoLookup.flush(c);
        }

        @Override
        public int getSlots() {
            return base.getSlots() + EXTRA;
        }

        @Override
        public ItemStack getStackInSlot(int i) {
            int n = i - base.getSlots();
            return n < 0 ? base.getStackInSlot(i) : at(n).getItem(n % StorageSize.MAX_SLOTS);
        }

        @Override
        public ItemStack extractItem(int i, int amount, boolean simulate) {
            int n = i - base.getSlots();
            if (n < 0) return base.extractItem(i, amount, simulate);
            int k = n % StorageSize.MAX_SLOTS;
            if (simulate) {
                ItemStack in = at(n).getItem(k);
                return in.copyWithCount(Math.min(amount, in.getCount()));
            }
            return at(n).removeItem(k, amount);
        }

        @Override
        public ItemStack insertItem(int i, ItemStack stack, boolean simulate) {
            return i < base.getSlots() ? base.insertItem(i, stack, simulate) : stack;
        }

        @Override
        public int getSlotLimit(int i) {
            return i < base.getSlots() ? base.getSlotLimit(i) : 64;
        }

        @Override
        public boolean isItemValid(int i, ItemStack stack) {
            return i < base.getSlots() ? base.isItemValid(i, stack) : ArmorStorage.mayStore(stack);
        }
    }
}
