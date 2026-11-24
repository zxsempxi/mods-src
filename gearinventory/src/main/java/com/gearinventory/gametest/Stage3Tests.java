package com.gearinventory.gametest;

import com.gearinventory.GearInventory;
import com.gearinventory.menu.GearInventoryMenu;
import com.gearinventory.trash.TrashAttachment;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.ClickType;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.GameType;
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;

@GameTestHolder(GearInventory.MODID)
@PrefixGameTestTemplate(false)
public class Stage3Tests {
    private static final int HOT = GearInventoryMenu.HOTBAR_START, TRASH = GearInventoryMenu.TRASH;

    @GameTest(template = "empty")
    public static void trashHoldsAndIsReplaced(GameTestHelper h) {
        Player p = h.makeMockPlayer(GameType.SURVIVAL);
        p.getInventory().setItem(0, new ItemStack(Items.DIRT, 10));
        p.getInventory().setItem(1, new ItemStack(Items.BREAD, 3));
        GearInventoryMenu m = new GearInventoryMenu(1, p.getInventory());
        m.clicked(HOT, 0, ClickType.PICKUP, p);
        m.clicked(TRASH, 0, ClickType.PICKUP, p);
        h.assertTrue(m.getCarried().isEmpty() && p.getData(TrashAttachment.TRASH).getCount() == 10, "not trashed");
        m.clicked(HOT + 1, 0, ClickType.PICKUP, p);
        m.clicked(TRASH, 0, ClickType.PICKUP, p);
        h.assertTrue(m.getCarried().isEmpty() && p.getData(TrashAttachment.TRASH).is(Items.BREAD), "not replaced");
        m.clicked(TRASH, 0, ClickType.PICKUP, p);
        h.assertTrue(m.getCarried().getCount() == 3 && p.getData(TrashAttachment.TRASH).isEmpty(), "cannot take back");
        h.succeed();
    }

    @GameTest(template = "empty")
    public static void trashShiftClickTakesBack(GameTestHelper h) {
        Player p = h.makeMockPlayer(GameType.SURVIVAL);
        p.setData(TrashAttachment.TRASH, new ItemStack(Items.BREAD, 5));
        GearInventoryMenu m = new GearInventoryMenu(1, p.getInventory());
        m.quickMoveStack(p, TRASH);
        h.assertTrue(p.getInventory().countItem(Items.BREAD) == 5 && p.getData(TrashAttachment.TRASH).isEmpty(), "dupe or loss");
        h.succeed();
    }

    @GameTest(template = "empty")
    public static void ctrlClickValidation(GameTestHelper h) {
        Player p = h.makeMockPlayer(GameType.SURVIVAL);
        p.getInventory().setItem(0, new ItemStack(Items.DIRT, 10));
        GearInventoryMenu m = new GearInventoryMenu(1, p.getInventory());
        h.assertFalse(m.trash(p, -1) || m.trash(p, 9999) || m.trash(p, TRASH) || m.trash(p, GearInventoryMenu.RESULT) || m.trash(p, HOT + 5), "bad index accepted");
        h.assertTrue(m.trash(p, HOT) && p.getInventory().getItem(0).isEmpty() && p.getData(TrashAttachment.TRASH).getCount() == 10, "ctrl-click");
        p.setItemSlot(EquipmentSlot.CHEST, new ItemStack(Items.IRON_CHESTPLATE));
        h.assertTrue(m.trash(p, 1) && p.getItemBySlot(EquipmentSlot.CHEST).isEmpty(), "armor");
        h.succeed();
    }

    @GameTest(template = "empty")
    public static void trashSurvivesSave(GameTestHelper h) {
        var reg = h.getLevel().registryAccess();
        Player p = h.makeMockPlayer(GameType.SURVIVAL);
        p.setData(TrashAttachment.TRASH, new ItemStack(Items.BREAD, 5));
        var tag = p.saveWithoutId(new net.minecraft.nbt.CompoundTag());
        Player q = h.makeMockPlayer(GameType.SURVIVAL);
        q.load(tag);
        h.assertTrue(q.getData(TrashAttachment.TRASH).getCount() == 5, "lost");
        h.succeed();
    }

    @GameTest(template = "empty")
    public static void craftingWorksAndReturns(GameTestHelper h) {
        ServerPlayer p = TestPlayers.server(h);
        p.setGameMode(GameType.SURVIVAL);
        p.getInventory().clearContent();
        p.getInventory().setItem(0, new ItemStack(Items.OAK_LOG, 3));
        GearInventoryMenu m = new GearInventoryMenu(1, p.getInventory());
        m.clicked(HOT, 0, ClickType.PICKUP, p);
        m.clicked(GearInventoryMenu.CRAFT_START, 0, ClickType.PICKUP, p);
        h.assertTrue(m.slots.get(GearInventoryMenu.RESULT).getItem().is(Items.OAK_PLANKS), "no recipe result");
        m.quickMoveStack(p, GearInventoryMenu.RESULT);
        h.assertTrue(p.getInventory().countItem(Items.OAK_PLANKS) == 4 && m.slots.get(GearInventoryMenu.CRAFT_START).getItem().getCount() == 2, "craft count");
        m.removed(p);
        h.assertTrue(p.getInventory().countItem(Items.OAK_LOG) == 2, "grid items lost on close");
        p.discard();
        h.succeed();
    }

    @GameTest(template = "empty")
    public static void trashClearedOnDeath(GameTestHelper h) {
        ServerPlayer dead = TestPlayers.server(h);
        dead.setData(TrashAttachment.TRASH, new ItemStack(Items.BREAD, 5));
        ServerPlayer respawned = TestPlayers.server(h);
        respawned.restoreFrom(dead, false);
        boolean cleared = respawned.getData(TrashAttachment.TRASH).isEmpty();
        dead.discard();
        respawned.discard();
        h.assertTrue(cleared, "trash copied on death");
        h.succeed();
    }
}