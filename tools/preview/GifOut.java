import java.awt.image.BufferedImage;
import java.awt.image.IndexColorModel;
import java.io.File;
import java.util.List;
import javax.imageio.IIOImage;
import javax.imageio.ImageIO;
import javax.imageio.ImageTypeSpecifier;
import javax.imageio.ImageWriter;
import javax.imageio.metadata.IIOMetadata;
import javax.imageio.metadata.IIOMetadataNode;
import javax.imageio.stream.ImageOutputStream;

/**
 * Minimal animated-GIF writer: one shared 255-colour palette (median cut over sampled frames)
 * and frame differencing (unchanged pixels become transparent) to keep files small.
 */
public class GifOut {
    static void write(List<BufferedImage> frames, int delayMs, File out) throws Exception {
        int w = frames.get(0).getWidth(), h = frames.get(0).getHeight();
        int[] pal = palette(frames, 255);
        byte[] r = new byte[256], g = new byte[256], b = new byte[256];
        for (int i = 0; i < pal.length; i++) { r[i] = (byte) (pal[i] >> 16); g[i] = (byte) (pal[i] >> 8); b[i] = (byte) pal[i]; }
        IndexColorModel icm = new IndexColorModel(8, 256, r, g, b, 255);
        byte[] lut = new byte[1 << 15];
        boolean[] known = new boolean[1 << 15];
        ImageWriter wr = ImageIO.getImageWritersBySuffix("gif").next();
        out.delete();
        try (ImageOutputStream ios = ImageIO.createImageOutputStream(out)) {
            wr.setOutput(ios);
            wr.prepareWriteSequence(null);
            byte[] prev = null;
            for (int f = 0; f < frames.size(); f++) {
                int[] px = frames.get(f).getRGB(0, 0, w, h, null, 0, w);
                byte[] idx = new byte[w * h], outIdx = new byte[w * h];
                for (int i = 0; i < px.length; i++) {
                    int c = px[i], key = ((c >> 9) & 0x7C00) | ((c >> 6) & 0x3E0) | ((c >> 3) & 0x1F);
                    if (!known[key]) { lut[key] = (byte) nearest(pal, c); known[key] = true; }
                    idx[i] = lut[key];
                    outIdx[i] = prev != null && prev[i] == idx[i] ? (byte) 255 : idx[i];
                }
                prev = idx;
                BufferedImage bi = new BufferedImage(w, h, BufferedImage.TYPE_BYTE_INDEXED, icm);
                bi.getRaster().setDataElements(0, 0, w, h, outIdx);
                IIOMetadata md = wr.getDefaultImageMetadata(new ImageTypeSpecifier(bi), null);
                String fmt = md.getNativeMetadataFormatName();
                IIOMetadataNode root = (IIOMetadataNode) md.getAsTree(fmt);
                IIOMetadataNode gce = child(root, "GraphicControlExtension");
                gce.setAttribute("disposalMethod", "doNotDispose");
                gce.setAttribute("userInputFlag", "FALSE");
                gce.setAttribute("transparentColorFlag", f == 0 ? "FALSE" : "TRUE");
                gce.setAttribute("delayTime", Integer.toString(Math.max(2, delayMs / 10)));
                gce.setAttribute("transparentColorIndex", "255");
                if (f == 0) {
                    IIOMetadataNode apps = child(root, "ApplicationExtensions");
                    IIOMetadataNode app = new IIOMetadataNode("ApplicationExtension");
                    app.setAttribute("applicationID", "NETSCAPE");
                    app.setAttribute("authenticationCode", "2.0");
                    app.setUserObject(new byte[]{1, 0, 0});
                    apps.appendChild(app);
                }
                md.setFromTree(fmt, root);
                wr.writeToSequence(new IIOImage(bi, null, md), null);
            }
            wr.endWriteSequence();
        }
    }

    private static IIOMetadataNode child(IIOMetadataNode root, String name) {
        for (int i = 0; i < root.getLength(); i++) if (root.item(i).getNodeName().equals(name)) return (IIOMetadataNode) root.item(i);
        IIOMetadataNode n = new IIOMetadataNode(name);
        root.appendChild(n);
        return n;
    }

    private static int nearest(int[] pal, int c) {
        int cr = (c >> 16) & 255, cg = (c >> 8) & 255, cb = c & 255, best = 0, bd = Integer.MAX_VALUE;
        for (int i = 0; i < pal.length; i++) {
            int dr = cr - ((pal[i] >> 16) & 255), dg = cg - ((pal[i] >> 8) & 255), db = cb - (pal[i] & 255);
            int d = dr * dr * 2 + dg * dg * 4 + db * db * 3;
            if (d < bd) { bd = d; best = i; }
        }
        return best;
    }

    /** Median-cut palette from a sample of pixels across the frames. */
    private static int[] palette(List<BufferedImage> frames, int n) {
        int w = frames.get(0).getWidth(), h = frames.get(0).getHeight();
        int step = Math.max(1, frames.size() / 8);
        java.util.ArrayList<int[]> boxes = new java.util.ArrayList<>();
        int count = 0;
        for (int f = 0; f < frames.size(); f += step) count += (w / 2) * (h / 2);
        int[] all = new int[count];
        int k = 0;
        for (int f = 0; f < frames.size(); f += step)
            for (int y = 0; y < h - 1; y += 2) for (int x = 0; x < w - 1; x += 2) all[k++] = frames.get(f).getRGB(x, y) & 0xFFFFFF;
        boxes.add(all);
        while (boxes.size() < n) {
            int bi = -1, bestRange = 0, bestCh = 0;
            for (int i = 0; i < boxes.size(); i++) {
                int[] bx = boxes.get(i);
                if (bx.length < 2) continue;
                for (int ch = 0; ch < 3; ch++) {
                    int lo = 255, hi = 0, sh = 16 - ch * 8;
                    for (int c : bx) { int v = (c >> sh) & 255; if (v < lo) lo = v; if (v > hi) hi = v; }
                    long score = (long) (hi - lo) * (long) Math.sqrt(bx.length);
                    if (score > bestRange) { bestRange = (int) Math.min(Integer.MAX_VALUE, score); bi = i; bestCh = ch; }
                }
            }
            if (bi < 0) break;
            int[] bx = boxes.remove(bi);
            final int sh = 16 - bestCh * 8;
            long[] keyed = new long[bx.length];
            for (int i = 0; i < bx.length; i++) keyed[i] = ((long) ((bx[i] >> sh) & 255) << 32) | bx[i];
            java.util.Arrays.sort(keyed);
            int mid = bx.length / 2;
            int[] a = new int[mid], b = new int[bx.length - mid];
            for (int i = 0; i < bx.length; i++) { if (i < mid) a[i] = (int) keyed[i]; else b[i - mid] = (int) keyed[i]; }
            boxes.add(a); boxes.add(b);
        }
        int[] pal = new int[boxes.size()];
        for (int i = 0; i < pal.length; i++) {
            long r = 0, g = 0, b = 0;
            int[] bx = boxes.get(i);
            for (int c : bx) { r += (c >> 16) & 255; g += (c >> 8) & 255; b += c & 255; }
            int m = Math.max(1, bx.length);
            pal[i] = (int) (r / m) << 16 | (int) (g / m) << 8 | (int) (b / m);
        }
        return pal;
    }
}
