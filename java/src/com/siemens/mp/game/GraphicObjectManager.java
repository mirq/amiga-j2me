/*
 * amiga-j2me: MIDP 2.0 implementation for the Amiga J2ME runtime.
 * Written for this project; licensed under GPLv2 (see COPYING).
 */
package com.siemens.mp.game;

import java.util.Vector;
import javax.microedition.lcdui.Graphics;
import javax.microedition.lcdui.Image;

/** An ordered list of sprites and tiled backgrounds, painted back to front. */
public class GraphicObjectManager extends com.siemens.mp.misc.NativeMem {
    private final Vector objects = new Vector();

    public GraphicObjectManager() {}

    /** Mirrors the bits of every byte (texture data comes in the other bit order). */
    public static byte[] createTextureBits(int width, int height, byte[] texture) {
        byte[] out = new byte[texture.length];
        for (int i = 0; i < texture.length; i++) {
            int v = texture[i] & 0xFF, r = 0;
            for (int b = 0; b < 8; b++) if ((v & (1 << b)) != 0) r |= 0x80 >> b;
            out[i] = (byte) r;
        }
        return out;
    }

    public void addObject(GraphicObject o) { objects.addElement(o); }
    public void insertObject(GraphicObject o, int position) { objects.insertElementAt(o, position); }
    public void deleteObject(GraphicObject o) { objects.removeElement(o); }
    public void deleteObject(int position) { objects.removeElementAt(position); }
    public GraphicObject getObjectAt(int index) { return (GraphicObject) objects.elementAt(index); }
    public int getObjectPosition(GraphicObject o) { return objects.indexOf(o); }

    public void paint(Graphics g, int x, int y) {
        g.translate(x, y);
        for (int i = 0; i < objects.size(); i++) ((GraphicObject) objects.elementAt(i)).draw(g);
        g.translate(-x, -y);
    }

    public void paint(Image image, int x, int y) { paint(image.getGraphics(), x, y); }
    public void paint(ExtendedImage image, int x, int y) { paint(image.getImage(), x, y); }
}
