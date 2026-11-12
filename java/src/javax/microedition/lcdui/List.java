/*
 * amiga-j2me: MIDP 2.0 implementation for the Amiga J2ME runtime.
 * Written for this project; licensed under GPLv2 (see COPYING).
 */
package javax.microedition.lcdui;

public class List extends Screen implements Choice {
    public static final Command SELECT_COMMAND = new Command("Select", Command.SCREEN, 0);

    private final ChoiceList list;
    private Command selectCommand = SELECT_COMMAND;
    private int scroll;

    public List(String title, int listType) { this(title, listType, new String[0], null); }

    public List(String title, int listType, String[] stringElements, Image[] imageElements) {
        if (listType != EXCLUSIVE && listType != MULTIPLE && listType != IMPLICIT) throw new IllegalArgumentException();
        setTitle(title);
        list = new ChoiceList(listType, stringElements, imageElements);
    }

    public int size() { return list.size(); }
    public String getString(int i) { list.check(i); return (String) list.strings.elementAt(i); }
    public Image getImage(int i) { list.check(i); return list.image(i); }
    public int append(String s, Image img) { int r = list.append(s, img); requestRepaint(); return r; }
    public void insert(int i, String s, Image img) { list.insert(i, s, img); requestRepaint(); }
    public void delete(int i) { list.delete(i); requestRepaint(); }
    public void deleteAll() { list.deleteAll(); scroll = 0; requestRepaint(); }
    public void set(int i, String s, Image img) {
        list.check(i);
        if (s == null) throw new NullPointerException();
        list.strings.setElementAt(s, i);
        list.images.setElementAt(img == null ? (Object) "" : img, i);
        requestRepaint();
    }
    public boolean isSelected(int i) { list.check(i); return list.type == IMPLICIT ? i == list.cursor : list.selected[i]; }
    public int getSelectedIndex() {
        if (list.type == IMPLICIT) return list.size() == 0 ? -1 : list.cursor;
        return list.selectedIndex();
    }
    public int getSelectedFlags(boolean[] out) {
        if (list.type != IMPLICIT) return list.flags(out);
        if (out.length < list.size()) throw new IllegalArgumentException();
        for (int i = 0; i < out.length; i++) out[i] = i == list.cursor && i < list.size();
        return list.size() > 0 ? 1 : 0;
    }
    public void setSelectedIndex(int i, boolean selected) {
        list.check(i);
        if (list.type == IMPLICIT) { if (selected) list.cursor = i; }
        else list.select(i, selected);
        requestRepaint();
    }
    public void setSelectedFlags(boolean[] in) {
        if (list.type == IMPLICIT) {
            for (int i = 0; i < list.size() && i < in.length; i++) if (in[i]) { list.cursor = i; break; }
        } else list.setFlags(in);
        requestRepaint();
    }
    public void setFitPolicy(int fitPolicy) { list.fitPolicy = fitPolicy; }
    public int getFitPolicy() { return list.fitPolicy; }
    public void setFont(int i, Font font) { list.check(i); list.fonts.setElementAt(font == null ? TEXT_FONT : font, i); requestRepaint(); }
    public Font getFont(int i) { list.check(i); return (Font) list.fonts.elementAt(i); }
    public void setSelectCommand(Command command) { selectCommand = command; }
    public void removeCommand(Command cmd) {
        if (cmd == selectCommand) selectCommand = null;
        super.removeCommand(cmd);
    }

    Command implicitCommand() { return list.type == IMPLICIT && list.size() > 0 ? selectCommand : null; }

    void paintContent(Graphics g, int top, int w, int h) {
        // keep the highlighted row in view
        if (list.size() > 0) {
            int ry = list.rowY(list.cursor), rh = list.rowHeight(list.cursor);
            if (ry < scroll) scroll = ry;
            else if (ry + rh > scroll + h) scroll = ry + rh - h;
        }
        list.paint(g, 0, top - scroll, w, true);
    }

    boolean contentKey(int type, int keyCode, int ch) {
        if (type == 1) return false;
        if (keyCode == Canvas.KEY_UP || keyCode == Canvas.KEY_DOWN) {
            if (!list.move(keyCode) && list.size() > 0) {      // wrap around
                list.cursor = keyCode == Canvas.KEY_UP ? list.size() - 1 : 0;
            }
            return true;
        }
        if (keyCode == Canvas.KEY_FIRE && type == 0) {
            if (list.type == IMPLICIT) fireCommand(selectCommand);
            else list.toggle();
            return true;
        }
        return false;
    }

    void pointerEvent(int type, int x, int y) {
        super.pointerEvent(type, x, y);
        if (type != 0 || y < contentY() || y >= contentY() + contentHeight()) return;
        int ry = contentY() - scroll;
        for (int i = 0; i < list.size(); i++) {
            int rh = list.rowHeight(i);
            if (y >= ry && y < ry + rh) {
                list.cursor = i;
                if (list.type == IMPLICIT) fireCommand(selectCommand);
                else list.toggle();
                requestRepaint();
                return;
            }
            ry += rh;
        }
    }
}
