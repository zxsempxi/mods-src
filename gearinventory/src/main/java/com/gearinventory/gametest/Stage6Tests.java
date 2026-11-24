package com.gearinventory.gametest;

import com.gearinventory.GearInventory;
import com.gearinventory.health.LowHealth;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;

@GameTestHolder(GearInventory.MODID)
@PrefixGameTestTemplate(false)
public class Stage6Tests {
    private static int beats(LowHealth s, float hp, int ticks) {
        int n = 0;
        for (int i = 0; i < ticks; i++) if (s.tick(hp, 20, 0.3F)) n++;
        return n;
    }

    @GameTest(template = "empty")
    public static void hysteresis(GameTestHelper h) {
        LowHealth s = new LowHealth();
        beats(s, 7, 5);
        h.assertFalse(s.active(), "on above threshold");
        beats(s, 6, 1);
        h.assertTrue(s.active(), "off at threshold");
        for (int i = 0; i < 40; i++) {
            s.tick(i % 2 == 0 ? 6.0F : 6.9F, 20, 0.3F);
            h.assertTrue(s.active(), "flickers at the border");
        }
        beats(s, 7.1F, 1);
        h.assertFalse(s.active(), "still on after recovery");
        h.succeed();
    }

    @GameTest(template = "empty")
    public static void rhythm(GameTestHelper h) {
        LowHealth a = new LowHealth(), b = new LowHealth();
        beats(a, 6, 20);
        beats(b, 1, 20);
        int slow = beats(a, 6, 400), fast = beats(b, 1, 400);
        h.assertTrue(slow == 20, "threshold beats in 20s: " + slow);
        h.assertTrue(fast == 50, "1 hp beats in 20s: " + fast);
        h.assertTrue(beats(new LowHealth(), 20, 400) == 0, "beats at full health");
        h.succeed();
    }

    @GameTest(template = "empty")
    public static void vignetteFadesAndFlashes(GameTestHelper h) {
        LowHealth s = new LowHealth();
        beats(s, 20, 10);
        h.assertTrue(s.alpha(0) == 0, "visible at full health");
        s.tick(17, 20, 0.3F);
        h.assertTrue(s.alpha(0) > 0.1F, "no damage flash");
        beats(s, 17, 20);
        h.assertTrue(s.alpha(0) == 0, "flash never ends");
        beats(s, 5, 60);
        float mid = s.alpha(0);
        beats(s, 2, 60);
        h.assertTrue(s.alpha(0) > mid * 0.9F && mid > 0.2F, "does not grow");
        float before = s.alpha(0);
        s.tick(20, 20, 0.3F);
        h.assertTrue(s.alpha(0) > before - 0.3F, "cuts off instead of fading");
        beats(s, 20, 60);
        h.assertTrue(s.alpha(0) == 0, "does not fade out");
        beats(s, 2, 5);
        s.tick(0, 20, 0.3F);
        h.assertFalse(s.active(), "active when dead");
        h.succeed();
    }
}
