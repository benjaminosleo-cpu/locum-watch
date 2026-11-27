package com.benjaminosleo.locumwatch.art;

/** Small maths and shape-building helpers shared by all the art classes. */
final class Art {
    private Art() {}

    static final float TAU = 6.2831855f;

    static float sin(double a) { return (float) Math.sin(a); }
    static float cos(double a) { return (float) Math.cos(a); }
    static float lerp(float a, float b, float t) { return a + (b - a) * t; }
    static float clamp(float v, float lo, float hi) { return v < lo ? lo : (v > hi ? hi : v); }
    static float clamp01(float v) { return v < 0f ? 0f : (v > 1f ? 1f : v); }
    /** 0 below a, 1 above b, straight line between. */
    static float ramp(float x, float a, float b) { return clamp01((x - a) / (b - a)); }
    /** Smoothstep of a 0..1 value. */
    static float smooth(float t) { t = clamp01(t); return t * t * (3f - 2f * t); }
    /** Smooth 0..1 ramp between a and b. */
    static float smooth(float x, float a, float b) { return smooth(ramp(x, a, b)); }
    /** Fractional part, always 0..1, taken in double so long run times stay smooth. */
    static float frac(double v) { return (float) (v - Math.floor(v)); }
    /** sin of a full turn every {@code period} seconds. */
    static float wave(double t, double period) { return (float) Math.sin(TAU * (t / period)); }
    /** 0 -> 1 -> 0 hump over a 0..1 input. */
    static float hump(float t) { return (float) Math.sin(Math.PI * clamp01(t)); }
    /** Eases past 1 and settles back (for bouncy arrivals). */
    static float overshoot(float t) {
        t = clamp01(t);
        float s = 1.70158f; t -= 1f;
        return t * t * ((s + 1f) * t + s) + 1f;
    }

    /** Cheap deterministic 0..1 hash of an integer. */
    static float hash(int n) {
        n = (n << 13) ^ n;
        n = n * (n * n * 15731 + 789221) + 1376312589;
        return ((n & 0x7fffffff) % 100000) / 100000f;
    }

    /** A usable animation clock: not-a-number and infinite times become 0, so a broken clock cannot break a picture. */
    static double clock(double t) { return t != t || t == Double.POSITIVE_INFINITY || t == Double.NEGATIVE_INFINITY ? 0.0 : t; }

    static Ink ink(int argb) { return Ink.solid(argb); }

    /**
     * A linear gradient that is always well-formed: if the two ends coincide (a piece drawn at
     * zero size) they are nudged apart, because backends reject a zero-length gradient.
     */
    static Ink linear(float x0, float y0, float x1, float y1, int[] colors, float[] stops) {
        if (x0 == x1 && y0 == y1) y1 = y0 + 0.01f;
        return Ink.linear(x0, y0, x1, y1, colors, stops);
    }

    /** A radial gradient that is always well-formed: the radius is kept above zero. */
    static Ink radial(float cx, float cy, float radius, int[] colors, float[] stops) {
        return Ink.radial(cx, cy, radius > 0.01f ? radius : 0.01f, colors, stops);
    }

    /** Vertical two-colour gradient. */
    static Ink vgrad(float y0, float y1, int c0, int c1) {
        return linear(0, y0, 0, y1, new int[]{c0, c1}, new float[]{0f, 1f});
    }

    /** Soft round glow: colour at the centre fading to transparent at the rim. */
    static Ink glow(float cx, float cy, float r, int c, float alpha) {
        return radial(cx, cy, r,
            new int[]{Ink.withAlpha(c, alpha), Ink.withAlpha(c, alpha * 0.45f), Ink.withAlpha(c, 0f)},
            new float[]{0f, 0.45f, 1f});
    }

    /** Smooth closed outline through the given points (x0,y0,x1,y1,...). */
    static Shape blob(Shape s, float... xy) {
        int n = xy.length / 2;
        s.moveTo(xy[0], xy[1]);
        for (int i = 0; i < n; i++) {
            int a = ((i - 1 + n) % n) * 2, b = i * 2, c = ((i + 1) % n) * 2, d = ((i + 2) % n) * 2;
            s.cubicTo(xy[b] + (xy[c] - xy[a]) / 6f, xy[b + 1] + (xy[c + 1] - xy[a + 1]) / 6f,
                xy[c] - (xy[d] - xy[b]) / 6f, xy[c + 1] - (xy[d + 1] - xy[b + 1]) / 6f,
                xy[c], xy[c + 1]);
        }
        return s.close();
    }

    /** Smooth open curve through the given points. */
    static Shape curve(Shape s, float... xy) {
        int n = xy.length / 2;
        s.moveTo(xy[0], xy[1]);
        for (int i = 0; i < n - 1; i++) {
            int a = Math.max(0, i - 1) * 2, b = i * 2, c = (i + 1) * 2, d = Math.min(n - 1, i + 2) * 2;
            s.cubicTo(xy[b] + (xy[c] - xy[a]) / 6f, xy[b + 1] + (xy[c + 1] - xy[a + 1]) / 6f,
                xy[c] - (xy[d] - xy[b]) / 6f, xy[c + 1] - (xy[d + 1] - xy[b + 1]) / 6f,
                xy[c], xy[c + 1]);
        }
        return s;
    }

