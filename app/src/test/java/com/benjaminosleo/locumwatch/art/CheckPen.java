package com.benjaminosleo.locumwatch.art;

import java.util.ArrayList;
import java.util.IdentityHashMap;
import java.util.List;

/** A pen that draws nothing: it counts the calls, checks every argument is sane and tracks save/restore balance. Used by the art tests. */
public final class CheckPen implements Pen {
    public int fills, strokes, rects, ovals, lines, clipRects, clipShapes, saves, restores, alphas, xforms;
    public int depth, maxDepth, underflow;
    public final List<String> problems = new ArrayList<>();
    public final IdentityHashMap<Ink, Boolean> inks = new IdentityHashMap<>();
    public final IdentityHashMap<Shape, Integer> shapeVersions = new IdentityHashMap<>();
    public int shapeRebuilds, gradientInkDraws, pathPts, multiContour, hairlines, zeroAlphaDraws;
    public float minX = Float.MAX_VALUE, minY = Float.MAX_VALUE, maxX = -Float.MAX_VALUE, maxY = -Float.MAX_VALUE;
    public boolean trackBounds = true;
    /** if true, remember shape versions across begin() calls to count shapes rebuilt between frames */
    private float[] m = {1, 0, 0, 1, 0, 0}; // a b c d e f : x' = a x + c y + e ; y' = b x + d y + f
    private final ArrayList<float[]> stack = new ArrayList<>();
    private final ArrayList<float[]> clipStack = new ArrayList<>();
    private float[] clip = {-1e9f, -1e9f, 1e9f, 1e9f};
    private float alpha = 1f;
    private final ArrayList<Float> alphaStack = new ArrayList<>();

    public int draws() { return fills + strokes + rects + ovals + lines; }

    public void resetCounts() {
        fills = strokes = rects = ovals = lines = clipRects = clipShapes = saves = restores = alphas = xforms = 0;
        shapeRebuilds = gradientInkDraws = pathPts = multiContour = hairlines = zeroAlphaDraws = 0;
        maxDepth = 0;
        minX = minY = Float.MAX_VALUE; maxX = maxY = -Float.MAX_VALUE;
    }

    private void fin(String where, float... v) {
        for (float f : v) if (Float.isNaN(f) || Float.isInfinite(f)) { if (problems.size() < 50) problems.add("non-finite arg in " + where); return; }
    }

    private void pt(float x, float y, float pad) {
        if (!trackBounds) return;
        float X = m[0] * x + m[2] * y + m[4], Y = m[1] * x + m[3] * y + m[5];
        float s = (float) Math.sqrt(Math.abs(m[0] * m[3] - m[1] * m[2]));
        float p = pad * s;
        float x0 = Math.max(X - p, clip[0]), y0 = Math.max(Y - p, clip[1]), x1 = Math.min(X + p, clip[2]), y1 = Math.min(Y + p, clip[3]);
        // only count if inside clip in a loose sense
        if (X + p < clip[0] || X - p > clip[2] || Y + p < clip[1] || Y - p > clip[3]) {
            // clamp to clip edge (conservative: shape may still reach the edge)
            X = Math.max(clip[0], Math.min(clip[2], X)); Y = Math.max(clip[1], Math.min(clip[3], Y));
            x0 = x1 = X; y0 = y1 = Y;
        }
        if (x0 < minX) minX = x0; if (y0 < minY) minY = y0; if (x1 > maxX) maxX = x1; if (y1 > maxY) maxY = y1;
    }

    private void ink(Ink ink, String where) {
        if (ink == null) { problems.add("null ink in " + where); return; }
        inks.put(ink, true);
        if (alpha <= 0.002f) zeroAlphaDraws++;
        if (ink.kind == Ink.SOLID) { if ((ink.color >>> 24) == 0) zeroAlphaDraws++; return; }
        gradientInkDraws++;
        if (ink.colors == null || ink.stops == null || ink.colors.length < 2 || ink.colors.length != ink.stops.length) {
            problems.add("bad gradient arrays in " + where); return;
        }
        for (int i = 0; i < ink.stops.length; i++) {
            float s = ink.stops[i];
            if (Float.isNaN(s) || s < 0f || s > 1f) problems.add("gradient stop out of range in " + where + ": " + s);
            if (i > 0 && s <= ink.stops[i - 1]) problems.add("gradient stops not strictly increasing in " + where);
        }
        fin(where + " gradient", ink.x0, ink.y0, ink.x1, ink.y1);
        if (ink.kind == Ink.LINEAR && ink.x0 == ink.x1 && ink.y0 == ink.y1) problems.add("degenerate linear gradient in " + where);
        if (ink.kind == Ink.RADIAL && !(ink.x1 > 0f)) problems.add("radial gradient radius <= 0 in " + where + ": " + ink.x1);
    }

    private void shape(Shape s, float pad, String where) {
        if (s == null) { problems.add("null shape in " + where); return; }
        Integer v = shapeVersions.put(s, s.version);
        if (v != null && v != s.version) shapeRebuilds++;
        if (s.nOps == 0) { problems.add("empty shape in " + where); return; }
        int k = 0, moves = 0;
        for (int i = 0; i < s.nOps; i++) {
            int n;
            switch (s.ops[i]) {
                case Shape.MOVE: moves++; n = 2; break;
                case Shape.LINE: n = 2; break;
                case Shape.QUAD: n = 4; break;
                case Shape.CUBIC: n = 6; break;
                default: n = 0;
            }
            for (int j = 0; j < n; j += 2) {
                float x = s.pts[k + j], y = s.pts[k + j + 1];
                if (Float.isNaN(x) || Float.isInfinite(x) || Float.isNaN(y) || Float.isInfinite(y)) { if (problems.size() < 50) problems.add("non-finite path point in " + where); }
                pt(x, y, pad);
            }
            k += n;
        }
        if (k != s.nPts) problems.add("shape point count mismatch in " + where);
        pathPts += s.nPts / 2;
        if (moves > 1) multiContour++;
    }

