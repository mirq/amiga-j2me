/*
 * amiga-j2me: MIDP 2.0 implementation for the Amiga J2ME runtime.
 * Written for this project; licensed under GPLv2 (see COPYING).
 */
package com.siemens.mp.game;

import javax.microedition.lcdui.Graphics;
import javax.microedition.lcdui.Image;

/** A sprite whose frames are stacked top to bottom in one image. */
public class Sprite extends GraphicObject {
    private final Image frames;
    private final int count, w, h;
    private int x, y, frame;
    private int cx, cy, cw, ch;

    public Sprite(byte[] pixels, int pixelOffset, int width, int height, byte[] mask, int maskOffset, int numFrames) {
        this(com.siemens.mp.ui.Image.createImageFromBitmap(cut(pixels, pixelOffset), cut(mask, maskOffset), width, height * numFrames),
             null, numFrames);
    }

    private static byte[] cut(byte[] a, int off) {
        if (a == null || off <= 0) return a;
        byte[] r = new byte[a.length - off];
        System.arraycopy(a, off, r, 0, r.length);
        return r;
    }

    public Sprite(ExtendedImage pixels, ExtendedImage mask, int numFrames) {
        this(pixels.getImage(), mask == null ? null : mask.getImage(), numFrames);
    }

    public Sprite(Image pixels, Image mask, int numFrames) {
        if (numFrames < 1) throw new IllegalArgumentException();
        frames = mask == null ? pixels : com.siemens.mp.ui.Image.createTransparentImageFromMask(pixels, mask);
        count = numFrames;
        w = frames.getWidth();
        h = frames.getHeight() / numFrames;
        cw = w;
        ch = h;
    }

    public int getFrame() { return frame; }
    public int getXPosition() { return x; }
    public int getYPosition() { return y; }
    public void setFrame(int n) { if (n >= 0 && n < count) frame = n; }
    public void setPosition(int x, int y) { this.x = x; this.y = y; }

    public void setCollisionRectangle(int x, int y, int width, int height) {
        cx = x; cy = y; cw = width; ch = height;
    }

    public boolean isCollidingWith(Sprite o) {
        return x + cx < o.x + o.cx + o.cw && o.x + o.cx < x + cx + cw
            && y + cy < o.y + o.cy + o.ch && o.y + o.cy < y + cy + ch;
    }

    public boolean isCollidingWithPos(int px, int py) {
        return px >= x + cx && px < x + cx + cw && py >= y + cy && py < y + cy + ch;
    }

    protected void paint(Graphics g) {
        g.drawRegion(frames, 0, frame * h, w, h, 0, x, y, Graphics.TOP | Graphics.LEFT);
    }
}
