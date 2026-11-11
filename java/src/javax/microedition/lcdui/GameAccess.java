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
}
