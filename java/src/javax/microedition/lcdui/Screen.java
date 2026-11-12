/*
 * amiga-j2me: MIDP 2.0 implementation for the Amiga J2ME runtime.
 * Written for this project; licensed under GPLv2 (see COPYING).
 */
package javax.microedition.lcdui;

import java.util.Vector;

/** Common look of the high-level screens: title bar, content, soft key bar. */
public abstract class Screen extends Displayable {
    static final Font TITLE_FONT = Font.getFont(Font.FACE_SYSTEM, Font.STYLE_BOLD, Font.SIZE_MEDIUM);
    static final Font TEXT_FONT = Font.getFont(Font.FACE_SYSTEM, Font.STYLE_PLAIN, Font.SIZE_MEDIUM);
    static final Font LABEL_FONT = Font.getFont(Font.FACE_SYSTEM, Font.STYLE_BOLD, Font.SIZE_SMALL);
    static final int BG = 0xFFFFFF, FG = 0x000000, HI_BG = 0x2040A0, HI_FG = 0xFFFFFF, BAR = 0x303848;

    Screen() {}

    int titleHeight() { return getTitle() == null ? 0 : TITLE_FONT.getHeight() + 4; }
    /** The content area: below the title, above the soft key bar. */
    int contentY() { return titleHeight(); }
    int contentHeight() { return Display.height - titleHeight() - Display.SOFT_BAR; }

    abstract void paintContent(Graphics g, int y, int w, int h);
    /** Returns true when the key was used. */
    boolean contentKey(int type, int keyCode, int ch) { return false; }

    void paintScreen(Graphics g) {
        int w = Display.width, th = titleHeight();
        g.reset(0, 0, w, Display.height);
        g.setColor(BG);
        g.fillRect(0, 0, w, Display.height);
        if (th > 0) {
            g.setColor(BAR); g.fillRect(0, 0, w, th);
            g.setColor(0xFFFFFF); g.setFont(TITLE_FONT);
            g.drawString(getTitle(), w / 2, 2, Graphics.TOP | Graphics.HCENTER);
        }
        g.setClip(0, contentY(), w, contentHeight());
        g.setColor(FG); g.setFont(TEXT_FONT);
        paintContent(g, contentY(), w, contentHeight());
        Display.instance.paintSoftBar(g, this);
    }

    void keyEvent(int type, int keyCode, int ch) {
        if (softKey(type, keyCode)) return;
        if (contentKey(type, keyCode, ch)) requestRepaint();
    }

    void pointerEvent(int type, int x, int y) {
        // a tap on the soft key bar presses that soft key
        if (type == 0 && y >= Display.height - Display.SOFT_BAR) {
            softKey(0, x < Display.width / 2 ? Canvas.KEY_SOFT_LEFT : Canvas.KEY_SOFT_RIGHT);
        }
    }

    /** Word wrap for the given width; '\n' breaks lines. */
    static Vector wrap(String text, Font f, int width) {
        Vector lines = new Vector();
        if (text == null) return lines;
        int n = text.length(), start = 0;
        while (start <= n) {
            int nl = text.indexOf('\n', start);
            if (nl < 0) nl = n;
            int a = start;
            if (a == nl) lines.addElement("");
            while (a < nl) {
                int end = nl;
                while (end > a + 1 && f.substringWidth(text, a, end - a) > width) {
                    int sp = text.lastIndexOf(' ', end - 1);
                    end = sp > a ? sp : end - 1;
                }
                lines.addElement(text.substring(a, end));
                a = end;
                while (a < nl && text.charAt(a) == ' ') a++;
            }
            start = nl + 1;
        }
        return lines;
    }
}
