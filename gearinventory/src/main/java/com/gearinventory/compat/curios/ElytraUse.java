package com.gearinventory.compat.curios;

import net.minecraft.sounds.SoundEvents;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.neoforged.neoforge.event.entity.player.PlayerInteractEvent;

/** Right-click with an elytra fills our slot; vanilla would put it on the chest, which is the rig. */
public final class ElytraUse {
    private ElytraUse() {}

    public static void onUse(PlayerInteractEvent.RightClickItem event) {
        Player p = event.getEntity();
        ItemStack hand = event.getItemStack();
        if (!hand.is(CuriosCompat.ELYTRA_LIKE)) return;
        if (CuriosCompat.get(p, CuriosCompat.ELYTRA).isEmpty() && CuriosCompat.canEquip(p, CuriosCompat.ELYTRA, hand)) {
            if (!p.level().isClientSide) {
                CuriosCompat.set(p, CuriosCompat.ELYTRA, hand.split(1));
                p.playSound(SoundEvents.ARMOR_EQUIP_ELYTRA.value(), 1, 1);
            }
            event.setCancellationResult(InteractionResult.sidedSuccess(p.level().isClientSide));
        } else event.setCancellationResult(InteractionResult.FAIL);
        event.setCanceled(true);
    }
}