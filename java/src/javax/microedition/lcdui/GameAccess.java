/*
 * amiga-j2me: MIDP 2.0 implementation for the Amiga J2ME runtime.
 * Written for this project; licensed under GPLv2 (see COPYING).
 */
package javax.microedition.lcdui;

/** What javax.microedition.lcdui.game needs from the package-private Display. Not MIDP API. */
public final class GameAccess {
    private GameAccess() {}

    public static int keyStates() {
        Display d = Display.instance;
        return d == null ? 0 : d.keyStates();
    }

    public static void present(Image buffer, int x, int y, int w, int h) {
        Display d = Display.instance;
        if (d != null) d.presentBuffer(buffer, x, y, w, h);
    }

    /** Pixel-level collision: is the image's pixel fully or partly opaque? */
    public static boolean isOpaque(Image img, int x, int y) {
        if (x < 0 || y < 0 || x >= img.w || y >= img.h) return false;
        return img.alpha == null || img.alpha[y * img.w + x] != 0;
    }

    /** A mutable image with the pixels (and alpha plane) of another one. */
    public static Image mutableCopy(Image src) {
        Image img = new Image(src.w, src.h, true);
        System.arraycopy(src.pix, 0, img.pix, 0, src.pix.length);
        if (src.alpha != null) {
            img.alpha = new byte[src.alpha.length];
            System.arraycopy(src.alpha, 0, img.alpha, 0, src.alpha.length);
        }
        return img;
    }

    /** Reads back what a Graphics has drawn (Nokia DirectGraphics.getPixels); x, y as for drawing. */
    public static void readRGB(Graphics g, int[] argb, int offset, int scanlength, int x, int y, int w, int h) {
        g.readRGB(argb, offset, scanlength, x, y, w, h);
    }
}
