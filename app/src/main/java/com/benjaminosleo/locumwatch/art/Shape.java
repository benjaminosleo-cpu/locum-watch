package com.benjaminosleo.locumwatch.art;

/**
 * A vector outline recorded as plain numbers, so the same art code can be drawn by
 * Android's Canvas (in the app) and by desktop Java (for preview renders).
 * Build once and reuse where possible; call {@link #reset()} to rebuild in place.
 */
public final class Shape {
    public static final byte MOVE = 0, LINE = 1, QUAD = 2, CUBIC = 3, CLOSE = 4;

    public byte[] ops = new byte[16];
    public float[] pts = new float[64];
    public int nOps, nPts;
    /** Bumped on every change, so a backend can cache its converted path. */
    public int version;
    /** Backend-private cache (e.g. an android.graphics.Path). */
    public Object cache;
    public int cacheVersion = -1;

    public Shape reset() { nOps = 0; nPts = 0; version++; return this; }

    public Shape moveTo(float x, float y) { op(MOVE); pt(x, y); return this; }
    public Shape lineTo(float x, float y) { op(LINE); pt(x, y); return this; }
    public Shape quadTo(float cx, float cy, float x, float y) { op(QUAD); pt(cx, cy); pt(x, y); return this; }
    public Shape cubicTo(float c1x, float c1y, float c2x, float c2y, float x, float y) {
        op(CUBIC); pt(c1x, c1y); pt(c2x, c2y); pt(x, y); return this;
    }
    public Shape close() { op(CLOSE); return this; }

    /** Adds a closed ellipse (four cubic arcs). */
    public Shape oval(float cx, float cy, float rx, float ry) {
        final float k = 0.5522848f, ox = rx * k, oy = ry * k;
        moveTo(cx + rx, cy);
        cubicTo(cx + rx, cy + oy, cx + ox, cy + ry, cx, cy + ry);
        cubicTo(cx - ox, cy + ry, cx - rx, cy + oy, cx - rx, cy);
        cubicTo(cx - rx, cy - oy, cx - ox, cy - ry, cx, cy - ry);
        cubicTo(cx + ox, cy - ry, cx + rx, cy - oy, cx + rx, cy);
        return close();
    }

    /** Adds a closed rounded rectangle. */
    public Shape roundRect(float l, float t, float r, float b, float rad) {
        rad = Math.min(rad, Math.min((r - l) / 2f, (b - t) / 2f));
        final float k = rad * (1f - 0.5522848f);
        moveTo(l + rad, t);
        lineTo(r - rad, t); cubicTo(r - k, t, r, t + k, r, t + rad);
        lineTo(r, b - rad); cubicTo(r, b - k, r - k, b, r - rad, b);
        lineTo(l + rad, b); cubicTo(l + k, b, l, b - k, l, b - rad);
        lineTo(l, t + rad); cubicTo(l, t + k, l + k, t, l + rad, t);
        return close();
    }

    private void op(byte o) {
        if (nOps == ops.length) ops = java.util.Arrays.copyOf(ops, nOps * 2);
        ops[nOps++] = o;
        version++;
    }

    private void pt(float x, float y) {
        if (nPts + 2 > pts.length) pts = java.util.Arrays.copyOf(pts, pts.length * 2);
        pts[nPts++] = x;
        pts[nPts++] = y;
    }
}
