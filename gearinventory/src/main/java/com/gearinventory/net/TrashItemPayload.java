package com.gearinventory.net;

import com.gearinventory.GearInventory;
import com.gearinventory.menu.GearInventoryMenu;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;
import net.neoforged.neoforge.network.event.RegisterPayloadHandlersEvent;

public record TrashItemPayload(int containerId, int slot) implements CustomPacketPayload {
    public static final Type<TrashItemPayload> TYPE = new Type<>(ResourceLocation.fromNamespaceAndPath(GearInventory.MODID, "trash"));
    public static final StreamCodec<RegistryFriendlyByteBuf, TrashItemPayload> CODEC = StreamCodec.composite(
            ByteBufCodecs.VAR_INT, TrashItemPayload::containerId, ByteBufCodecs.VAR_INT, TrashItemPayload::slot, TrashItemPayload::new);

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }

    public static void register(RegisterPayloadHandlersEvent event) {
        event.registrar("1").playToServer(TYPE, CODEC, (p, ctx) -> ctx.enqueueWork(() -> {
            var open = ctx.player().containerMenu;
            if (open.containerId != p.containerId() || !open.stillValid(ctx.player()) || ctx.player().isSpectator()) return;
            if (open instanceof GearInventoryMenu menu) menu.trash(ctx.player(), p.slot());
            else com.gearinventory.menu.LootModeSlots.trash(open, ctx.player(), p.slot());
        }));
    }
}