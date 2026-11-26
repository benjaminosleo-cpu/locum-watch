package com.benjaminosleo.locumwatch.art;

/**
 * The garden seen through the cottage window, in hero coordinates (see {@link Hero}).
 *
 * Two layers:
 *  - {@link #drawBackground(Pen)}: STATIC for a given hour. Thousands of draw calls; cache it
 *    in a bitmap and redraw only when the hour has moved on a few minutes.
 *  - {@link #drawLiveBack(Pen, double)} and {@link #drawLiveFront(Pen, double)}: ANIMATED,
 *    drawn every frame behind and in front of Mochi. No allocation; about 60 to 95 draw calls.
 */
public final class GardenScene {
    // ---- daylight colours ----
    static final int G_FAR = 0xFFBADB7C, G_MID = 0xFF8FC85C, G_NEAR = 0xFF6BAC48, G_DARK = 0xFF4C8A3C, G_LIGHT = 0xFFD4EA8C;
    static final int F_DARK = 0xFF3C7646, F_MID = 0xFF5C9C50, F_LIGHT = 0xFF8EC668, F_SUN = 0xFFC2E07C;
    static final int BARK = 0xFF73503E, BARK_DARK = 0xFF503629, BARK_LIGHT = 0xFF9C785E, MOSS = 0xFF7FA84E;
    static final int B_DEEP = 0xFFF09CBA, B_MID = 0xFFFAC2D4, B_LIGHT = 0xFFFFE4EC, B_CORE = 0xFFE0708C;
    static final int HILL_FAR = 0xFF9EC6B4, HILL_NEAR = 0xFF7CB088, PATH = 0xFFEBD9A8, WOOD = 0xFFB99C76, WOOD_DARK = 0xFF8A7054;
    static final int STONE = 0xFFB4B4AC, STONE_DARK = 0xFF8C8E88, WATER = 0xFF8CCAE8;
    static final int DAISY = 0xFFFFFDF4, BUTTER = 0xFFFFD84A, BELL = 0xFF8C9CEC, PINK = 0xFFF58CB0, HYDRA = 0xFF9CB4F0;

    private static final float L = Hero.WIN_L - 4f, T = Hero.WIN_T - 6f, R = Hero.WIN_R + 4f, B = Hero.WIN_B + 4f;
    private static final float HZ = Hero.HORIZON;

    private Palette pal = Palette.at(Palette.DAY);
    private float sprigPull;

    // ---- live layer: shapes built once ----
    private static final int SPRAYS = 4, PETALS = 11, TUFTS = 6, FLOWERS = 4, FLIES = 9, STARS = 5;
    private static final float[] SPRAY_X = {84f, 150f, 222f, 286f};
    private static final float[] SPRAY_LEN = {64f, 44f, 54f, 70f};
    private final Shape[] sprayTwig = new Shape[SPRAYS], sprayPale = new Shape[SPRAYS], sprayDeep = new Shape[SPRAYS],
        sprayCore = new Shape[SPRAYS], sprayLeaf = new Shape[SPRAYS];
    private final Shape[] tuft = new Shape[TUFTS], tuftLight = new Shape[TUFTS];
    private static final float[] TUFT_X = {58f, 104f, 150f, 214f, 262f, 304f};
    private static final float[] TUFT_Y = {372f, 392f, 398f, 398f, 390f, 368f};
    private static final float[] FLOWER_X = {84f, 128f, 240f, 286f};
    private static final float[] FLOWER_Y = {394f, 400f, 400f, 392f};
    private static final float[] FLOWER_H = {44f, 26f, 30f, 46f};
    private final Shape flowerHead = new Shape(), bellHead = new Shape(), petal = new Shape(), wing = new Shape();
    private final Shape sprigTwig = new Shape(), sprigLeaves = new Shape(), sprigBloom = new Shape(), scratch = new Shape();
    private final Shape birdBody = new Shape(), birdWing = new Shape(), spark = new Shape();
    private final Shape cloud = new Shape();

    private Ink iTwig, iPale, iDeep, iCore, iLeaf, iTuft, iTuftLight, iStem, iDaisy, iButter, iPink, iBell, iPetal, iPetal2;
    private Ink iWingA, iWingB, iBody, iFly, iFlyCore, iStar, iCloud, iBird, iBirdBelly, iBirdWing, iBeak, iEye;

    public GardenScene() {
        buildLive();
        setHour(Palette.DAY);
    }

    /** Sets the time of day for both layers. Allocates; call only when the hour has changed noticeably. */
    public void setHour(float hour) {
        pal = Palette.at(hour);
        iTwig = Ink.solid(pal.lit(BARK_DARK)); iPale = Ink.solid(pal.lit(B_LIGHT)); iDeep = Ink.solid(pal.lit(B_MID));
        iCore = Ink.solid(pal.lit(B_CORE)); iLeaf = Ink.solid(pal.lit(F_LIGHT));
        iTuft = Ink.solid(pal.lit(0xFF4F9440)); iTuftLight = Ink.solid(pal.lit(0xFF86C458)); iStem = Ink.solid(pal.lit(0xFF5C9C48));
        iDaisy = Ink.solid(pal.lit(DAISY)); iButter = Ink.solid(pal.lit(BUTTER)); iPink = Ink.solid(pal.lit(PINK));
        iBell = Ink.solid(pal.lit(BELL)); iPetal = Ink.solid(pal.lit(B_LIGHT)); iPetal2 = Ink.solid(pal.lit(B_MID));
        iWingA = Ink.solid(pal.lit(0xFFFFE38A)); iWingB = Ink.solid(pal.lit(0xFFB8A4F4)); iBody = Ink.solid(pal.lit(0xFF5A4636));
        iFly = Art.glow(0, 0, 9f, 0xFFE4FF8A, 0.85f); iFlyCore = Ink.solid(0xFFFAFFD0); iStar = Ink.solid(0xFFFFFFFF);
        iCloud = Ink.solid(Ink.withAlpha(pal.cloud, 0.9f));
        iBird = Ink.solid(pal.lit(0xFF7C9CC0)); iBirdBelly = Ink.solid(pal.lit(0xFFF6D8B0)); iBirdWing = Ink.solid(pal.lit(0xFF5C7CA4));
        iBeak = Ink.solid(pal.lit(0xFFF0A040)); iEye = Ink.solid(pal.lit(0xFF2C2420));
    }

    /** The palette currently in use (shared with the other hero layers). */
    public Palette palette() { return pal; }

    /** 0..1: bends the low nibble sprig down towards Mochi's mouth (from {@link MochiDirector#sprigPull()}). */
    public void setSprigPull(float pull) { sprigPull = Art.clamp01(pull); }

    // =====================================================================================
    // STATIC background
    // =====================================================================================

