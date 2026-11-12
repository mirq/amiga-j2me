/*
 * amiga-j2me: MIDP 2.0 implementation for the Amiga J2ME runtime.
 * Written for this project; licensed under GPLv2 (see COPYING).
 */
package com.nokia.mid.ui;

import javax.microedition.lcdui.GameAccess;
import javax.microedition.lcdui.Graphics;
import javax.microedition.lcdui.Image;

public class DirectUtils {
    private DirectUtils() {}

    public static DirectGraphics getDirectGraphics(Graphics g) { return new DirectGraphicsImpl(g); }

    /** A mutable image from encoded image data. */
    public static Image createImage(byte[] imageData, int imageOffset, int imageLength) {
        return GameAccess.mutableCopy(Image.createImage(imageData, imageOffset, imageLength));
    }

    /** A mutable image filled with the given ARGB colour. */
    public static Image createImage(int width, int height, int ARGBcolor) {
        int[] rgb = new int[width * height];
        for (int i = rgb.length - 1; i >= 0; i--) rgb[i] = ARGBcolor;
        return GameAccess.mutableCopy(Image.createRGBImage(rgb, width, height, true));
    }
}
