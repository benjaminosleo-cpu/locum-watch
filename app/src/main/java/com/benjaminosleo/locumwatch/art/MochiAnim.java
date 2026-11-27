package com.benjaminosleo.locumwatch.art;

/**
 * Mochi's movements. Each method bends a {@link MochiPose} for one activity at a given
 * moment (seconds since the activity began). Pure functions of time: no state, no allocation.
 * The director chooses which one to call and where Mochi is standing.
 */
final class MochiAnim {
    private MochiAnim() {}

    private static final int FN = MochiPose.FN, FF = MochiPose.FF, HN = MochiPose.HN, HF = MochiPose.HF;
    /** Ground covered by one full walking stride, in rig units (multiply by the pose scale). */
    static final float WALK_STRIDE = 36f;
    /** Lengths of the two tap reactions, seconds. */
    static final float POKE_AWAKE = 2.0f, POKE_ASLEEP = 3.0f;

    /** Eyelid for an occasional blink: mostly {@code open}, dipping shut for a moment. */
    private static float blink(float t, float open, float offset) {
        float k = Art.frac(t / 4.1f + offset);
        float k2 = Art.frac(t / 6.7f + offset * 2.3f);
        float shut = Math.max(Art.hump(Art.ramp(k, 0f, 0.05f)), Art.hump(Art.ramp(k2, 0f, 0.035f)));
        return open * (1f - shut);
    }

    /** A quick double ear flick every few seconds. */
    private static float earFlick(float t, float every) {
        float k = Art.frac(t / every + 0.4f) * every;
        return k < 0.5f ? -26f * Math.abs(Art.sin(k * Art.TAU * 2f)) * (1f - k * 2f * 0.4f) : 0f;
    }

    /** Legs tucked under, belly on the grass: the base for sleeping and resting. */
    private static void loaf(MochiPose o) {
        o.stand();
        o.bodyY = 25.5f; o.bodySX = 1.05f; o.bodySY = 0.96f;
        o.legL[FN] = 0f; o.legL[FF] = 0f; o.tuck = 1f;
        o.legL[HN] = 0f; o.legL[HF] = 0f;
        o.haunch = 1f;
    }

    /** Standing quietly: used as the neutral pose that turns and other clips pass through. */
    static void standIdle(MochiPose o, float t) {
        o.stand();
        o.bodySY = 1f + 0.012f * Art.wave(t, 3.2f);
        o.eyeOpen = blink(t, 0.85f, 0.35f);
    }

    /** Front view, standing square on: the middle frames of a turn. */
    static void standFront(MochiPose o, float t) {
        o.stand();
        o.view = MochiPose.FRONT;
        o.eye = MochiPose.EYE_OPEN; o.eyeOpen = 1f; o.blush = 1f;
        o.headRot = 2.5f * Art.wave(t, 2.1f);
    }