    /** STATIC. Paints the whole garden for the current hour (sky to foreground). Cache the result. */
    public void drawBackground(Pen p) {
        Painter g = new Painter(p, pal, Painter.GARDEN);
        p.save();
        p.clipRect(L, T, R, B);
        sky(g);
        hills(g);
        meadow(g);
        clearing(g);
        hedge(g);
        fenceAndPath(g);
        treeShade(g);
        basin(g);
        tree(g);
        blossomCanopy(g);
        groundDetails(g);
        foreground(g);
        light(g);
        p.restore();
    }

    private void sky(Painter g) {
        g.p.fillRect(L, T, R, HZ + 14f, Ink.linear(0, T, 0, HZ + 10f,
            new int[]{pal.skyTop, pal.skyMid, pal.skyLow}, new float[]{0f, 0.62f, 1f}));
        // stars
        if (pal.night > 0.05f) {
            g.rng.seed(11);
            for (int i = 0; i < 70; i++) {
                float x = g.rng.r(L, R), y = g.rng.r(T, HZ - 12f), r = g.rng.r(0.35f, 1.0f);
                float a = pal.night * g.rng.r(0.4f, 1f) * (1f - 0.6f * Art.ramp(y, 110f, HZ));
                g.p.fillOval(x, y, r, r, Ink.solid(Ink.withAlpha(0xFFFFFFFF, a)));
                if (r > 0.88f) {
                    g.p.line(x - 2.4f, y, x + 2.4f, y, Ink.solid(Ink.withAlpha(0xFFFFFFFF, a * 0.5f)), 0.4f);
                    g.p.line(x, y - 2.4f, x, y + 2.4f, Ink.solid(Ink.withAlpha(0xFFFFFFFF, a * 0.5f)), 0.4f);
                }
            }
        }
        // moon: a fat crescent with a halo
        if (pal.moonVis > 0.02f) {
            float mx = 246f, my = 108f, a = pal.moonVis;
            g.glow(mx, my, 64f, 0xFFBFD4FF, 0.42f * a);
            g.glow(mx, my, 26f, 0xFFFFFFFF, 0.5f * a);
            Shape m = new Shape();
            m.moveTo(mx + 3f, my - 13f).cubicTo(mx - 15f, my - 12f, mx - 15f, my + 12f, mx + 3f, my + 13f)
                .cubicTo(mx - 6f, my + 7f, mx - 6f, my - 7f, mx + 3f, my - 13f).close();
            g.p.fill(m, Ink.solid(Ink.withAlpha(0xFFFFF8DC, a)));
        }
        // sun and its bloom
        if (pal.sunVis > 0.02f && pal.sunY > T - 60f) {
            int sc = Ink.mix(0xFFFFF6D8, 0xFFFFD08A, pal.warm);
            g.glow(pal.sunX, pal.sunY, 120f, sc, 0.55f * pal.sunVis);
            g.glow(pal.sunX, pal.sunY, 44f, 0xFFFFFFFF, 0.6f * pal.sunVis);
            g.p.fillOval(pal.sunX, pal.sunY, 11f, 11f, Ink.solid(Ink.withAlpha(Ink.mix(0xFFFFFFFF, sc, 0.4f), pal.sunVis)));
        }
        // painted clouds: lit tops, shaded bellies
        float ca = 1f - 0.55f * pal.night;
        cloudAt(g, 208f, 138f, 1.0f, ca);
        cloudAt(g, 290f, 122f, 0.7f, ca * 0.9f);
        cloudAt(g, 132f, 150f, 0.6f, ca * 0.8f);
    }

    private void cloudAt(Painter g, float x, float y, float s, float a) {
        int body = pal.cloud, shade = Ink.mix(pal.cloud, pal.skyMid, 0.45f), top = Ink.mix(pal.cloud, 0xFFFFFFFF, 0.5f * (1f - pal.night));
        float[] ox = {-22, -8, 8, 22, 0}, oy = {2, -5, -3, 3, 4}, rr = {11, 14, 12, 9, 16};
        for (int i = 0; i < 5; i++) g.p.fillOval(x + ox[i] * s, y + (oy[i] + 2.5f) * s, rr[i] * s * 1.05f, rr[i] * s * 0.62f, Ink.solid(Ink.withAlpha(shade, a)));
        for (int i = 0; i < 5; i++) g.p.fillOval(x + ox[i] * s, y + oy[i] * s, rr[i] * s, rr[i] * s * 0.6f, Ink.solid(Ink.withAlpha(body, a)));
        for (int i = 1; i < 3; i++) g.p.fillOval(x + (ox[i] - 2f) * s, y + (oy[i] - 2.4f) * s, rr[i] * s * 0.7f, rr[i] * s * 0.32f, Ink.solid(Ink.withAlpha(top, a * 0.8f)));
    }

    private void hills(Painter g) {
        g.far = 0.62f;
        g.blob(HILL_FAR, 1f, L - 20, HZ + 30, L - 10, HZ - 22, 80, HZ - 34, 130, HZ - 20, 190, HZ - 30, 250, HZ - 44, 300, HZ - 30, R + 20, HZ - 36, R + 30, HZ + 30);
        g.far = 0.42f;
        g.blob(HILL_NEAR, 1f, L - 20, HZ + 30, L - 10, HZ - 8, 90, HZ - 16, 150, HZ - 6, 210, HZ - 14, 270, HZ - 22, R + 10, HZ - 10, R + 30, HZ + 30);
        // far woods: soft tree crowns along the ridge
        g.rng.seed(21);
        for (int i = 0; i < 46; i++) {
            float x = L + (R - L) * i / 45f + g.rng.s() * 3f;
            float y = HZ - 10f - 6f * Art.sin(x * 0.04f + 1f) + g.rng.s() * 2f;
            float r = g.rng.r(4f, 7.5f);
            g.far = 0.38f;
            g.oval(x, y, r, r * 0.9f, Ink.mix(F_DARK, F_MID, g.rng.f()));
            g.far = 0.3f;
            g.oval(x - r * 0.25f, y - r * 0.3f, r * 0.55f, r * 0.45f, F_LIGHT, 0.6f);
        }
        // a few pointed firs for rhythm
        for (int i = 0; i < 7; i++) {
            float x = L + 20f + i * 43f + g.rng.s() * 10f, y = HZ - 8f, h = g.rng.r(12f, 20f);
            g.far = 0.36f;
            Shape s = new Shape().moveTo(x, y - h).lineTo(x + h * 0.26f, y).lineTo(x - h * 0.26f, y).close();
            g.fill(s, F_DARK);
        }
        g.far = 0f;
    }

