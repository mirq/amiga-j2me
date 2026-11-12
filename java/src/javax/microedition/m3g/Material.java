/*
 * amiga-j2me: JSR-184 (M3G 1.0/1.1) implementation for the Amiga J2ME runtime.
 * Written for this project; licensed under GPLv2 (see COPYING).
 */
package javax.microedition.m3g;

public class Material extends Object3D {
    public static final int AMBIENT = 1024, DIFFUSE = 2048, EMISSIVE = 4096, SPECULAR = 8192;

    int ambient = 0x00333333, diffuse = 0xFFCCCCCC, emissive, specular;
    float shininess;
    boolean vertexColorTracking;

    public Material() {}

    public void setColor(int target, int ARGB) {
        if ((target & ~(AMBIENT | DIFFUSE | EMISSIVE | SPECULAR)) != 0 || target == 0) throw new IllegalArgumentException();
        if ((target & AMBIENT) != 0) ambient = ARGB & 0xFFFFFF;
        if ((target & DIFFUSE) != 0) diffuse = ARGB;
        if ((target & EMISSIVE) != 0) emissive = ARGB & 0xFFFFFF;
        if ((target & SPECULAR) != 0) specular = ARGB & 0xFFFFFF;
    }

    public int getColor(int target) {
        switch (target) {
        case AMBIENT: return ambient;
        case DIFFUSE: return diffuse;
        case EMISSIVE: return emissive;
        case SPECULAR: return specular;
        default: throw new IllegalArgumentException();
        }
    }

    public void setShininess(float s) {
        if (s < 0 || s > 128) throw new IllegalArgumentException();
        shininess = s;
    }
    public float getShininess() { return shininess; }
    public void setVertexColorTrackingEnable(boolean enable) { vertexColorTracking = enable; }
    public boolean isVertexColorTrackingEnabled() { return vertexColorTracking; }

    private static int rgb(float[] v) {
        return ((int) (M3GMath.clamp(v[0], 0, 1) * 255) << 16) | ((int) (M3GMath.clamp(v[1], 0, 1) * 255) << 8)
             | (int) (M3GMath.clamp(v[2], 0, 1) * 255);
    }

    void setProperty(int property, float[] v) {
        switch (property) {
        case AnimationTrack.ALPHA: diffuse = (diffuse & 0xFFFFFF) | ((int) (M3GMath.clamp(v[0], 0, 1) * 255) << 24); break;
        case AnimationTrack.AMBIENT_COLOR: ambient = rgb(v); break;
        case AnimationTrack.DIFFUSE_COLOR: diffuse = (diffuse & 0xFF000000) | rgb(v); break;
        case AnimationTrack.EMISSIVE_COLOR: emissive = rgb(v); break;
        case AnimationTrack.SPECULAR_COLOR: specular = rgb(v); break;
        case AnimationTrack.SHININESS: shininess = M3GMath.clamp(v[0], 0, 128); break;
        default: super.setProperty(property, v); break;
        }
    }

    Object3D duplicateImpl() {
        Material m = new Material();
        m.copyBase(this);
        m.ambient = ambient; m.diffuse = diffuse; m.emissive = emissive; m.specular = specular;
        m.shininess = shininess; m.vertexColorTracking = vertexColorTracking;
        return m;
    }
}
