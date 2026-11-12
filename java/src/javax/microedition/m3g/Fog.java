/*
 * amiga-j2me: JSR-184 (M3G 1.0/1.1) implementation for the Amiga J2ME runtime.
 * Written for this project; licensed under GPLv2 (see COPYING).
 */
package javax.microedition.m3g;

public class Fog extends Object3D {
    public static final int EXPONENTIAL = 80, LINEAR = 81;

    int mode = LINEAR, color;
    float density = 1, near, far = 1;

    public Fog() {}

    public void setMode(int m) {
        if (m != EXPONENTIAL && m != LINEAR) throw new IllegalArgumentException();
        mode = m;
    }
    public int getMode() { return mode; }
    public void setLinear(float near, float far) { this.near = near; this.far = far; }
    public float getNearDistance() { return near; }
    public float getFarDistance() { return far; }
    public void setDensity(float d) {
        if (d < 0) throw new IllegalArgumentException();
        density = d;
    }
    public float getDensity() { return density; }
    public void setColor(int RGB) { color = RGB & 0xFFFFFF; }
    public int getColor() { return color; }

    void setProperty(int property, float[] v) {
        switch (property) {
        case AnimationTrack.COLOR:
            color = ((int) (M3GMath.clamp(v[0], 0, 1) * 255) << 16) | ((int) (M3GMath.clamp(v[1], 0, 1) * 255) << 8)
                  | (int) (M3GMath.clamp(v[2], 0, 1) * 255);
            break;
        case AnimationTrack.DENSITY: density = v[0] < 0 ? 0 : v[0]; break;
        case AnimationTrack.NEAR_DISTANCE: near = v[0]; break;
        case AnimationTrack.FAR_DISTANCE: far = v[0]; break;
        default: super.setProperty(property, v); break;
        }
    }
}