    private void meadow(Painter g) {
        // sunlit meadow between the woods and the hedge
        g.far = 0.22f;
        g.blob(G_FAR, 1f, L - 20, HZ + 60, L - 10, HZ - 2, 100, HZ - 5, 200, HZ - 1, R + 10, HZ - 4, R + 30, HZ + 60);
        g.far = 0.12f;
        g.blob(G_LIGHT, 0.55f, 60, HZ + 12, 140, HZ + 3, 230, HZ + 8, 300, HZ + 4, 300, HZ + 16, 180, HZ + 20, 80, HZ + 22);
        g.rng.seed(31);
        int[] cols = {DAISY, BUTTER, PINK, BELL};
        for (int i = 0; i < 90; i++) {
            float x = g.rng.r(L, R), y = g.rng.r(HZ + 2f, HZ + 40f);
            g.oval(x, y, 0.9f, 0.7f, cols[g.rng.n(4)], 0.85f);
        }
        g.far = 0f;
    }

    private void clearing(Painter g) {
        g.p.fillRect(L, HZ + 36f, R, B, Ink.linear(0, HZ + 36f, 0, B,
            new int[]{g.c(G_FAR), g.c(G_MID), g.c(G_NEAR)}, new float[]{0f, 0.4f, 1f}));
        // broad washes of lighter and deeper green so the lawn is not flat
        g.rng.seed(41);
        for (int i = 0; i < 26; i++) {
            float x = g.rng.r(L, R), y = g.rng.r(HZ + 50f, B), w = g.rng.r(22f, 60f);
            g.oval(x, y, w, w * 0.2f, g.rng.chance(0.5f) ? G_LIGHT : G_DARK, g.rng.r(0.07f, 0.16f));
        }
        // the sunny heart of the clearing, where Mochi likes to lie
        g.glowOval(Hero.SPOT_X + 6f, Hero.SPOT_Y + 2f, 120f, 40f, g.c(0xFFE8F4A0), 0.5f);
        // grass blades: short strokes, longer and bolder towards the viewer
        for (int i = 0; i < 1100; i++) {
            float y = g.rng.r(HZ + 40f, B), x = g.rng.r(L, R);
            float d = Art.ramp(y, HZ + 40f, B), len = 2f + 6f * d * g.rng.r(0.6f, 1.2f);
            int col = g.rng.chance(0.55f) ? G_DARK : G_LIGHT;
            g.line(x, y, x + g.rng.s() * len * 0.35f, y - len, col, 0.6f + 0.7f * d, g.rng.r(0.25f, 0.6f));
        }
    }

    private void hedge(Painter g) {
        // left shrubs (behind the cherry trunk)
        g.foliage(52, 214, 34, 30, F_DARK, F_MID, F_LIGHT, 5f, 51);
        g.foliage(104, 222, 26, 18, F_DARK, F_MID, F_SUN, 4.4f, 52);
        // right: a tall leafy shrub that Mochi nibbles from, and a hydrangea beneath it
        g.foliage(300, 190, 44, 44, F_DARK, F_MID, F_LIGHT, 5.4f, 53);
        g.foliage(252, 222, 24, 17, 0xFF447F4A, F_MID, F_SUN, 4.4f, 54);
        g.foliage(304, 250, 30, 24, F_DARK, 0xFF5E9A58, F_LIGHT, 4.8f, 55);
        g.rng.seed(56);
        float[] hx = {288, 306, 322, 296, 314}, hy = {240, 232, 246, 258, 262};
        for (int i = 0; i < hx.length; i++) flowerBall(g, hx[i], hy[i], 7.5f, i % 2 == 0 ? HYDRA : 0xFFC4A8F0);
        // low mid hedge in the centre distance, with foxglove spires
        g.foliage(170, 214, 36, 9, F_DARK, F_MID, F_SUN, 3.6f, 57);
        g.foliage(222, 216, 20, 8, F_DARK, F_MID, F_LIGHT, 3.4f, 58);
        float[] fx = {118, 126, 236, 246, 68};
        for (int i = 0; i < fx.length; i++) spire(g, fx[i], 236f + (i % 2) * 3f, 20f + 4f * (i % 3), i % 2 == 0 ? PINK : 0xFFD8A0F0);
    }

    private void flowerBall(Painter g, float x, float y, float r, int col) {
        g.oval(x, y, r, r * 0.9f, Ink.mix(col, 0xFF5060A0, 0.35f));
        for (int i = 0; i < 16; i++) {
            double a = g.rng.f() * Art.TAU; float rr = (float) Math.sqrt(g.rng.f()) * r * 0.85f;
            g.blossom(x + (float) Math.cos(a) * rr, y + (float) Math.sin(a) * rr * 0.9f - 0.8f, 2.3f, g.rng.r(0, 72),
                Ink.mix(col, 0xFFFFFFFF, g.rng.r(0.1f, 0.6f)), 1f);
        }
    }

    private void spire(Painter g, float x, float y, float h, int col) {
        g.line(x, y, x + 1f, y - h, 0xFF5C9C48, 1.1f);
        for (int i = 0; i < 7; i++) {
            float k = i / 7f, yy = y - h * (0.3f + 0.7f * k), s = 2.6f * (1f - 0.55f * k);
            g.oval(x + (i % 2 == 0 ? -1.6f : 2.4f) * (1f - 0.5f * k) + k, yy, s, s * 0.8f, Ink.mix(col, 0xFFFFFFFF, 0.25f * (i % 3)));
        }
    }

    private void fenceAndPath(Painter g) {
        // winding footpath from the meadow gate into the clearing
        g.far = 0.3f;
        g.taper(Ink.mix(PATH, G_FAR, 0.35f), 4.5f, 1f, 195, 213, 203, 203, 218, 194, 240, 186, 262, 181);
        g.far = 0f;
        g.glowOval(195, 244, 30f, 7f, g.c(PATH), 0.55f);
        // rustic fence either side of the gap
        float[] px = {112, 126, 140, 154, 168, 180, 208, 220, 234, 248};
        g.rng.seed(61);
        for (int side = 0; side < 2; side++) {
            float x0 = side == 0 ? 108 : 206, x1 = side == 0 ? 182 : 252;
            g.line(x0, 226, x1, 225, WOOD_DARK, 2.2f);
            g.line(x0, 226 - 0.6f, x1, 225 - 0.6f, WOOD, 1.2f);
            g.line(x0, 233, x1, 232, WOOD_DARK, 2.2f);
            g.line(x0, 233 - 0.6f, x1, 232 - 0.6f, WOOD, 1.2f);
        }
        for (int i = 0; i < px.length; i++) {
            float h = 17f + g.rng.s() * 2f, lean = g.rng.s() * 1.2f;
            boolean gate = i == 5 || i == 6;
            float w = gate ? 3.6f : 2.6f; if (gate) h += 4f;
            g.line(px[i], 240, px[i] + lean, 240 - h, WOOD_DARK, w + 1f);
            g.line(px[i] - 0.4f, 240, px[i] + lean - 0.4f, 240 - h, WOOD, w - 0.6f);
            g.oval(px[i] + lean - 0.4f, 240 - h, w * 0.5f, w * 0.4f, 0xFFD8C09C);
            g.oval(px[i], 241, 5f, 1.8f, G_DARK, 0.5f);
        }
    }

