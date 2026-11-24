package com.gearinventory.registry;

import com.gearinventory.GearInventory;
import com.gearinventory.menu.GearInventoryMenu;
import net.minecraft.core.registries.Registries;
import net.minecraft.world.inventory.MenuType;
import net.neoforged.neoforge.common.extensions.IMenuTypeExtension;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredRegister;

public final class ModMenus {
    public static final DeferredRegister<MenuType<?>> MENUS = DeferredRegister.create(Registries.MENU, GearInventory.MODID);
    public static final DeferredHolder<MenuType<?>, MenuType<GearInventoryMenu>> GEAR =
            MENUS.register("gear", () -> IMenuTypeExtension.create((id, inv, buf) -> new GearInventoryMenu(id, inv)));

    private ModMenus() {}
}