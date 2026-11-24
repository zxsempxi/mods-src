package com.claude.armorrigs.client;

import com.claude.armorrigs.RigMenu;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Inventory;

public class RigScreen extends AbstractContainerScreen<RigMenu> {
    private static final int BG = 0xF01E1F20;
    private static final int BORDER = 0xFF4E5052;
    private static final int PANEL = 0xFF262728;
    private static final int HEADER = 0xFF96968F;
    private static final int CELL_LINE = 0xFF3A3C3E;
    private static final int CELL = 0xFF161718;
    private static final int TEXT_DARK = 0xFF151515;
    private static final int TEXT_LIGHT = 0xFFC8C8C2;

    public RigScreen(RigMenu menu, Inventory inv, Component title) {
        super(menu, inv, title);
        this.imageWidth = menu.imageWidth;
        this.imageHeight = menu.imageHeight;
        this.inventoryLabelX = menu.invX;
        this.inventoryLabelY = menu.invY - 11;
    }

    @Override
    protected void renderBg(GuiGraphics g, float partialTick, int mouseX, int mouseY) {
        int x0 = leftPos, y0 = topPos;
        g.fill(x0 - 1, y0 - 1, x0 + imageWidth + 1, y0 + imageHeight + 1, BORDER);
        g.fill(x0, y0, x0 + imageWidth, y0 + imageHeight, BG);
        for (int yy = 1; yy < imageHeight; yy += 3) g.fill(x0, y0 + yy, x0 + imageWidth, y0 + yy + 1, 0x22000000);

        for (RigMenu.Panel p : menu.panels) {
            int px = x0 + p.x(), py = y0 + p.y();
            g.fill(px - 1, py - 1, px + p.w() + 1, py + p.h() + 1, BORDER);
            g.fill(px, py, px + p.w(), py + p.h(), PANEL);
            g.fill(px, py, px + p.w(), py + 10, HEADER);
            if (p.present()) {
                for (int r = 0; r < p.rows(); r++) {
                    for (int c = 0; c < p.cols(); c++) {
                        cell(g, px + 3 + c * 18, py + RigMenu.HEADER + r * 18);
                    }
                }
            }
        }

        int ix = x0 + menu.invX, iy = y0 + menu.invY;
        g.fill(ix - 2, iy - 13, ix + 164, iy - 3, HEADER);
        for (int r = 0; r < 3; r++) for (int c = 0; c < 9; c++) cell(g, ix + c * 18, iy + r * 18);
        g.fill(ix, iy + 3 * 18 + 1, ix + 162, iy + 3 * 18 + 2, BORDER);
        for (int c = 0; c < 9; c++) cell(g, ix + c * 18, iy + 3 * 18 + 4);
    }

    private static void cell(GuiGraphics g, int x, int y) {
        g.fill(x, y, x + 18, y + 18, CELL_LINE);
        g.fill(x + 1, y + 1, x + 17, y + 17, CELL);
    }

    @Override
    protected void renderLabels(GuiGraphics g, int mouseX, int mouseY) {
        for (RigMenu.Panel p : menu.panels) {
            String key = switch (p.slot()) {
                case HEAD -> "label.armorrigs.head";
                case CHEST -> "label.armorrigs.chest";
                case LEGS -> "label.armorrigs.legs";
                default -> "label.armorrigs.feet";
            };
            g.drawString(font, Component.translatable(key), p.x() + 3, p.y() + 1, TEXT_DARK, false);
            if (!p.present()) {
                g.drawString(font, Component.translatable("label.armorrigs.empty"), p.x() + 3, p.y() + 14, 0xFF6E706E, false);
            }
        }
        g.drawString(font, playerInventoryTitle, inventoryLabelX, inventoryLabelY, TEXT_DARK, false);
    }

    @Override
    public void render(GuiGraphics g, int mouseX, int mouseY, float partialTick) {
        super.render(g, mouseX, mouseY, partialTick);
        renderTooltip(g, mouseX, mouseY);
    }
}
