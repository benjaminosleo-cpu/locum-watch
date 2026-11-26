package com.benjaminosleo.locumwatch.art;

/**
 * The cottage interior around the window, drawn over the garden in hero coordinates:
 * plaster wall, wonky timber frame with casements swung open into the room, the lintel
 * beam with hanging herbs, the deep sill and the things standing on it, including the
 * lantern that is lit when the alarm is switched on.
 *
 *  - {@link #drawStatic(Pen)}: STATIC for a given hour and armed state; leaves the window
 *    opening transparent. Cache it in a bitmap.
 *  - {@link #drawLive(Pen, double)}: ANIMATED extras (flame, glow flicker, glints). About 10 draw calls.
 */
public final class WindowFrame {
    static final int TIMBER = 0xFF8E6240, TIMBER_DARK = 0xFF64422A, TIMBER_LIGHT = 0xFFB88A5E, TIMBER_DEEP = 0xFF43291A;
    static final int IRON = 0xFF4C3E38, BRASS = 0xFFD8A848, TERRACOTTA = 0xFFCC7850, CREAM = 0xFFF8EED4;
    static final int FLAME = 0xFFFFC850, FLAME_CORE = 0xFFFFF4C0, CANDLE_GLOW = 0xFFFFB850;

    /** Lantern flame position (hero units). */
    static final float LX = 298f, LY = 391f;
    private static final float L = Hero.WIN_L, T = Hero.WIN_T, R = Hero.WIN_R, B = Hero.WIN_B;

    private Palette pal = Palette.at(Palette.DAY);
    private boolean armed = true;
    private final Decor decor = new Decor();

    private final Shape flame = new Shape(), flameCore = new Shape(), spark = new Shape();
    private Ink iFlame, iFlameCore, iHalo, iPool, iGlint;

    public WindowFrame() {
        flame.moveTo(0, 2.6f).cubicTo(-4.4f, 2.6f, -3.8f, -3, 0, -9.4f).cubicTo(3.8f, -3, 4.4f, 2.6f, 0, 2.6f).close();
        flameCore.moveTo(0, 2f).cubicTo(-2.2f, 2f, -1.9f, -1.2f, 0, -4.4f).cubicTo(1.9f, -1.2f, 2.2f, 2f, 0, 2f).close();
        Art.sparkle(spark, 0, 0, 1f);
        setHour(Palette.DAY);
    }

    /** Sets the time of day. Allocates; call only when the hour has changed noticeably. */
    public void setHour(float hour) {
        pal = Palette.at(hour);
        decor.setHour(hour);
        iFlame = Ink.solid(FLAME); iFlameCore = Ink.solid(FLAME_CORE);
        iHalo = Art.glow(LX, LY, 60f, CANDLE_GLOW, 0.5f);
        iPool = Art.glow(LX, LY, 150f, CANDLE_GLOW, 0.5f);
        iGlint = Ink.solid(0xFFFFFFFF);
    }

    /** Whether the alarm is switched on: lights the lantern on the sill. Changes both layers. */
    public void setArmed(boolean armed) { this.armed = armed; }

    // =====================================================================================

    /** STATIC. The whole interior for the current hour and armed state, with the window opening left clear. */
    public void drawStatic(Pen p) {
        Painter g = new Painter(p, pal, Painter.ROOM);
        wall(g);
        casements(g);
        frame(g);
        beamAndHerbs(g);
        sill(g);
        books(g);
        bottles(g);
        mortar(g);
        lantern(g);
        roomLight(g);
    }

    private void wall(Painter g) {
        Pen p = g.p;
        float[][] rects = {{0, 0, Hero.W, 36}, {0, B, Hero.W, Hero.H}, {0, 36, 30, B}, {330, 36, Hero.W, B}};
        for (int i = 0; i < rects.length; i++) {
            p.save();
            p.clipRect(rects[i][0], rects[i][1], rects[i][2], rects[i][3]);
            decor.wall(p, Hero.W, Hero.H, 0f);
            p.restore();
        }
        // soft shadow the frame and sill cast on the plaster
        g.rect(14, 26, 22, B, TIMBER_DEEP, 0.10f); g.rect(338, 26, 346, B, TIMBER_DEEP, 0.10f);
        p.fillRect(0, Hero.SILL_BOTTOM, Hero.W, Hero.SILL_BOTTOM + 16f, Art.vgrad(Hero.SILL_BOTTOM, Hero.SILL_BOTTOM + 16f,
            Ink.withAlpha(g.c(TIMBER_DEEP), 0.34f), Ink.withAlpha(g.c(TIMBER_DEEP), 0f)));
        // corbels holding up the sill
        for (int s = 0; s < 2; s++) {
            float x = s == 0 ? 40f : 320f, d = s == 0 ? 1f : -1f, y = Hero.SILL_BOTTOM;
            Shape c = new Shape().moveTo(x - 8f * d, y).lineTo(x + 10f * d, y).cubicTo(x + 9f * d, y + 12f, x + 2f * d, y + 22f, x - 8f * d, y + 26f).close();
            g.fill(c, TIMBER_DARK);
            Shape c2 = new Shape().moveTo(x - 6f * d, y).lineTo(x + 7f * d, y).cubicTo(x + 6f * d, y + 10f, x + 1f * d, y + 18f, x - 6f * d, y + 22f).close();
            g.fill(c2, TIMBER);
            g.oval(x - 1f * d, y + 8f, 2.2f, 2.2f, TIMBER_DARK, 0.7f);
        }
    }

