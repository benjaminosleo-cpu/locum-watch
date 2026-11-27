package com.benjaminosleo.locumwatch.art;

/**
 * Mochi the capybara: the character rig. Holds every shape and colour ready-made, so
 * drawing a frame allocates nothing. Give it a {@link MochiPose} and it draws that frame.
 *
 * The rig is modelled facing right in its own units (about 110 long, 75 tall when
 * standing) with the origin on the ground under the middle of the body.
 */
public final class Mochi {
    // ---- Mochi's daylight colours ----
    static final int FUR = 0xFFBD8A5C, FUR_LIGHT = 0xFFD9AB7B, FUR_SHADE = 0xFF9F6D45;
    static final int CREAM = 0xFFF0D7B0, OUTLINE = 0xFF6E452B, LEG = 0xFFA97950, LEG_FAR = 0xFF8C6240;
    static final int FOOT = 0xFF7B5334, NOSE = 0xFF53342A, EYE = 0xFF2C1C18, BLUSH = 0xFFF59C9A;
    static final int EAR_IN = 0xFFDD948A, MOUTH = 0xFF7C3A3A, TONGUE = 0xFFF09090;
    static final int GRASS = 0xFF6FAE48, PETAL = 0xFFFFC9DA, PETAL_DEEP = 0xFFF28FB0, POLLEN = 0xFFFFD75A;

    // head attaches here on the body (body-local), side view
    private static final float NECK_X = 33f, NECK_Y = -13f;
    private static final float[] HIP_X = {23f, 29f, -24f, -17f};
    private static final float[] HIP_Y = {13f, 11f, 13f, 11f};

    // ---- side-view shapes ----
    private final Shape body = new Shape(), belly = new Shape(), backLight = new Shape(), furTicks = new Shape();
    private final Shape haunch = new Shape(), haunchArc = new Shape(), leg = new Shape(), legLine = new Shape(), toes = new Shape();
    private final Shape head = new Shape(), muzzle = new Shape(), headLight = new Shape(), ear = new Shape(), earIn = new Shape();
    private final Shape mouth = new Shape(), tuft = new Shape(), whisk = new Shape();
    private final Shape lash = new Shape(), lidSleep = new Shape(), lidHappy = new Shape();
    private final Shape blade = new Shape(), leafItem = new Shape(), bloom = new Shape(), petalShape = new Shape();
    private final Shape zed = new Shape(), spark = new Shape(), alertMark = new Shape(), heart = new Shape(), scratchTicks = new Shape();
    // ---- front-view shapes ----
    private final Shape fBody = new Shape(), fHead = new Shape(), fMuzzle = new Shape(), fNose = new Shape();
    private final Shape fMouth = new Shape(), fEar = new Shape(), fTuft = new Shape(), fChin = new Shape(), bTicks = new Shape();
    private final Shape lidBlink = new Shape(), reach = new Shape();

    // ---- inks (rebuilt only when the light changes) ----
    private static final int LEVELS = 4; // steps of candle-light warmth, so the change near the window is gradual
    private final Ink[][] tint = new Ink[LEVELS][];
    private Ink iBody, iHead, iFur, iLight, iShade, iCream, iLine, iLeg, iLegFar, iFoot, iArm, iNose, iEye, iBlush, iEarIn, iEarBack, iPaw;
    private Ink iMouth, iTongue, iWhite, iShadow, iGrass, iPetal, iPetalDeep, iPollen, iZ, iAlert, iDust, iHeart, iFBody, iFHead;

    public Mochi() {
        buildSide();
        buildFront();
        buildExtras();
        setLight(Palette.at(Palette.DAY));
    }

    /** Re-tints Mochi for a time of day. Allocates; call only when the palette changes. */
    public void setLight(Palette pal) { setLight(pal, 0.35f + 0.65f * pal.night); }

    /**
     * Re-tints Mochi for a time of day. {@code roomLight} (0..1) is how strongly the cottage's
     * candle-light reaches Mochi when close to the window ({@link MochiPose#warm}): it is what
     * stops the fur going moonlit grey inside a warm, candle-lit frame. Allocates.
     */
    public void setLight(Palette pal, float roomLight) {
        for (int i = 0; i < LEVELS; i++) {
            buildInks(pal, 0.5f * Art.clamp01(roomLight) * i / (LEVELS - 1f));
            tint[i] = new Ink[]{iBody, iHead, iFBody, iFHead, iFur, iLight, iShade, iCream, iLine, iLeg, iLegFar, iArm, iFoot,
                iNose, iEye, iBlush, iEarIn, iEarBack, iMouth, iTongue, iWhite, iPaw};
        }
        iShadow = Ink.solid(pal.lit(0xFF2F5A2C));
        iGrass = Ink.solid(pal.character(GRASS)); iPetal = Ink.solid(pal.character(PETAL));
        iPetalDeep = Ink.solid(pal.character(PETAL_DEEP)); iPollen = Ink.solid(pal.character(POLLEN));
        iZ = Ink.solid(Ink.mix(pal.character(0xFFFFFFFF), 0xFFFFFFFF, 0.7f));
        iAlert = Ink.solid(0xFFFFE27A); iDust = Ink.solid(pal.lit(0xFFE9E2C4)); iHeart = Ink.solid(pal.character(0xFFF58CA0));
        use(0f);
    }

