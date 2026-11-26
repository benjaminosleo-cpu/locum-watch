package com.benjaminosleo.locumwatch.art;

/**
 * Decides what Mochi does from moment to moment and keeps {@link #pose} up to date.
 *
 * Deterministic: the same seed and the same sequence of {@link #step} calls always give
 * the same performance. Nothing is allocated after construction.
 *
 * Typical use, once per frame:
 * <pre>
 *   director.step(dtSeconds, hour, alarmRinging, secondsSinceAlarmEnded);
 *   mochi.draw(pen, director.pose);
 * </pre>
 */
public final class MochiDirector {
    /** Activities. WALK is the stroll between spots that links the others. */
    public static final int SLEEP = 0, REST = 1, SCRATCH = 2, GRAZE = 3, NIBBLE = 4, PLAY = 5,
        EXCITED = 6, AFTERGLOW = 7, WALK = 8;
    /** PLAY variants: roll on the back, popcorn hops, balancing a blossom on the nose. */
    public static final int PLAY_ROLL = 0, PLAY_POPCORN = 1, PLAY_BALANCE = 2;
    /** How long Mochi stays lively after an alarm ends. */
    public static final float AFTERGLOW_SECONDS = 180f;
    /** Pass as secondsSinceAlarmEnded when there has been no alarm. */
    public static final double NEVER = -1.0;

    // EXCITED timings (seconds)
    private static final float STARTLE = 0.45f, LAP = 1.65f, DASH = 0.7f, AT_GLASS = 4.6f, DROP = 0.35f;
    private static final float REENTER_X = 262f, REENTER_Y = 520f;
    private static final float NIBBLE_X = 224f, NIBBLE_Y = 300f;
    private static final float[] GRAZE_X = {126f, 228f, 164f, 208f}, GRAZE_Y = {313f, 317f, 326f, 296f};
    // afterglow segments
    private static final int G_PEEK = 0, G_DROP = 1, G_TROT = 2, G_HOP = 3, G_SIT = 4, G_DASH = 5;
    // where in the EXCITED loop we are
    private static final int X_STARTLE = 0, X_RUN = 1, X_DASH = 2, X_GLASS = 3, X_DROP = 4;

    /** The pose to draw this frame. Updated by {@link #step} and {@link #force}. */
    public final MochiPose pose = new MochiPose();

    private final MochiPose from = new MochiPose();
    private final Rng rng;
    private int act = REST, variant, nextAct = REST, nextVariant;
    private float t, dur = 10f;
    private float px = Hero.SPOT_X, py = Hero.SPOT_Y, face = 1f, faceNow = 1f;
    private float wx0, wy0, wx1, wy1;
    private float blendT = 9f, blendDur = 0.45f;
    private float pace = 1f, energy;
    private float exX, exY;
    private boolean exStartle;
    private int exPhase;
    private int seg;
    private float segT, segDur, segX, segY, segTheta, segSweep;
    private boolean glowEnding;
    private float sprig;

    public MochiDirector(long seed) {
        rng = new Rng(seed);
        reset(Palette.DAY);
    }

    /** Starts afresh at the favourite spot: asleep at night, otherwise resting. */
    public void reset(float hour) {
        px = Hero.SPOT_X; py = Hero.SPOT_Y; face = 1f; faceNow = 1f;
        act = Palette.nightAt(hour) > 0.6f ? SLEEP : REST;
        variant = 0; t = 0f; dur = (act == SLEEP ? 120f : 14f) / pace; blendT = 9f; glowEnding = false; sprig = 0f;
        computePose();
    }

    /** Speeds up how quickly Mochi moves on to the next activity (1 = natural; previews use more). */
    public void setPace(float pace) { this.pace = Math.max(0.1f, pace); }

    /** The current activity (one of the constants above). */
    public int activity() { return act; }

    /** True while Mochi is up at the window with paws on the sill. */
    public boolean atWindow() { return pose.overSill; }

    /** 0..1: how hard Mochi is tugging the nibble sprig (the garden bends it to match). */
    public float sprigPull() { return sprig; }

