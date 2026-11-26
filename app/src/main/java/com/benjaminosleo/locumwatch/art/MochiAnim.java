package com.benjaminosleo.locumwatch.art;

/**
 * Mochi's movements. Each method bends a {@link MochiPose} for one activity at a given
 * moment (seconds since the activity began). Pure functions of time: no state, no allocation.
 * The director chooses which one to call and where Mochi is standing.
 */
final class MochiAnim {
    private MochiAnim() {}

    private static final int FN = MochiPose.FN, FF = MochiPose.FF, HN = MochiPose.HN, HF = MochiPose.HF;

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

    /** SLEEP: loafed with chin down, slow breathing, drifting z's, an ear flick, a petal landing on the head. */
    static void sleep(MochiPose o, float t) {
        loaf(o);
        float br = Art.wave(t, 4.2f);
        o.bodySY = 0.96f + 0.03f * br; o.bodySX = 1.05f - 0.012f * br;
        o.headDx = 3f; o.headDy = 16.5f - 0.9f * br; o.headRot = 9f;
        o.eye = MochiPose.EYE_SLEEP;
        o.earRot = 10f + earFlick(t, 7.3f);
        o.zzz = Art.frac(t / 3.4f);
        o.blush = 1.1f;
        o.petal = Art.smooth(t, 5f, 5.6f);
        o.nose = t % 9f < 0.4f ? Art.sin(t * 40f) : 0f;
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

    /** SCRATCH: sat back, hind foot whirring behind the ear, eyes squeezed shut in bliss. */
    static void scratch(MochiPose o, float t) {
        o.stand();
        float burst = Art.frac(t / 1.7f) < 0.76f ? 1f : 0f;
        float whirr = Art.sin(t * Art.TAU * 8.5f) * burst;
        o.bodyY = 30.5f; o.bodyRot = -15f; o.haunch = 1f;
        o.legA[FN] = 17f; o.legA[FF] = 11f;
        o.legA[HF] = 70f; o.legL[HF] = 0.6f;
        o.legA[HN] = -134f + 11f * whirr; o.legL[HN] = 1.3f; o.hindOver = true;
        o.headDx = -19f + 0.8f * whirr; o.headDy = 13f; o.headRot = 24f + 2.2f * whirr;
        o.eye = MochiPose.EYE_HAPPY; o.blush = 1.6f; o.earRot = 14f + 6f * whirr;
        o.scratch = burst; o.jaw = 0.18f;
        o.bodySX = 1f + 0.012f * whirr;
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

    /** NIBBLE: up on the hind legs, stretching to a leafy sprig, front paws dangling. Returns reach 0..1. */
    static float nibble(MochiPose o, float t) {
        o.stand();
        float cyc = Art.frac(t / 3.2f) * 3.2f;
        float reach = Art.smooth(cyc, 0f, 0.5f) * (1f - Art.smooth(cyc, 1.5f, 2.0f));
        float chew = (1f - reach) * (0.5f + 0.5f * Art.sin(t * Art.TAU * 3.3f));
        float rot = -50f - 9f * reach;
        o.bodyRot = rot; o.bodyY = 46.5f + 3.5f * reach; o.bodySX = 1.02f + 0.05f * reach; o.haunch = 1f;
        o.legA[HN] = -rot + 4f; o.legA[HF] = -rot - 8f; o.legL[HN] = 0.92f; o.legL[HF] = 0.92f;
        o.legA[FN] = -rot - 86f + 10f * Art.wave(t, 0.9f) - 14f * reach; o.legL[FN] = 0.74f;
        o.legA[FF] = -rot - 70f - 10f * Art.wave(t, 0.9f) - 14f * reach; o.legL[FF] = 0.74f;
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

    /** PLAY, variant 2: sitting very still with a blossom balanced on the nose, going cross-eyed at it. */
    static void playBalance(MochiPose o, float t, float dur) {
        loaf(o);
        float wob = Art.wave(t, 1.9f) * 0.7f + Art.wave(t, 0.83f) * 0.3f;
        float on = Art.smooth(t, 0.3f, 0.9f) * (1f - Art.smooth(t, dur - 0.9f, dur - 0.5f));
        o.headDy = 1f - 3f * on; o.headRot = -9f * on + 3.5f * wob * on; o.headDx = 1.5f * wob * on;
        o.eyeOpen = 0.98f; o.lookX = 1.6f * on; o.lookY = -1.4f * on;
        o.item = on > 0.3f ? MochiPose.ITEM_BLOSSOM : MochiPose.ITEM_NONE;
        o.itemPhase = Art.frac(t / 6f);
        o.earRot = -6f * on + 4f * wob; o.blush = 1.4f;
    }

    /** WALK: an unhurried trot; {@code pep} 0..1 makes it bouncier (used in the afterglow). */
    static void walk(MochiPose o, float t, float pep) {
        o.stand();
        float w = Art.TAU * t * (2.0f + 1.1f * pep);
        float amp = 21f + 12f * pep;
        o.legA[FN] = amp * Art.sin(w); o.legA[HF] = amp * Art.sin(w + 0.4f);
        o.legA[FF] = -amp * Art.sin(w); o.legA[HN] = -amp * Art.sin(w + 0.4f);
        float bob = Math.abs(Art.sin(w));
        o.bodyY = 37f + (1.2f + 1.6f * pep) * bob; o.bodyRot = (1.4f + 1.6f * pep) * Art.sin(w + 0.8f);
        o.headDy = 1.2f * Art.sin(w * 2f + 0.6f); o.headRot = -2f - 5f * pep + 1.5f * Art.sin(w);
        o.earRot = 4f * Art.sin(w * 2f) + 14f * pep; o.eyeOpen = blink(t, 0.82f + 0.15f * pep, 0.3f);
        o.jaw = 0.45f * pep; o.blush = 1f + 0.5f * pep;
        o.lift = 2.5f * pep * bob;
    }

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
}
