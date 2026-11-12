/*
 * amiga-j2me: JSR-184 (M3G 1.0/1.1) implementation for the Amiga J2ME runtime.
 * Written for this project; licensed under GPLv2 (see COPYING).
 */
package javax.microedition.m3g;

public class CompositingMode extends Object3D {
    public static final int ALPHA = 64, ALPHA_ADD = 65, MODULATE = 66, MODULATE_X2 = 67, REPLACE = 68;

    int blending = REPLACE;
    float alphaThreshold, depthOffsetFactor, depthOffsetUnits;
    boolean depthTest = true, depthWrite = true, colorWrite = true, alphaWrite = true;

    public CompositingMode() {}

    public void setBlending(int mode) {
        if (mode < ALPHA || mode > REPLACE) throw new IllegalArgumentException();
        blending = mode;
    }
    public int getBlending() { return blending; }
    public void setAlphaThreshold(float t) {
        if (t < 0 || t > 1) throw new IllegalArgumentException();
        alphaThreshold = t;
    }
    public float getAlphaThreshold() { return alphaThreshold; }
    public void setAlphaWriteEnable(boolean enable) { alphaWrite = enable; }
    public boolean isAlphaWriteEnabled() { return alphaWrite; }
    public void setColorWriteEnable(boolean enable) { colorWrite = enable; }
    public boolean isColorWriteEnabled() { return colorWrite; }
    public void setDepthWriteEnable(boolean enable) { depthWrite = enable; }
    public boolean isDepthWriteEnabled() { return depthWrite; }
    public void setDepthTestEnable(boolean enable) { depthTest = enable; }
    public boolean isDepthTestEnabled() { return depthTest; }
    public void setDepthOffset(float factor, float units) { depthOffsetFactor = factor; depthOffsetUnits = units; }
    public float getDepthOffsetFactor() { return depthOffsetFactor; }
    public float getDepthOffsetUnits() { return depthOffsetUnits; }

    Object3D duplicateImpl() {
        CompositingMode c = new CompositingMode();
        c.copyBase(this);
        c.blending = blending; c.alphaThreshold = alphaThreshold; c.depthOffsetFactor = depthOffsetFactor;
        c.depthOffsetUnits = depthOffsetUnits; c.depthTest = depthTest; c.depthWrite = depthWrite;
        c.colorWrite = colorWrite; c.alphaWrite = alphaWrite;
        return c;
    }
}
