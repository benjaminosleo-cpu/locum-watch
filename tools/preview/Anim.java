import java.awt.image.BufferedImage;
import java.io.File;
import java.util.ArrayList;
import java.util.List;
import com.benjaminosleo.locumwatch.art.*;

/** Alarm strip, animated GIFs and live-layer draw-call statistics, all driven by the director. */
public class Anim {
    /** Eight frames across the EXCITED sequence, each inside the full hero. */
    static void alarmStrip() throws Exception {
        float sc = 0.5f;
        BufferedImage img = Render.image(Hero.W * sc * 8, Hero.H * sc);
        AwtPen p = Render.pen(img);
        HeroPanel hero = new HeroPanel();
        BufferedImage scene = Render.image(Hero.W, Hero.H), frame = Render.image(Hero.W, Hero.H);
        hero.drawSceneStatic(Render.pen(scene));
        hero.drawFrameStatic(Render.pen(frame));
        MochiDirector d = new MochiDirector(3);
        float[] ph = {0.015f, 0.1f, 0.22f, 0.36f, 0.52f, 0.585f, 0.7f, 0.9f};
        for (int i = 0; i < 8; i++) {
            d.force(MochiDirector.EXCITED, ph[i]);
            p.save();
            p.translate(i * Hero.W * sc, 0);
            p.scale(sc, sc, 0, 0);
            p.graphics().drawImage(scene, 0, 0, (int) Hero.W, (int) Hero.H, null);
            hero.drawSceneLive(p, 2.0 + ph[i] * 10.0, d);
            p.graphics().drawImage(frame, 0, 0, (int) Hero.W, (int) Hero.H, null);
            hero.drawFrameLive(p, 2.0 + ph[i] * 10.0, d);
            p.restore();
        }
        Render.write(img, "alarm_strip.png");
    }

    interface Script { boolean ringing(double t); double sinceEnd(double t); }

    /** Simulates the director in real time and writes a GIF (and a strip of sample frames for eyeballing). */
    static void gif(String name, float hour, double seconds, float pace, Script script, int warmup) throws Exception {
        int fps = 12, w = 360, h = 520;
        HeroPanel hero = new HeroPanel();
        hero.setHour(hour);
        BufferedImage scene = new BufferedImage(w, h, BufferedImage.TYPE_INT_ARGB), frame = new BufferedImage(w, h, BufferedImage.TYPE_INT_ARGB);
        hero.drawSceneStatic(new AwtPen(scene));
        hero.drawFrameStatic(new AwtPen(frame));
        MochiDirector d = new MochiDirector(5);
        d.setPace(pace);
        d.reset(hour);
        for (int i = 0; i < warmup; i++) d.step(1.0 / fps, hour, false, MochiDirector.NEVER);
        List<BufferedImage> frames = new ArrayList<>();
        int n = (int) (seconds * fps), min = Integer.MAX_VALUE, max = 0; long sum = 0; float jump = 0;
        for (int i = 0; i < n; i++) {
            double t = i / (double) fps;
            float ox = d.pose.x, oy = d.pose.y; int ov = d.pose.view;
            d.step(1.0 / fps, hour, script.ringing(t), script.sinceEnd(t));
            // biggest on-screen move between frames while Mochi is in view (a large value would be a visible jump)
            if (i > 0 && ov == d.pose.view && oy < 400 && d.pose.y < 400) jump = Math.max(jump, (float) Math.hypot(d.pose.x - ox, d.pose.y - oy));
            BufferedImage img = new BufferedImage(w, h, BufferedImage.TYPE_INT_RGB);
            AwtPen p = new AwtPen(img);
            p.graphics().drawImage(scene, 0, 0, null);
            int a = p.drawCalls;
            hero.drawSceneLive(p, t, d);
            int b = p.drawCalls;
            p.graphics().drawImage(frame, 0, 0, null);
            hero.drawFrameLive(p, t, d);
            int live = (b - a) + (p.drawCalls - b);
            min = Math.min(min, live); max = Math.max(max, live); sum += live;
            frames.add(img);
        }
        System.out.println(name + ": live draw calls per frame min " + min + ", mean " + (sum / n) + ", max " + max + "; largest step between frames " + Math.round(jump) + " units");
        GifOut.write(frames, 1000 / fps, new File(Render.OUT, name + ".gif"));
        // sample strip: 10 evenly spaced frames, half size
        BufferedImage strip = new BufferedImage(w / 2 * 10, h / 2, BufferedImage.TYPE_INT_RGB);
        for (int i = 0; i < 10; i++) strip.getGraphics().drawImage(frames.get(i * (n - 1) / 9), i * w / 2, 0, w / 2, h / 2, null);
        new File(Render.OUT, "frames").mkdirs();
        javax.imageio.ImageIO.write(strip, "png", new File(Render.OUT, "frames/" + name + "_samples.png"));
        System.out.println("wrote " + name + ".gif (" + n + " frames)");
    }

