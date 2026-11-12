/*
 * amiga-j2me: MIDP 2.0 implementation for the Amiga J2ME runtime.
 * Written for this project; licensed under GPLv2 (see COPYING).
 */
package com.nokia.mid.ui;

import javax.microedition.lcdui.GameAccess;
import javax.microedition.lcdui.Graphics;
import javax.microedition.lcdui.Image;

final class DirectGraphicsImpl implements DirectGraphics {
    private final Graphics g;
    private int alpha = 0xFF;

    DirectGraphicsImpl(Graphics g) { this.g = g; }

    public void setARGBColor(int argb) {
        alpha = argb >>> 24;
        g.setColor(argb & 0xFFFFFF);
    }

    public int getAlphaComponent() { return alpha; }
    public int getNativePixelFormat() { return TYPE_USHORT_565_RGB; }

    /** Nokia manipulation (flip, then rotate counter-clockwise) as a MIDP 2 sprite transform. */
    private static int transform(int m) {
        switch (m) {
        case FLIP_HORIZONTAL: return 2;                     // TRANS_MIRROR
        case FLIP_VERTICAL: return 1;                       // TRANS_MIRROR_ROT180
        case ROTATE_90: return 6;                           // TRANS_ROT270
        case ROTATE_180: return 3;                          // TRANS_ROT180
        case ROTATE_270: return 5;                          // TRANS_ROT90
        case FLIP_HORIZONTAL | FLIP_VERTICAL: return 3;
        case FLIP_HORIZONTAL | ROTATE_90: return 4;         // TRANS_MIRROR_ROT270
        case FLIP_HORIZONTAL | ROTATE_180: return 1;
        case FLIP_HORIZONTAL | ROTATE_270: return 7;        // TRANS_MIRROR_ROT90
        case FLIP_VERTICAL | ROTATE_90: return 7;
        case FLIP_VERTICAL | ROTATE_180: return 2;
        case FLIP_VERTICAL | ROTATE_270: return 4;
        case FLIP_HORIZONTAL | FLIP_VERTICAL | ROTATE_90: return 5;
        case FLIP_HORIZONTAL | FLIP_VERTICAL | ROTATE_180: return 0;
        case FLIP_HORIZONTAL | FLIP_VERTICAL | ROTATE_270: return 6;
        default: return 0;
        }
    }

    public void drawImage(Image img, int x, int y, int anchor, int manipulation) {
        if (img == null) throw new NullPointerException();
        int t = transform(manipulation);
        if (t == 0) g.drawImage(img, x, y, anchor);
        else g.drawRegion(img, 0, 0, img.getWidth(), img.getHeight(), t, x, y, anchor);
    }

    public void drawTriangle(int x1, int y1, int x2, int y2, int x3, int y3, int argbColor) {
        setARGBColor(argbColor);
        if (alpha == 0) return;
        g.drawLine(x1, y1, x2, y2);
        g.drawLine(x2, y2, x3, y3);
        g.drawLine(x3, y3, x1, y1);
    }

    public void fillTriangle(int x1, int y1, int x2, int y2, int x3, int y3, int argbColor) {
        setARGBColor(argbColor);
        if (alpha == 0) return;
        g.fillTriangle(x1, y1, x2, y2, x3, y3);
    }

    public void drawPolygon(int[] xPoints, int xOffset, int[] yPoints, int yOffset, int nPoints, int argbColor) {
        setARGBColor(argbColor);
        if (alpha == 0) return;
        for (int i = 0; i < nPoints; i++) {
            int j = (i + 1) % nPoints;
            g.drawLine(xPoints[xOffset + i], yPoints[yOffset + i], xPoints[xOffset + j], yPoints[yOffset + j]);
        }
    }

    /** A triangle fan from the first point: right for convex polygons. */
    public void fillPolygon(int[] xPoints, int xOffset, int[] yPoints, int yOffset, int nPoints, int argbColor) {
        setARGBColor(argbColor);
        if (alpha == 0) return;
        for (int i = 1; i + 1 < nPoints; i++) {
            g.fillTriangle(xPoints[xOffset], yPoints[yOffset], xPoints[xOffset + i], yPoints[yOffset + i],
                           xPoints[xOffset + i + 1], yPoints[yOffset + i + 1]);
        }
    }

    /** ARGB pixels through the manipulation, then drawn. */
    private void drawARGB(int[] argb, int w, int h, int x, int y, int manipulation, boolean processAlpha) {
        int t = transform(manipulation);
        if (t == 0) {
            g.drawRGB(argb, 0, w, x, y, w, h, processAlpha);
        } else {
            Image img = Image.createRGBImage(argb, w, h, processAlpha);
            g.drawRegion(img, 0, 0, w, h, t, x, y, Graphics.TOP | Graphics.LEFT);
        }
    }

    public void drawPixels(int[] pixels, boolean transparency, int offset, int scanlength,
                           int x, int y, int width, int height, int manipulation, int format) {
        if (format != TYPE_INT_888_RGB && format != TYPE_INT_8888_ARGB) throw new IllegalArgumentException();
        boolean a = transparency && format == TYPE_INT_8888_ARGB;
        if (manipulation == 0) { g.drawRGB(pixels, offset, scanlength, x, y, width, height, a); return; }
        int[] argb = new int[width * height];
        for (int r = 0; r < height; r++) System.arraycopy(pixels, offset + r * scanlength, argb, r * width, width);
        drawARGB(argb, width, height, x, y, manipulation, a);
    }