    /**
     * SLEEP: lies down, yawns, lets the eyes close and the chin sink, and only then do the z's
     * begin; a petal flutters down onto the head. In the last seconds before {@code dur} Mochi
     * wakes: ear flick, bleary blink, a long stretch. {@code zFrom} is when the z's may start.
     */
    static void sleep(MochiPose o, float t, float dur, float zFrom) {
        loaf(o);
        float left = dur - t;
        float settle = Art.smooth(t, 1.5f, 2.4f);
        float wake = 1f - Art.smooth(left, 0.5f, 1.8f);
        float asleep = settle * (1f - wake);
        float yawn = Art.hump(Art.ramp(t, 0.45f, 1.6f));
        float stretch = Art.hump(Art.ramp(left, 1.1f, 0.05f));
        float br = Art.wave(t, 4.2f);
        o.bodySY = 0.96f + 0.03f * br * asleep; o.bodySX = 1.05f - 0.012f * br * asleep + 0.07f * stretch;
        o.headDx = Art.lerp(1f, 3f, asleep) + 4f * stretch;
        o.headDy = Art.lerp(3.5f, 16.5f - 0.9f * br, asleep) - 3.5f * yawn - 2f * stretch;
        o.headRot = Art.lerp(-2f, 9f, asleep) - 9f * yawn - 5f * stretch;
        if (asleep > 0.55f || yawn > 0.45f) {
            o.eye = MochiPose.EYE_SLEEP;
        } else {
            o.eye = MochiPose.EYE_OPEN;
            // heavy lids on the way down, a bleary blink on the way up
            o.eyeOpen = Art.lerp(0.72f, 0.24f, asleep / 0.55f) * (1f - Art.hump(Art.ramp(left, 1.25f, 1.0f)));
        }
        o.jaw = yawn;
        float flick = left < 1.9f && left > 1.4f ? -28f * Math.abs(Art.sin((1.9f - left) * Art.TAU * 2f)) : 0f;
        o.earRot = 10f * asleep + earFlick(t, 7.3f) * asleep + flick;
        o.zzz = asleep > 0.95f && t > zFrom ? (t - zFrom) / 3.4f : -1f;
        o.blush = 1.1f;
        float pt = t % 70f; // a new petal now and then; the old one has long since blown away
        o.petal = t > 5f ? Art.smooth(pt, 5f, 5.2f) * (1f - Art.smooth(pt, 52f, 58f)) * (1f - wake) : 0f;
        o.petalFall = Art.smooth(pt, 5f, 7f);
        o.nose = asleep > 0.9f && t % 9f < 0.4f ? Art.sin(t * 40f) : 0f;
    }

    /** REST: loafed but awake, blinking, looking about, ears and nose twitching. */
    static void rest(MochiPose o, float t) {
        loaf(o);
        float br = Art.wave(t, 3.4f);
        o.bodySY = 0.96f + 0.018f * br;
        float look = Art.wave(t, 9.2f) * 0.7f + Art.wave(t, 5.3f) * 0.3f;
        o.headDx = 1f; o.headDy = 3f - 0.5f * br; o.headRot = -3f + 5.5f * look;
        o.lookX = 1.2f * Art.wave(t + 1.2f, 7.1f); o.lookY = 0.4f * look;
        o.eyeOpen = blink(t, 0.78f, 0.2f);
        o.earRot = earFlick(t, 5.1f) + 3f * Art.wave(t, 2.9f);
        o.nose = Art.frac(t / 3.7f) < 0.18f ? Art.sin(t * 34f) : 0f;
    }

    /**
     * SCRATCH: sits up on the haunches, tips the head back towards the foot, and brings the
     * hind leg up so the foot pokes out above the shoulders and whirrs at the spot behind the
     * ear, eyes squeezed shut in bliss. Eases in and out of the pose within {@code dur}.
     */
    static void scratch(MochiPose o, float t, float dur) {
        o.stand();
        float k = Art.smooth(t, 0f, 0.5f) * (1f - Art.smooth(t, dur - 0.5f, dur));
        float up = Art.smooth(t, 0.15f, 0.6f) * (1f - Art.smooth(t, dur - 0.6f, dur - 0.15f));
        float burst = Art.frac((t - 0.6f) / 1.7f) < 0.78f && up > 0.98f ? 1f : 0f;
        double w = t * Art.TAU * 5.5;
        float whirr = Art.sin(w) * burst, whirr2 = Art.cos(w) * burst;
        o.bodyY = Art.lerp(38f, 33f, k); o.bodyRot = -28f * k; o.haunch = 0.35f + 0.65f * k;
        o.legA[FN] = 28f * k; o.legL[FN] = 1f + 0.3f * k;
        o.legA[FF] = 21f * k; o.legL[FF] = 1f + 0.3f * k;
        o.legA[HF] = 62f * k; o.legL[HF] = 1f - 0.4f * k;
        o.legL[HN] = 0f; o.hindOver = true;
        // thigh up along the flank, knee forward, shin and foot rising clear of the shoulders
        o.kneeX = Art.lerp(MochiPose.KNEE_X, -21f + 1.5f * whirr2, up); o.kneeY = Art.lerp(MochiPose.KNEE_Y, -24f + 2f * whirr, up);
        o.reachX = Art.lerp(MochiPose.FOOT_X, 0f + 5f * whirr, up); o.reachY = Art.lerp(MochiPose.FOOT_Y, -39f - 2.5f * whirr2, up);
        o.headDx = (-14f + 1.2f * whirr) * k; o.headDy = -1f * k; o.headRot = (25f + 3.5f * whirr) * k;
        o.eye = k > 0.5f ? MochiPose.EYE_HAPPY : MochiPose.EYE_OPEN; o.blush = 1f + 0.7f * k;
        o.earRot = (12f + 9f * whirr) * k;
        o.scratch = burst; o.jaw = 0.2f * k;
        o.bodySX = 1f + 0.014f * whirr;
    }