    @Override public void save() {
        saves++; depth++; if (depth > maxDepth) maxDepth = depth;
        stack.add(m.clone()); clipStack.add(clip.clone()); alphaStack.add(alpha);
    }
    @Override public void restore() {
        restores++;
        if (depth == 0) { underflow++; problems.add("restore underflow"); return; }
        depth--;
        m = stack.remove(stack.size() - 1); clip = clipStack.remove(clipStack.size() - 1); alpha = alphaStack.remove(alphaStack.size() - 1);
    }
    private void concat(float a, float b, float c, float d, float e, float f) {
        float[] n = new float[6];
        n[0] = m[0] * a + m[2] * b; n[1] = m[1] * a + m[3] * b;
        n[2] = m[0] * c + m[2] * d; n[3] = m[1] * c + m[3] * d;
        n[4] = m[0] * e + m[2] * f + m[4]; n[5] = m[1] * e + m[3] * f + m[5];
        m = n; xforms++;
    }
    @Override public void translate(float dx, float dy) { fin("translate", dx, dy); concat(1, 0, 0, 1, dx, dy); }
    @Override public void rotate(float deg, float px, float py) {
        fin("rotate", deg, px, py);
        double r = Math.toRadians(deg); float c = (float) Math.cos(r), s = (float) Math.sin(r);
        concat(1, 0, 0, 1, px, py); concat(c, s, -s, c, 0, 0); concat(1, 0, 0, 1, -px, -py);
    }
    @Override public void scale(float sx, float sy, float px, float py) {
        fin("scale", sx, sy, px, py);
        concat(1, 0, 0, 1, px, py); concat(sx, 0, 0, sy, 0, 0); concat(1, 0, 0, 1, -px, -py);
    }
    @Override public void clipRect(float l, float t, float r, float b) {
        fin("clipRect", l, t, r, b); clipRects++;
        if (depth == 0) problems.add("clipRect outside save/restore");
        // axis-aligned approximation in device space
        float[] xs = {l, r, l, r}, ys = {t, t, b, b};
        float x0 = Float.MAX_VALUE, y0 = Float.MAX_VALUE, x1 = -Float.MAX_VALUE, y1 = -Float.MAX_VALUE;
        for (int i = 0; i < 4; i++) {
            float X = m[0] * xs[i] + m[2] * ys[i] + m[4], Y = m[1] * xs[i] + m[3] * ys[i] + m[5];
            x0 = Math.min(x0, X); y0 = Math.min(y0, Y); x1 = Math.max(x1, X); y1 = Math.max(y1, Y);
        }
        clip = new float[]{Math.max(clip[0], x0), Math.max(clip[1], y0), Math.min(clip[2], x1), Math.min(clip[3], y1)};
    }
    @Override public void clipShape(Shape s) {
        clipShapes++;
        if (depth == 0) problems.add("clipShape outside save/restore");
        boolean tb = trackBounds; trackBounds = false; shape(s, 0, "clipShape"); trackBounds = tb;
        // approximate with bounding box of the shape in device space
        float x0 = Float.MAX_VALUE, y0 = Float.MAX_VALUE, x1 = -Float.MAX_VALUE, y1 = -Float.MAX_VALUE;
        for (int i = 0; i + 1 < s.nPts; i += 2) {
            float X = m[0] * s.pts[i] + m[2] * s.pts[i + 1] + m[4], Y = m[1] * s.pts[i] + m[3] * s.pts[i + 1] + m[5];
            x0 = Math.min(x0, X); y0 = Math.min(y0, Y); x1 = Math.max(x1, X); y1 = Math.max(y1, Y);
        }
        clip = new float[]{Math.max(clip[0], x0), Math.max(clip[1], y0), Math.min(clip[2], x1), Math.min(clip[3], y1)};
    }
    @Override public void alpha(float a) {
        alphas++; fin("alpha", a);
        if (depth == 0) problems.add("alpha outside save/restore");
        alpha *= Math.max(0f, Math.min(1f, a));
    }
    @Override public void fill(Shape s, Ink ink) { fills++; ink(ink, "fill"); shape(s, 0, "fill"); }
    @Override public void stroke(Shape s, Ink ink, float width) {
        strokes++; fin("stroke width", width); if (width < 0) problems.add("negative stroke width"); if (width == 0) hairlines++;
        ink(ink, "stroke"); shape(s, width / 2f, "stroke");
    }
    @Override public void fillRect(float l, float t, float r, float b, Ink ink) {
        rects++; fin("fillRect", l, t, r, b); ink(ink, "fillRect"); pt(l, t, 0); pt(r, t, 0); pt(l, b, 0); pt(r, b, 0);
    }
    @Override public void fillOval(float cx, float cy, float rx, float ry, Ink ink) {
        ovals++; fin("fillOval", cx, cy, rx, ry); ink(ink, "fillOval");
        pt(cx - rx, cy - ry, 0); pt(cx + rx, cy - ry, 0); pt(cx - rx, cy + ry, 0); pt(cx + rx, cy + ry, 0);
    }
    @Override public void line(float x0, float y0, float x1, float y1, Ink ink, float width) {
        lines++; fin("line", x0, y0, x1, y1, width); if (width < 0) problems.add("negative line width"); if (width == 0) hairlines++;
        ink(ink, "line"); pt(x0, y0, width / 2f); pt(x1, y1, width / 2f);
    }

    public String bounds() { return String.format("[%.1f, %.1f, %.1f, %.1f]", minX, minY, maxX, maxY); }
}
