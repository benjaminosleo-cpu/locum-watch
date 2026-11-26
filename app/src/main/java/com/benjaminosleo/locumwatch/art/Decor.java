package com.benjaminosleo.locumwatch.art;

/**
 * Cottage-interior pieces for the scrolling settings page. Everything here is STATIC:
 * draw each piece once as a view background (or into a bitmap) at whatever size is needed.
 * Text is not drawn; each method's comment says where text can sit and what colour suits it.
 *
 * All methods allocate, so do not call them every frame.
 */
public final class Decor {
    static final int PLASTER = 0xFFF4E8CE, PLASTER_SHADE = 0xFFE2D0AC, PLASTER_LIGHT = 0xFFFCF5E2;
    static final int PARCH = 0xFFFAF0D4, PARCH_EDGE = 0xFFE2C990, PARCH_STAIN = 0xFFD2B070;
    static final int PLANK = 0xFFD6AC78, PLANK_DARK = 0xFFA87C4C, PLANK_LIGHT = 0xFFECC898;
    static final int GOOD = 0xFFA9CC82, GOOD_DARK = 0xFF6E9A52, GOOD_LIGHT = 0xFFCDE6A6;
    static final int WARN = 0xFFE6846A, WARN_DARK = 0xFFB4523C, WARN_LIGHT = 0xFFF6B09A;
    static final int INK_BROWN = 0xFF5A3C28;

    /** Button looks for {@link #plankButton}. */
    public static final int BUTTON_NORMAL = 0, BUTTON_GOOD = 1, BUTTON_ATTENTION = 2;
    /** Shelf vignettes for {@link #shelf}. */
    public static final int SHELF_APOTHECARY = 0, SHELF_LIBRARY = 1, SHELF_BREWING = 2, SHELF_COUNT = 3;
    /** Room a shelf needs above and below the y you give it (the top of the board). */
    public static final float SHELF_ABOVE = 80f, SHELF_BELOW = 22f;
    private static final float ITEM_SCALE = 1.36f;
    /** The wall texture repeats every this many units vertically. */
    public static final float WALL_TILE = 256f;
    /** The wall pattern repeats exactly every this many units, so one bitmap of this height tiles seamlessly. */
    public static final float WALL_PERIOD = 2 * WALL_TILE;

    private Palette pal = Palette.at(Palette.DAY);

    public Decor() {}

    /** Lights the pieces for a time of day (dimmer and warmer at night). Use the same hour as the hero so the wall joins up. */
    public void setHour(float hour) { pal = Palette.at(hour); }

    private Painter room(Pen p) { return new Painter(p, pal, Painter.ROOM); }
    private Painter soft(Pen p) { return new Painter(p, pal, Painter.ROOM_SOFT); }

    // ------------------------------------------------------------------ wall

    /**
     * STATIC. Plaster wall with timber uprights at the left and right edges, filling (0,0)-(w,h).
     * {@code offsetY} is how far down the page this piece starts, so separately drawn pieces
     * (the hero at 0, the settings below it) join without a seam. Labels drawn straight on the
     * wall read best in dark brown by day.
     */
    public void wall(Pen p, float w, float h, float offsetY) {
        Painter g = room(p);
        p.save();
        p.clipRect(0, 0, w, h);
        g.rect(0, 0, w, h, PLASTER);
        int first = (int) Math.floor(offsetY / WALL_TILE) - 1, last = (int) Math.floor((offsetY + h) / WALL_TILE) + 1;
        for (int tile = first; tile <= last; tile++) {
            float y0 = tile * WALL_TILE - offsetY;
            g.rng.seed(7001 + (tile & 1) * 13); // two alternating tiles: the wall repeats exactly every 2 tiles
            // soft uneven patches where the plaster was trowelled
            for (int i = 0; i < 14; i++) {
                float x = g.rng.r(0, w), y = y0 + g.rng.r(0, WALL_TILE), rx = g.rng.r(30f, 90f), ry = g.rng.r(10f, 30f);
                g.glowOval(x, y, rx, ry, g.c(g.rng.chance(0.5f) ? PLASTER_LIGHT : PLASTER_SHADE), g.rng.r(0.25f, 0.5f));
            }
            // trowel strokes
            for (int i = 0; i < 26; i++) {
                float x = g.rng.r(12f, w - 12f), y = y0 + g.rng.r(0, WALL_TILE), len = g.rng.r(10f, 34f);
                g.curve(g.rng.chance(0.5f) ? PLASTER_SHADE : PLASTER_LIGHT, g.rng.r(0.8f, 1.8f), 0.35f, x, y, x + len * 0.5f, y + g.rng.s() * 2.4f, x + len, y + g.rng.s() * 2f);
            }
            // freckles and one hairline crack
            for (int i = 0; i < 40; i++) g.oval(g.rng.r(0, w), y0 + g.rng.r(0, WALL_TILE), 0.6f, 0.6f, PLASTER_SHADE, 0.6f);
            float cx = g.rng.r(40f, w - 60f), cy = y0 + g.rng.r(20f, WALL_TILE - 40f);
            g.curve(0xFFC4AE88, 0.6f, 0.45f, cx, cy, cx + 5f, cy + 7f, cx + 3f, cy + 13f, cx + 9f, cy + 21f);
        }
        // timber uprights
        int posts = Math.max(2, 1 + Math.round(w / 420f));
        for (int i = 0; i < posts; i++) {
            float x = i == 0 ? 0f : (i == posts - 1 ? w - 9f : w * i / (posts - 1) - 4.5f);
            upright(g, x, x + 9f, h, offsetY, i == 0);
        }
        p.restore();
    }

