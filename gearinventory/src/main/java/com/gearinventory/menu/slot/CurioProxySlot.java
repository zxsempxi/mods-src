package com.gearinventory.menu.slot;

import com.gearinventory.compat.curios.CurioContainer;
import com.gearinventory.compat.curios.CuriosCompat;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;

public class CurioProxySlot extends Slot {
    private final Player player;
    private final String id;

    public CurioProxySlot(Player player, String id, int x, int y) {
        super(new CurioContainer(player, id), 0, x, y);
        this.player = player;
        this.id = id;
    }

    @Override public boolean mayPlace(ItemStack stack) { return CuriosCompat.canEquip(player, id, stack); }
    @Override public boolean mayPickup(Player p) { return CuriosCompat.canUnequip(p, id, getItem()) && super.mayPickup(p); }
    @Override public int getMaxStackSize() { return 1; }
}