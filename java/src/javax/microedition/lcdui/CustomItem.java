/*
 * amiga-j2me: MIDP 2.0 implementation for the Amiga J2ME runtime.
 * Written for this project; licensed under GPLv2 (see COPYING).
 */
package javax.microedition.lcdui;

public abstract class CustomItem extends Item {
    protected static final int TRAVERSE_HORIZONTAL = 1, TRAVERSE_VERTICAL = 2, KEY_PRESS = 4, KEY_RELEASE = 8,
                               KEY_REPEAT = 0x10, POINTER_PRESS = 0x20, POINTER_RELEASE = 0x40,
                               POINTER_DRAG = 0x80, NONE = 0;

    protected CustomItem(String label) { this.label = label; }

    public int getGameAction(int keyCode) {
        switch (keyCode) {
        case Canvas.KEY_UP: return Canvas.UP;
        case Canvas.KEY_DOWN: return Canvas.DOWN;
        case Canvas.KEY_LEFT: return Canvas.LEFT;
        case Canvas.KEY_RIGHT: return Canvas.RIGHT;
        case Canvas.KEY_FIRE: return Canvas.FIRE;
        default: return 0;
        }
    }

    protected final int getInteractionModes() { return KEY_PRESS | KEY_RELEASE | KEY_REPEAT; }
    protected abstract int getMinContentWidth();
    protected abstract int getMinContentHeight();
    protected abstract int getPrefContentWidth(int height);
    protected abstract int getPrefContentHeight(int width);
    protected void sizeChanged(int w, int h) {}
    protected final void invalidate() { changed(); }
    protected abstract void paint(Graphics g, int w, int h);
    protected final void repaint() { changed(); }
    protected final void repaint(int x, int y, int w, int h) { changed(); }
    protected boolean traverse(int dir, int viewportWidth, int viewportHeight, int[] visRect_inout) { return false; }
    protected void traverseOut() {}
    protected void keyPressed(int keyCode) {}
    protected void keyReleased(int keyCode) {}
    protected void keyRepeated(int keyCode) {}
    protected void pointerPressed(int x, int y) {}
    protected void pointerReleased(int x, int y) {}
    protected void pointerDragged(int x, int y) {}
    protected void showNotify() {}
    protected void hideNotify() {}

    int contentHeight(int w) { return getPrefContentHeight(w); }

    void paintContent(Graphics g, int x, int y, int w, boolean focused) {
        int h = getPrefContentHeight(w);
        int cx = g.getClipX(), cy = g.getClipY(), cw = g.getClipWidth(), ch = g.getClipHeight();
        g.translate(x, y);
        g.clipRect(0, 0, w, h);
        paint(g, w, h);
        g.translate(-x, -y);
        g.setClip(cx, cy, cw, ch);
    }

    boolean focusable() { return true; }

    boolean key(int type, int keyCode, int ch) {
        if (keyCode == Canvas.KEY_UP || keyCode == Canvas.KEY_DOWN) return false;   // the form moves on
        if (type == 0) keyPressed(keyCode);
        else if (type == 1) keyReleased(keyCode);
        else keyRepeated(keyCode);
        return true;
    }
}
