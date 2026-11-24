package com.gearinventory.gametest;

import com.gearinventory.GearInventory;
import com.gearinventory.data.ArmorStorageDataMap;
import com.gearinventory.menu.GearInventoryMenu;
import com.gearinventory.storage.ArmorStorage;
import net.minecraft.core.NonNullList;
import net.minecraft.core.component.DataComponents;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.ClickType;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.component.ItemContainerContents;
import net.minecraft.world.level.GameType;
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;

import java.util.List;

@GameTestHolder(GearInventory.MODID)
@PrefixGameTestTemplate(false)
public class Stage1Tests {
    private static final int CHEST0 = GearInventoryMenu.STORAGE_START + com.gearinventory.storage.StorageSize.MAX_SLOTS;

    private static Player wearing(GameTestHelper h, ItemStack chest) {
        Player p = h.makeMockPlayer(GameType.SURVIVAL);
        p.setItemSlot(EquipmentSlot.CHEST, chest);
        return p;
    }

    private static void click(GearInventoryMenu m, Player p, int slot) {
        m.clicked(slot, 0, ClickType.PICKUP, p);
    }

    @GameTest(template = "empty")
    public static void sizesFromPlan(GameTestHelper h) {
        h.assertTrue(ArmorStorageDataMap.size(new ItemStack(Items.LEATHER_CHESTPLATE)).slots() == 24, "leather chest");
        h.assertTrue(ArmorStorageDataMap.size(new ItemStack(Items.IRON_LEGGINGS)).slots() == 18, "iron legs");
        h.assertTrue(ArmorStorageDataMap.size(new ItemStack(Items.NETHERITE_LEGGINGS)).slots() == 24, "netherite legs");
        h.assertTrue(ArmorStorageDataMap.size(new ItemStack(Items.TURTLE_HELMET)).slots() == 2, "turtle");
        h.assertTrue(ArmorStorageDataMap.size(new ItemStack(Items.ELYTRA)).slots() == 0, "elytra");
        h.succeed();
    }

    @GameTest(template = "empty")
    public static void takeOffPutOnKeepsItems(GameTestHelper h) {
        Player p = wearing(h, new ItemStack(Items.IRON_CHESTPLATE));
        p.getInventory().setItem(0, new ItemStack(Items.BREAD, 20));
        GearInventoryMenu m = new GearInventoryMenu(1, p.getInventory());
        click(m, p, GearInventoryMenu.HOTBAR_START);
        click(m, p, CHEST0 + 5);
        h.assertTrue(m.getCarried().isEmpty() && m.slots.get(CHEST0 + 5).getItem().getCount() == 20, "not stored");
        click(m, p, GearInventoryMenu.ARMOR_START + 1);
        h.assertTrue(ArmorStorage.used(m.getCarried()) == 1, "taken off armor lost contents");
        h.assertFalse(m.slots.get(CHEST0 + 5).hasItem(), "slot still shows item: dupe");
        h.assertFalse(m.slots.get(CHEST0).mayPlace(new ItemStack(Items.DIRT)), "slot accepts without armor");
        click(m, p, GearInventoryMenu.ARMOR_START + 1);
        h.assertTrue(m.slots.get(CHEST0 + 5).getItem().getCount() == 20, "lost after re-equip");
        h.succeed();
    }

    @GameTest(template = "empty")
    public static void mergeWritesThrough(GameTestHelper h) {
        Player p = wearing(h, new ItemStack(Items.IRON_CHESTPLATE));
        p.getInventory().setItem(0, new ItemStack(Items.BREAD, 10));
        p.getInventory().setItem(1, new ItemStack(Items.BREAD, 10));
        GearInventoryMenu m = new GearInventoryMenu(1, p.getInventory());
        click(m, p, GearInventoryMenu.HOTBAR_START);
        click(m, p, CHEST0);
        click(m, p, GearInventoryMenu.HOTBAR_START + 1);
        click(m, p, CHEST0);
        h.assertTrue(ArmorStorage.read(p.getItemBySlot(EquipmentSlot.CHEST), 12).get(0).getCount() == 20, "component not updated on merge");
        m.quickMoveStack(p, CHEST0);
        h.assertTrue(p.getInventory().countItem(Items.BREAD) == 20 && ArmorStorage.used(p.getItemBySlot(EquipmentSlot.CHEST)) == 0, "shift-click out");
        h.succeed();
    }

