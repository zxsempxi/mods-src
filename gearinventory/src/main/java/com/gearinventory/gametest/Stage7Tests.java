package com.gearinventory.gametest;

import com.gearinventory.GearInventory;
import com.gearinventory.menu.GearInventoryMenu;
import com.gearinventory.net.LootActionPayload;
import com.gearinventory.net.OpenGearInventoryPayload;
import com.gearinventory.storage.ArmorStorage;
import com.gearinventory.storage.GearGuards;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.component.DataComponents;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.inventory.AnvilMenu;
import net.minecraft.world.inventory.GrindstoneMenu;
import net.minecraft.world.item.ArmorItem;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.component.BundleContents;
import net.minecraft.world.item.crafting.CraftingInput;
import net.minecraft.world.item.crafting.RecipeType;
import net.minecraft.world.level.GameType;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.entity.ShulkerBoxBlockEntity;
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;

import java.util.List;

@GameTestHolder(GearInventory.MODID)
@PrefixGameTestTemplate(false)
public class Stage7Tests {
    private static ItemStack rig(int bread) {
        ItemStack a = new ItemStack(Items.IRON_CHESTPLATE);
        ArmorStorage.write(a, List.of(new ItemStack(Items.BREAD, bread)));
        return a;
    }

    private static int inside(ItemStack a) {
        return ArmorStorage.read(a, 54).stream().mapToInt(ItemStack::getCount).sum();
    }

    @GameTest(template = "empty")
    public static void nestingEverywhere(GameTestHelper h) {
        ItemStack full = rig(3), empty = new ItemStack(Items.IRON_CHESTPLATE);
        var bundle = new BundleContents.Mutable(BundleContents.EMPTY);
        h.assertTrue(bundle.tryInsert(full) == 0 && full.getCount() == 1, "bundle took filled rig");
        h.assertTrue(new BundleContents.Mutable(BundleContents.EMPTY).tryInsert(empty.copy()) == 1, "bundle refuses empty armor");
        ShulkerBoxBlockEntity box = new ShulkerBoxBlockEntity(null, BlockPos.ZERO, Blocks.SHULKER_BOX.defaultBlockState());
        h.assertFalse(box.canPlaceItemThroughFace(0, full, Direction.UP), "hopper into shulker");
        h.assertTrue(box.canPlaceItemThroughFace(0, empty, Direction.UP), "hopper refuses empty armor");
        h.assertFalse(ArmorStorage.mayStore(full) || full.canFitInsideContainerItems(), "own storage");
        h.assertFalse(new ItemStack(Items.SHULKER_BOX).canFitInsideContainerItems(), "vanilla rule lost");
        h.succeed();
    }

    @GameTest(template = "empty")
    public static void rightClickSwapKeepsBoth(GameTestHelper h) {
        ServerPlayer p = TestPlayers.server(h);
        p.setItemSlot(EquipmentSlot.CHEST, rig(5));
        p.setItemInHand(InteractionHand.MAIN_HAND, rig(9));
        ItemStack hand = p.getMainHandItem();
        var r = hand.use(h.getLevel(), p, InteractionHand.MAIN_HAND);
        p.setItemInHand(InteractionHand.MAIN_HAND, r.getObject());
        h.assertTrue(inside(p.getItemBySlot(EquipmentSlot.CHEST)) == 9 && inside(p.getMainHandItem()) == 5,
                "worn " + inside(p.getItemBySlot(EquipmentSlot.CHEST)) + " hand " + inside(p.getMainHandItem()));
        h.succeed();
    }

    @GameTest(template = "empty")
    public static void swapWhileMenuOpen(GameTestHelper h) {
        ServerPlayer p = TestPlayers.server(h);
        p.setItemSlot(EquipmentSlot.CHEST, rig(5));
        GearInventoryMenu m = new GearInventoryMenu(1, p.getInventory());
        int first = GearInventoryMenu.STORAGE_START + 54;
        h.assertTrue(m.slots.get(first).getItem().getCount() == 5, "start");
        ItemStack old = p.getItemBySlot(EquipmentSlot.CHEST);
        p.setItemSlot(EquipmentSlot.CHEST, rig(9));
        m.quickMoveStack(p, first);
        h.assertTrue(p.getInventory().countItem(Items.BREAD) == 9 && inside(old) == 5 && inside(p.getItemBySlot(EquipmentSlot.CHEST)) == 0, "leak on swap");
        h.succeed();
    }