    private void upright(Painter g, float l, float r, float h, float offsetY, boolean leftEdge) {
        Pen p = g.p;
        p.fillRect(l, 0, r, h, Ink.linear(l, 0, r, 0, new int[]{g.c(WindowFrame.TIMBER), g.c(WindowFrame.TIMBER_LIGHT), g.c(WindowFrame.TIMBER_DARK)}, new float[]{0f, 0.4f, 1f}));
        for (int k = 0; k < 4; k++) {
            float x = l + 1.6f + k * 2.1f;
            g.line(x, 0, x, h, k % 2 == 0 ? WindowFrame.TIMBER_DEEP : WindowFrame.TIMBER_DARK, 0.6f, 0.4f);
        }
        int first = (int) Math.floor(offsetY / WALL_TILE) - 1, last = (int) Math.floor((offsetY + h) / WALL_TILE) + 1;
        for (int tile = first; tile <= last; tile++) {
            float y = tile * WALL_TILE - offsetY + 60f + Art.hash((tile & 1) * 7 + (leftEdge ? 1 : 5)) * 130f;
            g.oval((l + r) / 2f, y, 2.4f, 3.6f, WindowFrame.TIMBER_DARK, 0.6f);
            g.oval((l + r) / 2f, y, 1.3f, 2f, WindowFrame.TIMBER_DEEP, 0.7f);
        }
        float e = leftEdge ? r : l - 4f;
        p.fillRect(e, 0, e + 4f, h, Ink.linear(e, 0, e + 4f, 0, new int[]{Ink.withAlpha(g.c(WindowFrame.TIMBER_DEEP), leftEdge ? 0.22f : 0f), Ink.withAlpha(g.c(WindowFrame.TIMBER_DEEP), leftEdge ? 0f : 0.22f)}, new float[]{0f, 1f}));
    }

    // ------------------------------------------------------------------ parchment

    /**
     * STATIC. A sheet of aged paper with deckled edges, pinned to the wall, filling the
     * rectangle (the soft shadow spills about 4 units below and to the right).
     * Keep text 14 units in from the edges; dark brown ink reads best. {@code seed} varies
     * the edge wobble and stains so neighbouring cards differ.
     */
    public void parchment(Pen p, float l, float t, float r, float b, int seed) {
        Painter g = soft(p);
        Shape s = Art.wobblyRect(new Shape(), l + 1f, t + 1f, r - 1f, b - 1f, 1.5f, seed);
        p.save(); p.translate(1.6f, 3.2f); p.fill(s, Ink.solid(0x30402814)); p.restore();
        p.save(); p.translate(0.6f, 1.4f); p.fill(s, Ink.solid(0x38402814)); p.restore();
        g.fill(s, PARCH);
        p.save();
        p.clipShape(s);
        // darker, toasted edges; the middle stays light and calm for text
        g.stroke(s, PARCH_EDGE, 9f, 0.35f);
        g.stroke(s, PARCH_EDGE, 4f, 0.5f);
        g.rng.seed(seed * 31 + 5);
        for (int i = 0; i < 4; i++) {
            float x = g.rng.chance(0.5f) ? l + g.rng.r(0f, 26f) : r - g.rng.r(0f, 26f);
            float y = g.rng.chance(0.5f) ? t + g.rng.r(0f, 20f) : b - g.rng.r(0f, 20f);
            g.glowOval(x, y, g.rng.r(14f, 30f), g.rng.r(8f, 16f), g.c(PARCH_STAIN), 0.3f);
        }
        // faint paper fibres
        for (int i = 0; i < (int) ((r - l) * (b - t) / 900f) + 6; i++) {
            float x = g.rng.r(l + 6f, r - 14f), y = g.rng.r(t + 6f, b - 6f);
            g.line(x, y, x + g.rng.r(3f, 8f), y + g.rng.s() * 1.4f, PARCH_STAIN, 0.5f, 0.22f);
        }
        p.restore();
        g.stroke(s, 0xFFC4A46C, 0.8f, 0.7f);
        // turned-up corner
        Shape fold = new Shape().moveTo(r - 13f, b - 1.4f).lineTo(r - 1.4f, b - 12f).lineTo(r - 10.4f, b - 9.6f).close();
        g.fill(fold, 0xFFFFF8E4);
        g.stroke(fold, 0xFFC4A46C, 0.7f, 0.7f);
        // brass tacks
        tack(g, l + 9f, t + 8f); tack(g, r - 9f, t + 8f);
    }

    private void tack(Painter g, float x, float y) {
        g.oval(x + 0.7f, y + 1.3f, 3f, 3f, 0xFF402814, 0.3f);
        g.oval(x, y, 2.8f, 2.8f, 0xFFB8862C);
        g.oval(x, y, 2.2f, 2.2f, WindowFrame.BRASS);
        g.oval(x - 0.8f, y - 0.8f, 0.9f, 0.9f, 0xFFFFF0B8);
    }

    // ------------------------------------------------------------------ buttons

