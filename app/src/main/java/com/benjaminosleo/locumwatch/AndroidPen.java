package com.benjaminosleo.locumwatch;

import android.graphics.Canvas;
import android.graphics.LinearGradient;
import android.graphics.Paint;
import android.graphics.Path;
import android.graphics.RadialGradient;
import android.graphics.Shader;

import com.benjaminosleo.locumwatch.art.Ink;
import com.benjaminosleo.locumwatch.art.Pen;
import com.benjaminosleo.locumwatch.art.Shape;

/**
 * Draws the shared art code onto an Android Canvas. One instance per view; call
 * {@link #begin(Canvas)} at the start of each draw pass.
 */
public final class AndroidPen implements Pen {
    private Canvas c;
    private final Paint fill = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint stroke = new Paint(Paint.ANTI_ALIAS_FLAG);
    private float[] alphaStack = new float[32];
    private int depth;
    private float alpha = 1f;

    public AndroidPen() {
        fill.setStyle(Paint.Style.FILL);
        stroke.setStyle(Paint.Style.STROKE);
        stroke.setStrokeCap(Paint.Cap.ROUND);
        stroke.setStrokeJoin(Paint.Join.ROUND);
    }

    public AndroidPen begin(Canvas canvas) {
        c = canvas;
        depth = 0;
        alpha = 1f;
        return this;
    }

    @Override public void save() {
        c.save();
        if (depth == alphaStack.length) alphaStack = java.util.Arrays.copyOf(alphaStack, depth * 2);
        alphaStack[depth++] = alpha;
    }

    @Override public void restore() {
        c.restore();
        if (depth > 0) alpha = alphaStack[--depth];
    }

    @Override public void translate(float dx, float dy) { c.translate(dx, dy); }
    @Override public void rotate(float degrees, float px, float py) { c.rotate(degrees, px, py); }
    @Override public void scale(float sx, float sy, float px, float py) { c.scale(sx, sy, px, py); }
    @Override public void clipRect(float l, float t, float r, float b) { c.clipRect(l, t, r, b); }
    @Override public void clipShape(Shape s) { c.clipPath(path(s)); }
    @Override public void alpha(float a) { alpha *= Math.max(0f, Math.min(1f, a)); }

    @Override public void fill(Shape s, Ink ink) { c.drawPath(path(s), prep(fill, ink)); }

    @Override public void stroke(Shape s, Ink ink, float width) {
        stroke.setStrokeWidth(width);
        c.drawPath(path(s), prep(stroke, ink));
    }

    @Override public void fillRect(float l, float t, float r, float b, Ink ink) { c.drawRect(l, t, r, b, prep(fill, ink)); }

    @Override public void fillOval(float cx, float cy, float rx, float ry, Ink ink) {
        c.drawOval(cx - rx, cy - ry, cx + rx, cy + ry, prep(fill, ink));
    }

    @Override public void line(float x0, float y0, float x1, float y1, Ink ink, float width) {
        stroke.setStrokeWidth(width);
        c.drawLine(x0, y0, x1, y1, prep(stroke, ink));
    }

    private Paint prep(Paint p, Ink ink) {
        if (ink.kind == Ink.SOLID) {
            p.setShader(null);
            int col = ink.color;
            int a = Math.round((col >>> 24) * alpha);
            p.setColor((a << 24) | (col & 0x00FFFFFF));
        } else {
            Shader sh = (Shader) ink.cache;
            if (sh == null) {
                sh = ink.kind == Ink.LINEAR
                    ? new LinearGradient(ink.x0, ink.y0, ink.x1, ink.y1, ink.colors, ink.stops, Shader.TileMode.CLAMP)
                    : new RadialGradient(ink.x0, ink.y0, Math.max(0.01f, ink.x1), ink.colors, ink.stops, Shader.TileMode.CLAMP);
                ink.cache = sh;
            }
            p.setColor(0xFF000000); // shader supplies the colour; paint alpha scales it
            p.setShader(sh);
            p.setAlpha(Math.round(255 * alpha));
        }
        return p;
    }

    /** Converts a Shape to an android Path, cached on the Shape until it changes. */
    private Path path(Shape s) {
        Path p;
        if (s.cache instanceof Path) {
            p = (Path) s.cache;
            if (s.cacheVersion == s.version) return p;
            p.rewind();
        } else {
            p = new Path();
            s.cache = p;
        }
        s.cacheVersion = s.version;
        final float[] t = s.pts;
        int k = 0;
        for (int i = 0; i < s.nOps; i++) {
            switch (s.ops[i]) {
                case Shape.MOVE: p.moveTo(t[k], t[k + 1]); k += 2; break;
                case Shape.LINE: p.lineTo(t[k], t[k + 1]); k += 2; break;
                case Shape.QUAD: p.quadTo(t[k], t[k + 1], t[k + 2], t[k + 3]); k += 4; break;
                case Shape.CUBIC: p.cubicTo(t[k], t[k + 1], t[k + 2], t[k + 3], t[k + 4], t[k + 5]); k += 6; break;
                default: p.close();
            }
        }
        return p;
    }
}
