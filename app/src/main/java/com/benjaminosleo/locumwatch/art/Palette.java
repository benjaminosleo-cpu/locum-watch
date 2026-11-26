package com.benjaminosleo.locumwatch.art;

/**
 * Time-of-day lighting. {@link #at(float)} blends smoothly between night, dawn, day,
 * golden hour and dusk. Every art class paints with "daylight" colours and passes them
 * through {@link #lit(int)} (garden), {@link #character(int)} (Mochi) or {@link #room(int)}
 * (cottage interior), so the whole picture shifts together.
 * Immutable; creating one allocates, so only do it when the hour changes noticeably.
 */
public final class Palette {
    /** Typical hours used for the five named moods. */
    public static final float DAWN = 6.2f, DAY = 12f, GOLDEN = 17.3f, DUSK = 19.1f, NIGHT = 23f;

    // key: hour, skyTop, skyMid, skyLow, light, haze, room light, cloud, night, warm
    private static final float[] KH = {0f, 4.7f, 5.6f, 6.4f, 8.5f, 15.4f, 17.3f, 19.1f, 20.5f, 24f};
    private static final int[][] KC = {
        // skyTop    skyMid    skyLow    light     haze      room      cloud
        {0x101838, 0x22326A, 0x4A6498, 0x7890D2, 0x40588C, 0x94807E, 0x5A6CA0},
        {0x101838, 0x22326A, 0x4A6498, 0x7890D2, 0x40588C, 0x94807E, 0x5A6CA0},
        {0x343C78, 0x8A6C9E, 0xF2A88E, 0xAA9CCC, 0xB088A8, 0xB8A4A4, 0xE8A0A8},
        {0x82AAE0, 0xF4C2BC, 0xFFE4BA, 0xFFE6DE, 0xFFD8CC, 0xFFEEE6, 0xFFE4E0},
        {0x4E9EE8, 0x94CCF4, 0xE2F4F2, 0xFFFFFF, 0xCDE8F0, 0xFFFFFF, 0xFFFFFF},
        {0x4E9EE8, 0x94CCF4, 0xE2F4F2, 0xFFFFFF, 0xCDE8F0, 0xFFFFFF, 0xFFFFFF},
        {0x6498D2, 0xF6D290, 0xFFB564, 0xFFDCA2, 0xFFCC88, 0xFFE4BC, 0xFFDCA8},
        {0x34387A, 0xAE608C, 0xF98C66, 0xC49AB6, 0xD88480, 0xD0AEA6, 0xE88C8C},
        {0x101838, 0x22326A, 0x4A6498, 0x7890D2, 0x40588C, 0x94807E, 0x5A6CA0},
        {0x101838, 0x22326A, 0x4A6498, 0x7890D2, 0x40588C, 0x94807E, 0x5A6CA0},
    };
    private static final float[] K_NIGHT = {1f, 1f, 0.7f, 0.12f, 0f, 0f, 0f, 0.45f, 1f, 1f};
    private static final float[] K_WARM = {0f, 0f, 0.3f, 0.55f, 0f, 0f, 1f, 0.6f, 0f, 0f};

    /** The hour this palette was built for, 0..24. */
    public final float hour;
    /** Sky gradient colours from zenith to horizon. */
    public final int skyTop, skyMid, skyLow;
    /** Colour that distant things fade towards. */
    public final int haze;
    /** Cloud body colour. */
    public final int cloud;
    /** 0 in daylight, 1 in full night (drives stars, fireflies, candle glow). */
    public final float night;
    /** 0..1 how golden the light is (dawn and golden hour). */
    public final float warm;
    /** Sun position in hero units (may be above the window), and 0..1 visibility. */
    public final float sunX, sunY, sunVis;
    /** Moon visibility 0..1. */
    public final float moonVis;

    private final int light, room;

    private Palette(float h) {
        h = ((h % 24f) + 24f) % 24f;
        hour = h;
        int i = 0;
        while (i < KH.length - 2 && h >= KH[i + 1]) i++;
        float t = Art.smooth((h - KH[i]) / (KH[i + 1] - KH[i]));
        skyTop = mixKey(i, 0, t); skyMid = mixKey(i, 1, t); skyLow = mixKey(i, 2, t);
        light = mixKey(i, 3, t); haze = mixKey(i, 4, t); room = mixKey(i, 5, t); cloud = mixKey(i, 6, t);
        night = Art.lerp(K_NIGHT[i], K_NIGHT[i + 1], t);
        warm = Art.lerp(K_WARM[i], K_WARM[i + 1], t);
        // The sun crosses from the right (dawn) to the left (dusk) and climbs out of the window by day.
        float k = (h - 5.4f) / (19.6f - 5.4f);
        float alt = k <= 0f || k >= 1f ? -30f : (float) Math.pow(Math.sin(Math.PI * k), 1.5) * 300f;
        sunX = Art.lerp(272f, 150f, Art.clamp01(k));
        sunY = 172f - alt;
        sunVis = Art.smooth(Art.ramp(alt, -12f, 6f));
        moonVis = Art.smooth(Art.ramp(night, 0.55f, 1f));
    }

    /** How night-like an hour is, 0..1, without building a palette (no allocation). */
    public static float nightAt(float h) {
        h = ((h % 24f) + 24f) % 24f;
        int i = 0;
        while (i < KH.length - 2 && h >= KH[i + 1]) i++;
        return Art.lerp(K_NIGHT[i], K_NIGHT[i + 1], Art.smooth((h - KH[i]) / (KH[i + 1] - KH[i])));
    }

    /** Palette for an hour of the day (0..24, wraps). */
    public static Palette at(float hour) { return new Palette(hour); }

    private static int mixKey(int i, int c, float t) { return Ink.mix(0xFF000000 | KC[i][c], 0xFF000000 | KC[i + 1][c], t); }

    private static int mul(int c, int l) {
        int r = ((c >> 16) & 0xFF) * ((l >> 16) & 0xFF) / 255;
        int g = ((c >> 8) & 0xFF) * ((l >> 8) & 0xFF) / 255;
        int b = (c & 0xFF) * (l & 0xFF) / 255;
        return (c & 0xFF000000) | (r << 16) | (g << 8) | b;
    }

    /** A garden colour under this hour's light. */
    public int lit(int c) { return mul(c, light); }

    /** A garden colour under this hour's light, faded {@code far} (0..1) into the haze. */
    public int lit(int c, float far) { return Ink.mix(mul(c, light), (c & 0xFF000000) | (haze & 0xFFFFFF), far); }

    /** Mochi's colours: lit like the garden but kept a little truer so the eye finds Mochi first. */
    public int character(int c) { return Ink.mix(mul(c, light), c, 0.22f + 0.12f * night); }

    /** A cottage-interior colour under this hour's indoor light. */
    public int room(int c) { return mul(c, room); }

    /** Interior colour with the lighting effect softened (for surfaces that carry text). */
    public int roomSoft(int c) { return Ink.mix(mul(c, room), c, 0.55f); }

    /** Colour of things that glow by themselves (flames, fireflies, stars): never dimmed. */
    public int glow(int c) { return c; }
}
