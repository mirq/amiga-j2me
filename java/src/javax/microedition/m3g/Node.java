/*
 * amiga-j2me: JSR-184 (M3G 1.0/1.1) implementation for the Amiga J2ME runtime.
 * Written for this project; licensed under GPLv2 (see COPYING).
 */
package javax.microedition.m3g;

public abstract class Node extends Transformable {
    public static final int NONE = 144, ORIGIN = 145, X_AXIS = 146, Y_AXIS = 147, Z_AXIS = 148;

    Node parent;
    boolean renderingEnable = true, pickingEnable = true;
    float alphaFactor = 1;
    int scope = -1;

    Node() {}

    public void setRenderingEnable(boolean enable) { renderingEnable = enable; }
    public void setPickingEnable(boolean enable) { pickingEnable = enable; }
    public void setScope(int scope) { this.scope = scope; }
    public void setAlphaFactor(float f) {
        if (f < 0 || f > 1) throw new IllegalArgumentException();
        alphaFactor = f;
    }
    public boolean isRenderingEnabled() { return renderingEnable; }
    public boolean isPickingEnabled() { return pickingEnable; }
    public int getScope() { return scope; }
    public float getAlphaFactor() { return alphaFactor; }
    public Node getParent() { return parent; }
    public void setAlignment(Node zRef, int zTarget, Node yRef, int yTarget) {}
    public final void align(Node reference) {}
    public Node getAlignmentReference(int axis) { return null; }
    public int getAlignmentTarget(int axis) { return NONE; }

    /** node -> root of its tree */
    void toRoot(Transform out) {
        Transform t = new Transform();
        out.setIdentity();
        for (Node n = this; n != null; n = n.parent) {
            n.getCompositeTransform(t);
            t.postMultiply(out);
            out.set(t);
        }
    }

    public boolean getTransformTo(Node target, Transform transform) {
        if (target == null || transform == null) throw new NullPointerException();
        Node rootA = this, rootB = target;
        while (rootA.parent != null) rootA = rootA.parent;
        while (rootB.parent != null) rootB = rootB.parent;
        if (rootA != rootB) return false;
        Transform a = new Transform(), b = new Transform();
        toRoot(a);
        target.toRoot(b);
        try {
            b.invert();
        } catch (ArithmeticException e) {
            return false;
        }
        b.postMultiply(a);
        transform.set(b);
        return true;
    }

    void setProperty(int property, float[] v) {
        if (property == AnimationTrack.ALPHA) alphaFactor = M3GMath.clamp(v[0], 0, 1);
        else if (property == AnimationTrack.VISIBILITY) renderingEnable = v[0] >= 0.5f;
        else if (property == AnimationTrack.PICKABILITY) pickingEnable = v[0] >= 0.5f;
        else super.setProperty(property, v);
    }

    void copyNode(Node from) {
        copyTransformable(from);
        renderingEnable = from.renderingEnable; pickingEnable = from.pickingEnable;
        alphaFactor = from.alphaFactor; scope = from.scope;
    }
}