    private Shape opening() {
        return new Shape().moveTo(L, B).lineTo(R, B).lineTo(R, 66).quadTo(180, 22, L, 66).close();
    }

    private void frame(Painter g) {
        Pen p = g.p;
        Shape f = new Shape();
        // slightly wonky outer edge, clockwise; the opening is wound the other way to leave a hole
        f.moveTo(21, 30.5f).quadTo(100, 28.6f, 180, 30f).quadTo(270, 31.4f, 339.4f, 29.4f)
            .quadTo(340.4f, 200, 338.6f, B).lineTo(22.4f, B).quadTo(20.4f, 220, 21, 30.5f).close();
        f.moveTo(L, B).lineTo(R, B).lineTo(R, 66).quadTo(180, 22, L, 66).close();
        p.fill(f, Ink.linear(0, 30, 0, B, new int[]{g.c(TIMBER_LIGHT), g.c(TIMBER), g.c(TIMBER_DARK)}, new float[]{0f, 0.25f, 1f}));
        p.save();
        p.clipShape(f);
        g.rng.seed(301);
        // grain: long wavering lines following each timber
        // (the arched head is one piece with the grain running across; the jambs run down)
        p.save();
        p.clipRect(0, 68, Hero.W, B);
        for (int i = 0; i < 7; i++) {
            float x = 23.5f + i * 2.1f + g.rng.s() * 0.4f;
            g.curve(i % 3 == 0 ? TIMBER_DEEP : TIMBER_DARK, 0.7f, 0.4f, x, 60, x + g.rng.s() * 1.4f, 150, x + g.rng.s() * 1.4f, 280, x + g.rng.s(), B);
            float x2 = 325.5f + i * 2.1f + g.rng.s() * 0.4f;
            g.curve(i % 3 == 0 ? TIMBER_DEEP : TIMBER_DARK, 0.7f, 0.4f, x2, 60, x2 + g.rng.s() * 1.4f, 150, x2 + g.rng.s() * 1.4f, 280, x2 + g.rng.s(), B);
        }
        p.restore();
        p.save();
        p.clipRect(0, 0, Hero.W, 67);
        for (int i = 0; i < 12; i++) {
            float y = 32f + i * 3f;
            g.curve(i % 3 == 0 ? TIMBER_DEEP : TIMBER_DARK, 0.7f, 0.38f, 22, y + g.rng.s(), 110, y + g.rng.s() * 1.5f - 1f, 250, y + g.rng.s() * 1.5f - 1f, 338, y + g.rng.s());
        }
        g.line(22, 67, 36, 67, TIMBER_DEEP, 0.9f, 0.6f); g.line(324, 67, 338, 67, TIMBER_DEEP, 0.9f, 0.6f);
        p.restore();
        // knots
        knot(g, 28.6f, 150f, 2.6f); knot(g, 331f, 250f, 2.2f); knot(g, 120f, 37f, 2.4f); knot(g, 272f, 41f, 2f);
        // lighter top face of each timber, darker lower edges
        g.rect(21, 29, 339, 32.4f, TIMBER_LIGHT, 0.55f);
        g.rect(21, 29, 24.4f, B, TIMBER_LIGHT, 0.4f);
        g.rect(335.6f, 29, 340, B, TIMBER_DEEP, 0.3f);
        p.restore();
        // the reveal: a sunlit inner edge, then a dark line where glass would sit
        Shape o = opening();
        g.stroke(o, TIMBER_DEEP, 3.4f, 0.75f);
        p.save();
        p.clipShape(f);
        p.stroke(o, Ink.solid(Ink.withAlpha(Ink.mix(g.c(TIMBER_LIGHT), pal.lit(0xFFFFF4D0), 0.5f), 0.85f)), 2.2f);
        p.restore();
        g.stroke(f, TIMBER_DEEP, 1.2f, 0.7f);
        // wooden pegs pinning the joints
        float[] px = {28.4f, 331.6f, 28.4f, 331.6f}, py = {38f, 38f, 386f, 386f};
        for (int i = 0; i < 4; i++) { g.oval(px[i], py[i], 2.6f, 2.6f, TIMBER_DEEP, 0.8f); g.oval(px[i] - 0.5f, py[i] - 0.5f, 1.7f, 1.7f, TIMBER_LIGHT, 0.9f); }
    }

