package com.gearinventory.config;

import net.neoforged.neoforge.common.ModConfigSpec;

public final class ServerConfig {
    public enum TransferOrder { STORAGE_FIRST, INVENTORY_FIRST }

    private static final ModConfigSpec.EnumValue<TransferOrder> PICKUP_ORDER;
    private static final ModConfigSpec.ConfigValue<java.util.List<? extends String>> LOOT_MENUS;

    public static final ModConfigSpec SPEC;
    public static final ModConfigSpec.EnumValue<TransferOrder> SHIFT_CLICK;
    private static final ModConfigSpec.BooleanValue EXCLUSIVE, STORE_PICKUPS, SPILL_ON_BREAK;

    static {
        ModConfigSpec.Builder b = new ModConfigSpec.Builder();
        SHIFT_CLICK = b.comment("Where shift-click from the inventory goes first").defineEnum("shiftClickOrder", TransferOrder.STORAGE_FIRST);
        EXCLUSIVE = b.comment("Backpack and elytra cannot be worn together").define("backpackElytraExclusive", false);
        STORE_PICKUPS = b.comment("Picked up items that do not fit the inventory go to armor storage").define("pickupOverflowToStorage", true);
        SPILL_ON_BREAK = b.comment("Drop the contents when a worn piece breaks; off = contents are lost").define("dropContentsOnBreak", true);
        PICKUP_ORDER = b.comment("Where picked up items go first").defineEnum("pickupOrder", TransferOrder.INVENTORY_FIRST);
        LOOT_MENUS = b.comment("Experimental: class names of modded menus that extend a vanilla chest/shulker/hopper/dispenser menu").defineListAllowEmpty("lootMenuWhitelist", java.util.List.of(), () -> "", o -> o instanceof String);
        SPEC = b.build();
    }

    private ServerConfig() {}

    public static boolean exclusive() {
        return SPEC.isLoaded() && EXCLUSIVE.get();
    }

    public static boolean pickupToStorageFirst() {
        return SPEC.isLoaded() && PICKUP_ORDER.get() == TransferOrder.STORAGE_FIRST;
    }

    public static boolean lootMenuAllowed(String className) {
        return SPEC.isLoaded() && LOOT_MENUS.get().contains(className);
    }

    public static boolean spillOnBreak() {
        return !SPEC.isLoaded() || SPILL_ON_BREAK.get();
    }

    public static boolean storePickups() {
        return !SPEC.isLoaded() || STORE_PICKUPS.get();
    }

    public static TransferOrder shiftClick() {
        return SPEC.isLoaded() ? SHIFT_CLICK.get() : TransferOrder.STORAGE_FIRST;
    }
}