    /**
     * Advances the performance and refreshes {@link #pose}.
     *
     * @param dtSeconds time since the previous call
     * @param hour 0..24, local time of day
     * @param alarmRinging true for as long as the alarm is sounding
     * @param secondsSinceAlarmEnded time since the last alarm stopped, or {@link #NEVER}
     */
    public void step(double dtSeconds, float hour, boolean alarmRinging, double secondsSinceAlarmEnded) {
        float d = (float) Math.max(0.0, Math.min(dtSeconds, 3600.0));
        float since = secondsSinceAlarmEnded < 0 ? -1f : (float) Math.min(secondsSinceAlarmEnded, 1e6);
        do {
            float h = Math.min(d, 0.2f);
            advance(h, hour, alarmRinging, since);
            d -= h;
        } while (d > 0f);
    }

    /**
     * Jumps straight to an activity at a point 0..1 through one showing of it, at its usual
     * spot, and refreshes {@link #pose}. For contact sheets and tests; do not mix with step().
     */
    public void force(int activity, float phase) { force(activity, 0, phase); }

    /** As {@link #force(int, float)}, choosing the PLAY variant. */
    public void force(int activity, int playVariant, float phase) {
        phase = Art.clamp01(phase);
        px = Hero.SPOT_X; py = Hero.SPOT_Y; face = 1f; faceNow = 1f; blendT = 9f; glowEnding = false; sprig = 0f;
        act = activity; variant = playVariant; t = 0f;
        switch (activity) {
            case SLEEP: t = phase * 12f; break;
            case REST: t = phase * 10f; break;
            case SCRATCH: t = phase * 3.4f; break;
            case GRAZE: t = phase * 4.4f; break;
            case NIBBLE: px = NIBBLE_X; py = NIBBLE_Y; t = phase * 3.2f; break;
            case PLAY: dur = playDur(playVariant); t = phase * dur; break;
            case WALK:
                wx0 = 120f; wy0 = 312f; wx1 = 240f; wy1 = 300f; dur = 4.6f; t = phase * dur; nextAct = REST; break;
            case EXCITED:
                exX = px; exY = py; exStartle = true;
                t = phase * (STARTLE + sweepFrom(exX, exY) / Art.TAU * LAP + DASH + AT_GLASS + DROP * 0.99f);
                break;
            default: // AFTERGLOW: play the routine forward from the window
                act = AFTERGLOW; seg = G_PEEK; segT = 0f; segDur = 4f; energy = 0.8f;
                computePose();
                float end = phase * 22f;
                for (float s = 0f; s < end; s += 0.05f) advance(0.05f, Palette.DAY, false, 36f);
                blendT = 9f;
                break;
        }
        computePose();
    }

    // ------------------------------------------------------------------ scheduling

    private void advance(float h, float hour, boolean ringing, float since) {
        float df = face - faceNow;
        faceNow += Art.clamp(df, -h * 7f, h * 7f);
        blendT += h;
        t += h;
        boolean glow = since >= 0f && since < AFTERGLOW_SECONDS;
        if (ringing) {
            if (act != EXCITED) {
                exX = px; exY = py; exStartle = pose.view == MochiPose.SIDE && py < 360f;
                begin(EXCITED, 0, 1e9f);
            }
        } else if (act == EXCITED) {
            energy = glow ? 1f - since / AFTERGLOW_SECONDS : 1f;
            from.set(pose); blendT = 0f; act = AFTERGLOW; t = 0f; glowEnding = false;
            if (exPhase == X_GLASS || exPhase == X_DASH) startSeg(G_PEEK);
            else if (exPhase == X_DROP) startSeg(G_DROP);
            else startSeg(G_TROT);
        } else if (act == AFTERGLOW) {
            energy = glow ? 1f - since / AFTERGLOW_SECONDS : 0f;
            if (!glow) glowEnding = true;
            segT += h;
            if (glowEnding && (seg == G_TROT || seg == G_HOP || seg == G_SIT) && segT > 0.3f) {
                settle();
            } else if (segT >= segDur) {
                nextSeg();
            }
        } else if (glow && act != WALK) {
            // woken up part-way through an afterglow (for example the screen was reopened)
            energy = 1f - since / AFTERGLOW_SECONDS;
            from.set(pose); blendT = 0f; act = AFTERGLOW; t = 0f; glowEnding = false;
            startSeg(G_TROT);
        } else if (t >= dur) {
            if (act == WALK) {
                px = wx1; py = wy1;
                if (nextAct == NIBBLE) face = 1f;
                begin(nextAct, nextVariant, durationOf(nextAct, nextVariant, hour));
            } else {
                chooseNext(hour);
            }
        }
        computePose();
    }

