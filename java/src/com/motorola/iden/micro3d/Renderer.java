/*
 * amiga-j2me: Motorola iDEN micro3d API for the Amiga J2ME runtime, a layer
 * over the Mascot Capsule v3 implementation.
 * Written for this project; licensed under GPLv2 (see COPYING).
 */
package com.motorola.iden.micro3d;

import com.mascotcapsule.micro3d.v3.Graphics3D;
import java.util.Vector;
import javax.microedition.lcdui.Graphics;

/** Collects objects with draw() and puts them on the screen, depth sorted, with paint(). */
public class Renderer {
    private final Vector objects = new Vector();
    private int[] xs = new int[16], ys = new int[16];
    private final Graphics3D g3 = new Graphics3D();

    public Renderer() {}

    public void draw(Object3D object, int x, int y) {
        if (object == null) throw new NullPointerException();
        if (object.layout == null) return;
        int n = objects.size();
        if (n == xs.length) {
            int[] a = new int[n * 2], b = new int[n * 2];
            System.arraycopy(xs, 0, a, 0, n); System.arraycopy(ys, 0, b, 0, n);
            xs = a; ys = b;
        }
        xs[n] = Math.max(-2048, Math.min(2047, x));
        ys[n] = Math.max(-2048, Math.min(2047, y));
        objects.addElement(object);
    }

    public void paint(Graphics g) {
        if (g == null) throw new NullPointerException();
        // sizes of the projections that depend on the surface: the clip is the whole canvas in paint()
        int w = g.getClipX() + g.getClipWidth(), h = g.getClipY() + g.getClipHeight();
        g3.bind(g);
        try {
            for (int i = 0; i < objects.size(); i++) {
                Object3D o = (Object3D) objects.elementAt(i);
                if (o instanceof Figure) {
                    Figure f = (Figure) o;
                    if (f.prepare()) g3.renderFigure(f.figure, xs[i], ys[i], f.layout.toV3(w, h), f.layout.effect(f.sphereTexture));
                } else {
                    ((Primitive) o).draw(g3, xs[i], ys[i], w, h);
                }
            }
            g3.flush();
        } finally {
            g3.release(g);
            objects.removeAllElements();
        }
    }

    /** Rendering into a texture is not supported: the objects are dropped and the texture comes back as it was. */
    public Texture paint(Texture texture, int color) {
        objects.removeAllElements();
        return texture == null ? new Texture() : texture;
    }
}
