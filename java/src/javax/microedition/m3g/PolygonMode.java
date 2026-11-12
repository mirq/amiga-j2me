/*
 * amiga-j2me: JSR-184 (M3G 1.0/1.1) implementation for the Amiga J2ME runtime.
 * Written for this project; licensed under GPLv2 (see COPYING).
 */
package javax.microedition.m3g;

public class PolygonMode extends Object3D {
    public static final int CULL_BACK = 160, CULL_FRONT = 161, CULL_NONE = 162;
    public static final int SHADE_FLAT = 164, SHADE_SMOOTH = 165;
    public static final int WINDING_CCW = 168, WINDING_CW = 169;

    int culling = CULL_BACK, shading = SHADE_SMOOTH, winding = WINDING_CCW;
    boolean twoSided, localCamera, perspective;

    public PolygonMode() {}

    public void setCulling(int mode) {
        if (mode < CULL_BACK || mode > CULL_NONE) throw new IllegalArgumentException();
        culling = mode;
    }
    public int getCulling() { return culling; }
    public void setWinding(int mode) {
        if (mode != WINDING_CCW && mode != WINDING_CW) throw new IllegalArgumentException();
        winding = mode;
    }
    public int getWinding() { return winding; }
    public void setShading(int mode) {
        if (mode != SHADE_FLAT && mode != SHADE_SMOOTH) throw new IllegalArgumentException();
        shading = mode;
    }
    public int getShading() { return shading; }
    public void setTwoSidedLightingEnable(boolean enable) { twoSided = enable; }
    public boolean isTwoSidedLightingEnabled() { return twoSided; }
    public void setLocalCameraLightingEnable(boolean enable) { localCamera = enable; }
    public boolean isLocalCameraLightingEnabled() { return localCamera; }
    public void setPerspectiveCorrectionEnable(boolean enable) { perspective = enable; }
    public boolean isPerspectiveCorrectionEnabled() { return perspective; }

    Object3D duplicateImpl() {
        PolygonMode p = new PolygonMode();
        p.copyBase(this);
        p.culling = culling; p.shading = shading; p.winding = winding;
        p.twoSided = twoSided; p.localCamera = localCamera; p.perspective = perspective;
        return p;
    }
}
