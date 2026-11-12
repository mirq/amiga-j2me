/*
 * amiga-j2me: MIDP 2.0 implementation for the Amiga J2ME runtime.
 * Written for this project; licensed under GPLv2 (see COPYING).
 */
package javax.microedition.lcdui;

import java.util.Vector;

public class TextBox extends Screen {
    private final StringBuffer text = new StringBuffer();
    private int maxSize, constraints;

    public TextBox(String title, String text, int maxSize, int constraints) {
        if (maxSize <= 0) throw new IllegalArgumentException();
        setTitle(title);
        this.maxSize = maxSize; this.constraints = constraints;
        setString(text);
    }

    public String getString() { return text.toString(); }
    public void setString(String s) {
        if (s != null && s.length() > maxSize) throw new IllegalArgumentException();
        text.setLength(0);
        if (s != null) text.append(s);
        requestRepaint();
    }
    public int getChars(char[] data) {
        int n = text.length();
        text.getChars(0, n, data, 0);
        return n;
    }
    public void setChars(char[] data, int offset, int length) { setString(data == null ? null : new String(data, offset, length)); }
    public void insert(String src, int position) {
        if (text.length() + src.length() > maxSize) throw new IllegalArgumentException();
        text.insert(position, src);
        requestRepaint();
    }
    public void insert(char[] data, int offset, int length, int position) { insert(new String(data, offset, length), position); }
    public void delete(int offset, int length) { text.delete(offset, offset + length); requestRepaint(); }
    public int getMaxSize() { return maxSize; }
    public int setMaxSize(int m) {
        if (m <= 0) throw new IllegalArgumentException();
        maxSize = m;
        if (text.length() > m) text.setLength(m);
        return m;
    }
    public int size() { return text.length(); }
    public int getCaretPosition() { return text.length(); }
    public void setConstraints(int c) { constraints = c; }
    public int getConstraints() { return constraints; }
    public void setInitialInputMode(String characterSubset) {}

    void paintContent(Graphics g, int top, int w, int h) {
        Font f = TEXT_FONT;
        Vector lines = wrap(TextField.shown(text, constraints), f, w - 8);
        if (lines.size() == 0) lines.addElement("");
        int visible = Math.max(1, h / f.getHeight());
        int first = Math.max(0, lines.size() - visible);        // the end stays in view
        g.setColor(FG); g.setFont(f);
        int y = top + 2;
        String last = "";
        for (int i = first; i < lines.size(); i++) {
            last = (String) lines.elementAt(i);
            g.drawString(last, 4, y, Graphics.TOP | Graphics.LEFT);
            y += f.getHeight();
        }
        int cx = 4 + f.stringWidth(last), cy = y - f.getHeight();
        g.drawLine(cx, cy, cx, cy + f.getHeight() - 1);
    }

    boolean contentKey(int type, int keyCode, int ch) {
        if (type == 1 || ch == 0) return false;
        if (ch == 10) {
            if ((constraints & TextField.CONSTRAINT_MASK) != TextField.ANY) return false;
            ch = '\n';
            if (text.length() < maxSize) { text.append((char) ch); return true; }
            return false;
        }
        return TextField.type(text, ch, maxSize, constraints);
    }
}
