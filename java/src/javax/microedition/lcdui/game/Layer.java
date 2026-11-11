/*
 * amiga-j2me: MIDP 2.0 implementation for the Amiga J2ME runtime.
 * Written for this project; licensed under GPLv2 (see COPYING).
 */
package javax.microedition.lcdui.game;

import javax.microedition.lcdui.Graphics;

public abstract class Layer {
    int x, y, width, height;
    boolean visible = true;

    Layer(int width, int height) { setSize(width, height); }

    void setSize(int w, int h) {
        if (w < 1 || h < 1) throw new IllegalArgumentException();
        width = w; height = h;
    }

    public void setPosition(int x, int y) { this.x = x; this.y = y; }
    public void move(int dx, int dy) { x += dx; y += dy; }
    public final int getX() { return x; }
    public final int getY() { return y; }
    public final int getWidth() { return width; }
    public final int getHeight() { return height; }
    public void setVisible(boolean visible) { this.visible = visible; }
    public final boolean isVisible() { return visible; }
    public abstract void paint(Graphics g);
}