    private void knot(Painter g, float x, float y, float r) {
        g.oval(x, y, r * 1.6f, r * 2.2f, TIMBER_DARK, 0.55f);
        g.oval(x, y, r, r * 1.3f, TIMBER_DEEP, 0.7f);
    }

    /** The two casements, swung inward and seen edge-on-ish, hugging the sides of the view. */
    private void casements(Painter g) {
        Pen p = g.p;
        for (int s = 0; s < 2; s++) {
            float d = s == 0 ? 1f : -1f, hx = s == 0 ? L + 1f : R - 1f, fx = hx + 25f * d;
            float ht = 70f, hb = B - 3f, ft = 58f, fb = B + 5f;
            Shape glass = new Shape().moveTo(hx, ht).lineTo(fx, ft).lineTo(fx, fb).lineTo(hx, hb).close();
            // old glass: a faint milky tint with slanting reflections
            p.fill(glass, Ink.linear(hx, ht, fx, fb, new int[]{Ink.withAlpha(pal.lit(0xFFE8F6FF), 0.30f), Ink.withAlpha(pal.lit(0xFFE8F6FF), 0.10f), Ink.withAlpha(pal.lit(0xFFE8F6FF), 0.24f)}, new float[]{0f, 0.5f, 1f}));
            p.save();
            p.clipShape(glass);
            for (int i = 0; i < 3; i++) {
                float y = 110f + i * 104f;
                Shape r = new Shape().moveTo(hx, y).lineTo(fx, y - 30f).lineTo(fx, y - 14f).lineTo(hx, y + 16f).close();
                p.fill(r, Ink.solid(Ink.withAlpha(0xFFFFFFFF, 0.16f + 0.05f * (1f - pal.night))));
            }
            p.restore();
            // glazing bars, following the perspective
            for (int i = 1; i < 4; i++) {
                float k = i / 4f;
                g.line(hx, Art.lerp(ht, hb, k), fx, Art.lerp(ft, fb, k), TIMBER_DARK, 2.6f);
                g.line(hx, Art.lerp(ht, hb, k) - 0.6f, fx, Art.lerp(ft, fb, k) - 0.6f, TIMBER_LIGHT, 0.9f, 0.7f);
            }
            // stiles and rails
            g.line(hx, ht, fx, ft, TIMBER_DARK, 4.4f); g.line(hx, hb, fx, fb, TIMBER_DARK, 4.4f);
            g.line(fx, ft, fx, fb, TIMBER_DARK, 5f);
            g.line(fx - 0.9f * d, ft + 2f, fx - 0.9f * d, fb - 2f, TIMBER_LIGHT, 1.5f, 0.8f);
            g.line(hx + 2f * d, ht - 0.6f, fx - 2f * d, ft - 0.6f, TIMBER_LIGHT, 1.2f, 0.7f);
            // iron latch
            g.line(fx, 228f, fx - 7f * d, 232f, IRON, 2.4f);
            g.oval(fx, 228f, 2.6f, 2.6f, IRON);
            g.oval(fx - 0.6f, 227.4f, 1f, 1f, 0xFF9C8C84);
        }
    }