    /** GRAZE: head down tearing grass, then lifting a little to chew with a blade sticking out. */
    static void graze(MochiPose o, float t) {
        o.stand();
        float cyc = Art.frac(t / 4.4f) * 4.4f;
        float up = Art.smooth(cyc, 1.5f, 1.9f) * (1f - Art.smooth(cyc, 4.0f, 4.4f));
        float tug = (1f - up) * Art.sin(t * Art.TAU * 2.6f);
        float chew = up * (0.5f + 0.5f * Art.sin(t * Art.TAU * 3.1f));
        o.bodyY = 37f; o.bodyRot = 7f - 2f * up;
        o.legA[FN] = -7f + 3f; o.legA[FF] = -7f - 4f; o.legA[HN] = -7f - 3f; o.legA[HF] = -7f + 5f;
        o.headDx = 2f - 1f * up; o.headDy = Art.lerp(20.5f, 13f, up) + 1.2f * tug + 0.7f * chew;
        o.headRot = Art.lerp(44f, 24f, up) + 2f * tug;
        o.eyeOpen = blink(t, 0.7f, 0.6f); o.lookY = 1f - up;
        o.jaw = chew * 0.55f;
        o.item = up > 0.05f ? MochiPose.ITEM_GRASS : MochiPose.ITEM_NONE;
        o.itemPhase = Art.ramp(cyc, 1.5f, 4.2f);
        o.earRot = 4f * Art.wave(t, 2.3f) + earFlick(t, 6.3f);
    }

    /** NIBBLE: up on the hind legs, front paws propped on the old stump, stretching to a leafy sprig. Returns reach 0..1. */
    static float nibble(MochiPose o, float t) {
        o.stand();
        float cyc = Art.frac(t / 3.2f) * 3.2f;
        float reach = Art.smooth(cyc, 0f, 0.5f) * (1f - Art.smooth(cyc, 1.5f, 2.0f));
        float chew = (1f - reach) * (0.5f + 0.5f * Art.sin(t * Art.TAU * 3.3f));
        float rot = -50f - 9f * reach;
        o.bodyRot = rot; o.bodyY = 46.5f + 3.5f * reach; o.bodySX = 1.02f + 0.05f * reach; o.haunch = 1f;
        o.legA[HN] = -rot + 4f; o.legA[HF] = -rot - 8f; o.legL[HN] = 0.92f; o.legL[HF] = 0.92f;
        o.legA[FN] = -rot - 44f - 10f * reach; o.legL[FN] = 0.8f + 0.1f * reach;
        o.legA[FF] = -rot - 36f - 10f * reach; o.legL[FF] = 0.8f + 0.1f * reach;
        o.headRot = 26f - 8f * reach + 1.5f * chew; o.headDx = -3f; o.headDy = 3f + 0.8f * chew;
        o.eyeOpen = reach > 0.5f ? 0.95f : blink(t, 0.6f, 0.1f); o.lookX = 1f; o.lookY = -1f;
        o.jaw = Math.max(chew * 0.5f, reach > 0.8f ? 0.5f : 0f);
        o.item = reach < 0.3f && cyc > 1.5f ? MochiPose.ITEM_LEAF : MochiPose.ITEM_NONE;
        o.itemPhase = Art.ramp(cyc, 1.6f, 3.1f);
        o.earRot = 8f;
        return reach;
    }