    private void treeShade(Painter g) {
        // cool shade under the cherry tree, broken by coins of sunlight
        float day = 1f - pal.night;
        g.glowOval(86, 312, 96f, 34f, g.c(0xFF2E6A3C), 0.5f);
        g.glowOval(150, 268, 70f, 16f, g.c(0xFF2E6A3C), 0.3f);
        g.rng.seed(71);
        for (int i = 0; i < 46; i++) {
            float x = g.rng.r(L, 190f), y = g.rng.r(250f, 372f);
            float k = 1f - Art.ramp(x, 110f, 190f);
            float r = g.rng.r(2.5f, 7f);
            g.oval(x, y, r, r * 0.34f, 0xFFF0F8A8, (0.18f + 0.2f * g.rng.f()) * k * (0.35f + 0.65f * day));
        }
    }

    private void basin(Painter g) {
        // mossy stone water basin: Mochi's drinking bowl
        float x = 108f, y = 262f;
        g.oval(x + 2f, y + 9f, 25f, 5f, 0xFF2E6A3C, 0.4f);
        g.blob(STONE_DARK, 1f, x - 22, y - 1, x - 20, y + 8, x - 8, y + 12, x + 8, y + 12, x + 20, y + 8, x + 22, y - 1, x, y + 3);
        g.blob(STONE, 0.7f, x - 21, y, x - 19, y + 6, x - 10, y + 9, x - 12, y + 3);
        g.oval(x, y, 22f, 6.4f, STONE);
        g.oval(x, y - 0.6f, 22f, 6f, 0xFFD0D0C6, 0.8f);
        g.oval(x, y, 18.6f, 4.6f, STONE_DARK);
        g.p.fillOval(x, y + 0.5f, 18f, 4.1f, Ink.linear(0, y - 4, 0, y + 5, new int[]{Ink.mix(pal.skyMid, g.c(WATER), 0.5f), Ink.mix(pal.skyTop, g.c(0xFF4C88B0), 0.5f)}, new float[]{0f, 1f}));
        g.oval(x - 6f, y - 0.4f, 6f, 1f, 0xFFFFFFFF, 0.55f);
        g.oval(x + 9f, y + 1.6f, 3.2f, 1.3f, F_LIGHT);
        g.petal(x + 2f, y + 1.4f, 3.4f, 2.4f, 20f, B_LIGHT, 1f);
        g.oval(x - 16f, y + 8f, 6f, 2.6f, MOSS, 0.9f);
        g.oval(x + 12f, y + 10f, 7f, 2.4f, MOSS, 0.8f);
        g.oval(x - 19f, y + 3f, 3f, 2.2f, MOSS, 0.8f);
    }

    private void tree(Painter g) {
        // roots and trunk
        g.blob(BARK_DARK, 1f, 40, 322, 52, 300, 57, 250, 55, 180, 50, 110, 46, 30, 88, 30, 86, 110, 87, 180, 88, 250, 94, 298, 110, 320, 90, 316, 74, 322, 58, 316);
        g.blob(BARK, 1f, 48, 316, 58, 296, 61, 250, 59, 180, 54, 110, 50, 30, 80, 30, 78, 110, 79, 180, 80, 250, 84, 296, 96, 314, 72, 316);
        g.blob(BARK_LIGHT, 0.55f, 54, 300, 62, 250, 60, 180, 56, 110, 52, 34, 60, 34, 63, 110, 66, 180, 68, 250, 64, 300);
        g.rng.seed(81);
        for (int i = 0; i < 26; i++) {
            float y = g.rng.r(40f, 300f), x = g.rng.r(56f, 82f), len = g.rng.r(8f, 22f);
            g.curve(g.rng.chance(0.5f) ? BARK_DARK : BARK_LIGHT, g.rng.r(0.6f, 1.2f), 0.5f, x, y, x + g.rng.s() * 2f, y + len * 0.5f, x + g.rng.s() * 2f, y + len);
        }
        // horizontal cherry-bark bands
        for (int i = 0; i < 9; i++) {
            float y = 60f + i * 27f + g.rng.s() * 6f;
            g.curve(BARK_DARK, 1f, 0.45f, 57, y, 68, y + 2f, 82, y - 0.5f);
        }
        g.oval(70, 204, 4.4f, 6f, BARK_DARK, 0.8f);
        g.oval(70.6f, 204.4f, 2.4f, 3.8f, 0xFF36241C, 0.9f);
        // moss creeping up the roots
        g.oval(52, 312, 10f, 4f, MOSS, 0.9f); g.oval(92, 310, 9f, 3.6f, MOSS, 0.9f); g.oval(60, 300, 4f, 6f, MOSS, 0.7f);
        g.oval(84, 296, 3.6f, 5f, 0xFF96BC5C, 0.7f);
        // main boughs sweeping over the top of the view
        g.taper(BARK_DARK, 17f, 5f, 76, 150, 96, 108, 140, 84, 200, 72, 262, 70, 336, 84);
        g.taper(BARK, 12f, 3f, 76, 147, 96, 105, 140, 81, 200, 69.5f, 262, 67.5f, 336, 81.5f);
        g.taper(BARK_LIGHT, 3f, 1f, 82, 132, 100, 100, 142, 78.5f, 200, 67, 262, 65.5f);
        g.taper(BARK_DARK, 11f, 3f, 60, 170, 46, 130, 30, 96);
        g.taper(BARK, 7f, 2f, 60, 168, 46, 129, 30, 94);
        g.taper(BARK_DARK, 7f, 2f, 150, 82, 172, 60, 186, 36);
        g.taper(BARK, 4f, 1.4f, 150, 80.5f, 172, 59, 186, 35);
        g.taper(BARK_DARK, 5f, 1.5f, 236, 70, 262, 88, 296, 96);
        g.taper(BARK_DARK, 4f, 1.2f, 110, 96, 124, 112, 146, 118);
        g.taper(BARK_DARK, 4f, 1.2f, 282, 72, 300, 54, 322, 44);
    }

