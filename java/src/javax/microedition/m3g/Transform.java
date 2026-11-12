/*
 * amiga-j2me: JSR-184 (M3G 1.0/1.1) implementation for the Amiga J2ME runtime.
 * Written for this project; licensed under GPLv2 (see COPYING).
 */
package javax.microedition.m3g;

/** A 4x4 matrix, row-major: m[row * 4 + column]. */
public class Transform {
    final float[] m = new float[16];

    public Transform() { setIdentity(); }
    public Transform(Transform t) { set(t); }

    public void setIdentity() {
        for (int i = 0; i < 16; i++) m[i] = (i % 5 == 0) ? 1 : 0;
    }
    public void set(Transform t) { System.arraycopy(t.m, 0, m, 0, 16); }
    public void set(float[] matrix) {
        if (matrix.length < 16) throw new IllegalArgumentException();
        System.arraycopy(matrix, 0, m, 0, 16);
    }
    public void get(float[] matrix) {
        if (matrix.length < 16) throw new IllegalArgumentException();
        System.arraycopy(m, 0, matrix, 0, 16);
    }

    public void transpose() {
        for (int r = 0; r < 4; r++) {
            for (int c = r + 1; c < 4; c++) {
                float t = m[r * 4 + c];
                m[r * 4 + c] = m[c * 4 + r];
                m[c * 4 + r] = t;
            }
        }
    }

    /** this = this * t */
    public void postMultiply(Transform t) { mul(m, t.m); }

    static void mul(float[] a, float[] b) {
        float[] r = new float[16];
        mul(a, b, r);
        System.arraycopy(r, 0, a, 0, 16);
    }

    static void mul(float[] a, float[] b, float[] r) {
        for (int i = 0; i < 4; i++) {
            float a0 = a[i * 4], a1 = a[i * 4 + 1], a2 = a[i * 4 + 2], a3 = a[i * 4 + 3];
            r[i * 4]     = a0 * b[0] + a1 * b[4] + a2 * b[8]  + a3 * b[12];
            r[i * 4 + 1] = a0 * b[1] + a1 * b[5] + a2 * b[9]  + a3 * b[13];
            r[i * 4 + 2] = a0 * b[2] + a1 * b[6] + a2 * b[10] + a3 * b[14];
            r[i * 4 + 3] = a0 * b[3] + a1 * b[7] + a2 * b[11] + a3 * b[15];
        }
    }

    public void postScale(float sx, float sy, float sz) {
        for (int r = 0; r < 4; r++) { m[r * 4] *= sx; m[r * 4 + 1] *= sy; m[r * 4 + 2] *= sz; }
    }

    public void postTranslate(float tx, float ty, float tz) {
        for (int r = 0; r < 4; r++) m[r * 4 + 3] += m[r * 4] * tx + m[r * 4 + 1] * ty + m[r * 4 + 2] * tz;
    }

    public void postRotate(float angle, float ax, float ay, float az) {
        if (angle == 0) return;
        float len = (float) Math.sqrt(ax * ax + ay * ay + az * az);
        if (len == 0) throw new IllegalArgumentException();
        double rad = angle * (Math.PI / 360.0);         // half the angle
        float s = (float) Math.sin(rad) / len;
        postRotateQuat(ax * s, ay * s, az * s, (float) Math.cos(rad));
    }

    public void postRotateQuat(float qx, float qy, float qz, float qw) {
        float[] r = new float[16];
        quatToMatrix(qx, qy, qz, qw, r);
        mul(m, r);
    }

    static void quatToMatrix(float x, float y, float z, float w, float[] r) {
        float n = x * x + y * y + z * z + w * w;
        float s = n > 0 ? 2 / n : 0;
        float xx = x * x * s, yy = y * y * s, zz = z * z * s, xy = x * y * s, xz = x * z * s, yz = y * z * s,
              wx = w * x * s, wy = w * y * s, wz = w * z * s;
        r[0] = 1 - (yy + zz); r[1] = xy - wz;       r[2] = xz + wy;       r[3] = 0;
        r[4] = xy + wz;       r[5] = 1 - (xx + zz); r[6] = yz - wx;       r[7] = 0;
        r[8] = xz - wy;       r[9] = yz + wx;       r[10] = 1 - (xx + yy); r[11] = 0;
        r[12] = 0; r[13] = 0; r[14] = 0; r[15] = 1;
    }

    public void invert() {
        float[] inv = new float[16];
        if (!invert(m, inv)) throw new ArithmeticException("singular transform");
        System.arraycopy(inv, 0, m, 0, 16);
    }