    /** PLAY, variant 0: a happy roll on the back, paws paddling in the air. */
    static void playRoll(MochiPose o, float t, float dur) {
        o.stand();
        float in = Art.overshoot(Art.ramp(t, 0f, 0.55f)), out = Art.smooth(t, dur - 0.5f, dur);
        float k = in * (1f - out);
        float rock = Art.wave(t, 1.25f) * k;
        o.bodyRot = -(172f + 16f * rock) * k;
        o.bodyY = Art.lerp(38f, 26.5f, Art.clamp01(k));
        o.bodySX = 1f + 0.03f * k; o.haunch = 0.35f + 0.5f * k;
        float pad = Art.TAU * 2.1f * t;
        o.legA[FN] = (-18f + 26f * Art.sin(pad)) * k; o.legA[FF] = (-12f + 26f * Art.sin(pad + 2.1f)) * k;
        o.legA[HN] = (16f + 24f * Art.sin(pad + 1f)) * k; o.legA[HF] = (12f + 24f * Art.sin(pad + 3.3f)) * k;
        for (int i = 0; i < 4; i++) o.legL[i] = 1f - 0.2f * k;
        o.headDx = -3f * k; o.headDy = 5f * k; o.headRot = (-20f + 9f * rock) * k;
        o.eye = k > 0.4f ? MochiPose.EYE_SLEEP : MochiPose.EYE_OPEN; // upside down, the sleepy curve reads as a grin
        o.blush = 1f + 0.7f * k; o.jaw = 0.5f * k; o.earRot = -8f * k;
        o.hearts = 0.9f * k * Art.smooth(t, 1.0f, 1.6f); o.heartPhase = Art.frac(t / 2.2f);
        o.shadow = 1f;
    }

    /** PLAY, variant 1: "popcorn" hops on the spot with a mid-air wiggle. */
    static void playPopcorn(MochiPose o, float t, float energy) {
        o.stand();
        float per = 0.66f, k = Art.frac(t / per);
        int n = (int) Math.floor(t / per);
        float air = Art.ramp(k, 0.18f, 0.86f);
        float h = Art.hump(air);
        float squash = k < 0.18f ? Art.hump(k / 0.18f) : (k > 0.86f ? Art.hump((k - 0.86f) / 0.14f) * 0.7f : 0f);
        float dir = (n % 2 == 0) ? 1f : -1f;
        o.lift = (15f + 9f * energy) * h;
        o.bodySY = 1f - 0.13f * squash + 0.06f * h; o.bodySX = 1f + 0.1f * squash - 0.04f * h;
        o.bodyY = 38f - 4.5f * squash;
        o.bodyRot = dir * 11f * Art.sin(air * Art.TAU) * (air > 0f && air < 1f ? 1f : 0f);
        o.legA[FN] = -34f * h; o.legA[FF] = -24f * h; o.legA[HN] = 36f * h; o.legA[HF] = 26f * h;
        o.headRot = -8f * h + dir * 4f * h; o.headDy = 2f * squash;
        o.eye = h > 0.25f ? MochiPose.EYE_HAPPY : MochiPose.EYE_OPEN; o.eyeOpen = 0.95f;
        o.earRot = 22f * h - 8f * squash; o.blush = 1.5f; o.jaw = 0.6f * h;
    }

