/*
 * amiga-j2me: JSR-184 (M3G 1.0/1.1) implementation for the Amiga J2ME runtime.
 * Written for this project; licensed under GPLv2 (see COPYING).
 */
package javax.microedition.m3g;

/** base + sum of weight[i] * (target[i] - base), for positions and normals. */
public class MorphingMesh extends Mesh {
    private final VertexBuffer[] targets;
    private final float[] weights;
    private VertexBuffer morphed;       // scratch, rebuilt when drawn

    public MorphingMesh(VertexBuffer base, VertexBuffer[] targets, IndexBuffer submesh, Appearance appearance) {
        super(base, submesh, appearance);
        if (targets == null) throw new NullPointerException();
        this.targets = targets;
        weights = new float[targets.length];
    }

    public MorphingMesh(VertexBuffer base, VertexBuffer[] targets, IndexBuffer[] submeshes, Appearance[] appearances) {
        super(base, submeshes, appearances);
        if (targets == null) throw new NullPointerException();
        this.targets = targets;
        weights = new float[targets.length];
    }

    public VertexBuffer getMorphTarget(int index) { return targets[index]; }
    public int getMorphTargetCount() { return targets.length; }
    public void setWeights(float[] w) {
        if (w.length < weights.length) throw new IllegalArgumentException();
        System.arraycopy(w, 0, weights, 0, weights.length);
    }
    public void getWeights(float[] w) {
        if (w.length < weights.length) throw new IllegalArgumentException();
        System.arraycopy(weights, 0, w, 0, weights.length);
    }

    void setProperty(int property, float[] v) {
        if (property == AnimationTrack.MORPH_WEIGHTS) {
            for (int i = 0; i < weights.length; i++) weights[i] = i < v.length ? v[i] : 0;
        } else super.setProperty(property, v);
    }

    private static VertexArray blend(VertexArray base, VertexBuffer[] targets, float[] weights, boolean normals) {
        if (base == null) return null;
        int n = base.count * base.comps;
        float[] acc = new float[n];
        for (int i = 0; i < n; i++) acc[i] = base.size == 1 ? base.bytes[i] : base.shorts[i];
        for (int t = 0; t < targets.length; t++) {
            float w = weights[t];
            VertexArray ta = targets[t] == null ? null : (normals ? targets[t].normals : targets[t].positions);
            if (w == 0 || ta == null || ta.count != base.count || ta.comps != base.comps) continue;
            for (int i = 0; i < n; i++) {
                float b = base.size == 1 ? base.bytes[i] : base.shorts[i];
                acc[i] += w * ((ta.size == 1 ? ta.bytes[i] : ta.shorts[i]) - b);
            }
        }
        VertexArray out = new VertexArray(base.count, base.comps, base.size);
        int lo = base.size == 1 ? -128 : -32768, hi = base.size == 1 ? 127 : 32767;
        for (int i = 0; i < n; i++) {
            int v = (int) (acc[i] < 0 ? acc[i] - 0.5f : acc[i] + 0.5f);
            v = v < lo ? lo : (v > hi ? hi : v);
            if (base.size == 1) out.bytes[i] = (byte) v; else out.shorts[i] = (short) v;
        }
        return out;
    }

    VertexBuffer renderVertices() {
        boolean any = false;
        for (int i = 0; i < weights.length; i++) any |= weights[i] != 0;
        if (!any) return vertices;
        if (morphed == null) morphed = (VertexBuffer) vertices.duplicateImpl();
        morphed.positions = blend(vertices.positions, targets, weights, false);
        morphed.normals = blend(vertices.normals, targets, weights, true);
        morphed.defaultColor = vertices.defaultColor;
        return morphed;
    }
}
