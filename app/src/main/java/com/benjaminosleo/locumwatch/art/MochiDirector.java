package com.benjaminosleo.locumwatch.art;

/**
 * Decides what Mochi does from moment to moment and keeps {@link #pose} up to date.
 *
 * Deterministic: the same seed and the same sequence of calls always give the same
 * performance. Nothing is allocated after construction.
 *
 * Typical use, once per frame:
 * <pre>
 *   director.step(dtSeconds, hour, alarmRinging, secondsSinceAlarmEnded);
 *   mochi.draw(pen, director.pose);
 * </pre>
 * and on a tap: {@code if (director.hitTest(x, y)) director.poke();}
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
    private static final float JOIN = 0.6f, REJOIN = 1.0f;       // easing onto the track: from the grass, from under the sill
    private static final float REENTER_X = 262f, REENTER_Y = 520f;
    private static final float NIBBLE_X = 224f, NIBBLE_Y = 300f;
    private static final float[] GRAZE_X = {142f, 228f, 166f, 208f}, GRAZE_Y = {318f, 317f, 326f, 296f};
    private static final float WALK_SPEED = 38f;
    // a turn: stand up square, swing through the front view, settle facing the other way
    private static final float TURN_A = 0.26f, TURN_B = 0.16f, TURN_C = 0.2f, TURN = TURN_A + TURN_B + TURN_C;
    // on the track, the stretch at each end (|cos| below this) is run head-on or tail-on
    private static final float END_ZONE = 0.34f;
    // afterglow segments
    private static final int G_PEEK = 0, G_DROP = 1, G_TROT = 2, G_HOP = 3, G_SIT = 4, G_DASH = 5;
    // where in the EXCITED loop we are
    private static final int X_STARTLE = 0, X_RUN = 1, X_DASH = 2, X_GLASS = 3, X_DROP = 4;

    /** The pose to draw this frame. Updated by {@link #step}, {@link #force} and {@link #poke}. */
    public final MochiPose pose = new MochiPose();

    private final MochiPose from = new MochiPose();
    private final Rng rng;
    private int act = REST, variant, nextAct = REST, nextVariant;
    private float t, dur = 10f;
    private float px = Hero.SPOT_X, py = Hero.SPOT_Y, face = 1f, faceNow = 1f;
    private float wx0, wy0, wx1, wy1, walkLen;
    private float blendT = 9f, blendDur = 0.45f;
    private float pace = 1f, energy;
    private float exX, exY;
    private boolean exStartle;
    private int exPhase;
    private int seg;
    private float segT, segDur, segX, segY, segTheta, segSweep;
    private boolean glowEnding;
    private float sprig;
    private float turnT = -1f, turnFrom = 1f;
    private float pokeT = -1f, zFrom = 2.8f;
    private boolean pokeAsleep, pokeQueued;
    private float perch;

    public MochiDirector(long seed) {
        rng = new Rng(seed);
        reset(Palette.DAY);
    }

    /** Starts afresh at the favourite spot: asleep at night, otherwise resting. */
    public void reset(float hour) {
        if (hour != hour) hour = Palette.DAY;
        px = Hero.SPOT_X; py = Hero.SPOT_Y; face = 1f; faceNow = 1f;
        act = Palette.nightAt(hour) > 0.6f ? SLEEP : REST;
        variant = 0; dur = (act == SLEEP ? 120f : 14f) / pace; blendT = 9f; glowEnding = false; sprig = 0f;
        t = act == SLEEP ? 8f : 0f; // found already fast asleep, not just lying down
        turnT = -1f; pokeT = -1f; pokeQueued = false; zFrom = 2.8f; perch = 0f;
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

    /** 0..1: how settled the little bird is on Mochi's back (0 = on its fence post). */
    public float birdPerch() { return perch; }

    /** Where on Mochi's back the bird perches, hero units. */
    public float birdPerchX() { return pose.x - 9f * pose.facing * pose.scale; }
    public float birdPerchY() { return pose.y - pose.lift - (pose.bodyY + 27f * pose.bodySY) * pose.scale; }

    /**
     * Advances the performance and refreshes {@link #pose}.
     *
     * @param dtSeconds time since the previous call (negative or not-a-number counts as zero)
     * @param hour 0..24, local time of day
     * @param alarmRinging true for as long as the alarm is sounding
     * @param secondsSinceAlarmEnded time since the last alarm stopped, or {@link #NEVER}
     */
    public void step(double dtSeconds, float hour, boolean alarmRinging, double secondsSinceAlarmEnded) {
        if (!(dtSeconds >= 0)) dtSeconds = 0;
        if (hour != hour) hour = Palette.DAY;
        float d = (float) Math.min(dtSeconds, 3600.0);
        float since = !(secondsSinceAlarmEnded >= 0) ? -1f : (float) Math.min(secondsSinceAlarmEnded, 1e6);
        do {
            float h = Math.min(d, 0.2f);
            advance(h, hour, alarmRinging, since);
            d -= h;
        } while (d > 0f);
    }

    /**
     * True if the point (hero units) is on Mochi's body or head as currently posed, with
     * about 10 units of generosity. Use it to decide whether a tap should {@link #poke()}.
     */
    public boolean hitTest(float x, float y) {
        if (x < Hero.WIN_L || x > Hero.WIN_R || y < Hero.WIN_T || y > Hero.SILL_FRONT) return false;
        MochiPose o = pose;
        float s = o.scale, gy = o.y - o.lift, slack = 10f;
        if (o.view != MochiPose.SIDE) {
            return inOval(x, y, o.x, gy - 57f * s, 33f * s + slack, 31f * s + slack)
                || inOval(x, y, o.x, gy - 26f * s, 36f * s + slack, 27f * s + slack);
        }
        float f = o.facing >= 0f ? 1f : -1f;
        float by = gy - o.bodyY * s;
        double a = Math.toRadians(o.bodyRot);
        float ca = (float) Math.cos(a), sa = (float) Math.sin(a);
        // head centre: the neck point carried round by the body's tilt
        float hx = 35f + o.headDx, hy = -13f + o.headDy;
        float wx = o.x + f * s * (hx * ca - hy * sa), wy = by + s * (hx * sa + hy * ca);
        float tilt = Math.abs(sa);
        return inOval(x, y, o.x, by, (42f - 13f * tilt) * s + slack, (28f + 13f * tilt) * s + slack)
            || inOval(x, y, wx, wy, 26f * s + slack, 22f * s + slack);
    }

    private static boolean inOval(float x, float y, float cx, float cy, float rx, float ry) {
        float dx = (x - cx) / rx, dy = (y - cy) / ry;
        return dx * dx + dy * dy <= 1f;
    }

    /**
     * A tap on Mochi. Awake: looks up at the viewer, a happy blink, a heart, a little wiggle,
     * then carries on (about 2 s). Asleep: one bleary eye, a big yawn, a slow blink, back to
     * sleep (about 3 s). Ignored while the alarm is ringing; taps during a reaction do not
     * queue up (a tap near its end simply starts it again). The schedule waits meanwhile.
     */
    public void poke() {
        if (act == EXCITED) return;
        if (turnT >= 0f) { pokeQueued = true; return; }
        if (pokeT >= 0f) {
            float len = pokeAsleep ? MochiAnim.POKE_ASLEEP : MochiAnim.POKE_AWAKE;
            if (pokeT < 0.7f * len) return;
        }
        pokeAsleep = act == SLEEP && pose.eye == MochiPose.EYE_SLEEP;
        pokeT = 0f;
        computePose();
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
        turnT = -1f; pokeT = -1f; pokeQueued = false; zFrom = 2.8f; perch = 0f;
        act = activity; variant = playVariant; t = 0f; dur = 1e9f;
        switch (activity) {
            case SLEEP: t = phase * 12f; break;
            case REST: t = phase * 10f; break;
            case SCRATCH: dur = 5.6f; t = 0.6f + phase * 3.4f; break;
            case GRAZE: t = phase * 4.4f; break;
            case NIBBLE: px = NIBBLE_X; py = NIBBLE_Y; t = phase * 3.2f; break;
            case PLAY: dur = playDur(playVariant); t = phase * dur; break;
            case WALK:
                wx0 = 120f; wy0 = 312f; wx1 = 240f; wy1 = 300f; nextAct = REST;
                walkLen = (float) Math.hypot(wx1 - wx0, wy1 - wy0); dur = walkLen / WALK_SPEED; t = phase * dur;
                break;
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

    // ------------------------------------------------------------------ alarm page

    /** Length of the first EXCITED cycle from the favourite spot (startle, laps, dash, at the glass, drop). */
    float alarmFirst() { return STARTLE + sweepFrom(Hero.SPOT_X, Hero.SPOT_Y) / Art.TAU * LAP + DASH + AT_GLASS + DROP; }

    /** Length of each later cycle (up from under the sill, laps, dash, at the glass, drop). */
    float alarmLoop() { return sweepFrom(REENTER_X, REENTER_Y) / Art.TAU * LAP + DASH + AT_GLASS + DROP; }

    /**
     * Poses Mochi for the alarm page at {@code seconds} since the page appeared: the EXCITED
     * routine from the favourite spot, then its loop for ever. A pure function of the time
     * (taken modulo the loop in double precision), so it cannot drift however long it runs.
     */
    void poseAlarm(double seconds) {
        seconds = Math.max(0.0, Art.clock(seconds));
        double first = alarmFirst(), loop = alarmLoop();
        if (seconds >= first) seconds = first + (seconds - first) % loop;
        act = EXCITED; exX = Hero.SPOT_X; exY = Hero.SPOT_Y; exStartle = true;
        turnT = -1f; pokeT = -1f; blendT = 9f;
        t = (float) seconds;
        computePose();
    }

    // ------------------------------------------------------------------ scheduling

    private void advance(float h, float hour, boolean ringing, float since) {
        blendT += h;
        boolean glow = since >= 0f && since < AFTERGLOW_SECONDS;
        if (ringing) {
            turnT = -1f; pokeT = -1f; pokeQueued = false;
            if (act != EXCITED) {
                exX = px; exY = py; exStartle = pose.view == MochiPose.SIDE && py < 360f;
                faceNow = face;
                begin(EXCITED, 0, 1e9f);
            }
            t += h;
            perchTowards(0f, h);
            computePose();
            return;
        }
        // a turn or a tap reaction holds everything else still until it is over
        if (turnT >= 0f) {
            turnT += h;
            if (turnT >= TURN) {
                turnT = -1f; faceNow = face;
                turnPoseAt(TURN);
                from.set(pose); blendT = 0f; blendDur = 0.3f;
                if (pokeQueued) { pokeQueued = false; poke(); }
            }
            perchTowards(0f, h);
            computePose();
            return;
        }
        if (pokeT >= 0f) {
            pokeT += h;
            if (pokeT >= (pokeAsleep ? MochiAnim.POKE_ASLEEP : MochiAnim.POKE_AWAKE)) {
                pokeT = -1f;
                if (pokeAsleep) zFrom = t + 0.6f;
            }
            perchTowards(0f, h);
            computePose();
            return;
        }
        t += h;
        if (act == EXCITED) {
            // the alarm has just stopped
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
        // the bird comes over once Mochi has been resting quietly for a while, and leaves in good time
        boolean calm = act == REST && t > 5f && t < dur - 3.5f && Palette.nightAt(hour) < 0.5f;
        perchTowards(calm ? 1f : 0f, h);
        computePose();
    }

    private void perchTowards(float target, float h) {
        float d = target - perch;
        perch += Art.clamp(d, -h / 0.7f, h / 1.1f);
    }

    /** Starts an activity; if Mochi is facing the wrong way for it, a turn is played first. */
    private void begin(int a, int v, float d) {
        from.set(pose); blendT = 0f; blendDur = a == SLEEP || a == REST ? 0.7f : 0.45f; // lying down takes a little longer
        act = a; variant = v; t = 0f; dur = d;
        if (a == SLEEP) zFrom = 2.8f;
        if (a != EXCITED && a != AFTERGLOW && face != faceNow) {
            turnT = 0f; turnFrom = faceNow >= 0f ? 1f : -1f;
        }
    }

    private static float playDur(int v) { return v == PLAY_ROLL ? 6.5f : (v == PLAY_POPCORN ? 0.66f * 6f : 7.5f); }

    private float durationOf(int a, int v, float hour) {
        boolean night = Palette.nightAt(hour) > 0.6f;
        switch (a) {
            case SLEEP: return Math.max(9f, (night ? rng.r(300f, 900f) : rng.r(80f, 260f)) / pace);
            case REST: return (night ? rng.r(8f, 16f) : rng.r(22f, 60f)) / pace;
            case GRAZE: return 4.4f * Math.max(1, Math.round(rng.r(5f, 12f) / pace));
            case SCRATCH: return 1.2f + 1.7f * (pace > 1.5f ? 2 : 3 + rng.n(2));
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
        goThenDo(tx, ty, a, v, hour);
    }

    private void goThenDo(float tx, float ty, int a, int v, float hour) {
        float dx = tx - px, dy = ty - py, dist = (float) Math.sqrt(dx * dx + dy * dy);
        if (faceNow != 1f && faceNow != -1f) faceNow = faceNow >= 0f ? 1f : -1f;
        if (dist < 5f) {
            if (a == NIBBLE) face = 1f;
            begin(a, v, durationOf(a, v, hour));
            return;
        }
        wx0 = px; wy0 = py; wx1 = tx; wy1 = ty; nextAct = a; nextVariant = v; walkLen = dist;
        if (Math.abs(dx) > 3f) face = dx > 0 ? 1f : -1f;
        begin(WALK, 0, Math.max(0.9f, dist / WALK_SPEED));
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
        goThenDo(Hero.SPOT_X, Hero.SPOT_Y, REST, 0, Palette.DAY);
    }

    // ------------------------------------------------------------------ posing

    private void place() {
        pose.x = px; pose.y = py; pose.scale = Hero.depthScale(py); pose.facing = faceNow;
    }

    private void computePose() {
        sprig = 0f;
        if (turnT >= 0f) {
            turnPoseAt(turnT);
        } else {
            activityPose();
            if (blendT < blendDur) pose.blendFrom(from, Art.smooth(blendT / blendDur));
            if (pokeT >= 0f) {
                if (pokeAsleep) MochiAnim.pokeAsleep(pose, pokeT);
                else MochiAnim.pokeAwake(pose, pokeT, act == WALK || act == AFTERGLOW);
            }
        }
        // close to the window the cottage's candle-light falls on Mochi
        pose.warm = Art.smooth(pose.scale, 1.05f, 1.45f);
    }

    private void activityPose() {
        switch (act) {
            case SLEEP: MochiAnim.sleep(pose, t, dur, zFrom); place(); break;
            case REST: MochiAnim.rest(pose, t); place(); break;
            case SCRATCH: MochiAnim.scratch(pose, t, dur); place(); break;
            case GRAZE: MochiAnim.graze(pose, t); place(); break;
            case NIBBLE: sprig = MochiAnim.nibble(pose, t); place(); break;
            case PLAY:
                if (variant == PLAY_ROLL) MochiAnim.playRoll(pose, t, dur);
                else if (variant == PLAY_POPCORN) MochiAnim.playPopcorn(pose, t, 0.6f);
                else MochiAnim.playBalance(pose, t, dur);
                place();
                break;
            case WALK: {
                // mostly steady with a gentle start and stop; the stride follows the ground covered
                float u = Art.clamp01(t / dur), k = 0.55f * u + 0.45f * Art.smooth(u);
                px = Art.lerp(wx0, wx1, k); py = Art.lerp(wy0, wy1, k);
                float scale = Hero.depthScale(py);
                MochiAnim.walk(pose, Art.TAU * k * walkLen / (MochiAnim.WALK_STRIDE * scale), t, 0f);
                place();
                break;
            }
            case EXCITED: excitedPose(); break;
            default: glowPose(); break;
        }
    }

    /**
     * A turn on the spot: Mochi comes up to a square stand (side view, old direction), swings
     * through the front view, and settles facing the new way. No paper-thin frames.
     */
    private void turnPoseAt(float u) {
        if (u < TURN_A) {
            MochiAnim.standIdle(pose, u);
            place();
            pose.facing = turnFrom * Art.lerp(1f, 0.74f, Art.smooth(u, TURN_A - 0.1f, TURN_A));
            pose.blendFrom(from, Art.smooth(u / (TURN_A - 0.06f)));
        } else if (u < TURN_A + TURN_B) {
            float k = (u - TURN_A) / TURN_B;
            MochiAnim.standFront(pose, u);
            place();
            pose.facing = 0.8f + 0.2f * Art.hump(k);
            pose.lift = 2.5f * Art.hump(k);
        } else {
            float k = Art.clamp01((u - TURN_A - TURN_B) / TURN_C);
            MochiAnim.standIdle(pose, u);
            place();
            pose.facing = face * Art.lerp(0.74f, 1f, Art.smooth(k));
        }
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

    /**
     * Puts Mochi on the track at angle theta, easing in from (sx,sy) (k 0..1). Along the near and
     * far sides Mochi is seen side-on (the pose already in {@link #pose}); round each end the
     * view swings to head-on or tail-on, so the turn is never a thin sliver. {@code pep} picks
     * the gait for those end stretches (1 = alarm gallop).
     */
    private void onTrack(float theta, float sx, float sy, float k, float pep) {
        float ex = Hero.RING_X + Hero.RING_RX * Art.sin(theta), ey = Hero.RING_Y - Hero.RING_RY * Art.cos(theta);
        float b = Art.smooth(k);
        px = Art.lerp(sx, ex, b); py = Art.lerp(sy, ey, b);
        float c = Art.cos(theta), ac = Math.abs(c);
        face = c >= 0f ? 1f : -1f;
        if (ac < END_ZONE && b >= 1f) {
            MochiAnim.runEnd(pose, t, pep, Art.sin(theta) < 0f);
            faceNow = face;
            pose.facing = Art.lerp(1f, 0.8f, ac / END_ZONE);
        } else {
            faceNow = face;
            pose.facing = face * Art.lerp(0.72f, 1f, Art.smooth(ac, END_ZONE, 0.62f));
        }
        pose.x = px; pose.y = py; pose.scale = Hero.depthScale(py);
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
        pose.facing = Art.lerp(0.8f, 1f, Art.smooth(k, 0f, 0.25f));
        pose.overSill = k > 0.85f;
        px = pose.x; py = Math.min(pose.y, 440f);
    }

    private void excitedPose() {
        float sweep = sweepFrom(exX, exY), run = sweep / Art.TAU * LAP;
        float startle = exStartle ? STARTLE : 0f, join = JOIN;
        float first = startle + run + DASH + AT_GLASS + DROP;
        float u = t, sx = exX, sy = exY;
        if (u >= first) {
            sweep = sweepFrom(REENTER_X, REENTER_Y); run = sweep / Art.TAU * LAP;
            u = (u - first) % (run + DASH + AT_GLASS + DROP);
            sx = REENTER_X; sy = REENTER_Y; startle = 0f; join = REJOIN;
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
            onTrack((float) Math.PI - sweep + sweep * (u / run), sx, sy, u / join, 1f);
            // swing towards the window at the very end, ready for the dash
            if (pose.view == MochiPose.SIDE) pose.facing *= 1f - 0.2f * Art.smooth(u, run - 0.1f, run);
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
            case G_TROT: {
                float pep = 0.25f + 0.75f * energy;
                MochiAnim.walk(pose, MochiAnim.trotPhase(t, pep), t, pep);
                onTrack(segTheta + segSweep * Art.clamp01(segT / segDur), segX, segY, segT / 1.0f, pep);
                break;
            }
            case G_HOP: MochiAnim.playPopcorn(pose, segT, energy); place(); pose.facing = face; break;
            case G_SIT:
                MochiAnim.sitLook(pose, segT, energy); place();
                pose.facing = Art.lerp(0.8f, 1f, Art.smooth(segT, 0f, 0.2f));
                break;
            default: dashPose(segT, segDur); break;
        }
    }
}
