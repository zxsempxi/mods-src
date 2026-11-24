package com.gearinventory.client.screen;

import com.gearinventory.menu.LootModeSlots;
import com.gearinventory.menu.slot.ArmorGridSlot;
import com.gearinventory.net.LootActionPayload;
import com.gearinventory.net.TrashItemPayload;
import com.gearinventory.storage.StorageSize;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.network.chat.Component;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.ChestMenu;
import net.minecraft.world.inventory.Slot;
import net.neoforged.neoforge.network.PacketDistributor;

/** Replaces the vanilla chest, shulker, hopper and dispenser screens. Wide: three columns. Narrow: gear and storage share tabs. */
public class LootScreen extends AbstractContainerScreen<AbstractContainerMenu> {
    public static final int MIN_WIDTH = 356, MIN_HEIGHT = 190;
    private static final int WIDE = 414, COL_Y = 6, INV_Y = 88, ULTRA_W = 546;
    private static final String[] NAMES = {"head", "chest", "legs", "feet"};
    private final LootModeSlots.Layout l;
    private final int cols, rows;
    private final int[][] sections = new int[4][2];
    private boolean wide, ultra, storageTab = true, dragging;
    private int colH = 76;
    private int colX, colW, contX, scroll, content;

    public LootScreen(AbstractContainerMenu menu, Inventory inv, Component title) {
        super(menu, inv, title);
        l = LootModeSlots.layout(menu);
        cols = menu instanceof ChestMenu ? 9 : l.container() == 5 ? 5 : l.container() == 9 ? 3 : 9;
        rows = Mth.positiveCeilDiv(l.container(), cols);
    }

    @Override
    protected void init() {
        wide = width >= WIDE && !com.gearinventory.config.ClientConfig.ALWAYS_TABS.get();
        ultra = wide && Ultrawide.is(width, height) && width >= ULTRA_W + 150;
        colH = ultra ? 158 : 76;
        imageWidth = ultra ? ULTRA_W : wide ? 410 : 352;
        imageHeight = 186;
        colX = ultra ? 182 : wide ? 52 : 8;
        colW = wide ? 176 : 156;
        contX = ultra ? 376 : wide ? 240 : 182;
        super.init();
        leftPos = Mth.clamp(leftPos + com.gearinventory.config.ClientConfig.OFFSET_X.get(), 0, Math.max(0, width - imageWidth));
        topPos = Mth.clamp(topPos + com.gearinventory.config.ClientConfig.OFFSET_Y.get(), 0, Math.max(0, height - imageHeight));
        int by = topPos + Math.max(94, Math.min(130, rows * 18 + 24));
        String[] keys = {"take", "deposit", "sort"};
        boolean column = rows < 6;
        for (int i = 0; i < 3; i++) {
            int action = i;
            int bx = column ? leftPos + contX : leftPos + contX + (i % 2) * 82;
            int yy = column ? by + i * 17 : topPos + 133 + (i / 2) * 17;
            addRenderableWidget(Button.builder(Component.translatable("button.gearinventory." + keys[i]),
                    b -> PacketDistributor.sendToServer(new LootActionPayload(menu.containerId, action)))
                    .bounds(bx, yy, column ? 92 : 80, 15).build());
        }
        if (!wide) {
            addRenderableWidget(Button.builder(tabName(), b -> {
                storageTab = !storageTab;
                b.setMessage(tabName());
            }).bounds(leftPos + contX + (column ? 94 : 82), column ? by : topPos + 150, column ? 72 : 80, 15).build());
        }
        layout();
    }

    private Component tabName() {
        return Component.translatable(storageTab ? "button.gearinventory.tab.gear" : "button.gearinventory.tab.storage");
    }

    private void put(int index, int x, int y) {
        menu.slots.get(index).x = x;
        menu.slots.get(index).y = y;
    }

    private void layout() {
        for (int i = 0; i < l.container(); i++) put(i, contX + 1 + (i % cols) * 18, 20 + (i / cols) * 18);
        for (int i = 0; i < 27; i++) put(l.player() + i, 9 + (i % 9) * 18, INV_Y + 1 + (i / 9) * 18);
        for (int i = 0; i < 9; i++) put(l.hotbar() + i, 9 + i * 18, INV_Y + 59);
        boolean gear = wide || !storageTab, storage = wide || storageTab;
        for (int i = 0; i < 4; i++) put(l.armor() + i, gear ? 9 : -10000, 8 + i * 18);
        int[] right = {l.back(), l.elytra(), l.offhand(), l.trash()};
        for (int i = 0; i < 4; i++) put(right[i], gear ? 28 : -10000, 8 + i * 18);
        int y = 0;
        for (int i = 0; i < 4; i++) {
            StorageSize s = l.storages()[i].size();
            int h = 12 + (s.slots() == 0 ? 12 : s.height() * 18 + 3);
            sections[i][0] = y;
            sections[i][1] = h;
            y += h + 3;
        }
        content = y - 3;
        scroll = Mth.clamp(scroll, 0, maxScroll());
        for (int i = 0; i < 4; i++) {
            StorageSize s = l.storages()[i].size();
            for (int k = 0; k < StorageSize.MAX_SLOTS; k++) {
                ArmorGridSlot slot = (ArmorGridSlot) menu.slots.get(l.base() + i * StorageSize.MAX_SLOTS + k);
                int sy = k < s.slots() ? COL_Y + sections[i][0] + 13 + (k / s.width()) * 18 - scroll : -1;
                slot.visible = storage && k < s.slots() && sy - 1 >= COL_Y && sy + 17 <= COL_Y + colH;
                slot.x = slot.visible ? colX + 4 + (k % s.width()) * 18 : -10000;
                slot.y = slot.visible ? sy : -10000;
            }
        }
    }

