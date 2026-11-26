package com.benjaminosleo.locumwatch.art;

/**
 * How a shape is coloured: a flat colour, a linear gradient or a radial gradient.
 * Colours are 0xAARRGGBB ints. Immutable; create once and reuse where possible.
 */
public final class Ink {
    public static final int SOLID = 0, LINEAR = 1, RADIAL = 2;

    public final int kind;
    public final int color;            // SOLID
    public final float x0, y0, x1, y1; // LINEAR: start and end points. RADIAL: centre (x0,y0), radius x1
    public final int[] colors;         // gradient colours
    public final float[] stops;        // gradient positions 0..1, strictly increasing
    /** Backend-private cache (e.g. an android.graphics.Shader). */
    public Object cache;

    private Ink(int kind, int color, float x0, float y0, float x1, float y1, int[] colors, float[] stops) {
        this.kind = kind; this.color = color;
        this.x0 = x0; this.y0 = y0; this.x1 = x1; this.y1 = y1;
        this.colors = colors; this.stops = stops;
    }

    public static Ink solid(int argb) { return new Ink(SOLID, argb, 0, 0, 0, 0, null, null); }

    public static Ink linear(float x0, float y0, float x1, float y1, int[] colors, float[] stops) {
        return new Ink(LINEAR, 0, x0, y0, x1, y1, colors, stops);
    }

    public static Ink radial(float cx, float cy, float radius, int[] colors, float[] stops) {
        return new Ink(RADIAL, 0, cx, cy, radius, 0, colors, stops);
    }

    // ---- colour helpers ----
    public static int rgb(int r, int g, int b) { return 0xFF000000 | (r << 16) | (g << 8) | b; }

    public static int withAlpha(int argb, float alpha) {
        int a = Math.round(((argb >>> 24) & 0xFF) * Math.max(0f, Math.min(1f, alpha)));
        return (a << 24) | (argb & 0x00FFFFFF);
    }

    /** Blend from a (t=0) to b (t=1), all four channels. */
    public static int mix(int a, int b, float t) {
        t = Math.max(0f, Math.min(1f, t));
        int aa = (a >>> 24) & 0xFF, ar = (a >> 16) & 0xFF, ag = (a >> 8) & 0xFF, ab = a & 0xFF;
        int ba = (b >>> 24) & 0xFF, br = (b >> 16) & 0xFF, bg = (b >> 8) & 0xFF, bb = b & 0xFF;
        return (Math.round(aa + (ba - aa) * t) << 24) | (Math.round(ar + (br - ar) * t) << 16)
            | (Math.round(ag + (bg - ag) * t) << 8) | Math.round(ab + (bb - ab) * t);
    }
}
