package com.gearinventory.menu;

import com.gearinventory.compat.curios.CuriosCompat;
import com.gearinventory.menu.slot.ArmorGridSlot;
import com.gearinventory.menu.slot.CurioProxySlot;
import com.gearinventory.menu.slot.EquipmentMenuSlot;
import com.gearinventory.menu.slot.TrashSlot;
import com.gearinventory.mixin.MenuAccessor;
import com.gearinventory.storage.ArmorStorageContainer;
import com.gearinventory.storage.StorageSize;
import com.gearinventory.trash.TrashContainer;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

/**
 * Appends the same slots, in the same order and number, on both sides. Nothing depends on worn gear, so indices cannot
 * drift. Order: container, inventory 27, hotbar 9 (vanilla), then storage, armor 4, offhand, back, elytra, trash.
 */
public final class LootModeSlots {
    private static final int HIDDEN = -10000;
    private static final EquipmentSlot[] ARMOR = {EquipmentSlot.HEAD, EquipmentSlot.CHEST, EquipmentSlot.LEGS, EquipmentSlot.FEET};

    public interface Holder {
        Layout gearinventory$layout();

        void gearinventory$layout(Layout layout);
    }

    public record Layout(int container, int base, ArmorStorageContainer[] storages) {
        public int player() { return container; }
        public int hotbar() { return container + 27; }
        public int storageEnd() { return base + 4 * StorageSize.MAX_SLOTS; }
        public int armor() { return storageEnd(); }
        public int offhand() { return armor() + 4; }
        public int back() { return offhand() + 1; }
        public int elytra() { return back() + 1; }
        public int trash() { return elytra() + 1; }
        public int size() { return trash() + 1; }
    }

    private LootModeSlots() {}

    public static Layout layout(AbstractContainerMenu menu) {
        return ((Holder) menu).gearinventory$layout();
    }

    public static void add(AbstractContainerMenu menu, Class<?> exact, Inventory inv) {
        // Modded subclasses lay out their own screens, so they stay untouched in v1.
        if (menu.getClass() != exact && !com.gearinventory.config.ServerConfig.lootMenuAllowed(menu.getClass().getName()) || layout(menu) != null) return;
        MenuAccessor m = (MenuAccessor) menu;
        int base = menu.slots.size();
        ArmorStorageContainer[] storages = new ArmorStorageContainer[4];
        for (int i = 0; i < 4; i++) {
            storages[i] = new ArmorStorageContainer(inv.player, ARMOR[i]);
            for (int k = 0; k < StorageSize.MAX_SLOTS; k++) m.gearinventory$addSlot(new ArmorGridSlot(storages[i], k));
        }
        for (int i = 0; i < 4; i++) m.gearinventory$addSlot(new EquipmentMenuSlot(inv, ARMOR[i], 39 - i, HIDDEN, HIDDEN));
        m.gearinventory$addSlot(new EquipmentMenuSlot(inv, EquipmentSlot.OFFHAND, 40, HIDDEN, HIDDEN));
        m.gearinventory$addSlot(new CurioProxySlot(inv.player, CuriosCompat.BACK, HIDDEN, HIDDEN));
        m.gearinventory$addSlot(new CurioProxySlot(inv.player, CuriosCompat.ELYTRA, HIDDEN, HIDDEN));
        m.gearinventory$addSlot(new TrashSlot(new TrashContainer(inv.player), HIDDEN, HIDDEN));
        ((Holder) menu).gearinventory$layout(new Layout(base - 36, base, storages));
    }

