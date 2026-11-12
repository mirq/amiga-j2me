/*
 * amiga-j2me: JSR-184 (M3G 1.0/1.1) implementation for the Amiga J2ME runtime.
 * Written for this project; licensed under GPLv2 (see COPYING).
 */
package javax.microedition.m3g;

public abstract class IndexBuffer extends Object3D {
    int[] triangles;        // a triangle list: 3 indices per triangle

    IndexBuffer() {}

    public int getIndexCount() { return triangles.length; }

    public void getIndices(int[] indices) {
        if (indices.length < triangles.length) throw new IllegalArgumentException();
        System.arraycopy(triangles, 0, indices, 0, triangles.length);
    }
}