    @GameTest(template = "empty")
    public static void swapWhileOpenDoesNotLeak(GameTestHelper h) {
        ItemStack a = new ItemStack(Items.DIAMOND_CHESTPLATE);
        NonNullList<ItemStack> l = NonNullList.withSize(40, ItemStack.EMPTY);
        l.set(38, new ItemStack(Items.ARROW, 9));
        ArmorStorage.write(a, l);
        Player p = wearing(h, a);
        GearInventoryMenu m = new GearInventoryMenu(1, p.getInventory());
        Slot s = m.slots.get(CHEST0 + 38);
        h.assertTrue(s.getItem().getCount() == 9, "read");
        ItemStack b = new ItemStack(Items.LEATHER_CHESTPLATE);
        p.setItemSlot(EquipmentSlot.CHEST, b);
        s.setChanged();
        h.assertFalse(s.hasItem(), "old item visible in new armor");
        h.assertTrue(ArmorStorage.used(b) == 0 && ArmorStorage.used(a) == 1, "leaked between pieces");
        s.set(new ItemStack(Items.DIRT));
        h.assertTrue(ArmorStorage.used(b) == 0, "out of range write");
        h.succeed();
    }

    @GameTest(template = "empty")
    public static void droppedAndWornByOther(GameTestHelper h) {
        ItemStack a = new ItemStack(Items.IRON_CHESTPLATE);
        ArmorStorage.write(a, List.of(new ItemStack(Items.BREAD, 7)));
        Player second = wearing(h, a.copy());
        GearInventoryMenu m = new GearInventoryMenu(1, second.getInventory());
        h.assertTrue(m.slots.get(CHEST0).getItem().getCount() == 7, "other player sees nothing");
        h.succeed();
    }

    @GameTest(template = "empty")
    public static void survivesSaveAndLoad(GameTestHelper h) {
        ItemStack a = new ItemStack(Items.IRON_CHESTPLATE);
        ArmorStorage.write(a, List.of(ItemStack.EMPTY, new ItemStack(Items.BREAD, 7)));
        var reg = h.getLevel().registryAccess();
        ItemStack back = ItemStack.parse(reg, a.save(reg)).orElseThrow();
        h.assertTrue(ArmorStorage.read(back, 12).get(1).getCount() == 7, "lost in nbt");
        h.succeed();
    }

    @GameTest(template = "empty")
    public static void noNesting(GameTestHelper h) {
        Player p = wearing(h, new ItemStack(Items.IRON_CHESTPLATE));
        GearInventoryMenu m = new GearInventoryMenu(1, p.getInventory());
        ItemStack full = new ItemStack(Items.IRON_CHESTPLATE);
        ArmorStorage.write(full, List.of(new ItemStack(Items.DIRT)));
        ItemStack shulker = new ItemStack(Items.SHULKER_BOX);
        shulker.set(DataComponents.CONTAINER, ItemContainerContents.fromItems(List.of(new ItemStack(Items.DIRT))));
        h.assertFalse(m.slots.get(CHEST0).mayPlace(full), "armor with items");
        h.assertFalse(m.slots.get(CHEST0).mayPlace(shulker), "filled shulker");
        h.assertTrue(m.slots.get(CHEST0).mayPlace(new ItemStack(Items.IRON_CHESTPLATE)), "empty armor should fit");
        h.succeed();
    }

    @GameTest(template = "empty")
    public static void oldBiggerRigOverflow(GameTestHelper h) {
        ItemStack old = new ItemStack(Items.LEATHER_BOOTS);
        NonNullList<ItemStack> nine = NonNullList.withSize(9, ItemStack.EMPTY);
        for (int i = 0; i < 9; i++) if (i != 1) nine.set(i, new ItemStack(Items.BREAD, i + 1));
        ArmorStorage.write(old, nine);
        int before = nine.stream().mapToInt(ItemStack::getCount).sum();
        List<ItemStack> extra = ArmorStorage.takeOverflow(old);
        int after = ArmorStorage.read(old, 54).stream().mapToInt(ItemStack::getCount).sum() + extra.stream().mapToInt(ItemStack::getCount).sum();
        h.assertTrue(before == after && ArmorStorage.used(old) == 4 && extra.size() == 4, "before " + before + " after " + after);
        h.succeed();
    }
}