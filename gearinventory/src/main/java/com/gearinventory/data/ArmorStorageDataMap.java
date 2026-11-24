package com.gearinventory.data;

import com.gearinventory.GearInventory;
import com.gearinventory.storage.StorageSize;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.neoforged.neoforge.registries.datamaps.DataMapType;
import net.neoforged.neoforge.registries.datamaps.RegisterDataMapTypesEvent;

/** item (or item tag) -> storage size. Items without an entry give no slots. */
public final class ArmorStorageDataMap {
    public static final DataMapType<Item, StorageSize> TYPE = DataMapType.builder(
            ResourceLocation.fromNamespaceAndPath(GearInventory.MODID, "armor_storage"), Registries.ITEM, StorageSize.CODEC)
            .synced(StorageSize.CODEC, true).build();

    private ArmorStorageDataMap() {}

    public static void register(RegisterDataMapTypesEvent event) {
        event.register(TYPE);
    }

    public static StorageSize size(ItemStack stack) {
        if (stack.isEmpty()) return StorageSize.NONE;
        StorageSize size = stack.getItemHolder().getData(TYPE);
        return size == null ? StorageSize.NONE : size;
    }
}