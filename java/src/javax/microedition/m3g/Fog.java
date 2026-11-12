/*
 * amiga-j2me: JSR-184 (M3G 1.0/1.1) implementation for the Amiga J2ME runtime.
 * Written for this project; licensed under GPLv2 (see COPYING).
 */
package javax.microedition.m3g;

/** Fog settings are kept, but fog is not drawn. */
public class Fog extends Object3D {
    public static final int EXPONENTIAL = 80, LINEAR = 81;

    private int mode = LINEAR, color;
    private float density = 1, near, far = 1;

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
}
