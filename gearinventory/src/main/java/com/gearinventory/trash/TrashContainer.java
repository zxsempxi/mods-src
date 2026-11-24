package com.gearinventory.trash;

import net.minecraft.world.Container;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;

public class TrashContainer implements Container {
    private final Player player;

    public TrashContainer(Player player) {
        this.player = player;
    }

    @Override public int getContainerSize() { return 1; }
    @Override public boolean isEmpty() { return getItem(0).isEmpty(); }
    @Override public ItemStack getItem(int i) { return player.getData(TrashAttachment.TRASH); }

    @Override
    public ItemStack removeItem(int i, int n) {
        ItemStack taken = getItem(0).split(n);
        setChanged();
        return taken;
    }

    @Override
    public ItemStack removeItemNoUpdate(int i) {
        return removeItem(0, getItem(0).getCount());
    }

    @Override public void setItem(int i, ItemStack stack) { player.setData(TrashAttachment.TRASH, stack); }
    @Override public void setChanged() {}
    @Override public boolean stillValid(Player p) { return p == player; }
    @Override public void clearContent() { setItem(0, ItemStack.EMPTY); }
}