    /** Points the working inks at the set for a warmth 0..1 (no allocation). */
    private void use(float warm) {
        Ink[] k = tint[Math.round(Art.clamp01(warm) * (LEVELS - 1))];
        iBody = k[0]; iHead = k[1]; iFBody = k[2]; iFHead = k[3]; iFur = k[4]; iLight = k[5]; iShade = k[6]; iCream = k[7];
        iLine = k[8]; iLeg = k[9]; iLegFar = k[10]; iArm = k[11]; iFoot = k[12]; iNose = k[13]; iEye = k[14]; iBlush = k[15];
        iEarIn = k[16]; iEarBack = k[17]; iMouth = k[18]; iTongue = k[19]; iWhite = k[20]; iPaw = k[21];
    }

    private void buildInks(Palette pal, float w) {
        int fur = pal.characterWarm(FUR, w), light = pal.characterWarm(FUR_LIGHT, w), shade = pal.characterWarm(FUR_SHADE, w);
        iBody = Art.linear(0, -27, 0, 27, new int[]{light, fur, shade}, new float[]{0f, 0.45f, 1f});
        iHead = Art.linear(0, -20, 0, 19, new int[]{light, fur, Ink.mix(fur, shade, 0.6f)}, new float[]{0f, 0.5f, 1f});
        iFBody = Art.linear(0, -52, 0, 0, new int[]{fur, shade}, new float[]{0f, 1f});
        iFHead = Art.linear(0, -87, 0, -28, new int[]{light, fur, Ink.mix(fur, shade, 0.5f)}, new float[]{0f, 0.5f, 1f});
        iFur = Ink.solid(fur); iLight = Ink.solid(light); iShade = Ink.solid(shade);
        iCream = Ink.solid(pal.characterWarm(CREAM, w)); iLine = Ink.solid(pal.characterWarm(OUTLINE, w));
        int legC = pal.characterWarm(LEG, w);
        // legs fade in from the top so they melt into the body rather than sitting on it like a patch
        iLeg = Art.linear(0, -4, 0, 24, new int[]{Ink.withAlpha(legC, 0f), Ink.withAlpha(legC, 0f), legC, legC}, new float[]{0f, 0.3f, 0.62f, 1f});
        iLegFar = Ink.solid(pal.characterWarm(LEG_FAR, w)); iArm = Ink.solid(legC);
        iFoot = Ink.solid(pal.characterWarm(FOOT, w)); iNose = Ink.solid(pal.characterWarm(NOSE, w));
        iEye = Ink.solid(pal.characterWarm(EYE, w)); iBlush = Ink.solid(pal.characterWarm(BLUSH, w));
        iEarIn = Ink.solid(pal.characterWarm(EAR_IN, w));
        iEarBack = Ink.solid(pal.characterWarm(Ink.mix(EAR_IN, FUR_SHADE, 0.6f), w));
        iMouth = Ink.solid(pal.characterWarm(MOUTH, w)); iTongue = Ink.solid(pal.characterWarm(TONGUE, w));
        iWhite = Ink.solid(Ink.mix(pal.characterWarm(0xFFFFFFFF, w), 0xFFFFFFFF, 0.6f));
        iPaw = Ink.solid(pal.characterWarm(Ink.mix(FUR, CREAM, 0.35f), w));
    }