    @GameTest(template = "empty")
    public static void dispenserEquips(GameTestHelper h) {
        ServerPlayer p = TestPlayers.server(h);
        ItemStack a = rig(6);
        p.setItemSlot(p.getEquipmentSlotForItem(a), a.split(1));
        h.assertTrue(a.isEmpty() && inside(p.getItemBySlot(EquipmentSlot.CHEST)) == 6, "dispense path lost contents");
        h.assertTrue(a.getItem() instanceof ArmorItem || a.isEmpty(), "sanity");
        h.succeed();
    }

    @GameTest(template = "empty")
    public static void breakingSpillsOnce(GameTestHelper h) {
        ServerPlayer p = TestPlayers.server(h);
        ItemStack a = rig(7);
        a.setDamageValue(a.getMaxDamage() - 1);
        p.setItemSlot(EquipmentSlot.CHEST, a);
        GearInventoryMenu m = new GearInventoryMenu(1, p.getInventory());
        p.getItemBySlot(EquipmentSlot.CHEST).hurtAndBreak(5, h.getLevel(), p, i -> {});
        h.assertTrue(p.getItemBySlot(EquipmentSlot.CHEST).isEmpty(), "did not break");
        int dropped = h.getLevel().getEntitiesOfClass(ItemEntity.class, p.getBoundingBox().inflate(2), e -> e.getItem().is(Items.BREAD))
                .stream().mapToInt(e -> e.getItem().getCount()).sum();
        h.assertTrue(dropped == 7, "dropped " + dropped);
        h.assertFalse(m.slots.get(GearInventoryMenu.STORAGE_START + 54).hasItem(), "still in menu: dupe");
        h.getLevel().getEntitiesOfClass(ItemEntity.class, p.getBoundingBox().inflate(2)).forEach(ItemEntity::discard);
        h.succeed();
    }

    @GameTest(template = "empty")
    public static void mergingNeverEatsContents(GameTestHelper h) {
        ServerPlayer p = TestPlayers.server(h);
        ItemStack a = rig(2), b = new ItemStack(Items.IRON_CHESTPLATE);
        a.setDamageValue(50);
        b.setDamageValue(50);
        var recipes = h.getLevel().getRecipeManager();
        h.assertTrue(recipes.getRecipeFor(RecipeType.CRAFTING, CraftingInput.of(2, 1, List.of(a, b)), h.getLevel()).isEmpty(), "grid repair");
        ItemStack c = b.copy();
        h.assertTrue(recipes.getRecipeFor(RecipeType.CRAFTING, CraftingInput.of(2, 1, List.of(b, c)), h.getLevel()).isPresent(), "empty armor no longer repairs");
        AnvilMenu anvil = new AnvilMenu(1, p.getInventory());
        anvil.getSlot(0).set(b.copy());
        anvil.getSlot(1).set(a.copy());
        anvil.createResult();
        h.assertTrue(anvil.getSlot(2).getItem().isEmpty(), "anvil merged a filled rig");
        p.experienceLevel = 50;
        anvil.getSlot(0).set(a.copy());
        anvil.getSlot(1).set(b.copy());
        anvil.createResult();
        h.assertTrue(inside(anvil.getSlot(2).getItem()) == 2, "anvil lost contents of the repaired rig");
        GrindstoneMenu g = new GrindstoneMenu(1, p.getInventory());
        g.getSlot(0).set(a.copy());
        g.getSlot(1).set(b.copy());
        h.assertTrue(g.getSlot(2).getItem().isEmpty(), "grindstone merged");
        h.succeed();
    }

    @GameTest(template = "empty")
    public static void serverRejectsStalePackets(GameTestHelper h) {
        ServerPlayer p = TestPlayers.server(h);
        p.containerMenu = p.inventoryMenu;
        for (int id = -1; id < 3; id++)
            for (int a = -1; a < 5; a++) h.assertFalse(LootActionPayload.handle(p, new LootActionPayload(id, a)), "accepted without a menu");
        p.setGameMode(GameType.CREATIVE);
        h.assertFalse(OpenGearInventoryPayload.open(p), "opens in creative");
        p.setGameMode(GameType.SPECTATOR);
        h.assertFalse(OpenGearInventoryPayload.open(p), "opens for spectator");
        h.succeed();
    }

    @GameTest(template = "empty")
    public static void unrelatedContainersUntouched(GameTestHelper h) {
        ItemStack box = new ItemStack(Items.SHULKER_BOX);
        box.set(DataComponents.CONTAINER, net.minecraft.world.item.component.ItemContainerContents.fromItems(List.of(new ItemStack(Items.DIRT))));
        h.assertFalse(GearGuards.filled(box) || GearGuards.filled(new ItemStack(Items.IRON_CHESTPLATE)) || GearGuards.filled(ItemStack.EMPTY), "wrong match");
        h.succeed();
    }
}