    private int maxScroll() {
        return wide || storageTab ? Math.max(0, content - colH) : 0;
    }

    private static void cell(GuiGraphics g, int x, int y) {
        g.fill(x - 1, y - 1, x + 17, y + 17, 0xFF3A3C3E);
        g.fill(x, y, x + 16, y + 16, 0xFF161718);
    }

    @Override
    protected void renderBg(GuiGraphics g, float partial, int mx, int my) {
        int lx = leftPos, ty = topPos;
        g.fill(lx - 1, ty - 1, lx + imageWidth + 1, ty + imageHeight + 1, 0xFF4E5052);
        g.fill(lx, ty, lx + imageWidth, ty + imageHeight, 0xF01E1F20);
        g.fill(lx + contX, ty + COL_Y, lx + contX + 162, ty + COL_Y + 10, 0xFF96968F);
        for (int i = 0; i < l.size(); i++) {
            Slot s = menu.slots.get(i);
            if (s.x > -1000 && !(s instanceof ArmorGridSlot)) cell(g, lx + s.x, ty + s.y);
        }
        Slot bin = menu.slots.get(l.trash());
        if (bin.x > -1000 && !bin.hasItem()) {
            g.fill(lx + bin.x + 4, ty + bin.y + 3, lx + bin.x + 12, ty + bin.y + 4, 0xFF6E706E);
            g.fill(lx + bin.x + 5, ty + bin.y + 5, lx + bin.x + 11, ty + bin.y + 14, 0xFF4E5052);
        }
        if (ultra && minecraft != null && minecraft.player != null) {
            g.fill(lx + 60, ty + 7, lx + 168, ty + 80, 0xFF0A0A0B);
            net.minecraft.client.gui.screens.inventory.InventoryScreen.renderEntityInInventoryFollowsMouse(g, lx + 61, ty + 8, lx + 167, ty + 79, 30, 0.0625F, mx, my, minecraft.player);
        }
        if (wide || storageTab) {
            g.enableScissor(lx + colX, ty + COL_Y, lx + colX + colW, ty + COL_Y + colH);
            for (int i = 0; i < 4; i++) {
                int y = ty + COL_Y + sections[i][0] - scroll;
                g.fill(lx + colX, y, lx + colX + colW, y + sections[i][1], 0xFF262728);
                g.fill(lx + colX, y, lx + colX + colW, y + 10, 0xFF96968F);
                g.drawString(font, Component.translatable("section.gearinventory." + NAMES[i]), lx + colX + 3, y + 1, 0xFF151515, false);
                StorageSize s = l.storages()[i].size();
                if (s.slots() == 0)
                    g.drawString(font, Component.translatable("section.gearinventory.none." + NAMES[i]), lx + colX + 4, y + 13, 0xFF6E706E, false);
                for (int k = 0; k < s.slots(); k++) cell(g, lx + colX + 4 + (k % s.width()) * 18, y + 13 + (k / s.width()) * 18);
            }
            g.disableScissor();
            if (maxScroll() > 0) {
                int h = Math.max(12, colH * colH / content), y = ty + COL_Y + (colH - h) * scroll / maxScroll(), x = lx + colX + colW + 2;
                g.fill(x, ty + COL_Y, x + 5, ty + COL_Y + colH, 0xFF161718);
                g.fill(x, y, x + 5, y + h, 0xFF96968F);
            }
        }
        StatBar.render(g, font, lx + 4, ty + 169, imageWidth - 8);
    }

    @Override
    protected void renderLabels(GuiGraphics g, int mx, int my) {
        g.drawString(font, font.plainSubstrByWidth(title.getString(), 156), contX + 3, COL_Y + 1, 0xFF151515, false);
    }

    @Override
    public void render(GuiGraphics g, int mx, int my, float partial) {
        layout();
        super.render(g, mx, my, partial);
        renderTooltip(g, mx, my);
    }

    private boolean overColumn(double x, double y) {
        return x >= leftPos + colX && x < leftPos + colX + colW + 8 && y >= topPos + COL_Y && y < topPos + COL_Y + colH;
    }

    private void dragTo(double y) {
        int h = Math.max(12, colH * colH / Math.max(1, content));
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
        if (button == 0 && hasControlDown() && hoveredSlot != null && hoveredSlot.hasItem()
                && menu.getCarried().isEmpty() && hoveredSlot.index != l.trash()) {
            PacketDistributor.sendToServer(new TrashItemPayload(menu.containerId, hoveredSlot.index));
            return true;
        }
        if (button == 0 && maxScroll() > 0 && overColumn(x, y) && x >= leftPos + colX + colW) {
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