    /**
     * STATIC. A wooden plank button filling the rectangle (allow 5 units at the bottom for its
     * thickness and shadow; put the label in the top part). Styles: BUTTON_NORMAL plain wood,
     * BUTTON_GOOD sage-green paint (done / all well), BUTTON_ATTENTION warm red paint (needs
     * doing). {@code pressed} sinks it. Dark brown text reads on all three.
     */
    public void plankButton(Pen p, float l, float t, float r, float b, int style, boolean pressed) {
        Painter g = soft(p);
        int base = style == BUTTON_GOOD ? GOOD : (style == BUTTON_ATTENTION ? WARN : PLANK);
        int dark = style == BUTTON_GOOD ? GOOD_DARK : (style == BUTTON_ATTENTION ? WARN_DARK : PLANK_DARK);
        int light = style == BUTTON_GOOD ? GOOD_LIGHT : (style == BUTTON_ATTENTION ? WARN_LIGHT : PLANK_LIGHT);
        float depth = 4.5f, sink = pressed ? 3f : 0f;
        float ft = t + sink, fb = b - depth + sink * 0.5f;
        if (pressed) { base = Ink.mix(base, dark, 0.45f); light = Ink.mix(light, dark, 0.5f); }
        int seed = Math.round(l * 3 + t * 7 + r);
        Shape edge = Art.wobblyRect(new Shape(), l, ft + 2f, r, b, 0.6f, seed);
        if (!pressed) { p.save(); p.translate(0.6f, 2.2f); p.fill(edge, Ink.solid(0x34402814)); p.restore(); }
        g.fill(edge, Ink.mix(dark, 0xFF402814, 0.25f));
        Shape face = Art.wobblyRect(new Shape(), l, ft, r, fb, 0.7f, seed);
        p.fill(face, Ink.linear(0, ft, 0, fb, new int[]{g.c(light), g.c(base), g.c(Ink.mix(base, dark, 0.35f))}, new float[]{0f, 0.3f, 1f}));
        p.save();
        p.clipShape(face);
        g.rng.seed(seed);
        // wood grain showing through the paint
        int lines = Math.max(3, (int) ((fb - ft) / 5f));
        for (int i = 0; i < lines; i++) {
            float y = ft + (fb - ft) * (i + 0.5f) / lines + g.rng.s();
            g.curve(dark, 0.7f, 0.34f, l, y, l + (r - l) * 0.33f, y + g.rng.s() * 2f, l + (r - l) * 0.66f, y + g.rng.s() * 2f, r, y + g.rng.s());
        }
        float kx = l + (r - l) * g.rng.r(0.7f, 0.86f), ky = ft + (fb - ft) * g.rng.r(0.3f, 0.7f);
        g.oval(kx, ky, 3.4f, 2f, dark, 0.3f); g.oval(kx, ky, 1.6f, 0.9f, dark, 0.45f);
        g.rect(l, ft, r, ft + 1.6f, 0xFFFFFFFF, pressed ? 0.08f : 0.3f);
        if (pressed) g.p.fillRect(l, ft, r, ft + 7f, Art.vgrad(ft, ft + 7f, 0x40301808, 0x00301808));
        p.restore();
        g.stroke(face, Ink.mix(dark, 0xFF402814, 0.45f), 1f, 0.8f);
        // iron nail heads at each end
        float ny = (ft + fb) / 2f;
        for (int i = 0; i < 2; i++) {
            float nx = i == 0 ? l + 8f : r - 8f;
            g.oval(nx + 0.4f, ny + 0.7f, 2.2f, 2.2f, 0xFF301808, 0.35f);
            g.oval(nx, ny, 2f, 2f, WindowFrame.IRON);
            g.oval(nx - 0.6f, ny - 0.6f, 0.7f, 0.7f, 0xFFB8A89C);
        }
    }

    /**
     * STATIC. Section header: a small wooden sign hanging on two cords from the top of the
     * rectangle. The board takes the lower three quarters; one line of text goes centred on it,
     * in cream (the wood is dark).
     */
    public void sectionHeader(Pen p, float l, float t, float r, float b) {
        Painter g = room(p);
        float bt = t + (b - t) * 0.26f, bb = b - 3f, mid = (bt + bb) / 2f;
        // cords from one nail
        float nx = (l + r) / 2f;
        g.line(nx, t + 2f, l + 20f, bt + 3f, 0xFFC8B088, 1.3f); g.line(nx, t + 2f, r - 20f, bt + 3f, 0xFFC8B088, 1.3f);
        g.oval(nx, t + 2.4f, 2.4f, 2.4f, WindowFrame.IRON); g.oval(nx - 0.6f, t + 1.8f, 0.8f, 0.8f, 0xFFB8A89C);
        // board with arrow-notched ends
        Shape s = new Shape().moveTo(l + 8f, bt).lineTo(r - 8f, bt + 0.6f).lineTo(r - 1f, mid).lineTo(r - 8f, bb).lineTo(l + 8f, bb - 0.6f).lineTo(l + 1f, mid).close();
        p.save(); p.translate(0.8f, 2.6f); p.fill(s, Ink.solid(0x38402814)); p.restore();
        p.fill(s, Ink.linear(0, bt, 0, bb, new int[]{g.c(0xFF9C6E48), g.c(0xFF7E5636), g.c(0xFF684428)}, new float[]{0f, 0.4f, 1f}));
        p.save();
        p.clipShape(s);
        g.rng.seed(Math.round(l + t * 3));
        for (int i = 0; i < 5; i++) {
            float y = bt + (bb - bt) * (i + 0.5f) / 5f;
            g.curve(WindowFrame.TIMBER_DEEP, 0.7f, 0.35f, l, y + g.rng.s(), (l + r) / 2f, y + g.rng.s() * 1.6f, r, y + g.rng.s());
        }
        g.rect(l, bt, r, bt + 1.5f, 0xFFE0B888, 0.5f);
        p.restore();
        g.stroke(s, WindowFrame.TIMBER_DEEP, 1f, 0.8f);
        // a painted sprig at each end
        for (int i = 0; i < 2; i++) {
            float x = i == 0 ? l + 16f : r - 16f, d = i == 0 ? 1f : -1f;
            g.leaf(x, mid + 1f, 6f, 3f, i == 0 ? -40f : -140f, 0xFFA8CC78, 0.95f);
            g.leaf(x, mid + 1f, 6f, 3f, i == 0 ? 40f : 140f, 0xFF8CB868, 0.95f);
            g.blossom(x - 1.6f * d, mid + 1f, 2.4f, 0, 0xFFFFD8E4, 1f);
        }
    }

    // ------------------------------------------------------------------ shelves

