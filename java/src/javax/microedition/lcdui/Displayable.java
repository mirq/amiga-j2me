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
    /** type: 0 pressed, 1 released, 2 repeated; ch: typed character or 0 */
    void keyEvent(int type, int keyCode, int ch) {}
    void pointerEvent(int type, int x, int y) {}   // 0 pressed, 1 released, 2 dragged

    // ---- soft keys ----
    // Up to two commands sit on the soft keys (positive left, negative
    // right). With more, the right key opens a menu of all of them.

    boolean menuOpen;
    int menuSel;

    /** A command the screen adds itself (List's select); shown first. */
    Command implicitCommand() { return null; }

    private Vector allCommands() {
        Vector v = new Vector();
        Command imp = implicitCommand();
        if (imp != null) v.addElement(imp);
        synchronized (commands) {
            for (int i = 0; i < commands.size(); i++) {
                if (commands.elementAt(i) != imp) v.addElement(commands.elementAt(i));
            }
        }
        return v;
    }

    /** The command for a soft key: 0 = left, 1 = right (null = none, or the menu). */
    Command softCommand(int side) {
        Vector v = allCommands();
        int n = v.size();
        if (n == 0) return null;
        Command pos = null, neg = null, pos2 = null;
        for (int i = 0; i < n; i++) {
            Command c = (Command) v.elementAt(i);
            if (c.isNegative()) { if (neg == null) neg = c; }
            else if (pos == null) pos = c;
            else if (pos2 == null) pos2 = c;
        }
        if (side == 0) return pos;
        if (n > 2) return null;                 // the menu
        return neg != null ? neg : pos2;
    }

    String softLabel(int side) {
        if (menuOpen) return side == 0 ? "Select" : "Back";
        Command c = softCommand(side);
        if (c != null) return c.getLabel();
        return side == 1 && allCommands().size() > 2 ? "Menu" : null;
    }

    boolean hasSoftBar() { return allCommands().size() > 0; }

    /** Soft keys and the open menu. Returns true when the key was used. */
    boolean softKey(int type, int keyCode) {
        if (menuOpen) {
            if (type == 1) return true;
            Vector v = allCommands();
            int n = v.size();
            if (keyCode == Canvas.KEY_UP) menuSel = (menuSel + n - 1) % n;
            else if (keyCode == Canvas.KEY_DOWN) menuSel = (menuSel + 1) % n;
            else if (keyCode == Canvas.KEY_FIRE || keyCode == Canvas.KEY_SOFT_LEFT) {
                menuOpen = false;
                requestRepaint();
                if (menuSel < n) fireCommand((Command) v.elementAt(menuSel));
                return true;
            } else if (keyCode == Canvas.KEY_SOFT_RIGHT || keyCode == Canvas.KEY_CLEAR) menuOpen = false;
            requestRepaint();
            return true;
        }
        if (keyCode != Canvas.KEY_SOFT_LEFT && keyCode != Canvas.KEY_SOFT_RIGHT) return false;
        if (!hasSoftBar()) return false;
        if (type != 0) return true;
        Command c = softCommand(keyCode == Canvas.KEY_SOFT_LEFT ? 0 : 1);
        if (c != null) fireCommand(c);
        else if (keyCode == Canvas.KEY_SOFT_RIGHT && allCommands().size() > 2) {
            menuOpen = true; menuSel = 0;
            requestRepaint();
        }
        return true;
    }

    void paintMenu(Graphics g) {
        Vector v = allCommands();
        Font f = Font.getFont(Font.FACE_SYSTEM, Font.STYLE_PLAIN, Font.SIZE_MEDIUM);
        int lh = f.getHeight() + 2, w = 0;
        for (int i = 0; i < v.size(); i++) w = Math.max(w, f.stringWidth(((Command) v.elementAt(i)).getLabel()));
        w = Math.min(w + 12, Display.width);
        int h = v.size() * lh + 4, x = Display.width - w, y = Display.height - Display.SOFT_BAR - h;
        g.reset(0, 0, Display.width, Display.height);
        g.setColor(0xF0F0F0); g.fillRect(x, y, w, h);
        g.setColor(0x303848); g.drawRect(x, y, w - 1, h - 1);
        g.setFont(f);
        for (int i = 0; i < v.size(); i++) {
            int ly = y + 2 + i * lh;
            if (i == menuSel) { g.setColor(0x2040A0); g.fillRect(x + 1, ly, w - 2, lh); g.setColor(0xFFFFFF); }
            else g.setColor(0);
            g.drawString(((Command) v.elementAt(i)).getLabel(), x + 6, ly + 1, Graphics.TOP | Graphics.LEFT);
        }
    }

    void fireCommand(Command c) {
        CommandListener l = listener;
        if (c != null && l != null) l.commandAction(c, this);
    }
}