    public void drawPixels(short[] pixels, boolean transparency, int offset, int scanlength,
                           int x, int y, int width, int height, int manipulation, int format) {
        int[] argb = new int[width * height];
        boolean hasAlpha = false;
        for (int r = 0; r < height; r++) {
            int s = offset + r * scanlength, d = r * width;
            for (int c = 0; c < width; c++) {
                int p = pixels[s + c] & 0xFFFF, v;
                switch (format) {
                case TYPE_USHORT_4444_ARGB:
                    v = ((p & 0xF000) << 16) | ((p & 0xF000) << 12) | ((p & 0x0F00) << 12) | ((p & 0x0F00) << 8)
                      | ((p & 0x00F0) << 8) | ((p & 0x00F0) << 4) | ((p & 0x000F) << 4) | (p & 0x000F);
                    hasAlpha = true;
                    break;
                case TYPE_USHORT_444_RGB:
                    v = 0xFF000000 | ((p & 0x0F00) << 12) | ((p & 0x0F00) << 8) | ((p & 0x00F0) << 8)
                      | ((p & 0x00F0) << 4) | ((p & 0x000F) << 4) | (p & 0x000F);
                    break;
                case TYPE_USHORT_565_RGB:
                    v = 0xFF000000 | ((p & 0xF800) << 8) | ((p & 0x07E0) << 5) | ((p & 0x001F) << 3);
                    break;
                case TYPE_USHORT_555_RGB:
                    v = 0xFF000000 | ((p & 0x7C00) << 9) | ((p & 0x03E0) << 6) | ((p & 0x001F) << 3);
                    break;
                case TYPE_USHORT_1555_ARGB:
                    v = ((p & 0x8000) != 0 ? 0xFF000000 : 0) | ((p & 0x7C00) << 9) | ((p & 0x03E0) << 6) | ((p & 0x001F) << 3);
                    hasAlpha = true;
                    break;
                default:
                    throw new IllegalArgumentException();
                }
                argb[d + c] = v;
            }
        }
        drawARGB(argb, width, height, x, y, manipulation, transparency && hasAlpha);
    }

    public void drawPixels(byte[] pixels, byte[] transparencyMask, int offset, int scanlength,
                           int x, int y, int width, int height, int manipulation, int format) {
        if (format != TYPE_BYTE_1_GRAY) throw new IllegalArgumentException();
        // 1 bit per pixel, most significant bit first; offset and scanlength count pixels
        int[] argb = new int[width * height];
        for (int r = 0; r < height; r++) {
            for (int c = 0; c < width; c++) {
                int bit = offset + r * scanlength + c;
                boolean on = (pixels[bit >> 3] & (0x80 >> (bit & 7))) != 0;
                boolean opaque = transparencyMask == null || (transparencyMask[bit >> 3] & (0x80 >> (bit & 7))) != 0;
                argb[r * width + c] = (opaque ? 0xFF000000 : 0) | (on ? 0 : 0xFFFFFF);
            }
        }
        drawARGB(argb, width, height, x, y, manipulation, transparencyMask != null);
    }

    public void getPixels(int[] pixels, int offset, int scanlength, int x, int y, int width, int height, int format) {
        if (format != TYPE_INT_888_RGB && format != TYPE_INT_8888_ARGB) throw new IllegalArgumentException();
        GameAccess.readRGB(g, pixels, offset, scanlength, x, y, width, height);
    }

    public void getPixels(short[] pixels, int offset, int scanlength, int x, int y, int width, int height, int format) {
        int[] argb = new int[width * height];
        GameAccess.readRGB(g, argb, 0, width, x, y, width, height);
        for (int r = 0; r < height; r++) {
            for (int c = 0; c < width; c++) {
                int v = argb[r * width + c], p;
                switch (format) {
                case TYPE_USHORT_4444_ARGB:
                    p = 0xF000 | ((v >> 12) & 0x0F00) | ((v >> 8) & 0x00F0) | ((v >> 4) & 0x000F); break;
                case TYPE_USHORT_444_RGB:
                    p = ((v >> 12) & 0x0F00) | ((v >> 8) & 0x00F0) | ((v >> 4) & 0x000F); break;
                case TYPE_USHORT_565_RGB:
                    p = ((v >> 8) & 0xF800) | ((v >> 5) & 0x07E0) | ((v >> 3) & 0x001F); break;
                case TYPE_USHORT_555_RGB:
                    p = ((v >> 9) & 0x7C00) | ((v >> 6) & 0x03E0) | ((v >> 3) & 0x001F); break;
                case TYPE_USHORT_1555_ARGB:
                    p = 0x8000 | ((v >> 9) & 0x7C00) | ((v >> 6) & 0x03E0) | ((v >> 3) & 0x001F); break;
                default:
                    throw new IllegalArgumentException();
                }
                pixels[offset + r * scanlength + c] = (short) p;
            }
        }
    }

    public void getPixels(byte[] pixels, byte[] transparencyMask, int offset, int scanlength,
                          int x, int y, int width, int height, int format) {
        if (format != TYPE_BYTE_1_GRAY) throw new IllegalArgumentException();
        int[] argb = new int[width * height];
        GameAccess.readRGB(g, argb, 0, width, x, y, width, height);
        for (int r = 0; r < height; r++) {
            for (int c = 0; c < width; c++) {
                int v = argb[r * width + c], bit = offset + r * scanlength + c;
                int lum = ((v >> 16) & 0xFF) * 77 + ((v >> 8) & 0xFF) * 150 + (v & 0xFF) * 29;
                if (lum < 128 * 256) pixels[bit >> 3] |= (byte) (0x80 >> (bit & 7));
                else pixels[bit >> 3] &= (byte) ~(0x80 >> (bit & 7));
                if (transparencyMask != null) transparencyMask[bit >> 3] |= (byte) (0x80 >> (bit & 7));
            }
        }
    }
}
