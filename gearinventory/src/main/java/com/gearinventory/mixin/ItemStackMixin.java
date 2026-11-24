package com.gearinventory.mixin;

import com.gearinventory.storage.GearGuards;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.neoforged.neoforge.common.extensions.IItemStackExtension;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import java.util.function.Consumer;

/**
 * Nesting: bundles, shulker slots, hoppers and modded storage all ask canFitInsideContainerItems, and it has no event.
 * Breaking: the stack still has its contents only before shrink; PlayerDestroyItemEvent does not cover worn armor.
 */
@Mixin(ItemStack.class)
public abstract class ItemStackMixin implements IItemStackExtension {
    @Override
    public boolean canFitInsideContainerItems() {
        ItemStack self = (ItemStack) (Object) this;
        return !GearGuards.filled(self) && self.getItem().canFitInsideContainerItems(self);
    }

    @Inject(method = "hurtAndBreak(ILnet/minecraft/server/level/ServerLevel;Lnet/minecraft/world/entity/LivingEntity;Ljava/util/function/Consumer;)V",
            at = @At(value = "INVOKE", target = "Lnet/minecraft/world/item/ItemStack;shrink(I)V"))
    private void gearinventory$spill(int amount, ServerLevel level, LivingEntity entity, Consumer<Item> onBroken, CallbackInfo ci) {
        GearGuards.spill((ItemStack) (Object) this, level, entity);
    }
}