    private void buildSide() {
        body.moveTo(-40, 2).cubicTo(-42, -17, -25, -28, -3, -27).cubicTo(14, -27, 30, -24, 38, -12)
            .cubicTo(43, -2, 40, 14, 30, 21).cubicTo(18, 27, -14, 27, -27, 23).cubicTo(-36, 20, -39, 12, -40, 2).close();
        belly.moveTo(-24, 20).cubicTo(-8, 13, 14, 12, 33, 8).cubicTo(39, 11, 36, 18, 30, 21)
            .cubicTo(18, 27, -14, 27, -24, 23).close();
        backLight.moveTo(-31, -15).cubicTo(-18, -25.5f, 8, -25.5f, 27, -18).cubicTo(10, -21, -14, -21, -31, -15).close();
        furTicks.moveTo(-14, -18).lineTo(-17, -14).moveTo(-4, -19).lineTo(-7, -15).moveTo(6, -18).lineTo(3, -14.5f)
            .moveTo(-30, -4).lineTo(-32, 0).moveTo(-2, 4).lineTo(-4.5f, 8).moveTo(8, 2).lineTo(5.5f, 6);
        haunch.oval(-22, 7, 14.5f, 14f);
        haunchArc.moveTo(-13.5f, -4).cubicTo(-7, 2, -7.5f, 13, -13, 19);
        leg.moveTo(-7f, -4).cubicTo(-7.4f, 6, -5.6f, 13, -4.8f, 18.6f).cubicTo(-4.6f, 23.4f, -2, 24.6f, 1, 24.6f)
            .lineTo(5.6f, 24.6f).cubicTo(9.6f, 24.6f, 9.6f, 19.8f, 6f, 18.8f).cubicTo(6, 13, 7.2f, 5, 7f, -4).close();
        legLine.moveTo(-6.3f, 8).cubicTo(-5.6f, 13, -5f, 16, -4.8f, 18.6f).cubicTo(-4.6f, 23.4f, -2, 24.6f, 1, 24.6f)
            .lineTo(5.6f, 24.6f).cubicTo(9.6f, 24.6f, 9.6f, 19.8f, 6f, 18.8f).cubicTo(6, 15, 6.4f, 11, 6.7f, 8);
        toes.moveTo(3.6f, 22f).lineTo(3.7f, 24.2f).moveTo(6.2f, 22f).lineTo(6.4f, 24f);

        head.moveTo(-22, -2).cubicTo(-23, -14, -14, -20, -3, -20).cubicTo(8, -20, 17, -19, 22, -14.5f)
            .cubicTo(26, -11, 27.2f, -3, 26.2f, 4).cubicTo(25, 11.5f, 19, 17.5f, 9, 18.2f)
            .cubicTo(0, 19, -10, 19, -16, 14).cubicTo(-20, 10.5f, -22, 5, -22, -2).close();
        muzzle.moveTo(11, -9).cubicTo(18, -13.5f, 25.5f, -9, 25.8f, 3).cubicTo(24.5f, 11, 19, 16.6f, 10, 17.2f)
            .cubicTo(3.5f, 14, 4, -4, 11, -9).close();
        headLight.moveTo(-17, -13).cubicTo(-9, -19.5f, 8, -19.5f, 20, -14).cubicTo(8, -16.5f, -8, -16.5f, -17, -13).close();
        ear.moveTo(-3.6f, 3).cubicTo(-6.6f, -2, -5, -8.6f, 0, -8.6f).cubicTo(5, -8.6f, 6.6f, -2, 3.8f, 3).close();
        earIn.moveTo(-1.6f, 1.6f).cubicTo(-3.4f, -2, -2.4f, -5.6f, 0.3f, -5.6f).cubicTo(3, -5.6f, 3.6f, -2, 2.1f, 1.6f).close();
        mouth.moveTo(25.6f, 6.4f).quadTo(21, 11.4f, 15.5f, 8.2f);
        tuft.moveTo(-6, -19.4f).lineTo(-7.6f, -23.4f).moveTo(-2.4f, -19.8f).lineTo(-2.8f, -24.6f).moveTo(1.2f, -19.6f).lineTo(2.6f, -23.2f);
        whisk.oval(19.5f, 1.5f, 0.55f, 0.55f).oval(21.6f, 4f, 0.55f, 0.55f).oval(17.6f, 4.4f, 0.55f, 0.55f);
        lash.moveTo(-4.4f, -0.4f).quadTo(-0.4f, -5.2f, 4.2f, -1.2f).moveTo(-4.4f, -0.4f).lineTo(-5.4f, -1.1f);
        lidBlink.moveTo(-4f, -0.6f).quadTo(0, 3f, 4f, -0.6f);
        lidSleep.moveTo(-4f, -0.8f).quadTo(0, 3.4f, 4f, -0.8f).moveTo(-3.2f, 0.9f).lineTo(-4.6f, 2.4f);
        lidHappy.moveTo(-4f, 1.4f).quadTo(0, -3.8f, 4f, 1.4f);
    }

    private void buildFront() {
        Art.blob(fBody, 0, -50, 24, -45, 35, -25, 31, -6, 0, 0, -31, -6, -35, -25, -24, -45);
        // a capybara's head from the front is a blunt block: flat crown, straight cheeks, square jaw
        fHead.moveTo(-28, -73).cubicTo(-28, -82, -22, -86.4f, -12, -87.2f).quadTo(0, -88.6f, 12, -87.2f).cubicTo(22, -86.4f, 28, -82, 28, -73)
            .cubicTo(28.6f, -64, 31.5f, -54, 31.5f, -44).cubicTo(31.5f, -34, 27, -28, 17, -28).lineTo(-17, -28)
            .cubicTo(-27, -28, -31.5f, -34, -31.5f, -44).cubicTo(-31.5f, -54, -28.6f, -64, -28, -73).close();
        // the big square muzzle fills the lower face and runs up between the eyes
        fMuzzle.moveTo(-12, -67).cubicTo(-8, -69, 8, -69, 12, -67).cubicTo(20, -63, 24.5f, -54, 24.5f, -45)
            .cubicTo(24.5f, -36, 21, -30.6f, 13, -30.6f).lineTo(-13, -30.6f).cubicTo(-21, -30.6f, -24.5f, -36, -24.5f, -45)
            .cubicTo(-24.5f, -54, -20, -63, -12, -67).close();
        // broad nose pad, set high, close under the line of the eyes
        fNose.moveTo(-10.5f, -62).cubicTo(-10, -66.6f, 10, -66.6f, 10.5f, -62).cubicTo(11.5f, -57.6f, 6, -55.4f, 0, -55.4f)
            .cubicTo(-6, -55.4f, -11.5f, -57.6f, -10.5f, -62).close();
        fMouth.moveTo(0, -55.4f).lineTo(0, -44.6f).moveTo(-8f, -45f).quadTo(-4f, -39.6f, 0, -44.6f).quadTo(4f, -39.6f, 8f, -45f);
        fEar.moveTo(-2.9f, 2.6f).cubicTo(-4.6f, -1.2f, -3.5f, -5.2f, 0, -5.2f).cubicTo(3.5f, -5.2f, 4.6f, -1.2f, 2.9f, 2.6f).close();
        fTuft.moveTo(-3.4f, -87.4f).lineTo(-4.6f, -91.4f).moveTo(0, -87.8f).lineTo(0.2f, -92.8f).moveTo(3.4f, -87.4f).lineTo(5f, -91f);
        fChin.moveTo(-24, -31).cubicTo(-10, -24, 10, -24, 24, -31).cubicTo(12, -28, -12, -28, -24, -31).close();
        // fur ticks for the view from behind
        bTicks.moveTo(-8, -72).lineTo(-9.5f, -67).moveTo(0, -74).lineTo(0, -68.6f).moveTo(8, -72).lineTo(9.5f, -67)
            .moveTo(-12, -30).lineTo(-13.5f, -25).moveTo(0, -32).lineTo(0, -26.6f).moveTo(12, -30).lineTo(13.5f, -25)
            .moveTo(-5, -14).quadTo(0, -10.5f, 5, -14);
    }

