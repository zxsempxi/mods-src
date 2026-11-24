package com.gearinventory.gametest;

import com.gearinventory.GearInventory;
import com.gearinventory.menu.LootModeSlots;
import com.gearinventory.net.LootActionPayload;
import com.gearinventory.storage.ArmorStorage;
import com.gearinventory.trash.TrashAttachment;
import net.minecraft.core.component.DataComponents;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.util.RandomSource;
import net.minecraft.world.SimpleContainer;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.ChestMenu;
import net.minecraft.world.inventory.ClickType;
import net.minecraft.world.inventory.DispenserMenu;
import net.minecraft.world.inventory.HopperMenu;
import net.minecraft.world.inventory.ShulkerBoxMenu;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.component.ItemContainerContents;
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;

import java.util.List;

@GameTestHolder(GearInventory.MODID)
@PrefixGameTestTemplate(false)
public class Stage5Tests {
    private static AbstractContainerMenu[] all(ServerPlayer p) {
        return new AbstractContainerMenu[]{ChestMenu.threeRows(1, p.getInventory(), new SimpleContainer(27)),
                ChestMenu.sixRows(1, p.getInventory(), new SimpleContainer(54)), new ShulkerBoxMenu(1, p.getInventory(), new SimpleContainer(27)),
                new HopperMenu(1, p.getInventory(), new SimpleContainer(5)), new DispenserMenu(1, p.getInventory(), new SimpleContainer(9))};
    }

    private static ServerPlayer geared(GameTestHelper h) {
        ServerPlayer p = TestPlayers.server(h);
        p.setItemSlot(EquipmentSlot.CHEST, new ItemStack(Items.LEATHER_CHESTPLATE));
        p.setItemSlot(EquipmentSlot.FEET, new ItemStack(Items.LEATHER_BOOTS));
        return p;
    }

    private static int total(AbstractContainerMenu m, Item item) {
        int n = m.getCarried().is(item) ? m.getCarried().getCount() : 0;
        for (Slot s : m.slots) if (s.getItem().is(item)) n += s.getItem().getCount();
        return n;
    }

    @GameTest(template = "empty")
    public static void sameSlotsOnBothConstructors(GameTestHelper h) {
        ServerPlayer p = TestPlayers.server(h);
        AbstractContainerMenu[] server = all(p), client = {ChestMenu.threeRows(1, p.getInventory()), ChestMenu.sixRows(1, p.getInventory()),
                new ShulkerBoxMenu(1, p.getInventory()), new HopperMenu(1, p.getInventory()), new DispenserMenu(1, p.getInventory())};
        int[] sizes = {27, 54, 27, 5, 9};
        for (int i = 0; i < 5; i++) {
            var l = LootModeSlots.layout(server[i]);
            h.assertTrue(l != null && l.container() == sizes[i] && l.size() == server[i].slots.size(), "layout " + i);
            h.assertTrue(server[i].slots.size() == client[i].slots.size(), "count differs " + i);
            for (int k = 0; k < l.size(); k++)
                h.assertTrue(server[i].slots.get(k).getClass() == client[i].slots.get(k).getClass(), "slot " + k);
        }
        h.succeed();
    }

    @GameTest(template = "empty")
    public static void shiftClickFillsInventoryThenStorage(GameTestHelper h) {
        for (int which = 0; which < 5; which++) {
            ServerPlayer p = geared(h);
            for (int i = 0; i < 36; i++) p.getInventory().setItem(i, new ItemStack(Items.DIRT, 64));
            AbstractContainerMenu m = all(p)[which];
            m.slots.get(0).set(new ItemStack(Items.BREAD, 30));
            m.quickMoveStack(p, 0);
            h.assertTrue(total(m, Items.BREAD) == 30 && ArmorStorage.used(p.getItemBySlot(EquipmentSlot.CHEST)) == 1, "to storage " + which);
            m.quickMoveStack(p, LootModeSlots.layout(m).base() + 54);
            h.assertTrue(m.slots.get(0).getItem().getCount() == 30, "storage to container " + which);
        }
        h.succeed();
    }

    @GameTest(template = "empty")
    public static void buttons(GameTestHelper h) {
        ServerPlayer p = geared(h);
        ChestMenu m = ChestMenu.threeRows(7, p.getInventory(), new SimpleContainer(27));
        p.containerMenu = m;
        m.slots.get(5).set(new ItemStack(Items.BREAD, 40));
        m.slots.get(9).set(new ItemStack(Items.BREAD, 40));
        m.slots.get(20).set(new ItemStack(Items.APPLE, 3));
        h.assertFalse(LootActionPayload.handle(p, new LootActionPayload(8, 0)) || LootActionPayload.handle(p, new LootActionPayload(7, 99)), "bad packet accepted");
        h.assertTrue(LootActionPayload.handle(p, new LootActionPayload(7, LootActionPayload.SORT)), "sort");
        h.assertTrue(m.slots.get(0).getItem().is(Items.APPLE) && m.slots.get(1).getItem().getCount() == 64
                && m.slots.get(2).getItem().getCount() == 16, "sort order");
        p.getInventory().setItem(9, new ItemStack(Items.BREAD, 5));
        p.getInventory().setItem(0, new ItemStack(Items.BREAD, 7));
        p.getInventory().setItem(10, new ItemStack(Items.DIRT, 5));
        LootActionPayload.handle(p, new LootActionPayload(7, LootActionPayload.DEPOSIT_ALL));
        h.assertTrue(p.getInventory().getItem(9).isEmpty() && p.getInventory().getItem(0).getCount() == 7
                && p.getInventory().getItem(10).getCount() == 5, "deposit rules");
        LootActionPayload.handle(p, new LootActionPayload(7, LootActionPayload.TAKE_ALL));
        h.assertTrue(m.slots.get(0).getItem().isEmpty() && total(m, Items.BREAD) == 92 && total(m, Items.APPLE) == 3, "take all");
        h.succeed();
    }

