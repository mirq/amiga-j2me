/*
 * amiga-j2me: MIDP 2.0 implementation for the Amiga J2ME runtime.
 * Written for this project; licensed under GPLv2 (see COPYING).
 */
package javax.microedition.lcdui;

public abstract class Item {
    public static final int LAYOUT_DEFAULT = 0, LAYOUT_LEFT = 1, LAYOUT_RIGHT = 2, LAYOUT_CENTER = 3,
                            LAYOUT_TOP = 0x10, LAYOUT_BOTTOM = 0x20, LAYOUT_VCENTER = 0x30,
                            LAYOUT_NEWLINE_BEFORE = 0x100, LAYOUT_NEWLINE_AFTER = 0x200,
                            LAYOUT_SHRINK = 0x400, LAYOUT_EXPAND = 0x800, LAYOUT_VSHRINK = 0x1000,
                            LAYOUT_VEXPAND = 0x2000, LAYOUT_2 = 0x4000;
    public static final int PLAIN = 0, HYPERLINK = 1, BUTTON = 2;

    String label;
    int layout;
    Item() {}
    public String getLabel() { return label; }
    public void setLabel(String l) { label = l; }
    public int getLayout() { return layout; }
    public void setLayout(int l) { layout = l; }
}
