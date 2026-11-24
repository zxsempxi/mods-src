package com.gearinventory.gametest;

import com.mojang.authlib.GameProfile;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.level.GameType;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.common.util.FakePlayer;

import java.util.UUID;

/** A server player that never joins: Curios cannot sync to vanilla mock connections. */
final class TestPlayers {
    private TestPlayers() {}

    static ServerPlayer server(GameTestHelper h) {
        ServerPlayer p = new FakePlayer(h.getLevel(), new GameProfile(UUID.randomUUID(), "geartest"));
        p.setGameMode(GameType.SURVIVAL);
        Vec3 pos = h.absoluteVec(Vec3.ZERO);
        p.setPos(pos.x, pos.y + 2, pos.z);
        return p;
    }
}