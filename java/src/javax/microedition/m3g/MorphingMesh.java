/*
 * amiga-j2me: JSR-184 (M3G 1.0/1.1) implementation for the Amiga J2ME runtime.
 * Written for this project; licensed under GPLv2 (see COPYING).
 */
package javax.microedition.m3g;

/** Drawn as its base mesh: the morph targets and weights are kept but not blended in. */
public class MorphingMesh extends Mesh {
    private final VertexBuffer[] targets;
    private final float[] weights;

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
            for (int i = 0; i < weights.length && i < v.length; i++) weights[i] = v[i];
        } else super.setProperty(property, v);
    }
}