    /** Pointed leaf from (x,y), {@code len} long and {@code wid} wide, pointing along angle (degrees, 0 = right). */
    static Shape leaf(Shape s, float x, float y, float len, float wid, float deg) {
        double a = Math.toRadians(deg);
        float dx = (float) Math.cos(a), dy = (float) Math.sin(a), nx = -dy, ny = dx;
        float tx = x + dx * len, ty = y + dy * len, h = wid * 0.5f;
        s.moveTo(x, y);
        s.cubicTo(x + dx * len * 0.25f + nx * h, y + dy * len * 0.25f + ny * h,
            x + dx * len * 0.7f + nx * h, y + dy * len * 0.7f + ny * h, tx, ty);
        s.cubicTo(x + dx * len * 0.7f - nx * h, y + dy * len * 0.7f - ny * h,
            x + dx * len * 0.25f - nx * h, y + dy * len * 0.25f - ny * h, x, y);
        return s.close();
    }

    /** Rounded petal from (x,y) pointing along angle; blunt, slightly notched tip. */
    static Shape petal(Shape s, float x, float y, float len, float wid, float deg) {
        double a = Math.toRadians(deg);
        float dx = (float) Math.cos(a), dy = (float) Math.sin(a), nx = -dy, ny = dx, h = wid * 0.5f;
        s.moveTo(x, y);
        s.cubicTo(x + dx * len * 0.3f + nx * h * 1.1f, y + dy * len * 0.3f + ny * h * 1.1f,
            x + dx * len * 0.95f + nx * h * 1.2f, y + dy * len * 0.95f + ny * h * 1.2f,
            x + dx * len + nx * h * 0.25f, y + dy * len + ny * h * 0.25f);
        s.lineTo(x + dx * len * 0.9f, y + dy * len * 0.9f);
        s.lineTo(x + dx * len - nx * h * 0.25f, y + dy * len - ny * h * 0.25f);
        s.cubicTo(x + dx * len * 0.95f - nx * h * 1.2f, y + dy * len * 0.95f - ny * h * 1.2f,
            x + dx * len * 0.3f - nx * h * 1.1f, y + dy * len * 0.3f - ny * h * 1.1f, x, y);
        return s.close();
    }

    /** Five-petalled blossom added to the shape. */
    static Shape blossom(Shape s, float x, float y, float r, float rotDeg) {
        for (int i = 0; i < 5; i++) petal(s, x, y, r, r * 0.8f, rotDeg + i * 72f);
        return s;
    }

    /** Four-pointed sparkle. */
    static Shape sparkle(Shape s, float x, float y, float r) {
        float k = r * 0.22f;
        s.moveTo(x, y - r);
        s.quadTo(x + k, y - k, x + r, y); s.quadTo(x + k, y + k, x, y + r);
        s.quadTo(x - k, y + k, x - r, y); s.quadTo(x - k, y - k, x, y - r);
        return s.close();
    }

    /** A hand-cut rectangle: straight-ish edges with small wobbles, seeded. */
    static Shape wobblyRect(Shape s, float l, float t, float r, float b, float wob, int seed) {
        float step = 22f;
        int nx = Math.max(2, Math.round((r - l) / step)), ny = Math.max(2, Math.round((b - t) / step));
        int k = seed * 131;
        s.moveTo(l + w(k++, wob), t + w(k++, wob));
        for (int i = 1; i <= nx; i++) {
            float x = l + (r - l) * i / nx;
            s.quadTo(x - (r - l) / nx * 0.5f, t + w(k++, wob) * 1.4f, x + (i == nx ? w(k++, wob) : 0), t + w(k++, wob));
        }
        for (int i = 1; i <= ny; i++) {
            float y = t + (b - t) * i / ny;
            s.quadTo(r + w(k++, wob) * 1.4f, y - (b - t) / ny * 0.5f, r + w(k++, wob), y + (i == ny ? w(k++, wob) : 0));
        }
        for (int i = nx - 1; i >= 0; i--) {
            float x = l + (r - l) * i / nx;
            s.quadTo(x + (r - l) / nx * 0.5f, b + w(k++, wob) * 1.4f, x + (i == 0 ? w(k++, wob) : 0), b + w(k++, wob));
        }
        for (int i = ny - 1; i >= 1; i--) {
            float y = t + (b - t) * i / ny;
            s.quadTo(l + w(k++, wob) * 1.4f, y + (b - t) / ny * 0.5f, l + w(k++, wob), y);
        }
        return s.close();
    }

    private static float w(int k, float wob) { return (hash(k) - 0.5f) * 2f * wob; }
}
