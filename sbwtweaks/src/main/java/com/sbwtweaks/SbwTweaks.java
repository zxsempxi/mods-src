package com.sbwtweaks;

import com.mojang.serialization.MapCodec;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.item.CreativeModeTab;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.storage.loot.LootContext;
import net.minecraft.world.level.storage.loot.LootTable;
import net.minecraft.world.level.storage.loot.parameters.LootContextParamSets;
import net.minecraft.world.level.storage.loot.predicates.LootItemCondition;
import net.minecraft.world.level.storage.loot.predicates.LootItemConditionType;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.fml.common.Mod;
import net.neoforged.neoforge.common.NeoForge;
import net.neoforged.neoforge.event.BuildCreativeModeTabContentsEvent;
import net.neoforged.neoforge.event.entity.EntityJoinLevelEvent;
import net.neoforged.neoforge.registries.DeferredRegister;

import java.util.ArrayList;
import java.util.Set;
import java.util.function.Supplier;

@Mod("sbwtweaks")
public class SbwTweaks {
    /** Everything that drives, flies or floats. Fixed guns, mortar and drones stay. */
    private static final Set<String> CUT = Set.of("ac_130h", "ah_6", "air_sheep", "a_10a", "bmp_2", "bradley", "fh_77bw", "happiest_ghast", "ju_87", "j_16", "kirov", "kv_16", "lav_150", "lav_25", "lav_ad", "mi_28", "m_1a_2", "plz_05", "prism_tank", "sodayo_pick_up", "sodayo_pick_up_hmg", "sodayo_pick_up_rocket", "sodayo_pick_up_tow", "speedboat", "tiny_speedboat", "tom_6", "truck", "t_90a", "wheel_chair", "yx_100", "ztz_99a");

    private static final DeferredRegister<LootItemConditionType> CONDITIONS = DeferredRegister.create(Registries.LOOT_CONDITION_TYPE, "sbwtweaks");
    private static final Supplier<LootItemConditionType> ANY_CHEST =
            CONDITIONS.register("any_chest", () -> new LootItemConditionType(MapCodec.unit(AnyChest.INSTANCE)));

    public SbwTweaks(IEventBus bus) {
        CONDITIONS.register(bus);
        bus.addListener(SbwTweaks::tab);
        NeoForge.EVENT_BUS.addListener(SbwTweaks::join);
    }

    private static boolean cut(EntityType<?> type) {
        ResourceLocation id = BuiltInRegistries.ENTITY_TYPE.getKey(type);
        return id.getNamespace().equals("superbwarfare") && CUT.contains(id.getPath());
    }

    private static void join(EntityJoinLevelEvent event) {
        if (!event.getLevel().isClientSide && cut(event.getEntity().getType())) event.setCanceled(true);
    }

    /** Creative vehicle containers keep their entity id in a component. */
    private static void tab(BuildCreativeModeTabContentsEvent event) {
        for (ItemStack stack : new ArrayList<>(event.getParentEntries())) {
            ResourceLocation id = BuiltInRegistries.ITEM.getKey(stack.getItem());
            if (!id.getNamespace().equals("superbwarfare") || !id.getPath().contains("container")) continue;
            String data = stack.getComponentsPatch().toString();
            for (String name : CUT) {
                if (data.contains("superbwarfare:" + name + "\"")) {
                    event.remove(stack, CreativeModeTab.TabVisibility.PARENT_AND_SEARCH_TABS);
                    break;
                }
            }
        }
    }

    /** True for any chest loot table of any mod, except those Superb Warfare fills on its own. */
    public enum AnyChest implements LootItemCondition {
        INSTANCE;

        private static final Set<String> COVERED = Set.of("simple_dungeon", "abandoned_mineshaft", "shipwreck_map", "shipwreck_supply",
                "shipwreck_treasure", "ruined_portal", "pillager_outpost", "stronghold_library", "woodland_mansion", "end_city_treasure",
                "ancient_city", "ancient_city_ice_box", "bastion_bridge", "bastion_hoglin_stable", "bastion_other", "buried_treasure",
                "desert_pyramid", "igloo", "jungle_temple");

        @Override
        public LootItemConditionType getType() {
            return ANY_CHEST.get();
        }

        @Override
        public boolean test(LootContext ctx) {
            ResourceLocation id = ctx.getQueriedLootTableId();
            if (id == null || id.getNamespace().equals("superbwarfare") || id.getNamespace().equals("sbwtweaks")) return false;
            if (id.getNamespace().equals("minecraft") && id.getPath().startsWith("chests/") && COVERED.contains(id.getPath().substring(7))) return false;
            if (id.getPath().startsWith("blocks/") || id.getPath().startsWith("entities/")) return false;
            if (id.getPath().contains("chests/")) return true;
            return ctx.getResolver().get(Registries.LOOT_TABLE, ResourceKey.create(Registries.LOOT_TABLE, id))
                    .map(h -> h.value().getParamSet() == LootContextParamSets.CHEST).orElse(false);
        }
    }
}