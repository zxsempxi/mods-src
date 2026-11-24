package com.claude.armorrigs.client;

import com.claude.armorrigs.ArmorRigs;
import com.mojang.blaze3d.platform.InputConstants;
import net.minecraft.client.KeyMapping;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.screens.inventory.InventoryScreen;
import net.minecraft.network.chat.Component;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.ClientTickEvent;
import net.neoforged.neoforge.client.event.RegisterKeyMappingsEvent;
import net.neoforged.neoforge.client.event.RegisterMenuScreensEvent;
import net.neoforged.neoforge.client.event.ScreenEvent;
import net.neoforged.neoforge.network.PacketDistributor;
import org.lwjgl.glfw.GLFW;

public final class ClientSetup {
    public static final KeyMapping OPEN = new KeyMapping("key.armorrigs.open",
            InputConstants.Type.KEYSYM, GLFW.GLFW_KEY_Y, "key.categories.armorrigs");

    private ClientSetup() {}

    static void open() {
        PacketDistributor.sendToServer(new ArmorRigs.OpenRigPayload());
    }

    @EventBusSubscriber(modid = ArmorRigs.MODID, bus = EventBusSubscriber.Bus.MOD, value = Dist.CLIENT)
    public static final class ModEvents {
        @SubscribeEvent
        public static void screens(RegisterMenuScreensEvent event) {
            event.register(ArmorRigs.RIG_MENU.get(), RigScreen::new);
        }

        @SubscribeEvent
        public static void keys(RegisterKeyMappingsEvent event) {
            event.register(OPEN);
        }
    }

    @EventBusSubscriber(modid = ArmorRigs.MODID, value = Dist.CLIENT)
    public static final class GameEvents {
        @SubscribeEvent
        public static void tick(ClientTickEvent.Post event) {
            Minecraft mc = Minecraft.getInstance();
            while (OPEN.consumeClick()) {
                if (mc.player != null && mc.screen == null) open();
            }
        }

        @SubscribeEvent
        public static void initScreen(ScreenEvent.Init.Post event) {
            if (event.getScreen() instanceof InventoryScreen inv) {
                event.addListener(Button.builder(Component.translatable("button.armorrigs.open"), b -> open())
                        .bounds(inv.getGuiLeft(), inv.getGuiTop() - 20, 76, 18)
                        .build());
            }
        }
    }
}
