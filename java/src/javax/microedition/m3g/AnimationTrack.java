/*
 * amiga-j2me: JSR-184 (M3G 1.0/1.1) implementation for the Amiga J2ME runtime.
 * Written for this project; licensed under GPLv2 (see COPYING).
 */
package javax.microedition.m3g;

import java.util.Vector;

public class AnimationTrack extends Object3D {
    public static final int ALPHA = 256, AMBIENT_COLOR = 257, COLOR = 258, CROP = 259, DENSITY = 260,
        DIFFUSE_COLOR = 261, EMISSIVE_COLOR = 262, FAR_DISTANCE = 263, FIELD_OF_VIEW = 264, INTENSITY = 265,
        MORPH_WEIGHTS = 266, NEAR_DISTANCE = 267, ORIENTATION = 268, PICKABILITY = 269, SCALE = 270,
        SHININESS = 271, SPECULAR_COLOR = 272, SPOT_ANGLE = 273, SPOT_EXPONENT = 274, TRANSLATION = 275,
        VISIBILITY = 276;

    private final KeyframeSequence sequence;
    private final int property;
    private AnimationController controller;
    private final float[] value;

    public AnimationTrack(KeyframeSequence sequence, int property) {
        if (sequence == null) throw new NullPointerException();
        if (property < ALPHA || property > VISIBILITY) throw new IllegalArgumentException();
        this.sequence = sequence; this.property = property;
        value = new float[sequence.getComponentCount()];
    }

    public void setController(AnimationController c) { controller = c; }
    public AnimationController getController() { return controller; }
    public KeyframeSequence getKeyframeSequence() { return sequence; }
    public int getTargetProperty() { return property; }

    /** Tracks are not blended by weight: the last active one with a non-zero weight wins. */
    void apply(Object3D target, int worldTime) {
        AnimationController c = controller;
        if (c == null || !c.isActive(worldTime) || c.getWeight() <= 0) return;
        sequence.sample(c.getPosition(worldTime), value);
        target.setProperty(property, value);
    }

    void collectReferences(Vector out) { add(out, sequence); add(out, controller); }
}
