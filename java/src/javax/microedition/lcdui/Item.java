/*
 * amiga-j2me: MIDP 2.0 implementation for the Amiga J2ME runtime.
 * Written for this project; licensed under GPLv2 (see COPYING).
 */
package javax.microedition.lcdui;

import java.util.Vector;

public abstract class Item {
    public static final int LAYOUT_DEFAULT = 0, LAYOUT_LEFT = 1, LAYOUT_RIGHT = 2, LAYOUT_CENTER = 3,
                            LAYOUT_TOP = 0x10, LAYOUT_BOTTOM = 0x20, LAYOUT_VCENTER = 0x30,
                            LAYOUT_NEWLINE_BEFORE = 0x100, LAYOUT_NEWLINE_AFTER = 0x200,
                            LAYOUT_SHRINK = 0x400, LAYOUT_EXPAND = 0x800, LAYOUT_VSHRINK = 0x1000,
                            LAYOUT_VEXPAND = 0x2000, LAYOUT_2 = 0x4000;
    public static final int PLAIN = 0, HYPERLINK = 1, BUTTON = 2;

    String label;
    int layout;
    Form form;
    final Vector commands = new Vector();
    Command defaultCommand;
    ItemCommandListener commandListener;
    int y, height;      // set by the form's layout

    Item() {}

    public String getLabel() { return label; }
    public void setLabel(String l) { label = l; changed(); }
    public int getLayout() { return layout; }
    public void setLayout(int l) { layout = l; changed(); }
    public void addCommand(Command cmd) {
        if (cmd == null) throw new NullPointerException();
        if (!commands.contains(cmd)) commands.addElement(cmd);
    }
    public void removeCommand(Command cmd) {
        commands.removeElement(cmd);
        if (defaultCommand == cmd) defaultCommand = null;
    }
    public void setItemCommandListener(ItemCommandListener l) { commandListener = l; }
    public void setDefaultCommand(Command cmd) {
        defaultCommand = cmd;
        if (cmd != null) addCommand(cmd);
    }
    public int getPreferredWidth() { return Display.width; }
    public int getPreferredHeight() { return contentHeight(Display.width - 8) + labelHeight(); }
    public void setPreferredSize(int width, int height) {}
    public int getMinimumWidth() { return 16; }
    public int getMinimumHeight() { return Screen.TEXT_FONT.getHeight(); }
    public void notifyStateChanged() {
        if (form != null) form.itemStateChanged(this);
    }

    void changed() { if (form != null) form.itemChanged(); }

    int labelHeight() { return label == null || label.length() == 0 ? 0 : Screen.LABEL_FONT.getHeight() + 1; }

    // ---- what the form asks of an item ----
    /** Height of the part below the label for the given width. */
    abstract int contentHeight(int w);
    abstract void paintContent(Graphics g, int x, int y, int w, boolean focused);
    boolean focusable() { return commands.size() > 0; }
    /** Returns true when the key was used. */
    boolean key(int type, int keyCode, int ch) {
        if (type == 0 && keyCode == Canvas.KEY_FIRE) {
            Command c = defaultCommand != null ? defaultCommand
                      : (commands.size() > 0 ? (Command) commands.elementAt(0) : null);
            if (c != null && commandListener != null) { commandListener.commandAction(c, this); return true; }
        }
        return false;
    }
}
