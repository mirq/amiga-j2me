/*
 * amiga-j2me: Motorola iDEN micro3d API for the Amiga J2ME runtime, a layer
 * over the Mascot Capsule v3 implementation.
 * Written for this project; licensed under GPLv2 (see COPYING).
 */
package com.motorola.iden.micro3d;

public class Light {
    public static final int MAX_AMBIENT_LIGHT_INTENSITY = 4096, MAX_DIRECTIONAL_LIGHT_INTENSITY = 16384,
        MIN_AMBIENT_LIGHT_INTENSITY = 0, MIN_DIRECTIONAL_LIGHT_INTENSITY = 0;
    private int ambient, directional;
    private Vector3D direction;

    public Light(int ambientIntensity, Vector3D direction, int directionalIntensity) {
        setAmbientIntensity(ambientIntensity);
        setDirectionVector(direction);
        setDirectionalIntensity(directionalIntensity);
    }

    public int getAmbientIntensity() { return ambient; }
    public int getDirectionalIntensity() { return directional; }
    public Vector3D getDirectionVector() { return direction; }
    public void setAmbientIntensity(int i) {
        if (i < 0 || i > MAX_AMBIENT_LIGHT_INTENSITY) throw new IllegalArgumentException();
        ambient = i;
    }
    public void setDirectionalIntensity(int i) {
        if (i < 0 || i > MAX_DIRECTIONAL_LIGHT_INTENSITY) throw new IllegalArgumentException();
        directional = i;
    }
    public void setDirectionVector(Vector3D d) {
        if (d == null) throw new NullPointerException();
        direction = d;
    }
}
