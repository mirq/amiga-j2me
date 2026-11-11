/*
 * amiga-j2me: MIDP 2.0 implementation for the Amiga J2ME runtime.
 * Written for this project; licensed under GPLv2 (see COPYING).
 */
package javax.microedition.lcdui.game;

import java.util.Vector;
import javax.microedition.lcdui.Graphics;

public class LayerManager {
    private final Vector layers = new Vector();
    private int viewX, viewY, viewWidth = Integer.MAX_VALUE, viewHeight = Integer.MAX_VALUE;

    public LayerManager() {}

    public void append(Layer l) {
        if (l == null) throw new NullPointerException();
        layers.removeElement(l);
        layers.addElement(l);
    }

    public void insert(Layer l, int index) {
        if (l == null) throw new NullPointerException();
        boolean had = layers.contains(l);
        if (index < 0 || index > layers.size() || (had && index >= layers.size())) throw new IndexOutOfBoundsException();
        layers.removeElement(l);
        layers.insertElementAt(l, index);
    }

    public Layer getLayerAt(int index) {
        if (index < 0 || index >= layers.size()) throw new IndexOutOfBoundsException();
        return (Layer) layers.elementAt(index);
    }

    public int getSize() { return layers.size(); }

    public void remove(Layer l) {
        if (l == null) throw new NullPointerException();
        layers.removeElement(l);
    }

    public void setViewWindow(int x, int y, int width, int height) {
        if (width < 0 || height < 0) throw new IllegalArgumentException();
        viewX = x; viewY = y; viewWidth = width; viewHeight = height;
    }

    public void paint(Graphics g, int x, int y) {
        int cx = g.getClipX(), cy = g.getClipY(), cw = g.getClipWidth(), ch = g.getClipHeight();
        g.translate(x - viewX, y - viewY);
        g.clipRect(viewX, viewY, viewWidth, viewHeight);
        for (int i = layers.size() - 1; i >= 0; i--) {
            Layer l = (Layer) layers.elementAt(i);
            if (l.visible) l.paint(g);
        }
        g.translate(-x + viewX, -y + viewY);
        g.setClip(cx, cy, cw, ch);
    }
}
