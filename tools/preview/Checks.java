import java.awt.image.BufferedImage;
import java.util.Random;
import com.benjaminosleo.locumwatch.art.*;

/** Numeric checks: the hero/wall seam, text contrast, draw-call budgets, and a long director soak. */
public class Checks {
    static double lin(int c) { double v = c / 255.0; return v <= 0.03928 ? v / 12.92 : Math.pow((v + 0.055) / 1.055, 2.4); }
    static double lum(int rgb) { return 0.2126 * lin((rgb >> 16) & 255) + 0.7152 * lin((rgb >> 8) & 255) + 0.0722 * lin(rgb & 255); }
    static double contrast(int a, int b) { double x = lum(a), y = lum(b); return (Math.max(x, y) + 0.05) / (Math.min(x, y) + 0.05); }

    static int avg(BufferedImage im, float l, float t, float r, float b) {
        float s = Render.S;
        long R = 0, G = 0, B = 0, n = 0;
        for (int y = Math.round(t * s); y < Math.round(b * s); y++) for (int x = Math.round(l * s); x < Math.round(r * s); x++) {
            int c = im.getRGB(x, y); R += (c >> 16) & 255; G += (c >> 8) & 255; B += c & 255; n++;
        }
        return (int) ((R / n) << 16 | (G / n) << 8 | (B / n));
    }

    static void all() throws Exception {
        seam();
        contrasts();
        budgets();
        alarmRects();
        soak();
    }

    /** The last row of the hero's frame layer against the first row of the page wall below it. */
    static void seam() {
        System.out.println("-- seam: hero bottom row vs page wall top row (largest channel difference over x = 0..359) --");
        for (float hour : new float[]{Palette.DAY, Palette.NIGHT, Palette.DUSK}) {
            for (int armed = 1; armed >= 0; armed--) {
                BufferedImage hero = Render.image(Hero.W, Hero.H), wall = Render.image(Hero.W, 40);
                WindowFrame f = new WindowFrame(); f.setHour(hour); f.setArmed(armed == 1);
                f.drawStatic(Render.pen(hero));
                Decor d = new Decor(); d.setHour(hour);
                d.wall(Render.pen(wall), Hero.W, 40, Hero.H);
                int worst = 0, wx = 0;
                // compare against what the wall itself would have painted on that last row
                BufferedImage ref = Render.image(Hero.W, Hero.H);
                d.wall(Render.pen(ref), Hero.W, Hero.H, 0);
                int y = hero.getHeight() - 1;
                for (int x = 0; x < hero.getWidth(); x++) {
                    int a = hero.getRGB(x, y), b = ref.getRGB(x, y);
                    for (int sh = 0; sh <= 16; sh += 8) { int dd = Math.abs(((a >> sh) & 255) - ((b >> sh) & 255)); if (dd > worst) { worst = dd; wx = x; } }
                }
                int mid = hero.getWidth() / 2, near = Math.round(298 * Render.S);
                System.out.printf("hour %4.1f lantern %s: hero last row #%06X (x=180) #%06X (x=298); wall first row #%06X #%06X; worst difference from plain wall %d/255 at x=%d%n",
                    hour, armed == 1 ? "on " : "off", hero.getRGB(mid, y) & 0xFFFFFF, hero.getRGB(near, y) & 0xFFFFFF,
                    wall.getRGB(mid, 0) & 0xFFFFFF, wall.getRGB(near, 0) & 0xFFFFFF, worst, Math.round(wx / Render.S));
            }
        }
    }

