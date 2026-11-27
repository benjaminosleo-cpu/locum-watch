package com.benjaminosleo.locumwatch.art;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import org.junit.Test;

import java.util.Random;

/**
 * The artwork is drawn in code, so it can be exercised without a phone: every layer is
 * drawn through a checking pen at every time of day, and must finish without an error,
 * without a nonsense number and with its saves and restores balanced.
 */
public class ArtTest {
    /** Most draw calls the moving layer may make in one frame (it is about 110 to 140 in practice). */
    private static final int LIVE_BUDGET = 180;

    private static void clean(CheckPen pen, String what) {
        assertTrue(what + ": " + pen.problems, pen.problems.isEmpty());
        assertEquals(what + ": saves and restores balanced", 0, pen.depth);
        assertEquals(what + ": restore underflow", 0, pen.underflow);
    }

    @Test
    public void windowSceneDrawsCleanlyAroundTheClock() {
        for (float hour = 0; hour < 24; hour += 0.75f) {
            for (int armed = 0; armed < 2; armed++) {
                HeroPanel panel = new HeroPanel();
                panel.setHour(hour);
                panel.setArmed(armed == 1);
                MochiDirector director = new MochiDirector(7);
                director.reset(hour);
                CheckPen pen = new CheckPen();
                panel.drawSceneStatic(pen);
                panel.drawFrameStatic(pen);
                for (int i = 0; i < 40; i++) {
                    director.step(0.25, hour, false, MochiDirector.NEVER);
                    panel.drawSceneLive(pen, i * 0.25, director);
                    panel.drawFrameLive(pen, i * 0.25, director);
                }
                clean(pen, "window at " + hour + (armed == 1 ? " armed" : " off"));
            }
        }
    }

    @Test
    public void mochiSurvivesALongDayOfEverything() {
        Random rnd = new Random(11);
        HeroPanel panel = new HeroPanel();
        MochiDirector director = new MochiDirector(3);
        director.reset(8f);
        CheckPen pen = new CheckPen();
        pen.trackBounds = false;
        double t = 0, sinceAlarm = MochiDirector.NEVER;
        boolean ringing = false;
        int worst = 0;
        boolean[] seen = new boolean[16];
        for (int frame = 0; frame < 220_000; frame++) {        // about two simulated hours at 30 a second
            double dt = 1 / 30.0;
            int dice = rnd.nextInt(20_000);
            if (dice == 0) dt = Double.NaN;                     // a clock hiccup must not poison her pose
            else if (dice == 1) dt = 900;                       // the app was in the background for a while
            else if (dice == 2) dt = -5;
            else if (dice < 6) director.poke();                 // tapped
            else if (dice < 9) director.hitTest(rnd.nextFloat() * Hero.W, rnd.nextFloat() * Hero.H);
            else if (dice == 9 && !ringing) ringing = true;     // an alarm starts...
            else if (dice < 400 && ringing) {                   // ...and is stopped a little later
                ringing = false;
                sinceAlarm = 0;
            }
            if (!ringing && sinceAlarm >= 0 && !Double.isNaN(dt) && dt > 0) sinceAlarm += dt;
            float hour = (float) ((8 + t / 300.0) % 24);        // the day races past: an hour every five minutes
            if (frame % 9000 == 0) panel.setHour(hour);
            director.step(dt, hour, ringing, ringing ? MochiDirector.NEVER : sinceAlarm);
            if (!Double.isNaN(dt) && dt > 0 && dt < 1) t += dt;
            MochiPose pose = director.pose;
            assertTrue("pose stays finite at frame " + frame,
                !Float.isNaN(pose.x) && !Float.isInfinite(pose.x) && !Float.isNaN(pose.y) && !Float.isInfinite(pose.y));
            int act = director.activity();
            if (act >= 0 && act < seen.length) seen[act] = true;
            if (frame % 7 == 0) {
                pen.resetCounts();
                panel.drawSceneLive(pen, t, director);
                panel.drawFrameLive(pen, t, director);
                if (pen.draws() > worst) worst = pen.draws();
                assertTrue("frame " + frame + ": " + pen.problems, pen.problems.isEmpty());
                assertEquals("frame " + frame + " balanced", 0, pen.depth);
            }
        }
        assertTrue("moving layer stays within " + LIVE_BUDGET + " draw calls a frame, worst was " + worst, worst <= LIVE_BUDGET);
        for (int act : new int[]{MochiDirector.SLEEP, MochiDirector.REST, MochiDirector.SCRATCH, MochiDirector.GRAZE,
                MochiDirector.NIBBLE, MochiDirector.PLAY, MochiDirector.EXCITED, MochiDirector.AFTERGLOW}) {
            assertTrue("activity " + act + " turned up during the day", seen[act]);
        }
    }