    private void blossomMass(Painter g, float cx, float cy, float rx, float ry, int seed) {
        g.rng.seed(seed);
        int n = (int) (rx * ry / 5.5f);
        // shadowed underside first, then lighter and lighter dabs towards the top
        for (int i = 0; i < n; i++) {
            double a = g.rng.f() * Art.TAU; float rr = (float) Math.sqrt(g.rng.f());
            float x = cx + (float) Math.cos(a) * rx * rr, y = cy + (float) Math.sin(a) * ry * rr + ry * 0.12f;
            float s = g.rng.r(3.4f, 6f);
            g.oval(x, y, s, s * 0.8f, Ink.mix(B_DEEP, 0xFFC87CA8, g.rng.f() * 0.5f), 0.95f);
        }
        for (int i = 0; i < n; i++) {
            double a = g.rng.f() * Art.TAU; float rr = (float) Math.sqrt(g.rng.f()) * 0.92f;
            float x = cx + (float) Math.cos(a) * rx * rr, y = cy + (float) Math.sin(a) * ry * rr - ry * 0.04f;
            g.blossom(x, y, g.rng.r(3.2f, 4.8f), g.rng.r(0, 72), Ink.mix(B_MID, B_DEEP, g.rng.f() * 0.4f), 0.95f);
        }
        for (int i = 0; i < n * 2 / 3; i++) {
            double a = g.rng.f() * Art.TAU; float rr = (float) Math.sqrt(g.rng.f()) * 0.8f;
            float x = cx + (float) Math.cos(a) * rx * rr - rx * 0.08f, y = cy + (float) Math.sin(a) * ry * rr - ry * 0.2f;
            float s = g.rng.r(2.8f, 4.2f);
            g.blossom(x, y, s, g.rng.r(0, 72), Ink.mix(B_LIGHT, B_MID, g.rng.f() * 0.35f), 1f);
            if (g.rng.chance(0.4f)) g.oval(x, y, 0.9f, 0.9f, B_CORE, 0.9f);
        }
        // a few fresh leaves poking out
        for (int i = 0; i < n / 7; i++) {
            double a = g.rng.f() * Art.TAU;
            float x = cx + (float) Math.cos(a) * rx * 0.9f, y = cy + (float) Math.sin(a) * ry * 0.9f;
            g.leaf(x, y, 6f, 3f, g.rng.r(0, 360), g.rng.chance(0.5f) ? F_LIGHT : 0xFFB48C5C, 0.9f);
        }
    }

    private void blossomCanopy(Painter g) {
        blossomMass(g, 44, 104, 22, 26, 91);
        blossomMass(g, 62, 58, 44, 26, 92);
        blossomMass(g, 126, 60, 40, 22, 93);
        blossomMass(g, 190, 50, 44, 18, 94);
        blossomMass(g, 256, 52, 42, 18, 95);
        blossomMass(g, 312, 58, 30, 24, 96);
        blossomMass(g, 132, 112, 20, 10, 97);
        blossomMass(g, 292, 96, 22, 10, 98);
        blossomMass(g, 100, 92, 18, 12, 99);
    }

    private void groundDetails(Painter g) {
        g.rng.seed(101);
        // clover patches
        for (int i = 0; i < 9; i++) {
            float x = g.rng.r(L, R), y = g.rng.r(250f, B);
            if (nearSpot(x, y, 1.2f)) continue;
            for (int k = 0; k < 6; k++) {
                float cx = x + g.rng.s() * 8f, cy = y + g.rng.s() * 2.6f;
                for (int q = 0; q < 3; q++) g.oval(cx + Art.cos(q * 2.1f) * 1.4f, cy + Art.sin(q * 2.1f) * 0.9f, 1.4f, 1.0f, 0xFF5CA04C, 0.5f);
            }
        }
        // scattered meadow flowers, thinner around Mochi's spot
        for (int i = 0; i < 100; i++) {
            float x = g.rng.r(L, R), y = g.rng.r(246f, B);
            if (nearSpot(x, y, 1.15f) && g.rng.chance(0.85f)) continue;
            float d = Art.ramp(y, 240f, B), s = 1.3f + 1.7f * d;
            int kind = g.rng.n(4);
            if (kind == 0) {
                g.blossom(x, y, s * 1.5f, g.rng.r(0, 72), DAISY, 1f);
                g.oval(x, y, s * 0.5f, s * 0.45f, BUTTER);
            } else if (kind == 1) {
                g.oval(x, y, s * 0.9f, s * 0.75f, BUTTER);
                g.oval(x - s * 0.2f, y - s * 0.2f, s * 0.4f, s * 0.3f, 0xFFFFF0A0);
            } else if (kind == 2) {
                g.line(x, y, x + 0.6f, y - s * 3f, 0xFF5C9C48, 0.7f);
                for (int k = 0; k < 3; k++) g.oval(x + 1.2f + k * 0.5f, y - s * (1.4f + k * 0.8f), s * 0.7f, s * 0.6f, BELL);
            } else {
                g.blossom(x, y, s * 1.3f, g.rng.r(0, 72), PINK, 1f);
                g.oval(x, y, s * 0.4f, s * 0.4f, 0xFFFFF0A0);
            }
        }
        // fallen petals, thickest under the tree
        for (int i = 0; i < 85; i++) {
            float x = L + (R - L) * (float) Math.pow(g.rng.f(), 1.5), y = g.rng.r(250f, B);
            float d = Art.ramp(y, 240f, B);
            g.petal(x, y, 2f + 2.4f * d, 1.3f + 1.4f * d, g.rng.r(0, 360), g.rng.chance(0.6f) ? B_LIGHT : B_MID, 0.9f);
        }
        // toadstools by the roots
        mushroom(g, 100, 316, 1.0f); mushroom(g, 110, 321, 0.7f); mushroom(g, 44, 330, 0.8f);
        // a couple of stones
        g.oval(256, 336, 8f, 3.6f, STONE_DARK); g.oval(255, 335, 7f, 3f, STONE); g.oval(253, 333.6f, 3.4f, 1.2f, 0xFFD8D8D0, 0.8f);
        g.oval(268, 339, 4.6f, 2.4f, STONE_DARK); g.oval(267.6f, 338.4f, 3.8f, 1.8f, STONE);
    }

    private static boolean nearSpot(float x, float y, float k) {
        float dx = (x - Hero.SPOT_X) / (78f * k), dy = (y - Hero.SPOT_Y - 2f) / (26f * k);
        return dx * dx + dy * dy < 1f;
    }

    private void mushroom(Painter g, float x, float y, float s) {
        g.oval(x, y + 1f * s, 6f * s, 1.8f * s, 0xFF2E6A3C, 0.4f);
        g.blob(0xFFF4E8D0, 1f, x - 2.2f * s, y, x - 1.8f * s, y - 7f * s, x + 1.8f * s, y - 7f * s, x + 2.4f * s, y);
        g.blob(0xFFD8644C, 1f, x - 7f * s, y - 6f * s, x - 4f * s, y - 12f * s, x + 4f * s, y - 12f * s, x + 7f * s, y - 6f * s, x, y - 5f * s);
        g.oval(x - 2.4f * s, y - 9.6f * s, 1.3f * s, 1f * s, 0xFFFFF4E0); g.oval(x + 2.6f * s, y - 8.4f * s, 1f * s, 0.8f * s, 0xFFFFF4E0);
        g.oval(x + 0.4f * s, y - 10.8f * s, 0.8f * s, 0.6f * s, 0xFFFFF4E0);
    }

