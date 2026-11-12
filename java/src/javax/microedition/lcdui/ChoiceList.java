/*
 * amiga-j2me: MIDP 2.0 implementation for the Amiga J2ME runtime.
 * Written for this project; licensed under GPLv2 (see COPYING).
 */
package javax.microedition.lcdui;

import java.util.Vector;

/** The elements, selection and drawing shared by List and ChoiceGroup. Not MIDP API. */
final class ChoiceList {
    final int type;
    final Vector strings = new Vector(), images = new Vector(), fonts = new Vector();
    boolean[] selected = new boolean[8];
    int cursor;         // highlighted element
    int fitPolicy;

    ChoiceList(int type, String[] stringElements, Image[] imageElements) {
        this.type = type;
        if (stringElements == null) throw new NullPointerException();
        if (imageElements != null && imageElements.length != stringElements.length) throw new IllegalArgumentException();
        for (int i = 0; i < stringElements.length; i++) {
            append(stringElements[i], imageElements == null ? null : imageElements[i]);
        }
    }

    int size() { return strings.size(); }

    void check(int i) { if (i < 0 || i >= size()) throw new IndexOutOfBoundsException(); }

    int append(String s, Image img) {
        insert(size(), s, img);
        return size() - 1;
    }

    void insert(int at, String s, Image img) {
        if (s == null) throw new NullPointerException();
        if (at < 0 || at > size()) throw new IndexOutOfBoundsException();
        int n = size();
        if (n + 1 > selected.length) {
            boolean[] ns = new boolean[selected.length * 2];
            System.arraycopy(selected, 0, ns, 0, n);
            selected = ns;
        }
        System.arraycopy(selected, at, selected, at + 1, n - at);
        selected[at] = false;
        strings.insertElementAt(s, at); images.insertElementAt(img == null ? (Object) "" : img, at);
        fonts.insertElementAt(Screen.TEXT_FONT, at);
        if (type != Choice.MULTIPLE && n == 0) selected[0] = true;
        if (cursor >= at && n > 0) cursor++;
    }

    void delete(int i) {
        check(i);
        int n = size();
        boolean wasSel = selected[i];
        System.arraycopy(selected, i + 1, selected, i, n - i - 1);
        strings.removeElementAt(i); images.removeElementAt(i); fonts.removeElementAt(i);
        if (cursor >= size()) cursor = Math.max(0, size() - 1);
        if (type != Choice.MULTIPLE && wasSel && size() > 0) selected[Math.min(i, size() - 1)] = true;
    }

    void deleteAll() {
        strings.removeAllElements(); images.removeAllElements(); fonts.removeAllElements();
        cursor = 0;
    }

    Image image(int i) {
        Object o = images.elementAt(i);
        return o instanceof Image ? (Image) o : null;
    }

    int selectedIndex() {
        if (type == Choice.MULTIPLE) return -1;
        for (int i = 0; i < size(); i++) if (selected[i]) return i;
        return -1;
    }

    void select(int i, boolean sel) {
        check(i);
        if (type == Choice.MULTIPLE) { selected[i] = sel; return; }
        if (!sel) return;
        for (int k = 0; k < size(); k++) selected[k] = k == i;
        cursor = i;
    }

    int flags(boolean[] out) {
        if (out.length < size()) throw new IllegalArgumentException();
        int n = 0;
        for (int i = 0; i < out.length; i++) {
            out[i] = i < size() && selected[i];
            if (out[i]) n++;
        }
        return n;
    }

    void setFlags(boolean[] in) {
        if (in.length < size()) throw new IllegalArgumentException();
        if (type == Choice.MULTIPLE) {
            for (int i = 0; i < size(); i++) selected[i] = in[i];
        } else {
            int first = 0;
            for (int i = 0; i < size(); i++) if (in[i]) { first = i; break; }
            if (size() > 0) select(first, true);
        }
    }

    int rowHeight(int i) {
        int h = ((Font) fonts.elementAt(i)).getHeight() + 2;
        Image img = image(i);
        return img != null && img.getHeight() + 2 > h ? img.getHeight() + 2 : h;
    }

    int height() {
        int h = 0;
        for (int i = 0; i < size(); i++) h += rowHeight(i);
        return h;
    }

    int rowY(int i) {
        int y = 0;
        for (int k = 0; k < i; k++) y += rowHeight(k);
        return y;
    }

    void paint(Graphics g, int x, int y, int w, boolean focused) {
        for (int i = 0; i < size(); i++) {
            int rh = rowHeight(i), tx = x + 3;
            boolean hi = focused && i == cursor;
            if (y + rh > g.getClipY() && y < g.getClipY() + g.getClipHeight()) {
                if (hi) { g.setColor(Screen.HI_BG); g.fillRect(x, y, w, rh); }
                g.setColor(hi ? Screen.HI_FG : Screen.FG);
                if (type == Choice.EXCLUSIVE || type == Choice.MULTIPLE || type == Choice.POPUP) {
                    int by = y + (rh - 9) / 2;
                    if (type == Choice.MULTIPLE) {
                        g.drawRect(tx, by, 8, 8);
                        if (selected[i]) g.fillRect(tx + 2, by + 2, 5, 5);
                    } else {
                        g.drawArc(tx, by, 8, 8, 0, 360);
                        if (selected[i]) g.fillArc(tx + 2, by + 2, 5, 5, 0, 360);
                    }
                    tx += 13;
                }
                Image img = image(i);
                if (img != null) { g.drawImage(img, tx, y + 1, Graphics.TOP | Graphics.LEFT); tx += img.getWidth() + 3; }
                Font f = (Font) fonts.elementAt(i);
                g.setFont(f);
                g.drawString((String) strings.elementAt(i), tx, y + (rh - f.getHeight()) / 2, Graphics.TOP | Graphics.LEFT);
            }
            y += rh;
        }
    }

    /** Up/down within the list. Returns false at either end (the form moves on). */
    boolean move(int keyCode) {
        if (keyCode == Canvas.KEY_UP && cursor > 0) { cursor--; return true; }
        if (keyCode == Canvas.KEY_DOWN && cursor < size() - 1) { cursor++; return true; }
        return false;
    }

    /** Fire on the highlighted element. Returns true when the selection changed. */
    boolean toggle() {
        if (size() == 0) return false;
        if (type == Choice.MULTIPLE) { selected[cursor] = !selected[cursor]; return true; }
        if (selected[cursor]) return false;
        select(cursor, true);
        return true;
    }
}