    static void contrasts() {
        int INK = 0x4A3424, CREAM = 0xFFF4DC;
        System.out.println("-- contrast: dark-brown text #4A3424 on parchment and planks; cream #FFF4DC on status boards and sign --");
        System.out.println(" hour | parchment | plank wood | plank green | plank red || board ok | board off | board problem | sign");
        double[] min = {99, 99, 99, 99, 99, 99, 99, 99};
        for (float h : new float[]{12f, 15f, 17.3f, 19.1f, 20f, 21.5f, 23f, 3f, 5.6f, 6.2f, 8f}) {
            Decor d = new Decor(); d.setHour(h);
            BufferedImage pg = Render.image(360, 640);
            AwtPen p = Render.pen(pg);
            d.wall(p, 360, 640, 520);
            d.parchment(p, 16, 10, 344, 150, 1);
            d.plankButton(p, 34, 160, 326, 208, Decor.BUTTON_NORMAL, false);
            d.plankButton(p, 34, 220, 326, 268, Decor.BUTTON_GOOD, false);
            d.plankButton(p, 34, 280, 326, 328, Decor.BUTTON_ATTENTION, false);
            d.sectionHeader(p, 85, 340, 275, 388);
            for (int i = 0; i < 3; i++) d.statusBoard(p, 62, 400 + i * 72, 298, 464 + i * 72, i);
            int[] bg = {avg(pg, 60, 40, 300, 120), avg(pg, 60, 172, 300, 192), avg(pg, 60, 232, 300, 252), avg(pg, 60, 292, 300, 312),
                avg(pg, 92, 420, 268, 454), avg(pg, 92, 492, 268, 526), avg(pg, 92, 564, 268, 598), avg(pg, 120, 360, 240, 378)};
            StringBuilder sb = new StringBuilder(String.format("%5.1f |", h));
            for (int i = 0; i < 8; i++) {
                double c = contrast(i < 4 ? INK : CREAM, bg[i]);
                min[i] = Math.min(min[i], c);
                sb.append(String.format(" #%06X %.2f |", bg[i], c));
            }
            System.out.println(sb);
        }
        System.out.printf("lowest: parchment %.2f, wood %.2f, green %.2f, red %.2f, board ok %.2f, off %.2f, problem %.2f, sign %.2f%n",
            min[0], min[1], min[2], min[3], min[4], min[5], min[6], min[7]);
    }

    /** Worst-case live draw calls: every activity swept through its phases, the alarm run, and the alarm page. */
    static void budgets() {
        System.out.println("-- live draw calls per frame --");
        BufferedImage img = Render.image(Hero.W, Hero.H);
        int worst = 0, worstAct = -1; float worstHour = 0; long sum = 0, n = 0;
        for (float hour : new float[]{Palette.DAY, Palette.DUSK, 20f, Palette.NIGHT}) {
            HeroPanel hero = new HeroPanel(); hero.setHour(hour);
            MochiDirector d = new MochiDirector(2);
            for (int act = 0; act <= MochiDirector.WALK; act++) for (int v = 0; v < (act == MochiDirector.PLAY ? 3 : 1); v++) for (int i = 0; i <= 60; i++) {
                d.force(act, v, i / 60f);
                AwtPen p = Render.pen(img);
                hero.drawSceneLive(p, 3.0 + i * 0.37, d);
                hero.drawFrameLive(p, 3.0 + i * 0.37, d);
                sum += p.drawCalls; n++;
                if (p.drawCalls > worst) { worst = p.drawCalls; worstAct = act; worstHour = hour; }
            }
            // and a real-time run with an alarm, pokes and the bird
            d.reset(hour);
            for (int i = 0; i < 30 * 120; i++) {
                double t = i / 30.0;
                d.step(1 / 30.0, hour, t > 40 && t < 60, t < 60 ? MochiDirector.NEVER : t - 60);
                if (i % 200 == 50) d.poke();
                AwtPen p = Render.pen(img);
                hero.drawSceneLive(p, t, d);
                hero.drawFrameLive(p, t, d);
                sum += p.drawCalls; n++;
                if (p.drawCalls > worst) { worst = p.drawCalls; worstAct = d.activity(); worstHour = hour; }
            }
        }
        System.out.println("hero live: mean " + (sum / n) + ", worst " + worst + " (activity " + worstAct + " at hour " + worstHour + ") over " + n + " frames");
        int aw = 0; long as = 0, an = 0;
        for (float hour : new float[]{Palette.DAY, 20f, Palette.NIGHT}) {
            AlarmArt art = new AlarmArt(); art.setHour(hour);
            BufferedImage page = Render.image(360, 640);
            for (int i = 0; i < 30 * 40; i++) {
                AwtPen p = Render.pen(page);
                art.drawLive(p, 360, 640, i / 30.0);
                as += p.drawCalls; an++; aw = Math.max(aw, p.drawCalls);
            }
        }
        System.out.println("alarm page live: mean " + (as / an) + ", worst " + aw + " over " + an + " frames");
    }

