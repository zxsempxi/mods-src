package com.claude.armorrigs;

import net.minecraft.core.component.DataComponents;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.component.ItemContainerContents;
import net.minecraft.world.level.GameType;
import net.neoforged.bus.api.EventPriority;
import net.neoforged.neoforge.common.NeoForge;
import net.neoforged.neoforge.event.entity.living.LivingDropsEvent;
import net.neoforged.neoforge.event.tick.PlayerTickEvent;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/** Only active with -Darmorrigs.selftest=true. Destructive for the player: run in a throwaway world. */
final class SelfTest {
    private static final Logger LOG = LoggerFactory.getLogger("armorrigs-selftest");
    private static int ticks;

    static void register() {
        NeoForge.EVENT_BUS.addListener(SelfTest::tick);
        NeoForge.EVENT_BUS.addListener(EventPriority.HIGHEST, SelfTest::drops);
    }

    private static void check(String name, boolean ok, String info) {
        LOG.info("{} {} [{}]", ok ? "PASS" : "FAIL", name, info);
    }

    private static int count(ItemStack armor) {
        return armor.getOrDefault(DataComponents.CONTAINER, ItemContainerContents.EMPTY).stream().mapToInt(ItemStack::getCount).sum();
    }

    private static void tick(PlayerTickEvent.Post e) {
        if (!(e.getEntity() instanceof ServerPlayer sp)) return;
        ticks++;
        try {
            if (ticks == 200) run(sp);
            if (ticks == 6000 && Boolean.getBoolean("armorrigs.selftest.death")) {
                sp.setGameMode(GameType.SURVIVAL);
                sp.removeAllEffects();
                sp.hurt(sp.damageSources().genericKill(), Float.MAX_VALUE);
                check("player died", sp.isDeadOrDying(), "");
            }
        } catch (Throwable t) {
            LOG.error("FAIL exception", t);
        }
    }

    private static void run(ServerPlayer sp) {
        sp.setGameMode(GameType.SURVIVAL);
        Inventory inv = sp.getInventory();
        inv.clearContent();
        int open = 0;
        for (Slot s : sp.inventoryMenu.slots) {
            if (s.container == inv && s.getContainerSlot() < 36 && s.mayPlace(new ItemStack(Items.STONE))) open++;
        }
        check("open slots = 18", open == 18, "open=" + open);
        int accepted = 0;
        for (int i = 0; i < 40; i++) if (inv.add(new ItemStack(Items.IRON_SWORD))) accepted++;
        boolean inLocked = false;
        for (int i = 9; i < 36; i++) if (i % 9 >= 3 && !inv.items.get(i).isEmpty()) inLocked = true;
        check("pickup fills exactly 18", accepted == 18 && !inLocked, "accepted=" + accepted + " inLocked=" + inLocked);
        inv.clearContent();
        inv.items.set(17, new ItemStack(Items.DIRT, 5));
        Slot lockedSlot = sp.inventoryMenu.slots.stream().filter(s -> s.container == inv && s.getContainerSlot() == 17).findFirst().orElseThrow();
        check("item in locked slot can be taken", lockedSlot.mayPickup(sp), "");
        inv.add(new ItemStack(Items.DIRT, 10));
        check("pickup does not grow locked stack", inv.items.get(17).getCount() == 5, "count=" + inv.items.get(17).getCount());

        inv.clearContent();
        ItemStack chest = new ItemStack(Items.LEATHER_CHESTPLATE);
        sp.setItemSlot(EquipmentSlot.CHEST, chest);
        inv.items.set(0, new ItemStack(Items.BREAD, 32));
        RigMenu menu = new RigMenu(99, inv);
        int bread = -1;
        for (int i = 0; i < menu.slots.size(); i++) if (menu.slots.get(i).getItem().is(Items.BREAD)) bread = i;
        menu.quickMoveStack(sp, bread);
        check("shift-click into rig", count(chest) == 32 && inv.items.get(0).isEmpty(), "rig=" + count(chest) + " inv=" + inv.items.get(0).getCount());
        check("reopen keeps items", new RigMenu(100, inv).slots.get(0).getItem().getCount() == 32, "");
        RigMenu back = new RigMenu(101, inv);
        back.quickMoveStack(sp, 0);
        int total = count(chest) + inv.countItem(Items.BREAD);
        check("shift-click back, no dupe or loss", count(chest) == 0 && total == 32, "rig=" + count(chest) + " total=" + total);
        back.quickMoveStack(sp, 0);
        ItemStack nested = new ItemStack(Items.SHULKER_BOX);
        nested.set(DataComponents.CONTAINER, ItemContainerContents.fromItems(java.util.List.of(new ItemStack(Items.DIRT))));
        check("no nested filled container", !back.slots.get(0).mayPlace(nested), "");
        RigMenu open2 = new RigMenu(102, inv);
        sp.setItemSlot(EquipmentSlot.CHEST, ItemStack.EMPTY);
        check("menu invalid after armor removed", !open2.stillValid(sp), "");
        chest.set(DataComponents.CONTAINER, ItemContainerContents.fromItems(java.util.List.of(new ItemStack(Items.BREAD, 7))));
        sp.setItemSlot(EquipmentSlot.CHEST, chest);
        check("contents survive take off / put on", new RigMenu(103, inv).slots.get(0).getItem().getCount() == 7, "");
        LOG.info("DONE");
    }

    private static void drops(LivingDropsEvent e) {
        if (!(e.getEntity() instanceof ServerPlayer)) return;
        int n = 0;
        for (ItemEntity d : e.getDrops()) if (d.getItem().is(Items.LEATHER_CHESTPLATE)) n += count(d.getItem());
        check("death drop keeps rig contents", n == 7, "bread=" + n);
    }

    private SelfTest() {}
}