    private void beamAndHerbs(Painter g) {
        Pen p = g.p;
        // heavy lintel beam across the top
        p.fillRect(0, 0, Hero.W, 23f, Ink.linear(0, 0, 0, 23f, new int[]{g.c(TIMBER_DARK), g.c(TIMBER), g.c(TIMBER_DEEP)}, new float[]{0f, 0.45f, 1f}));
        g.rng.seed(311);
        for (int i = 0; i < 10; i++) {
            float y = 2f + i * 2.1f;
            g.curve(i % 2 == 0 ? TIMBER_DEEP : TIMBER_LIGHT, 0.7f, 0.35f, 0, y + g.rng.s(), 120, y + g.rng.s() * 1.6f, 240, y + g.rng.s() * 1.6f, 360, y + g.rng.s());
        }
        knot(g, 148f, 11f, 2.6f); knot(g, 236f, 14f, 2f);
        p.fillRect(0, 23f, Hero.W, 31f, Art.vgrad(23f, 31f, Ink.withAlpha(g.c(TIMBER_DEEP), 0.4f), Ink.withAlpha(g.c(TIMBER_DEEP), 0f)));
        // bundles of drying herbs hung from iron hooks
        bundle(g, 58f, 20f, 66f, 0xFF8A9A5C, 0xFFB08CD8, 0);
        bundle(g, 84f, 20f, 44f, 0xFFA8A060, 0xFFF0C860, 1);
        bundle(g, 304f, 20f, 60f, 0xFF7E9A62, 0xFF9CB878, 2);
        bundle(g, 278f, 20f, 40f, 0xFF9C9458, 0xFFE89C8C, 1);
        // a sun-catcher crystal on a thread
        g.line(250f, 22f, 250f, 80f, 0xFFE8E0D0, 0.6f, 0.8f);
        Shape c = new Shape().moveTo(250f, 80f).lineTo(255f, 87f).lineTo(250f, 99f).lineTo(245f, 87f).close();
        p.fill(c, Ink.linear(245f, 80f, 255f, 99f, new int[]{Ink.withAlpha(0xFFFFFFFF, 0.9f), Ink.withAlpha(pal.lit(0xFFB8E4F8), 0.8f), Ink.withAlpha(pal.lit(0xFFE0B8F0), 0.85f)}, new float[]{0f, 0.5f, 1f}));
        g.line(250f, 80f, 250f, 99f, 0xFFFFFFFF, 0.5f, 0.7f);
        g.line(245f, 87f, 255f, 87f, 0xFFFFFFFF, 0.5f, 0.6f);
    }

    /** An upside-down bunch: kind 0 lavender spikes, 1 round flower heads, 2 leafy sprigs. */
    private void bundle(Painter g, float x, float y, float len, int leaf, int flower, int kind) {
        g.rng.seed((int) (x * 7));
        g.line(x, y - 4f, x, y + 8f, 0xFFD8C8A8, 0.8f);
        g.oval(x, y - 2f, 1.8f, 1.8f, IRON);
        int n = 9;
        for (int i = 0; i < n; i++) {
            float a = (i - (n - 1) / 2f) * 5.2f + g.rng.s() * 2f;
            double ra = Math.toRadians(90f + a);
            float l2 = len * g.rng.r(0.78f, 1f);
            float ex = x + (float) Math.cos(ra) * l2, ey = y + 8f + (float) Math.sin(ra) * l2;
            g.line(x + a * 0.06f, y + 8f, ex, ey, Ink.mix(leaf, 0xFF6C5C3C, 0.4f), 0.9f);
            if (kind == 0) {
                for (int k = 0; k < 6; k++) {
                    float u = 1f - k * 0.055f;
                    g.oval(x + (ex - x) * u + (k % 2 == 0 ? -1f : 1f), y + 8f + (ey - y - 8f) * u, 1.5f, 1.9f, Ink.mix(flower, 0xFF7C64B4, g.rng.f() * 0.5f));
                }
            } else if (kind == 1) {
                g.oval(ex, ey, 3f, 2.8f, Ink.mix(flower, 0xFFB07840, 0.25f));
                g.oval(ex - 0.6f, ey - 0.6f, 1.7f, 1.5f, flower);
                g.leaf(x + (ex - x) * 0.55f, y + 8f + (ey - y - 8f) * 0.55f, 6f, 2.6f, 90f + a + (i % 2 == 0 ? 40f : -40f), leaf, 0.95f);
            } else {
                for (int k = 2; k < 8; k++) {
                    float u = k / 8f;
                    g.leaf(x + (ex - x) * u, y + 8f + (ey - y - 8f) * u, 6.5f, 2.8f, 90f + a + (k % 2 == 0 ? 46f : -46f), Ink.mix(leaf, flower, g.rng.f()), 0.95f);
                }
            }
        }
        // twine binding
        g.rect(x - 3.4f, y + 9f, x + 3.4f, y + 13.4f, 0xFFD8C090);
        g.line(x - 3.4f, y + 11f, x + 3.4f, y + 11.4f, 0xFFA08858, 0.6f);
    }

