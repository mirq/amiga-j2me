/*
 * amiga-j2me: MIDP 2.0 implementation for the Amiga J2ME runtime.
 * Written for this project; licensed under GPLv2 (see COPYING).
 */
package javax.microedition.lcdui;

public class ChoiceGroup extends Item implements Choice {
    private final ChoiceList list;

    public ChoiceGroup(String label, int choiceType) { this(label, choiceType, new String[0], null); }

    public ChoiceGroup(String label, int choiceType, String[] stringElements, Image[] imageElements) {
        if (choiceType != EXCLUSIVE && choiceType != MULTIPLE && choiceType != POPUP) throw new IllegalArgumentException();
        this.label = label;
        list = new ChoiceList(choiceType, stringElements, imageElements);
    }

    public int size() { return list.size(); }
    public String getString(int i) { list.check(i); return (String) list.strings.elementAt(i); }
    public Image getImage(int i) { list.check(i); return list.image(i); }
    public int append(String s, Image img) { int r = list.append(s, img); changed(); return r; }
    public void insert(int i, String s, Image img) { list.insert(i, s, img); changed(); }
    public void delete(int i) { list.delete(i); changed(); }
    public void deleteAll() { list.deleteAll(); changed(); }
    public void set(int i, String s, Image img) {
        list.check(i);
        if (s == null) throw new NullPointerException();
        list.strings.setElementAt(s, i);
        list.images.setElementAt(img == null ? (Object) "" : img, i);
        changed();
    }
    public boolean isSelected(int i) { list.check(i); return list.selected[i]; }
    public int getSelectedIndex() { return list.selectedIndex(); }
    public int getSelectedFlags(boolean[] out) { return list.flags(out); }
    public void setSelectedIndex(int i, boolean selected) { list.select(i, selected); changed(); }
    public void setSelectedFlags(boolean[] in) { list.setFlags(in); changed(); }
    public void setFitPolicy(int fitPolicy) { list.fitPolicy = fitPolicy; }
    public int getFitPolicy() { return list.fitPolicy; }
    public void setFont(int i, Font font) { list.check(i); list.fonts.setElementAt(font == null ? Screen.TEXT_FONT : font, i); changed(); }
    public Font getFont(int i) { list.check(i); return (Font) list.fonts.elementAt(i); }

    int contentHeight(int w) { return list.height(); }
    void paintContent(Graphics g, int x, int y, int w, boolean focused) { list.paint(g, x, y, w, focused); }
    boolean focusable() { return list.size() > 0; }

    boolean key(int type, int keyCode, int ch) {
        if (type == 1) return false;
        if (keyCode == Canvas.KEY_UP || keyCode == Canvas.KEY_DOWN) return list.move(keyCode);
        if (keyCode == Canvas.KEY_FIRE) {
            if (list.toggle()) notifyStateChanged();
            return true;
        }
        return false;
    }
}