    private void buildExtras() {
        blade.moveTo(0, 0).quadTo(7, -3.4f, 13, 2).quadTo(7, -0.6f, 0, 1.6f).close();
        Art.leaf(leafItem, 0, 0, 10f, 5.6f, 20f);
        Art.blossom(bloom, 0, 0, 4.6f, -90f);
        Art.petal(petalShape, 0, 0, 5.2f, 3.6f, 0f);
        zed.moveTo(-3, -3.4f).lineTo(3, -3.4f).lineTo(-3, 3.4f).lineTo(3.2f, 3.4f);
        Art.sparkle(spark, 0, 0, 1f);
        alertMark.moveTo(-1.5f, -13).cubicTo(-1.9f, -15.4f, 1.9f, -15.4f, 1.5f, -13).lineTo(0.7f, -3.6f)
            .cubicTo(0.5f, -2.6f, -0.5f, -2.6f, -0.7f, -3.6f).close().oval(0, 0.6f, 1.5f, 1.5f);
        heart.moveTo(0, 3.6f).cubicTo(-6.4f, -1.2f, -3.4f, -5.6f, 0, -2.4f).cubicTo(3.4f, -5.6f, 6.4f, -1.2f, 0, 3.6f).close();
        scratchTicks.moveTo(-4, -8).quadTo(-7, -5, -6, -1).moveTo(-9, -10).quadTo(-13, -6, -12, -1).moveTo(1, -9).quadTo(-1, -7, -1, -4);
    }

    // =====================================================================================

    /**
     * ANIMATED. Draws Mochi for one frame: ground shadow, body and any effects (z's,
     * dust, alert marks). About 35 to 70 draw calls depending on the pose.
     */
    public void draw(Pen p, MochiPose o) {
        use(o.warm);
        drawShadow(p, o);
        float turn = Math.abs(o.facing) < 0.12f ? (o.facing < 0 ? -0.12f : 0.12f) : o.facing;
        p.save();
        p.translate(o.x, o.y - o.lift);
        if (o.view == MochiPose.FRONT) {
            p.scale(o.scale * Math.abs(turn), o.scale, 0, 0);
            drawFront(p, o);
        } else if (o.view == MochiPose.BACK) {
            p.scale(o.scale * Math.abs(turn), o.scale, 0, 0);
            drawBack(p, o);
        } else {
            p.scale(o.scale * turn, o.scale, 0, 0);
            drawSide(p, o);
        }
        p.restore();
        drawEffects(p, o);
    }

    /**
     * ANIMATED. When Mochi is up at the window ({@code pose.overSill}), draws the two front
     * paws; call it after the window frame so they rest on top of the sill. Otherwise does nothing.
     */
    public void drawOverSill(Pen p, MochiPose o) {
        if (!o.overSill || o.view != MochiPose.FRONT) return;
        use(o.warm);
        p.save();
        p.translate(o.x, o.y - o.lift);
        p.scale(o.scale, o.scale, 0, 0);
        for (int s = -1; s <= 1; s += 2) {
            float px = pawX(o) * s, py = pawY(o);
            p.fillOval(px, py, 8.6f, 6.4f, iLine);
            p.fillOval(px, py - 0.4f, 7.5f, 5.4f, iArm);
            p.fillOval(px - 1f * s, py - 1.8f, 4.6f, 2.4f, iFur);
            p.line(px - 2.4f, py + 1f, px - 2.4f, py + 4.2f, iLine, 0.9f);
            p.line(px + 2.4f, py + 1f, px + 2.4f, py + 4.2f, iLine, 0.9f);
        }
        p.restore();
    }

    private void drawShadow(Pen p, MochiPose o) {
        if (o.shadow <= 0.01f || o.overSill) return;
        float k = Art.clamp01(1f - o.lift / (60f * o.scale));
        boolean side = o.view == MochiPose.SIDE;
        float w = (side ? 50f : 34f) * o.scale * (0.7f + 0.3f * k);
        p.save();
        p.alpha(0.30f * o.shadow * (0.5f + 0.5f * k));
        p.fillOval(o.x + (side ? 4f * o.facing * o.scale : 0f), o.y + 1.5f * o.scale, w, 6.5f * o.scale * (0.7f + 0.3f * k), iShadow);
        p.restore();
    }

    // ------------------------------------------------------------------ side view

