package com.gearinventory.client.screen;

import com.gearinventory.menu.GearInventoryMenu;
import com.gearinventory.menu.slot.ArmorGridSlot;
import com.gearinventory.storage.StorageSize;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.client.gui.screens.inventory.InventoryScreen;
import net.minecraft.network.chat.Component;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.inventory.Slot;

public class GearInventoryScreen extends AbstractContainerScreen<GearInventoryMenu> {
    private static final String[] NAMES = {"head", "chest", "legs", "feet", "backpack"};
    private static final int COL_X = 176, COL_Y = 6, COL_W = 168, BAR_W = 5, GAP = 6;
    /** Ultrawide: two pocket columns side by side. */
    private boolean two;
    private int colH = 156;
    private final int[][] sections = new int[5][2]; // y, h
    private int scroll, content;
    private boolean dragging;

    public GearInventoryScreen(GearInventoryMenu menu, Inventory inv, Component title) {
        super(menu, inv, title);
        imageWidth = 356;
        imageHeight = 184;
    }

    @Override
    protected void init() {
        two = Ultrawide.is(width, height) && width >= 356 + COL_W + GAP + 170;
        colH = two && height >= 236 ? 186 : 156;
        imageWidth = 356 + (two ? COL_W + GAP : 0);
        imageHeight = colH + 28;
        super.init();
        leftPos = Mth.clamp(leftPos + com.gearinventory.config.ClientConfig.OFFSET_X.get(), 0, Math.max(0, width - imageWidth));
        topPos = Mth.clamp(topPos + com.gearinventory.config.ClientConfig.OFFSET_Y.get(), 0, Math.max(0, height - imageHeight));
    }

    private int col(int i) {
        return two && i >= 2 ? 1 : 0;
    }

    private int colX(int i) {
        return COL_X + col(i) * (COL_W + GAP);
    }

    private int barX() {
        return imageWidth - 10;
    }

    private int maxScroll() {
        return Math.max(0, content - colH);
    }

    private StorageSize size(int i) {
        return i < 4 ? menu.storages[i].size() : StorageSize.NONE;
    }

    private void layout() {
        int[] ys = new int[2];
        for (int i = 0; i < 5; i++) {
            StorageSize s = size(i);
            int h = 12 + (i == 4 && menu.slots.get(GearInventoryMenu.BACK).hasItem() ? 22 : s.slots() == 0 ? 14 : s.height() * 18 + 3);
            sections[i][0] = ys[col(i)];
            sections[i][1] = h;
            ys[col(i)] += h + 4;
        }
        content = Math.max(ys[0], ys[1]) - 4;
        scroll = Mth.clamp(scroll, 0, maxScroll());
        for (int i = 0; i < 4; i++) {
            StorageSize s = size(i);
            for (int k = 0; k < StorageSize.MAX_SLOTS; k++) {
                ArmorGridSlot slot = (ArmorGridSlot) menu.slots.get(GearInventoryMenu.STORAGE_START + i * StorageSize.MAX_SLOTS + k);
                int sy = k < s.slots() ? COL_Y + sections[i][0] + 13 + (k / s.width()) * 18 - scroll : -1;
                // A slot takes clicks only when it is fully inside the column.
                slot.visible = k < s.slots() && sy - 1 >= COL_Y && sy + 17 <= COL_Y + colH;
                slot.x = slot.visible ? colX(i) + 4 + (k % s.width()) * 18 : -10000;
                slot.y = slot.visible ? sy : -10000;
            }
        }
    }

    private void cell(GuiGraphics g, int x, int y) {
        g.fill(x - 1, y - 1, x + 17, y + 17, 0xFF3A3C3E);
        g.fill(x, y, x + 16, y + 16, 0xFF161718);
    }

