package com.gearinventory.compat.curios;

import com.gearinventory.GearInventory;
import com.gearinventory.config.ServerConfig;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.tags.TagKey;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.neoforged.neoforge.common.util.TriState;
import top.theillusivec4.curios.api.CuriosApi;
import top.theillusivec4.curios.api.SlotContext;
import top.theillusivec4.curios.api.event.CurioCanEquipEvent;
import top.theillusivec4.curios.api.type.inventory.IDynamicStackHandler;

public final class CuriosCompat {
    public static final String BACK = "back", ELYTRA = "elytra";
    public static final TagKey<Item> ELYTRA_LIKE = TagKey.create(Registries.ITEM, ResourceLocation.fromNamespaceAndPath(GearInventory.MODID, "elytra_like"));
    public static final TagKey<Item> BACK_ITEMS = TagKey.create(Registries.ITEM, ResourceLocation.fromNamespaceAndPath("curios", "back"));

    private CuriosCompat() {}

    public static IDynamicStackHandler handler(LivingEntity e, String id) {
        return CuriosApi.getCuriosInventory(e).flatMap(c -> c.getStacksHandler(id)).map(h -> h.getStacks())
                .filter(s -> s.getSlots() > 0).orElse(null);
    }

    public static ItemStack get(LivingEntity e, String id) {
        IDynamicStackHandler h = handler(e, id);
        return h == null ? ItemStack.EMPTY : h.getStackInSlot(0);
    }

    public static void set(LivingEntity e, String id, ItemStack stack) {
        IDynamicStackHandler h = handler(e, id);
        if (h != null) h.setStackInSlot(0, stack);
    }

    /** What flight and rendering should treat as the worn elytra. */
    public static ItemStack flightItem(LivingEntity e, ItemStack chest) {
        if (chest.canElytraFly(e)) return chest;
        ItemStack worn = get(e, ELYTRA);
        return worn.canElytraFly(e) ? worn : chest;
    }

    public static boolean excluded(LivingEntity e, String id) {
        if (!ServerConfig.exclusive()) return false;
        return id.equals(ELYTRA) && !get(e, BACK).isEmpty() || id.equals(BACK) && !get(e, ELYTRA).isEmpty();
    }

    public static boolean canEquip(Player p, String id, ItemStack stack) {
        SlotContext ctx = new SlotContext(id, p, 0, false, true);
        return handler(p, id) != null && !excluded(p, id) && CuriosApi.isStackValid(ctx, stack)
                && CuriosApi.getCurio(stack).map(c -> c.canEquip(ctx)).orElse(true);
    }

    public static boolean canUnequip(Player p, String id, ItemStack stack) {
        SlotContext ctx = new SlotContext(id, p, 0, false, true);
        return CuriosApi.getCurio(stack).map(c -> c.canUnequip(ctx)).orElse(true);
    }

    /** Covers Curios' own screen and other mods; our menu asks canEquip directly. */
    public static void onCanEquip(CurioCanEquipEvent event) {
        if (excluded(event.getEntity(), event.getSlotContext().identifier())) event.setEquipResult(TriState.FALSE);
    }
}