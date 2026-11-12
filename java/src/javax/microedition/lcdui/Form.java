/*
 * amiga-j2me: MIDP 2.0 implementation for the Amiga J2ME runtime.
 * Written for this project; licensed under GPLv2 (see COPYING).
 */
package javax.microedition.lcdui;

import java.util.Vector;

/** Items one below the other, full width; Up/Down move the focus and scroll. */
public class Form extends Screen {
    private final Vector items = new Vector();
    private ItemStateListener stateListener;
    private int focus = -1;     // index of the focused item, -1 = none
    private int scroll;         // pixels scrolled
    private int totalHeight;
    private static final int MARGIN = 4, GAP = 3;

    public Form(String title) { setTitle(title); }

    public Form(String title, Item[] items) {
        setTitle(title);
        if (items != null) {
            for (int i = 0; i < items.length; i++) append(items[i]);
        }
    }

    private void own(Item item) {
        if (item == null) throw new NullPointerException();
        if (item.form != null) throw new IllegalStateException();
        item.form = this;
    }

    public int append(Item item) {
        own(item);
        items.addElement(item);
        itemChanged();
        return items.size() - 1;
    }
    public int append(String str) {
        if (str == null) throw new NullPointerException();
        return append(new StringItem(null, str));
    }
    public int append(Image img) {
        if (img == null) throw new NullPointerException();
        return append(new ImageItem(null, img, ImageItem.LAYOUT_DEFAULT, null));
    }
    public void insert(int itemNum, Item item) {
        own(item);
        items.insertElementAt(item, itemNum);
        itemChanged();
    }
    public void delete(int itemNum) {
        ((Item) items.elementAt(itemNum)).form = null;
        items.removeElementAt(itemNum);
        if (focus >= items.size()) focus = -1;
        itemChanged();
    }
    public void deleteAll() {
        for (int i = 0; i < items.size(); i++) ((Item) items.elementAt(i)).form = null;
        items.removeAllElements();
        focus = -1; scroll = 0;
        itemChanged();
    }
    public void set(int itemNum, Item item) {
        own(item);
        ((Item) items.elementAt(itemNum)).form = null;
        items.setElementAt(item, itemNum);
        itemChanged();
    }
    public Item get(int itemNum) { return (Item) items.elementAt(itemNum); }
    public int size() { return items.size(); }
    public void setItemStateListener(ItemStateListener l) { stateListener = l; }
    public int getWidth() { return Display.width - 2 * MARGIN; }
    public int getHeight() { return contentHeight(); }

    void itemChanged() { requestRepaint(); }
    void itemStateChanged(Item item) {
        ItemStateListener l = stateListener;
        if (l != null) l.itemStateChanged(item);
        requestRepaint();
    }

    private void layout() {
        int w = Display.width - 2 * MARGIN, y = GAP;
        for (int i = 0; i < items.size(); i++) {
            Item it = (Item) items.elementAt(i);
            it.y = y;
            it.height = it.labelHeight() + it.contentHeight(w);
            y += it.height + GAP;
        }
        totalHeight = y;
        if (focus < 0) {        // focus the first item that takes it
            for (int i = 0; i < items.size(); i++) {
                if (((Item) items.elementAt(i)).focusable()) { focus = i; break; }
            }
        }
    }

    void paintContent(Graphics g, int top, int w, int h) {
        layout();
        if (scroll > totalHeight - h) scroll = Math.max(0, totalHeight - h);
        int cw = w - 2 * MARGIN;
        for (int i = 0; i < items.size(); i++) {
            Item it = (Item) items.elementAt(i);
            int y = top + it.y - scroll;
            if (y + it.height < top || y > top + h) continue;
            int lh = it.labelHeight();
            if (lh > 0) {
                g.setColor(0x404040); g.setFont(LABEL_FONT);
                g.drawString(it.label, MARGIN, y, Graphics.TOP | Graphics.LEFT);
            }
            g.setColor(FG); g.setFont(TEXT_FONT);
            it.paintContent(g, MARGIN, y + lh, cw, i == focus);
        }
        if (totalHeight > h) {      // scroll bar
            g.setColor(0xC0C0C0); g.fillRect(w - 2, top, 2, h);
            g.setColor(BAR); g.fillRect(w - 2, top + scroll * h / totalHeight, 2, Math.max(4, h * h / totalHeight));
        }
    }

    private void reveal(Item it) {
        int h = contentHeight();
        if (it.y - GAP < scroll) scroll = it.y - GAP;
        else if (it.y + it.height + GAP > scroll + h) scroll = it.y + it.height + GAP - h;
        if (scroll < 0) scroll = 0;
    }

    boolean contentKey(int type, int keyCode, int ch) {
        layout();
        Item cur = focus >= 0 && focus < items.size() ? (Item) items.elementAt(focus) : null;
        if (cur != null && cur.key(type, keyCode, ch)) return true;
        if (type == 1) return false;
        if (keyCode == Canvas.KEY_DOWN || keyCode == Canvas.KEY_UP) {
            int step = keyCode == Canvas.KEY_DOWN ? 1 : -1;
            int h = contentHeight();
            // next focusable item that is on, or one step from, the visible part
            for (int i = focus + step; i >= 0 && i < items.size(); i += step) {
                Item it = (Item) items.elementAt(i);
                if (!it.focusable()) continue;
                if (it.y > scroll + h + h / 2 || it.y + it.height < scroll - h / 2) break;
                focus = i;
                reveal(it);
                return true;
            }
            int max = Math.max(0, totalHeight - h);
            int ns = Math.max(0, Math.min(max, scroll + step * 3 * TEXT_FONT.getHeight()));
            if (ns != scroll) { scroll = ns; return true; }
        }
        return false;
    }

    Command implicitCommand() { return null; }
}
