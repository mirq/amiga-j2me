/*
 * amiga-j2me: JSR-184 (M3G 1.0/1.1) implementation for the Amiga J2ME runtime.
 * Written for this project; licensed under GPLv2 (see COPYING).
 */
package javax.microedition.m3g;

import java.util.Vector;

public class Group extends Node {
    final Vector children = new Vector();

    public Group() {}

    public void addChild(Node child) {
        if (child == null) throw new NullPointerException();
        if (child == this || child instanceof World || (child.parent != null && child.parent != this)) {
            throw new IllegalArgumentException();
        }
        if (child.parent == null) {
            child.parent = this;
            children.addElement(child);
        }
    }

    public void removeChild(Node child) {
        if (child != null && children.removeElement(child)) child.parent = null;
    }

    public int getChildCount() { return children.size(); }
    public Node getChild(int index) { return (Node) children.elementAt(index); }

    public boolean pick(int scope, float ox, float oy, float oz, float dx, float dy, float dz, RayIntersection ri) {
        return false;       // picking is not implemented
    }

    public boolean pick(int scope, float x, float y, Camera camera, RayIntersection ri) {
        return false;
    }

    void collectReferences(Vector out) {
        for (int i = 0; i < children.size(); i++) add(out, (Object3D) children.elementAt(i));
    }

    /** Nodes are copied with their whole subtree. */
    Object3D duplicateImpl() {
        Group g = new Group();
        copyGroup(g);
        return g;
    }

    void copyGroup(Group to) {
        to.copyNode(this);
        for (int i = 0; i < children.size(); i++) {
            Node c = (Node) ((Node) children.elementAt(i)).duplicateImpl();
            c.parent = to;
            to.children.addElement(c);
        }
    }
}
