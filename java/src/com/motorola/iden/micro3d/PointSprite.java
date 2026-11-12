/*
 * amiga-j2me: Motorola iDEN micro3d API for the Amiga J2ME runtime, a layer
 * over the Mascot Capsule v3 implementation.
 * Written for this project; licensed under GPLv2 (see COPYING).
 */
package com.motorola.iden.micro3d;

import com.mascotcapsule.micro3d.v3.Graphics3D;

/** A textured rectangle that always faces the camera. */
public class PointSprite extends Primitive {
    public static final int LOCAL_SIZE = 0, PIXEL_SIZE = 1, PERSPECTIVE_PROJECTION = 0, PARALLEL_PROJECTION = 2;
    private int width, height, rotation, displayType;

    public PointSprite(Vector3D vertexA, int width, int height, int rotation, int textureX, int textureY,
                       int textureWidth, int textureHeight, int displayType, Layout3D layout, Texture texture) {
        super(2);
        if (vertexA == null) throw new NullPointerException();
        vertices[0] = vertexA;
        vertices[1] = vertexA;
        this.width = width; this.height = height; this.rotation = rotation;
        tex[0] = textureX; tex[1] = textureY; tex[2] = textureX + textureWidth; tex[3] = textureY + textureHeight;
        setDisplayType(displayType);
        this.layout = layout;
        setTexture(texture);
    }

    public int getWidth() { return width; }
    public int getHeight() { return height; }
    public int getRotation() { return rotation; }
    public int getDisplayType() { return displayType; }
    public void setWidth(int w) { width = w; }
    public void setHeight(int h) { height = h; }
    public void setRotation(int r) { rotation = r; }
    public void setDisplayType(int t) {
        if ((t & ~3) != 0) throw new IllegalArgumentException();
        displayType = t;
    }
    public void setColor(int color) {}
    public Vector3D getVector(int id) {
        if (id != VERTEX_A) throw new IllegalArgumentException();
        return vertices[0];
    }
    public void setVector(int id, Vector3D v) {
        if (v == null) throw new NullPointerException();
        if (id != VERTEX_A) throw new IllegalArgumentException();
        vertices[0] = v;
    }

    int type() { return Graphics3D.PRIMITVE_POINT_SPRITES; }

    void draw(Graphics3D g, int x, int y, int w, int h) {
        com.mascotcapsule.micro3d.v3.Texture t = current();
        if (t == null) return;
        int cmd = Graphics3D.PRIMITVE_POINT_SPRITES | Graphics3D.PDATA_POINT_SPRITE_PARAMS_PER_CMD | ((blending - BLENDING_NONE) << 5);
        if (colorKey) cmd |= Graphics3D.PATTR_COLORKEY;
        int[] none = new int[0];
        g.renderPrimitives(t, x, y, layout.toV3(w, h), layout.effect(null), cmd, 1,
                           new int[] { vertices[0].x, vertices[0].y, vertices[0].z }, none,
                           new int[] { width, height, rotation, tex[0], tex[1], tex[2], tex[3], displayType }, none);
    }
}
