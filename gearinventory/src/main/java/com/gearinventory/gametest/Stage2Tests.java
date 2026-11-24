package com.gearinventory.gametest;

import com.gearinventory.GearInventory;
import com.gearinventory.menu.GearInventoryMenu;
import com.gearinventory.storage.ArmorStorage;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.GameType;
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;

@GameTestHolder(GearInventory.MODID)
@PrefixGameTestTemplate(false)
public class Stage2Tests {
    private static int total(GearInventoryMenu m, Item item) {
        return m.slots.stream().map(Slot::getItem).filter(s -> s.is(item)).mapToInt(ItemStack::getCount).sum();
    }

    @GameTest(template = "empty")
    public static void shiftClickIntoStorageFirst(GameTestHelper h) {
        Player p = h.makeMockPlayer(GameType.SURVIVAL);
        p.setItemSlot(EquipmentSlot.CHEST, new ItemStack(Items.LEATHER_CHESTPLATE));
        p.getInventory().setItem(0, new ItemStack(Items.BREAD, 20));
        GearInventoryMenu m = new GearInventoryMenu(1, p.getInventory());
        m.quickMoveStack(p, GearInventoryMenu.HOTBAR_START);
        h.assertTrue(ArmorStorage.used(p.getItemBySlot(EquipmentSlot.CHEST)) == 1 && total(m, Items.BREAD) == 20, "not in storage");
        h.succeed();
    }

    @GameTest(template = "empty")
    public static void fullStorageFallsThrough(GameTestHelper h) {
        Player p = h.makeMockPlayer(GameType.SURVIVAL);
        p.setItemSlot(EquipmentSlot.FEET, new ItemStack(Items.LEATHER_BOOTS));
        for (int i = 0; i < 9; i++) p.getInventory().setItem(i, new ItemStack(Items.IRON_SWORD));
        GearInventoryMenu m = new GearInventoryMenu(1, p.getInventory());
        for (int i = 0; i < 9; i++) m.quickMoveStack(p, GearInventoryMenu.HOTBAR_START + i);
        h.assertTrue(total(m, Items.IRON_SWORD) == 9, "swords " + total(m, Items.IRON_SWORD));
        h.assertTrue(ArmorStorage.used(p.getItemBySlot(EquipmentSlot.FEET)) == 4, "storage should hold 4");
        h.succeed();
    }

    /** Thousands of random shift-clicks must not create or destroy anything. */
    @GameTest(template = "empty", timeoutTicks = 400)
    public static void randomShiftClicksConserveItems(GameTestHelper h) {
        Player p = h.makeMockPlayer(GameType.SURVIVAL);
        p.setItemSlot(EquipmentSlot.HEAD, new ItemStack(Items.IRON_HELMET));
        p.setItemSlot(EquipmentSlot.CHEST, new ItemStack(Items.DIAMOND_CHESTPLATE));
        p.setItemSlot(EquipmentSlot.LEGS, new ItemStack(Items.IRON_LEGGINGS));
        p.setItemSlot(EquipmentSlot.FEET, new ItemStack(Items.LEATHER_BOOTS));
        for (int i = 0; i < 30; i++) p.getInventory().setItem(i, new ItemStack(i % 2 == 0 ? Items.BREAD : Items.ARROW, 1 + i));
        GearInventoryMenu m = new GearInventoryMenu(1, p.getInventory());
        int bread = total(m, Items.BREAD), arrows = total(m, Items.ARROW);
        var rnd = net.minecraft.util.RandomSource.create(42);
        for (int n = 0; n < 5000; n++) {
            int i = GearInventoryMenu.MAIN_START + rnd.nextInt(m.slots.size() - GearInventoryMenu.MAIN_START);
            m.quickMoveStack(p, i);
            h.assertTrue(total(m, Items.BREAD) == bread && total(m, Items.ARROW) == arrows, "changed at step " + n);
        }
        h.succeed();
    }
}