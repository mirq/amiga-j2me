/*
 * amiga-j2me: Mascot Capsule micro3d v3 API for the Amiga J2ME runtime.
 * Written for this project; licensed under GPLv2 (see COPYING).
 */
package com.mascotcapsule.micro3d.v3;

/** A 3x4 matrix; 4096 is 1.0 in the rotation part, the last column is a translation in model units. */
public class AffineTrans {
    public int m00, m01, m02, m03, m10, m11, m12, m13, m20, m21, m22, m23;

    public AffineTrans() {}
    public AffineTrans(int m00, int m01, int m02, int m03, int m10, int m11, int m12, int m13,
                       int m20, int m21, int m22, int m23) {
        set(m00, m01, m02, m03, m10, m11, m12, m13, m20, m21, m22, m23);
    }
    public AffineTrans(AffineTrans a) { set(a); }
    public AffineTrans(int[][] a) { set(a); }
    public AffineTrans(int[] a) { set(a, 0); }
    public AffineTrans(int[] a, int offset) { set(a, offset); }

    public final void setIdentity() { set(4096, 0, 0, 0, 0, 4096, 0, 0, 0, 0, 4096, 0); }

    public final void set(int m00, int m01, int m02, int m03, int m10, int m11, int m12, int m13,
                          int m20, int m21, int m22, int m23) {
        this.m00 = m00; this.m01 = m01; this.m02 = m02; this.m03 = m03;
        this.m10 = m10; this.m11 = m11; this.m12 = m12; this.m13 = m13;
        this.m20 = m20; this.m21 = m21; this.m22 = m22; this.m23 = m23;
    }

    public final void set(AffineTrans a) {
        set(a.m00, a.m01, a.m02, a.m03, a.m10, a.m11, a.m12, a.m13, a.m20, a.m21, a.m22, a.m23);
    }

    public final void set(int[][] a) {
        if (a.length < 3 || a[0].length < 4 || a[1].length < 4 || a[2].length < 4) throw new IllegalArgumentException();
        set(a[0][0], a[0][1], a[0][2], a[0][3], a[1][0], a[1][1], a[1][2], a[1][3], a[2][0], a[2][1], a[2][2], a[2][3]);
    }

    public final void set(int[] a) { set(a, 0); }

    public final void set(int[] a, int o) {
        if (o < 0 || a.length - o < 12) throw new IllegalArgumentException();
        set(a[o], a[o + 1], a[o + 2], a[o + 3], a[o + 4], a[o + 5], a[o + 6], a[o + 7], a[o + 8], a[o + 9], a[o + 10], a[o + 11]);
    }

    public final void get(int[] a) { get(a, 0); }

    public final void get(int[] a, int o) {
        if (o < 0 || a.length - o < 12) throw new IllegalArgumentException();
        a[o] = m00; a[o + 1] = m01; a[o + 2] = m02; a[o + 3] = m03;
        a[o + 4] = m10; a[o + 5] = m11; a[o + 6] = m12; a[o + 7] = m13;
        a[o + 8] = m20; a[o + 9] = m21; a[o + 10] = m22; a[o + 11] = m23;
    }

    public final Vector3D transform(Vector3D v) {
        return new Vector3D(((v.x * m00 + v.y * m01 + v.z * m02 + 2048) >> 12) + m03,
                            ((v.x * m10 + v.y * m11 + v.z * m12 + 2048) >> 12) + m13,
                            ((v.x * m20 + v.y * m21 + v.z * m22 + 2048) >> 12) + m23);
    }
    public final Vector3D transPoint(Vector3D v) { return transform(v); }

    /** this = this * a */
    public final void multiply(AffineTrans a) { multiply(this, a); }
    public final void mul(AffineTrans a) { multiply(this, a); }
    public final void mul(AffineTrans a, AffineTrans b) { multiply(a, b); }

