package com.gearinventory.storage;

import com.gearinventory.config.ServerConfig;
import com.gearinventory.data.ArmorStorageDataMap;
import net.minecraft.core.component.DataComponents;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.component.ItemContainerContents;
import net.neoforged.neoforge.event.AnvilUpdateEvent;
import net.neoforged.neoforge.event.GrindstoneEvent;

/** Rules that keep the contents of gear from being nested, merged away or destroyed with the piece. */
public final class GearGuards {
    private GearGuards() {}

    /** Storage gear with something inside. */
    public static boolean filled(ItemStack stack) {
        return !stack.isEmpty() && ArmorStorage.hasContents(stack) && ArmorStorageDataMap.size(stack).slots() > 0;
    }

    public static void spill(ItemStack armor, ServerLevel level, LivingEntity entity) {
        if (entity == null || armor.getCount() > 1 || !filled(armor) || !ServerConfig.spillOnBreak()) return;
        ItemContainerContents contents = armor.get(DataComponents.CONTAINER);
        armor.remove(DataComponents.CONTAINER);
        for (ItemStack stack : contents.nonEmptyItemsCopy()) {
            ItemEntity drop = new ItemEntity(level, entity.getX(), entity.getY() + 0.5, entity.getZ(), stack);
            drop.setDefaultPickUpDelay();
            level.addFreshEntity(drop);
        }
    }

    /** Crafting and smithing keep components; everything else would consume the piece with its contents. */
    public static boolean blocksProcessing(net.minecraft.world.item.crafting.RecipeInput input) {
        if (input instanceof net.minecraft.world.item.crafting.CraftingInput
                || input instanceof net.minecraft.world.item.crafting.SmithingRecipeInput) return false;
        for (int i = 0; i < input.size(); i++) if (filled(input.getItem(i))) return true;
        return false;
    }

    /** Dropped filled gear survives fire, lava, cactus and explosions, but not the void. */
    public static void onInvulnerable(net.neoforged.neoforge.event.entity.EntityInvulnerabilityCheckEvent event) {
        if (event.getEntity() instanceof ItemEntity e
                && !event.getSource().is(net.minecraft.tags.DamageTypeTags.BYPASSES_INVULNERABILITY)
                && filled(e.getItem())) event.setInvulnerable(true);
    }

    public static void onAnvil(AnvilUpdateEvent event) {
        if (filled(event.getRight())) event.setCanceled(true);
    }

    public static void onGrindstone(GrindstoneEvent.OnPlaceItem event) {
        boolean two = !event.getTopItem().isEmpty() && !event.getBottomItem().isEmpty();
        if (two && (filled(event.getTopItem()) || filled(event.getBottomItem()))) event.setCanceled(true);
    }
}
