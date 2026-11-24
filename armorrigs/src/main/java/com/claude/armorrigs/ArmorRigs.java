package com.claude.armorrigs;

import net.minecraft.core.registries.Registries;
import net.minecraft.network.chat.Component;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.SimpleMenuProvider;
import net.minecraft.world.inventory.MenuType;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.fml.common.Mod;
import net.neoforged.neoforge.common.NeoForge;
import net.neoforged.neoforge.common.extensions.IMenuTypeExtension;
import net.neoforged.neoforge.event.entity.player.ItemTooltipEvent;
import net.neoforged.neoforge.network.event.RegisterPayloadHandlersEvent;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredRegister;

@Mod(ArmorRigs.MODID)
public class ArmorRigs {
    public static final String MODID = "armorrigs";

    public static final DeferredRegister<MenuType<?>> MENUS = DeferredRegister.create(Registries.MENU, MODID);
    public static final DeferredHolder<MenuType<?>, MenuType<RigMenu>> RIG_MENU =
            MENUS.register("rig", () -> IMenuTypeExtension.create((id, inv, buf) -> new RigMenu(id, inv)));

    public ArmorRigs(IEventBus modBus) {
        RigConfig.load();
        MENUS.register(modBus);
        if (Boolean.getBoolean("armorrigs.selftest")) SelfTest.register();
        modBus.addListener(this::registerPayloads);
        NeoForge.EVENT_BUS.addListener(this::onTooltip);
    }

    private void registerPayloads(RegisterPayloadHandlersEvent event) {
        event.registrar("1").playToServer(OpenRigPayload.TYPE, OpenRigPayload.STREAM_CODEC, (payload, ctx) ->
                ctx.enqueueWork(() -> {
                    if (ctx.player() instanceof ServerPlayer sp) {
                        sp.openMenu(new SimpleMenuProvider((id, inv, pl) -> new RigMenu(id, inv),
                                Component.translatable("screen.armorrigs.title")));
                    }
                }));
    }

    private void onTooltip(ItemTooltipEvent event) {
        int[] size = RigConfig.sizeFor(event.getItemStack());
        if (size != null) {
            event.getToolTip().add(Component.translatable("tooltip.armorrigs.size", size[0], size[1])
                    .withStyle(net.minecraft.ChatFormatting.DARK_GRAY));
        }
    }

    public record OpenRigPayload() implements CustomPacketPayload {
        public static final Type<OpenRigPayload> TYPE =
                new Type<>(ResourceLocation.fromNamespaceAndPath(MODID, "open_rig"));
        public static final StreamCodec<RegistryFriendlyByteBuf, OpenRigPayload> STREAM_CODEC =
                StreamCodec.unit(new OpenRigPayload());

        @Override
        public Type<? extends CustomPacketPayload> type() {
            return TYPE;
        }
    }
}
