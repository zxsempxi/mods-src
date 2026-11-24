package com.gearinventory.net;

import com.gearinventory.GearInventory;
import com.gearinventory.menu.GearInventoryMenu;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.chat.Component;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.SimpleMenuProvider;
import net.neoforged.neoforge.network.event.RegisterPayloadHandlersEvent;

/** Client asks to open the gear menu; the server decides. */
public record OpenGearInventoryPayload() implements CustomPacketPayload {
    public static final Type<OpenGearInventoryPayload> TYPE =
            new Type<>(ResourceLocation.fromNamespaceAndPath(GearInventory.MODID, "open"));
    public static final StreamCodec<RegistryFriendlyByteBuf, OpenGearInventoryPayload> CODEC =
            StreamCodec.unit(new OpenGearInventoryPayload());

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }

    public static void register(RegisterPayloadHandlersEvent event) {
        event.registrar("1").playToServer(TYPE, CODEC, (payload, ctx) -> ctx.enqueueWork(() -> {
            if (ctx.player() instanceof ServerPlayer player) open(player);
        }));
    }

    public static boolean open(ServerPlayer player) {
        if (!player.isAlive() || player.isCreative() || player.isSpectator()) return false;
        if (player.containerMenu != player.inventoryMenu) return false;
        return player.openMenu(new SimpleMenuProvider((id, inv, p) -> new GearInventoryMenu(id, inv),
                Component.translatable("screen.gearinventory.title"))).isPresent();
    }
}