    /** this = a * b */
    public final void multiply(AffineTrans a, AffineTrans b) {
        int l00 = a.m00, l01 = a.m01, l02 = a.m02, l03 = a.m03, l10 = a.m10, l11 = a.m11, l12 = a.m12, l13 = a.m13,
            l20 = a.m20, l21 = a.m21, l22 = a.m22, l23 = a.m23;
        int r00 = b.m00, r01 = b.m01, r02 = b.m02, r03 = b.m03, r10 = b.m10, r11 = b.m11, r12 = b.m12, r13 = b.m13,
            r20 = b.m20, r21 = b.m21, r22 = b.m22, r23 = b.m23;
        m00 = (l00 * r00 + l01 * r10 + l02 * r20 + 2048) >> 12;
        m01 = (l00 * r01 + l01 * r11 + l02 * r21 + 2048) >> 12;
        m02 = (l00 * r02 + l01 * r12 + l02 * r22 + 2048) >> 12;
        m03 = ((l00 * r03 + l01 * r13 + l02 * r23 + 2048) >> 12) + l03;
        m10 = (l10 * r00 + l11 * r10 + l12 * r20 + 2048) >> 12;
        m11 = (l10 * r01 + l11 * r11 + l12 * r21 + 2048) >> 12;
        m12 = (l10 * r02 + l11 * r12 + l12 * r22 + 2048) >> 12;
        m13 = ((l10 * r03 + l11 * r13 + l12 * r23 + 2048) >> 12) + l13;
        m20 = (l20 * r00 + l21 * r10 + l22 * r20 + 2048) >> 12;
        m21 = (l20 * r01 + l21 * r11 + l22 * r21 + 2048) >> 12;
        m22 = (l20 * r02 + l21 * r12 + l22 * r22 + 2048) >> 12;
        m23 = ((l20 * r03 + l21 * r13 + l22 * r23 + 2048) >> 12) + l23;
    }

    /** The rotation setters leave the translation column alone; 4096 units make a full turn. */
    public final void setRotationX(int r) {
        int c = Util3D.cos(r), s = Util3D.sin(r);
        m00 = 4096; m01 = 0; m02 = 0;
        m10 = 0; m11 = c; m12 = -s;
        m20 = 0; m21 = s; m22 = c;
    }

    public final void setRotationY(int r) {
        int c = Util3D.cos(r), s = Util3D.sin(r);
        m00 = c; m01 = 0; m02 = s;
        m10 = 0; m11 = 4096; m12 = 0;
        m20 = -s; m21 = 0; m22 = c;
    }

    public final void setRotationZ(int r) {
        int c = Util3D.cos(r), s = Util3D.sin(r);
        m00 = c; m01 = -s; m02 = 0;
        m10 = s; m11 = c; m12 = 0;
        m20 = 0; m21 = 0; m22 = 4096;
    }

    /** Rotation about the axis v. */
    public final void setRotation(Vector3D v, int r) {
        double len = Math.sqrt((double) v.x * v.x + (double) v.y * v.y + (double) v.z * v.z);
        if (len == 0) { setRotationZ(0); return; }
        double x = v.x / len, y = v.y / len, z = v.z / len;
        double a = r * (Math.PI / 2048), s = Math.sin(a), c = Math.cos(a), n = 1 - c;
        m00 = fix(x * x * n + c); m01 = fix(x * y * n - z * s); m02 = fix(z * x * n + y * s);
        m10 = fix(x * y * n + z * s); m11 = fix(y * y * n + c); m12 = fix(y * z * n - x * s);
        m20 = fix(z * x * n - y * s); m21 = fix(y * z * n + x * s); m22 = fix(z * z * n + c);
    }

    private static int fix(double v) {
        v *= 4096;
        return (int) (v < 0 ? v - 0.5 : v + 0.5);
    }

    public final void rotationX(int r) { setRotationX(r); }
    public final void rotationY(int r) { setRotationY(r); }
    public final void rotationZ(int r) { setRotationZ(r); }
    public final void rotationV(Vector3D v, int r) { setRotation(v, r); }

    /** A camera at pos looking along the direction look, with up as the upward direction. */
    public final void setViewTrans(Vector3D pos, Vector3D look, Vector3D up) {
        Vector3D t = Vector3D.outerProduct(look, up);
        t.unit();
        m00 = t.x; m01 = t.y; m02 = t.z;
        m03 = (-pos.x * t.x - pos.y * t.y - pos.z * t.z + 2048) >> 12;
        t = Vector3D.outerProduct(look, t);
        t.unit();
        m10 = t.x; m11 = t.y; m12 = t.z;
        m13 = (-pos.x * t.x - pos.y * t.y - pos.z * t.z + 2048) >> 12;
        t.set(look);
        t.unit();
        m20 = t.x; m21 = t.y; m22 = t.z;
        m23 = (-pos.x * t.x - pos.y * t.y - pos.z * t.z + 2048) >> 12;
    }
    public final void lookAt(Vector3D pos, Vector3D look, Vector3D up) { setViewTrans(pos, look, up); }
}