    private void begin(int a, int v, float d) {
        from.set(pose); blendT = 0f; blendDur = 0.45f;
        act = a; variant = v; t = 0f; dur = d;
    }

    private static float playDur(int v) { return v == PLAY_ROLL ? 6.5f : (v == PLAY_POPCORN ? 0.66f * 6f : 7f); }

    private float durationOf(int a, int v, float hour) {
        boolean night = Palette.nightAt(hour) > 0.6f;
        switch (a) {
            case SLEEP: return (night ? rng.r(300f, 900f) : rng.r(80f, 260f)) / pace;
            case REST: return (night ? rng.r(8f, 16f) : rng.r(22f, 60f)) / pace;
            case GRAZE: return 4.4f * Math.max(1, Math.round(rng.r(5f, 12f) / pace));
            case SCRATCH: return 1.7f * (pace > 1.5f ? 2 : 3 + rng.n(2));
            case NIBBLE: return 3.2f * Math.max(1, Math.round(rng.r(3f, 5f) / Math.min(pace, 3f)));
            case PLAY: return playDur(v);
            default: return 5f;
        }
    }

    /** Picks the next thing to do, walking to its spot first if it is somewhere else. */
    private void chooseNext(float hour) {
        float night = Palette.nightAt(hour);
        int a; int v = 0;
        if (night > 0.6f) {
            a = act == SLEEP && rng.chance(0.3f) ? REST : SLEEP;
        } else {
            float sleepy = 0.20f + 0.5f * night;
            float r = rng.f();
            if (r < sleepy) a = SLEEP;
            else if (r < sleepy + 0.26f) a = REST;
            else if (r < sleepy + 0.54f) a = GRAZE;
            else if (r < sleepy + 0.62f) a = SCRATCH;
            else if (r < sleepy + 0.70f) a = NIBBLE;
            else a = PLAY;
            if (a == act) a = act == GRAZE ? REST : GRAZE;
            if (a == PLAY) v = rng.n(3);
        }
        float tx = px, ty = py;
        if (a == GRAZE) {
            int g = rng.n(GRAZE_X.length);
            if (Math.abs(GRAZE_X[g] - px) < 12f) g = (g + 1) % GRAZE_X.length;
            tx = GRAZE_X[g]; ty = GRAZE_Y[g];
        } else if (a == NIBBLE) {
            tx = NIBBLE_X; ty = NIBBLE_Y;
        } else if (a == SLEEP || a == PLAY || Math.abs(px - Hero.SPOT_X) > 34f || Math.abs(py - Hero.SPOT_Y) > 12f) {
            tx = Hero.SPOT_X; ty = Hero.SPOT_Y;
        }
        goThenDo(tx, ty, a, v, hour, 0f);
    }

    private void goThenDo(float tx, float ty, int a, int v, float hour, float pep) {
        float dx = tx - px, dy = ty - py, dist = (float) Math.sqrt(dx * dx + dy * dy);
        if (dist < 5f) {
            if (a == NIBBLE) face = 1f;
            begin(a, v, durationOf(a, v, hour));
            return;
        }
        wx0 = px; wy0 = py; wx1 = tx; wy1 = ty; nextAct = a; nextVariant = v;
        if (Math.abs(dx) > 3f) face = dx > 0 ? 1f : -1f;
        begin(WALK, 0, Math.max(0.8f, dist / (27f + 20f * pep)));
    }

