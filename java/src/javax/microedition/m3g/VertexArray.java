/*
 * amiga-j2me: JSR-184 (M3G 1.0/1.1) implementation for the Amiga J2ME runtime.
 * Written for this project; licensed under GPLv2 (see COPYING).
 */
package javax.microedition.m3g;

public class VertexArray extends Object3D {
    final int count, comps, size;       // vertices, components per vertex, bytes per component
    byte[] bytes;                       // size 1
    short[] shorts;                     // size 2

    public VertexArray(int numVertices, int numComponents, int componentSize) {
        if (numVertices < 1 || numVertices > 65535 || numComponents < 2 || numComponents > 4
            || componentSize < 1 || componentSize > 2) {
            throw new IllegalArgumentException();
        }
        count = numVertices; comps = numComponents; size = componentSize;
        if (size == 1) bytes = new byte[count * comps];
        else shorts = new short[count * comps];
    }

    public void set(int firstVertex, int numVertices, short[] values) {
        if (size != 2) throw new IllegalStateException();
        if (firstVertex < 0 || numVertices < 0 || firstVertex + numVertices > count) throw new IndexOutOfBoundsException();
        if (values.length < numVertices * comps) throw new IllegalArgumentException();
        System.arraycopy(values, 0, shorts, firstVertex * comps, numVertices * comps);
    }

    public void set(int firstVertex, int numVertices, byte[] values) {
        if (size != 1) throw new IllegalStateException();
        if (firstVertex < 0 || numVertices < 0 || firstVertex + numVertices > count) throw new IndexOutOfBoundsException();
        if (values.length < numVertices * comps) throw new IllegalArgumentException();
        System.arraycopy(values, 0, bytes, firstVertex * comps, numVertices * comps);
    }

    public void get(int firstVertex, int numVertices, short[] values) {
        if (size != 2) throw new IllegalStateException();
        System.arraycopy(shorts, firstVertex * comps, values, 0, numVertices * comps);
    }

    public void get(int firstVertex, int numVertices, byte[] values) {
        if (size != 1) throw new IllegalStateException();
        System.arraycopy(bytes, firstVertex * comps, values, 0, numVertices * comps);
    }

    public int getVertexCount() { return count; }
    public int getComponentCount() { return comps; }
    public int getComponentType() { return size; }

    float component(int vertex, int c) {
        return size == 1 ? bytes[vertex * comps + c] : shorts[vertex * comps + c];
    }

    Object data() { return size == 1 ? (Object) bytes : (Object) shorts; }

    Object3D duplicateImpl() {
        VertexArray v = new VertexArray(count, comps, size);
        v.copyBase(this);
        if (size == 1) System.arraycopy(bytes, 0, v.bytes, 0, bytes.length);
        else System.arraycopy(shorts, 0, v.shorts, 0, shorts.length);
        return v;
    }
}
