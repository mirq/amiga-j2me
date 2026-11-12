/*
 * amiga-j2me: JSR-184 (M3G 1.0/1.1) implementation for the Amiga J2ME runtime.
 * Written for this project; licensed under GPLv2 (see COPYING).
 */
package javax.microedition.m3g;

import javax.microedition.lcdui.GameAccess;
import javax.microedition.lcdui.Image;

/** Stored as the rasteriser wants it: R5G6B5 pixels and, for formats with alpha, an alpha plane. */
public class Image2D extends Object3D {
    public static final int ALPHA = 96, LUMINANCE = 97, LUMINANCE_ALPHA = 98, RGB = 99, RGBA = 100;

    final int format, width, height;
    short[] pix;
    byte[] alpha;
    private final boolean mutable;

    private static int bpp(int format) {
        switch (format) {
        case ALPHA: case LUMINANCE: return 1;
        case LUMINANCE_ALPHA: return 2;
        case RGB: return 3;
        case RGBA: return 4;
        default: throw new IllegalArgumentException();
        }
    }

    public Image2D(int format, int width, int height) {
        bpp(format);
        if (width <= 0 || height <= 0) throw new IllegalArgumentException();
        this.format = format; this.width = width; this.height = height;
        pix = new short[width * height];
        for (int i = 0; i < pix.length; i++) pix[i] = (short) 0xFFFF;
        if (format == ALPHA || format == LUMINANCE_ALPHA || format == RGBA) {
            alpha = new byte[width * height];
            for (int i = 0; i < alpha.length; i++) alpha[i] = (byte) 0xFF;
        }
        mutable = true;
    }

    public Image2D(int format, int width, int height, byte[] image) {
        int n = bpp(format);
        if (width <= 0 || height <= 0 || image.length < width * height * n) throw new IllegalArgumentException();
        this.format = format; this.width = width; this.height = height;
        pix = new short[width * height];
        if (format == ALPHA || format == LUMINANCE_ALPHA || format == RGBA) alpha = new byte[width * height];
        store(0, 0, width, height, image, null);
        mutable = false;
    }

    public Image2D(int format, int width, int height, byte[] image, byte[] palette) {
        int n = bpp(format);
        if (width <= 0 || height <= 0 || image.length < width * height) throw new IllegalArgumentException();
        if (palette.length < 256 * n && palette.length % n != 0) throw new IllegalArgumentException();
        this.format = format; this.width = width; this.height = height;
        pix = new short[width * height];
        if (format == ALPHA || format == LUMINANCE_ALPHA || format == RGBA) alpha = new byte[width * height];
        store(0, 0, width, height, image, palette);
        mutable = false;
    }

    public Image2D(int format, Object image) {
        bpp(format);
        if (!(image instanceof Image)) throw new IllegalArgumentException();
        Image img = (Image) image;
        this.format = format; width = img.getWidth(); height = img.getHeight();
        int[] argb = new int[width * height];
        img.getRGB(argb, 0, width, 0, 0, width, height);
        pix = new short[width * height];
        if (format == ALPHA || format == LUMINANCE_ALPHA || format == RGBA) alpha = new byte[width * height];
        for (int i = 0; i < argb.length; i++) {
            int p = argb[i], r = (p >> 16) & 255, g = (p >> 8) & 255, b = p & 255;
            if (format == LUMINANCE || format == LUMINANCE_ALPHA) { r = g = b = (r * 77 + g * 150 + b * 29) >> 8; }
            if (format == ALPHA) { r = g = b = 255; }
            pix[i] = (short) (((r & 0xF8) << 8) | ((g & 0xFC) << 3) | (b >> 3));
            if (alpha != null) alpha[i] = (byte) (format == ALPHA ? ((r * 77 + g * 150 + b * 29) >> 8) : (p >>> 24));
        }
        mutable = false;
    }

    private void store(int x, int y, int w, int h, byte[] src, byte[] palette) {
        int n = bpp(format), s = 0;
        for (int row = 0; row < h; row++) {
            for (int col = 0; col < w; col++) {
                byte[] from = src;
                int at = s;
                if (palette != null) { from = palette; at = (src[s] & 255) * n; s++; }
                else s += n;
                int r, g, b, a = 255;
                switch (format) {
                case ALPHA: r = g = b = 255; a = from[at] & 255; break;
                case LUMINANCE: r = g = b = from[at] & 255; break;
                case LUMINANCE_ALPHA: r = g = b = from[at] & 255; a = from[at + 1] & 255; break;
                case RGB: r = from[at] & 255; g = from[at + 1] & 255; b = from[at + 2] & 255; break;
                default: r = from[at] & 255; g = from[at + 1] & 255; b = from[at + 2] & 255; a = from[at + 3] & 255; break;
                }
                int i = (y + row) * width + x + col;
                pix[i] = (short) (((r & 0xF8) << 8) | ((g & 0xFC) << 3) | (b >> 3));
                if (alpha != null) alpha[i] = (byte) a;
            }
        }
    }

    public void set(int x, int y, int w, int h, byte[] image) {
        if (!mutable) throw new IllegalStateException();
        if (x < 0 || y < 0 || w <= 0 || h <= 0 || x + w > width || y + h > height
            || image.length < w * h * bpp(format)) {
            throw new IllegalArgumentException();
        }
        store(x, y, w, h, image, null);
    }

    public boolean isMutable() { return mutable; }
    public int getFormat() { return format; }
    public int getWidth() { return width; }
    public int getHeight() { return height; }

    /** As an lcdui image (backgrounds and sprites are drawn in 2D). */
    Image toImage() {
        int[] argb = new int[width * height];
        for (int i = 0; i < argb.length; i++) {
            int p = pix[i] & 0xFFFF;
            argb[i] = ((alpha == null ? 255 : alpha[i] & 255) << 24) | ((p & 0xF800) << 8) | ((p & 0x07E0) << 5) | ((p & 0x1F) << 3);
        }
        return Image.createRGBImage(argb, width, height, alpha != null);
    }
}
