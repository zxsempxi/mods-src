package com.gearinventory.storage;

import com.gearinventory.data.ArmorStorageDataMap;
import net.minecraft.core.NonNullList;
import net.minecraft.core.component.DataComponents;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.component.BundleContents;
import net.minecraft.world.item.component.ItemContainerContents;

import java.util.ArrayList;
import java.util.List;

/**
 * Contents live in the vanilla minecraft:container component. Armor never uses it otherwise, vanilla already saves,
 * syncs and compares it, and gear filled by the older Armor Rigs mod is read without a conversion step.
 */
public final class ArmorStorage {
    private ArmorStorage() {}

    public static NonNullList<ItemStack> read(ItemStack armor, int slots) {
        NonNullList<ItemStack> list = NonNullList.withSize(slots, ItemStack.EMPTY);
        armor.getOrDefault(DataComponents.CONTAINER, ItemContainerContents.EMPTY).copyInto(list);
        return list;
    }

    public static void write(ItemStack armor, List<ItemStack> items) {
        if (items.stream().allMatch(ItemStack::isEmpty)) armor.remove(DataComponents.CONTAINER);
        else armor.set(DataComponents.CONTAINER, ItemContainerContents.fromItems(items));
    }

    public static boolean hasContents(ItemStack stack) {
        ItemContainerContents c = stack.get(DataComponents.CONTAINER);
        return c != null && c.nonEmptyItems().iterator().hasNext();
    }

    /** No containers with items inside containers. */
    public static boolean mayStore(ItemStack stack) {
        if (stack.isEmpty() || hasContents(stack)) return false;
        BundleContents bundle = stack.get(DataComponents.BUNDLE_CONTENTS);
        return (bundle == null || bundle.isEmpty()) && stack.getItem().canFitInsideContainerItems();
    }

    public static int used(ItemStack armor) {
        ItemContainerContents c = armor.get(DataComponents.CONTAINER);
        if (c == null) return 0;
        int n = 0;
        for (ItemStack ignored : c.nonEmptyItems()) n++;
        return n;
    }

    /** Items that no longer fit the configured size: compacts what fits, removes and returns the rest. */
    public static List<ItemStack> takeOverflow(ItemStack armor) {
        int slots = ArmorStorageDataMap.size(armor).slots();
        ItemContainerContents c = armor.get(DataComponents.CONTAINER);
        if (slots == 0 || c == null) return List.of();
        List<ItemStack> all = new ArrayList<>(c.stream().map(ItemStack::copy).toList());
        List<ItemStack> extra = new ArrayList<>();
        for (int i = slots; i < all.size(); i++) if (!all.get(i).isEmpty()) extra.add(all.get(i));
        if (extra.isEmpty()) return List.of();
        NonNullList<ItemStack> kept = read(armor, slots);
        for (int i = 0; i < slots && !extra.isEmpty(); i++) if (kept.get(i).isEmpty()) kept.set(i, extra.remove(0));
        write(armor, kept);
        return extra;
    }
}