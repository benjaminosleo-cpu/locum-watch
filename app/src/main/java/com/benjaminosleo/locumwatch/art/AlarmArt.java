package com.benjaminosleo.locumwatch.art;

/**
 * Artwork for the full-screen alarm page, for any width and height.
 *
 * Layout (ask the *Rect methods; each fills {l, t, r, b}):
 *  - top: a small arched cottage window with Mochi bouncing excitedly at the sill
 *  - middle third: free for a parchment holding the sender and message ({@link #messageRect})
 *  - lower third: the big STOP button ({@link #stopRect}) and a small second button ({@link #secondaryRect})
 *
 * {@link #drawStatic} is STATIC (cache it); {@link #drawLive} is ANIMATED, about 75 draw calls.
 * The parchment itself comes from {@link Decor#parchment}; the buttons from
 * {@link #stopButton} and {@link #secondaryButton}.
 */
public final class AlarmArt {
    static final int STOP_RED = 0xFFE8463E, STOP_LIGHT = 0xFFFF7A62, STOP_DARK = 0xFFA81C2A, STOP_DEEP = 0xFF5E0E18, STOP_CREAM = 0xFFFFF4DC;
    static final int GO_GREEN = 0xFF2F7A5E, GO_LIGHT = 0xFF58A882, GO_DARK = 0xFF1C4C3C;

    private final GardenScene garden = new GardenScene();
    private final Decor decor = new Decor();
    private final Mochi mochi = new Mochi();
    private final MochiPose pose = new MochiPose();
    private Palette pal = Palette.at(Palette.DAY);

    private final Shape opening = new Shape(), flame = new Shape(), spark = new Shape();
    private float builtW = -1f, builtH = -1f;
    private final float[] win = new float[4];
    private Ink iHalo, iFlame, iFlameCore, iSpark, iCandleGlow;

    public AlarmArt() {
        flame.moveTo(0, 2.6f).cubicTo(-4.4f, 2.6f, -3.8f, -3, 0, -9.4f).cubicTo(3.8f, -3, 4.4f, 2.6f, 0, 2.6f).close();
        Art.sparkle(spark, 0, 0, 1f);
        iFlame = Ink.solid(WindowFrame.FLAME); iFlameCore = Ink.solid(WindowFrame.FLAME_CORE); iSpark = Ink.solid(0xFFFFE890);
        setHour(Palette.DAY);
    }

    /** Sets the time of day shown through the window. Allocates; then redraw the static layer. */
    public void setHour(float hour) {
        pal = Palette.at(hour);
        garden.setHour(hour);
        decor.setHour(hour);
        mochi.setLight(pal);
        builtW = -1f;
    }

    /** The arched window opening. */
    public static void windowRect(float w, float h, float[] out) {
        float ww = Math.min(w * 0.66f, h * 0.42f), wh = h * 0.285f, top = h * 0.07f;
        out[0] = (w - ww) / 2f; out[1] = top; out[2] = (w + ww) / 2f; out[3] = top + wh;
    }

    /** Where the message parchment goes (middle third). */
    public static void messageRect(float w, float h, float[] out) {
        out[0] = 18f; out[1] = h * 0.405f; out[2] = w - 18f; out[3] = h * 0.67f;
    }

    /** Where the STOP button goes: at least 300 x 130 on a 360-wide screen. */
    public static void stopRect(float w, float h, float[] out) {
        float bw = Math.min(w - 40f, Math.max(300f, w * 0.88f)), bh = Math.max(130f, h * 0.215f);
        out[0] = (w - bw) / 2f; out[1] = h * 0.70f; out[2] = (w + bw) / 2f; out[3] = h * 0.70f + bh;
    }

    /** Where the smaller "stop and open WhatsApp" button goes. */
    public static void secondaryRect(float w, float h, float[] out) {
        float bw = Math.min(w - 80f, 240f), top = h * 0.70f + Math.max(130f, h * 0.215f) + 10f;
        out[0] = (w - bw) / 2f; out[1] = top; out[2] = (w + bw) / 2f; out[3] = Math.min(h - 6f, top + 40f);
    }