    // ------------------------------------------------------------------ afterglow routine

    private void startSeg(int s) {
        seg = s; segT = 0f;
        switch (s) {
            case G_PEEK: segDur = (4f + 3f * (1f - energy)) / Math.min(pace, 2f); break;
            case G_DROP: segDur = DROP; break;
            case G_TROT:
                segX = px; segY = py;
                segTheta = thetaOf(px, py);
                segSweep = wrap((float) Math.PI - segTheta) + (energy > 0.5f ? Art.TAU : 0f);
                if (segSweep < 1.2f) segSweep += Art.TAU;
                segDur = segSweep / Art.TAU * Art.lerp(5.2f, 2.5f, energy);
                break;
            case G_HOP: segDur = 0.66f * 4f; break;
            case G_SIT: segDur = Art.lerp(9f, 4f, energy) / Math.min(pace, 2f); break;
            default: segDur = 0.8f; break;
        }
    }

    private void nextSeg() {
        from.set(pose); blendT = 0f; blendDur = 0.3f;
        switch (seg) {
            case G_PEEK: startSeg(G_DROP); break;
            case G_DROP:
                px = REENTER_X; py = REENTER_Y;
                if (glowEnding) { settle(); } else { startSeg(G_TROT); }
                break;
            case G_TROT: startSeg(energy > 0.35f ? G_HOP : G_SIT); break;
            case G_HOP: startSeg(G_SIT); break;
            case G_SIT: startSeg(energy > 0.3f ? G_DASH : G_TROT); break;
            default: startSeg(glowEnding ? G_DROP : G_PEEK); break;
        }
    }

    /** Afterglow over: amble back to the favourite spot and rest. */
    private void settle() {
        glowEnding = false;
        act = REST;
        goThenDo(Hero.SPOT_X, Hero.SPOT_Y, REST, 0, Palette.DAY, 0.2f);
    }

    // ------------------------------------------------------------------ posing

    private void place() {
        pose.x = px; pose.y = py; pose.scale = Hero.depthScale(py); pose.facing = faceNow;
    }

    private void computePose() {
        sprig = 0f;
        switch (act) {
            case SLEEP: MochiAnim.sleep(pose, t); place(); break;
            case REST: MochiAnim.rest(pose, t); place(); break;
            case SCRATCH: MochiAnim.scratch(pose, t); place(); break;
            case GRAZE: MochiAnim.graze(pose, t); place(); break;
            case NIBBLE: sprig = MochiAnim.nibble(pose, t); place(); break;
            case PLAY:
                if (variant == PLAY_ROLL) MochiAnim.playRoll(pose, t, dur);
                else if (variant == PLAY_POPCORN) MochiAnim.playPopcorn(pose, t, 0.6f);
                else MochiAnim.playBalance(pose, t, dur);
                place();
                break;
            case WALK: {
                float k = Art.smooth(t / dur);
                px = Art.lerp(wx0, wx1, k); py = Art.lerp(wy0, wy1, k);
                MochiAnim.walk(pose, t, 0f);
                place();
                break;
            }
            case EXCITED: excitedPose(); break;
            default: glowPose(); break;
        }
        if (blendT < blendDur) pose.blendFrom(from, Art.smooth(blendT / blendDur));
    }

    private static float wrap(float a) {
        a %= Art.TAU;
        return a < 0f ? a + Art.TAU : a;
    }

    /** Angle round the running track nearest to a point (0 = far side, pi = nearest the window). */
    private static float thetaOf(float x, float y) {
        return (float) Math.atan2((x - Hero.RING_X) / Hero.RING_RX, -(y - Hero.RING_Y) / Hero.RING_RY);
    }

    /** Angle to run from a start point so the run ends nearest the window after two-and-a-bit laps. */
    private static float sweepFrom(float x, float y) {
        return wrap((float) Math.PI - thetaOf(x, y)) + 2f * Art.TAU;
    }