    private void foreground(Painter g) {
        // dark leafy corners at the bottom frame the clearing and push the eye to the middle
        g.foliage(40, 392, 34, 26, 0xFF2F6A3E, 0xFF4A8C48, F_LIGHT, 6.5f, 111);
        g.foliage(322, 394, 32, 28, 0xFF2F6A3E, 0xFF4A8C48, F_LIGHT, 6.5f, 112);
        g.rng.seed(113);
        // broad hosta-like leaves, left
        for (int i = 0; i < 7; i++) {
            float a = -150f + i * 17f + g.rng.s() * 6f, len = g.rng.r(26f, 40f);
            g.leaf(56 + i * 3f, 402, len, len * 0.42f, a, 0xFF3F8448, 1f);
            g.leaf(56 + i * 3f, 402, len * 0.92f, len * 0.2f, a - 2f, 0xFF78B45C, 0.7f);
        }
        // fern fronds, right
        for (int i = 0; i < 6; i++) {
            float a = -128f + i * 14f, len = g.rng.r(34f, 50f);
            double ra = Math.toRadians(a);
            float bx = 306 - i * 2f, by = 404;
            g.line(bx, by, bx + (float) Math.cos(ra) * len, by + (float) Math.sin(ra) * len, 0xFF3F8448, 1.1f);
            for (int k = 2; k < 12; k++) {
                float u = k / 12f, x = bx + (float) Math.cos(ra) * len * u, y = by + (float) Math.sin(ra) * len * u, l2 = 8f * (1f - u * 0.7f);
                g.leaf(x, y, l2, 2.6f, a - 62f, 0xFF4E9A4C, 0.95f);
                g.leaf(x, y, l2, 2.6f, a + 62f, 0xFF6CB458, 0.95f);
            }
        }
        // tall foreground grass along the very bottom
        for (int i = 0; i < 80; i++) {
            float x = g.rng.r(L, R), h = g.rng.r(8f, 22f) * (0.6f + 0.6f * Math.abs(x - 180f) / 150f);
            g.leaf(x, B + 2f, h, 2.6f, -90f + g.rng.s() * 16f, g.rng.chance(0.5f) ? 0xFF4C9040 : 0xFF74B850, 0.95f);
        }
    }

    private void light(Painter g) {
        float day = 1f - pal.night;
        // shafts of sun through the branches
        if (day > 0.2f) {
            float lean = (180f - pal.sunX) * 0.45f;
            int col = Ink.mix(0xFFFFFBE0, 0xFFFFD890, pal.warm);
            float a = (0.13f + 0.2f * pal.warm) * day;
            float[] sx = {96f, 150f, 214f, 262f};
            float[] sw = {16f, 24f, 14f, 20f};
            for (int i = 0; i < sx.length; i++) {
                Shape s = new Shape().moveTo(sx[i], T).lineTo(sx[i] + sw[i], T).lineTo(sx[i] + sw[i] * 2.2f + lean, 330f).lineTo(sx[i] - sw[i] * 0.6f + lean, 330f).close();
                g.p.fill(s, Ink.linear(0, T, 0, 330f, new int[]{Ink.withAlpha(col, a), Ink.withAlpha(col, a * 0.7f), Ink.withAlpha(col, 0f)}, new float[]{0f, 0.5f, 1f}));
            }
        }
        // warm bloom round a low sun; cool pool of moonlight on the clearing at night
        if (pal.sunVis > 0.02f && pal.warm > 0.05f) g.glow(pal.sunX, pal.sunY, 150f, Ink.mix(0xFFFFE0A0, 0xFFFFB070, pal.warm), 0.3f * pal.warm * pal.sunVis);
        if (pal.night > 0.05f) {
            g.glowOval(Hero.SPOT_X + 10f, Hero.SPOT_Y, 130f, 44f, 0xFFBCD4FF, 0.3f * pal.night);
            g.glowOval(108f, 262f, 30f, 9f, 0xFFDCE8FF, 0.35f * pal.night);
        }
        // soft vignette so the middle glows
        int dark = Ink.mix(g.c(0xFF1E4030), 0xFF0A1024, pal.night * 0.5f);
        g.p.fillRect(L, T, R, B, Ink.radial(180f, 250f, 250f,
            new int[]{Ink.withAlpha(dark, 0f), Ink.withAlpha(dark, 0f), Ink.withAlpha(dark, 0.42f)}, new float[]{0f, 0.55f, 1f}));
    }

    // =====================================================================================
    // ANIMATED live layer
    // =====================================================================================

