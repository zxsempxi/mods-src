package com.gearinventory.trash;

import com.gearinventory.GearInventory;
import net.minecraft.world.item.ItemStack;
import net.neoforged.neoforge.attachment.AttachmentType;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredRegister;
import net.neoforged.neoforge.registries.NeoForgeRegistries;

/** One saved stack per player. Deliberately not copied on death. */
public final class TrashAttachment {
    public static final DeferredRegister<AttachmentType<?>> TYPES = DeferredRegister.create(NeoForgeRegistries.ATTACHMENT_TYPES, GearInventory.MODID);
    public static final DeferredHolder<AttachmentType<?>, AttachmentType<ItemStack>> TRASH = TYPES.register("trash",
            () -> AttachmentType.builder(() -> ItemStack.EMPTY).serialize(ItemStack.OPTIONAL_CODEC, s -> !s.isEmpty()).build());

    private TrashAttachment() {}
}