    private void sill(Painter g) {
        Pen p = g.p;
        float st = Hero.SILL_TOP, sf = Hero.SILL_FRONT, sb = Hero.SILL_BOTTOM;
        Shape top = new Shape().moveTo(24, st).lineTo(336, st).lineTo(356, sf).lineTo(4, sf).close();
        p.fill(top, Ink.linear(0, st, 0, sf, new int[]{g.c(TIMBER), g.c(TIMBER_LIGHT)}, new float[]{0f, 1f}));
        p.save();
        p.clipShape(top);
        g.rng.seed(321);
        for (int i = 0; i < 9; i++) {
            float y = st + 1.6f + i * 2.5f;
            g.curve(i % 2 == 0 ? TIMBER_DARK : TIMBER_DEEP, 0.7f, 0.4f, 4, y + g.rng.s(), 120, y + g.rng.s() * 1.4f, 240, y + g.rng.s() * 1.4f, 356, y + g.rng.s());
        }
        knot(g, 150f, 409f, 2.2f); knot(g, 226f, 404f, 1.6f);
        // light from the window lying across the middle of the sill
        int spill = Ink.mix(pal.lit(0xFFFFF6D8), 0xFFC8D8FF, pal.night);
        g.glowOval(180f, st + 4f, 130f, 16f, spill, 0.5f - 0.25f * pal.night);
        g.rect(24, st, 336, st + 2.4f, TIMBER_DEEP, 0.4f);
        p.restore();
        // rounded front edge
        Shape front = new Shape().roundRect(3, sf - 1f, 357, sb, 4.5f);
        p.fill(front, Ink.linear(0, sf, 0, sb, new int[]{g.c(TIMBER_LIGHT), g.c(TIMBER), g.c(TIMBER_DARK)}, new float[]{0f, 0.3f, 1f}));
        for (int i = 0; i < 4; i++) {
            float y = sf + 2.4f + i * 2.4f;
            g.curve(TIMBER_DEEP, 0.7f, 0.35f, 6, y, 130, y + g.rng.s(), 250, y + g.rng.s(), 354, y);
        }
        g.stroke(front, TIMBER_DEEP, 1.1f, 0.6f);
        g.line(6, sf - 0.4f, 354, sf - 0.4f, 0xFFE0B888, 1f, 0.6f);
        // blown-in petals
        float[] qx = {150, 176, 214, 232, 131}, qy = {404, 411, 402, 412, 413};
        for (int i = 0; i < qx.length; i++) {
            p.fill(Art.petal(new Shape(), qx[i], qy[i], 5f, 3.2f, i * 70f), Ink.solid(g.c(i % 2 == 0 ? 0xFFFFDCE8 : 0xFFF8BCD0)));
        }
    }

    private void books(Painter g) {
        Pen p = g.p;
        // shadow under the stack
        g.oval(70f, 414.5f, 30f, 3.4f, TIMBER_DEEP, 0.4f);
        book(g, 46f, 403f, 94f, 414f, 0xFF3C7C86, 0xFFE8C468, false);
        p.save(); p.rotate(-2.5f, 70f, 398f);
        book(g, 51f, 393f, 89f, 403f, 0xFF964E6E, 0xFFF0D8A0, true);
        p.restore();
        // potted herb on top
        float px = 70f, py = 392.4f;
        g.rng.seed(331);
        for (int i = 0; i < 9; i++) {
            float a = -90f + (i - 4) * 13f + g.rng.s() * 5f, len = g.rng.r(20f, 34f);
            double ra = Math.toRadians(a);
            float ex = px + (float) Math.cos(ra) * len, ey = py - 14f + (float) Math.sin(ra) * len;
            g.curve(0xFF5C8C48, 1f, 1f, px + (i - 4) * 0.8f, py - 14f, (px + ex) / 2f + g.rng.s() * 2f, (py - 14f + ey) / 2f, ex, ey);
            for (int k = 2; k <= 5; k++) {
                float u = k / 5f, x = px + (ex - px) * u, y = py - 14f + (ey - py + 14f) * u;
                int col = Ink.mix(0xFF5C9C4C, 0xFFA4D070, g.rng.f());
                g.leaf(x, y, 7.4f, 4.4f, a - 55f, col, 1f);
                g.leaf(x, y, 7.4f, 4.4f, a + 55f, Ink.mix(col, 0xFF3C7C40, 0.3f), 1f);
            }
            if (i % 3 == 1) g.blossom(ex, ey, 2.6f, 0, 0xFFFFF4FA, 1f);
        }
        Shape pot = new Shape().moveTo(px - 11f, py - 16f).lineTo(px + 11f, py - 16f).lineTo(px + 8f, py).lineTo(px - 8f, py).close();
        p.fill(pot, Ink.linear(px - 11f, 0, px + 11f, 0, new int[]{g.c(0xFFE09468), g.c(TERRACOTTA), g.c(0xFF9C5438)}, new float[]{0f, 0.45f, 1f}));
        g.fill(new Shape().roundRect(px - 12.4f, py - 19f, px + 12.4f, py - 14.4f, 1.6f), 0xFFD88860);
        g.line(px - 11f, py - 18f, px + 4f, py - 18f, 0xFFF4B48C, 1f, 0.8f);
        g.line(px - 6.4f, py - 11f, px - 5f, py - 3f, 0xFFF4B48C, 1.4f, 0.5f);
    }

