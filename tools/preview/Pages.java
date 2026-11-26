import java.awt.Color;
import java.awt.Font;
import java.awt.Graphics2D;
import java.awt.RenderingHints;
import java.awt.image.BufferedImage;
import com.benjaminosleo.locumwatch.art.*;

/** Mock settings page and alarm page. The lettering here stands in for the Android text laid over the art. */
public class Pages {
    static Graphics2D text(AwtPen p) {
        Graphics2D g = p.graphics();
        g.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_ON);
        return g;
    }

    static void label(AwtPen p, String s, float x, float y, float size, int rgb, boolean bold, boolean centre) {
        Graphics2D g = text(p);
        g.setFont(new Font(Font.SERIF, bold ? Font.BOLD : Font.PLAIN, 1).deriveFont(size));
        g.setColor(new Color(rgb));
        float w = (float) g.getFontMetrics().getStringBounds(s, g).getWidth();
        g.drawString(s, centre ? x - w / 2f : x, y);
    }

    static final int BROWN = 0x4A3020, CREAM = 0xFFF4DC;

    static void decor() throws Exception {
        float W = 360, H = 1400;
        BufferedImage img = Render.image(W, H);
        AwtPen p = Render.pen(img);
        Decor d = new Decor();
        int start = p.drawCalls;
        d.wall(p, W, H, Hero.H);
        System.out.println("decor wall " + (int) W + "x" + (int) H + ": " + (p.drawCalls - start) + " draw calls");

        d.sectionHeader(p, 96, 8, 264, 54);
        label(p, "Alarm", 180, 43, 16, CREAM, true, true);
        int c0 = p.drawCalls;
        d.parchment(p, 16, 62, 344, 318, 1);
        System.out.println("parchment: " + (p.drawCalls - c0) + " draw calls");
        label(p, "Alarm switched on", 34, 100, 15, BROWN, false, false);
        d.toggle(p, 274, 80, 326, 108, true);
        label(p, "Vibrate as well", 34, 144, 15, BROWN, false, false);
        d.toggle(p, 274, 124, 326, 152, false);
        label(p, "Alarm volume", 34, 188, 15, BROWN, false, false);
        d.sliderTrack(p, 46, 200, 314, 228, 0.64f);
        d.sliderThumb(p, 46 + (314 - 46) * 0.64f, 214, 13, false);
        label(p, "WhatsApp group to watch", 34, 262, 15, BROWN, false, false);
        d.field(p, 32, 270, 328, 302, false);
        label(p, "Burnley locums", 42, 291, 15, BROWN, false, false);

        d.shelf(p, Decor.SHELF_APOTHECARY, 28, 398, 304);

        d.sectionHeader(p, 96, 432, 264, 478);
        label(p, "Checks", 180, 467, 16, CREAM, true, true);
        d.parchment(p, 16, 486, 344, 760, 2);
        String[] names = {"Notification access: granted", "Battery saver: needs turning off", "Sound the alarm now (test)", "Send a test message (pressed)"};
        int[] styles = {Decor.BUTTON_GOOD, Decor.BUTTON_ATTENTION, Decor.BUTTON_NORMAL, Decor.BUTTON_NORMAL};
        for (int i = 0; i < 4; i++) {
            float t = 508 + i * 60;
            int b0 = p.drawCalls;
            d.plankButton(p, 34, t, 326, t + 48, styles[i], i == 3);
            if (i == 0) System.out.println("plank button: " + (p.drawCalls - b0) + " draw calls");
            label(p, names[i], 180, t + 27 + (i == 3 ? 3 : 0), 14.5f, BROWN, true, true);
        }

        d.shelf(p, Decor.SHELF_LIBRARY, 28, 838, 304);

        d.sectionHeader(p, 86, 872, 274, 918);
        label(p, "Message log", 180, 907, 16, CREAM, true, true);
        d.parchment(p, 16, 926, 344, 1244, 3);
        String[] who = {"Rota coordinator  ·  06:42", "Rota coordinator  ·  Tuesday 21:15", "Sam (ODP)  ·  Monday 17:03"};
        String[] msg = {"Locum needed today 08:00 to 18:00,", "Anyone free for a long day on Thursday?", "Thanks all, shift is now covered"};
        String[] msg2 = {"obstetric theatres. Reply here.", "Bank rates apply.", ""};
        boolean[] rang = {true, true, false};
        for (int i = 0; i < 3; i++) {
            float y = 958 + i * 78;
            label(p, who[i], 34, y, 12.5f, 0x8A6A48, true, false);
            label(p, msg[i], 34, y + 20, 14.5f, BROWN, false, false);
            label(p, msg2[i], 34, y + 38, 14.5f, BROWN, false, false);
            if (rang[i]) label(p, "rang", 300, y, 12.5f, 0xB4523C, true, false);
            if (i < 2) p.line(34, y + 52, 326, y + 52, Ink.solid(0x40604020), 0.8f);
        }
        d.field(p, 32, 1194, 328, 1226, true);
        label(p, "Type a word to test the filter", 42, 1215, 14, 0x8A6A48, false, false);

        int s0 = p.drawCalls;
        d.shelf(p, Decor.SHELF_BREWING, 28, 1330, 304);
        System.out.println("shelf: " + (p.drawCalls - s0) + " draw calls");
        label(p, "Locum Watch  ·  Mochi is on duty", 180, 1384, 12.5f, 0x8A6A48, false, true);
        Render.write(img, "decor.png");

        // the same pieces by candle-light
        BufferedImage n = Render.image(W, 330);
        AwtPen q = Render.pen(n);
        d.setHour(Palette.NIGHT);
        d.wall(q, W, 330, Hero.H);
        d.sectionHeader(q, 96, 8, 264, 54);
        label(q, "Alarm", 180, 43, 16, CREAM, true, true);
        d.parchment(q, 16, 62, 344, 226, 1);
        label(q, "Alarm switched on", 34, 100, 15, BROWN, false, false);
        d.toggle(q, 274, 80, 326, 108, true);
        d.plankButton(q, 34, 126, 326, 174, Decor.BUTTON_ATTENTION, false);
        label(q, "Battery saver: needs turning off", 180, 153, 14.5f, BROWN, true, true);
        d.sliderTrack(q, 46, 184, 314, 212, 0.4f);
        d.sliderThumb(q, 46 + (314 - 46) * 0.4f, 198, 13, false);
        d.shelf(q, Decor.SHELF_BREWING, 28, 296, 304);
        Render.write(n, "decor_night.png");
    }

    static void alarmPage() throws Exception {
        float W = 360, H = 640;
        BufferedImage img = Render.image(W * 2 + 8, H);
        AwtPen p = Render.pen(img);
        p.fillRect(0, 0, W * 2 + 8, H, Ink.solid(0xFF201810));
        AlarmArt art = new AlarmArt();
        art.setHour(6.6f);
        Decor d = new Decor();
        float[] r = new float[4];
        for (int i = 0; i < 2; i++) {
            p.save();
            p.translate(i * (W + 8), 0);
            p.clipRect(0, 0, W, H);
            int a = p.drawCalls;
            art.drawStatic(p, W, H);
            int b = p.drawCalls;
            art.drawLive(p, W, H, 3.2 + i * 0.21);
            if (i == 0) System.out.println("alarm page: static " + (b - a) + ", live " + (p.drawCalls - b) + " draw calls");
            AlarmArt.messageRect(W, H, r);
            d.parchment(p, r[0], r[1], r[2], r[3], 4);
            label(p, "Rota coordinator", W / 2, r[1] + 34, 18, BROWN, true, true);
            label(p, "Burnley locums  ·  06:42", W / 2, r[1] + 54, 12.5f, 0x8A6A48, false, true);
            label(p, "Locum needed today 08:00 to 18:00,", W / 2, r[1] + 88, 16, BROWN, false, true);
            label(p, "obstetric theatres. Reply here or", W / 2, r[1] + 110, 16, BROWN, false, true);
            label(p, "ring switchboard.", W / 2, r[1] + 132, 16, BROWN, false, true);
            AlarmArt.stopRect(W, H, r);
            int s = p.drawCalls;
            art.stopButton(p, r[0], r[1], r[2], r[3], i == 1);
            if (i == 0) System.out.println("stop button: " + (p.drawCalls - s) + " draw calls, " + (r[2] - r[0]) + " x " + (r[3] - r[1]));
            label(p, "STOP", W / 2, (r[1] + r[3]) / 2f + 14 + (i == 1 ? 7 : -3), 52, 0xFFF8E8, true, true);
            AlarmArt.secondaryRect(W, H, r);
            art.secondaryButton(p, r[0], r[1], r[2], r[3], i == 1);
            label(p, "Stop and open WhatsApp", W / 2, (r[1] + r[3]) / 2f + 3 + (i == 1 ? 2.5f : 0), 13.5f, 0xFFF8E8, true, true);
            p.restore();
        }
        Render.write(img, "alarm_page.png");
    }
}
