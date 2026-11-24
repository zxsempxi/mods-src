package com.gearinventory.config;

import net.neoforged.neoforge.common.ModConfigSpec;

public final class ClientConfig {
    public static final ModConfigSpec SPEC;
    public static final ModConfigSpec.BooleanValue HIDE_BACKPACK_IN_FLIGHT, HIDE_HUD, VIGNETTE, HEARTBEAT;
    public static final ModConfigSpec.DoubleValue THRESHOLD, HEARTBEAT_VOLUME;
    public static final ModConfigSpec.BooleanValue ALWAYS_TABS;
    public static final ModConfigSpec.IntValue OFFSET_X, OFFSET_Y;

    static {
        ModConfigSpec.Builder b = new ModConfigSpec.Builder();
        HIDE_BACKPACK_IN_FLIGHT = b.comment("Do not render the backpack while gliding").define("hideBackpackInFlight", true);
        HIDE_HUD = b.comment("Hide vanilla health, food, armor, experience and air bars").define("hideHud", true);
        VIGNETTE = b.comment("Red screen edges at low health and on damage").define("vignette", true);
        THRESHOLD = b.comment("Share of max health that counts as low").defineInRange("lowHealthThreshold", 0.3, 0.05, 0.9);
        HEARTBEAT = b.define("heartbeat", true);
        HEARTBEAT_VOLUME = b.defineInRange("heartbeatVolume", 0.8, 0.0, 1.0);
        ALWAYS_TABS = b.comment("Loot screen: use tabs even on wide screens").define("alwaysTabs", false);
        OFFSET_X = b.comment("Move both screens, in GUI pixels").defineInRange("screenOffsetX", 0, -200, 200);
        OFFSET_Y = b.defineInRange("screenOffsetY", 0, -120, 120);
        SPEC = b.build();
    }

    private ClientConfig() {}
}