    private void book(Painter g, float l, float t, float r, float b, int cover, int trim, boolean pagesRight) {
        g.fill(new Shape().roundRect(l, t, r, b, 1.8f), cover);
        // page block showing at one end
        float pl = pagesRight ? r - 5f : l + 1.4f, pr = pagesRight ? r - 1.4f : l + 5f;
        g.rect(pl, t + 1.6f, pr, b - 1.6f, CREAM);
        for (int i = 0; i < 3; i++) g.line(pl, t + 3f + i * 2.2f, pr, t + 3f + i * 2.2f, 0xFFC8B890, 0.4f);
        // spine bands and a little emblem
        float s0 = pagesRight ? l + 6f : r - 6f, s1 = pagesRight ? l + 10f : r - 10f;
        g.line(s0, t + 1f, s0, b - 1f, trim, 1.2f); g.line(s1, t + 1f, s1, b - 1f, trim, 0.8f);
        g.blossom((l + r) / 2f, (t + b) / 2f, 2.6f, 0, trim, 0.9f);
        g.rect(l + 1.5f, t + 0.6f, r - 1.5f, t + 2f, 0xFFFFFFFF, 0.22f);
        g.stroke(new Shape().roundRect(l, t, r, b, 1.8f), TIMBER_DEEP, 0.8f, 0.55f);
    }

    private void bottles(Painter g) {
        Pen p = g.p;
        // round-bellied flask of something green
        float x = 104f, by = 414f;
        g.oval(x + 1f, by, 11f, 2.6f, TIMBER_DEEP, 0.4f);
        Shape fl = new Shape();
        fl.moveTo(x - 2.6f, by - 26f).lineTo(x - 2.6f, by - 18f).cubicTo(x - 11f, by - 16f, x - 11f, by - 1f, x - 5f, by)
            .lineTo(x + 5f, by).cubicTo(x + 11f, by - 1f, x + 11f, by - 16f, x + 2.6f, by - 18f).lineTo(x + 2.6f, by - 26f).close();
        p.fill(fl, Ink.solid(Ink.withAlpha(pal.lit(0xFFE8F8F8), 0.42f)));
        p.save();
        p.clipShape(fl);
        p.fillRect(x - 12f, by - 11f, x + 12f, by, Ink.linear(0, by - 11f, 0, by, new int[]{Ink.withAlpha(g.c(0xFF7CD8A8), 0.92f), Ink.withAlpha(g.c(0xFF3C9C7C), 0.95f)}, new float[]{0f, 1f}));
        g.oval(x, by - 11f, 9.6f, 1.5f, 0xFFB4F0CC, 0.9f);
        g.oval(x - 3f, by - 5f, 1f, 1f, 0xFFD8FFE8, 0.8f); g.oval(x + 3.6f, by - 7.4f, 0.7f, 0.7f, 0xFFD8FFE8, 0.8f);
        p.restore();
        g.stroke(fl, 0xFF4C6C74, 0.9f, 0.6f);
        g.curve(0xFFFFFFFF, 1.5f, 0.75f, x - 6.6f, by - 13f, x - 7.8f, by - 9f, x - 6.4f, by - 4.6f);
        g.fill(new Shape().roundRect(x - 3.6f, by - 30f, x + 3.6f, by - 25f, 1.2f), 0xFFC89C68);
        g.line(x - 3f, by - 24.4f, x + 3f, by - 24.4f, 0xFFD8C090, 1.2f);
        // squat little vial with a rosy tincture
        float vx = 116.6f;
        g.oval(vx, by, 5.4f, 1.6f, TIMBER_DEEP, 0.35f);
        Shape v = new Shape().roundRect(vx - 4.4f, by - 11f, vx + 4.4f, by, 2.4f);
        p.fill(v, Ink.solid(Ink.withAlpha(pal.lit(0xFFF8E8F0), 0.45f)));
        p.save(); p.clipShape(v);
        p.fillRect(vx - 5f, by - 7f, vx + 5f, by, Ink.solid(Ink.withAlpha(g.c(0xFFF07CA4), 0.92f)));
        p.restore();
        g.stroke(v, 0xFF74506C, 0.8f, 0.6f);
        g.rect(vx - 2f, by - 14.6f, vx + 2f, by - 10.6f, 0xFFC89C68);
        g.line(vx - 2.6f, by - 8.6f, vx - 2.6f, by - 3f, 0xFFFFFFFF, 1f, 0.7f);
    }

