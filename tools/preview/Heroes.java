import java.awt.image.BufferedImage;
import com.benjaminosleo.locumwatch.art.*;

/** Hero panel renders and draw-call statistics. */
public class Heroes {
    /** Renders one complete hero frame into the pen. Returns {scene static, frame static, live} draw calls. */
    static int[] frame(AwtPen p, HeroPanel hero, MochiDirector d, double t) {
        int a = p.drawCalls;
        hero.drawSceneStatic(p);
        int b = p.drawCalls;
        hero.drawSceneLive(p, t, d);
        int c = p.drawCalls;
        hero.drawFrameStatic(p);
        int e = p.drawCalls;
        hero.drawFrameLive(p, t, d);
        int f = p.drawCalls;
        return new int[]{b - a, e - c, (c - b) + (f - e)};
    }

    static void still(String name, float hour, boolean armed, int activity, int variant, float phase, double t) throws Exception {
        BufferedImage img = Render.image(Hero.W, Hero.H);
        AwtPen p = Render.pen(img);
        HeroPanel hero = new HeroPanel();
        hero.setHour(hour); hero.setArmed(armed);
        MochiDirector d = new MochiDirector(3);
        d.force(activity, variant, phase);
        int[] n = frame(p, hero, d, t);
        System.out.println(name + ": scene static " + n[0] + ", frame static " + n[1] + ", live " + n[2] + " draw calls");
        Render.write(img, name);
    }

    static void stills() throws Exception {
        still("hero_dawn.png", Palette.DAWN, true, MochiDirector.SLEEP, 0, 0.7f, 3.0);
        still("hero_day.png", Palette.DAY, true, MochiDirector.GRAZE, 0, 0.62f, 7.0);
        still("hero_golden.png", Palette.GOLDEN, true, MochiDirector.REST, 0, 0.3f, 11.0);
        still("hero_dusk.png", Palette.DUSK, true, MochiDirector.NIBBLE, 0, 0.12f, 17.0);
        still("hero_night.png", Palette.NIGHT, true, MochiDirector.SLEEP, 0, 0.9f, 23.0);
        still("hero_day_off.png", Palette.DAY, false, MochiDirector.GRAZE, 0, 0.62f, 7.0);
    }
}