    @GameTest(template = "empty")
    public static void trashInLootMode(GameTestHelper h) {
        ServerPlayer p = geared(h);
        ChestMenu m = ChestMenu.threeRows(7, p.getInventory(), new SimpleContainer(27));
        var l = LootModeSlots.layout(m);
        m.slots.get(3).set(new ItemStack(Items.DIRT, 9));
        h.assertTrue(LootModeSlots.trash(m, p, 3) && p.getData(TrashAttachment.TRASH).getCount() == 9, "ctrl-click");
        m.setCarried(new ItemStack(Items.BREAD, 2));
        m.clicked(l.trash(), 0, ClickType.PICKUP, p);
        h.assertTrue(m.getCarried().isEmpty() && p.getData(TrashAttachment.TRASH).is(Items.BREAD), "click replace");
        h.assertFalse(LootModeSlots.trash(m, p, l.trash()) || LootModeSlots.trash(m, p, 5000), "bad index");
        h.succeed();
    }

    @GameTest(template = "empty")
    public static void nestingBothWays(GameTestHelper h) {
        ServerPlayer p = geared(h);
        ItemStack full = new ItemStack(Items.IRON_CHESTPLATE);
        ArmorStorage.write(full, List.of(new ItemStack(Items.DIRT)));
        ItemStack box = new ItemStack(Items.SHULKER_BOX);
        box.set(DataComponents.CONTAINER, ItemContainerContents.fromItems(List.of(new ItemStack(Items.DIRT))));
        ShulkerBoxMenu m = new ShulkerBoxMenu(1, p.getInventory(), new SimpleContainer(27));
        h.assertFalse(m.slots.get(0).mayPlace(full), "filled rig in shulker");
        h.assertTrue(m.slots.get(0).mayPlace(new ItemStack(Items.IRON_CHESTPLATE)), "empty rig refused");
        h.assertFalse(m.slots.get(LootModeSlots.layout(m).base() + 54).mayPlace(box), "filled shulker in rig");
        h.succeed();
    }

    /** Many click types on every slot of every menu: nothing may appear or vanish. */
    @GameTest(template = "empty", timeoutTicks = 600)
    public static void randomClicksConserveItems(GameTestHelper h) {
        RandomSource rnd = RandomSource.create(99);
        for (int which = 0; which < 5; which++) {
            ServerPlayer p = geared(h);
            for (int i = 0; i < 30; i++) p.getInventory().setItem(i, new ItemStack(i % 2 == 0 ? Items.BREAD : Items.ARROW, 1 + i));
            AbstractContainerMenu m = all(p)[which];
            p.containerMenu = m;
            m.slots.get(1).set(new ItemStack(Items.ARROW, 50));
            var l = LootModeSlots.layout(m);
            int bread = everything(m, Items.BREAD), arrows = everything(m, Items.ARROW);
            for (int n = 0; n < 4000; n++) {
                int index = rnd.nextInt(l.trash());
                switch (rnd.nextInt(6)) {
                    case 0, 1 -> m.clicked(index, rnd.nextInt(2), ClickType.PICKUP, p);
                    case 2 -> m.clicked(index, 0, ClickType.QUICK_MOVE, p);
                    case 3 -> m.clicked(index, rnd.nextInt(9), ClickType.SWAP, p);
                    case 4 -> m.clicked(index, 0, ClickType.PICKUP_ALL, p);
                    default -> LootActionPayload.handle(p, new LootActionPayload(1, rnd.nextInt(3)));
                }
                int b = everything(m, Items.BREAD), a = everything(m, Items.ARROW);
                h.assertTrue(b == bread && a == arrows, "menu " + which + " step " + n + " slot " + index + ": bread " + bread + "->" + b + ", arrows " + arrows + "->" + a);
            }
        }
        h.succeed();
    }

    /** Visible items plus the contents of gear that is not worn (worn gear already shows through its storage slots). */
    private static int everything(AbstractContainerMenu m, Item item) {
        int n = total(m, item) + inside(m.getCarried(), item);
        var l = LootModeSlots.layout(m);
        for (int i = 0; i < l.size(); i++)
            if (i < l.armor() || i >= l.offhand()) n += inside(m.slots.get(i).getItem(), item);
        return n;
    }

    private static int inside(ItemStack gear, Item item) {
        ItemContainerContents c = gear.get(DataComponents.CONTAINER);
        return c == null ? 0 : c.stream().filter(s -> s.is(item)).mapToInt(ItemStack::getCount).sum();
    }
}