    /**
     * STATIC. A wall shelf used as a divider: the top of the board is at {@code y}, spanning
     * {@code x} to {@code x + width}. Things on it rise up to SHELF_ABOVE above y; brackets hang
     * SHELF_BELOW below. {@code kind} is SHELF_APOTHECARY (jars, candle, sprig), SHELF_LIBRARY
     * (books, crystals, scroll) or SHELF_BREWING (cauldron, potion bottles, mushrooms).
     */
    public void shelf(Pen p, int kind, float x, float y, float width) {
        Painter g = room(p);
        float r = x + width;
        // things first, then the board over their feet
        // (drawn a third larger than modelled, spread over the same width)
        float k = ITEM_SCALE, ix = x + width / 2f - width / (2f * k);
        p.save();
        p.scale(k, k, x + width / 2f, y);
        if (kind == SHELF_LIBRARY) library(g, ix, y, width / k);
        else if (kind == SHELF_BREWING) brewing(g, ix, y, width / k);
        else apothecary(g, ix, y, width / k);
        p.restore();
        board(g, x, r, y);
    }

    private void board(Painter g, float l, float r, float y) {
        Pen p = g.p;
        p.fillRect(l - 2f, y + 7f, r + 2f, y + 16f, Art.vgrad(y + 7f, y + 16f, Ink.withAlpha(g.c(WindowFrame.TIMBER_DEEP), 0.3f), Ink.withAlpha(g.c(WindowFrame.TIMBER_DEEP), 0f)));
        for (int i = 0; i < 2; i++) {
            float bx = i == 0 ? l + 16f : r - 16f;
            Shape c = new Shape().moveTo(bx - 4f, y + 6f).lineTo(bx + 4f, y + 6f).lineTo(bx + 3f, y + 20f).cubicTo(bx + 1f, y + 22f, bx - 1f, y + 22f, bx - 3f, y + 20f).close();
            g.fill(c, WindowFrame.TIMBER_DARK);
            g.line(bx - 2f, y + 7f, bx - 1.6f, y + 19f, WindowFrame.TIMBER_LIGHT, 1f, 0.6f);
            g.oval(bx, y + 16f, 1.2f, 1.2f, WindowFrame.IRON);
        }
        Shape b = Art.wobblyRect(new Shape(), l, y, r, y + 7f, 0.5f, Math.round(l + y));
        p.fill(b, Ink.linear(0, y, 0, y + 7f, new int[]{g.c(WindowFrame.TIMBER_LIGHT), g.c(WindowFrame.TIMBER), g.c(WindowFrame.TIMBER_DARK)}, new float[]{0f, 0.35f, 1f}));
        g.line(l + 2f, y + 3.4f, r - 2f, y + 3.8f, WindowFrame.TIMBER_DEEP, 0.6f, 0.35f);
        g.line(l + 2f, y + 5.2f, r - 2f, y + 5f, WindowFrame.TIMBER_DEEP, 0.6f, 0.25f);
        g.stroke(b, WindowFrame.TIMBER_DEEP, 0.9f, 0.7f);
        g.line(l + 1f, y + 0.8f, r - 1f, y + 0.8f, 0xFFE8C090, 0.9f, 0.7f);
    }

    private void jar(Painter g, float x, float y, float w, float h, int content, int lid, boolean label) {
        Pen p = g.p;
        Shape s = new Shape().roundRect(x - w / 2f, y - h, x + w / 2f, y, w * 0.24f);
        g.oval(x + 1f, y - 0.4f, w * 0.6f, 1.6f, WindowFrame.TIMBER_DEEP, 0.35f);
        p.fill(s, Ink.solid(Ink.withAlpha(g.c(0xFFE8F4F0), 0.9f)));
        p.save(); p.clipShape(s);
        p.fillRect(x - w, y - h * 0.72f, x + w, y, Art.vgrad(y - h * 0.72f, y, g.c(Ink.mix(content, 0xFFFFFFFF, 0.25f)), g.c(content)));
        p.restore();
        if (label) {
            g.rect(x - w * 0.3f, y - h * 0.52f, x + w * 0.3f, y - h * 0.22f, 0xFFFAF0D4);
            g.line(x - w * 0.18f, y - h * 0.42f, x + w * 0.18f, y - h * 0.42f, INK_BROWN, 0.6f, 0.6f);
            g.line(x - w * 0.18f, y - h * 0.32f, x + w * 0.1f, y - h * 0.32f, INK_BROWN, 0.6f, 0.6f);
        }
        g.stroke(s, 0xFF5C6C68, 0.8f, 0.6f);
        g.line(x - w * 0.34f, y - h * 0.8f, x - w * 0.34f, y - h * 0.2f, 0xFFFFFFFF, 1.2f, 0.6f);
        g.fill(new Shape().roundRect(x - w * 0.4f, y - h - 4f, x + w * 0.4f, y - h + 1.4f, 1.4f), lid);
        g.rect(x - w * 0.4f, y - h - 4f, x + w * 0.4f, y - h - 2.8f, 0xFFFFFFFF, 0.25f);
    }

    private void candle(Painter g, float x, float y, float h, boolean lit) {
        if (lit) g.glow(x, y - h - 6f, 26f, WindowFrame.CANDLE_GLOW, 0.5f);
        g.oval(x, y - 1f, 8f, 2.2f, WindowFrame.BRASS); g.oval(x, y - 1.6f, 6.4f, 1.5f, 0xFFF0CC78);
        g.fill(new Shape().roundRect(x - 3.2f, y - h, x + 3.2f, y - 1f, 1.2f), 0xFFFBF0D8);
        g.rect(x + 0.8f, y - h + 1f, x + 3.2f, y - 1f, 0xFFE4CCA0, 0.7f);
        g.oval(x - 2.6f, y - h + 3f, 1.2f, 2.6f, 0xFFFBF0D8);
        g.line(x, y - h, x, y - h - 2.4f, 0xFF3C2C24, 0.8f);
        if (lit) {
            int old = g.mode; g.mode = Painter.RAW;
            Shape f = new Shape().moveTo(x, y - h - 0.6f).cubicTo(x - 3.2f, y - h - 0.6f, x - 2.6f, y - h - 5f, x, y - h - 10f)
                .cubicTo(x + 2.6f, y - h - 5f, x + 3.2f, y - h - 0.6f, x, y - h - 0.6f).close();
            g.fill(f, WindowFrame.FLAME);
            g.oval(x, y - h - 3f, 1.3f, 2.2f, WindowFrame.FLAME_CORE);
            g.mode = old;
        }
    }

