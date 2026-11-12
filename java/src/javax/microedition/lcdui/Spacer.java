/*
 * amiga-j2me: MIDP 2.0 implementation for the Amiga J2ME runtime.
 * Written for this project; licensed under GPLv2 (see COPYING).
 */
package javax.microedition.lcdui;

public class Spacer extends Item {
    private int w, h;
    public Spacer(int minWidth, int minHeight) { setMinimumSize(minWidth, minHeight); }
    public void setMinimumSize(int minWidth, int minHeight) {
        if (minWidth < 0 || minHeight < 0) throw new IllegalArgumentException();
        w = minWidth; h = minHeight; changed();
    }
    public void addCommand(Command cmd) { throw new IllegalStateException(); }
    public void setDefaultCommand(Command cmd) { throw new IllegalStateException(); }
    public void setLabel(String label) { throw new IllegalStateException(); }
    int contentHeight(int width) { return h; }
    void paintContent(Graphics g, int x, int y, int width, boolean focused) {}
    boolean focusable() { return false; }
}
