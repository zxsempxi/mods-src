package com.gearinventory.gametest;

import com.gearinventory.GearInventory;
import com.gearinventory.menu.GearInventoryMenu;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.GameType;
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;

@GameTestHolder(GearInventory.MODID)
@PrefixGameTestTemplate(false)
public class Stage0Tests { // bare player: no storage, so shift-click toggles main/hotbar
    @GameTest(template = "empty")
    public static void slotCount(GameTestHelper h) {
        Player p = h.makeMockPlayer(GameType.SURVIVAL);
        GearInventoryMenu m = new GearInventoryMenu(1, p.getInventory());
        h.assertTrue(m.slots.size() == 49 + 4 * com.gearinventory.storage.StorageSize.MAX_SLOTS, "slots: " + m.slots.size());
        long distinct = m.slots.stream().limit(41).map(s -> s.getContainerSlot()).distinct().count();
        h.assertTrue(distinct == 41, "slot indices overlap");
        h.succeed();
    }

    @GameTest(template = "empty")
    public static void shiftClickBothWays(GameTestHelper h) {
        Player p = h.makeMockPlayer(GameType.SURVIVAL);
        p.getInventory().setItem(0, new ItemStack(Items.BREAD, 20));
        GearInventoryMenu m = new GearInventoryMenu(1, p.getInventory());
        m.quickMoveStack(p, GearInventoryMenu.HOTBAR_START);
        h.assertTrue(p.getInventory().getItem(9).getCount() == 20 && p.getInventory().getItem(0).isEmpty(), "hotbar -> main");
        m.quickMoveStack(p, GearInventoryMenu.MAIN_START);
        h.assertTrue(p.getInventory().getItem(0).getCount() == 20, "main -> hotbar");
        h.assertTrue(p.getInventory().countItem(Items.BREAD) == 20, "dupe or loss");
        h.succeed();
    }

    @GameTest(template = "empty")
    public static void armorShiftClick(GameTestHelper h) {
        Player p = h.makeMockPlayer(GameType.SURVIVAL);
        p.getInventory().setItem(9, new ItemStack(Items.IRON_CHESTPLATE));
        GearInventoryMenu m = new GearInventoryMenu(1, p.getInventory());
        m.quickMoveStack(p, GearInventoryMenu.MAIN_START);
        h.assertTrue(p.getInventory().getArmor(2).is(Items.IRON_CHESTPLATE), "chestplate not equipped");
        m.quickMoveStack(p, GearInventoryMenu.ARMOR_START + 1);
        h.assertTrue(p.getInventory().getArmor(2).isEmpty() && p.getInventory().countItem(Items.IRON_CHESTPLATE) == 1, "unequip");
        h.assertFalse(m.slots.get(0).mayPlace(new ItemStack(Items.IRON_CHESTPLATE)), "chestplate in head slot");
        h.succeed();
    }

    @GameTest(template = "empty")
    public static void fullInventoryKeepsItem(GameTestHelper h) {
        Player p = h.makeMockPlayer(GameType.SURVIVAL);
        for (int i = 0; i < 36; i++) p.getInventory().setItem(i, new ItemStack(Items.DIRT, 64));
        p.getInventory().setItem(39, new ItemStack(Items.IRON_HELMET));
        GearInventoryMenu m = new GearInventoryMenu(1, p.getInventory());
        m.quickMoveStack(p, 0);
        h.assertTrue(p.getInventory().getItem(39).is(Items.IRON_HELMET), "helmet lost");
        h.assertTrue(p.getInventory().countItem(Items.DIRT) == 36 * 64, "dirt changed");
        h.succeed();
    }
}