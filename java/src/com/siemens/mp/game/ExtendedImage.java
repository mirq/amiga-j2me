/*
 * amiga-j2me: MIDP 2.0 implementation for the Amiga J2ME runtime.
 * Written for this project; licensed under GPLv2 (see COPYING).
 */
package com.siemens.mp.game;

import javax.microedition.lcdui.GameAccess;
import javax.microedition.lcdui.Image;
import javax.microedition.lcdui.Graphics;

/** A mutable image with direct pixel access; colour 0 is white, 1 is black. */
public class ExtendedImage extends com.siemens.mp.misc.NativeMem {
    private final Image image;

    public ExtendedImage(Image image) {
        if (image == null) throw new NullPointerException();
        if (image.getWidth() % 8 != 0) throw new IllegalArgumentException("width must be a multiple of 8");
        this.image = image;
    }

    public Image getImage() { return image; }
    public void blitToScreen(int x, int y) { GameAccess.blit(image, x, y); }

    public void clear(byte color) {
        Graphics g = image.getGraphics();
        g.setColor(color == 0 ? 0xFFFFFF : 0);
        g.fillRect(0, 0, image.getWidth(), image.getHeight());
    }

    public int getPixel(int x, int y) {
        int[] p = new int[1];
        image.getRGB(p, 0, 1, x, y, 1, 1);
        return (p[0] & 0xFFFFFF) == 0xFFFFFF ? 0 : 1;
    }

    public void setPixel(int x, int y, byte color) {
        Graphics g = image.getGraphics();
        g.setColor(color == 0 ? 0xFFFFFF : 0);
        g.fillRect(x, y, 1, 1);
    }

    /** 1 bit per pixel, rows padded to whole bytes */
    public void getPixelBytes(byte[] pixels, int x, int y, int width, int height) {
        int[] rgb = new int[width * height];
        int stride = (width + 7) / 8;
        image.getRGB(rgb, 0, width, x, y, width, height);
        for (int i = 0; i < stride * height && i < pixels.length; i++) pixels[i] = 0;
        for (int j = 0; j < height; j++) {
            for (int i = 0; i < width; i++) {
                int at = j * stride + (i >> 3);
                if (at < pixels.length && (rgb[j * width + i] & 0xFFFFFF) != 0xFFFFFF) pixels[at] |= 0x80 >> (i & 7);
            }
        }
    }

    public void setPixels(byte[] pixels, int x, int y, int width, int height) {
        Image part = com.siemens.mp.ui.Image.createImageFromBitmap(pixels, width, height);
        if (part != null) image.getGraphics().drawImage(part, x, y, Graphics.TOP | Graphics.LEFT);
    }
}
