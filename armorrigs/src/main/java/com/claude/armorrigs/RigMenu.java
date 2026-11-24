package com.claude.armorrigs;

import net.minecraft.core.NonNullList;
import net.minecraft.core.component.DataComponents;
import net.minecraft.world.SimpleContainer;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.component.ItemContainerContents;

import java.util.ArrayList;
import java.util.List;

public class RigMenu extends AbstractContainerMenu {
    public static final EquipmentSlot[] ORDER = {EquipmentSlot.HEAD, EquipmentSlot.CHEST, EquipmentSlot.LEGS, EquipmentSlot.FEET};
    public static final int HEADER = 12;

    /** Layout of one armor panel, identical on client and server. */
    public record Panel(EquipmentSlot slot, int cols, int rows, int x, int y, int w, int h, boolean present) {}

    public final List<Panel> panels = new ArrayList<>();
    public final int imageWidth;
    public final int imageHeight;
    public final int invX;
    public final int invY;
    private final Player player;
    private final ItemStack[] worn = new ItemStack[4];
    private final int rigSlotCount;

    public RigMenu(int id, Inventory inv) {
        super(ArmorRigs.RIG_MENU.get(), id);
        this.player = inv.player;
        boolean server = !player.level().isClientSide;

        int[][] size = new int[4][];
        for (int i = 0; i < 4; i++) {
            worn[i] = player.getItemBySlot(ORDER[i]);
            size[i] = RigConfig.sizeFor(worn[i]);
        }

        // Columns: [helmet over boots] [chest] [legs]
        int[][] columns = {{0, 3}, {1}, {2}};
        int x = 7;
        int maxBottom = 0;
        Panel[] built = new Panel[4];
        for (int[] col : columns) {
            int colW = 0;
            int y = 6;
            for (int idx : col) {
                int[] s = size[idx];
                boolean present = s != null;
                int cols = present ? s[0] : 0;
                int rows = present ? s[1] : 0;
                int w = Math.max(cols * 18, 58) + 6;
                int h = HEADER + (present ? rows * 18 + 4 : 14);
                built[idx] = new Panel(ORDER[idx], cols, rows, x, y, w, h, present);
                colW = Math.max(colW, w);
                y += h + 5;
            }
            maxBottom = Math.max(maxBottom, y);
            x += colW + 6;
        }
        for (Panel p : built) panels.add(p);

        int rigWidth = x + 1;
        this.imageWidth = Math.max(rigWidth, 176);
        this.invX = (imageWidth - 162) / 2;
        this.invY = maxBottom + 13;
        this.imageHeight = invY + 3 * 18 + 4 + 18 + 7;

        int count = 0;
        for (int i = 0; i < 4; i++) {
            Panel p = built[i];
            if (!p.present()) continue;
            int slots = p.cols() * p.rows();
            ItemStack armor = worn[i];
            ItemContainerContents contents = armor.getOrDefault(DataComponents.CONTAINER, ItemContainerContents.EMPTY);
            RigContainer container = new RigContainer(slots, armor, server);
            NonNullList<ItemStack> list = NonNullList.withSize(slots, ItemStack.EMPTY);
            contents.copyInto(list);
            for (int k = 0; k < slots; k++) container.getItems().set(k, list.get(k));
            if (server) {
                // Items stored beyond the current size (config shrank) go back to the player instead of vanishing.
                List<ItemStack> all = contents.stream().toList();
                boolean overflow = false;
                for (int k = slots; k < all.size(); k++) {
                    if (!all.get(k).isEmpty()) {
                        player.getInventory().placeItemBackInInventory(all.get(k).copy());
                        overflow = true;
                    }
                }
                if (overflow) container.setChanged();
            }
            for (int r = 0; r < p.rows(); r++) {
                for (int c = 0; c < p.cols(); c++) {
                    addSlot(new RigSlot(container, r * p.cols() + c, p.x() + 4 + c * 18, p.y() + HEADER + 1 + r * 18));
                    count++;
                }
            }
        }
        this.rigSlotCount = count;

        for (int r = 0; r < 3; r++) {
            for (int c = 0; c < 9; c++) {
                addSlot(new Slot(inv, 9 + r * 9 + c, invX + 1 + c * 18, invY + 1 + r * 18));
            }
        }
        for (int c = 0; c < 9; c++) {
            addSlot(new Slot(inv, c, invX + 1 + c * 18, invY + 1 + 3 * 18 + 4));
        }
    }

    @Override
    public boolean stillValid(Player p) {
        for (int i = 0; i < 4; i++) {
            if (panels.get(i).present() && p.getItemBySlot(ORDER[i]) != worn[i]) return false;
        }
        return p.isAlive();
    }

    @Override
    public ItemStack quickMoveStack(Player p, int index) {
        Slot slot = slots.get(index);
        if (!slot.hasItem()) return ItemStack.EMPTY;
        ItemStack stack = slot.getItem();
        ItemStack copy = stack.copy();
        int total = slots.size();
        if (index < rigSlotCount) {
            if (!moveItemStackTo(stack, rigSlotCount, total, true)) return ItemStack.EMPTY;
        } else {
            if (rigSlotCount == 0 || !moveItemStackTo(stack, 0, rigSlotCount, false)) return ItemStack.EMPTY;
        }
        if (stack.isEmpty()) slot.setByPlayer(ItemStack.EMPTY);
        else slot.setChanged();
        return copy;
    }

    /** Container backed by the CONTAINER component of the worn armor piece. */
    static class RigContainer extends SimpleContainer {
        private final ItemStack armor;
        private final boolean server;

        RigContainer(int size, ItemStack armor, boolean server) {
            super(size);
            this.armor = armor;
            this.server = server;
        }

        @Override
        public void setChanged() {
            super.setChanged();
            if (server) armor.set(DataComponents.CONTAINER, ItemContainerContents.fromItems(getItems()));
        }
    }

    static class RigSlot extends Slot {
        RigSlot(SimpleContainer c, int index, int x, int y) {
            super(c, index, x, y);
        }

        @Override
        public boolean mayPlace(ItemStack stack) {
            ItemContainerContents inner = stack.get(DataComponents.CONTAINER);
            if (inner != null && inner.nonEmptyItems().iterator().hasNext()) return false; // no nested storage
            return RigConfig.sizeFor(stack) == null || inner == null;
        }
    }
}
