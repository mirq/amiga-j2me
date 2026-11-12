/*
 * amiga-j2me: Mascot Capsule micro3d v3 API for the Amiga J2ME runtime.
 * Written for this project; licensed under GPLv2 (see COPYING).
 */
package com.mascotcapsule.micro3d.v3;

public class Vector3D {
    public int x, y, z;

    public Vector3D() {}
    public Vector3D(int x, int y, int z) { this.x = x; this.y = y; this.z = z; }
    public Vector3D(Vector3D v) { x = v.x; y = v.y; z = v.z; }

    public final int getX() { return x; }
    public final int getY() { return y; }
    public final int getZ() { return z; }
    public final void setX(int x) { this.x = x; }
    public final void setY(int y) { this.y = y; }
    public final void setZ(int z) { this.z = z; }
    public final void set(int x, int y, int z) { this.x = x; this.y = y; this.z = z; }
    public final void set(Vector3D v) { x = v.x; y = v.y; z = v.z; }

    /** Makes the length 4096. */
    public final void unit() {
        double fx = x, fy = y, fz = z, len = Math.sqrt(fx * fx + fy * fy + fz * fz);
        if (len == 0) { x = 0; y = 0; z = 4096; return; }
        x = (int) (fx * 4096 / len);
        y = (int) (fy * 4096 / len);
        z = (int) (fz * 4096 / len);
    }

    public final int innerProduct(Vector3D v) { return x * v.x + y * v.y + z * v.z; }
    public static int innerProduct(Vector3D a, Vector3D b) { return a.innerProduct(b); }

    public final void outerProduct(Vector3D v) {
        int ax = x, ay = y, az = z;
        x = ay * v.z - az * v.y;
        y = az * v.x - ax * v.z;
        z = ax * v.y - ay * v.x;
    }

    public static Vector3D outerProduct(Vector3D a, Vector3D b) {
        Vector3D r = new Vector3D(a);
        r.outerProduct(b);
        return r;
    }
}