    private void bookSpine(Painter g, float x, float y, float w, float h, float lean, int col, int trim) {
        Pen p = g.p;
        p.save();
        p.rotate(lean, x + (lean > 0 ? 0 : w), y);
        Shape s = new Shape().roundRect(x, y - h, x + w, y, 1.2f);
        p.fill(s, Ink.linear(x, 0, x + w, 0, new int[]{g.c(Ink.mix(col, 0xFFFFFFFF, 0.2f)), g.c(col), g.c(Ink.mix(col, 0xFF000000, 0.25f))}, new float[]{0f, 0.4f, 1f}));
        g.rect(x + 0.6f, y - h * 0.86f, x + w - 0.6f, y - h * 0.8f, trim); g.rect(x + 0.6f, y - h * 0.24f, x + w - 0.6f, y - h * 0.18f, trim);
        g.oval(x + w / 2f, y - h * 0.52f, w * 0.22f, w * 0.22f, trim);
        g.stroke(s, WindowFrame.TIMBER_DEEP, 0.7f, 0.5f);
        p.restore();
    }

    private void bottle(Painter g, float x, float y, float w, float h, int liquid, int shape) {
        Pen p = g.p;
        Shape s = new Shape();
        float nw = w * 0.2f, nh = h * 0.34f;
        if (shape == 0) { // round flask
            s.moveTo(x - nw, y - h).lineTo(x - nw, y - h + nh).cubicTo(x - w * 0.62f, y - h + nh + 2f, x - w * 0.62f, y - 0.6f, x - w * 0.24f, y)
                .lineTo(x + w * 0.24f, y).cubicTo(x + w * 0.62f, y - 0.6f, x + w * 0.62f, y - h + nh + 2f, x + nw, y - h + nh).lineTo(x + nw, y - h).close();
        } else if (shape == 1) { // cone
            s.moveTo(x - nw, y - h).lineTo(x - nw, y - h + nh).lineTo(x - w * 0.5f, y - 2f).quadTo(x - w * 0.5f, y, x - w * 0.4f, y)
                .lineTo(x + w * 0.4f, y).quadTo(x + w * 0.5f, y, x + w * 0.5f, y - 2f).lineTo(x + nw, y - h + nh).lineTo(x + nw, y - h).close();
        } else { // tall square-shouldered
            s.moveTo(x - nw, y - h).lineTo(x - nw, y - h + nh * 0.6f).quadTo(x - w * 0.5f, y - h + nh * 0.7f, x - w * 0.5f, y - h + nh * 1.2f)
                .lineTo(x - w * 0.5f, y).lineTo(x + w * 0.5f, y).lineTo(x + w * 0.5f, y - h + nh * 1.2f)
                .quadTo(x + w * 0.5f, y - h + nh * 0.7f, x + nw, y - h + nh * 0.6f).lineTo(x + nw, y - h).close();
        }
        g.oval(x + 1f, y - 0.4f, w * 0.55f, 1.6f, WindowFrame.TIMBER_DEEP, 0.35f);
        p.fill(s, Ink.solid(Ink.withAlpha(g.c(0xFFE4F2F4), 0.9f)));
        p.save(); p.clipShape(s);
        p.fillRect(x - w, y - h * 0.5f, x + w, y, Art.vgrad(y - h * 0.5f, y, g.c(Ink.mix(liquid, 0xFFFFFFFF, 0.3f)), g.c(liquid)));
        g.oval(x - w * 0.1f, y - h * 0.3f, 1f, 1f, 0xFFFFFFFF, 0.7f); g.oval(x + w * 0.16f, y - h * 0.18f, 0.7f, 0.7f, 0xFFFFFFFF, 0.7f);
        p.restore();
        g.stroke(s, 0xFF4C5C64, 0.8f, 0.6f);
        g.line(x - w * 0.3f, y - h * 0.44f, x - w * 0.34f, y - h * 0.14f, 0xFFFFFFFF, 1.2f, 0.7f);
        g.fill(new Shape().roundRect(x - nw - 0.8f, y - h - 3.4f, x + nw + 0.8f, y - h + 1f, 1f), 0xFFC89C68);
    }

    private void apothecary(Painter g, float x, float y, float w) {
        jar(g, x + w * 0.12f, y, 20f, 30f, 0xFF86B45C, 0xFFC89C68, true);
        jar(g, x + w * 0.24f, y, 16f, 22f, 0xFFE8B048, 0xFF9C6E48, false);
        candle(g, x + w * 0.40f, y, 26f, true);
        // a small bunch of lavender lying down
        float sx = x + w * 0.54f;
        for (int i = 0; i < 5; i++) {
            g.line(sx, y - 2f, sx + 30f, y - 3f - i * 1.4f, 0xFF7C9458, 0.8f);
            for (int k = 0; k < 4; k++) g.oval(sx + 20f + k * 3f, y - 3f - i * 1.4f + (k % 2), 1.6f, 1.3f, k % 2 == 0 ? 0xFFB08CD8 : 0xFF8C6CC0);
        }
        g.rect(sx + 3f, y - 5f, sx + 6f, y - 0.6f, 0xFFD8C090);
        jar(g, x + w * 0.76f, y, 18f, 34f, 0xFF9CB8E8, 0xFFC89C68, true);
        // stoneware pot of dried mushrooms
        float mx = x + w * 0.90f;
        mushroomDeco(g, mx - 5f, y - 14f, 0.9f, 0xFFD8644C); mushroomDeco(g, mx + 4f, y - 15f, 0.75f, 0xFFE8A048);
        Shape pot = new Shape().moveTo(mx - 11f, y - 14f).lineTo(mx + 11f, y - 14f).lineTo(mx + 8f, y).lineTo(mx - 8f, y).close();
        g.p.fill(pot, Ink.linear(mx - 11f, 0, mx + 11f, 0, new int[]{g.c(0xFFD8D0C0), g.c(0xFFB8AC98), g.c(0xFF8C8070)}, new float[]{0f, 0.5f, 1f}));
        g.rect(mx - 9.6f, y - 9f, mx + 9.6f, y - 7f, 0xFF6C8CB0, 0.8f);
    }

