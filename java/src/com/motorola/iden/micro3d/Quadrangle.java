/*
 * amiga-j2me: Motorola iDEN micro3d API for the Amiga J2ME runtime, a layer
 * over the Mascot Capsule v3 implementation.
 * Written for this project; licensed under GPLv2 (see COPYING).
 */
package com.motorola.iden.micro3d;

import com.mascotcapsule.micro3d.v3.Graphics3D;

public class Quadrangle extends Primitive {
    private Quadrangle(Vector3D a, Vector3D b, Vector3D c, Vector3D d, Layout3D layout) {
        super(4);
        setVertices(new Vector3D[] { a, b, c, d });
        this.layout = layout;
    }
    private void uv(int xa, int ya, int xb, int yb, int xc, int yc, int xd, int yd, Texture t) {
        tex[0] = xa; tex[1] = ya; tex[2] = xb; tex[3] = yb; tex[4] = xc; tex[5] = yc; tex[6] = xd; tex[7] = yd;
        setTexture(t);
    }
    private void face(Vector3D n) { if (n == null) throw new NullPointerException(); faceNormal = n; }

    public Quadrangle(Vector3D a, Vector3D b, Vector3D c, Vector3D d, int xa, int ya, int xb, int yb, int xc, int yc,
                      int xd, int yd, Layout3D layout, Texture texture) {
        this(a, b, c, d, layout); uv(xa, ya, xb, yb, xc, yc, xd, yd, texture);
    }
    public Quadrangle(Vector3D a, Vector3D b, Vector3D c, Vector3D d, Layout3D layout, int color) {
        this(a, b, c, d, layout); this.color = color;
    }
    public Quadrangle(Vector3D a, Vector3D b, Vector3D c, Vector3D d, Vector3D faceNormal, int xa, int ya, int xb, int yb,
                      int xc, int yc, int xd, int yd, Layout3D layout, Texture texture) {
        this(a, b, c, d, layout); face(faceNormal); uv(xa, ya, xb, yb, xc, yc, xd, yd, texture);
    }
    public Quadrangle(Vector3D a, Vector3D b, Vector3D c, Vector3D d, Vector3D faceNormal, Layout3D layout, int color) {
        this(a, b, c, d, layout); face(faceNormal); this.color = color;
    }
    public Quadrangle(Vector3D a, Vector3D b, Vector3D c, Vector3D d, Vector3D na, Vector3D nb, Vector3D nc, Vector3D nd,
                      int xa, int ya, int xb, int yb, int xc, int yc, int xd, int yd, Layout3D layout, Texture texture) {
        this(a, b, c, d, layout); setNormals(new Vector3D[] { na, nb, nc, nd }); uv(xa, ya, xb, yb, xc, yc, xd, yd, texture);
    }
    public Quadrangle(Vector3D a, Vector3D b, Vector3D c, Vector3D d, Vector3D na, Vector3D nb, Vector3D nc, Vector3D nd,
                      Layout3D layout, int color) {
        this(a, b, c, d, layout); setNormals(new Vector3D[] { na, nb, nc, nd }); this.color = color;
    }
    int type() { return Graphics3D.PRIMITVE_QUADS; }
}