    private void build(float w, float h) {
        if (w == builtW && h == builtH) return;
        builtW = w; builtH = h;
        windowRect(w, h, win);
        float l = win[0], t = win[1], r = win[2], b = win[3], arch = (r - l) * 0.16f;
        opening.reset().moveTo(l, b).lineTo(r, b).lineTo(r, t + arch).quadTo((l + r) / 2f, t - arch, l, t + arch).close();
        float cx = (l + r) / 2f, cy = (t + b) / 2f;
        iHalo = Art.glow(cx, cy, (r - l) * 1.05f, 0xFFFFC860, 0.75f);
        iCandleGlow = Art.glow(0, 0, 46f, WindowFrame.CANDLE_GLOW, 0.6f);
    }

    /** Scale that maps hero units onto this window (the view is a close crop of the hero's lower window). */
    private float crop() { return (win[2] - win[0]) / 236f; }

    private void intoGarden(Pen p) {
        float k = crop();
        p.translate((win[0] + win[2]) / 2f - 180f * k, win[3] - Hero.WIN_B * k);
        p.scale(k, k, 0, 0);
    }

    /** STATIC. Warm candle-lit wall, the window with the garden beyond, its frame and sill, and two wall candles. */
    public void drawStatic(Pen p, float w, float h) {
        build(w, h);
        Painter g = new Painter(p, pal, Painter.ROOM);
        float l = win[0], t = win[1], r = win[2], b = win[3], cx = (l + r) / 2f;
        decor.wall(p, w, h, 0f);
        // the whole room glows warm while the alarm rings
        p.fillRect(0, 0, w, h, Art.vgrad(0, h, 0x40FFB860, 0x18FF9040));
        p.fillOval(cx, (t + b) / 2f, w, w, Art.glow(cx, (t + b) / 2f, w, 0xFFFFD080, 0.5f));
        // garden through the opening
        p.save();
        p.clipShape(opening);
        intoGarden(p);
        garden.drawBackground(p);
        p.restore();
        // timber frame: outer board with the opening cut out
        float th = 13f, arch = (r - l) * 0.16f;
        Shape f = new Shape();
        f.moveTo(l - th, t - th * 0.4f).quadTo(cx, t - th * 0.9f, r + th, t - th * 0.5f).quadTo(r + th + 1f, (t + b) / 2f, r + th - 0.6f, b)
            .lineTo(l - th + 0.6f, b).quadTo(l - th - 1f, (t + b) / 2f, l - th, t - th * 0.4f).close();
        f.moveTo(l, b).lineTo(r, b).lineTo(r, t + arch).quadTo(cx, t - arch, l, t + arch).close();
        p.save(); p.translate(1.5f, 3f); p.fill(f, Ink.solid(0x30301808)); p.restore();
        p.fill(f, Ink.linear(0, t - th, 0, b, new int[]{g.c(WindowFrame.TIMBER_LIGHT), g.c(WindowFrame.TIMBER), g.c(WindowFrame.TIMBER_DARK)}, new float[]{0f, 0.3f, 1f}));
        p.save();
        p.clipShape(f);
        g.rng.seed(901);
        for (int i = 0; i < 6; i++) {
            float x = l - th + 1.6f + i * 2.1f, x2 = r + 1.6f + i * 2.1f;
            g.curve(WindowFrame.TIMBER_DEEP, 0.7f, 0.35f, x, t + arch, x + g.rng.s(), (t + b) / 2f, x + g.rng.s(), b);
            g.curve(WindowFrame.TIMBER_DEEP, 0.7f, 0.35f, x2, t + arch, x2 + g.rng.s(), (t + b) / 2f, x2 + g.rng.s(), b);
        }
        for (int i = 0; i < 9; i++) {
            float y = t - th * 0.6f + i * 3f;
            p.save(); p.clipRect(0, 0, w, t + arch);
            g.curve(WindowFrame.TIMBER_DEEP, 0.7f, 0.33f, l - th, y + g.rng.s(), cx, y - 2f + g.rng.s(), r + th, y + g.rng.s());
            p.restore();
        }
        p.restore();
        g.stroke(opening, WindowFrame.TIMBER_DEEP, 3f, 0.75f);
        p.save(); p.clipShape(f);
        p.stroke(opening, Ink.solid(Ink.withAlpha(Ink.mix(g.c(WindowFrame.TIMBER_LIGHT), pal.lit(0xFFFFF4D0), 0.5f), 0.85f)), 2f);
        p.restore();
        g.stroke(f, WindowFrame.TIMBER_DEEP, 1.1f, 0.7f);
        // sill
        Shape top = new Shape().moveTo(l - th - 4f, b).lineTo(r + th + 4f, b).lineTo(r + th + 14f, b + 12f).lineTo(l - th - 14f, b + 12f).close();
        p.fill(top, Art.vgrad(b, b + 12f, g.c(WindowFrame.TIMBER), g.c(WindowFrame.TIMBER_LIGHT)));
        Shape front = new Shape().roundRect(l - th - 15f, b + 11f, r + th + 15f, b + 20f, 3.6f);
        p.fill(front, Art.vgrad(b + 11f, b + 20f, g.c(WindowFrame.TIMBER_LIGHT), g.c(WindowFrame.TIMBER_DARK)));
        g.stroke(front, WindowFrame.TIMBER_DEEP, 1f, 0.6f);
        p.fillRect(l - th - 15f, b + 20f, r + th + 15f, b + 32f, Art.vgrad(b + 20f, b + 32f, Ink.withAlpha(g.c(WindowFrame.TIMBER_DEEP), 0.3f), Ink.withAlpha(g.c(WindowFrame.TIMBER_DEEP), 0f)));
        // wall candles either side
        for (int s = 0; s < 2; s++) sconce(g, sconceX(w, s), sconceY());
    }

