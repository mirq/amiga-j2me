/*
 * amiga-j2me: JSR-184 (M3G 1.0/1.1) implementation for the Amiga J2ME runtime.
 * Written for this project; licensed under GPLv2 (see COPYING).
 */
package javax.microedition.m3g;

public class Camera extends Node {
    public static final int GENERIC = 48, PARALLEL = 49, PERSPECTIVE = 50;

    private int type = GENERIC;
    private final float[] params = new float[4];        // fovy / height, aspect, near, far
    private final Transform projection = new Transform();

    public Camera() {}

    public void setParallel(float fovy, float aspectRatio, float near, float far) {
        if (fovy <= 0 || aspectRatio <= 0) throw new IllegalArgumentException();
        type = PARALLEL;
        params[0] = fovy; params[1] = aspectRatio; params[2] = near; params[3] = far;
        float h = fovy, w = aspectRatio * h, d = far - near;
        projection.setIdentity();
        float[] m = projection.m;
        m[0] = 2 / w; m[5] = 2 / h; m[10] = -2 / d; m[11] = -(near + far) / d;
    }

    public void setPerspective(float fovy, float aspectRatio, float near, float far) {
        if (fovy <= 0 || fovy >= 180 || aspectRatio <= 0 || near <= 0 || far <= 0) throw new IllegalArgumentException();
        type = PERSPECTIVE;
        params[0] = fovy; params[1] = aspectRatio; params[2] = near; params[3] = far;
        double half = fovy * (Math.PI / 360.0);
        float h = (float) Math.tan(half), w = aspectRatio * h, d = far - near;
        float[] m = projection.m;
        for (int i = 0; i < 16; i++) m[i] = 0;
        m[0] = 1 / w; m[5] = 1 / h; m[10] = -(near + far) / d; m[11] = -2 * near * far / d; m[14] = -1;
    }

    public void setGeneric(Transform t) {
        type = GENERIC;
        projection.set(t);
    }

    public int getProjection(Transform t) {
        if (t != null) t.set(projection);
        return type;
    }

    public int getProjection(float[] p) {
        if (p != null && type != GENERIC) {
            if (p.length < 4) throw new IllegalArgumentException();
            System.arraycopy(params, 0, p, 0, 4);
        }
        return type;
    }

    void setProperty(int property, float[] v) {
        if (property == AnimationTrack.FIELD_OF_VIEW && type != GENERIC) {
            if (type == PERSPECTIVE) setPerspective(M3GMath.clamp(v[0], 0.01f, 179.99f), params[1], params[2], params[3]);
            else setParallel(v[0], params[1], params[2], params[3]);
        } else if (property == AnimationTrack.NEAR_DISTANCE && type != GENERIC) {
            if (type == PERSPECTIVE) setPerspective(params[0], params[1], v[0], params[3]);
            else setParallel(params[0], params[1], v[0], params[3]);
        } else if (property == AnimationTrack.FAR_DISTANCE && type != GENERIC) {
            if (type == PERSPECTIVE) setPerspective(params[0], params[1], params[2], v[0]);
            else setParallel(params[0], params[1], params[2], v[0]);
        } else super.setProperty(property, v);
    }

    Object3D duplicateImpl() {
        Camera c = new Camera();
        c.copyNode(this);
        c.type = type;
        System.arraycopy(params, 0, c.params, 0, 4);
        c.projection.set(projection);
        return c;
    }
}