    @Override
    protected void renderBg(GuiGraphics g, float partial, int mx, int my) {
        int l = leftPos, t = topPos;
        g.fill(l - 1, t - 1, l + imageWidth + 1, t + imageHeight + 1, 0xFF4E5052);
        g.fill(l, t, l + imageWidth, t + imageHeight, 0xF01E1F20);
        for (int i = 0; i < GearInventoryMenu.END; i++) cell(g, l + menu.slots.get(i).x, t + menu.slots.get(i).y);
        for (int i = GearInventoryMenu.RESULT; i <= GearInventoryMenu.ELYTRA; i++) cell(g, l + menu.slots.get(i).x, t + menu.slots.get(i).y);
        if (!menu.slots.get(GearInventoryMenu.TRASH).hasItem()) {
            int tx = l + 108, ty = t + 60;
            g.fill(tx + 4, ty + 3, tx + 12, ty + 4, 0xFF6E706E);
            g.fill(tx + 6, ty + 2, tx + 10, ty + 3, 0xFF6E706E);
            g.fill(tx + 5, ty + 5, tx + 11, ty + 14, 0xFF4E5052);
        }
        g.fill(l + 146, t + 30, l + 151, t + 32, 0xFF96968F);
        g.fill(l + 50, t + 7, l + 102, t + 79, 0xFF0A0A0B);
        if (minecraft != null && minecraft.player != null)
            InventoryScreen.renderEntityInInventoryFollowsMouse(g, l + 51, t + 8, l + 101, t + 78, 30, 0.0625F, mx, my, minecraft.player);
        g.enableScissor(l + COL_X, t + COL_Y, l + colX(4) + COL_W, t + COL_Y + colH);
        for (int i = 0; i < 5; i++) {
            int y = t + COL_Y + sections[i][0] - scroll;
            int cx = l + colX(i);
            g.fill(cx, y, cx + COL_W, y + sections[i][1], 0xFF262728);
            g.fill(cx, y, cx + COL_W, y + 10, 0xFF96968F);
            g.drawString(font, Component.translatable("section.gearinventory." + NAMES[i]), cx + 3, y + 1, 0xFF151515, false);
            StorageSize s = size(i);
            var pack = menu.slots.get(GearInventoryMenu.BACK).getItem();
            if (i == 4 && !pack.isEmpty()) {
                g.renderItem(pack, cx + 4, y + 13);
                g.drawString(font, font.plainSubstrByWidth(pack.getHoverName().getString(), canOpenPack() ? COL_W - 82 : COL_W - 28), cx + 24, y + 17, 0xFFC8C8C2, false);
                if (canOpenPack()) {
                    Component open = Component.translatable("button.gearinventory.open");
                    g.fill(l + openX(), y + 13, l + openX() + 50, y + 27, 0xFF4E5052);
                    g.drawCenteredString(font, open, l + openX() + 25, y + 16, 0xFFE8E8E2);
                }
            } else if (s.slots() == 0)
                g.drawString(font, Component.translatable("section.gearinventory.none." + NAMES[i]), cx + 4, y + 14, 0xFF6E706E, false);
            for (int k = 0; k < s.slots(); k++) cell(g, cx + 4 + (k % s.width()) * 18, y + 13 + (k / s.width()) * 18);
        }
        g.disableScissor();
        if (maxScroll() > 0) {
            int h = Math.max(16, colH * colH / content), y = t + COL_Y + (colH - h) * scroll / maxScroll();
            g.fill(l + barX(), t + COL_Y, l + barX() + BAR_W, t + COL_Y + colH, 0xFF161718);
            g.fill(l + barX(), y, l + barX() + BAR_W, y + h, 0xFF96968F);
        }
        StatBar.render(g, font, l + 4, t + imageHeight - 18, imageWidth - 8);
    }

    @Override
    protected void renderLabels(GuiGraphics g, int mx, int my) {}

    @Override
    public void render(GuiGraphics g, int mx, int my, float partial) {
        layout();
        super.render(g, mx, my, partial);
        renderTooltip(g, mx, my);
    }

    private int openX() {
        return colX(4) + COL_W - 54;
    }

    private boolean canOpenPack() {
        return com.gearinventory.compat.sophisticated.SophisticatedCompat.loaded()
                && "sophisticatedbackpacks".equals(net.minecraft.core.registries.BuiltInRegistries.ITEM
                .getKey(menu.slots.get(GearInventoryMenu.BACK).getItem().getItem()).getNamespace());
    }

    private boolean overColumn(double x, double y) {
        return x >= leftPos + COL_X && x < leftPos + barX() + BAR_W && y >= topPos + COL_Y && y < topPos + COL_Y + colH;
    }

    private void dragTo(double y) {
        int h = Math.max(16, colH * colH / Math.max(1, content));
        scroll = (int) Mth.clamp((y - topPos - COL_Y - h / 2.0) / (colH - h) * maxScroll(), 0, maxScroll());
        layout();
    }

    @Override
    public boolean mouseScrolled(double x, double y, double dx, double dy) {
        if (overColumn(x, y) && maxScroll() > 0) {
            scroll = Mth.clamp(scroll - (int) Math.signum(dy) * 18, 0, maxScroll());
            layout();
            return true;
        }
        return super.mouseScrolled(x, y, dx, dy);
    }

    @Override
    public boolean mouseClicked(double x, double y, int button) {
        if (button == 0 && hasControlDown() && hoveredSlot != null && hoveredSlot.hasItem() && menu.getCarried().isEmpty()
                && hoveredSlot.index != GearInventoryMenu.TRASH && hoveredSlot.index != GearInventoryMenu.RESULT) {
            net.neoforged.neoforge.network.PacketDistributor.sendToServer(
                    new com.gearinventory.net.TrashItemPayload(menu.containerId, hoveredSlot.index));
            return true;
        }
        int openY = topPos + COL_Y + sections[4][0] - scroll + 13;
        if (button == 0 && canOpenPack() && overColumn(x, y) && x >= leftPos + openX() && x < leftPos + openX() + 50 && y >= openY && y < openY + 14) {
            com.gearinventory.compat.sophisticated.SophisticatedCompat.open();
            return true;
        }
        if (button == 0 && maxScroll() > 0 && overColumn(x, y) && x >= leftPos + barX()) {
            dragging = true;
            dragTo(y);
            return true;
        }
        return super.mouseClicked(x, y, button);
    }

    @Override
    public boolean mouseDragged(double x, double y, int button, double dx, double dy) {
        if (dragging) {
            dragTo(y);
            return true;
        }
        return super.mouseDragged(x, y, button, dx, dy);
    }

    @Override
    public boolean mouseReleased(double x, double y, int button) {
        dragging = false;
        return super.mouseReleased(x, y, button);
    }
}