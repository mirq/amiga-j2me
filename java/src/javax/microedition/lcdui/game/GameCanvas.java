/*
 * amiga-j2me: MIDP 2.0 implementation for the Amiga J2ME runtime.
 * Written for this project; licensed under GPLv2 (see COPYING).
 */
package javax.microedition.lcdui.game;

import javax.microedition.lcdui.Canvas;
import javax.microedition.lcdui.Graphics;
import javax.microedition.lcdui.Image;
import javax.microedition.lcdui.GameAccess;

public abstract class GameCanvas extends Canvas {
    public static final int UP_PRESSED = 1 << UP, DOWN_PRESSED = 1 << DOWN, LEFT_PRESSED = 1 << LEFT,
                            RIGHT_PRESSED = 1 << RIGHT, FIRE_PRESSED = 1 << FIRE,
                            GAME_A_PRESSED = 1 << GAME_A, GAME_B_PRESSED = 1 << GAME_B,
                            GAME_C_PRESSED = 1 << GAME_C, GAME_D_PRESSED = 1 << GAME_D;

    private Image buffer;
    private final boolean suppressKeyEvents;

    protected GameCanvas(boolean suppressKeyEvents) {
        this.suppressKeyEvents = suppressKeyEvents;
        buffer = Image.createImage(getWidth(), getHeight());
    }

    private Image buffer() {
        int w = getWidth(), h = getHeight();
        if (buffer.getWidth() != w || buffer.getHeight() != h) buffer = Image.createImage(w, h);
        return buffer;
    }

    protected Graphics getGraphics() { return buffer().getGraphics(); }

    public int getKeyStates() { return GameAccess.keyStates(); }

    public void paint(Graphics g) {
        g.drawImage(buffer(), 0, 0, Graphics.TOP | Graphics.LEFT);
    }

    public void flushGraphics() {
        Image b = buffer();
        flushGraphics(0, 0, b.getWidth(), b.getHeight());
    }

    public void flushGraphics(int x, int y, int width, int height) {
        if (width <= 0 || height <= 0 || !isShown()) return;
        GameAccess.present(buffer(), x, y, width, height);
    }

    /** Whether key events are kept from keyPressed and friends (asked by Canvas). */
    public boolean suppressesGameKeys() { return suppressKeyEvents; }
}
