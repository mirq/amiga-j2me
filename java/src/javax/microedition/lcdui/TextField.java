/*
 * amiga-j2me: MIDP 2.0 implementation for the Amiga J2ME runtime.
 * Written for this project; licensed under GPLv2 (see COPYING).
 */
package javax.microedition.lcdui;

public class TextField extends Item {
    public static final int ANY = 0, EMAILADDR = 1, NUMERIC = 2, PHONENUMBER = 3, URL = 4, DECIMAL = 5,
                            PASSWORD = 0x10000, UNEDITABLE = 0x20000, SENSITIVE = 0x40000,
                            NON_PREDICTIVE = 0x80000, INITIAL_CAPS_WORD = 0x100000,
                            INITIAL_CAPS_SENTENCE = 0x200000, CONSTRAINT_MASK = 0xFFFF;

    final StringBuffer text = new StringBuffer();
    int maxSize, constraints;

    public TextField(String label, String text, int maxSize, int constraints) {
        if (maxSize <= 0) throw new IllegalArgumentException();
        this.label = label; this.maxSize = maxSize; this.constraints = constraints;
        setString(text);
    }

    public String getString() { return text.toString(); }
    public void setString(String s) {
        if (s != null && s.length() > maxSize) throw new IllegalArgumentException();
        text.setLength(0);
        if (s != null) text.append(s);
        changed();
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
        changed();
    }
    public void insert(char[] data, int offset, int length, int position) { insert(new String(data, offset, length), position); }
    public void delete(int offset, int length) { text.delete(offset, offset + length); changed(); }
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

    /** Typing shared with TextBox: appends or deletes at the end. Returns true when the text changed. */
    static boolean type(StringBuffer sb, int ch, int maxSize, int constraints) {
        if ((constraints & UNEDITABLE) != 0) return false;
        if (ch == 8) {
            if (sb.length() == 0) return false;
            sb.setLength(sb.length() - 1);
            return true;
        }
        if (ch < 32 || sb.length() >= maxSize) return false;
        int kind = constraints & CONSTRAINT_MASK;
        if (kind == NUMERIC && !((ch >= '0' && ch <= '9') || (ch == '-' && sb.length() == 0))) return false;
        if (kind == DECIMAL && !((ch >= '0' && ch <= '9') || ch == '.' || (ch == '-' && sb.length() == 0))) return false;
        if (kind == PHONENUMBER && !((ch >= '0' && ch <= '9') || ch == '+' || ch == '*' || ch == '#')) return false;
        sb.append((char) ch);
        return true;
    }

    static String shown(StringBuffer sb, int constraints) {
        if ((constraints & PASSWORD) == 0) return sb.toString();
        StringBuffer stars = new StringBuffer();
        for (int i = 0; i < sb.length(); i++) stars.append('*');
        return stars.toString();
    }

    int contentHeight(int w) { return Screen.TEXT_FONT.getHeight() + 4; }

    void paintContent(Graphics g, int x, int y, int w, boolean focused) {
        Font f = Screen.TEXT_FONT;
        g.setColor(focused ? Screen.HI_BG : 0x808080);
        g.drawRect(x, y, w - 1, f.getHeight() + 3);
        String s = shown(text, constraints);
        // keep the end (where the caret is) in view
        int start = 0;
        while (f.substringWidth(s, start, s.length() - start) > w - 8) start++;
        g.setColor(Screen.FG); g.setFont(f);
        g.drawSubstring(s, start, s.length() - start, x + 3, y + 2, Graphics.TOP | Graphics.LEFT);
        if (focused) {
            int cx = x + 3 + f.substringWidth(s, start, s.length() - start);
            g.drawLine(cx, y + 2, cx, y + 1 + f.getHeight());
        }
    }

    boolean focusable() { return true; }

    boolean key(int type, int keyCode, int ch) {
        if (type != 1 && ch != 0 && ch != 10) {
            if (type(text, ch, maxSize, constraints)) notifyStateChanged();
            return true;
        }
        return super.key(type, keyCode, ch);
    }
}
