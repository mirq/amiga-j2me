/*
 * amiga-j2me: JSR-184 (M3G 1.0/1.1) implementation for the Amiga J2ME runtime.
 * Written for this project; licensed under GPLv2 (see COPYING).
 */
package javax.microedition.m3g;

/** Translation, orientation (a quaternion), scale and a generic matrix: T * R * S * M. */
public abstract class Transformable extends Object3D {
    final float[] translation = new float[3];
    final float[] scale = { 1, 1, 1 };
    final float[] quat = { 0, 0, 0, 1 };        // x, y, z, w
    final Transform generic = new Transform();

    Transformable() {}

    public void setOrientation(float angle, float ax, float ay, float az) {
        if (angle != 0 && ax == 0 && ay == 0 && az == 0) throw new IllegalArgumentException();
        quat[0] = 0; quat[1] = 0; quat[2] = 0; quat[3] = 1;
        preRotate(angle, ax, ay, az);
    }

    private static void quatMul(float[] a, float[] b, float[] out) {
        float x = a[3] * b[0] + a[0] * b[3] + a[1] * b[2] - a[2] * b[1];
        float y = a[3] * b[1] - a[0] * b[2] + a[1] * b[3] + a[2] * b[0];
        float z = a[3] * b[2] + a[0] * b[1] - a[1] * b[0] + a[2] * b[3];
        float w = a[3] * b[3] - a[0] * b[0] - a[1] * b[1] - a[2] * b[2];
        out[0] = x; out[1] = y; out[2] = z; out[3] = w;
    }

    private static float[] axisQuat(float angle, float ax, float ay, float az) {
        float len = (float) Math.sqrt(ax * ax + ay * ay + az * az);
        if (len == 0) return new float[] { 0, 0, 0, 1 };
        double rad = angle * (Math.PI / 360.0);
        float s = (float) Math.sin(rad) / len;
        return new float[] { ax * s, ay * s, az * s, (float) Math.cos(rad) };
    }

    public void preRotate(float angle, float ax, float ay, float az) {
        if (angle == 0) return;
        if (ax == 0 && ay == 0 && az == 0) throw new IllegalArgumentException();
        quatMul(axisQuat(angle, ax, ay, az), quat, quat);
    }

    public void postRotate(float angle, float ax, float ay, float az) {
        if (angle == 0) return;
        if (ax == 0 && ay == 0 && az == 0) throw new IllegalArgumentException();
        quatMul(quat, axisQuat(angle, ax, ay, az), quat);
    }

    public void getOrientation(float[] angleAxis) {
        if (angleAxis.length < 4) throw new IllegalArgumentException();
        float w = quat[3] > 1 ? 1 : (quat[3] < -1 ? -1 : quat[3]);
        float s = (float) Math.sqrt(1 - w * w);
        angleAxis[0] = (float) (2 * M3GMath.acos(w) * 180 / Math.PI);
        if (s < 1e-6f) { angleAxis[1] = 0; angleAxis[2] = 0; angleAxis[3] = 1; }
        else { angleAxis[1] = quat[0] / s; angleAxis[2] = quat[1] / s; angleAxis[3] = quat[2] / s; }
    }

    public void setScale(float sx, float sy, float sz) { scale[0] = sx; scale[1] = sy; scale[2] = sz; }
    public void scale(float sx, float sy, float sz) { scale[0] *= sx; scale[1] *= sy; scale[2] *= sz; }
    public void getScale(float[] xyz) { System.arraycopy(scale, 0, xyz, 0, 3); }
    public void setTranslation(float tx, float ty, float tz) { translation[0] = tx; translation[1] = ty; translation[2] = tz; }
    public void translate(float tx, float ty, float tz) { translation[0] += tx; translation[1] += ty; translation[2] += tz; }
    public void getTranslation(float[] xyz) { System.arraycopy(translation, 0, xyz, 0, 3); }
    public void setTransform(Transform t) { if (t == null) generic.setIdentity(); else generic.set(t); }
    public void getTransform(Transform t) { t.set(generic); }

    public void getCompositeTransform(Transform t) {
        t.setIdentity();
        t.postTranslate(translation[0], translation[1], translation[2]);
        t.postRotateQuat(quat[0], quat[1], quat[2], quat[3]);
        t.postScale(scale[0], scale[1], scale[2]);
        t.postMultiply(generic);
    }

    void setProperty(int property, float[] v) {
        switch (property) {
        case AnimationTrack.TRANSLATION: setTranslation(v[0], v[1], v[2]); break;
        case AnimationTrack.SCALE:
            if (v.length >= 3) setScale(v[0], v[1], v[2]); else setScale(v[0], v[0], v[0]);
            break;
        case AnimationTrack.ORIENTATION: quat[0] = v[0]; quat[1] = v[1]; quat[2] = v[2]; quat[3] = v[3]; break;
        default: super.setProperty(property, v); break;
        }
    }

    void copyTransformable(Transformable from) {
        copyBase(from);
        System.arraycopy(from.translation, 0, translation, 0, 3);
        System.arraycopy(from.scale, 0, scale, 0, 3);
        System.arraycopy(from.quat, 0, quat, 0, 4);
        generic.set(from.generic);
    }
}
