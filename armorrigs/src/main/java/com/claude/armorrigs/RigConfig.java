package com.claude.armorrigs;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonObject;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.world.item.ItemStack;
import net.neoforged.fml.loading.FMLPaths;

import java.io.Reader;
import java.io.Writer;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.LinkedHashMap;
import java.util.Map;

/** Item id -> {columns, rows} of the container that the worn piece provides. */
public final class RigConfig {
    private static final Map<String, int[]> SIZES = new LinkedHashMap<>();

    private RigConfig() {}

    private static void defaults() {
        SIZES.clear();
        put("leather", 2, 1, 3, 3, 6, 2, 2, 2);
        put("chainmail", 2, 2, 4, 3, 6, 3, 3, 2);
        put("iron", 2, 2, 4, 3, 6, 3, 3, 2);
        put("golden", 3, 2, 4, 4, 7, 3, 3, 3);
        put("diamond", 3, 2, 5, 4, 8, 3, 4, 3);
        put("netherite", 4, 2, 6, 4, 8, 4, 4, 4);
        SIZES.put("minecraft:turtle_helmet", new int[]{1, 2});
    }

    private static void put(String mat, int hc, int hr, int cc, int cr, int lc, int lr, int bc, int br) {
        SIZES.put("minecraft:" + mat + "_helmet", new int[]{hc, hr});
        SIZES.put("minecraft:" + mat + "_chestplate", new int[]{cc, cr});
        SIZES.put("minecraft:" + mat + "_leggings", new int[]{lc, lr});
        SIZES.put("minecraft:" + mat + "_boots", new int[]{bc, br});
    }

    public static void load() {
        defaults();
        Path path = FMLPaths.CONFIGDIR.get().resolve("armorrigs.json");
        Gson gson = new GsonBuilder().setPrettyPrinting().create();
        try {
            if (Files.exists(path)) {
                try (Reader r = Files.newBufferedReader(path, StandardCharsets.UTF_8)) {
                    JsonObject obj = gson.fromJson(r, JsonObject.class);
                    if (obj != null) {
                        SIZES.clear();
                        for (Map.Entry<String, JsonElement> e : obj.entrySet()) {
                            JsonArray a = e.getValue().getAsJsonArray();
                            int c = Math.max(1, Math.min(9, a.get(0).getAsInt()));
                            int rows = Math.max(1, Math.min(6, a.get(1).getAsInt()));
                            SIZES.put(e.getKey(), new int[]{c, rows});
                        }
                    }
                }
            } else {
                JsonObject obj = new JsonObject();
                for (Map.Entry<String, int[]> e : SIZES.entrySet()) {
                    JsonArray a = new JsonArray();
                    a.add(e.getValue()[0]);
                    a.add(e.getValue()[1]);
                    obj.add(e.getKey(), a);
                }
                Files.createDirectories(path.getParent());
                try (Writer w = Files.newBufferedWriter(path, StandardCharsets.UTF_8)) {
                    gson.toJson(obj, w);
                }
            }
        } catch (Exception ex) {
            defaults();
        }
    }

    /** Returns {columns, rows} or null if this item is not a rig. */
    public static int[] sizeFor(ItemStack stack) {
        if (stack == null || stack.isEmpty()) return null;
        return SIZES.get(BuiltInRegistries.ITEM.getKey(stack.getItem()).toString());
    }
}