    /**
     * A film strip: the director runs in real time at 30 fps and a frame is captured every
     * {@code dt} seconds from {@code t0}, cropped to (cl,ct,cr,cb) hero units.
     */
    static void film(String name, float hour, long seed, float pace, Script script, double t0, double dt, int n, int cols,
                     float cl, float ct, float cr, float cb, float sc) throws Exception {
        HeroPanel hero = new HeroPanel();
        hero.setHour(hour);
        int W = Math.round(Hero.W * sc), H = Math.round(Hero.H * sc);
        BufferedImage scene = new BufferedImage(W, H, BufferedImage.TYPE_INT_ARGB), frame = new BufferedImage(W, H, BufferedImage.TYPE_INT_ARGB);
        AwtPen sp = new AwtPen(scene); sp.scale(sc, sc, 0, 0); hero.drawSceneStatic(sp);
        AwtPen fp = new AwtPen(frame); fp.scale(sc, sc, 0, 0); hero.drawFrameStatic(fp);
        MochiDirector d = new MochiDirector(seed);
        d.setPace(pace);
        d.reset(hour);
        int cw = Math.round((cr - cl) * sc), ch = Math.round((cb - ct) * sc), rows = (n + cols - 1) / cols;
        BufferedImage out = new BufferedImage(cw * cols, ch * rows, BufferedImage.TYPE_INT_RGB);
        double t = 0; int cap = 0, prev = d.activity();
        while (cap < n) {
            d.step(1 / 30.0, hour, script.ringing(t), script.sinceEnd(t));
            t += 1 / 30.0;
            if (t + 1e-6 >= t0 + cap * dt) {
                BufferedImage img = new BufferedImage(W, H, BufferedImage.TYPE_INT_ARGB);
                AwtPen p = new AwtPen(img);
                p.graphics().drawImage(scene, 0, 0, null);
                p.scale(sc, sc, 0, 0);
                hero.drawSceneLive(p, t, d);
                p.graphics().drawImage(frame, 0, 0, (int) Hero.W, (int) Hero.H, null);
                hero.drawFrameLive(p, t, d);
                out.getGraphics().drawImage(img.getSubimage(Math.round(cl * sc), Math.round(ct * sc), cw, ch), (cap % cols) * cw, (cap / cols) * ch, null);
                cap++;
            }
        }
        new File(Render.OUT, "frames").mkdirs();
        javax.imageio.ImageIO.write(out, "png", new File(Render.OUT, "frames/" + name + ".png"));
        System.out.println("wrote frames/" + name + ".png");
    }

    /** When (seconds, at 30 fps) the director first changes from one activity to another, or -1. */
    static double firstChange(float hour, long seed, float pace, int fromAct, int toAct) {
        MochiDirector d = new MochiDirector(seed);
        d.setPace(pace);
        d.reset(hour);
        int prev = d.activity();
        for (int i = 0; i < 30 * 60 * 30; i++) {
            d.step(1 / 30.0, hour, false, MochiDirector.NEVER);
            if (d.activity() != prev) {
                if (prev == fromAct && d.activity() == toAct) return i / 30.0;
                prev = d.activity();
            }
        }
        return -1;
    }