    /**
     * PLAY, variant 2: a blossom drifts down and lands on the nose; Mochi holds very still,
     * going cross-eyed at it, until a sneeze sends it flying.
     */
    static void playBalance(MochiPose o, float t, float dur) {
        loaf(o);
        float fall = Art.ramp(t, 0.3f, 1.7f), land = Art.smooth(t, 1.55f, 1.8f);
        float ts = dur - 1.5f;                                   // the sneeze
        float wind = Art.smooth(t, ts, ts + 0.55f), snap = Art.smooth(t, ts + 0.55f, ts + 0.68f);
        float after = Art.ramp(t, ts + 0.6f, dur);
        float on = land * (1f - snap);
        float wob = (Art.wave(t, 1.9f) * 0.7f + Art.wave(t, 0.83f) * 0.3f) * on;
        // watches it come down, then balances it
        o.headDy = 1f - 3f * on + 2.5f * snap * (1f - after);
        o.headRot = -9f * Math.max(on, fall * (1f - snap)) + 3.5f * wob - 13f * wind * (1f - snap) + 15f * snap * (1f - after);
        o.headDx = 1.5f * wob + 3f * snap * (1f - after);
        o.eyeOpen = 0.98f; o.lookX = 1.6f * on; o.lookY = -1.4f * Math.max(on, fall * (1f - snap));
        if (wind > 0.5f && after < 0.5f) o.eye = MochiPose.EYE_SLEEP;   // screwed shut for the "choo"
        o.jaw = 0.9f * snap * (1f - after) + 0.25f * wind * (1f - snap);
        o.item = t > 0.3f && after < 1f ? MochiPose.ITEM_BLOSSOM : MochiPose.ITEM_NONE;
        float drop = 1f - (0.5f * fall + 0.5f * Art.smooth(fall));                 // eased descent
        o.itemY = -62f * drop + (-26f * after + 70f * after * after) * snap;
        o.itemX = 9f * Art.sin(fall * 8f) * (1f - land) + 44f * after * snap;
        o.itemAlpha = Art.smooth(t, 0.3f, 0.5f) * (1f - Art.smooth(after, 0.55f, 1f));
        o.itemPhase = Art.frac(t / 6f);
        o.earRot = -6f * on + 4f * wob + 16f * snap * (1f - after); o.blush = 1.4f;
        o.bodySX = 1.05f - 0.03f * wind * (1f - snap) + 0.04f * snap * (1f - after);
    }

    /**
     * WALK: an unhurried trot. {@code w} is the stride phase in radians (the director derives it
     * from distance covered, so the feet do not skate); {@code pep} 0..1 makes it bouncier.
     */
    static void walk(MochiPose o, float w, float t, float pep) {
        o.stand();
        float amp = 22f + 11f * pep;
        o.legA[FN] = amp * Art.sin(w); o.legA[HF] = amp * Art.sin(w + 0.4f);
        o.legA[FF] = -amp * Art.sin(w); o.legA[HN] = -amp * Art.sin(w + 0.4f);
        float bob = Math.abs(Art.sin(w));
        o.bodyY = 36.6f + (2.2f + 1f * pep) * bob; o.bodyRot = (2f + 1.2f * pep) * Art.sin(w + 0.8f);
        o.headDy = 1.6f * Art.sin(w * 2f + 0.6f); o.headRot = -2f - 5f * pep + 2f * Art.sin(w);
        o.earRot = 5f * Art.sin(w * 2f) + 14f * pep; o.eyeOpen = blink(t, 0.82f + 0.15f * pep, 0.3f);
        o.jaw = 0.45f * pep; o.blush = 1f + 0.5f * pep;
        o.lift = 2.5f * pep * bob;
    }

    /** Stride phase for the lively afterglow trot, whose speed round the track matches this cadence. */
    static float trotPhase(float t, float pep) { return Art.TAU * t * (2.0f + 1.1f * pep); }

