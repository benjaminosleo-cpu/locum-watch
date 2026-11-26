import com.benjaminosleo.locumwatch.art.Ink;
import com.benjaminosleo.locumwatch.art.Pen;
import com.benjaminosleo.locumwatch.art.Shape;

import java.awt.AlphaComposite;
import java.awt.BasicStroke;
import java.awt.Color;
import java.awt.Graphics2D;
import java.awt.LinearGradientPaint;
import java.awt.MultipleGradientPaint;
import java.awt.Paint;
import java.awt.RadialGradientPaint;
import java.awt.RenderingHints;
import java.awt.geom.AffineTransform;
import java.awt.geom.Ellipse2D;
import java.awt.geom.Line2D;
import java.awt.geom.Path2D;
import java.awt.geom.Rectangle2D;
import java.awt.image.BufferedImage;
import java.util.ArrayDeque;

/** Desktop implementation of Pen, for rendering preview PNGs. Counts draw calls. */
public final class AwtPen implements Pen {
    private final Graphics2D g;
    private final ArrayDeque<Object[]> stack = new ArrayDeque<>();
    private float alpha = 1f;
    public int drawCalls;

    public AwtPen(BufferedImage img) {
        g = img.createGraphics();
        g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        g.setRenderingHint(RenderingHints.KEY_STROKE_CONTROL, RenderingHints.VALUE_STROKE_PURE);
        g.setRenderingHint(RenderingHints.KEY_RENDERING, RenderingHints.VALUE_RENDER_QUALITY);
    }

    public Graphics2D graphics() { return g; }

    @Override public void save() { stack.push(new Object[]{g.getTransform(), g.getClip(), alpha}); }
    @Override public void restore() {
        Object[] s = stack.pop();
        g.setTransform((AffineTransform) s[0]);
        g.setClip((java.awt.Shape) s[1]);
        alpha = (Float) s[2];
    }
    @Override public void translate(float dx, float dy) { g.translate(dx, dy); }
    @Override public void rotate(float deg, float px, float py) { g.rotate(Math.toRadians(deg), px, py); }
    @Override public void scale(float sx, float sy, float px, float py) {
        g.translate(px, py); g.scale(sx, sy); g.translate(-px, -py);
    }
    @Override public void clipRect(float l, float t, float r, float b) { g.clip(new Rectangle2D.Float(l, t, r - l, b - t)); }
    @Override public void clipShape(Shape s) { g.clip(path(s)); }
    @Override public void alpha(float a) { alpha *= Math.max(0f, Math.min(1f, a)); }

    @Override public void fill(Shape s, Ink ink) { prep(ink); g.fill(path(s)); drawCalls++; }
    @Override public void stroke(Shape s, Ink ink, float width) { prep(ink); stroke(width); g.draw(path(s)); drawCalls++; }
    @Override public void fillRect(float l, float t, float r, float b, Ink ink) {
        prep(ink); g.fill(new Rectangle2D.Float(l, t, r - l, b - t)); drawCalls++;
    }
    @Override public void fillOval(float cx, float cy, float rx, float ry, Ink ink) {
        prep(ink); g.fill(new Ellipse2D.Float(cx - rx, cy - ry, rx * 2, ry * 2)); drawCalls++;
    }
    @Override public void line(float x0, float y0, float x1, float y1, Ink ink, float width) {
        prep(ink); stroke(width); g.draw(new Line2D.Float(x0, y0, x1, y1)); drawCalls++;
    }

    private void stroke(float w) { g.setStroke(new BasicStroke(w, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND)); }

    private void prep(Ink ink) {
        g.setComposite(AlphaComposite.getInstance(AlphaComposite.SRC_OVER, alpha));
        g.setPaint(paint(ink));
    }

    private static Color c(int argb) { return new Color(argb, true); }

    private static Paint paint(Ink ink) {
        if (ink.kind == Ink.SOLID) return c(ink.color);
        Color[] cs = new Color[ink.colors.length];
        for (int i = 0; i < cs.length; i++) cs[i] = c(ink.colors[i]);
        if (ink.kind == Ink.LINEAR) {
            return new LinearGradientPaint(ink.x0, ink.y0, ink.x1, ink.y1, ink.stops, cs,
                MultipleGradientPaint.CycleMethod.NO_CYCLE);
        }
        return new RadialGradientPaint(ink.x0, ink.y0, Math.max(0.01f, ink.x1), ink.stops, cs,
            MultipleGradientPaint.CycleMethod.NO_CYCLE);
    }

    private static Path2D.Float path(Shape s) {
        Path2D.Float p = new Path2D.Float(Path2D.WIND_NON_ZERO);
        int k = 0;
        for (int i = 0; i < s.nOps; i++) {
            switch (s.ops[i]) {
                case Shape.MOVE: p.moveTo(s.pts[k], s.pts[k + 1]); k += 2; break;
                case Shape.LINE: p.lineTo(s.pts[k], s.pts[k + 1]); k += 2; break;
                case Shape.QUAD: p.quadTo(s.pts[k], s.pts[k + 1], s.pts[k + 2], s.pts[k + 3]); k += 4; break;
                case Shape.CUBIC:
                    p.curveTo(s.pts[k], s.pts[k + 1], s.pts[k + 2], s.pts[k + 3], s.pts[k + 4], s.pts[k + 5]); k += 6; break;
                default: p.closePath();
            }
        }
        return p;
    }
}
