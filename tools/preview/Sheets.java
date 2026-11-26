import java.awt.image.BufferedImage;
import com.benjaminosleo.locumwatch.art.*;

/** Contact sheets of Mochi's activities. */
public class Sheets {
    /** Rows top to bottom: SLEEP, REST, SCRATCH, GRAZE, NIBBLE, PLAY (roll), PLAY (popcorn, balance), EXCITED, AFTERGLOW. */
    static void mochiSheet() throws Exception {
        int[][] rows = {
            {MochiDirector.SLEEP, 0}, {MochiDirector.REST, 0}, {MochiDirector.SCRATCH, 0}, {MochiDirector.GRAZE, 0},
            {MochiDirector.NIBBLE, 0}, {MochiDirector.PLAY, 0}, {MochiDirector.PLAY, 1}, {MochiDirector.EXCITED, 0},
            {MochiDirector.AFTERGLOW, 0}};
        float[][] phases = {
            {0.05f, 0.3f, 0.62f, 0.9f}, {0.1f, 0.35f, 0.6f, 0.9f}, {0.1f, 0.2f, 0.45f, 0.48f}, {0.1f, 0.3f, 0.6f, 0.8f},
            {0.1f, 0.3f, 0.6f, 0.85f}, {0.03f, 0.2f, 0.5f, 0.7f}, {0.2f, 0.42f, -0.5f, -0.75f}, {0.02f, 0.2f, 0.5f, 0.8f},
            {0.1f, 0.35f, 0.5f, 0.8f}};
        float cw = 180, ch = 150, sc = 1.25f;
        BufferedImage img = Render.image(cw * 4, ch * rows.length);
        AwtPen p = Render.pen(img);
        p.fillRect(0, 0, cw * 4, ch * rows.length, Ink.solid(0xFF9CCB6E));
        Mochi mochi = new Mochi();
        MochiDirector d = new MochiDirector(7);
        int max = 0;
        for (int r = 0; r < rows.length; r++) {
            p.fillRect(0, r * ch + ch - 28, cw * 4, r * ch + ch, Ink.solid(0xFF8DBE60));
            p.line(0, r * ch, cw * 4, r * ch, Ink.solid(0x30000000), 0.5f);
            for (int c = 0; c < 4; c++) {
                float ph = phases[r][c];
                int act = rows[r][0], var = rows[r][1];
                if (ph < 0) { var = 2; ph = -ph; }
                d.force(act, var, ph);
                MochiPose o = d.pose;
                p.save();
                p.clipRect(c * cw, r * ch, c * cw + cw, r * ch + ch);
                boolean big = o.scale > 1.3f;
                float k = big ? 0.72f : sc;
                // re-centre each cell on Mochi so every frame is drawn large
                float ox = c * cw + cw / 2f - (big ? Hero.RING_X : (o.x - (o.x - Hero.SPOT_X) * 0.25f)) * k;
                float oy = r * ch + ch - 22 - (big ? Hero.AT_WINDOW_Y - 22 : Hero.SPOT_Y) * k;
                p.translate(ox, oy);
                p.scale(k, k, 0, 0);
                int before = p.drawCalls;
                mochi.draw(p, o);
                mochi.drawOverSill(p, o);
                max = Math.max(max, p.drawCalls - before);
                p.restore();
            }
        }
        System.out.println("mochi worst-case draw calls on sheet: " + max);
        Render.write(img, "mochi_sheet.png");
    }
}