    private void mushroomDeco(Painter g, float x, float y, float s, int cap) {
        g.fill(new Shape().roundRect(x - 2f * s, y - 8f * s, x + 2f * s, y + 2f, 1.4f), 0xFFF4E8D0);
        g.blob(cap, 1f, x - 7f * s, y - 6f * s, x - 4f * s, y - 12f * s, x + 4f * s, y - 12f * s, x + 7f * s, y - 6f * s, x, y - 5f * s);
        g.oval(x - 2.4f * s, y - 9.6f * s, 1.2f * s, 0.9f * s, 0xFFFFF4E0); g.oval(x + 2.6f * s, y - 8.4f * s, 0.9f * s, 0.7f * s, 0xFFFFF4E0);
    }

    private void library(Painter g, float x, float y, float w) {
        int[] cols = {0xFF3C7C86, 0xFF964E6E, 0xFFC89048, 0xFF5C7CA8, 0xFF7C9C5C, 0xFFB45C4C, 0xFF6C5C94};
        float[] ws = {9f, 7f, 11f, 8f, 10f, 7f, 9f}, hs = {40f, 34f, 44f, 36f, 30f, 42f, 38f};
        float bx = x + w * 0.08f;
        for (int i = 0; i < 6; i++) {
            bookSpine(g, bx, y, ws[i], hs[i], i == 5 ? 9f : 0f, cols[i], 0xFFE8C468);
            bx += ws[i] + (i == 4 ? 4f : 0.6f);
        }
        // amethyst cluster
        float cx = x + w * 0.52f;
        float[] ox = {-9f, -3f, 4f, 10f}, hh = {14f, 24f, 18f, 11f}, ww = {5f, 6.4f, 5.6f, 4.6f}, lean = {-14f, -3f, 8f, 20f};
        for (int i = 0; i < 4; i++) {
            g.p.save(); g.p.rotate(lean[i], cx + ox[i], y);
            Shape c = new Shape().moveTo(cx + ox[i] - ww[i], y).lineTo(cx + ox[i] - ww[i], y - hh[i] * 0.7f).lineTo(cx + ox[i], y - hh[i]).lineTo(cx + ox[i] + ww[i], y - hh[i] * 0.7f).lineTo(cx + ox[i] + ww[i], y).close();
            g.p.fill(c, Ink.linear(cx + ox[i] - ww[i], 0, cx + ox[i] + ww[i], 0, new int[]{g.c(0xFFE4D0FC), g.c(0xFFB490E8), g.c(0xFF8460C0)}, new float[]{0f, 0.45f, 1f}));
            g.line(cx + ox[i], y - hh[i], cx + ox[i], y, 0xFFFFFFFF, 0.6f, 0.5f);
            g.stroke(c, 0xFF5C4490, 0.7f, 0.5f);
            g.p.restore();
        }
        g.p.fill(Art.sparkle(new Shape(), cx - 2f, y - 26f, 3.4f), Ink.solid(0xE6FFFFFF));
        // rolled scroll tied with ribbon
        float sx = x + w * 0.70f;
        g.fill(new Shape().roundRect(sx, y - 10f, sx + 40f, y - 0.6f, 4.6f), 0xFFF6E8C4);
        g.oval(sx + 2f, y - 5.3f, 2.6f, 4.6f, 0xFFE2C990); g.oval(sx + 2f, y - 5.3f, 1.2f, 2.6f, 0xFFC4A46C);
        g.rect(sx + 18f, y - 10.4f, sx + 22f, y - 0.4f, 0xFFD85C6C);
        g.stroke(new Shape().roundRect(sx, y - 10f, sx + 40f, y - 0.6f, 4.6f), 0xFFA88C58, 0.7f, 0.6f);
        // a feather quill in a pot
        float qx = x + w * 0.90f;
        g.curve(0xFFF4F0E4, 3.6f, 1f, qx, y - 14f, qx + 4f, y - 32f, qx + 12f, y - 46f);
        g.curve(0xFFC8D4E0, 1f, 0.9f, qx, y - 12f, qx + 4f, y - 32f, qx + 12f, y - 46f);
        g.fill(new Shape().roundRect(qx - 6f, y - 14f, qx + 6f, y, 2.4f), 0xFF3C4C6C);
        g.rect(qx - 6f, y - 14f, qx + 6f, y - 12f, 0xFF6C7C9C);
    }

