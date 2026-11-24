package com.gearinventory.gametest;

import com.gearinventory.GearInventory;
import com.gearinventory.compat.tacz.AmmoLookup;
import com.gearinventory.storage.ArmorStorage;
import com.tacz.guns.api.item.IAmmoBox;
import com.tacz.guns.api.item.builder.AmmoItemBuilder;
import com.tacz.guns.api.item.builder.GunItemBuilder;
import com.tacz.guns.api.item.gun.AbstractGunItem;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.neoforged.fml.ModList;
import net.neoforged.neoforge.capabilities.Capabilities;
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;
import net.neoforged.neoforge.items.IItemHandler;

import java.util.List;

/** Run with -PwithTacz; without TaCZ the gun part is skipped. */
@GameTestHolder(GearInventory.MODID)
@PrefixGameTestTemplate(false)
public class Stage8Tests {
    private static final ResourceLocation AK = ResourceLocation.fromNamespaceAndPath("tacz", "ak47");
    private static final ResourceLocation AMMO = ResourceLocation.fromNamespaceAndPath("tacz", "762x39");

    @GameTest(template = "empty")
    public static void handlerOrderAndCounts(GameTestHelper h) {
        ServerPlayer p = TestPlayers.server(h);
        ItemStack rig = new ItemStack(Items.LEATHER_CHESTPLATE);
        ArmorStorage.write(rig, List.of(new ItemStack(Items.ARROW, 30)));
        p.setItemSlot(EquipmentSlot.CHEST, rig);
        IItemHandler base = p.getCapability(Capabilities.ItemHandler.ENTITY, null);
        IItemHandler w = (IItemHandler) AmmoLookup.wrap(p, base);
        h.assertTrue(w.getSlots() == base.getSlots() + AmmoLookup.EXTRA, "slots");
        h.assertTrue(p.getCapability(Capabilities.ItemHandler.ENTITY, null).getSlots() == base.getSlots(), "leaks to other mods");
        int at = base.getSlots();
        h.assertTrue(w.extractItem(at, 10, true).getCount() == 10 && ArmorStorage.read(rig, 24).get(0).getCount() == 30, "simulate changed items");
        h.assertTrue(w.extractItem(at, 10, false).getCount() == 10 && ArmorStorage.read(rig, 24).get(0).getCount() == 20, "extract");
        h.assertTrue(w.extractItem(at, 64, false).getCount() == 20 && ArmorStorage.used(rig) == 0, "over-extract");
        p.setItemSlot(EquipmentSlot.CHEST, ItemStack.EMPTY);
        h.assertTrue(w.getStackInSlot(at).isEmpty() && w.extractItem(at, 5, false).isEmpty(), "works without armor");
        h.succeed();
    }

    /** In singleplayer the client and server player have one entity id, and Entity.equals compares only that. */
    @GameTest(template = "empty")
    public static void sameIdPlayers(GameTestHelper h) {
        ServerPlayer a = TestPlayers.server(h), b = TestPlayers.server(h);
        b.setId(a.getId());
        for (ServerPlayer p : List.of(a, b)) {
            ItemStack rig = new ItemStack(Items.LEATHER_CHESTPLATE);
            ArmorStorage.write(rig, List.of(new ItemStack(Items.ARROW, 30)));
            p.setItemSlot(EquipmentSlot.CHEST, rig);
        }
        AmmoLookup.extra(a, 0);
        IItemHandler w = (IItemHandler) AmmoLookup.wrap(b, b.getCapability(Capabilities.ItemHandler.ENTITY, null));
        w.extractItem(w.getSlots() - AmmoLookup.EXTRA, 10, false);
        h.assertTrue(ArmorStorage.read(b.getItemBySlot(EquipmentSlot.CHEST), 24).get(0).getCount() == 20, "ammo not taken from the right player");
        h.assertTrue(ArmorStorage.read(a.getItemBySlot(EquipmentSlot.CHEST), 24).get(0).getCount() == 30, "ammo taken from the other player");
        h.succeed();
    }

    @GameTest(template = "empty", timeoutTicks = 200)
    public static void gunUsesPockets(GameTestHelper h) {
        if (ModList.get().isLoaded("tacz")) Guns.run(h);
        else h.succeed();
    }

    /** Separate class so TaCZ types load only when the mod is present. */
    private static final class Guns {
        static void run(GameTestHelper h) {
            ServerPlayer p = TestPlayers.server(h);
            ItemStack gun = GunItemBuilder.create().setId(AK).setAmmoCount(0).build();
            AbstractGunItem item = (AbstractGunItem) gun.getItem();
            p.getInventory().setItem(0, gun);
            h.assertFalse(item.canReload(p, gun), "reloads with no ammo at all");
            ItemStack rig = new ItemStack(Items.IRON_CHESTPLATE);
            ArmorStorage.write(rig, List.of(AmmoItemBuilder.create().setId(AMMO).setCount(40).build()));
            p.setItemSlot(EquipmentSlot.CHEST, rig);
            h.assertTrue(item.canReload(p, gun), "pocket ammo not seen");
            IItemHandler w = (IItemHandler) AmmoLookup.wrap(p, p.getCapability(Capabilities.ItemHandler.ENTITY, null));
            int got = item.findAndExtractInventoryAmmo(w, gun, 30);
            int left = ArmorStorage.read(rig, 35).stream().mapToInt(ItemStack::getCount).sum();
            h.assertTrue(got == 30 && left == 10, "got " + got + " left " + left);

            ItemStack box = new ItemStack(BuiltInRegistries.ITEM.get(ResourceLocation.fromNamespaceAndPath("tacz", "ammo_box")));
            IAmmoBox api = (IAmmoBox) box.getItem();
            api.setAmmoId(box, AMMO);
            api.setAmmoCount(box, 50);
            ArmorStorage.write(rig, List.of(box));
            got = item.findAndExtractInventoryAmmo(w, gun, 30);
            ItemStack stored = ArmorStorage.read(rig, 35).get(0);
            h.assertTrue(got == 30 && api.getAmmoCount(stored) == 20, "box: got " + got + " left " + api.getAmmoCount(stored) + " (free ammo if 50)");
            p.setItemSlot(EquipmentSlot.CHEST, ItemStack.EMPTY);
            h.assertFalse(item.canReload(p, gun), "sees ammo of armor that is off");
            h.succeed();
        }
    }
}
