/*
 * amiga-j2me: JSR-184 (M3G 1.0/1.1) implementation for the Amiga J2ME runtime.
 * Written for this project; licensed under GPLv2 (see COPYING).
 */
package javax.microedition.m3g;

import java.util.Vector;

/** Drawn in its rest pose: the skeleton is kept (and animated) but the vertices do not follow it. */
public class SkinnedMesh extends Mesh {
    private final Group skeleton;

    public SkinnedMesh(VertexBuffer vertices, IndexBuffer submesh, Appearance appearance, Group skeleton) {
        super(vertices, submesh, appearance);
        if (skeleton == null) throw new NullPointerException();
        if (skeleton.parent != null || skeleton instanceof World) throw new IllegalArgumentException();
        this.skeleton = skeleton;
        skeleton.parent = this;
    }

    public SkinnedMesh(VertexBuffer vertices, IndexBuffer[] submeshes, Appearance[] appearances, Group skeleton) {
        super(vertices, submeshes, appearances);
        if (skeleton == null) throw new NullPointerException();
        if (skeleton.parent != null || skeleton instanceof World) throw new IllegalArgumentException();
        this.skeleton = skeleton;
        skeleton.parent = this;
    }

    public Group getSkeleton() { return skeleton; }
    public void addTransform(Node bone, int weight, int firstVertex, int numVertices) {
        if (bone == null) throw new NullPointerException();
        if (weight <= 0 || numVertices <= 0) throw new IllegalArgumentException();
    }
    public void getBoneTransform(Node bone, Transform transform) { transform.setIdentity(); }
    public int getBoneVertices(Node bone, int[] indices, float[] weights) { return 0; }

    void collectReferences(Vector out) {
        super.collectReferences(out);
        add(out, skeleton);
    }
}