    private void mortar(Painter g) {
        Pen p = g.p;
        float x = 262f, by = 414f;
        g.oval(x + 1f, by, 15f, 3f, TIMBER_DEEP, 0.4f);
        // pestle leaning out
        g.line(x - 4f, by - 10f, x + 14f, by - 27f, 0xFF8C8C88, 5.4f);
        g.line(x - 4.6f, by - 10.6f, x + 13.4f, by - 27.6f, 0xFFD0D0C8, 2.6f);
        g.oval(x + 14.6f, by - 27.6f, 3.6f, 3.6f, 0xFFB0B0AA);
        Shape bowl = new Shape().moveTo(x - 14f, by - 14f).lineTo(x + 14f, by - 14f).cubicTo(x + 13f, by - 4f, x + 8f, by - 2.4f, x + 6f, by - 2f)
            .lineTo(x + 7.4f, by).lineTo(x - 7.4f, by).lineTo(x - 6f, by - 2f).cubicTo(x - 8f, by - 2.4f, x - 13f, by - 4f, x - 14f, by - 14f).close();
        p.fill(bowl, Ink.linear(x - 14f, 0, x + 14f, 0, new int[]{g.c(0xFFD8D8D0), g.c(0xFFB4B4AC), g.c(0xFF86867E)}, new float[]{0f, 0.5f, 1f}));
        g.oval(x, by - 14f, 14f, 2.6f, 0xFFE4E4DC);
        g.oval(x, by - 13.8f, 11.6f, 1.8f, 0xFF7C8C5C);
        g.oval(x - 3f, by - 14.2f, 4f, 0.9f, 0xFFA4BC6C);
        g.stroke(bowl, 0xFF5C5C58, 0.8f, 0.5f);
        // a sprig lying beside it
        g.line(x - 20f, by - 1f, x - 30f, by - 4f, 0xFF6C8C48, 0.9f);
        for (int i = 0; i < 4; i++) g.leaf(x - 21f - i * 2.6f, by - 1.2f - i * 0.8f, 5f, 2.4f, i % 2 == 0 ? -140f : 150f, 0xFF86B45C, 1f);
    }

    /** The lantern: lit and warm when armed, cold and dark when not. */
    private void lantern(Painter g) {
        Pen p = g.p;
        float x = LX, by = 414f;
        g.oval(x + 1f, by + 0.6f, 17f, 3.2f, TIMBER_DEEP, 0.42f);
        if (armed) g.glowOval(x, by + 1f, 62f, 12f, CANDLE_GLOW, 0.6f);
        // glass
        Shape glass = new Shape().moveTo(x - 10f, by - 4f).lineTo(x - 11.4f, by - 41f).lineTo(x + 11.4f, by - 41f).lineTo(x + 10f, by - 4f).close();
        if (armed) {
            p.fill(glass, Ink.radial(x, LY, 26f, new int[]{0xFFFFF8D0, 0xF2FFD478, 0xE6F0A040}, new float[]{0f, 0.5f, 1f}));
        } else {
            p.fill(glass, Ink.linear(x - 11f, 0, x + 11f, 0, new int[]{Ink.withAlpha(g.c(0xFFC8D8E0), 0.75f), Ink.withAlpha(g.c(0xFF8C9CA8), 0.7f)}, new float[]{0f, 1f}));
        }
        // candle
        g.mode = armed ? Painter.RAW : Painter.ROOM;
        g.fill(new Shape().roundRect(x - 3.4f, by - 19f, x + 3.4f, by - 4f, 1.4f), armed ? 0xFFFFF4D8 : 0xFFD8D0C0);
        g.rect(x + 0.8f, by - 18f, x + 3.4f, by - 4f, armed ? 0xFFF4D8A0 : 0xFFA8A098, 0.6f);
        g.line(x, by - 19f, x, by - 21.6f, 0xFF3C2C24, 0.9f);
        g.mode = Painter.ROOM;
        if (!armed) g.line(x - 7.6f, by - 36f, x - 6.6f, by - 10f, 0xFFFFFFFF, 1.4f, 0.4f);
        // iron cage: posts, base, pitched cap and carrying ring
        g.line(x - 10f, by - 4f, x - 11.4f, by - 41f, IRON, 2.2f); g.line(x + 10f, by - 4f, x + 11.4f, by - 41f, IRON, 2.2f);
        g.line(x, by - 4f, x, by - 2f, IRON, 1f);
        g.line(x - 11f, by - 23f, x + 11f, by - 23f, IRON, 1.2f, armed ? 0.55f : 0.8f);
        g.fill(new Shape().roundRect(x - 13.4f, by - 5f, x + 13.4f, by, 1.6f), IRON);
        g.rect(x - 12f, by - 4.6f, x + 12f, by - 3.6f, 0xFF8C7C70, 0.7f);
        Shape cap = new Shape().moveTo(x - 14.6f, by - 40f).lineTo(x - 6f, by - 52f).lineTo(x + 6f, by - 52f).lineTo(x + 14.6f, by - 40f).close();
        g.fill(cap, IRON);
        g.line(x - 12f, by - 41.4f, x - 5f, by - 51f, 0xFF8C7C70, 0.9f, 0.8f);
        g.rect(x - 3f, by - 55f, x + 3f, by - 52f, IRON);
        g.stroke(new Shape().oval(x, by - 59f, 4f, 4.4f), IRON, 1.5f);
        g.oval(x - 13.4f, by - 41f, 1.4f, 1.4f, BRASS); g.oval(x + 13.4f, by - 41f, 1.4f, 1.4f, BRASS);
    }

