/*
 * amiga-j2me: Motorola iDEN micro3d API for the Amiga J2ME runtime, a layer
 * over the Mascot Capsule v3 implementation.
 * Written for this project; licensed under GPLv2 (see COPYING).
 */
package com.motorola.iden.micro3d;

public class Vector3D {
    public static final int MAX_VALUE = 32767, MIN_VALUE = -32768, SIZE_OF_VECTOR3D = 3;
    int x, y, z;

    public Vector3D() {}
    public Vector3D(int x, int y, int z) { this.x = x; this.y = y; this.z = z; }
    public Vector3D(Vector3D v) { x = v.x; y = v.y; z = v.z; }

    public int getX() { return x; }
    public int getY() { return y; }
    public int getZ() { return z; }
    public void setX(int x) { this.x = x; }
    public void setY(int y) { this.y = y; }
    public void setZ(int z) { this.z = z; }
    public void set(int x, int y, int z) { this.x = x; this.y = y; this.z = z; }
    public void set(Vector3D v) { x = v.x; y = v.y; z = v.z; }
    public int innerProduct(Vector3D m) { return x * m.x + y * m.y + z * m.z; }

    public void outerProduct(Vector3D m) {
        int ax = x, ay = y, az = z;
        x = ay * m.z - az * m.y;
        y = az * m.x - ax * m.z;
        z = ax * m.y - ay * m.x;
    }

    public static Vector3D outerProduct(Vector3D a, Vector3D b) {
        Vector3D r = new Vector3D(a);
        r.outerProduct(b);
        return r;
    }

    /** Makes the length 4096. */
    public void normalize() {
        com.mascotcapsule.micro3d.v3.Vector3D v = v3();
        v.unit();
        x = v.x; y = v.y; z = v.z;
    }

    public static Vector3D normalize(Vector3D v) {
        Vector3D r = new Vector3D(v);
        r.normalize();
        return r;
    }

    boolean isZero() { return x == 0 && y == 0 && z == 0; }
    com.mascotcapsule.micro3d.v3.Vector3D v3() { return new com.mascotcapsule.micro3d.v3.Vector3D(x, y, z); }
}
