package com.gearinventory.client.screen;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.player.Player;

import java.util.Locale;

/** Health, food, armor, experience and (only when not full) air. Reads the player every frame. */
public final class StatBar {
    private StatBar() {}

    private static int icon(GuiGraphics g, Font font, String sprite, String text, int x, int y) {
        g.blitSprite(ResourceLocation.withDefaultNamespace("hud/" + sprite), x, y, 9, 9);
        g.drawString(font, text, x + 11, y + 1, 0xFFC8C8C2, false);
        return x + 11 + font.width(text) + 8;
    }

    public static void render(GuiGraphics g, Font font, int x, int y, int w) {
        g.fill(x, y, x + w, y + 14, 0xFF262728);
        Player p = Minecraft.getInstance().player;
        if (p == null) return;
        int cx = x + 4, cy = y + 2;
        String hp = Mth.ceil(p.getHealth()) + "/" + Mth.ceil(p.getMaxHealth());
        if (p.getAbsorptionAmount() > 0) hp += " +" + Mth.ceil(p.getAbsorptionAmount());
        cx = icon(g, font, "heart/full", hp, cx, cy);
        var food = p.getFoodData();
        cx = icon(g, font, "food_full", food.getFoodLevel() + String.format(Locale.ROOT, " (%.1f)", food.getSaturationLevel()), cx, cy);
        cx = icon(g, font, "armor_full", String.valueOf(p.getArmorValue()), cx, cy);
        if (p.getAirSupply() < p.getMaxAirSupply())
            cx = icon(g, font, "air", Math.max(0, Mth.ceil(p.getAirSupply() * 100F / p.getMaxAirSupply())) + "%", cx, cy);
        Component level = Component.translatable("stat.gearinventory.level", p.experienceLevel);
        g.drawString(font, level, cx, cy + 1, 0xFF80C860, false);
        cx += font.width(level) + 5;
        int end = x + w - 5;
        if (end - cx > 10) {
            g.fill(cx, cy + 2, end, cy + 8, 0xFF161718);
            g.fill(cx, cy + 2, cx + (int) ((end - cx) * Mth.clamp(p.experienceProgress, 0, 1)), cy + 8, 0xFF5E9A44);
        }
    }
}
