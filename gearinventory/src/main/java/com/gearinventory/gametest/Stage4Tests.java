package com.gearinventory.gametest;

import com.gearinventory.GearInventory;
import com.gearinventory.compat.curios.CuriosCompat;
import com.gearinventory.menu.GearInventoryMenu;
import com.gearinventory.storage.ArmorStorage;
import com.gearinventory.storage.PickupOverflow;
import com.gearinventory.trash.TrashAttachment;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.inventory.ClickType;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.GameType;
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;

import java.util.List;

@GameTestHolder(GearInventory.MODID)
@PrefixGameTestTemplate(false)
public class Stage4Tests {
    private static ServerPlayer player(GameTestHelper h) {
        ServerPlayer p = TestPlayers.server(h);
        p.setGameMode(GameType.SURVIVAL);
        p.getInventory().clearContent();
        return p;
    }

    @GameTest(template = "empty")
    public static void filledArmorCannotBeTrashed(GameTestHelper h) {
        ServerPlayer p = player(h);
        ItemStack a = new ItemStack(Items.IRON_CHESTPLATE);
        ArmorStorage.write(a, List.of(new ItemStack(Items.BREAD)));
        p.setItemSlot(EquipmentSlot.CHEST, a);
        GearInventoryMenu m = new GearInventoryMenu(1, p.getInventory());
        h.assertFalse(m.trash(p, 1), "ctrl-click");
        m.setCarried(a.copy());
        m.clicked(GearInventoryMenu.TRASH, 0, ClickType.PICKUP, p);
        h.assertTrue(!m.getCarried().isEmpty() && p.getData(TrashAttachment.TRASH).isEmpty(), "plain click");
        m.setCarried(ItemStack.EMPTY);
        p.discard();
        h.succeed();
    }

    @GameTest(template = "empty")
    public static void elytraSlotRules(GameTestHelper h) {
        ServerPlayer p = player(h);
        GearInventoryMenu m = new GearInventoryMenu(1, p.getInventory());
        h.assertTrue(CuriosCompat.handler(p, "elytra") != null && CuriosCompat.handler(p, "back") != null, "slots missing");
        h.assertTrue(m.slots.get(GearInventoryMenu.ELYTRA).mayPlace(new ItemStack(Items.ELYTRA)), "elytra rejected");
        h.assertFalse(m.slots.get(GearInventoryMenu.ELYTRA).mayPlace(new ItemStack(Items.IRON_CHESTPLATE)), "chestplate accepted");
        h.assertFalse(m.slots.get(GearInventoryMenu.ARMOR_START + 1).mayPlace(new ItemStack(Items.ELYTRA)), "elytra in rig slot");
        p.getInventory().setItem(0, new ItemStack(Items.ELYTRA));
        m.quickMoveStack(p, GearInventoryMenu.HOTBAR_START);
        h.assertTrue(CuriosCompat.get(p, "elytra").is(Items.ELYTRA) && p.getInventory().countItem(Items.ELYTRA) == 0, "shift-click in");
        m.quickMoveStack(p, GearInventoryMenu.ELYTRA);
        h.assertTrue(CuriosCompat.get(p, "elytra").isEmpty() && p.getInventory().countItem(Items.ELYTRA) == 1, "shift-click out");
        p.discard();
        h.succeed();
    }

    @GameTest(template = "empty", timeoutTicks = 200)
    public static void flightAndWearFromSlot(GameTestHelper h) {
        ServerPlayer p = player(h);
        p.setItemSlot(EquipmentSlot.CHEST, new ItemStack(Items.IRON_CHESTPLATE));
        p.setOnGround(false);
        h.assertFalse(p.tryToStartFallFlying(), "flies without elytra");
        CuriosCompat.set(p, "elytra", new ItemStack(Items.ELYTRA));
        h.assertTrue(p.tryToStartFallFlying(), "does not start");
        try {
            var tick = net.minecraft.world.entity.LivingEntity.class.getDeclaredMethod("updateFallFlying");
            var ticks = net.minecraft.world.entity.LivingEntity.class.getDeclaredField("fallFlyTicks");
            tick.setAccessible(true);
            ticks.setAccessible(true);
            ticks.setInt(p, 19);
            p.setOnGround(false);
            tick.invoke(p);
        } catch (ReflectiveOperationException e) {
            throw new IllegalStateException(e);
        }
        h.assertTrue(p.isFallFlying(), "hooked tick stopped the flight");
        h.assertTrue(CuriosCompat.get(p, "elytra").getDamageValue() == 1, "no wear");
        h.assertTrue(p.getItemBySlot(EquipmentSlot.CHEST).getDamageValue() == 0, "rig worn instead");
        p.discard();
        h.succeed();
    }
    @GameTest(template = "empty")
    public static void brokenElytraDoesNotFly(GameTestHelper h) {
        ServerPlayer p = player(h);
        ItemStack e = new ItemStack(Items.ELYTRA);
        e.setDamageValue(e.getMaxDamage() - 1);
        CuriosCompat.set(p, "elytra", e);
        p.setOnGround(false);
        h.assertFalse(p.tryToStartFallFlying(), "broken elytra flies");
        p.discard();
        h.succeed();
    }