    private float sconceX(float w, int side) { return side == 0 ? Math.max(24f, win[0] * 0.42f) : w - Math.max(24f, win[0] * 0.42f); }
    private float sconceY() { return (win[1] + win[3]) / 2f + 14f; }

    private void sconce(Painter g, float x, float y) {
        g.glow(x, y - 20f, 60f, WindowFrame.CANDLE_GLOW, 0.4f);
        g.fill(new Shape().roundRect(x - 2.4f, y - 2f, x + 2.4f, y + 24f, 2f), WindowFrame.IRON);
        g.oval(x, y + 24f, 4f, 4f, WindowFrame.IRON);
        g.oval(x, y, 10f, 3f, WindowFrame.BRASS); g.oval(x, y - 0.8f, 8f, 2f, 0xFFF0CC78);
        g.mode = Painter.RAW;
        g.fill(new Shape().roundRect(x - 3.6f, y - 18f, x + 3.6f, y - 0.6f, 1.4f), 0xFFFFF4D8);
        g.rect(x + 0.8f, y - 17f, x + 3.6f, y - 0.6f, 0xFFF0D8A8, 0.7f);
        g.mode = Painter.ROOM;
        g.line(x, y - 18f, x, y - 20.4f, 0xFF3C2C24, 0.9f);
    }

