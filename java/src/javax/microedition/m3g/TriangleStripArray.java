/*
 * amiga-j2me: JSR-184 (M3G 1.0/1.1) implementation for the Amiga J2ME runtime.
 * Written for this project; licensed under GPLv2 (see COPYING).
 */
package javax.microedition.m3g;

public class TriangleStripArray extends IndexBuffer {
    public TriangleStripArray(int firstIndex, int[] stripLengths) {
        if (firstIndex < 0) throw new IndexOutOfBoundsException();
        int total = 0;
        for (int i = 0; i < stripLengths.length; i++) total += stripLengths[i];
        int[] indices = new int[total];
        for (int i = 0; i < total; i++) indices[i] = firstIndex + i;
        build(indices, stripLengths);
    }

    public TriangleStripArray(int[] indices, int[] stripLengths) {
        build(indices, stripLengths);
    }

    private TriangleStripArray() {}

    /** Strips to a triangle list; every second triangle is turned so all face the same way. */
    private void build(int[] indices, int[] stripLengths) {
        if (stripLengths.length == 0) throw new IllegalArgumentException();
        int total = 0, tris = 0;
        for (int i = 0; i < stripLengths.length; i++) {
            if (stripLengths[i] < 3) throw new IllegalArgumentException();
            total += stripLengths[i];
            tris += stripLengths[i] - 2;
        }
        if (indices.length < total) throw new IllegalArgumentException();
        triangles = new int[tris * 3];
        int pos = 0, out = 0;
        for (int s = 0; s < stripLengths.length; s++) {
            for (int i = 0; i + 2 < stripLengths[s]; i++) {
                int a = indices[pos + i], b = indices[pos + i + 1], c = indices[pos + i + 2];
                if (a < 0 || b < 0 || c < 0 || a > 65535 || b > 65535 || c > 65535) throw new IndexOutOfBoundsException();
                if ((i & 1) == 0) { triangles[out++] = a; triangles[out++] = b; triangles[out++] = c; }
                else { triangles[out++] = b; triangles[out++] = a; triangles[out++] = c; }
            }
            pos += stripLengths[s];
        }
    }

    Object3D duplicateImpl() {
        TriangleStripArray t = new TriangleStripArray();
        t.copyBase(this);
        t.triangles = triangles;
        return t;
    }
}