    /** General 4x4 inverse by cofactors. Returns false when the matrix is singular. */
    static boolean invert(float[] a, float[] inv) {
        float[] c = new float[16];
        c[0] = a[5] * a[10] * a[15] - a[5] * a[11] * a[14] - a[9] * a[6] * a[15] + a[9] * a[7] * a[14] + a[13] * a[6] * a[11] - a[13] * a[7] * a[10];
        c[4] = -a[4] * a[10] * a[15] + a[4] * a[11] * a[14] + a[8] * a[6] * a[15] - a[8] * a[7] * a[14] - a[12] * a[6] * a[11] + a[12] * a[7] * a[10];
        c[8] = a[4] * a[9] * a[15] - a[4] * a[11] * a[13] - a[8] * a[5] * a[15] + a[8] * a[7] * a[13] + a[12] * a[5] * a[11] - a[12] * a[7] * a[9];
        c[12] = -a[4] * a[9] * a[14] + a[4] * a[10] * a[13] + a[8] * a[5] * a[14] - a[8] * a[6] * a[13] - a[12] * a[5] * a[10] + a[12] * a[6] * a[9];
        c[1] = -a[1] * a[10] * a[15] + a[1] * a[11] * a[14] + a[9] * a[2] * a[15] - a[9] * a[3] * a[14] - a[13] * a[2] * a[11] + a[13] * a[3] * a[10];
        c[5] = a[0] * a[10] * a[15] - a[0] * a[11] * a[14] - a[8] * a[2] * a[15] + a[8] * a[3] * a[14] + a[12] * a[2] * a[11] - a[12] * a[3] * a[10];
        c[9] = -a[0] * a[9] * a[15] + a[0] * a[11] * a[13] + a[8] * a[1] * a[15] - a[8] * a[3] * a[13] - a[12] * a[1] * a[11] + a[12] * a[3] * a[9];
        c[13] = a[0] * a[9] * a[14] - a[0] * a[10] * a[13] - a[8] * a[1] * a[14] + a[8] * a[2] * a[13] + a[12] * a[1] * a[10] - a[12] * a[2] * a[9];
        c[2] = a[1] * a[6] * a[15] - a[1] * a[7] * a[14] - a[5] * a[2] * a[15] + a[5] * a[3] * a[14] + a[13] * a[2] * a[7] - a[13] * a[3] * a[6];
        c[6] = -a[0] * a[6] * a[15] + a[0] * a[7] * a[14] + a[4] * a[2] * a[15] - a[4] * a[3] * a[14] - a[12] * a[2] * a[7] + a[12] * a[3] * a[6];
        c[10] = a[0] * a[5] * a[15] - a[0] * a[7] * a[13] - a[4] * a[1] * a[15] + a[4] * a[3] * a[13] + a[12] * a[1] * a[7] - a[12] * a[3] * a[5];
        c[14] = -a[0] * a[5] * a[14] + a[0] * a[6] * a[13] + a[4] * a[1] * a[14] - a[4] * a[2] * a[13] - a[12] * a[1] * a[6] + a[12] * a[2] * a[5];
        c[3] = -a[1] * a[6] * a[11] + a[1] * a[7] * a[10] + a[5] * a[2] * a[11] - a[5] * a[3] * a[10] - a[9] * a[2] * a[7] + a[9] * a[3] * a[6];
        c[7] = a[0] * a[6] * a[11] - a[0] * a[7] * a[10] - a[4] * a[2] * a[11] + a[4] * a[3] * a[10] + a[8] * a[2] * a[7] - a[8] * a[3] * a[6];
        c[11] = -a[0] * a[5] * a[11] + a[0] * a[7] * a[9] + a[4] * a[1] * a[11] - a[4] * a[3] * a[9] - a[8] * a[1] * a[7] + a[8] * a[3] * a[5];
        c[15] = a[0] * a[5] * a[10] - a[0] * a[6] * a[9] - a[4] * a[1] * a[10] + a[4] * a[2] * a[9] + a[8] * a[1] * a[6] - a[8] * a[2] * a[5];
        float det = a[0] * c[0] + a[1] * c[4] + a[2] * c[8] + a[3] * c[12];
        if (det == 0) return false;
        det = 1 / det;
        for (int i = 0; i < 16; i++) inv[i] = c[i] * det;
        return true;
    }

    /** Transforms an array of 4-component vectors in place. */
    public void transform(float[] vectors) {
        if (vectors.length % 4 != 0) throw new IllegalArgumentException();
        for (int i = 0; i < vectors.length; i += 4) {
            float x = vectors[i], y = vectors[i + 1], z = vectors[i + 2], w = vectors[i + 3];
            vectors[i]     = m[0] * x + m[1] * y + m[2] * z + m[3] * w;
            vectors[i + 1] = m[4] * x + m[5] * y + m[6] * z + m[7] * w;
            vectors[i + 2] = m[8] * x + m[9] * y + m[10] * z + m[11] * w;
            vectors[i + 3] = m[12] * x + m[13] * y + m[14] * z + m[15] * w;
        }
    }

    public void transform(VertexArray in, float[] out, boolean W) {
        int n = in.getVertexCount(), comps = in.getComponentCount();
        if (out.length < n * 4) throw new IllegalArgumentException();
        for (int i = 0; i < n; i++) {
            float x = in.component(i, 0), y = in.component(i, 1), z = comps > 2 ? in.component(i, 2) : 0;
            float w = comps > 3 ? in.component(i, 3) : (W ? 1 : 0);
            out[i * 4]     = m[0] * x + m[1] * y + m[2] * z + m[3] * w;
            out[i * 4 + 1] = m[4] * x + m[5] * y + m[6] * z + m[7] * w;
            out[i * 4 + 2] = m[8] * x + m[9] * y + m[10] * z + m[11] * w;
            out[i * 4 + 3] = m[12] * x + m[13] * y + m[14] * z + m[15] * w;
        }
    }
}
