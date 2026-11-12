/*
 * amiga-j2me: JSR-184 (M3G 1.0/1.1) implementation for the Amiga J2ME runtime.
 * Written for this project; licensed under GPLv2 (see COPYING).
 */
package javax.microedition.m3g;

public class Light extends Node {
    public static final int AMBIENT = 128, DIRECTIONAL = 129, OMNI = 130, SPOT = 131;

    int mode = DIRECTIONAL;
    int color = 0x00FFFFFF;
    float intensity = 1, constant = 1, linear, quadratic, spotAngle = 45, spotExponent;

    public Light() {}

    public void setMode(int m) {
        if (m < AMBIENT || m > SPOT) throw new IllegalArgumentException();
        mode = m;
    }
    public void setIntensity(float i) { intensity = i; }
    public void setColor(int RGB) { color = RGB & 0xFFFFFF; }
    public void setSpotAngle(float angle) {
        if (angle < 0 || angle > 90) throw new IllegalArgumentException();
        spotAngle = angle;
    }
    public void setSpotExponent(float e) {
        if (e < 0 || e > 128) throw new IllegalArgumentException();
        spotExponent = e;
    }
    public void setAttenuation(float c, float l, float q) {
        if (c < 0 || l < 0 || q < 0 || (c == 0 && l == 0 && q == 0)) throw new IllegalArgumentException();
        constant = c; linear = l; quadratic = q;
    }
    public int getMode() { return mode; }
    public float getIntensity() { return intensity; }
    public int getColor() { return color; }
    public float getSpotAngle() { return spotAngle; }
    public float getSpotExponent() { return spotExponent; }
    public float getConstantAttenuation() { return constant; }
    public float getLinearAttenuation() { return linear; }
    public float getQuadraticAttenuation() { return quadratic; }

    void setProperty(int property, float[] v) {
        switch (property) {
        case AnimationTrack.COLOR:
            color = ((int) (M3GMath.clamp(v[0], 0, 1) * 255) << 16) | ((int) (M3GMath.clamp(v[1], 0, 1) * 255) << 8)
                  | (int) (M3GMath.clamp(v[2], 0, 1) * 255);
            break;
        case AnimationTrack.INTENSITY: intensity = v[0]; break;
        case AnimationTrack.SPOT_ANGLE: spotAngle = M3GMath.clamp(v[0], 0, 90); break;
        case AnimationTrack.SPOT_EXPONENT: spotExponent = M3GMath.clamp(v[0], 0, 128); break;
        default: super.setProperty(property, v); break;
        }
    }

    Object3D duplicateImpl() {
        Light l = new Light();
        l.copyNode(this);
        l.mode = mode; l.color = color; l.intensity = intensity; l.constant = constant; l.linear = linear;
        l.quadratic = quadratic; l.spotAngle = spotAngle; l.spotExponent = spotExponent;
        return l;
    }
}
