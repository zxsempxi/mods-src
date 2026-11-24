package com.gearinventory.client;

import com.gearinventory.GearInventory;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.fml.ModContainer;
import net.neoforged.fml.common.Mod;
import net.neoforged.neoforge.client.gui.ConfigurationScreen;
import net.neoforged.neoforge.client.gui.IConfigScreenFactory;

/** Adds the settings button in the mod list. */
@Mod(value = GearInventory.MODID, dist = Dist.CLIENT)
public class ConfigScreen {
    public ConfigScreen(ModContainer container) {
        container.registerExtensionPoint(IConfigScreenFactory.class, ConfigurationScreen::new);
    }
}