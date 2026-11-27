package com.benjaminosleo.locumwatch.art;

/**
 * Everything that describes one frame of Mochi: where the capybara is and how each
 * part of the rig is bent. Filled in by {@link MochiDirector} (or {@link MochiAnim})
 * and drawn by {@link Mochi}. Plain mutable numbers so it can be reused every frame.
 */
public final class MochiPose {
    /** Views: seen from the side, facing the window, or from behind (running away round the far turn). */
    public static final int SIDE = 0, FRONT = 1, BACK = 2;
    /** Eye styles. */
    public static final int EYE_OPEN = 0, EYE_SLEEP = 1, EYE_HAPPY = 2, EYE_SPARKLE = 3;
    /** Things carried: nothing, a grass blade in the mouth, a leaf in the mouth, a blossom balanced on the nose. */
    public static final int ITEM_NONE = 0, ITEM_GRASS = 1, ITEM_LEAF = 2, ITEM_BLOSSOM = 3;
    /** Leg indices: front near, front far, hind near, hind far. */
    public static final int FN = 0, FF = 1, HN = 2, HF = 3;

    public int view;
    /** Ground point under the middle of the body, hero units. */
    public float x, y;
    /** Overall size; 1 is the rig's native size (about 110 units long). */
    public float scale;
    /** Side view: +1 faces right, -1 faces left; in-between values squash for a turn. */
    public float facing;
    /** Height of a jump above the ground. */
    public float lift;
    /** Ground shadow strength 0..1. */
    public float shadow;

    /** Body: height of its centre above the ground, tilt in degrees, squash and stretch. */
    public float bodyY, bodyRot, bodySX, bodySY;
    /** Head offset and tilt relative to the body. */
    public float headDx, headDy, headRot;
    /** Ear angle: positive lays the ears back. */
    public float earRot;
    public int eye;
    /** 0 shut .. 1 wide (EYE_OPEN only). */
    public float eyeOpen;
    public float lookX, lookY;
    /** Mouth: 0 closed .. 1 open (chewing, panting). */
    public float jaw;
    /** Cheek blush strength. */
    public float blush;
    /** Nose twitch, -1..1. */
    public float nose;
    /** Leg swing in degrees (0 = straight down from the body) and length factor (0 hides the leg). */
    public final float[] legA = new float[4], legL = new float[4];
    /** Draw the near hind leg over the head (scratching). */
    public boolean hindOver;
    /** 0..1: how rounded-out the near thigh is (1 when sitting or loafing). */
    public float haunch;
    /** 0..1: front paws tucked under the chest (loafing). */
    public float tuck;
    /** 0..1 ghosted extra legs for a running blur. */
    public float legBlur;
    public int item;
    /** 0..1 life of the carried item (wobble, how much is left). */
    public float itemPhase;
    /** 0..1 petal resting on the head. */
    public float petal;
    /** Sleeping z's: negative for none, else the number of z cycles since they began (keeps counting up). */
    public float zzz;
    /** 0..1 alert marks around the head, with a looping phase. */
    public float alert, alertPhase;
    /** 0..1 dust puffs and speed lines behind a run, with a looping phase. */
    public float dust, dustPhase;
    /** 0..1 little motion ticks beside the ear while scratching. */
    public float scratch;
    /** Front view: 0 paws down .. 1 paws up on the window sill. */
    public float pawsUp;
    /** Front view: true when the paws rest on the sill and must be drawn over the window frame. */
    public boolean overSill;
    /** Floating hearts: 0..1 fades them in, above 1 also makes them bigger; with a looping phase. */
    public float hearts, heartPhase;
    /** 0..1: how much of the cottage's candle-light falls on Mochi (1 when up at the window). */
    public float warm;
    /** 0..1: the head petal's fall, 0 high above and 1 settled on the crown. */
    public float petalFall;
    /** Carried blossom: offset from its resting place on the nose (head units) and opacity. */
    public float itemX, itemY, itemAlpha;
    /** Scratching leg (when hindOver): where the foot and the knee are, in body units. */
    public float reachX, reachY, kneeX, kneeY;
    /** Body units: where a hind foot rests on the ground, and its knee (the scratching leg starts and ends here). */
    public static final float FOOT_X = -19f, FOOT_Y = 34f, KNEE_X = -22f, KNEE_Y = 22f;

    public MochiPose() { stand(); }