    /** Flat-out gallop used while the alarm rings: ears back, legs a blur, dust flying. */
    static void gallop(MochiPose o, float t) {
        o.stand();
        float w = Art.TAU * t * 4.3f;
        o.legA[FN] = -12f + 50f * Art.sin(w); o.legA[FF] = -12f + 50f * Art.sin(w + 0.5f);
        o.legA[HN] = 12f + 50f * Art.sin(w + 2.3f); o.legA[HF] = 12f + 50f * Art.sin(w + 2.8f);
        o.legBlur = 1f;
        o.bodyRot = 7f * Art.sin(w + 1.1f); o.bodySX = 1.07f + 0.05f * Art.sin(w + 0.4f); o.bodySY = 0.97f;
        o.lift = 5f * Math.max(0f, Art.sin(w + 0.9f)); o.bodyY = 37f;
        o.headRot = -9f + 3f * Art.sin(w + 2f); o.headDx = 2f; o.headDy = -1f;
        o.earRot = 42f; o.eye = MochiPose.EYE_SPARKLE; o.jaw = 0.85f; o.blush = 1.6f;
        o.dust = 1f; o.dustPhase = Art.frac(t * 2.4f); o.alertPhase = Art.frac(t);
    }

    /**
     * Rounding an end of the track, seen head-on (coming towards the window) or from behind
     * (going away): a bounding run. {@code pep} 1 is the alarm gallop, less is the afterglow trot.
     */
    static void runEnd(MochiPose o, float t, float pep, boolean away) {
        o.stand();
        o.view = away ? MochiPose.BACK : MochiPose.FRONT;
        float hop = Math.abs(Art.sin(t * Art.TAU * (2.0f + 2.3f * pep)));
        o.lift = (3f + 6f * pep) * hop;
        o.bodySY = 1f + 0.05f * hop; o.bodySX = 1f - 0.03f * hop;
        o.bodyRot = (3f + 3f * pep) * Art.sin(t * Art.TAU * (2.0f + 2.3f * pep));
        o.eye = pep > 0.9f ? MochiPose.EYE_SPARKLE : MochiPose.EYE_OPEN; o.eyeOpen = 1f;
        o.jaw = 0.3f + 0.5f * pep; o.blush = 1.2f + 0.4f * pep;
        o.earRot = 8f * Art.sin(t * 19f); o.alertPhase = Art.frac(t);
        o.dust = 0f;
    }

    /** A startled hop on the spot with alert marks: the first half-second of an alarm. */
    static void startle(MochiPose o, float t, float dur) {
        o.stand();
        float k = Art.ramp(t, 0f, dur), h = Art.hump(k);
        o.lift = 16f * h; o.bodySY = 1f + 0.08f * h; o.bodySX = 1f - 0.05f * h;
        o.legA[FN] = -20f * h; o.legA[FF] = -12f * h; o.legA[HN] = 20f * h; o.legA[HF] = 12f * h;
        o.eye = MochiPose.EYE_SPARKLE; o.earRot = -18f; o.headRot = -10f * h; o.jaw = 0.5f;
        o.alert = 1f; o.alertPhase = k; o.blush = 1.4f;
    }

    /** Front view, bounding towards the window: {@code k} 0..1 is how far through the dash. */
    static void approach(MochiPose o, float t, float k) {
        o.stand();
        o.view = MochiPose.FRONT;
        float hop = Math.abs(Art.sin(t * Art.TAU * 2.6f));
        o.lift = 12f * hop * (1f - 0.5f * k);
        o.bodySY = 1f + 0.05f * hop; o.bodySX = 1f - 0.03f * hop;
        o.eye = MochiPose.EYE_SPARKLE; o.jaw = 0.8f; o.blush = 1.5f;
        o.pawsUp = Art.smooth(k, 0.55f, 0.95f);
        o.earRot = 10f * Art.sin(t * 20f); o.alertPhase = Art.frac(t);
        o.headDx = 0f; o.headDy = 0f; o.headRot = 4f * Art.sin(t * 9f);
    }

