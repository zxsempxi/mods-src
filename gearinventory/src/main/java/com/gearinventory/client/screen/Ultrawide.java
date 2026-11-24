package com.gearinventory.client.screen;

/** 21:9 and wider. */
final class Ultrawide {
    private Ultrawide() {}

    static boolean is(int width, int height) {
        return width >= height * 2.1;
    }
}