    private void buildLive() {
        Rng r = new Rng(201);
        for (int i = 0; i < SPRAYS; i++) {
            float len = SPRAY_LEN[i];
            Shape tw = sprayTwig[i] = new Shape(), pa = sprayPale[i] = new Shape(), de = sprayDeep[i] = new Shape(),
                co = sprayCore[i] = new Shape(), le = sprayLeaf[i] = new Shape();
            // a drooping twig from (0,0) with two side shoots; blossoms cluster along it
            float bend = (i % 2 == 0 ? 1f : -1f) * len * 0.22f;
            tw.moveTo(0, -8).quadTo(bend * 0.2f, len * 0.5f, bend, len);
            tw.moveTo(bend * 0.25f, len * 0.42f).quadTo(bend * 0.4f - 9f, len * 0.6f, bend * 0.3f - 14f, len * 0.78f);
            tw.moveTo(bend * 0.12f, len * 0.22f).quadTo(10f, len * 0.34f, 15f, len * 0.52f);
            float[] ax = {bend, bend * 0.3f - 14f, 15f}, ay = {len, len * 0.78f, len * 0.52f};
            float[] sx = {0f, bend * 0.25f, bend * 0.12f}, sy = {0f, len * 0.42f, len * 0.22f};
            for (int b = 0; b < 3; b++) {
                int n = b == 0 ? 6 : 4;
                for (int k = 0; k < n; k++) {
                    float u = 0.35f + 0.65f * (k + r.f() * 0.6f) / n;
                    float x = Art.lerp(sx[b], ax[b], u) + r.s() * 4.5f, y = Art.lerp(sy[b], ay[b], u) + r.s() * 3.5f;
                    float s = r.r(4.2f, 5.6f);
                    Art.blossom(k % 3 == 0 ? de : pa, x, y, s, r.r(0, 72));
                    co.oval(x, y, 1.15f, 1.15f);
                }
                Art.leaf(le, ax[b], ay[b], 7f, 3.2f, 80f + r.s() * 40f);
                Art.leaf(le, Art.lerp(sx[b], ax[b], 0.3f), Art.lerp(sy[b], ay[b], 0.3f), 6f, 3f, 20f + r.s() * 50f);
            }
        }
        for (int i = 0; i < TUFTS; i++) {
            Shape a = tuft[i] = new Shape(), b = tuftLight[i] = new Shape();
            int n = 7 + r.n(3);
            for (int k = 0; k < n; k++) {
                float off = (k - n / 2f) * 3.4f, h = r.r(16f, 30f), ang = -90f + (k - n / 2f) * 7f + r.s() * 6f;
                Art.leaf(k % 2 == 0 ? a : b, off, 0, h, 3.6f, ang);
            }
        }
        Art.blossom(flowerHead, 0, 0, 5.2f, -90f);
        // a nodding bell flower
        bellHead.moveTo(-3.4f, -2).cubicTo(-4f, 3, -4.6f, 5, -5f, 7).lineTo(-2f, 5.6f).lineTo(0, 7.6f).lineTo(2f, 5.6f).lineTo(5f, 7)
            .cubicTo(4.6f, 5, 4f, 3, 3.4f, -2).cubicTo(2, -4, -2, -4, -3.4f, -2).close();
        Art.petal(petal, -2.6f, 0, 5.4f, 3.6f, 0f);
        wing.moveTo(0, 0).cubicTo(2, -6, 7, -7.4f, 7.6f, -3).cubicTo(8, -0.6f, 4, 0.6f, 0, 0)
            .cubicTo(3.4f, 0.4f, 6.4f, 2.6f, 5.2f, 5).cubicTo(3.6f, 6.6f, 0.8f, 3, 0, 0).close();
        // low sprig on the right-hand shrub: grows from (0,0) down-left
        sprigTwig.moveTo(0, 0).quadTo(-10, 2, -20, 14).moveTo(-11, 4.6f).quadTo(-17, 2, -23, 4);
        Art.leaf(sprigLeaves, -20, 14, 9, 4.6f, 130); Art.leaf(sprigLeaves, -16, 9.6f, 8, 4.2f, 190); Art.leaf(sprigLeaves, -14, 8, 8, 4.2f, 60);
        Art.leaf(sprigLeaves, -8, 3, 7.6f, 4f, 100); Art.leaf(sprigLeaves, -23, 4, 8, 4.2f, 170); Art.leaf(sprigLeaves, -17, 3.2f, 7, 3.8f, 250);
        Art.leaf(sprigLeaves, -4, 1, 7, 3.8f, 290);
        Art.blossom(sprigBloom, -21, 15.6f, 3.6f, 10); Art.blossom(sprigBloom, -24, 5, 3.2f, 40);
        // plump little bird
        birdBody.moveTo(-6, 0).cubicTo(-6.4f, -5, -1, -7.6f, 3, -6).cubicTo(6.6f, -4.6f, 7, 0.6f, 4, 3).cubicTo(1, 5, -4, 4.6f, -6, 0).close();
        birdBody.moveTo(-5.4f, 1).lineTo(-11.6f, 2.6f).lineTo(-5f, 3.4f).close();
        birdWing.moveTo(-4.4f, -1.4f).cubicTo(-2, -4, 2, -2.6f, 2.4f, 0.4f).cubicTo(0, 2.6f, -3, 1.6f, -4.4f, -1.4f).close();
        Art.sparkle(spark, 0, 0, 1f);
        cloud.oval(-20, 2, 11, 6).oval(-7, -4, 14, 8).oval(9, -2, 12, 7).oval(22, 3, 9, 5).oval(0, 4, 18, 6);
    }

    /**
     * ANIMATED. The parts of the garden that move BEHIND Mochi: a drifting cloud or twinkling
     * stars, a little bird on the fence, the low sprig Mochi nibbles, butterflies.
     * {@code t} is seconds (any ever-growing clock); everything loops smoothly.
     */
    public void drawLiveBack(Pen p, double t) {
        float day = 1f - pal.night;
        p.save();
        p.clipRect(Hero.WIN_L, Hero.WIN_T, Hero.WIN_R, Hero.WIN_B);
        if (day > 0.3f) {
            // a small cloud sailing across the open patch of sky
            float k = Art.frac(t / 96.0);
            p.save();
            p.alpha(0.85f * Art.smooth(Art.hump(k) * 2.2f) * day);
            p.translate(Art.lerp(90f, 340f, k), 128f + 3f * Art.wave(t, 31.0));
            p.scale(0.85f, 0.85f, 0, 0);
            p.fill(cloud, iCloud);
            p.restore();
        }
        if (pal.night > 0.3f) {
            for (int i = 0; i < STARS; i++) {
                float tw = 0.5f + 0.5f * Art.wave(t + i * 1.7, 2.3 + i * 0.61);
                p.save();
                p.alpha(pal.night * (0.35f + 0.65f * tw));
                p.translate(112f + i * 46f + (i % 2) * 9f, 122f + ((i * 37) % 30));
                p.scale(2.2f + 1.6f * tw, 2.2f + 1.6f * tw, 0, 0);
                p.fill(spark, iStar);
                p.restore();
            }
        }
        if (day > 0.4f) bird(p, t, day);
        // nibble sprig
        p.save();
        p.translate(Hero.SPRIG_X + 20f, Hero.SPRIG_Y - 14f);
        p.rotate(3f * Art.wave(t, 4.3) + 1.5f * Art.wave(t, 1.7) - 11f * sprigPull, 0, 0);
        p.stroke(sprigTwig, iTwig, 1.4f);
        p.fill(sprigLeaves, iLeaf);
        p.fill(sprigBloom, iPale);
        p.restore();
        p.restore();
    }

    /** A plump bird that sits on a fence post, looks about, and now and then hops along to the next post. */
    private void bird(Pen p, double t, float day) {
        double cyc = t % 34.0;
        float hopK = Art.ramp((float) cyc, 20f, 20.5f) - Art.ramp((float) cyc, 31f, 31.5f);
        float x = Art.lerp(234f, 248f, Art.smooth(hopK)), y = 222.5f - 6f * Art.hump(Art.frac(hopK)) * (hopK > 0f && hopK < 1f ? 1f : 0f);
        float peck = Art.frac(t / 5.3) < 0.08f ? Art.hump(Art.frac(t / 5.3) / 0.08f) : 0f;
        float dir = Art.frac(t / 13.7) < 0.5f ? 1f : -1f;
        p.save();
        p.alpha(Art.smooth(day, 0.4f, 0.7f));
        p.translate(x, y);
        p.scale(0.8f * dir, 0.8f, 0, 0);
        p.rotate(18f * peck + 3f * Art.wave(t, 0.9), 0, 3);
        p.fill(birdBody, iBird);
        p.fillOval(1.6f, 0.8f, 3.6f, 3f, iBirdBelly);
        p.fill(birdWing, iBirdWing);
        p.line(6.2f, -2.6f, 8.8f, -1.8f, iBeak, 1.4f);
        p.fillOval(3.8f, -3.4f, 0.8f, 0.8f, iEye);
        p.restore();
    }

