package com.gearinventory.client;

import com.gearinventory.GearInventory;
import com.gearinventory.client.screen.GearInventoryScreen;
import com.gearinventory.net.OpenGearInventoryPayload;
import com.gearinventory.registry.ModMenus;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.screens.inventory.InventoryScreen;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.RegisterMenuScreensEvent;
import net.neoforged.neoforge.client.event.ScreenEvent;
import net.neoforged.neoforge.network.PacketDistributor;

@EventBusSubscriber(modid = GearInventory.MODID, value = Dist.CLIENT)
public final class ClientEvents {
    private ClientEvents() {}

    @SubscribeEvent
    public static void opening(ScreenEvent.Opening event) {
        var player = Minecraft.getInstance().player;
        if (player != null && !player.isSpectator() && event.getNewScreen() instanceof net.minecraft.client.gui.screens.inventory.AbstractContainerScreen<?> vanilla
                && vanilla.getClass().getName().startsWith("net.minecraft.") && com.gearinventory.menu.LootModeSlots.layout(vanilla.getMenu()) != null) {
            var window = Minecraft.getInstance().getWindow();
            // Too small even for tabs: keep the vanilla screen, the extra slots stay off-screen.
            if (window.getGuiScaledWidth() >= com.gearinventory.client.screen.LootScreen.MIN_WIDTH && window.getGuiScaledHeight() >= com.gearinventory.client.screen.LootScreen.MIN_HEIGHT)
                event.setNewScreen(new com.gearinventory.client.screen.LootScreen(vanilla.getMenu(), player.getInventory(), vanilla.getTitle()));
            return;
        }
        if (!(event.getNewScreen() instanceof InventoryScreen) || player == null) return;
        if (player.isCreative() || player.isSpectator()) return;
        event.setCanceled(true);
        PacketDistributor.sendToServer(new OpenGearInventoryPayload());
    }

    @SubscribeEvent
    public static void tooltip(net.neoforged.neoforge.event.entity.player.ItemTooltipEvent event) {
        var size = com.gearinventory.data.ArmorStorageDataMap.size(event.getItemStack());
        if (size.slots() == 0) return;
        event.getToolTip().add(net.minecraft.network.chat.Component.translatable("tooltip.gearinventory.capacity", size.width(), size.height(),
                com.gearinventory.storage.ArmorStorage.used(event.getItemStack()), size.slots()).withStyle(net.minecraft.ChatFormatting.GRAY));
    }

    @EventBusSubscriber(modid = GearInventory.MODID, bus = EventBusSubscriber.Bus.MOD, value = Dist.CLIENT)
    public static final class ModBus {
        @SubscribeEvent
        public static void screens(RegisterMenuScreensEvent event) {
            event.register(ModMenus.GEAR.get(), GearInventoryScreen::new);
        }
    }
}