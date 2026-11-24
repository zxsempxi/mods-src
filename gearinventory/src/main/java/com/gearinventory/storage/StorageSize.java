package com.gearinventory.storage;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;

public record StorageSize(int width, int height) {
    public static final int MAX_WIDTH = 9, MAX_HEIGHT = 6, MAX_SLOTS = MAX_WIDTH * MAX_HEIGHT;
    public static final StorageSize NONE = new StorageSize(0, 0);
    public static final Codec<StorageSize> CODEC = RecordCodecBuilder.create(i -> i.group(
            Codec.intRange(0, MAX_WIDTH).fieldOf("width").forGetter(StorageSize::width),
            Codec.intRange(0, MAX_HEIGHT).fieldOf("height").forGetter(StorageSize::height)
    ).apply(i, StorageSize::new));

    public int slots() {
        return width * height;
    }
}