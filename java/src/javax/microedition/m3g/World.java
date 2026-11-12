/*
 * amiga-j2me: JSR-184 (M3G 1.0/1.1) implementation for the Amiga J2ME runtime.
 * Written for this project; licensed under GPLv2 (see COPYING).
 */
package javax.microedition.m3g;

import java.util.Vector;

public class World extends Group {
    private Camera activeCamera;
    private Background background;

    public World() {}

    public void setBackground(Background b) { background = b; }
    public Background getBackground() { return background; }
    public void setActiveCamera(Camera c) {
        if (c == null) throw new NullPointerException();
        activeCamera = c;
    }
    public Camera getActiveCamera() { return activeCamera; }

    void collectReferences(Vector out) {
        super.collectReferences(out);
        add(out, activeCamera);
        add(out, background);
    }

    Object3D duplicateImpl() {
        World w = new World();
        copyGroup(w);
        w.background = background;
        // the copy's camera is the copy of ours, found by its place in the tree
        w.activeCamera = (Camera) findTwin(this, w, activeCamera);
        return w;
    }

    private static Node findTwin(Group a, Group b, Node wanted) {
        for (int i = 0; i < a.children.size(); i++) {
            Node ca = (Node) a.children.elementAt(i), cb = (Node) b.children.elementAt(i);
            if (ca == wanted) return cb;
            if (ca instanceof Group) {
                Node r = findTwin((Group) ca, (Group) cb, wanted);
                if (r != null) return r;
            }
        }
        return null;
    }
}