    private void drawSide(Pen p, MochiPose o) {
        p.save();
        p.translate(0, -o.bodyY);
        p.rotate(o.bodyRot, 0, 0);
        leg(p, o, MochiPose.FF, iLegFar);
        leg(p, o, MochiPose.HF, iLegFar);

        p.save();
        p.scale(o.bodySX, o.bodySY, 0, 0);
        p.fill(body, iBody);
        p.save(); p.alpha(0.55f); p.fill(backLight, iLight); p.restore();
        p.save(); p.alpha(0.78f); p.fill(belly, iCream); p.restore();
        p.save(); p.alpha(0.55f); p.stroke(furTicks, iShade, 1.1f); p.restore();
        p.stroke(body, iLine, 1.5f);
        if (o.haunch > 0.02f) {
            p.save();
            p.alpha(Art.clamp01(o.haunch * 1.4f));
            p.scale(0.86f + 0.14f * o.haunch, 0.86f + 0.14f * o.haunch, -22, 10);
            p.fill(haunch, iFur);
            p.stroke(haunchArc, iLine, 1.2f);
            p.restore();
        }
        p.restore();

        if (!o.hindOver) leg(p, o, MochiPose.HN, iLeg);
        leg(p, o, MochiPose.FN, iLeg);
        if (o.tuck > 0.02f) {
            // loafing: two small paws tucked under the chest
            p.save();
            p.alpha(Art.clamp01(o.tuck * 1.5f));
            p.fillOval(39.6f, 22.8f, 5f, 3.3f, iLine); p.fillOval(39.6f, 22.6f, 4f, 2.5f, iArm);
            p.fillOval(32.6f, 23.6f, 5.8f, 3.9f, iLine); p.fillOval(32.6f, 23.4f, 4.8f, 3f, iPaw);
            p.line(34.6f, 23.2f, 36.4f, 23.3f, iLine, 0.7f); p.line(34.3f, 25f, 35.9f, 25.1f, iLine, 0.7f);
            p.restore();
        }
        drawHeadSide(p, o);
        if (o.hindOver) reachLeg(p, o);
        p.restore();
    }

    private static final int LIMB_N = 9;
    private final float[] limbX = new float[(LIMB_N + 1) * 2], limbY = new float[(LIMB_N + 1) * 2];

    /**
     * The near hind leg brought up to scratch: a tapering limb from the haunch, bending through
     * the knee (kneeX, kneeY) to the foot at (reachX, reachY), which pokes out above the
     * shoulders behind the ear. Rebuilt in place each frame (no allocation).
     */
    private void reachLeg(Pen p, MochiPose o) {
        float hx = -23f * o.bodySX, hy = 9f * o.bodySY;
        for (int i = 0; i <= LIMB_N; i++) {
            float u = (float) i / LIMB_N, v = 1f - u;
            float x = v * v * hx + 2f * v * u * o.kneeX + u * u * o.reachX, y = v * v * hy + 2f * v * u * o.kneeY + u * u * o.reachY;
            float tx = 2f * v * (o.kneeX - hx) + 2f * u * (o.reachX - o.kneeX), ty = 2f * v * (o.kneeY - hy) + 2f * u * (o.reachY - o.kneeY);
            float len = (float) Math.sqrt(tx * tx + ty * ty);
            if (len < 0.001f) { tx = 0f; ty = -1f; len = 1f; }
            float hw = Art.lerp(8.6f, 4.4f, Art.smooth(u));
            float nx = -ty / len * hw, ny = tx / len * hw;
            limbX[i] = x + nx; limbY[i] = y + ny;
            limbX[2 * LIMB_N + 1 - i] = x - nx; limbY[2 * LIMB_N + 1 - i] = y - ny;
        }
        int n = (LIMB_N + 1) * 2;
        // a smooth closed outline through the midpoints of the sampled edge
        reach.reset().moveTo((limbX[n - 1] + limbX[0]) / 2f, (limbY[n - 1] + limbY[0]) / 2f);
        for (int i = 0; i < n; i++) {
            int j = (i + 1) % n;
            reach.quadTo(limbX[i], limbY[i], (limbX[i] + limbX[j]) / 2f, (limbY[i] + limbY[j]) / 2f);
        }
        reach.close();
        p.fill(reach, iArm);
        p.stroke(reach, iLine, 1.3f);
        // the foot, turned along the last stretch of the leg, toes spread
        float ang = (float) Math.toDegrees(Math.atan2(o.reachY - o.kneeY, o.reachX - o.kneeX));
        p.save();
        p.translate(o.reachX, o.reachY);
        p.rotate(ang, 0, 0);
        p.fillOval(1.6f, 0, 7f, 5.6f, iLine);
        p.fillOval(1.6f, 0, 5.8f, 4.4f, iFoot);
        p.line(4.4f, -1.9f, 7.2f, -2.4f, iLine, 0.8f); p.line(4.8f, 1.5f, 7.6f, 1.8f, iLine, 0.8f);
        p.restore();
    }

    private void leg(Pen p, MochiPose o, int i, Ink ink) {
        float len = o.legL[i];
        if (len <= 0.04f) return;
        float hx = HIP_X[i] * o.bodySX, hy = HIP_Y[i] * o.bodySY;
        if (o.legBlur > 0.05f) {
            for (int g = -1; g <= 1; g += 2) {
                p.save();
                p.alpha(0.28f * o.legBlur);
                p.translate(hx, hy); p.rotate(o.legA[i] + g * 20f, 0, 0); p.scale(1f, len, 0, 0);
                p.fill(leg, ink);
                p.restore();
            }
        }
        p.save();
        p.translate(hx, hy); p.rotate(o.legA[i], 0, 0); p.scale(1f, len, 0, 0);
        p.fill(leg, ink);
        p.fillOval(3.2f, 22.6f, 4.9f, 2.1f, iFoot);
        p.stroke(legLine, iLine, 1.3f);
        p.stroke(toes, iLine, 0.8f);
        p.restore();
    }