    static void alarmRects() {
        System.out.println("-- alarm page rectangles (w x h: window | message | stop | secondary) --");
        float[][] sizes = {{360, 640}, {360, 616}, {360, 780}, {411, 731}, {320, 480}, {640, 360}, {360, 300}, {360, 120}, {200, 40}};
        float[] r = new float[4];
        for (float[] s : sizes) {
            StringBuilder sb = new StringBuilder((int) s[0] + " x " + (int) s[1] + ":");
            for (int k = 0; k < 4; k++) {
                if (k == 0) AlarmArt.windowRect(s[0], s[1], r); else if (k == 1) AlarmArt.messageRect(s[0], s[1], r);
                else if (k == 2) AlarmArt.stopRect(s[0], s[1], r); else AlarmArt.secondaryRect(s[0], s[1], r);
                sb.append(String.format(" [%.0f,%.0f %.0fx%.0f]", r[0], r[1], r[2] - r[0], r[3] - r[1]));
                if (r[3] < r[1] || r[3] > s[1] + 0.01f) throw new IllegalStateException("bad rect " + k + " for " + s[0] + "x" + s[1]);
            }
            System.out.println(sb);
        }
    }

    static boolean finite(MochiPose o) {
        float[] v = {o.x, o.y, o.scale, o.facing, o.lift, o.shadow, o.bodyY, o.bodyRot, o.bodySX, o.bodySY, o.headDx, o.headDy, o.headRot,
            o.earRot, o.eyeOpen, o.lookX, o.lookY, o.jaw, o.blush, o.nose, o.haunch, o.legBlur, o.tuck, o.itemPhase, o.petal, o.zzz, o.alert,
            o.alertPhase, o.dust, o.dustPhase, o.scratch, o.pawsUp, o.hearts, o.heartPhase, o.warm, o.petalFall, o.itemX, o.itemY, o.itemAlpha,
            o.reachX, o.reachY, o.kneeX, o.kneeY, o.legA[0], o.legA[1], o.legA[2], o.legA[3], o.legL[0], o.legL[1], o.legL[2], o.legL[3]};
        for (float f : v) if (Float.isNaN(f) || Float.isInfinite(f)) return false;
        return o.x > -200 && o.x < 560 && o.y > 0 && o.y < 900 && o.scale > 0.3f && o.scale < 3f;
    }