    /** Puts Mochi on the track at angle theta, easing in from (sx,sy) over the first moments (k 0..1). */
    private void onTrack(float theta, float sx, float sy, float k) {
        float ex = Hero.RING_X + Hero.RING_RX * Art.sin(theta), ey = Hero.RING_Y - Hero.RING_RY * Art.cos(theta);
        float b = Art.smooth(k);
        px = Art.lerp(sx, ex, b); py = Art.lerp(sy, ey, b);
        face = Art.cos(theta) >= 0f ? 1f : -1f;
        faceNow = Art.clamp(Art.cos(theta) * 3.2f, -1f, 1f);
        pose.x = px; pose.y = py; pose.scale = Hero.depthScale(py); pose.facing = faceNow;
    }

    private void placeAtWindow() {
        pose.x = Hero.RING_X; pose.y = Hero.AT_WINDOW_Y; pose.scale = Hero.AT_WINDOW_SCALE; pose.facing = 1f;
    }

    private void dashPose(float u, float len) {
        float k = Art.clamp01(u / len), e = Art.smooth(k);
        float y0 = Hero.RING_Y + Hero.RING_RY;
        MochiAnim.approach(pose, t, k);
        pose.x = Hero.RING_X; pose.y = Art.lerp(y0, Hero.AT_WINDOW_Y, e);
        pose.scale = Art.lerp(Hero.depthScale(y0), Hero.AT_WINDOW_SCALE, e);
        pose.facing = Art.lerp(0.45f, 1f, Art.smooth(k, 0f, 0.25f));
        pose.overSill = k > 0.85f;
        px = pose.x; py = Math.min(pose.y, 440f);
    }

    private void excitedPose() {
        float sweep = sweepFrom(exX, exY), run = sweep / Art.TAU * LAP;
        float startle = exStartle ? STARTLE : 0f;
        float first = startle + run + DASH + AT_GLASS + DROP;
        float u = t, sx = exX, sy = exY;
        if (u >= first) {
            sweep = sweepFrom(REENTER_X, REENTER_Y); run = sweep / Art.TAU * LAP;
            u = (u - first) % (run + DASH + AT_GLASS + DROP);
            sx = REENTER_X; sy = REENTER_Y; startle = 0f;
        }
        if (u < startle) {
            exPhase = X_STARTLE;
            MochiAnim.startle(pose, u, startle);
            place();
            return;
        }
        u -= startle;
        if (u < run) {
            exPhase = X_RUN;
            MochiAnim.gallop(pose, t);
            onTrack((float) Math.PI - sweep + sweep * (u / run), sx, sy, u / 0.6f);
            // lean into the turn towards the window at the very end
            pose.facing *= 1f - 0.5f * Art.smooth(u, run - 0.12f, run);
            return;
        }
        u -= run;
        if (u < DASH) { exPhase = X_DASH; dashPose(u, DASH); return; }
        u -= DASH;
        if (u < AT_GLASS) {
            exPhase = X_GLASS;
            MochiAnim.windowExcited(pose, u);
            placeAtWindow();
            px = Hero.RING_X; py = 440f;
            return;
        }
        exPhase = X_DROP;
        MochiAnim.windowDrop(pose, (u - AT_GLASS) / DROP);
        placeAtWindow();
    }

    private void glowPose() {
        switch (seg) {
            case G_PEEK: MochiAnim.windowPeek(pose, segT, energy); placeAtWindow(); break;
            case G_DROP: MochiAnim.windowDrop(pose, segT / segDur); placeAtWindow(); break;
            case G_TROT:
                MochiAnim.walk(pose, t, 0.25f + 0.75f * energy);
                onTrack(segTheta + segSweep * Art.clamp01(segT / segDur), segX, segY, segT / 0.9f);
                break;
            case G_HOP: MochiAnim.playPopcorn(pose, segT, energy); place(); pose.facing = face; break;
            case G_SIT:
                MochiAnim.sitLook(pose, segT, energy); place();
                pose.facing = Art.lerp(0.45f, 1f, Art.smooth(segT, 0f, 0.2f));
                break;
            default: dashPose(segT, segDur); break;
        }
    }
}
