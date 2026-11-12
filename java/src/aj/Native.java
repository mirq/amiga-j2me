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
    /** A TiledLayer's cells c0..c1-1, r0..r1-1 with the layer's corner at x, y. */
    public static native void drawTiles(short[] dst, int dw, int dh, int cx, int cy, int cw, int ch,
                                        short[] src, byte[] alpha, int sw, int sh, int tileW, int tileH,
                                        int tilesPerRow, int[] cells, int columns,
                                        int c0, int r0, int c1, int r1, int x, int y, int[] animated);
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

    /** scale: each pixel shown scale x scale (1..4) */
    public static native int videoOpen(int w, int h, int scale);
    public static native void videoClose();
    public static native void present(short[] px, int w, int h, int x, int y, int rw, int rh);
    /** ev = {type, code, down, x, y, ch}. Returns 0 when there is no event. */
    public static native int pollEvent(int[] ev);
    public static native void waitEvent(int ms);
    public static native int screenshot();

    // ---- audio (native/audio): WAV and Standard MIDI Files ----
    /** Returns a handle > 0, or 0 when the data is neither or there is no audio output. */
    public static native int sndCreate(byte[] data, int off, int len);
    public static native void sndClose(int h);
    /** loops: times to play, -1 = forever; volume 0..100 */
    public static native void sndPlay(int h, int loops, int volume);
    public static native void sndStop(int h);
    public static native void sndRewind(int h);
    public static native void sndVolume(int h, int volume);
    public static native int sndPlaying(int h);
    public static native int sndDuration(int h);
    public static native int sndPosition(int h);
    /** Feeds the audio output; call every frame / event loop round. */
    public static native void audioUpdate();

    // ---- 3D (native/g3d), used by javax.microedition.m3g ----
    // i[0..9]: target width, height, clip x, y, w, h, viewport x, y, w, h
    public static final int G3D_I_FLAGS = 10, G3D_I_VERTICES = 11, G3D_I_INDICES = 12, G3D_I_POS_TYPE = 13,
        G3D_I_NRM_TYPE = 14, G3D_I_COL_COMPS = 15, G3D_I_DEF_COLOR = 16, G3D_I_TC_TYPE = 17, G3D_I_TC_COMPS = 18,
        G3D_I_TEX_W = 19, G3D_I_TEX_H = 20, G3D_I_TEX_BLEND = 21, G3D_I_TEX_COLOR = 22, G3D_I_WRAP_S = 23,
        G3D_I_WRAP_T = 24, G3D_I_BLEND = 25, G3D_I_ALPHA_REF = 26, G3D_I_LIGHTS = 27, G3D_I_FOG_MODE = 28, G3D_I_FOG_COLOR = 29,
        G3D_I_SIZE = 30;
    // f: modelview 0..15, projection 16..31 (row-major), position scale 32, bias 33..35,
    //    texcoord scale 36, bias 37..39, texture transform 40..55, material 56..69,
    //    fog near or density 70, fog far 71, lights from 72 (12 each)
    public static final int G3D_F_MV = 0, G3D_F_PROJ = 16, G3D_F_POS_SCALE = 32, G3D_F_POS_BIAS = 33,
        G3D_F_TC_SCALE = 36, G3D_F_TC_BIAS = 37, G3D_F_TEX_XF = 40, G3D_F_MATERIAL = 56, G3D_F_FOG = 70, G3D_F_LIGHTS = 72,
        G3D_LIGHT_FLOATS = 12, G3D_MAX_LIGHTS = 8, G3D_F_SIZE = 72 + 12 * 8;
    /** flags: 1 = clear colour, 2 = clear depth */
    public static native void g3dClear(short[] dst, int[] i, int argb, int flags);
    /** pos, nrm and tc are byte[] or short[] (nrm and tc may be null); idx is a triangle list. */
    public static native void g3dDraw(short[] dst, int[] i, float[] f, Object pos, Object nrm, byte[] col,
                                      Object tc, int[] idx, short[] tex, byte[] texAlpha);
    public static final int G3D_FIG_COLORKEY = 1, G3D_FIG_TWO_SIDED = 0x10, G3D_FIG_LIGHTING = 0x20;
    /**
     * A Mascot Capsule figure (see g3d.h). tris: 9 ints each; opts: vertices, triangles,
     * pattern mask, face, blending on, pass, reuse the last transform, texture w, h.
     */
    public static native void g3dFigure(short[] dst, int[] i, float[] pos, float[] nrm, float[] params,
                                        int[] tris, int[] opts, short[] tex, byte[] texAlpha);
    /** zlib data to outLen bytes; null when it is damaged. */
    public static native byte[] inflate(byte[] src, int off, int len, int outLen);

    /** Whole-file access (record stores). readFile returns null when there is no such file. */
    public static native byte[] readFile(String path);
    public static native int writeFile(String path, byte[] data, int len);
    public static native int deleteFile(String path);
    public static native int makeDir(String path);
    /** Names of the plain files in a directory, each followed by a newline. */
    public static native String listFiles(String dir);
}