    private void drawHeadSide(Pen p, MochiPose o) {
        p.save();
        p.translate(NECK_X * o.bodySX + o.headDx, NECK_Y * o.bodySY + o.headDy);
        p.rotate(o.headRot, -8, 4);
        // far ear peeks over the crown
        p.save(); p.translate(-5.5f, -18.5f); p.rotate(-8f - o.earRot * 0.8f, 0, 2); p.fill(ear, iShade); p.stroke(ear, iLine, 1.2f); p.restore();
        p.fill(head, iHead);
        p.save(); p.alpha(0.5f); p.fill(headLight, iLight); p.restore();
        p.save(); p.alpha(0.86f); p.fill(muzzle, iCream); p.restore();
        p.stroke(head, iLine, 1.5f);
        if (o.blush > 0.02f) {
            p.save(); p.alpha(Art.clamp01(0.5f * o.blush));
            p.fillOval(1.5f, 5.2f, 5.6f, 3.4f, iBlush);
            p.restore();
        }
        // mouth: a small smile, with a dark opening underneath when chewing or panting
        if (o.jaw > 0.05f) {
            p.fillOval(20.2f, 10.2f + o.jaw * 1.6f, 3.6f, 1.2f + 2.6f * o.jaw, iMouth);
            if (o.jaw > 0.5f) p.fillOval(19.8f, 11.6f + o.jaw * 1.8f, 2.1f, 1.3f * o.jaw, iTongue);
        }
        p.stroke(mouth, iLine, 1.25f);
        p.fillOval(23.4f + o.nose * 0.5f, -8.6f - Math.abs(o.nose) * 0.4f, 1.9f, 1.35f, iNose);
        p.save(); p.alpha(0.4f); p.fill(whisk, iLine); p.restore();
        eye(p, o, 4.6f, -8.4f, 1f, false, false);
        // near ear
        p.save(); p.translate(-14f, -16.5f); p.rotate(-24f - o.earRot, 0, 2);
        p.fill(ear, iFur); p.fill(earIn, iEarIn); p.stroke(ear, iLine, 1.3f);
        p.restore();
        p.save(); p.alpha(0.8f); p.stroke(tuft, iLine, 1.1f); p.restore();

        if (o.petal > 0.02f) {
            // the petal flutters down from above and settles on the crown
            float up = 1f - Art.clamp01(o.petalFall);
            p.save(); p.alpha(Art.clamp01(o.petal));
            p.translate(-4f + 9f * up * Art.sin(up * 9f), -21.6f - 52f * up);
            p.rotate(-12f + 300f * up, 0, 0);
            p.scale(1f, 1f - 0.5f * Math.abs(Art.sin(up * 11f)), 0, 0);
            p.fill(petalShape, iPetal);
            p.restore();
        }
        if (o.item == MochiPose.ITEM_GRASS) {
            float wob = Art.sin(o.itemPhase * 40f) * 9f * o.jaw;
            p.save(); p.translate(23f, 8.6f); p.rotate(18f + wob, 0, 0);
            p.scale(1.25f - 0.6f * o.itemPhase, 1f, 0, 0);
            p.fill(blade, iGrass);
            p.rotate(-34f, 0, 0); p.scale(0.8f, 1f, 0, 0);
            p.fill(blade, iGrass);
            p.restore();
        } else if (o.item == MochiPose.ITEM_LEAF) {
            p.save(); p.translate(23.6f, 8.2f); p.rotate(-10f + Art.sin(o.itemPhase * 40f) * 8f, 0, 0);
            p.scale(1.1f - 0.6f * o.itemPhase, 1.1f - 0.4f * o.itemPhase, 0, 0);
            p.fill(leafItem, iGrass);
            p.restore();
        } else if (o.item == MochiPose.ITEM_BLOSSOM) {
            p.save(); p.alpha(Art.clamp01(o.itemAlpha));
            p.translate(23.6f + o.itemX, -16.4f + o.itemY); p.rotate(Art.sin(o.itemPhase * 30f) * 10f + o.itemX * 9f, 0, 3f);
            p.fill(bloom, iPetal);
            p.fillOval(0, 0, 1.3f, 1.3f, iPollen);
            p.restore();
        }
        p.restore();
    }

    /**
     * One eye at (x,y) in head units. {@code mirror} flips it for the right-hand eye of the front
     * view; {@code front} draws the calm open eye as a plain round one with two highlights
     * (the side view keeps its sleepy lash line).
     */
    private void eye(Pen p, MochiPose o, float x, float y, float size, boolean mirror, boolean front) {
        p.save();
        p.translate(x, y);
        p.scale(size, size, 0, 0);
        float m = mirror ? -1f : 1f;
        if (o.eye == MochiPose.EYE_SLEEP) {
            p.scale(m, 1f, 0, 0);
            p.stroke(lidSleep, iEye, 1.45f);
        } else if (o.eye == MochiPose.EYE_HAPPY) {
            p.stroke(lidHappy, iEye, 1.6f);
        } else if (o.eye == MochiPose.EYE_SPARKLE) {
            p.fillOval(o.lookX, o.lookY, 4.9f, 5.3f, iEye);
            p.fillOval(o.lookX - 1.5f, o.lookY - 2f, 1.9f, 2.1f, iWhite);
            p.fillOval(o.lookX + 1.9f, o.lookY + 2.1f, 0.95f, 0.95f, iWhite);
            float tw = 1.6f + 0.9f * Art.sin(o.alertPhase * Art.TAU * 2f);
            p.save(); p.translate(o.lookX + 2.3f, o.lookY - 2.6f); p.scale(tw, tw, 0, 0); p.fill(spark, iWhite); p.restore();
        } else {
            float open = Art.clamp01(o.eyeOpen);
            if (open < 0.16f) {
                p.scale(m, 1f, 0, 0);
                p.stroke(front ? lidBlink : lidSleep, iEye, 1.45f);
            } else if (front) {
                float lx = o.lookX * 0.8f, ly = o.lookY * 0.6f + (1f - open) * 1.4f;
                p.fillOval(lx, ly, 4.2f, 4.6f * open, iEye);
                p.fillOval(lx - 1.3f, ly - 1.7f * open, 1.55f, 1.65f * open, iWhite);
                p.fillOval(lx + 1.6f, ly + 1.9f * open, 0.8f, 0.8f * open, iWhite);
            } else {
                p.fillOval(o.lookX * 0.8f, o.lookY * 0.6f + (1f - open) * 1.2f, 3.8f, 4.2f * open, iEye);
                p.fillOval(o.lookX * 0.8f + 1.2f, o.lookY * 0.6f - 1.5f * open + (1f - open) * 1.2f, 1.35f, 1.35f * open, iWhite);
                p.save(); p.translate(0, (1f - open) * 2.2f); p.scale(m, 1f, 0, 0); p.stroke(lash, iEye, 1.15f); p.restore();
            }
        }
        p.restore();
    }

