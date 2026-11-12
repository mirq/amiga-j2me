/*
 * amiga-j2me: MIDP 2.0 implementation for the Amiga J2ME runtime.
 * Written for this project; licensed under GPLv2 (see COPYING).
 */
package javax.microedition.lcdui;

import java.util.Vector;

public class StringItem extends Item {
    private String text;
    private final int mode;
    private Font font;

    public StringItem(String label, String text) { this(label, text, PLAIN); }

    public StringItem(String label, String text, int appearanceMode) {
        if (appearanceMode < PLAIN || appearanceMode > BUTTON) throw new IllegalArgumentException();
        this.label = label; this.text = text; this.mode = appearanceMode;
    }

    public String getText() { return text; }
    public void setText(String t) { text = t; changed(); }
    public int getAppearanceMode() { return mode; }
    public void setFont(Font f) { font = f; changed(); }
    public Font getFont() { return font == null ? Screen.TEXT_FONT : font; }

    int contentHeight(int w) {
        if (text == null || text.length() == 0) return 0;
        return Screen.wrap(text, getFont(), w).size() * getFont().getHeight() + (mode == BUTTON ? 4 : 0);
    }

    void paintContent(Graphics g, int x, int y, int w, boolean focused) {
        if (text == null) return;
        Font f = getFont();
        Vector lines = Screen.wrap(text, f, w);
        int h = lines.size() * f.getHeight();
        if (mode == BUTTON) {
            g.setColor(focused ? Screen.HI_BG : 0xD0D0D0);
            g.fillRoundRect(x, y, w, h + 4, 6, 6);
            y += 2;
        } else if (focused) {
            g.setColor(Screen.HI_BG);
            g.fillRect(x - 2, y, w + 4, h);
        }
        g.setColor(focused ? Screen.HI_FG : (mode == HYPERLINK ? 0x0000C0 : Screen.FG));
        g.setFont(f);
        for (int i = 0; i < lines.size(); i++) {
            String s = (String) lines.elementAt(i);
            if (mode == BUTTON) g.drawString(s, x + w / 2, y + i * f.getHeight(), Graphics.TOP | Graphics.HCENTER);
            else g.drawString(s, x, y + i * f.getHeight(), Graphics.TOP | Graphics.LEFT);
        }
    }
}