    private void brewing(Painter g, float x, float y, float w) {
        Pen p = g.p;
        bottle(g, x + w * 0.10f, y, 20f, 30f, 0xFFF07CA4, 0);
        bottle(g, x + w * 0.21f, y, 12f, 38f, 0xFF7CC8E8, 2);
        // tiny cauldron with a bubbling brew
        float cx = x + w * 0.42f;
        g.glow(cx, y - 22f, 22f, 0xFFA8F088, 0.4f);
        g.oval(cx + 1f, y - 0.4f, 21f, 2.4f, WindowFrame.TIMBER_DEEP, 0.4f);
        g.line(cx - 12f, y - 3f, cx - 15f, y, 0xFF2C2828, 2.6f); g.line(cx + 12f, y - 3f, cx + 15f, y, 0xFF2C2828, 2.6f);
        Shape pot = new Shape().moveTo(cx - 17f, y - 20f).cubicTo(cx - 24f, y - 8f, cx - 14f, y - 1f, cx, y - 1f).cubicTo(cx + 14f, y - 1f, cx + 24f, y - 8f, cx + 17f, y - 20f).close();
        p.fill(pot, Ink.linear(cx - 20f, 0, cx + 20f, 0, new int[]{g.c(0xFF6C6468), g.c(0xFF3C383C), g.c(0xFF242024)}, new float[]{0f, 0.4f, 1f}));
        g.oval(cx, y - 20f, 18.4f, 3.6f, 0xFF4C4448);
        g.mode = Painter.ROOM_SOFT;
        g.oval(cx, y - 20f, 15.6f, 2.6f, 0xFF7CD868);
        g.oval(cx - 5f, y - 20.6f, 2.4f, 1f, 0xFFC8F8A8); g.oval(cx + 6f, y - 19.6f, 1.6f, 0.8f, 0xFFC8F8A8);
        g.oval(cx - 4f, y - 27f, 2.2f, 2.2f, 0xFFA8EC88, 0.8f); g.oval(cx + 3f, y - 33f, 1.6f, 1.6f, 0xFFA8EC88, 0.7f); g.oval(cx + 7f, y - 25f, 1.2f, 1.2f, 0xFFA8EC88, 0.7f);
        g.oval(cx - 4.6f, y - 27.6f, 0.7f, 0.7f, 0xFFFFFFFF, 0.9f);
        g.mode = Painter.ROOM;
        g.curve(0xFFA09498, 1f, 0.6f, cx - 15f, y - 16f, cx - 17f, y - 10f, cx - 13f, y - 5f);
        // mushrooms growing from a mossy log slice
        float mx = x + w * 0.63f;
        mushroomDeco(g, mx - 6f, y - 5f, 1.1f, 0xFFD8644C); mushroomDeco(g, mx + 5f, y - 5f, 0.8f, 0xFFD8644C); mushroomDeco(g, mx + 12f, y - 4f, 0.55f, 0xFFE8A048);
        g.fill(new Shape().roundRect(mx - 15f, y - 6f, mx + 18f, y, 2.6f), 0xFF8A6A4C);
        g.oval(mx - 4f, y - 6f, 9f, 1.8f, 0xFF86AC54); g.oval(mx + 10f, y - 5.6f, 5f, 1.4f, 0xFF9CBC5C);
        bottle(g, x + w * 0.80f, y, 18f, 26f, 0xFFB490E8, 1);
        bottle(g, x + w * 0.91f, y, 14f, 20f, 0xFFF0C048, 0);
        // a wooden star hung from a nail above
        float sx = x + w * 0.80f, sy = y - 50f;
        g.line(sx, sy - 9f, sx, sy - 3f, 0xFFC8B088, 0.8f);
        Shape star = new Shape();
        for (int i = 0; i < 10; i++) {
            double a = -Math.PI / 2 + i * Math.PI / 5; float rr = i % 2 == 0 ? 6f : 2.7f;
            float px = sx + (float) Math.cos(a) * rr, py = sy + 3f + (float) Math.sin(a) * rr;
            if (i == 0) star.moveTo(px, py); else star.lineTo(px, py);
        }
        star.close();
        g.fill(star, 0xFFF0CC68); g.stroke(star, 0xFFB8862C, 0.7f, 0.8f);
    }

    // ------------------------------------------------------------------ slider, toggle, field

    /**
     * STATIC. Slider track across the rectangle (drawn centred vertically, about 10 units
     * thick): a carved wooden groove, filled with a leafy green vine from the left up to
     * {@code fraction} (0..1). Leave half a thumb's width of margin inside l and r.
     */
    public void sliderTrack(Pen p, float l, float t, float r, float b, float fraction) {
        Painter g = soft(p);
        float cy = (t + b) / 2f, x = Art.lerp(l, r, Art.clamp01(fraction));
        Shape groove = new Shape().roundRect(l - 4f, cy - 5f, r + 4f, cy + 5f, 5f);
        p.fill(groove, Art.vgrad(cy - 5f, cy + 5f, g.c(0xFF7C5636), g.c(0xFFA87C50)));
        g.stroke(groove, WindowFrame.TIMBER_DEEP, 0.9f, 0.6f);
        g.line(l - 1f, cy + 4.2f, r + 1f, cy + 4.2f, 0xFFF0D0A0, 0.8f, 0.6f);
        if (x > l + 1f) {
            g.line(l, cy, x, cy, 0xFF5C8C40, 5.6f);
            g.line(l, cy - 0.8f, x, cy - 0.8f, 0xFF8CC060, 3f);
            for (float lx = l + 7f; lx < x - 4f; lx += 13f) {
                g.leaf(lx, cy - 0.5f, 6.4f, 3.4f, -42f, 0xFFB4DC80, 1f);
                g.leaf(lx + 6f, cy + 0.5f, 6.4f, 3.4f, 42f, 0xFF78B050, 1f);
            }
        }
        // notch marks
        for (int i = 0; i <= 4; i++) {
            float nx = Art.lerp(l, r, i / 4f);
            g.line(nx, cy + 8f, nx, cy + 11f, INK_BROWN, 1f, 0.4f);
        }
    }

