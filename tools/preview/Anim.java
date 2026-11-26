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