    /**
     * ANIMATED. Mochi bouncing at the window with paws on the sill, a pulsing halo round the
     * window, flickering candles and a few sparkles. Draw over the static layer every frame.
     */
    public void drawLive(Pen p, float w, float h, double t) {
        build(w, h);
        float l = win[0], tp = win[1], r = win[2], b = win[3], cx = (l + r) / 2f, cy = (tp + b) / 2f;
        // halo pulsing with the ring of the alarm
        float pulse = 0.5f + 0.5f * Art.wave(t, 0.92);
        p.save();
        p.alpha(0.28f + 0.3f * pulse);
        p.scale(1f + 0.06f * pulse, 1f + 0.06f * pulse, cx, cy);
        p.fillOval(cx, cy, (r - l) * 1.05f, (r - l) * 1.05f, iHalo);
        p.restore();
        // Mochi, clipped to the opening
        MochiAnim.windowExcited(pose, (float) (t % 1000.0));
        pose.x = Hero.RING_X; pose.y = Hero.AT_WINDOW_Y; pose.scale = Hero.AT_WINDOW_SCALE; pose.facing = 1f;
        p.save();
        p.clipShape(opening);
        intoGarden(p);
        garden.drawLiveFront(p, t);
        mochi.draw(p, pose);
        p.restore();
        p.save();
        intoGarden(p);
        mochi.drawOverSill(p, pose);
        p.restore();
        // candles
        for (int s = 0; s < 2; s++) {
            float x = sconceX(w, s), y = sconceY() - 20f;
            float f = 0.5f * Art.wave(t + s * 0.3, 0.37) + 0.3f * Art.wave(t + s, 0.23) + 0.2f * Art.wave(t, 1.9);
            p.save();
            p.translate(x, y);
            p.save(); p.alpha(0.5f + 0.2f * f); p.fillOval(0, -4f, 46f, 46f, iCandleGlow); p.restore();
            p.rotate(5f * Art.wave(t + s, 0.53), 0, 2.6f);
            p.scale(1.1f + 0.06f * f, 1.1f + 0.16f * f, 0, 2.6f);
            p.fill(flame, iFlame);
            p.scale(0.5f, 0.5f, 0, 2f);
            p.fill(flame, iFlameCore);
            p.restore();
        }
        // sparkles popping round the frame
        for (int i = 0; i < 6; i++) {
            float k = Art.frac(t / 1.3 + i / 6.0);
            double a = i * 1.05 + 0.4;
            float rx = (r - l) * 0.62f, ry = (b - tp) * 0.66f;
            p.save();
            p.alpha(Art.hump(k));
            p.translate(cx + (float) Math.cos(a) * rx * (1f + 0.12f * k), cy - 6f + (float) Math.sin(a) * ry * (1f + 0.12f * k));
            p.scale(3f + 5f * Art.hump(k), 3f + 5f * Art.hump(k), 0, 0);
            p.fill(spark, iSpark);
            p.restore();
        }
    }

    // ------------------------------------------------------------------ buttons

    /**
     * STATIC. The STOP button: a huge raised red slab with cut corners, a cream border and a
     * raised paw at each end (a "halt" signal that does not need words). The middle is left
     * clear for the word; cream or white text reads best. {@code pressed} sinks it into its base.
     */
    public void stopButton(Pen p, float l, float t, float r, float b, boolean pressed) {
        float depth = 11f, sink = pressed ? 8f : 0f;
        float ft = t + sink, fb = b - depth + sink * 0.25f, cut = Math.min((fb - ft) * 0.3f, 36f);
        Shape base = slab(l, ft + 6f, r, b, cut);
        if (!pressed) { p.save(); p.translate(0f, 5f); p.fill(slab(l + 2f, ft + 8f, r - 2f, b, cut), Ink.solid(0x44200008)); p.restore(); }
        p.fill(base, Art.vgrad(ft, b, STOP_DARK, STOP_DEEP));
        Shape face = slab(l, ft, r, fb, cut);
        int top = pressed ? Ink.mix(STOP_RED, STOP_DARK, 0.25f) : STOP_LIGHT, mid = pressed ? Ink.mix(STOP_RED, STOP_DARK, 0.4f) : STOP_RED;
        p.fill(face, Ink.linear(0, ft, 0, fb, new int[]{top, mid, Ink.mix(mid, STOP_DARK, 0.45f)}, new float[]{0f, 0.35f, 1f}));
        p.save();
        p.clipShape(face);
        if (pressed) {
            p.fillRect(l, ft, r, ft + 16f, Art.vgrad(ft, ft + 16f, 0x70300008, 0x00300008));
        } else {
            // glossy band across the top
            Shape gloss = new Shape().moveTo(l, ft).lineTo(r, ft).lineTo(r, ft + (fb - ft) * 0.30f).quadTo((l + r) / 2f, ft + (fb - ft) * 0.46f, l, ft + (fb - ft) * 0.30f).close();
            p.fill(gloss, Ink.solid(0x2CFFFFFF));
        }
        p.restore();
        float in = 9f;
        p.stroke(slab(l + in, ft + in, r - in, fb - in, cut * 0.76f), Ink.solid(Ink.withAlpha(STOP_CREAM, pressed ? 0.8f : 1f)), 3.2f);
        p.stroke(face, Ink.solid(STOP_DEEP), 1.6f);
        float py = (ft + fb) / 2f + 2f, ps = Math.min(1.5f, (fb - ft) / 80f);
        paw(p, l + 46f, py, ps, -10f, pressed);
        paw(p, r - 46f, py, ps, 10f, pressed);
    }

