package com.gearinventory;

import com.gearinventory.net.OpenGearInventoryPayload;
import com.gearinventory.registry.ModMenus;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.fml.common.Mod;

@Mod(GearInventory.MODID)
public class GearInventory {
    public static final String MODID = "gearinventory";

    public GearInventory(IEventBus modBus, net.neoforged.fml.ModContainer container) {
        container.registerConfig(net.neoforged.fml.config.ModConfig.Type.SERVER, com.gearinventory.config.ServerConfig.SPEC);
        container.registerConfig(net.neoforged.fml.config.ModConfig.Type.CLIENT, com.gearinventory.config.ClientConfig.SPEC);
        var bus = net.neoforged.neoforge.common.NeoForge.EVENT_BUS;
        bus.addListener(net.neoforged.bus.api.EventPriority.LOWEST, com.gearinventory.storage.PickupOverflow::onPickup);
        bus.addListener(com.gearinventory.storage.GearGuards::onAnvil);
        bus.addListener(com.gearinventory.storage.GearGuards::onInvulnerable);
        bus.addListener(com.gearinventory.storage.GearGuards::onGrindstone);
        bus.addListener(com.gearinventory.compat.curios.ElytraUse::onUse);
        bus.addListener(com.gearinventory.compat.curios.CuriosCompat::onCanEquip);
        ModMenus.MENUS.register(modBus);
        modBus.addListener(com.gearinventory.data.ArmorStorageDataMap::register);
        com.gearinventory.trash.TrashAttachment.TYPES.register(modBus);
        modBus.addListener(com.gearinventory.net.TrashItemPayload::register);
        modBus.addListener(com.gearinventory.net.LootActionPayload::register);
        com.gearinventory.registry.ModSounds.SOUNDS.register(modBus);
        modBus.addListener(OpenGearInventoryPayload::register);
    }
}