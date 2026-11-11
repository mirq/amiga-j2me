/*
 * amiga-j2me: MIDP 2.0 implementation for the Amiga J2ME runtime.
 * Written for this project; licensed under GPLv2 (see COPYING).
 */
package javax.microedition.lcdui;

import java.util.Vector;

public abstract class Displayable {
    final Vector commands = new Vector();
    CommandListener listener;
    private String title;
    private Ticker ticker;

    Displayable() {}

    public String getTitle() { return title; }
    public void setTitle(String s) { title = s; requestRepaint(); }
    public Ticker getTicker() { return ticker; }
    public void setTicker(Ticker t) { ticker = t; requestRepaint(); }
    public boolean isShown() { return Display.instance != null && Display.instance.current == this; }

    public void addCommand(Command cmd) {
        if (cmd == null) throw new NullPointerException();
        synchronized (commands) {
            if (commands.contains(cmd)) return;
            int i = 0;      // keep sorted by priority (lower value first)
            while (i < commands.size() && ((Command) commands.elementAt(i)).getPriority() <= cmd.getPriority()) i++;
            commands.insertElementAt(cmd, i);
        }
        requestRepaint();
    }

    public void removeCommand(Command cmd) {
        commands.removeElement(cmd);
        requestRepaint();
    }

    public void setCommandListener(CommandListener l) { listener = l; }

    public int getWidth() { return Display.width; }
    public int getHeight() { return Display.height; }
    protected void sizeChanged(int w, int h) {}

    void requestRepaint() {
        Display d = Display.instance;
        if (d != null && d.current == this) d.requestRepaint(0, 0, Display.width, Display.height);
    }

    // ---- hooks for the event thread (Display) ----
    void showNotifyImpl() {}
    void hideNotifyImpl() {}
    /** Draws the whole screen. */
    abstract void paintScreen(Graphics g);
    void keyEvent(int type, int keyCode) {}        // 0 pressed, 1 released, 2 repeated
    void pointerEvent(int type, int x, int y) {}   // 0 pressed, 1 released, 2 dragged

    /** The command for a soft key: 0 = left, 1 = right. */
    Command softCommand(int side) {
        synchronized (commands) {
            int n = commands.size();
            if (n == 0) return null;
            Command neg = null, pos = null;
            for (int i = 0; i < n; i++) {
                Command c = (Command) commands.elementAt(i);
                if (c.isNegative()) { if (neg == null) neg = c; }
                else if (pos == null) pos = c;
            }
            if (n == 1) return side == 0 ? (pos != null ? pos : null) : neg;
            if (side == 1) {
                if (neg != null) return neg;
                // two positive commands: the second goes right
                int seen = 0;
                for (int i = 0; i < n; i++) {
                    Command c = (Command) commands.elementAt(i);
                    if (!c.isNegative() && seen++ == 1) return c;
                }
                return null;
            }
            return pos;
        }
    }

    void fireCommand(Command c) {
        CommandListener l = listener;
        if (c != null && l != null) l.commandAction(c, this);
    }
}