    /** STATIC. Slider thumb: a round wooden knob with a brass rim and a blossom, centred at (cx,cy). */
    public void sliderThumb(Pen p, float cx, float cy, float radius, boolean pressed) {
        Painter g = soft(p);
        float r = radius * (pressed ? 0.94f : 1f);
        if (!pressed) g.oval(cx + 0.6f, cy + 2.4f, r, r, 0xFF301808, 0.3f);
        g.oval(cx, cy, r, r, 0xFFB8862C);
        p.fillOval(cx, cy, r - 1.6f, r - 1.6f, Ink.radial(cx - r * 0.3f, cy - r * 0.35f, r * 1.5f, new int[]{g.c(pressed ? 0xFFE0B888 : 0xFFF4D4A4), g.c(0xFFC8985C), g.c(0xFF9C6E40)}, new float[]{0f, 0.6f, 1f}));
        g.blossom(cx, cy, r * 0.52f, -90f, 0xFFFFD8E4, 1f);
        g.oval(cx, cy, r * 0.14f, r * 0.14f, 0xFFE8A048);
        g.stroke(new Shape().oval(cx, cy, r, r), 0xFF7C5424, 0.8f, 0.7f);
    }

    /**
     * STATIC. On/off switch filling the rectangle (best about 52 x 28): a wooden slot with a
     * sliding knob. ON: knob to the right, slot glowing amber with a small flame. OFF: knob to
     * the left, slot dark with a small crescent moon.
     */
    public void toggle(Pen p, float l, float t, float r, float b, boolean on) {
        Painter g = soft(p);
        float h = b - t, rad = h / 2f, cy = (t + b) / 2f;
        Shape slot = new Shape().roundRect(l, t, r, b, rad);
        if (on) {
            p.fill(slot, Art.vgrad(t, b, 0xFFF0A838, 0xFFFFD880));
        } else {
            p.fill(slot, Art.vgrad(t, b, g.c(0xFF5C4A3C), g.c(0xFF86705C)));
        }
        p.save(); p.clipShape(slot);
        p.fillRect(l, t, r, t + h * 0.3f, Art.vgrad(t, t + h * 0.3f, 0x50301808, 0x00301808));
        p.restore();
        g.stroke(slot, WindowFrame.TIMBER_DEEP, 1.2f, 0.75f);
        float kx = on ? r - rad : l + rad, ix = on ? l + rad * 1.05f : r - rad * 1.05f;
        if (on) {
            Shape f = new Shape().moveTo(ix, cy + rad * 0.5f).cubicTo(ix - rad * 0.5f, cy + rad * 0.5f, ix - rad * 0.4f, cy - rad * 0.1f, ix, cy - rad * 0.66f)
                .cubicTo(ix + rad * 0.4f, cy - rad * 0.1f, ix + rad * 0.5f, cy + rad * 0.5f, ix, cy + rad * 0.5f).close();
            p.fill(f, Ink.solid(0xFFFFF8D8));
        } else {
            Shape m = new Shape().moveTo(ix + rad * 0.16f, cy - rad * 0.5f).cubicTo(ix - rad * 0.6f, cy - rad * 0.44f, ix - rad * 0.6f, cy + rad * 0.44f, ix + rad * 0.16f, cy + rad * 0.5f)
                .cubicTo(ix - rad * 0.2f, cy + rad * 0.24f, ix - rad * 0.2f, cy - rad * 0.24f, ix + rad * 0.16f, cy - rad * 0.5f).close();
            g.fill(m, 0xFFC8BCB0, 0.8f);
        }
        float kr = rad - 2.6f;
        g.oval(kx + 0.4f, cy + 1.6f, kr, kr, 0xFF301808, 0.35f);
        g.oval(kx, cy, kr, kr, on ? 0xFFB8862C : 0xFF8C7458);
        p.fillOval(kx, cy, kr - 1.3f, kr - 1.3f, Ink.radial(kx - kr * 0.3f, cy - kr * 0.35f, kr * 1.5f, new int[]{g.c(0xFFF8E0B8), g.c(on ? 0xFFD8AC70 : 0xFFC0A078), g.c(0xFF9C7448)}, new float[]{0f, 0.6f, 1f}));
        g.oval(kx - kr * 0.3f, cy - kr * 0.34f, kr * 0.24f, kr * 0.16f, 0xFFFFFFFF, 0.6f);
    }

    /**
     * STATIC. Background for a text-entry field: a slightly sunken strip of paper with an ink
     * underline. {@code focused} warms the underline and adds a soft glow. Text sits 8 units in.
     */
    public void field(Pen p, float l, float t, float r, float b, boolean focused) {
        Painter g = soft(p);
        Shape s = new Shape().roundRect(l, t, r, b, 3.4f);
        if (focused) { p.save(); p.fill(new Shape().roundRect(l - 2f, t - 2f, r + 2f, b + 2f, 5f), Ink.solid(0x50FFC050)); p.restore(); }
        g.fill(s, 0xFFF2E2BC);
        p.save(); p.clipShape(s);
        p.fillRect(l, t, r, t + 5f, Art.vgrad(t, t + 5f, Ink.withAlpha(g.c(0xFF8C6C3C), 0.28f), Ink.withAlpha(g.c(0xFF8C6C3C), 0f)));
        p.fillRect(l, t, l + 4f, b, Ink.linear(l, 0, l + 4f, 0, new int[]{Ink.withAlpha(g.c(0xFF8C6C3C), 0.18f), Ink.withAlpha(g.c(0xFF8C6C3C), 0f)}, new float[]{0f, 1f}));
        p.restore();
        g.stroke(s, 0xFFC4A46C, 0.8f, 0.8f);
        g.curve(focused ? 0xFFD8862C : INK_BROWN, focused ? 1.8f : 1.2f, focused ? 1f : 0.7f, l + 6f, b - 4f, (l + r) / 2f, b - 4.6f, r - 6f, b - 3.8f);
    }
}
