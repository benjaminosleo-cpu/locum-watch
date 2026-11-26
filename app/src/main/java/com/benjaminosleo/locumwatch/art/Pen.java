package com.benjaminosleo.locumwatch.art;

/**
 * The only drawing surface the art code may use. Two implementations exist: one over
 * Android's Canvas (in the app) and one over desktop Java2D (tools/preview) so every
 * frame can be rendered to a PNG and looked at without a phone.
 *
 * Coordinates are in pixels, y down. All fills and strokes are anti-aliased.
 * Strokes always use round caps and round joins.
 */
public interface Pen {
    void save();
    void restore();
    void translate(float dx, float dy);
    /** Degrees, clockwise, about (px, py). */
    void rotate(float degrees, float px, float py);
    /** Scale about (px, py). Negative sx mirrors horizontally. */
    void scale(float sx, float sy, float px, float py);
    /** Intersect the clip with a rectangle (saved and restored with save/restore). */
    void clipRect(float l, float t, float r, float b);
    /** Intersect the clip with a shape. */
    void clipShape(Shape s);
    /**
     * Multiplies the opacity of everything drawn until the matching restore().
     * Applied per draw call (overlapping shapes inside will show through each other).
     */
    void alpha(float a);

    void fill(Shape s, Ink ink);
    void stroke(Shape s, Ink ink, float width);
    void fillRect(float l, float t, float r, float b, Ink ink);
    void fillOval(float cx, float cy, float rx, float ry, Ink ink);
    void line(float x0, float y0, float x1, float y1, Ink ink, float width);
}
