/*
 * amiga-j2me: Motorola iDEN micro3d API for the Amiga J2ME runtime, a layer
 * over the Mascot Capsule v3 implementation.
 * Written for this project; licensed under GPLv2 (see COPYING).
 */
package com.motorola.iden.micro3d;

import com.mascotcapsule.micro3d.v3.AffineTrans;

public class AffineTransform {
    public static final int M00 = 0, M01 = 1, M02 = 2, M03 = 3, M10 = 4, M11 = 5, M12 = 6, M13 = 7,
        M20 = 8, M21 = 9, M22 = 10, M23 = 11, MAX_VALUE = 32767, MIN_VALUE = -32768;

    final AffineTrans t = new AffineTrans();

    public AffineTransform() {}
    public AffineTransform(int[][] elements) { set(elements); }

    public int get(int fieldID) {
        if (fieldID < 0 || fieldID > 11) throw new IllegalArgumentException();
        int[] a = new int[12];
        t.get(a);
        return a[fieldID];
    }

    public void set(int fieldID, int value) {
        if (fieldID < 0 || fieldID > 11) throw new IllegalArgumentException();
        int[] a = new int[12];
        t.get(a);
        a[fieldID] = value;
        t.set(a);
    }

    public void set(int[][] elements) {
        if (elements == null) throw new NullPointerException();
        t.set(elements);
    }

    public void setIdentity() { t.setIdentity(); }
    public void multiply(AffineTransform m) { t.multiply(m.t); }

    public static AffineTransform multiply(AffineTransform a, AffineTransform b) {
        AffineTransform r = new AffineTransform();
        r.t.multiply(a.t, b.t);
        return r;
    }

    public static void multiply(AffineTransform dest, AffineTransform a, AffineTransform b) { dest.t.multiply(a.t, b.t); }

    /** Makes the three axes unit length again. */
    public void normalize() {
        Vector3D c;
        c = new Vector3D(t.m00, t.m10, t.m20); if (!c.isZero()) { c.normalize(); t.m00 = c.x; t.m10 = c.y; t.m20 = c.z; }
        c = new Vector3D(t.m01, t.m11, t.m21); if (!c.isZero()) { c.normalize(); t.m01 = c.x; t.m11 = c.y; t.m21 = c.z; }
        c = new Vector3D(t.m02, t.m12, t.m22); if (!c.isZero()) { c.normalize(); t.m02 = c.x; t.m12 = c.y; t.m22 = c.z; }
    }

    public void rotateV(Vector3D axis, int angle) { t.setRotation(axis.v3(), angle); }
    public void rotateX(int angle) { t.setRotationX(angle); }
    public void rotateY(int angle) { t.setRotationY(angle); }
    public void rotateZ(int angle) { t.setRotationZ(angle); }

    public Vector3D transformPoint(Vector3D source) {
        com.mascotcapsule.micro3d.v3.Vector3D r = t.transform(source.v3());
        return new Vector3D(r.x, r.y, r.z);
    }

    public static AffineTransform getViewPointTransform(Vector3D position, Vector3D look, Vector3D up) {
        if (Vector3D.outerProduct(look, up).isZero()) throw new IllegalArgumentException();
        AffineTransform a = new AffineTransform();
        a.t.setViewTrans(position.v3(), look.v3(), up.v3());
        return a;
    }
}