    /** Film strips of the moments that are easy to get wrong: turns, lying down, waking, the alarm run. */
    static void films() throws Exception {
        Script idle = new Script() {
            public boolean ringing(double t) { return false; }
            public double sinceEnd(double t) { return MochiDirector.NEVER; }
        };
        int[][] pairs = {{MochiDirector.SLEEP, MochiDirector.WALK}, {MochiDirector.WALK, MochiDirector.SLEEP},
            {MochiDirector.GRAZE, MochiDirector.WALK}, {MochiDirector.NIBBLE, MochiDirector.WALK}, {MochiDirector.WALK, MochiDirector.PLAY}};
        String[] names = {"film_wake_and_walk", "film_lie_down_to_sleep", "film_graze_turn_walk", "film_nibble_turn_walk", "film_walk_to_play"};
        for (int i = 0; i < pairs.length; i++) {
            double at = -1; long seed = 21;
            for (; seed < 40 && at < 0; seed++) at = firstChange(Palette.DAY, seed, 4f, pairs[i][0], pairs[i][1]);
            if (at < 0) { System.out.println("no " + names[i] + " found"); continue; }
            boolean longer = i < 2;
            film(names[i], Palette.DAY, seed - 1, 4f, idle, at - (i == 0 ? 2.2 : 0.3), longer ? 0.2 : 0.1, 24, 12, 60, 190, 300, 340, 0.8f);
        }
        Script alarm = new Script() {
            public boolean ringing(double t) { return t >= 1.0 && t < 14.0; }
            public double sinceEnd(double t) { return t < 14.0 ? MochiDirector.NEVER : t - 14.0; }
        };
        film("film_alarm_onset", Palette.DAY, 5, 1f, alarm, 0.9, 0.1, 48, 12, 40, 190, 320, 400, 0.7f);
        // the alarm page's little window through its first cycle and the start of the loop
        float[] win = new float[4];
        AlarmArt.windowRect(360, 640, win);
        float sc = 1.2f; int cw = Math.round(270 * sc), ch = Math.round(200 * sc);
        for (int night = 0; night < 2; night++) {
            AlarmArt art = new AlarmArt();
            art.setHour(night == 0 ? Palette.DAY : 3f);
            BufferedImage bg = new BufferedImage(Math.round(360 * sc), Math.round(640 * sc), BufferedImage.TYPE_INT_ARGB);
            AwtPen bp = new AwtPen(bg); bp.scale(sc, sc, 0, 0);
            art.drawStatic(bp, 360, 640);
            BufferedImage out = new BufferedImage(cw * 8, ch * 3, BufferedImage.TYPE_INT_RGB);
            for (int i = 0; i < 24; i++) {
                double t = i < 16 ? i * 0.3 : 9.2 + (i - 16) * 0.55;
                BufferedImage img = new BufferedImage(bg.getWidth(), bg.getHeight(), BufferedImage.TYPE_INT_ARGB);
                AwtPen p = new AwtPen(img);
                p.graphics().drawImage(bg, 0, 0, null);
                p.scale(sc, sc, 0, 0);
                art.drawLive(p, 360, 640, t);
                out.getGraphics().drawImage(img.getSubimage(Math.round(45 * sc), Math.round(20 * sc), cw, ch), (i % 8) * cw, (i / 8) * ch, null);
            }
            javax.imageio.ImageIO.write(out, "png", new File(Render.OUT, "frames/film_alarm_page_" + (night == 0 ? "day" : "night") + ".png"));
            System.out.println("wrote frames/film_alarm_page_" + (night == 0 ? "day" : "night") + ".png");
        }
        film("film_alarm_reenter", Palette.DAY, 5, 1f, alarm, 10.3, 0.1, 24, 12, 40, 190, 320, 400, 0.7f);
    }

    static void gifs() throws Exception {
        gif("anim_idle", Palette.DAY, 16, 9f, new Script() {
            public boolean ringing(double t) { return false; }
            public double sinceEnd(double t) { return MochiDirector.NEVER; }
        }, 0);
        gif("anim_alarm", Palette.DAY, 22, 3f, new Script() {
            public boolean ringing(double t) { return t >= 1.5 && t < 13.2; }
            // after the alarm, time is squeezed so the whole afterglow calms down within the clip
            public double sinceEnd(double t) { return t < 13.2 ? MochiDirector.NEVER : (t - 13.2) * 14.0; }
        }, 0);
        gif("anim_night", Palette.NIGHT, 10, 1f, new Script() {
            public boolean ringing(double t) { return false; }
            public double sinceEnd(double t) { return MochiDirector.NEVER; }
        }, 0);
    }
}