    @Test
    public void tappingMochiOnlyCountsOnMochi() {
        MochiDirector director = new MochiDirector(5);
        director.reset(12f);
        for (int i = 0; i < 60; i++) director.step(1 / 30.0, 12f, false, MochiDirector.NEVER);
        assertTrue("a tap on her is a hit", director.hitTest(director.pose.x, director.pose.y - 8));
        assertTrue("a tap in the far corner is not", !director.hitTest(2, 2));
        assertTrue("nonsense is not", !director.hitTest(Float.NaN, Float.NaN));
    }

    @Test
    public void cottageDecorDrawsCleanlyAtAnySize() {
        float[][] sizes = {{328, 64}, {236, 64}, {190, 48}, {64, 48}, {1, 1}, {0, 0}, {900, 30}, {40, 600}};
        for (float hour = 0; hour < 24; hour += 3) {
            Decor decor = new Decor();
            decor.setHour(hour);
            CheckPen pen = new CheckPen();
            decor.wall(pen, 360, Decor.WALL_PERIOD, 0);
            decor.wall(pen, 411, 700, Hero.H);
            for (float[] s : sizes) {
                float w = s[0], h = s[1];
                decor.parchment(pen, 0, 0, w, h, 3);
                for (int style : new int[]{Decor.BUTTON_NORMAL, Decor.BUTTON_GOOD, Decor.BUTTON_ATTENTION}) {
                    decor.plankButton(pen, 0, 0, w, h, style, false);
                    decor.plankButton(pen, 0, 0, w, h, style, true);
                }
                for (int style : new int[]{Decor.STATUS_OK, Decor.STATUS_OFF, Decor.STATUS_PROBLEM}) {
                    decor.statusBoard(pen, 0, 0, w, h, style);
                }
                decor.sectionHeader(pen, 0, 0, w, h);
                decor.sliderTrack(pen, 0, 0, w, h, 0f);
                decor.sliderTrack(pen, 0, 0, w, h, 0.5f);
                decor.sliderTrack(pen, 0, 0, w, h, 1f);
                decor.sliderThumb(pen, w / 2, h / 2, Math.min(w, h) / 2, false);
                decor.sliderThumb(pen, w / 2, h / 2, Math.min(w, h) / 2, true);
                decor.toggle(pen, 0, 0, w, h, true);
                decor.toggle(pen, 0, 0, w, h, false);
                decor.field(pen, 0, 0, w, h, true);
                decor.field(pen, 0, 0, w, h, false);
                for (int kind : new int[]{Decor.SHELF_APOTHECARY, Decor.SHELF_LIBRARY, Decor.SHELF_BREWING}) {
                    decor.shelf(pen, kind, 12, Decor.SHELF_ABOVE, w - 24);
                }
            }
            clean(pen, "decor at " + hour);
        }
        assertTrue("status board leaves room for its cords",
            Decor.STATUS_BOARD_TOP_FRACTION >= 0f && Decor.STATUS_BOARD_TOP_FRACTION < 0.5f);
    }

    @Test
    public void wallRepeatsExactlySoTheTiledBackgroundHasNoSeam() {
        // The page background is one bitmap of WALL_PERIOD repeated down the page: the wall drawn
        // at an offset of one whole period must make exactly the same calls as at offset zero.
        Decor decor = new Decor();
        decor.setHour(12f);
        CheckPen a = new CheckPen(), b = new CheckPen();
        decor.wall(a, 360, Decor.WALL_PERIOD, 0);
        decor.wall(b, 360, Decor.WALL_PERIOD, Decor.WALL_PERIOD);
        assertEquals("same number of strokes one period down", a.draws(), b.draws());
        assertEquals(a.bounds(), b.bounds());
    }

