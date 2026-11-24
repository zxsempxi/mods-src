package com.gearinventory.compat.sophisticated;

import net.neoforged.fml.ModList;
import net.neoforged.neoforge.network.PacketDistributor;
import net.p3pp3rf1y.sophisticatedbackpacks.network.BackpackOpenPayload;

public final class SophisticatedCompat {
    private SophisticatedCompat() {}

    public static boolean loaded() {
        return ModList.get().isLoaded("sophisticatedbackpacks");
    }

    /** Same request as the mod's own B key. Call only when loaded(). */
    public static void open() {
        PacketDistributor.sendToServer(new BackpackOpenPayload());
    }
}