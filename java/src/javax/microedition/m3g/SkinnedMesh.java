/*
 * amiga-j2me: JSR-184 (M3G 1.0/1.1) implementation for the Amiga J2ME runtime.
 * Written for this project; licensed under GPLv2 (see COPYING).
 */
package javax.microedition.m3g;

import java.util.Vector;

/**
 * Vertices follow the bones they were attached to with addTransform:
 * v' = sum of weight * (bone now) * (bone at rest)^-1 * v, weights normalised
 * per vertex. Computed in Java each time the mesh is drawn.
 */
public class SkinnedMesh extends Mesh {
    private final Group skeleton;
    private final Vector bones = new Vector();      // Bone
    private VertexBuffer skinned;                   // scratch

    private static final class Bone {
        Node node;
        int weight, first, count;
        float[] restInverse = new float[16];        // mesh -> bone, when attached
    }

    public SkinnedMesh(VertexBuffer vertices, IndexBuffer submesh, Appearance appearance, Group skeleton) {
        super(vertices, submesh, appearance);
        this.skeleton = attach(skeleton);
    }

    public SkinnedMesh(VertexBuffer vertices, IndexBuffer[] submeshes, Appearance[] appearances, Group skeleton) {
        super(vertices, submeshes, appearances);
        this.skeleton = attach(skeleton);
    }

    private Group attach(Group s) {
        if (s == null) throw new NullPointerException();
        if (s.parent != null || s instanceof World) throw new IllegalArgumentException();
        s.parent = this;
        return s;
    }

    public Group getSkeleton() { return skeleton; }

    /** bone -> this mesh, through the skeleton. False when the node is not in it. */
    private boolean boneToMesh(Node bone, float[] out) {
        Transform acc = new Transform(), t = new Transform();
        for (Node n = bone; n != this; n = n.parent) {
            if (n == null) return false;
            n.getCompositeTransform(t);
            t.postMultiply(acc);
            acc.set(t);
        }
        acc.get(out);
        return true;
    }

    public void addTransform(Node bone, int weight, int firstVertex, int numVertices) {
        if (bone == null) throw new NullPointerException();
        if (weight <= 0 || numVertices <= 0) throw new IllegalArgumentException();
        if (firstVertex < 0 || firstVertex + numVertices > 65535) throw new IndexOutOfBoundsException();
        Bone b = new Bone();
        float[] m = new float[16];
        if (!boneToMesh(bone, m)) throw new IllegalArgumentException("the bone is not in the skeleton");
        if (!Transform.invert(m, b.restInverse)) new Transform().get(b.restInverse);
        b.node = bone; b.weight = weight; b.first = firstVertex; b.count = numVertices;
        bones.addElement(b);
    }

    public void getBoneTransform(Node bone, Transform transform) {
        if (bone == null || transform == null) throw new NullPointerException();
        for (int i = 0; i < bones.size(); i++) {
            Bone b = (Bone) bones.elementAt(i);
            if (b.node == bone) { transform.set(b.restInverse); return; }
        }
        throw new IllegalArgumentException();
    }

    public int getBoneVertices(Node bone, int[] indices, float[] weights) {
        int n = 0;
        for (int i = 0; i < bones.size(); i++) {
            Bone b = (Bone) bones.elementAt(i);
            if (b.node != bone) continue;
            for (int v = 0; v < b.count; v++) {
                if (indices != null && n < indices.length) indices[n] = b.first + v;
                if (weights != null && n < weights.length) weights[n] = b.weight;
                n++;
            }
        }
        return n;
    }

    VertexBuffer renderVertices() {
        VertexArray pos = vertices.positions;
        if (bones.size() == 0 || pos == null) return vertices;
        int n = pos.count;
        float[] acc = new float[n * 3], accN = vertices.normals == null ? null : new float[n * 3];
        float[] wsum = new float[n];
        float[] now = new float[16], m = new float[16];
        float scale = vertices.posScale, bx = vertices.posBias[0], by = vertices.posBias[1], bz = vertices.posBias[2];
        VertexArray nrm = vertices.normals;
        for (int i = 0; i < bones.size(); i++) {
            Bone b = (Bone) bones.elementAt(i);
            if (!boneToMesh(b.node, now)) continue;
            Transform.mul(now, b.restInverse, m);       // rest pose -> current pose, in mesh space
            int end = Math.min(n, b.first + b.count);
            float w = b.weight;
            for (int v = b.first; v < end; v++) {
                float x = pos.component(v, 0) * scale + bx, y = pos.component(v, 1) * scale + by,
                      z = pos.component(v, 2) * scale + bz;
                acc[v * 3]     += w * (m[0] * x + m[1] * y + m[2] * z + m[3]);
                acc[v * 3 + 1] += w * (m[4] * x + m[5] * y + m[6] * z + m[7]);
                acc[v * 3 + 2] += w * (m[8] * x + m[9] * y + m[10] * z + m[11]);
                if (accN != null) {
                    float nx = nrm.component(v, 0), ny = nrm.component(v, 1), nz = nrm.component(v, 2);
                    accN[v * 3]     += w * (m[0] * nx + m[1] * ny + m[2] * nz);
                    accN[v * 3 + 1] += w * (m[4] * nx + m[5] * ny + m[6] * nz);
                    accN[v * 3 + 2] += w * (m[8] * nx + m[9] * ny + m[10] * nz);
                }
                wsum[v] += w;
            }
        }
        if (skinned == null) skinned = (VertexBuffer) vertices.duplicateImpl();
        // positions go out as shorts with the mesh's own scale and bias
        VertexArray outPos = new VertexArray(n, 3, 2);
        VertexArray outNrm = nrm == null ? null : new VertexArray(n, 3, nrm.size);
        float inv = scale == 0 ? 0 : 1 / scale;
        for (int v = 0; v < n; v++) {
            for (int c = 0; c < 3; c++) {
                float p;
                if (wsum[v] > 0) p = (acc[v * 3 + c] / wsum[v] - vertices.posBias[c]) * inv;
                else p = pos.component(v, c);
                int q = (int) (p < 0 ? p - 0.5f : p + 0.5f);
                outPos.shorts[v * 3 + c] = (short) (q < -32768 ? -32768 : (q > 32767 ? 32767 : q));
                if (outNrm != null) {
                    float f = wsum[v] > 0 ? accN[v * 3 + c] / wsum[v] : nrm.component(v, c);
                    int qn = (int) (f < 0 ? f - 0.5f : f + 0.5f);
                    if (nrm.size == 1) outNrm.bytes[v * 3 + c] = (byte) (qn < -128 ? -128 : (qn > 127 ? 127 : qn));
                    else outNrm.shorts[v * 3 + c] = (short) (qn < -32768 ? -32768 : (qn > 32767 ? 32767 : qn));
                }
            }
        }
        skinned.positions = outPos;
        skinned.normals = outNrm;
        skinned.defaultColor = vertices.defaultColor;
        return skinned;
    }

    void collectReferences(Vector out) {
        super.collectReferences(out);
        add(out, skeleton);
    }
}