    @GameTest(template = "empty")
    public static void pickupOverflow(GameTestHelper h) {
        ServerPlayer p = player(h);
        for (int i = 0; i < 36; i++) p.getInventory().setItem(i, new ItemStack(Items.DIRT, 64));
        p.getInventory().setItem(0, new ItemStack(Items.BREAD, 60));
        p.setItemSlot(EquipmentSlot.FEET, new ItemStack(Items.LEATHER_BOOTS));
        ItemEntity e = new ItemEntity(h.getLevel(), p.getX(), p.getY(), p.getZ(), new ItemStack(Items.BREAD, 20));
        e.setNoPickUpDelay();
        h.getLevel().addFreshEntity(e);
        e.playerTouch(p);
        int stored = ArmorStorage.read(p.getItemBySlot(EquipmentSlot.FEET), 4).stream().mapToInt(ItemStack::getCount).sum();
        h.assertTrue(p.getInventory().countItem(Items.BREAD) == 64 && stored == 16, "inv " + p.getInventory().countItem(Items.BREAD) + " stored " + stored);
        h.assertTrue(e.isRemoved() || e.getItem().isEmpty(), "entity left: dupe");
        p.discard();
        h.succeed();
    }

    @GameTest(template = "empty")
    public static void pickupRespectsDelayAndFullStorage(GameTestHelper h) {
        ServerPlayer p = player(h);
        for (int i = 0; i < 36; i++) p.getInventory().setItem(i, new ItemStack(Items.DIRT, 64));
        p.setItemSlot(EquipmentSlot.FEET, new ItemStack(Items.LEATHER_BOOTS));
        ItemEntity e = new ItemEntity(h.getLevel(), p.getX(), p.getY(), p.getZ(), new ItemStack(Items.IRON_SWORD));
        e.setPickUpDelay(40);
        e.playerTouch(p);
        h.assertTrue(e.getItem().getCount() == 1, "ignored pickup delay");
        h.assertTrue(PickupOverflow.insert(p, new ItemStack(Items.BREAD, 64)).isEmpty(), "insert");
        for (int i = 0; i < 3; i++) PickupOverflow.insert(p, new ItemStack(Items.BREAD, 64));
        h.assertTrue(PickupOverflow.insert(p, new ItemStack(Items.BREAD, 10)).getCount() == 10, "overfilled");
        p.discard();
        h.succeed();
    }

    /** Random partial pickups: inventory + storage + ground must stay constant. */
    @GameTest(template = "empty")
    public static void randomPickupsConserveItems(GameTestHelper h) {
        var rnd = net.minecraft.util.RandomSource.create(7);
        for (int n = 0; n < 300; n++) {
            ServerPlayer p = player(h);
            for (int i = 0; i < 36; i++) p.getInventory().setItem(i, new ItemStack(Items.DIRT, 64));
            int free = rnd.nextInt(3);
            for (int i = 0; i < free; i++) p.getInventory().setItem(i, new ItemStack(Items.BREAD, 1 + rnd.nextInt(64)));
            if (rnd.nextBoolean()) p.setItemSlot(EquipmentSlot.FEET, new ItemStack(Items.LEATHER_BOOTS));
            for (int i = rnd.nextInt(5); i > 0; i--) PickupOverflow.insert(p, new ItemStack(Items.BREAD, 1 + rnd.nextInt(64)));
            int before = p.getInventory().countItem(Items.BREAD) + stored(p), amount = 1 + rnd.nextInt(64);
            ItemEntity e = new ItemEntity(h.getLevel(), p.getX(), p.getY(), p.getZ(), new ItemStack(Items.BREAD, amount));
            e.setNoPickUpDelay();
            e.playerTouch(p);
            int after = p.getInventory().countItem(Items.BREAD) + stored(p) + (e.isRemoved() ? 0 : e.getItem().getCount());
            h.assertTrue(before + amount == after, "case " + n + ": " + (before + amount) + " -> " + after);
        }
        h.succeed();
    }

    private static int stored(ServerPlayer p) {
        return ArmorStorage.read(p.getItemBySlot(EquipmentSlot.FEET), 54).stream().mapToInt(ItemStack::getCount).sum();
    }
}