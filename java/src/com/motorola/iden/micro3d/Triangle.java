/*
 * amiga-j2me: Motorola iDEN micro3d API for the Amiga J2ME runtime, a layer
 * over the Mascot Capsule v3 implementation.
 * Written for this project; licensed under GPLv2 (see COPYING).
 */
package com.motorola.iden.micro3d;

import com.mascotcapsule.micro3d.v3.Graphics3D;

public class Triangle extends Primitive {
    private Triangle(Vector3D a, Vector3D b, Vector3D c, Layout3D layout) {
        super(3);
        setVertices(new Vector3D[] { a, b, c });
        this.layout = layout;
    }
    private void uv(int xa, int ya, int xb, int yb, int xc, int yc, Texture t) {
        tex[0] = xa; tex[1] = ya; tex[2] = xb; tex[3] = yb; tex[4] = xc; tex[5] = yc;
        setTexture(t);
    }
    private void face(Vector3D n) { if (n == null) throw new NullPointerException(); faceNormal = n; }

    public Triangle(Vector3D a, Vector3D b, Vector3D c, int xa, int ya, int xb, int yb, int xc, int yc, Layout3D layout, Texture texture) {
        this(a, b, c, layout); uv(xa, ya, xb, yb, xc, yc, texture);
    }
    public Triangle(Vector3D a, Vector3D b, Vector3D c, Layout3D layout, int color) {
        this(a, b, c, layout); this.color = color;
    }
    public Triangle(Vector3D a, Vector3D b, Vector3D c, Vector3D faceNormal, int xa, int ya, int xb, int yb, int xc, int yc,
                    Layout3D layout, Texture texture) {
        this(a, b, c, layout); face(faceNormal); uv(xa, ya, xb, yb, xc, yc, texture);
    }
    public Triangle(Vector3D a, Vector3D b, Vector3D c, Vector3D faceNormal, Layout3D layout, int color) {
        this(a, b, c, layout); face(faceNormal); this.color = color;
    }
    public Triangle(Vector3D a, Vector3D b, Vector3D c, Vector3D na, Vector3D nb, Vector3D nc,
                    int xa, int ya, int xb, int yb, int xc, int yc, Layout3D layout, Texture texture) {
        this(a, b, c, layout); setNormals(new Vector3D[] { na, nb, nc }); uv(xa, ya, xb, yb, xc, yc, texture);
    }
    public Triangle(Vector3D a, Vector3D b, Vector3D c, Vector3D na, Vector3D nb, Vector3D nc, Layout3D layout, int color) {
        this(a, b, c, layout); setNormals(new Vector3D[] { na, nb, nc }); this.color = color;
    }
    int type() { return Graphics3D.PRIMITVE_TRIANGLES; }
}
