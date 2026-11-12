/*
 * amiga-j2me: MIDP 2.0 implementation for the Amiga J2ME runtime.
 * Written for this project; licensed under GPLv2 (see COPYING).
 */
package com.siemens.mp.ui;

import java.io.IOException;

/** Siemens bitmap helpers: 1 bit per pixel, most significant bit first, a set bit is black. */
public class Image extends com.siemens.mp.misc.NativeMem {
    private javax.microedition.lcdui.Image img;

    protected Image() {}
    public Image(int width, int height) { img = javax.microedition.lcdui.Image.createImage(width, height); }
    public Image(Image source) { img = source.img; }
    public Image(byte[] data, int width, int height) { img = createImageFromBitmap(data, width, height); }
    public Image(byte[] data, int width, int height, boolean transparent) {
        img = transparent ? createTransparentImageFromBitmap(data, width, height) : createImageFromBitmap(data, width, height);
    }
    public Image(String name, boolean scale) throws IOException { img = javax.microedition.lcdui.Image.createImage(name); }
    public Image(byte[] data) { img = javax.microedition.lcdui.Image.createImage(data, 0, data.length); }

    public int getWidth() { return img.getWidth(); }
    public int getHeight() { return img.getHeight(); }
    public static javax.microedition.lcdui.Image getNativeImage(Image i) { return i == null ? null : i.img; }

    public static javax.microedition.lcdui.Image createImageFromBitmap(byte[] data, int width, int height) {
        return createImageFromBitmap(data, null, width, height);
    }

    /** mask: a set bit is an opaque pixel */
    public static javax.microedition.lcdui.Image createImageFromBitmap(byte[] data, byte[] mask, int width, int height) {
        if (data == null) return null;
        int stride = (width + 7) / 8;
        int[] px = new int[width * height];
        for (int y = 0; y < height; y++) {
            for (int x = 0; x < width; x++) {
                int i = y * stride + (x >> 3), bit = 0x80 >> (x & 7);
                if (i >= data.length) continue;
                int c = (data[i] & bit) != 0 ? 0xFF000000 : 0xFFFFFFFF;
                if (mask != null && (i >= mask.length || (mask[i] & bit) == 0)) c = 0;
                px[y * width + x] = c;
            }
        }
        return javax.microedition.lcdui.Image.createRGBImage(px, width, height, mask != null);
    }

    /** 2 bits per pixel: 00 transparent, 01 white, 1x black */
    public static javax.microedition.lcdui.Image createTransparentImageFromBitmap(byte[] data, int width, int height) {
        if (data == null) return null;
        int stride = (width + 3) / 4;
        int[] px = new int[width * height];
        for (int y = 0; y < height; y++) {
            for (int x = 0; x < width; x++) {
                int i = y * stride + (x >> 2);
                if (i >= data.length) continue;
                int v = (data[i] >> (6 - 2 * (x & 3))) & 3;
                px[y * width + x] = v == 0 ? 0 : (v == 1 ? 0xFFFFFFFF : 0xFF000000);
            }
        }
        return javax.microedition.lcdui.Image.createRGBImage(px, width, height, true);
    }

    public static javax.microedition.lcdui.Image createImageWithoutScaling(String name) throws IOException {
        return javax.microedition.lcdui.Image.createImage(name);
    }

    public static javax.microedition.lcdui.Image createImageFromFile(String name, boolean scale) throws IOException {
        return javax.microedition.lcdui.Image.createImage(name);
    }

    public static javax.microedition.lcdui.Image createRGBImage(byte[] data, int width, int height, int bitsPerPixel) {
        int[] px = new int[width * height];
        if (bitsPerPixel == 16) {
            for (int i = 0; i < px.length && i * 2 + 1 < data.length; i++) {
                int v = ((data[i * 2] & 0xFF) << 8) | (data[i * 2 + 1] & 0xFF);
                px[i] = 0xFF000000 | ((v & 0xF800) << 8) | ((v & 0x07E0) << 5) | ((v & 0x1F) << 3);
            }
        } else {
            for (int i = 0; i < px.length && i < data.length; i++) {
                int v = data[i] & 0xFF;     // 3-3-2
                px[i] = 0xFF000000 | ((v & 0xE0) << 16) | ((v & 0x1C) << 11) | ((v & 3) << 6);
            }
        }
        return javax.microedition.lcdui.Image.createRGBImage(px, width, height, false);
    }

    /** White pixels of the mask become transparent. */
    public static javax.microedition.lcdui.Image createTransparentImageFromMask(javax.microedition.lcdui.Image image,
                                                                              javax.microedition.lcdui.Image mask) {
        int w = image.getWidth(), h = image.getHeight();
        int mw = Math.min(w, mask.getWidth()), mh = Math.min(h, mask.getHeight());
        int[] px = new int[w * h], mk = new int[mw * mh];
        image.getRGB(px, 0, w, 0, 0, w, h);
        mask.getRGB(mk, 0, mw, 0, 0, mw, mh);
        for (int y = 0; y < mh; y++) {
            for (int x = 0; x < mw; x++) {
                if ((mk[y * mw + x] & 0xFFFFFF) == 0xFFFFFF) px[y * w + x] = 0;
                else px[y * w + x] |= 0xFF000000;
            }
        }
        return javax.microedition.lcdui.Image.createRGBImage(px, w, h, true);
    }

    public static void mirrorImageHorizontally(javax.microedition.lcdui.Image image) { mirror(image, true); }
    public static void mirrorImageVertically(javax.microedition.lcdui.Image image) { mirror(image, false); }

    private static void mirror(javax.microedition.lcdui.Image image, boolean horizontally) {
        if (!image.isMutable()) return;
        javax.microedition.lcdui.Image copy = javax.microedition.lcdui.Image.createImage(image, 0, 0, image.getWidth(),
            image.getHeight(), horizontally ? javax.microedition.lcdui.game.Sprite.TRANS_MIRROR
                                            : javax.microedition.lcdui.game.Sprite.TRANS_MIRROR_ROT180);
        image.getGraphics().drawImage(copy, 0, 0, 0);
    }
}
