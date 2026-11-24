package com.gearinventory.health;

/** Pure logic for the low-health effects, free of rendering so it can be tested and ported. Call tick() 20 times a second. */
public final class LowHealth {
    public static final float HYSTERESIS = 1.0F;
    private static final float SLOW = 1.0F, FAST = 0.4F, FADE = 0.05F, FLASH_FADE = 0.08F;
    private boolean active;
    private float intensity, flash, sinceBeat, lastHealth = -1;

    /** Returns true when a heartbeat should sound this tick. */
    public boolean tick(float health, float max, float threshold) {
        if (lastHealth >= 0 && health < lastHealth - 0.01F) flash = Math.min(1, flash + 0.5F);
        lastHealth = health;
        flash = Math.max(0, flash - FLASH_FADE);
        float limit = max * threshold;
        if (health <= 0) active = false;
        else if (health <= limit) active = true;
        else if (health > limit + HYSTERESIS) active = false;
        float target = active ? 0.35F + 0.65F * severity(health, limit) : 0;
        intensity += Math.max(-FADE, Math.min(FADE, target - intensity));
        if (!active) {
            sinceBeat = SLOW;
            return false;
        }
        sinceBeat += 0.05F;
        if (sinceBeat + 1.0E-4F < interval(health, limit)) return false;
        sinceBeat = 0;
        return true;
    }

    /** 0 at the threshold, 1 at two health points or less. */
    public static float severity(float health, float limit) {
        if (limit <= 2) return 1;
        return Math.max(0, Math.min(1, (limit - health) / (limit - 2)));
    }

    public static float interval(float health, float limit) {
        return SLOW + (FAST - SLOW) * severity(health, limit);
    }

    public boolean active() {
        return active;
    }

    /** Vignette opacity: steady part pulsing with the beat, plus the damage flash. */
    public float alpha(float partial) {
        float pulse = active ? 0.75F + 0.25F * (float) Math.exp(-(sinceBeat + partial * 0.05F) * 6) : 1;
        return Math.max(0, Math.min(1, intensity * pulse + flash * 0.4F));
    }

    public void reset() {
        active = false;
        intensity = flash = 0;
        sinceBeat = SLOW;
        lastHealth = -1;
    }
}
