/*
 * amiga-j2me: Mascot Capsule micro3d v3 API for the Amiga J2ME runtime.
 * Written for this project; licensed under GPLv2 (see COPYING).
 */
package com.mascotcapsule.micro3d.v3;

/** Where and how figures are projected: a view matrix, the screen centre and a projection. */
public class FigureLayout {
    static final int SCALE = 0, PARALLEL = 1, PERSPECTIVE_FOV = 2, PERSPECTIVE_WH = 3;

    AffineTrans affine;
    private AffineTrans[] array;
    int mode = SCALE;
    int scaleX = 512, scaleY = 512, centerX, centerY, parallelW, parallelH;
    int near, far, angle, perspW, perspH;

    public FigureLayout() { affine = new AffineTrans(); affine.setIdentity(); }
    public FigureLayout(AffineTrans trans, int sx, int sy, int cx, int cy) {
        setAffineTrans(trans);
        scaleX = sx; scaleY = sy; centerX = cx; centerY = cy;
    }

    public final AffineTrans getAffineTrans() { return affine; }
    public final void setAffineTrans(AffineTrans trans) {
        if (trans == null) { trans = new AffineTrans(); trans.setIdentity(); }
        affine = trans;
        array = null;
    }
    public final void setAffineTrans(AffineTrans[] trans) { setAffineTransArray(trans); }
    public final void setAffineTransArray(AffineTrans[] trans) {
        if (trans == null) throw new NullPointerException();
        for (int i = 0; i < trans.length; i++) if (trans[i] == null) throw new NullPointerException();
        array = trans;
        if (trans.length > 0) affine = trans[0];
    }
    public final void selectAffineTrans(int idx) {
        if (array == null || idx < 0 || idx >= array.length) throw new IllegalArgumentException();
        affine = array[idx];
    }
    AffineTrans transAt(int idx) { return array != null && idx >= 0 && idx < array.length ? array[idx] : null; }

    public final int getCenterX() { return centerX; }
    public final int getCenterY() { return centerY; }
    public final void setCenter(int cx, int cy) { centerX = cx; centerY = cy; }
    public final int getScaleX() { return scaleX; }
    public final int getScaleY() { return scaleY; }
    public final void setScale(int sx, int sy) { scaleX = sx; scaleY = sy; mode = SCALE; }
    public final int getParallelWidth() { return parallelW; }
    public final int getParallelHeight() { return parallelH; }
    public final void setParallelSize(int w, int h) {
        if (w < 0 || h < 0) throw new IllegalArgumentException();
        parallelW = w; parallelH = h; mode = PARALLEL;
    }
    public final void setPerspective(int zNear, int zFar, int angle) {
        if (zNear >= zFar || zNear < 1 || zNear > 32766 || zFar < 2 || zFar > 32767 || angle < 1 || angle > 2047) throw new IllegalArgumentException();
        near = zNear; far = zFar; this.angle = angle; mode = PERSPECTIVE_FOV;
    }
    public final void setPerspective(int zNear, int zFar, int width, int height) {
        if (zNear >= zFar || zNear < 1 || zNear > 32766 || zFar < 2 || zFar > 32767 || width < 0 || height < 0) throw new IllegalArgumentException();
        near = zNear; far = zFar; perspW = width; perspH = height; mode = PERSPECTIVE_WH;
    }
}