    /**
     * ANIMATED. The parts of the garden that move IN FRONT of Mochi: foreground grass tufts
     * and flowers swaying, butterflies (day) or fireflies (night), drifting petals, and the
     * blossom sprays that overhang the top of the window.
     */
    public void drawLiveFront(Pen p, double t) {
        float day = 1f - pal.night;
        p.save();
        p.clipRect(Hero.WIN_L, Hero.WIN_T, Hero.WIN_R, Hero.WIN_B);
        // a soft breeze that comes and goes
        float gust = 0.6f + 0.4f * Art.wave(t, 11.0);
        for (int i = 0; i < TUFTS; i++) {
            float sway = (4f * Art.wave(t + i * 0.9, 3.1) + 2f * Art.wave(t + i * 1.7, 1.3)) * gust;
            p.save();
            p.translate(TUFT_X[i], TUFT_Y[i]);
            p.rotate(sway, 0, 0);
            p.fill(tuft[i], iTuft);
            p.rotate(sway * 0.5f, 0, 0);
            p.fill(tuftLight[i], iTuftLight);
            p.restore();
        }
        for (int i = 0; i < FLOWERS; i++) {
            float sway = (5f * Art.wave(t + i * 1.3, 3.7) + 2f * Art.wave(t + i * 0.7, 1.5)) * gust;
            float h = FLOWER_H[i];
            p.save();
            p.translate(FLOWER_X[i], FLOWER_Y[i]);
            p.rotate(sway + (i % 2 == 0 ? -5f : 5f), 0, 0);
            p.line(0, 0, 0, -h, iStem, 1.5f);
            if (i % 2 == 0) {
                p.translate(0, -h);
                p.rotate(sway * 1.5f, 0, 0);
                p.fill(flowerHead, i == 0 ? iDaisy : iPink);
                p.fillOval(0, 0, 1.7f, 1.7f, iButter);
            } else {
                p.translate(0, -h + 2f);
                p.rotate(sway * 2f + 12f, 0, -3);
                p.fill(bellHead, i == 1 ? iBell : iButter);
            }
            p.restore();
        }
        if (day > 0.45f) {
            float a = Art.smooth(day, 0.45f, 0.8f);
            butterfly(p, t, 0, a);
            butterfly(p, t, 1, a);
        }
        if (pal.night > 0.25f) {
            float a = Art.smooth(pal.night, 0.25f, 0.7f);
            for (int i = 0; i < FLIES; i++) {
                float x = 180f + 128f * Art.sin(t * (0.11 + i * 0.013) + i * 2.4) + 10f * Art.wave(t + i, 2.9);
                float y = 290f + 62f * Art.sin(t * (0.09 + i * 0.017) + i * 1.3) + 6f * Art.wave(t + i * 2.0, 2.1) - (i % 3) * 22f;
                float pulse = 0.5f + 0.5f * Art.wave(t + i * 1.1, 2.6 + (i % 4) * 0.45);
                pulse = pulse * pulse;
                p.save();
                p.translate(x, y);
                p.alpha(a * (0.25f + 0.75f * pulse));
                p.fillOval(0, 0, 9f, 9f, iFly);
                p.fillOval(0, 0, 1.2f, 1.2f, iFlyCore);
                p.restore();
            }
        }
        // drifting petals
        for (int i = 0; i < PETALS; i++) {
            double per = 9.0 + (i % 5) * 1.7;
            float k = Art.frac(t / per + i * 0.37);
            float x = 50f + ((i * 67) % 250) + 34f * k * gust + 9f * Art.sin(k * 9f + i);
            float y = 60f + 350f * k * (0.8f + 0.04f * (i % 4));
            p.save();
            p.alpha(Art.smooth(k, 0f, 0.08f) * (1f - Art.smooth(k, 0.86f, 1f)) * (0.95f - 0.2f * pal.night));
            p.translate(x, y);
            p.rotate(k * 560f + i * 50f, 0, 0);
            p.scale(1f, 0.45f + 0.55f * Math.abs(Art.sin(k * 14f + i)), 0, 0);
            p.fill(petal, i % 3 == 0 ? iPetal2 : iPetal);
            p.restore();
        }
        // overhanging blossom sprays
        for (int i = 0; i < SPRAYS; i++) {
            float sway = (3.2f * Art.wave(t + i * 1.9, 5.3) + 1.6f * Art.wave(t + i * 0.6, 2.1)) * gust;
            p.save();
            p.translate(SPRAY_X[i], Hero.WIN_T + (i == 1 || i == 2 ? 4f : 14f));
            p.rotate(sway, 0, 0);
            p.stroke(sprayTwig[i], iTwig, 1.5f);
            p.fill(sprayLeaf[i], iLeaf);
            p.fill(sprayDeep[i], iDeep);
            p.fill(sprayPale[i], iPale);
            p.fill(sprayCore[i], iCore);
            p.restore();
        }
        p.restore();
    }

    private void butterfly(Pen p, double t, int i, float a) {
        double s = t * (i == 0 ? 0.23 : 0.19) + i * 2.0;
        float x = (i == 0 ? 150f : 228f) + 78f * Art.sin(s) + 14f * Art.sin(s * 3.1 + 1f);
        float y = (i == 0 ? 262f : 330f) + 22f * Art.sin(s * 1.7 + 0.6f) + 8f * Art.sin(s * 4.3);
        float flap = Math.abs(Art.sin(t * (9.0 + i)));
        float dir = Art.cos(s) >= 0 ? 1f : -1f;
        p.save();
        p.alpha(a);
        p.translate(x, y);
        p.rotate(dir * 14f, 0, 0);
        p.scale(0.9f, 0.9f, 0, 0);
        p.save(); p.scale(0.25f + 0.75f * flap, 1f, 0, 0); p.fill(wing, i == 0 ? iWingA : iWingB); p.restore();
        p.save(); p.scale(-(0.25f + 0.75f * flap), 1f, 0, 0); p.fill(wing, i == 0 ? iWingA : iWingB); p.restore();
        p.line(0, -2.4f, 0, 3f, iBody, 1.2f);
        p.restore();
    }
}
