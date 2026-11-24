package com.gearinventory.client.hud;

import com.gearinventory.GearInventory;
import com.gearinventory.config.ClientConfig;
import com.gearinventory.health.LowHealth;
import com.gearinventory.registry.ModSounds;
import com.mojang.blaze3d.systems.RenderSystem;
import net.minecraft.client.DeltaTracker;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.resources.sounds.SimpleSoundInstance;
import net.minecraft.resources.ResourceLocation;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.ClientTickEvent;
import net.neoforged.neoforge.client.event.RegisterGuiLayersEvent;
import net.neoforged.neoforge.client.event.RenderGuiLayerEvent;
import net.neoforged.neoforge.client.gui.VanillaGuiLayers;

import java.util.Set;

/** Hides the vanilla bars, draws the vignette and plays the heartbeat. */
public final class HealthEffects {
    private static final int NX = 48, NY = 24;
    private static final Set<ResourceLocation> HIDDEN = Set.of(VanillaGuiLayers.PLAYER_HEALTH, VanillaGuiLayers.FOOD_LEVEL,
            VanillaGuiLayers.ARMOR_LEVEL, VanillaGuiLayers.EXPERIENCE_BAR, VanillaGuiLayers.EXPERIENCE_LEVEL, VanillaGuiLayers.AIR_LEVEL);
    private static final LowHealth STATE = new LowHealth();

    private HealthEffects() {}

    /** Vertex gradient: a texture showed a hard edge, its faint pixels were cut off by the shader. */
    private static void mesh(GuiGraphics g, float alpha) {
        var buf = g.bufferSource().getBuffer(net.minecraft.client.renderer.RenderType.guiOverlay());
        var pose = g.pose().last().pose();
        float w = g.guiWidth(), h = g.guiHeight();
        int[] dx = {0, 0, 1, 1}, dy = {0, 1, 1, 0};
        for (int x = 0; x < NX; x++) {
            for (int y = 0; y < NY; y++) {
                for (int k = 0; k < 4; k++) {
                    float u = (x + dx[k]) / (float) NX, v = (y + dy[k]) / (float) NY;
                    float d = (float) Math.sqrt((u - 0.5) * (u - 0.5) + (v - 0.5) * (v - 0.5)) * 2;
                    float s = net.minecraft.util.Mth.clamp((d - 0.55F) / 0.85F, 0, 1);
                    s = s * s * (3 - 2 * s);
                    buf.addVertex(pose, u * w, v * h, 0).setColor(0.45F, 0.0F, 0.0F, s * alpha * 0.9F);
                }
            }
        }
        g.flush();
    }

    private static void render(GuiGraphics g, DeltaTracker delta) {
        Minecraft mc = Minecraft.getInstance();
        if (mc.player == null || mc.options.hideGui || !ClientConfig.VIGNETTE.get()) return;
        float alpha = STATE.alpha(delta.getGameTimeDeltaPartialTick(false));
        if (alpha <= 0.005F) return;
        RenderSystem.disableDepthTest();
        RenderSystem.depthMask(false);
        RenderSystem.enableBlend();
        RenderSystem.defaultBlendFunc();
        mesh(g, alpha);
        RenderSystem.disableBlend();
        RenderSystem.depthMask(true);
        RenderSystem.enableDepthTest();
    }

    @EventBusSubscriber(modid = GearInventory.MODID, value = Dist.CLIENT)
    public static final class Game {
        @SubscribeEvent
        public static void hide(RenderGuiLayerEvent.Pre event) {
            // AppleSkin draws its icons over the food bar even when the bar itself is hidden.
            if (ClientConfig.HIDE_HUD.get() && (HIDDEN.contains(event.getName()) || event.getName().getNamespace().equals("appleskin"))) event.setCanceled(true);
        }

        @SubscribeEvent
        public static void tick(ClientTickEvent.Post event) {
            Minecraft mc = Minecraft.getInstance();
            var p = mc.player;
            if (p == null || p.isCreative() || p.isSpectator()) {
                STATE.reset();
                return;
            }
            if (mc.isPaused()) return;
            boolean beat = STATE.tick(p.getHealth(), p.getMaxHealth(), ClientConfig.THRESHOLD.get().floatValue());
            if (beat && ClientConfig.HEARTBEAT.get())
                mc.getSoundManager().play(SimpleSoundInstance.forUI(ModSounds.HEARTBEAT.get(), 1, ClientConfig.HEARTBEAT_VOLUME.get().floatValue()));
        }
    }

    @EventBusSubscriber(modid = GearInventory.MODID, bus = EventBusSubscriber.Bus.MOD, value = Dist.CLIENT)
    public static final class ModBus {
        @SubscribeEvent
        public static void layers(RegisterGuiLayersEvent event) {
            event.registerAbove(VanillaGuiLayers.CAMERA_OVERLAYS, ResourceLocation.fromNamespaceAndPath(GearInventory.MODID, "vignette"), HealthEffects::render);
        }
    }
}