    // ------------------------------------------------------------------ front view

    private float pawX(MochiPose o) { return Art.lerp(11f, 28f, o.pawsUp); }
    private float pawY(MochiPose o) { return Art.lerp(-9f, -24f, o.pawsUp); }

    private void drawFront(Pen p, MochiPose o) {
        p.save();
        p.rotate(o.bodyRot, 0, 0);
        p.scale(o.bodySX, o.bodySY, 0, 0);
        // hind feet
        for (int s = -1; s <= 1; s += 2) {
            p.fillOval(20f * s, -3.4f, 9.4f, 4.8f, iLine);
            p.fillOval(20f * s, -3.8f, 8.4f, 4f, iFoot);
        }
        p.fill(fBody, iFBody);
        p.save(); p.alpha(0.8f); p.fillOval(0, -21, 19f, 17.5f, iCream); p.restore();
        p.stroke(fBody, iLine, 1.5f);
        // front legs: an outlined capsule from shoulder to paw
        float px = pawX(o), py = pawY(o);
        for (int s = -1; s <= 1; s += 2) {
            float sx = Art.lerp(14f, 24f, o.pawsUp) * s, sy = Art.lerp(-27f, -34f, o.pawsUp);
            p.line(sx, sy, px * s, py, iLine, 12f);
            p.line(sx, sy, px * s, py, iArm, 9.4f);
            if (!o.overSill) {
                p.fillOval(px * s, py + 1.6f, 6.2f, 3.4f, iFoot);
            }
        }
        p.restore();

        // head
        p.save();
        p.translate(o.headDx, o.headDy);
        p.rotate(o.headRot, 0, -38);
        frontEars(p, o, true);
        p.save(); p.alpha(0.35f); p.translate(0, 2.5f); p.fill(fChin, iLine); p.restore();
        p.fill(fHead, iFHead);
        p.stroke(fHead, iLine, 1.5f);
        p.save(); p.alpha(0.9f); p.fill(fMuzzle, iCream); p.restore();
        if (o.blush > 0.02f) {
            p.save(); p.alpha(Art.clamp01(0.5f * o.blush));
            p.fillOval(-25.4f, -49.5f, 5.2f, 3.4f, iBlush);
            p.fillOval(25.4f, -49.5f, 5.2f, 3.4f, iBlush);
            p.restore();
        }
        if (o.jaw > 0.05f) {
            p.fillOval(0, -41.6f + 2.6f * o.jaw, 4.6f + 1.2f * o.jaw, 1.2f + 4f * o.jaw, iMouth);
            if (o.jaw > 0.4f) p.fillOval(0, -40f + 4.4f * o.jaw, 3f, 2.1f * o.jaw, iTongue);
        }
        p.save(); p.alpha(0.5f); p.fill(fNose, iShade); p.restore();
        // wide-set nostrils
        p.save(); p.rotate(-24f, -6.6f, -60.6f); p.fillOval(-6.6f + o.nose * 0.4f, -60.6f, 2.6f, 1.6f, iNose); p.restore();
        p.save(); p.rotate(24f, 6.6f, -60.6f); p.fillOval(6.6f + o.nose * 0.4f, -60.6f, 2.6f, 1.6f, iNose); p.restore();
        p.stroke(fMouth, iLine, 1.3f);
        eye(p, o, -20.5f, -70f, 1.08f, false, true);
        eye(p, o, 20.5f, -70f, 1.08f, true, true);
        p.save(); p.alpha(0.8f); p.stroke(fTuft, iLine, 1.1f); p.restore();
        if (o.petal > 0.02f) {
            p.save(); p.alpha(Art.clamp01(o.petal)); p.translate(-9f, -87.6f); p.rotate(-14f, 0, 0);
            p.fill(petalShape, iPetal);
            p.restore();
        }
        p.restore();
    }

    /** Small ears set on the top edge of the head; from the front their insides show, from behind they do not. */
    private void frontEars(Pen p, MochiPose o, boolean inside) {
        for (int s = -1; s <= 1; s += 2) {
            p.save(); p.translate(19.5f * s, -86.2f); p.rotate((24f + o.earRot) * s, 0, 2.6f);
            p.fill(fEar, iFur);
            if (inside) p.fillOval(0, -1.5f, 1.7f, 2.2f, iEarBack);
            p.stroke(fEar, iLine, 1.3f);
            p.restore();
        }
    }