    @Test
    public void heroLayoutConstantsMakeSense() {
        assertTrue("lantern is inside the picture", Hero.LANTERN_L >= 0 && Hero.LANTERN_R <= Hero.W
            && Hero.LANTERN_T >= 0 && Hero.LANTERN_B <= Hero.H && Hero.LANTERN_R - Hero.LANTERN_L >= 44
            && Hero.LANTERN_B - Hero.LANTERN_T >= 44);
        assertTrue("status board is inside the picture, under the window", Hero.BOARD_L >= 0 && Hero.BOARD_R <= Hero.W
            && Hero.BOARD_T >= Hero.WIN_B && Hero.BOARD_B <= Hero.H);
        assertTrue("status board is big enough for two lines of text",
            Hero.BOARD_R - Hero.BOARD_L >= 200 && Hero.BOARD_B - Hero.BOARD_T >= 56);
    }

    @Test
    public void alarmPagePiecesAlwaysFitTheScreen() {
        float[] win = new float[4], msg = new float[4], stop = new float[4], sec = new float[4];
        for (float w = 280; w <= 1000; w += 24) {
            for (float h = 240; h <= 1400; h += 20) {
                String at = " on " + w + "x" + h;
                AlarmArt.windowRect(w, h, win);
                AlarmArt.messageRect(w, h, msg);
                AlarmArt.stopRect(w, h, stop);
                AlarmArt.secondaryRect(w, h, sec);
                for (float[] r : new float[][]{win, msg, stop, sec}) {
                    for (float v : r) assertTrue("finite" + at, !Float.isNaN(v) && !Float.isInfinite(v));
                    assertTrue("not inside-out" + at, r[2] >= r[0] && r[3] >= r[1]);
                    assertTrue("on screen" + at + ": " + java.util.Arrays.toString(r),
                        r[0] >= 0 && r[1] >= 0 && r[2] <= w + 0.01f && r[3] <= h + 0.01f);
                }
                assertTrue("STOP is a proper target" + at, stop[2] - stop[0] >= 200 && stop[3] - stop[1] >= 90);
                assertTrue("message sits above STOP" + at, msg[3] <= stop[1] + 0.01f);
                assertTrue("second button sits below STOP" + at, sec[1] >= stop[3] - 0.01f);
                if (h >= 560) {   // every real phone, held upright
                    assertTrue("message has room" + at, msg[3] - msg[1] >= 150);
                    assertTrue("second button is a proper target" + at, sec[3] - sec[1] >= 44);
                }
            }
        }
    }

    @Test
    public void alarmPageDrawsCleanlyAroundTheClock() {
        float[][] screens = {{360, 640}, {411, 731}, {320, 480}, {640, 360}, {800, 1280}};
        float[] r = new float[4];
        for (float hour = 1; hour < 24; hour += 4.5f) {
            AlarmArt art = new AlarmArt();
            art.setHour(hour);
            for (float[] s : screens) {
                CheckPen pen = new CheckPen();
                art.drawStatic(pen, s[0], s[1]);
                int worst = 0;
                for (double t = 0; t < 30; t += 1 / 15.0) {
                    pen.resetCounts();
                    art.drawLive(pen, s[0], s[1], t);
                    if (pen.draws() > worst) worst = pen.draws();
                }
                AlarmArt.stopRect(s[0], s[1], r);
                art.stopButton(pen, 0, 0, r[2] - r[0], r[3] - r[1], false);
                art.stopButton(pen, 0, 0, r[2] - r[0], r[3] - r[1], true);
                AlarmArt.secondaryRect(s[0], s[1], r);
                art.secondaryButton(pen, 0, 0, r[2] - r[0], r[3] - r[1], false);
                art.secondaryButton(pen, 0, 0, r[2] - r[0], r[3] - r[1], true);
                clean(pen, "alarm page " + s[0] + "x" + s[1] + " at " + hour);
                assertTrue("alarm page moving layer within budget, worst " + worst, worst <= LIVE_BUDGET);
            }
        }
    }
}
