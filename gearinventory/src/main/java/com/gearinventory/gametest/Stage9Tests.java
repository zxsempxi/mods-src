package com.gearinventory.gametest;

import com.gearinventory.GearInventory;
import com.gearinventory.storage.ArmorStorage;
import net.minecraft.core.BlockPos;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.crafting.RecipeType;
import net.minecraft.world.item.crafting.SingleRecipeInput;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.entity.FurnaceBlockEntity;
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;

import java.util.List;

@GameTestHolder(GearInventory.MODID)
@PrefixGameTestTemplate(false)
public class Stage9Tests {
    private static ItemStack rig() {
        ItemStack a = new ItemStack(Items.IRON_CHESTPLATE);
        ArmorStorage.write(a, List.of(new ItemStack(Items.BREAD, 3)));
        return a;
    }

    @GameTest(template = "empty")
    public static void filledGearIsNotSmelted(GameTestHelper h) {
        var rm = h.getLevel().getRecipeManager();
        ItemStack empty = new ItemStack(Items.IRON_CHESTPLATE);
        for (RecipeType<?> raw : List.of(RecipeType.SMELTING, RecipeType.BLASTING)) {
            @SuppressWarnings("unchecked")
            var t = (RecipeType<net.minecraft.world.item.crafting.AbstractCookingRecipe>) raw;
            h.assertTrue(rm.getRecipeFor(t, new SingleRecipeInput(rig()), h.getLevel()).isEmpty(), "filled rig smelts: " + raw);
            h.assertTrue(rm.getRecipeFor(t, new SingleRecipeInput(empty), h.getLevel()).isPresent(), "empty armor no longer smelts: " + raw);
        }
        h.succeed();
    }

    @GameTest(template = "empty", timeoutTicks = 300)
    public static void realFurnaceLeavesItAlone(GameTestHelper h) {
        BlockPos pos = new BlockPos(0, 1, 0);
        h.setBlock(pos, Blocks.FURNACE);
        FurnaceBlockEntity f = h.getBlockEntity(pos);
        f.setItem(0, rig());
        f.setItem(1, new ItemStack(Items.COAL, 4));
        h.runAfterDelay(240, () -> {
            h.assertTrue(ArmorStorage.used(f.getItem(0)) == 1 && f.getItem(2).isEmpty() && f.getItem(1).getCount() == 4, "furnace processed it");
            f.clearContent();
            h.setBlock(pos, Blocks.AIR);
            h.succeed();
        });
    }

    @GameTest(template = "empty")
    public static void droppedGearSurvivesFire(GameTestHelper h) {
        var src = h.getLevel().damageSources();
        for (var s : List.of(src.lava(), src.inFire(), src.onFire(), src.cactus(), src.explosion(null, null), src.lightningBolt())) {
            ItemEntity full = new ItemEntity(h.getLevel(), 0, 100, 0, rig());
            full.hurt(s, 1000);
            h.assertFalse(full.isRemoved(), "destroyed by " + s.getMsgId());
            ItemEntity plain = new ItemEntity(h.getLevel(), 0, 100, 0, new ItemStack(Items.IRON_CHESTPLATE));
            plain.hurt(s, 1000);
            h.assertTrue(plain.isRemoved(), "empty armor became immune to " + s.getMsgId());
        }
        ItemEntity v = new ItemEntity(h.getLevel(), 0, 100, 0, rig());
        v.hurt(src.fellOutOfWorld(), 1000);
        h.assertTrue(v.isRemoved(), "void must still remove items");
        h.succeed();
    }
}
