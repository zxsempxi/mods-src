package com.gearinventory.storage;

import com.gearinventory.data.ArmorStorageDataMap;
import net.minecraft.core.NonNullList;
import net.minecraft.core.component.DataComponents;
import net.minecraft.world.Container;
import net.minecraft.world.ContainerHelper;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.component.ItemContainerContents;

/**
 * Live view of whatever is worn right now. The slot count is constant, so swapping armor while the menu is open
 * cannot shift slot indices; slots past the current size are simply inactive.
 */
public class ArmorStorageContainer implements Container {
    private final Player player;
    public final EquipmentSlot equipment;
    private ItemStack cachedArmor = ItemStack.EMPTY;
    private ItemContainerContents cachedContents;
    private NonNullList<ItemStack> items = NonNullList.withSize(StorageSize.MAX_SLOTS, ItemStack.EMPTY);

    public ArmorStorageContainer(Player player, EquipmentSlot equipment) {
        this.player = player;
        this.equipment = equipment;
    }

    public ItemStack armor() {
        return player.getItemBySlot(equipment);
    }

    public StorageSize size() {
        return ArmorStorageDataMap.size(armor());
    }

    private NonNullList<ItemStack> items() {
        ItemStack armor = armor();
        ItemContainerContents now = armor.get(DataComponents.CONTAINER);
        if (armor != cachedArmor || now != cachedContents) {
            items = ArmorStorage.read(armor, StorageSize.MAX_SLOTS);
            cachedArmor = armor;
            cachedContents = now;
        }
        return items;
    }

    @Override
    public int getContainerSize() {
        return StorageSize.MAX_SLOTS;
    }

    @Override
    public boolean isEmpty() {
        return items().stream().allMatch(ItemStack::isEmpty);
    }

    @Override
    public ItemStack getItem(int i) {
        return i < size().slots() ? items().get(i) : ItemStack.EMPTY;
    }

    @Override
    public ItemStack removeItem(int i, int amount) {
        if (i >= size().slots()) return ItemStack.EMPTY;
        ItemStack taken = ContainerHelper.removeItem(items(), i, amount);
        if (!taken.isEmpty()) setChanged();
        return taken;
    }

    @Override
    public ItemStack removeItemNoUpdate(int i) {
        if (i >= size().slots()) return ItemStack.EMPTY;
        ItemStack taken = ContainerHelper.takeItem(items(), i);
        setChanged();
        return taken;
    }

    @Override
    public void setItem(int i, ItemStack stack) {
        if (i >= size().slots()) return;
        items().set(i, stack);
        setChanged();
    }

    /** Writes the cached list (slots are mutated in place by vanilla menu code) back to the worn piece. */
    @Override
    public void setChanged() {
        ItemStack armor = armor();
        if (armor.isEmpty() || armor != cachedArmor) return;
        ArmorStorage.write(armor, items);
        cachedContents = armor.get(DataComponents.CONTAINER);
    }

    @Override
    public boolean stillValid(Player p) {
        return p == player && p.isAlive();
    }

    @Override
    public void clearContent() {
        items().clear();
        setChanged();
    }
}