    private void roomLight(Painter g) {
        Pen p = g.p;
        // steady part of the candle glow (the flicker is in the live layer)
        if (armed) {
            float a = 0.10f + 0.34f * pal.night;
            Shape mask = new Shape().moveTo(0, 0).lineTo(Hero.W, 0).lineTo(Hero.W, Hero.H).lineTo(0, Hero.H).close();
            mask.moveTo(L, B).lineTo(R, B).lineTo(R, 66).quadTo(180, 22, L, 66).close();
            p.save();
            p.clipShape(mask);
            p.fillRect(0, 0, Hero.W, Hero.H, holeGlow(a));
            p.restore();
        }
        // gentle shading into the corners of the room
        int dark = Ink.mix(0xFF3A2414, 0xFF140C10, pal.night);
        float a = 0.16f + 0.22f * pal.night;
        p.fillRect(0, 0, Hero.W, 36, Art.vgrad(0, 36, Ink.withAlpha(dark, a), Ink.withAlpha(dark, a * 0.3f)));
        p.fillRect(0, 440, Hero.W, Hero.H, Art.vgrad(440, Hero.H, Ink.withAlpha(dark, 0f), Ink.withAlpha(dark, a * 0.5f)));
    }

    /** Candle glow as an ink; painted only on the wall strips so the garden hole stays clear. */
    private Ink holeGlow(float a) {
        return Art.glow(LX, LY + 10f, 190f, CANDLE_GLOW, a);
    }

    // =====================================================================================

    /**
     * ANIMATED. Flickering lantern flame and its halo (only when armed), and glints on the
     * flask and the sun-catcher. {@code t} in seconds; loops smoothly.
     */
    public void drawLive(Pen p, double t) {
        if (armed) {
            float f = 0.5f * Art.wave(t, 0.37) + 0.3f * Art.wave(t, 0.23) + 0.2f * Art.wave(t, 1.9);
            p.save();
            p.alpha(0.55f + 0.14f * f);
            p.fillOval(LX, LY, 60f, 60f, iHalo);
            p.restore();
            p.save();
            p.translate(LX + 0.5f * Art.wave(t, 0.61), LY + 1.4f);
            p.rotate(5f * Art.wave(t, 0.53) + 3f * f, 0, 2.6f);
            p.scale(1f + 0.06f * f, 1f + 0.14f * f, 0, 2.6f);
            p.fill(flame, iFlame);
            p.fill(flameCore, iFlameCore);
            p.restore();
        }
        // glints: flask, vial, crystal
        float day = 1f - pal.night;
        float gl = armed ? Math.max(day, 0.6f) : day * 0.9f + 0.1f;
        glint(p, 98f, 402f, 2.6f, gl * (0.45f + 0.55f * Art.hump(Art.frac(t / 5.2))));
        glint(p, 114.4f, 407f, 1.8f, gl * (0.3f + 0.7f * Art.hump(Art.frac(t / 6.4 + 0.4))));
        float sw = Art.wave(t, 7.0);
        glint(p, 250f + 2.2f * sw, 87f, 3.4f + 1.4f * Math.abs(sw), (0.35f + 0.65f * Math.abs(sw)) * (0.4f + 0.6f * day));
    }

    private void glint(Pen p, float x, float y, float r, float a) {
        p.save();
        p.alpha(Art.clamp01(a));
        p.translate(x, y);
        p.scale(r, r, 0, 0);
        p.fill(spark, iGlint);
        p.restore();
    }
}
