package com.benjaminosleo.locumwatch.art;

/**
 * Brush helpers for the STATIC, cached layers only (it allocates freely).
 * Every colour passed in is a daylight colour; the painter lights it for the hour.
 */
final class Painter {
    static final int GARDEN = 0, ROOM = 1, RAW = 2, ROOM_SOFT = 3;

    final Pen p;
    final Palette pal;
    final Rng rng = new Rng(1);
    private final Shape tmp = new Shape();
    int mode = GARDEN;
    /** 0..1 haze applied to garden colours (distance). */
    float far;

    Painter(Pen p, Palette pal, int mode) { this.p = p; this.pal = pal; this.mode = mode; }

    int c(int col) {
        switch (mode) {
            case GARDEN: return far > 0f ? pal.lit(col, far) : pal.lit(col);
            case ROOM: return pal.room(col);
            case ROOM_SOFT: return pal.roomSoft(col);
            default: return col;
        }
    }

    Ink ink(int col) { return Ink.solid(c(col)); }
    Ink ink(int col, float a) { return Ink.solid(Ink.withAlpha(c(col), a)); }

    void rect(float l, float t, float r, float b, int col) { p.fillRect(l, t, r, b, ink(col)); }
    void rect(float l, float t, float r, float b, int col, float a) { p.fillRect(l, t, r, b, ink(col, a)); }
    void oval(float x, float y, float rx, float ry, int col) { p.fillOval(x, y, rx, ry, ink(col)); }
    void oval(float x, float y, float rx, float ry, int col, float a) { p.fillOval(x, y, rx, ry, ink(col, a)); }
    void line(float x0, float y0, float x1, float y1, int col, float w) { p.line(x0, y0, x1, y1, ink(col), w); }
    void line(float x0, float y0, float x1, float y1, int col, float w, float a) { p.line(x0, y0, x1, y1, ink(col, a), w); }
    void fill(Shape s, int col) { p.fill(s, ink(col)); }
    void fill(Shape s, int col, float a) { p.fill(s, ink(col, a)); }
    void stroke(Shape s, int col, float w) { p.stroke(s, ink(col), w); }
    void stroke(Shape s, int col, float w, float a) { p.stroke(s, ink(col, a), w); }

    /** Vertical gradient rectangle. */
    void vrect(float l, float t, float r, float b, int top, int bottom) {
        p.fillRect(l, t, r, b, Art.vgrad(t, b, c(top), c(bottom)));
    }

    void blob(int col, float a, float... xy) { fill(Art.blob(tmp.reset(), xy), col, a); }
    void curve(int col, float w, float a, float... xy) { stroke(Art.curve(tmp.reset(), xy), col, w, a); }
    void leaf(float x, float y, float len, float wid, float deg, int col, float a) { fill(Art.leaf(tmp.reset(), x, y, len, wid, deg), col, a); }
    void petal(float x, float y, float len, float wid, float deg, int col, float a) { fill(Art.petal(tmp.reset(), x, y, len, wid, deg), col, a); }
    void blossom(float x, float y, float r, float rot, int col, float a) { fill(Art.blossom(tmp.reset(), x, y, r, rot), col, a); }

    /** Soft round glow of a colour that is NOT relit (light sources). */
    void glow(float x, float y, float r, int col, float a) {
        p.fillOval(x, y, r, r, Art.glow(x, y, r, col, a));
    }

    /** Soft glow squashed into an ellipse (pools of light on a surface). */
    void glowOval(float x, float y, float rx, float ry, int col, float a) {
        p.save();
        p.scale(1f, ry / rx, x, y);
        p.fillOval(x, y, rx, rx, Art.glow(x, y, rx, col, a));
        p.restore();
    }

    /** A line that tapers from width w0 to w1 along a smooth curve through the points. */
    void taper(int col, float w0, float w1, float... xy) {
        int n = xy.length / 2, steps = (n - 1) * 8;
        float lx = xy[0], ly = xy[1];
        for (int i = 1; i <= steps; i++) {
            float u = (float) i / steps * (n - 1);
            int k = Math.min(n - 2, (int) u);
            float f = u - k;
            int a = Math.max(0, k - 1) * 2, b = k * 2, cc = (k + 1) * 2, d = Math.min(n - 1, k + 2) * 2;
            float x = cr(xy[a], xy[b], xy[cc], xy[d], f), y = cr(xy[a + 1], xy[b + 1], xy[cc + 1], xy[d + 1], f);
            line(lx, ly, x, y, col, Art.lerp(w0, w1, (float) i / steps));
            lx = x; ly = y;
        }
    }

    private static float cr(float p0, float p1, float p2, float p3, float t) {
        return 0.5f * ((2f * p1) + (-p0 + p2) * t + (2f * p0 - 5f * p1 + 4f * p2 - p3) * t * t + (-p0 + 3f * p1 - 3f * p2 + p3) * t * t * t);
    }

    /**
     * A painted mass of leaves: a dark underpainting, then dabs of mid and light tone
     * that get brighter towards the top-left, the way a gouache bush is built up.
     */
    void foliage(float cx, float cy, float rx, float ry, int dark, int mid, int light, float dab, int seed) {
        rng.seed(seed);
        int n = 12;
        float[] xy = new float[n * 2];
        for (int i = 0; i < n; i++) {
            double a = Art.TAU * i / n;
            float k = 0.86f + 0.2f * rng.f();
            xy[i * 2] = cx + (float) Math.cos(a) * rx * k; xy[i * 2 + 1] = cy + (float) Math.sin(a) * ry * k;
        }
        blob(dark, 1f, xy);
        int count = Math.max(12, (int) (rx * ry / (dab * dab) * 2.6f));
        for (int pass = 0; pass < 3; pass++) {
            int col = pass == 0 ? dark : (pass == 1 ? mid : light);
            int m = pass == 0 ? count : (pass == 1 ? count : count * 2 / 3);
            for (int i = 0; i < m; i++) {
                double a = rng.f() * Art.TAU;
                float rr = (float) Math.sqrt(rng.f()) * (pass == 0 ? 1.08f : (pass == 1 ? 0.95f : 0.8f));
                float x = cx + (float) Math.cos(a) * rx * rr, y = cy + (float) Math.sin(a) * ry * rr;
                if (pass == 1 && y > cy + ry * 0.55f && rng.chance(0.7f)) continue;
                if (pass == 2) {
                    x -= rx * 0.14f; y -= ry * 0.22f;
                    if (y > cy + ry * 0.15f && rng.chance(0.8f)) continue;
                }
                float sz = dab * rng.r(0.7f, 1.3f);
                int cc = Ink.mix(col, pass == 2 ? mid : dark, rng.f() * 0.35f);
                leaf(x, y, sz * 1.5f, sz, rng.r(-160f, -20f), cc, pass == 0 ? 1f : 0.92f);
            }
        }
    }
}