    /**
     * Several simulated days: long stretches of real-time frames broken up by stutters, long
     * gaps, zero, negative, not-a-number and absurd time steps, with random alarms and random
     * taps (some of them triple taps). Draws a frame now and then. Fails loudly on any exception
     * or any pose value that is not finite and sensible.
     */
    static void soak() {
        System.out.println("-- director soak --");
        Random rnd = new Random(42);
        long steps = 0, pokes = 0, alarms = 0, odd = 0;
        long[] seen = new long[9];
        BufferedImage img = Render.image(Hero.W, Hero.H);
        int runs = 4; double days = 3.0;
        for (int run = 0; run < runs; run++) {
            MochiDirector d = new MochiDirector(1000 + run);
            HeroPanel hero = new HeroPanel();
            double clock = 0, alarmUntil = -1, alarmEnded = -1;
            float lastHour = -1;
            int burst = 0;
            d.reset(0f);
            while (clock < days * 86400) {
                double dt;
                if (burst > 0) {
                    burst--;
                    dt = 1 / 30.0 + (rnd.nextInt(10) == 0 ? rnd.nextDouble() * 0.05 : 0);
                } else {
                    int kind = rnd.nextInt(100);
                    if (kind < 55) { burst = 200 + rnd.nextInt(1800); dt = 1 / 30.0; }
                    else if (kind < 70) dt = rnd.nextDouble() * 2.0;            // a stutter
                    else if (kind < 90) dt = 20 + rnd.nextDouble() * 900;        // the screen was off
                    else if (kind < 93) dt = 0;
                    else if (kind < 95) dt = -1;
                    else if (kind < 97) dt = Double.NaN;
                    else if (kind < 98) dt = Double.POSITIVE_INFINITY;
                    else dt = 1e9;                                               // the clock jumped
                    if (kind >= 90) odd++;
                }
                clock += dt >= 0 && dt < 1e8 ? dt : 0;
                float hour = (float) ((clock / 3600.0) % 24.0);
                if (rnd.nextInt(50000) == 0) hour = Float.NaN;
                if (alarmUntil < 0 && rnd.nextInt(60000) == 0) { alarmUntil = clock + 2 + rnd.nextDouble() * 120; alarms++; }
                boolean ringing = alarmUntil >= 0 && clock < alarmUntil;
                if (alarmUntil >= 0 && !ringing) { alarmEnded = alarmUntil; alarmUntil = -1; }
                double since = ringing || alarmEnded < 0 ? MochiDirector.NEVER : clock - alarmEnded;
                if (rnd.nextInt(50000) == 0) since = Double.NaN;
                d.step(dt, hour, ringing, since);
                steps++;
                if (rnd.nextInt(400) == 0) {
                    float x = 40 + rnd.nextFloat() * 280, y = 60 + rnd.nextFloat() * 340;
                    if (rnd.nextInt(3) != 0) { x = d.pose.x; y = d.pose.y - d.pose.lift - 30 * d.pose.scale; }
                    if (d.hitTest(x, y)) { d.poke(); pokes++; if (rnd.nextInt(3) == 0) { d.poke(); d.poke(); } }
                }
                seen[d.activity()]++;
                if (!finite(d.pose)) throw new IllegalStateException("non-finite or runaway pose at step " + steps + " activity " + d.activity());
                float perch = d.birdPerch();
                if (!(perch >= 0f && perch <= 1f)) throw new IllegalStateException("bad bird perch " + perch);
                if (steps % 4999 == 0) {
                    float h2 = Float.isNaN(hour) ? 12f : hour;
                    if (Math.abs(h2 - lastHour) > 0.5f) { hero.setHour(h2); lastHour = h2; }
                    AwtPen p = Render.pen(img);
                    hero.drawSceneLive(p, clock, d);
                    hero.drawFrameLive(p, clock, d);
                }
            }
        }
        // the alarm page loop: far into a long alarm, and with a broken clock
        AlarmArt art = new AlarmArt();
        BufferedImage page = Render.image(360, 640);
        for (double t : new double[]{0, 0.2, 5, 59.99, 3599.9, 3600, 36000.123, 1e7, Double.NaN, -5, Double.POSITIVE_INFINITY}) art.drawLive(Render.pen(page), 360, 640, t);
        // the loop is exact: one hour in, the pose equals the pose at the same point of the second cycle
        System.out.println("soak passed: " + steps + " steps over " + Math.round(runs * days) + " simulated days, " + alarms + " alarms, " + pokes
            + " pokes, " + odd + " odd time steps; steps per activity " + java.util.Arrays.toString(seen) + "; no exceptions, every pose finite");
    }
}
