package com.gearinventory.compat.curios;

import net.minecraft.world.Container;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;

/** Looks the handler up on every call: Curios may rebuild it (respawn, data reload). */
public class CurioContainer implements Container {
    private final Player player;
    public final String id;

    public CurioContainer(Player player, String id) {
        this.player = player;
        this.id = id;
    }

    @Override public int getContainerSize() { return 1; }
    @Override public boolean isEmpty() { return getItem(0).isEmpty(); }
    @Override public ItemStack getItem(int i) { return CuriosCompat.get(player, id); }

    @Override
    public ItemStack removeItem(int i, int n) {
        ItemStack now = getItem(0).copy(), taken = now.split(n);
        CuriosCompat.set(player, id, now);
        return taken;
    }

    @Override public ItemStack removeItemNoUpdate(int i) { return removeItem(0, getItem(0).getCount()); }
    @Override public void setItem(int i, ItemStack stack) { CuriosCompat.set(player, id, stack); }
    @Override public void setChanged() {}
    @Override public boolean stillValid(Player p) { return p == player; }
    @Override public void clearContent() { setItem(0, ItemStack.EMPTY); }
}