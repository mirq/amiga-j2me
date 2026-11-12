/*
 * amiga-j2me: JSR-184 (M3G 1.0/1.1) implementation for the Amiga J2ME runtime.
 * Written for this project; licensed under GPLv2 (see COPYING).
 */
package javax.microedition.m3g;

import java.util.Vector;

public class Mesh extends Node {
    VertexBuffer vertices;
    IndexBuffer[] submeshes;
    Appearance[] appearances;

    public Mesh(VertexBuffer vertices, IndexBuffer submesh, Appearance appearance) {
        if (vertices == null || submesh == null) throw new NullPointerException();
        this.vertices = vertices;
        submeshes = new IndexBuffer[] { submesh };
        appearances = new Appearance[] { appearance };
    }

    public Mesh(VertexBuffer vertices, IndexBuffer[] submeshes, Appearance[] appearances) {
        if (vertices == null || submeshes == null) throw new NullPointerException();
        if (submeshes.length == 0 || (appearances != null && appearances.length < submeshes.length)) {
            throw new IllegalArgumentException();
        }
        this.vertices = vertices;
        this.submeshes = new IndexBuffer[submeshes.length];
        this.appearances = new Appearance[submeshes.length];
        for (int i = 0; i < submeshes.length; i++) {
            if (submeshes[i] == null) throw new NullPointerException();
            this.submeshes[i] = submeshes[i];
            if (appearances != null) this.appearances[i] = appearances[i];
        }
    }

    public void setAppearance(int index, Appearance a) { appearances[index] = a; }
    public Appearance getAppearance(int index) { return appearances[index]; }
    public IndexBuffer getIndexBuffer(int index) { return submeshes[index]; }
    public VertexBuffer getVertexBuffer() { return vertices; }
    public int getSubmeshCount() { return submeshes.length; }

    /** The vertices to draw: the deformed ones for morphing and skinned meshes. */
    VertexBuffer renderVertices() { return vertices; }

    void collectReferences(Vector out) {
        add(out, vertices);
        for (int i = 0; i < submeshes.length; i++) { add(out, submeshes[i]); add(out, appearances[i]); }
    }

    Object3D duplicateImpl() {
        Mesh m = new Mesh(vertices, submeshes, appearances);
        m.copyNode(this);
        return m;
    }
}
