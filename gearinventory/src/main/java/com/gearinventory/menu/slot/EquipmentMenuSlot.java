package com.gearinventory.menu.slot;

import com.gearinventory.compat.curios.CuriosCompat;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.enchantment.EnchantmentEffectComponents;
import net.minecraft.world.item.enchantment.EnchantmentHelper;

/** Same rules as the vanilla armor/offhand slot (vanilla ArmorSlot is package-private). */
public class EquipmentMenuSlot extends Slot {
    private final Player owner;
    private final EquipmentSlot equipment;

    public EquipmentMenuSlot(Inventory inv, EquipmentSlot equipment, int index, int x, int y) {
        super(inv, index, x, y);
        this.owner = inv.player;
        this.equipment = equipment;
    }

    @Override
    public void setByPlayer(ItemStack newStack, ItemStack oldStack) {
        owner.onEquipItem(equipment, oldStack, newStack);
        super.setByPlayer(newStack, oldStack);
    }

    @Override
    public int getMaxStackSize() {
        return equipment == EquipmentSlot.OFFHAND ? super.getMaxStackSize() : 1;
    }

    @Override
    public boolean mayPlace(ItemStack stack) {
        // the chest is the rig: elytra and backpacks have their own slots
        if (equipment == EquipmentSlot.CHEST && (stack.is(CuriosCompat.ELYTRA_LIKE) || stack.is(CuriosCompat.BACK_ITEMS))) return false;
        return equipment == EquipmentSlot.OFFHAND || stack.canEquip(equipment, owner);
    }

    @Override
    public boolean mayPickup(Player player) {
        ItemStack stack = getItem();
        boolean bound = equipment != EquipmentSlot.OFFHAND && !stack.isEmpty() && !player.isCreative()
                && EnchantmentHelper.has(stack, EnchantmentEffectComponents.PREVENT_ARMOR_CHANGE);
        return !bound && super.mayPickup(player);
    }
}