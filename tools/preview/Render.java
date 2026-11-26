import java.awt.image.BufferedImage;
import java.io.File;
import javax.imageio.ImageIO;
import com.benjaminosleo.locumwatch.art.*;

/** Renders every preview image into tools/preview/out/. Pass names to render only some (e.g. "sheet hero"). */
public class Render {
    static final float S = 2f;
    static final File OUT = new File("tools/preview/out");

    public static void main(String[] a) throws Exception {
        OUT.mkdirs();
        java.util.Set<String> only = new java.util.HashSet<>(java.util.Arrays.asList(a));
        if (want(only, "sheet")) Sheets.mochiSheet();
        if (want(only, "hero")) Heroes.stills();
        if (want(only, "strip")) Anim.alarmStrip();
        if (want(only, "gif")) Anim.gifs();
        if (want(only, "decor")) Pages.decor();
        if (want(only, "alarm")) Pages.alarmPage();
    }

    static boolean want(java.util.Set<String> only, String name) { return only.isEmpty() || only.contains(name); }

    static BufferedImage image(float w, float h) {
        return new BufferedImage(Math.round(w * S), Math.round(h * S), BufferedImage.TYPE_INT_ARGB);
    }

    static AwtPen pen(BufferedImage img) {
        AwtPen p = new AwtPen(img);
        p.scale(S, S, 0, 0);
        return p;
    }

    static void write(BufferedImage img, String name) throws Exception {
        ImageIO.write(img, "png", new File(OUT, name));
        System.out.println("wrote " + name + " (" + img.getWidth() + "x" + img.getHeight() + ")");
    }
}
