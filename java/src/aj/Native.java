/*
 * amiga-j2me: MIDP 2.0 implementation for the Amiga J2ME runtime.
 * Written for this project; licensed under GPLv2 (see COPYING).
 */
package aj;

/**
 * Native methods of the runtime (native/kni). Every drawing method takes the
 * destination first: pixel array (R5G6B5), its width and height, and the clip
 * rectangle in surface coordinates.
 */
public final class Native {
    private Native() {}

    public static native void fillRect(short[] dst, int dw, int dh, int cx, int cy, int cw, int ch,
                                       int rgb, int x, int y, int w, int h);
    public static native void drawLine(short[] dst, int dw, int dh, int cx, int cy, int cw, int ch,
                                       int rgb, int x0, int y0, int x1, int y1, int dotted);
    public static native void fillTriangle(short[] dst, int dw, int dh, int cx, int cy, int cw, int ch,
                                           int rgb, int x0, int y0, int x1, int y1, int x2, int y2);
    public static native void arc(short[] dst, int dw, int dh, int cx, int cy, int cw, int ch,
                                  int rgb, int x, int y, int w, int h, int start, int sweep,
                                  int fill, int dotted);
    public static native void blit(short[] dst, int dw, int dh, int cx, int cy, int cw, int ch,
                                   short[] src, byte[] alpha, int sw, int sh,
                                   int sx, int sy, int w, int h, int transform, int dx, int dy);
    public static native void drawRGB(short[] dst, int dw, int dh, int cx, int cy, int cw, int ch,
                                      int[] rgb, int offset, int scanlength,
                                      int x, int y, int w, int h, int processAlpha);
    /** Returns the advance in pixels. */
    public static native int drawChars(short[] dst, int dw, int dh, int cx, int cy, int cw, int ch,
                                       int rgb, int font, char[] text, int off, int len, int x, int y);
    public static native int drawString(short[] dst, int dw, int dh, int cx, int cy, int cw, int ch,
                                        int rgb, int font, String text, int off, int len, int x, int y);
    public static native int charsWidth(int font, char[] text, int off, int len);
    public static native int stringWidth(int font, String text, int off, int len);
    /** height << 8 | ascent */
    public static native int fontInfo(int font);

    /** Returns 1 when some pixel is not fully opaque. alpha may be null. */
    public static native int fromRGB(int[] rgb, int off, int n, short[] px, byte[] alpha);
    public static native void toRGB(short[] px, byte[] alpha, int sw, int[] rgb, int off, int scan,
                                    int x, int y, int w, int h);

    /** Returns w | h << 15 | hasAlpha << 30, or 0 when the data is no image. */
    public static native int decodeBegin(byte[] data, int off, int len);
    public static native void decodeEnd(short[] px, byte[] alpha);

    public static native int videoOpen(int w, int h);
    public static native void videoClose();
    public static native void present(short[] px, int w, int h, int x, int y, int rw, int rh);
    /** ev = {type, code, down, x, y, ch}. Returns 0 when there is no event. */
    public static native int pollEvent(int[] ev);
    public static native void waitEvent(int ms);
    public static native int screenshot();

    /** Whole-file access (record stores). readFile returns null when there is no such file. */
    public static native byte[] readFile(String path);
    public static native int writeFile(String path, byte[] data, int len);
    public static native int deleteFile(String path);
    public static native int makeDir(String path);
    /** Names of the plain files in a directory, each followed by a newline. */
    public static native String listFiles(String dir);
}