    /** Front view, EXCITED at the window: paws on the sill, bouncing, sparkling eyes, alert marks. */
    static void windowExcited(MochiPose o, float t) {
        o.stand();
        o.view = MochiPose.FRONT;
        float per = 0.46f, k = Art.frac(t / per);
        float h = Art.hump(Art.ramp(k, 0.12f, 1f));
        float squash = k < 0.12f ? Art.hump(k / 0.12f) : 0f;
        o.lift = 15f * h - 2.5f * squash;
        o.bodySY = 1f + 0.05f * h - 0.05f * squash; o.bodySX = 1f - 0.03f * h + 0.04f * squash;
        o.pawsUp = 1f; o.overSill = true;
        o.headDx = 0f; o.headDy = -1.5f * h; o.headRot = 7f * Art.wave(t, 1.38f);
        o.eye = MochiPose.EYE_SPARKLE; o.lookY = -0.4f;
        o.jaw = 0.65f + 0.35f * Art.sin(t * Art.TAU * 2.2f);
        o.earRot = 9f * Art.sin(t * Art.TAU * 3.1f); o.blush = 1.7f;
        o.alert = 1f; o.alertPhase = Art.frac(t);
    }

    /** Front view, calmly peeking in at the window after the alarm: head tilts, blinks, a small bob. */
    static void windowPeek(MochiPose o, float t, float energy) {
        o.stand();
        o.view = MochiPose.FRONT;
        float bob = Art.wave(t, 1.1f);
        float hopK = Art.frac(t / 2.6f);
        float hop = energy > 0.45f ? Art.hump(Art.ramp(hopK, 0.7f, 0.95f)) : 0f;
        o.lift = 2f * bob + 9f * hop * energy;
        o.pawsUp = 1f; o.overSill = true;
        o.headRot = 10f * Art.wave(t, 3.1f); o.headDy = -1f * bob;
        o.eye = MochiPose.EYE_OPEN; o.eyeOpen = blink(t, 1f, 0.15f); o.lookX = 0.6f * Art.wave(t, 2.3f);
        o.jaw = 0.25f + 0.25f * energy; o.blush = 1.4f;
        o.earRot = 7f * Art.sin(t * Art.TAU * 0.9f) + earFlick(t, 3.3f) * 0.5f;
        o.hearts = 0.8f; o.heartPhase = Art.frac(t / 2.4f);
    }

    /** Front view, sitting on the grass looking up at the window with a happy wiggle. */
    static void sitLook(MochiPose o, float t, float energy) {
        o.stand();
        o.view = MochiPose.FRONT;
        float wig = Art.sin(t * Art.TAU * (1.2f + 1.4f * energy));
        o.bodyRot = 3.2f * wig * (0.4f + energy);
        o.bodySX = 1f + 0.03f * wig * energy; o.bodySY = 1f + 0.015f * Art.wave(t, 3f);
        o.headRot = 8f * Art.wave(t, 2.7f) - 1.5f * wig; o.headDx = 0.6f * wig;
        o.eye = MochiPose.EYE_OPEN; o.eyeOpen = blink(t, 0.98f, 0.5f);
        o.jaw = 0.2f + 0.3f * energy; o.blush = 1.3f;
        o.earRot = 6f * wig + earFlick(t, 4.4f) * 0.5f;
        o.hearts = 0.6f * energy; o.heartPhase = Art.frac(t / 2.8f);
    }

    /** Front view, letting go of the sill and dropping out of sight below it. */
    static void windowDrop(MochiPose o, float k) {
        o.stand();
        o.view = MochiPose.FRONT;
        o.pawsUp = 1f - 0.5f * k; o.overSill = k < 0.25f;
        o.eye = MochiPose.EYE_OPEN; o.eyeOpen = 1f; o.blush = 1.3f; o.jaw = 0.3f;
        o.lift = -150f * k * k;
    }

    // ------------------------------------------------------------------ tap reactions (laid over the current pose)