    /** Vanilla uses slots.size() as "end of player inventory", which is no longer true, so shift-click is replaced. */
    public static ItemStack quickMove(AbstractContainerMenu menu, Player player, int index) {
        Layout l = layout(menu);
        Slot slot = menu.slots.get(index);
        if (!slot.hasItem() || !slot.mayPickup(player)) return ItemStack.EMPTY;
        MenuAccessor m = (MenuAccessor) menu;
        ItemStack stack = slot.getItem(), original = stack.copy();
        boolean moved;
        if (index < l.container()) {
            moved = m.gearinventory$move(stack, l.player(), l.base(), true);
            if (!stack.isEmpty()) moved |= m.gearinventory$move(stack, l.base(), l.storageEnd(), false);
        } else if (index < l.base()) {
            moved = m.gearinventory$move(stack, 0, l.container(), false);
        } else if (index < l.storageEnd()) {
            moved = m.gearinventory$move(stack, 0, l.container(), false);
            if (!stack.isEmpty()) moved |= m.gearinventory$move(stack, l.player(), l.base(), false);
        } else {
            moved = m.gearinventory$move(stack, l.player(), l.base(), false);
        }
        if (!moved || stack.getCount() == original.getCount()) return ItemStack.EMPTY;
        if (stack.isEmpty()) slot.setByPlayer(ItemStack.EMPTY, original);
        else slot.setChanged();
        slot.onTake(player, stack);
        return original;
    }

    public static boolean trash(AbstractContainerMenu menu, Player p, int index) {
        Layout l = layout(menu);
        if (l == null || index < 0 || index >= l.size() || index == l.trash() || !menu.getCarried().isEmpty()) return false;
        Slot slot = menu.slots.get(index);
        if (!slot.hasItem() || !slot.mayPickup(p) || !GearInventoryMenu.trashable(slot.getItem())) return false;
        ItemStack stack = slot.getItem().copy();
        slot.setByPlayer(ItemStack.EMPTY, stack);
        menu.slots.get(l.trash()).set(stack);
        return true;
    }

    public static void takeAll(AbstractContainerMenu menu, Player p) {
        Layout l = layout(menu);
        for (int i = 0; i < l.container(); i++) quickMove(menu, p, i);
    }

    /** Like Terraria's quick stack: only items the container already has, and the hotbar is left alone. */
    public static void depositAll(AbstractContainerMenu menu) {
        Layout l = layout(menu);
        for (int i = l.player(); i < l.storageEnd(); i++) {
            if (i >= l.hotbar() && i < l.base()) continue;
            ItemStack stack = menu.slots.get(i).getItem();
            if (stack.isEmpty()) continue;
            boolean known = false;
            for (int c = 0; c < l.container() && !known; c++)
                known = ItemStack.isSameItemSameComponents(menu.slots.get(c).getItem(), stack);
            if (!known) continue;
            ItemStack before = stack.copy();
            ((MenuAccessor) menu).gearinventory$move(stack, 0, l.container(), false);
            if (stack.isEmpty()) menu.slots.get(i).setByPlayer(ItemStack.EMPTY, before);
            else menu.slots.get(i).setChanged();
        }
    }

    public static void sort(AbstractContainerMenu menu) {
        Layout l = layout(menu);
        List<ItemStack> merged = new ArrayList<>();
        for (int i = 0; i < l.container(); i++) {
            Slot slot = menu.slots.get(i);
            ItemStack stack = slot.getItem().copy();
            for (ItemStack m : merged) {
                if (stack.isEmpty() || !ItemStack.isSameItemSameComponents(m, stack)) continue;
                int n = Math.min(stack.getCount(), Math.max(0, slot.getMaxStackSize(m) - m.getCount()));
                m.grow(n);
                stack.shrink(n);
            }
            if (!stack.isEmpty()) merged.add(stack);
        }
        merged.sort(Comparator.<ItemStack, String>comparing(s -> BuiltInRegistries.ITEM.getKey(s.getItem()).toString())
                .thenComparing(s -> s.getHoverName().getString())
                .thenComparing(ItemStack::getCount, Comparator.reverseOrder()));
        for (int i = 0; i < l.container(); i++) menu.slots.get(i).set(i < merged.size() ? merged.get(i) : ItemStack.EMPTY);
    }
}
