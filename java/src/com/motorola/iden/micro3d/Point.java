/*
 * amiga-j2me: Motorola iDEN micro3d API for the Amiga J2ME runtime, a layer
 * over the Mascot Capsule v3 implementation.
 * Written for this project; licensed under GPLv2 (see COPYING).
 */
package com.motorola.iden.micro3d;

import com.mascotcapsule.micro3d.v3.Graphics3D;

public class Point extends Primitive {
    public Point(Vector3D vertexA, Layout3D layout, int color) {
        super(1);
        setVertices(new Vector3D[] { vertexA });
        this.layout = layout;
        this.color = color;
    }
    int type() { return Graphics3D.PRIMITVE_POINTS; }
}