    /**
     * Tapped while awake: the head comes up to look at the viewer, a happy blink, a heart
     * floats up and a little wiggle; {@code s} is seconds into the reaction. Works on whatever
     * pose is already in {@code o}; {@code still} relaxes walking legs to a stand.
     */
    static void pokeAwake(MochiPose o, float s, boolean still) {
        float k = s / POKE_AWAKE;
        float e = Art.smooth(k, 0f, 0.14f) * (1f - Art.smooth(k, 0.8f, 1f));
        float wig = Art.sin(s * Art.TAU * 3.2f) * Art.smooth(k, 0.22f, 0.36f) * (1f - Art.smooth(k, 0.66f, 0.82f));
        boolean smile = (k > 0.2f && k < 0.42f) || (k > 0.56f && k < 0.66f);
        if (o.view == MochiPose.SIDE) {
            if (Math.abs(o.bodyRot) < 100f) {
                // level the head whatever the body is doing, then tip it up towards the window
                float target = -13f - 0.55f * o.bodyRot;
                o.headRot = Art.lerp(o.headRot, target, e) + 2.5f * wig;
                o.headDy = Art.lerp(o.headDy, Math.min(o.headDy, 1f) - 4f, e);
                o.item = e > 0.5f ? MochiPose.ITEM_NONE : o.item;
            }
            if (still) {
                for (int i = 0; i < 4; i++) o.legA[i] = Art.lerp(o.legA[i], 0f, e);
                o.lift = Art.lerp(o.lift, 0f, e);
            }
            o.bodyRot += 3.4f * wig; o.bodySX += 0.025f * wig;
            o.lookX = Art.lerp(o.lookX, -0.6f, e); o.lookY = Art.lerp(o.lookY, -0.5f, e);
        } else {
            o.headRot += 9f * wig + 5f * e; o.bodyRot += 3f * wig;
        }
        if (e > 0.3f && o.eye != MochiPose.EYE_SPARKLE) {
            o.eye = smile ? MochiPose.EYE_HAPPY : MochiPose.EYE_OPEN;
            o.eyeOpen = 1f;
        }
        o.earRot += 14f * wig - 8f * e;
        o.blush = Math.max(o.blush, 1f + 0.8f * e);
        o.jaw = Math.max(o.jaw, 0.35f * e);
        o.zzz = -1f;
        o.hearts = 1.7f * Art.smooth(k, 0.12f, 0.2f) * (1f - Art.smooth(k, 0.8f, 0.95f));
        o.heartPhase = 0.08f + 0.8f * Art.ramp(k, 0.12f, 1.25f);
    }

    /**
     * Tapped while asleep: one bleary eye opens, a huge yawn, a slow blink, and back to sleep.
     * Laid over the sleeping pose; {@code s} is seconds into the reaction.
     */
    static void pokeAsleep(MochiPose o, float s) {
        float k = s / POKE_ASLEEP;
        float up = Art.smooth(k, 0.02f, 0.14f) * (1f - Art.smooth(k, 0.84f, 1f));
        float yawn = Art.hump(Art.ramp(k, 0.16f, 0.56f));
        float slowBlink = Art.hump(Art.ramp(k, 0.62f, 0.78f));
        o.zzz = -1f;
        o.headDy -= 6f * up + 4f * yawn; o.headRot -= 5f * up + 9f * yawn; o.headDx -= 1f * up;
        o.jaw = Math.max(o.jaw, yawn);
        o.earRot += k < 0.14f ? -26f * Math.abs(Art.sin(k / 0.14f * Art.TAU)) : -6f * up;
        if (up > 0.35f && yawn < 0.4f) {
            o.eye = MochiPose.EYE_OPEN;
            o.eyeOpen = 0.5f * Art.smooth(up, 0.35f, 0.9f) * (1f - slowBlink);
            o.lookX = -0.5f;
        } else {
            o.eye = MochiPose.EYE_SLEEP;
        }
        o.bodySY += 0.03f * yawn; o.blush = Math.max(o.blush, 1.2f);
    }
}
