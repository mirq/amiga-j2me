/*
 * amiga-j2me: Motorola iDEN micro3d API for the Amiga J2ME runtime, a layer
 * over the Mascot Capsule v3 implementation.
 * Written for this project; licensed under GPLv2 (see COPYING).
 */
package com.motorola.iden.micro3d;

import com.mascotcapsule.micro3d.v3.AffineTrans;
import com.mascotcapsule.micro3d.v3.Effect3D;
import com.mascotcapsule.micro3d.v3.FigureLayout;

/** The camera, the object's own transform, the projection, the light and the shading of an object. */
public class Layout3D {
    public static final int PERSPECTIVE_FOV = 21, PERSPECTIVE_WIDTH = 22, PERSPECTIVE_WIDTH_HEIGHT = 23,
        PARALLEL_SCALE = 24, PARALLEL_WIDTH = 25, PARALLEL_WIDTH_HEIGHT = 26;

    private AffineTransform view, viewPoint;
    private int type = PARALLEL_SCALE;
    private int[] params = { 4096, 4096, 0, 0 };
    private Light light;
    private boolean toon, semiTransparent;
    private int toonThreshold = 128, toonHigh = 255, toonLow = 96;

    public Layout3D() {
        view = new AffineTransform(); view.setIdentity();
        viewPoint = new AffineTransform(); viewPoint.setIdentity();
    }

    public Light getLight() { return light; }
    public void setLight(Light light) { this.light = light; }
    public int getProjectionType() { return type; }
    public int[] getProjectionParameters() {
        int n = type == PARALLEL_WIDTH ? 1 : (type == PARALLEL_SCALE || type == PARALLEL_WIDTH_HEIGHT ? 2 : (type == PERSPECTIVE_WIDTH_HEIGHT ? 4 : 3));
        int[] r = new int[n];
        System.arraycopy(params, 0, r, 0, n);
        return r;
    }

    public void setProjection(int type, int[] p) {
        if (p == null) throw new NullPointerException();
        int need;
        switch (type) {
        case PARALLEL_WIDTH: need = 1; break;
        case PARALLEL_SCALE: case PARALLEL_WIDTH_HEIGHT: need = 2; break;
        case PERSPECTIVE_FOV: case PERSPECTIVE_WIDTH: need = 3; break;
        case PERSPECTIVE_WIDTH_HEIGHT: need = 4; break;
        default: throw new IllegalArgumentException();
        }
        if (p.length < need) throw new IllegalArgumentException();
        if (need >= 3 && (p[0] >= p[1] || p[0] < 1 || p[1] > 32767)) throw new IllegalArgumentException();
        if (type == PERSPECTIVE_FOV && (p[2] < 1 || p[2] > 2047)) throw new IllegalArgumentException();
        this.type = type;
        System.arraycopy(p, 0, params, 0, need);
    }

    public int getToonThreshold() { return toonThreshold; }
    public int getToonHighColor() { return toonHigh; }
    public int getToonLowColor() { return toonLow; }
    public boolean isToonShaded() { return toon; }
    public void setToonShading(boolean toon) { this.toon = toon; }
    public void setToonShading(int threshold, int highColor, int lowColor) {
        if (threshold < 0 || threshold > 255 || highColor < 0 || highColor > 255 || lowColor < 0 || lowColor > 255) throw new IllegalArgumentException();
        toonThreshold = threshold; toonHigh = highColor; toonLow = lowColor;
    }
    public boolean isSemiTransparent() { return semiTransparent; }
    public void setSemiTransparent(boolean on) { semiTransparent = on; }

    public AffineTransform getViewTransform() { return view; }
    public AffineTransform getViewPointTransform() { return viewPoint; }
    public void setViewTransform(AffineTransform t) { if (t == null) throw new NullPointerException(); view = t; }
    public void setViewPointTransform(AffineTransform t) { if (t == null) throw new NullPointerException(); viewPoint = t; }
    public void setViewPoint(Vector3D position, Vector3D look, Vector3D up) {
        viewPoint = AffineTransform.getViewPointTransform(position, look, up);
    }

    /** The object's own transform becomes a rotation. */
    public void rotateV(Vector3D axis, int angle) { view = new AffineTransform(); view.setIdentity(); view.rotateV(axis, angle); }
    public void rotateX(int angle) { view = new AffineTransform(); view.setIdentity(); view.rotateX(angle); }
    public void rotateY(int angle) { view = new AffineTransform(); view.setIdentity(); view.rotateY(angle); }
    public void rotateZ(int angle) { view = new AffineTransform(); view.setIdentity(); view.rotateZ(angle); }

    /** The same thing for the v3 renderer; w, h: size of the drawing surface. */
    FigureLayout toV3(int w, int h) {
        AffineTrans m = new AffineTrans();
        m.multiply(viewPoint.t, view.t);
        FigureLayout l = new FigureLayout();
        l.setAffineTrans(m);
        switch (type) {
        case PARALLEL_SCALE: l.setScale(params[0], params[1]); break;
        case PARALLEL_WIDTH: l.setParallelSize(params[0], Math.max(1, params[0] * h / Math.max(1, w))); break;
        case PARALLEL_WIDTH_HEIGHT: l.setParallelSize(params[0], params[1]); break;
        case PERSPECTIVE_FOV: l.setPerspective(params[0], params[1], params[2]); break;
        case PERSPECTIVE_WIDTH: l.setPerspective(params[0], params[1], params[2], Math.max(1, params[2] * h / Math.max(1, w))); break;
        default: l.setPerspective(params[0], params[1], params[2], params[3]); break;
        }
        return l;
    }

    Effect3D effect(Texture sphere) {
        Effect3D e = new Effect3D();
        if (light != null) {
            e.setLight(new com.mascotcapsule.micro3d.v3.Light(light.getDirectionVector().v3(), light.getDirectionalIntensity(),
                                                             light.getAmbientIntensity()));
        }
        e.setShadingType(toon ? Effect3D.TOON_SHADING : Effect3D.NORMAL_SHADING);
        e.setToonParams(toonThreshold, toonHigh, toonLow);
        e.setTransparency(semiTransparent);
        if (sphere != null && sphere.sphere != null) e.setSphereTexture(sphere.sphere);
        return e;
    }
}