    /** Seen from behind (running away round the far turn of the track): the same blocky shape with no face. */
    private void drawBack(Pen p, MochiPose o) {
        p.save();
        p.rotate(o.bodyRot, 0, 0);
        p.scale(o.bodySX, o.bodySY, 0, 0);
        for (int s = -1; s <= 1; s += 2) {
            p.fillOval(20f * s, -3.4f, 9.4f, 4.8f, iLine);
            p.fillOval(20f * s, -3.8f, 8.4f, 4f, iFoot);
        }
        p.restore();
        // head first: from behind it sits beyond the round rump
        p.save();
        p.translate(o.headDx, o.headDy);
        p.rotate(o.headRot, 0, -38);
        frontEars(p, o, false);
        p.fill(fHead, iFHead);
        p.stroke(fHead, iLine, 1.5f);
        p.save(); p.alpha(0.8f); p.stroke(fTuft, iLine, 1.1f); p.restore();
        p.restore();
        p.save();
        p.rotate(o.bodyRot, 0, 0);
        p.scale(o.bodySX, o.bodySY, 0, 0);
        p.fill(fBody, iFBody);
        p.stroke(fBody, iLine, 1.5f);
        p.save(); p.alpha(0.55f); p.stroke(bTicks, iShade, 1.1f); p.restore();
        // so the rump reads as a rump: a stub of a tail, and the soles of the hind feet kicking up in turn
        p.fillOval(0, -19f, 5.6f, 4.8f, iLine);
        p.fillOval(0, -19.6f, 4.3f, 3.6f, iLight);
        for (int s = -1; s <= 1; s += 2) {
            float kick = 3.2f * Math.max(0f, s * Art.sin(o.alertPhase * Art.TAU * 4f));
            p.fillOval(17f * s, -5.4f - kick, 8.2f, 6f, iLine);
            p.fillOval(17f * s, -5.6f - kick, 7f, 4.9f, iFoot);
            p.fillOval(17f * s, -4.6f - kick, 3.6f, 2.5f, iPaw);
            for (int toe = -1; toe <= 1; toe++) p.fillOval(17f * s + 3.3f * toe, -8.3f - kick, 1.3f, 1.2f, iPaw);
        }
        p.restore();
    }

    // ------------------------------------------------------------------ effects

    private void drawEffects(Pen p, MochiPose o) {
        float s = o.scale, f = o.view == MochiPose.SIDE ? o.facing : 0f;
        if (o.zzz >= 0f) {
            for (int i = 0; i < 3; i++) {
                if (o.zzz - i / 3f < 0f) continue; // the first z's rise one after another
                float k = Art.frac(o.zzz - i / 3f);
                float a = Art.hump(k) * 0.9f;
                p.save();
                p.alpha(a);
                p.translate(o.x + f * s * (30f + 10f * k) + Art.sin(k * 7f + i) * 3f * s, o.y - s * (52f + 46f * k));
                p.rotate(-12f + 20f * k, 0, 0);
                p.scale(s * (0.7f + 1.0f * k), s * (0.7f + 1.0f * k), 0, 0);
                p.stroke(zed, iZ, 1.5f);
                p.restore();
            }
        }
        if (o.alert > 0.02f) {
            float cy = o.y - o.lift - (o.view == MochiPose.SIDE ? 78f : 92f) * s;
            float cx = o.x + f * s * 30f;
            float pop = 0.8f + 0.2f * Art.sin(o.alertPhase * Art.TAU * 3f);
            for (int i = -1; i <= 1; i++) {
                p.save();
                p.alpha(Art.clamp01(o.alert));
                p.translate(cx + i * 15f * s, cy - (i == 0 ? 7f : 0f) * s);
                p.rotate(i * 26f, 0, 12f * s);
                p.scale(s * pop * 1.15f, s * pop * 1.15f, 0, 0);
                p.fill(alertMark, iAlert);
                p.restore();
            }
        }
        if (o.dust > 0.02f) {
            for (int i = 0; i < 3; i++) {
                float k = Art.frac(o.dustPhase + i / 3f);
                p.save();
                p.alpha(o.dust * 0.6f * (1f - k));
                p.fillOval(o.x - f * s * (36f + 30f * k), o.y - s * (3f + 9f * k), s * (3f + 6f * k), s * (2.4f + 4.4f * k), iDust);
                p.restore();
            }
            p.save();
            p.alpha(o.dust * 0.5f);
            p.line(o.x - f * s * 52f, o.y - s * 40f, o.x - f * s * 68f, o.y - s * 40f, iDust, 1.6f * s);
            p.line(o.x - f * s * 50f, o.y - s * 28f, o.x - f * s * 62f, o.y - s * 28f, iDust, 1.4f * s);
            p.restore();
        }
        if (o.scratch > 0.02f) {
            p.save();
            p.alpha(o.scratch * 0.8f);
            p.translate(o.x + f * s * 4f, o.y - s * 58f);
            p.scale(s * f, s, 0, 0);
            p.stroke(scratchTicks, iZ, 1.2f);
            p.restore();
        }
        if (o.hearts > 0.02f) {
            for (int i = 0; i < 2; i++) {
                float k = Art.frac(o.heartPhase + i * 0.5f);
                p.save();
                p.alpha(Art.clamp01(o.hearts) * Art.hump(k));
                p.translate(o.x + (i == 0 ? -1f : 1f) * s * (26f + 8f * k), o.y - o.lift - s * (70f + 26f * k));
                float hs = s * (0.8f + 0.5f * k) * Math.max(1f, o.hearts);
                p.scale(hs, hs, 0, 0);
                p.fill(heart, iHeart);
                p.restore();
            }
        }
    }
}