    private static Shape slab(float l, float t, float r, float b, float cut) {
        float k = cut * 0.25f;
        return new Shape().moveTo(l + cut, t).lineTo(r - cut, t).quadTo(r - k, t + k, r, t + cut).lineTo(r, b - cut).quadTo(r - k, b - k, r - cut, b)
            .lineTo(l + cut, b).quadTo(l + k, b - k, l, b - cut).lineTo(l, t + cut).quadTo(l + k, t + k, l + cut, t).close();
    }

    /** A cream paw print: one big pad and four toes. */
    private void paw(Pen p, float x, float y, float s, float rot, boolean dim) {
        Ink ink = Ink.solid(Ink.withAlpha(STOP_CREAM, dim ? 0.75f : 0.95f));
        p.save();
        p.translate(x, y);
        p.rotate(rot, 0, 0);
        p.scale(s, s, 0, 0);
        Shape pad = Art.blob(new Shape(), 0, -4, 9, 1, 8, 9, 0, 11, -8, 9, -9, 1);
        p.fill(pad, ink);
        p.fillOval(-11.5f, -8f, 3.6f, 4.6f, ink); p.fillOval(-4.2f, -14.5f, 3.8f, 5f, ink);
        p.fillOval(4.2f, -14.5f, 3.8f, 5f, ink); p.fillOval(11.5f, -8f, 3.6f, 4.6f, ink);
        p.restore();
    }

    /**
     * STATIC. The smaller "stop and open WhatsApp" button: a forest-green pill with a cream
     * rim and a leaf at each end. Cream text reads best.
     */
    public void secondaryButton(Pen p, float l, float t, float r, float b, boolean pressed) {
        float depth = 5f, sink = pressed ? 3.5f : 0f, ft = t + sink, fb = b - depth + sink * 0.3f, rad = (fb - ft) / 2f;
        if (!pressed) p.fill(new Shape().roundRect(l + 1f, ft + 5f, r - 1f, b + 3f, rad), Ink.solid(0x38200808));
        p.fill(new Shape().roundRect(l, ft + 3f, r, b, rad), Ink.solid(GO_DARK));
        Shape face = new Shape().roundRect(l, ft, r, fb, rad);
        int top = pressed ? GO_GREEN : GO_LIGHT, bot = pressed ? Ink.mix(GO_GREEN, GO_DARK, 0.5f) : GO_GREEN;
        p.fill(face, Art.vgrad(ft, fb, top, bot));
        p.stroke(new Shape().roundRect(l + 4f, ft + 4f, r - 4f, fb - 4f, rad - 4f), Ink.solid(Ink.withAlpha(STOP_CREAM, 0.85f)), 1.4f);
        p.stroke(face, Ink.solid(0xFF123428), 1.2f);
        float cy = (ft + fb) / 2f;
        for (int i = 0; i < 2; i++) {
            float x = i == 0 ? l + rad + 2f : r - rad - 2f;
            p.fill(Art.leaf(new Shape(), x, cy + 4f, 9f, 5f, i == 0 ? -120f : -60f), Ink.solid(0xFFCDE6A6));
            p.fill(Art.leaf(new Shape(), x, cy + 4f, 8f, 4.4f, i == 0 ? -50f : -130f), Ink.solid(0xFFA9CC82));
        }
    }
}