    /** Neutral standing pose facing right. */
    public MochiPose stand() {
        view = SIDE; scale = 1f; facing = 1f; lift = 0f; shadow = 1f;
        bodyY = 38f; bodyRot = 0f; bodySX = 1f; bodySY = 1f;
        headDx = 0f; headDy = 0f; headRot = 0f; earRot = 0f;
        eye = EYE_OPEN; eyeOpen = 0.85f; lookX = 0f; lookY = 0f; jaw = 0f; blush = 1f; nose = 0f;
        for (int i = 0; i < 4; i++) { legA[i] = 0f; legL[i] = 1f; }
        hindOver = false; haunch = 0.35f; legBlur = 0f; tuck = 0f;
        item = ITEM_NONE; itemPhase = 0f; petal = 0f; zzz = -1f;
        alert = 0f; alertPhase = 0f; dust = 0f; dustPhase = 0f; scratch = 0f;
        pawsUp = 0f; overSill = false; hearts = 0f; heartPhase = 0f;
        warm = 0f; petalFall = 1f; itemX = 0f; itemY = 0f; itemAlpha = 1f;
        reachX = FOOT_X; reachY = FOOT_Y; kneeX = KNEE_X; kneeY = KNEE_Y;
        return this;
    }

    /** Copies every value from another pose. */
    public MochiPose set(MochiPose o) {
        view = o.view; x = o.x; y = o.y; scale = o.scale; facing = o.facing; lift = o.lift; shadow = o.shadow;
        bodyY = o.bodyY; bodyRot = o.bodyRot; bodySX = o.bodySX; bodySY = o.bodySY;
        headDx = o.headDx; headDy = o.headDy; headRot = o.headRot; earRot = o.earRot;
        eye = o.eye; eyeOpen = o.eyeOpen; lookX = o.lookX; lookY = o.lookY; jaw = o.jaw; blush = o.blush; nose = o.nose;
        for (int i = 0; i < 4; i++) { legA[i] = o.legA[i]; legL[i] = o.legL[i]; }
        hindOver = o.hindOver; haunch = o.haunch; legBlur = o.legBlur; tuck = o.tuck;
        item = o.item; itemPhase = o.itemPhase; petal = o.petal; zzz = o.zzz;
        alert = o.alert; alertPhase = o.alertPhase; dust = o.dust; dustPhase = o.dustPhase; scratch = o.scratch;
        pawsUp = o.pawsUp; overSill = o.overSill; hearts = o.hearts; heartPhase = o.heartPhase;
        warm = o.warm; petalFall = o.petalFall; itemX = o.itemX; itemY = o.itemY; itemAlpha = o.itemAlpha;
        reachX = o.reachX; reachY = o.reachY; kneeX = o.kneeX; kneeY = o.kneeY;
        return this;
    }

    /**
     * Eases the rig's bend values from {@code from} towards this pose's own values
     * (k = 0 gives {@code from}, k = 1 leaves this pose untouched). Position, size,
     * facing and the either/or settings always stay this pose's own.
     */
    public void blendFrom(MochiPose from, float k) {
        if (k >= 1f || from.view != view) return;
        lift = Art.lerp(from.lift, lift, k);
        bodyY = Art.lerp(from.bodyY, bodyY, k);
        bodyRot = Art.lerp(from.bodyRot, bodyRot, k);
        bodySX = Art.lerp(from.bodySX, bodySX, k);
        bodySY = Art.lerp(from.bodySY, bodySY, k);
        headDx = Art.lerp(from.headDx, headDx, k);
        headDy = Art.lerp(from.headDy, headDy, k);
        headRot = Art.lerp(from.headRot, headRot, k);
        earRot = Art.lerp(from.earRot, earRot, k);
        jaw = Art.lerp(from.jaw, jaw, k);
        blush = Art.lerp(from.blush, blush, k);
        haunch = Art.lerp(from.haunch, haunch, k);
        legBlur = Art.lerp(from.legBlur, legBlur, k);
        tuck = Art.lerp(from.tuck, tuck, k);
        pawsUp = Art.lerp(from.pawsUp, pawsUp, k);
        for (int i = 0; i < 4; i++) {
            legA[i] = Art.lerp(from.legA[i], legA[i], k);
            legL[i] = Art.lerp(from.legL[i], legL[i], k);
        }
    }
}
