package com.gearinventory.menu;

import com.gearinventory.menu.slot.EquipmentMenuSlot;
import com.gearinventory.compat.curios.CuriosCompat;
import com.gearinventory.config.ServerConfig;
import com.gearinventory.data.ArmorStorageDataMap;
import com.gearinventory.menu.slot.CurioProxySlot;
import com.gearinventory.menu.slot.ArmorGridSlot;
import com.gearinventory.storage.ArmorStorage;
import com.gearinventory.storage.ArmorStorageContainer;
import com.gearinventory.storage.StorageSize;
import net.minecraft.server.level.ServerPlayer;
import com.gearinventory.registry.ModMenus;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import com.gearinventory.menu.slot.TrashSlot;
import com.gearinventory.trash.TrashContainer;
import net.minecraft.world.Container;
import net.minecraft.world.inventory.ClickType;
import net.minecraft.world.inventory.CraftingContainer;
import net.minecraft.world.inventory.CraftingMenu;
import net.minecraft.world.inventory.ResultContainer;
import net.minecraft.world.inventory.ResultSlot;
import net.minecraft.world.inventory.TransientCraftingContainer;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;

public class GearInventoryMenu extends AbstractContainerMenu {
    public static final int ARMOR_START = 0, OFFHAND = 4, MAIN_START = 5, HOTBAR_START = 32, END = 41, STORAGE_START = 41,
            STORAGE_END = STORAGE_START + 4 * StorageSize.MAX_SLOTS, RESULT = STORAGE_END, CRAFT_START = RESULT + 1, TRASH = CRAFT_START + 4, BACK = TRASH + 1, ELYTRA = TRASH + 2;
    private final Player player;
    private final CraftingContainer craft = new TransientCraftingContainer(this, 2, 2);
    private final ResultContainer result = new ResultContainer();
    public final ArmorStorageContainer[] storages = new ArmorStorageContainer[4];
    private static final EquipmentSlot[] ARMOR = {EquipmentSlot.HEAD, EquipmentSlot.CHEST, EquipmentSlot.LEGS, EquipmentSlot.FEET};

    public GearInventoryMenu(int id, Inventory inv) {
        super(ModMenus.GEAR.get(), id);
        this.player = inv.player;
        for (int i = 0; i < 4; i++) addSlot(new EquipmentMenuSlot(inv, ARMOR[i], 39 - i, 8, 8 + i * 18));
        addSlot(new EquipmentMenuSlot(inv, EquipmentSlot.OFFHAND, 40, 28, 44));
        for (int r = 0; r < 3; r++) for (int c = 0; c < 9; c++) addSlot(new Slot(inv, 9 + r * 9 + c, 8 + c * 18, 84 + r * 18));
        for (int c = 0; c < 9; c++) addSlot(new Slot(inv, c, 8 + c * 18, 142));
        for (int i = 0; i < 4; i++) {
            if (inv.player instanceof ServerPlayer) {
                for (ItemStack extra : ArmorStorage.takeOverflow(inv.player.getItemBySlot(ARMOR[i]))) inv.placeItemBackInInventory(extra);
            }
            storages[i] = new ArmorStorageContainer(inv.player, ARMOR[i]);
            for (int k = 0; k < StorageSize.MAX_SLOTS; k++) addSlot(new ArmorGridSlot(storages[i], k));
        }
        addSlot(new ResultSlot(player, craft, result, 0, 154, 23));
        for (int i = 0; i < 4; i++) addSlot(new Slot(craft, i, 108 + (i % 2) * 18, 14 + (i / 2) * 18));
        addSlot(new TrashSlot(new TrashContainer(player), 108, 60));
        addSlot(new CurioProxySlot(player, CuriosCompat.BACK, 28, 8));
        addSlot(new CurioProxySlot(player, CuriosCompat.ELYTRA, 28, 26));
    }

    @Override
    public void slotsChanged(Container changed) {
        CraftingMenu.slotChangedCraftingGrid(this, player.level(), player, craft, result, null);
    }

    @Override
    public void removed(Player p) {
        super.removed(p);
        result.clearContent();
        if (!p.level().isClientSide) clearContainer(p, craft);
    }

    @Override
    public boolean canTakeItemForPickAll(ItemStack stack, Slot slot) {
        return slot.container != result && !(slot instanceof TrashSlot) && super.canTakeItemForPickAll(stack, slot);
    }

    @Override
    public void clicked(int index, int button, ClickType type, Player p) {
        if (index == TRASH && type == ClickType.PICKUP && !getCarried().isEmpty()) {
            if (!trashable(getCarried())) return;
            slots.get(TRASH).set(getCarried());
            setCarried(ItemStack.EMPTY);
            return;
        }
        super.clicked(index, button, type, p);
    }

    /** Filled gear would take everything inside with it, so it never goes to the trash. */
    public static boolean trashable(ItemStack stack) {
        return ArmorStorageDataMap.size(stack).slots() == 0 || !ArmorStorage.hasContents(stack);
    }

    /** Ctrl+click. Runs on the server; the previous trash content is gone for good. */
    public boolean trash(Player p, int index) {
        if (index < 0 || index >= slots.size() || index == TRASH || index == RESULT || !getCarried().isEmpty()) return false;
        Slot slot = slots.get(index);
        if (!slot.hasItem() || !slot.mayPickup(p) || !trashable(slot.getItem())) return false;
        ItemStack stack = slot.getItem().copy();
        slot.setByPlayer(ItemStack.EMPTY, stack);
        slots.get(TRASH).set(stack);
        return true;
    }

    @Override
    public boolean stillValid(Player player) {
        return player.isAlive();
    }

    @Override
    public ItemStack quickMoveStack(Player player, int index) {
        Slot slot = slots.get(index);
        if (!slot.hasItem()) return ItemStack.EMPTY;
        ItemStack stack = slot.getItem();
        ItemStack original = stack.copy();
        boolean moved;
        if (index == RESULT) {
            moved = moveItemStackTo(stack, MAIN_START, END, true);
            if (moved) slot.onQuickCraft(stack, original);
        } else if (index < MAIN_START || index >= STORAGE_START) {
            moved = moveItemStackTo(stack, MAIN_START, END, false);
        } else {
            boolean storageFirst = ServerConfig.shiftClick() == ServerConfig.TransferOrder.STORAGE_FIRST;
            moved = moveItemStackTo(stack, ARMOR_START, OFFHAND, false) || moveItemStackTo(stack, BACK, ELYTRA + 1, false);
            if (!moved && storageFirst) moved = moveItemStackTo(stack, STORAGE_START, STORAGE_END, false);
            if (!stack.isEmpty()) {
                moved |= index < HOTBAR_START ? moveItemStackTo(stack, HOTBAR_START, END, false)
                                              : moveItemStackTo(stack, MAIN_START, HOTBAR_START, false);
            }
            if (!stack.isEmpty() && !storageFirst) moved |= moveItemStackTo(stack, STORAGE_START, STORAGE_END, false);
        }        if (!moved) return ItemStack.EMPTY;
        if (stack.isEmpty()) slot.setByPlayer(ItemStack.EMPTY, original);
        else slot.setChanged();
        if (stack.getCount() == original.getCount()) return ItemStack.EMPTY;
        slot.onTake(player, stack);
        if (index == RESULT) player.drop(stack, false);
        return original;
    }
}