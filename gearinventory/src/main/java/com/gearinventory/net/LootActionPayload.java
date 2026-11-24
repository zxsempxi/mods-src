package com.gearinventory.net;

import com.gearinventory.GearInventory;
import com.gearinventory.menu.LootModeSlots;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.player.Player;
import net.neoforged.neoforge.network.event.RegisterPayloadHandlersEvent;

/** Take all / deposit all / sort, merged into one packet with an action id. */
public record LootActionPayload(int containerId, int action) implements CustomPacketPayload {
    public static final int TAKE_ALL = 0, DEPOSIT_ALL = 1, SORT = 2;
    public static final Type<LootActionPayload> TYPE = new Type<>(ResourceLocation.fromNamespaceAndPath(GearInventory.MODID, "loot"));
    public static final StreamCodec<RegistryFriendlyByteBuf, LootActionPayload> CODEC = StreamCodec.composite(
            ByteBufCodecs.VAR_INT, LootActionPayload::containerId, ByteBufCodecs.VAR_INT, LootActionPayload::action, LootActionPayload::new);

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }

    public static void register(RegisterPayloadHandlersEvent event) {
        event.registrar("1").playToServer(TYPE, CODEC, (p, ctx) -> ctx.enqueueWork(() -> handle(ctx.player(), p)));
    }

    public static boolean handle(Player player, LootActionPayload p) {
        var menu = player.containerMenu;
        if (menu.containerId != p.containerId() || LootModeSlots.layout(menu) == null || player.isSpectator()
                || !menu.stillValid(player) || !menu.getCarried().isEmpty()) return false;
        switch (p.action()) {
            case TAKE_ALL -> LootModeSlots.takeAll(menu, player);
            case DEPOSIT_ALL -> LootModeSlots.depositAll(menu);
            case SORT -> LootModeSlots.sort(menu);
            default -> {
                return false;
            }
        }
        menu.broadcastChanges();
        return true;
    }
}
