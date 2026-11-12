/*
 * amiga-j2me: Motorola iDEN micro3d API for the Amiga J2ME runtime, a layer
 * over the Mascot Capsule v3 implementation.
 * Written for this project; licensed under GPLv2 (see COPYING).
 */
package com.motorola.iden.micro3d;

import com.mascotcapsule.micro3d.v3.Graphics3D;

public abstract class Primitive extends Object3D {
    public static final int BLENDING_NONE = 200, BLENDING_HALF = 201, BLENDING_ADD = 202, BLENDING_SUB = 203;
    public static final int VERTEX_A = 1, VERTEX_B = 2, VERTEX_C = 3, VERTEX_D = 4, NORMAL = 16, FACE_NORMAL = 32;

    final Vector3D[] vertices, normals;
    Vector3D faceNormal;
    final int[] tex;
    int color, blending = BLENDING_NONE;
    boolean colorKey;

    protected Primitive(int size) {
        vertices = new Vector3D[size];
        normals = new Vector3D[size];
        tex = new int[size * 2];
    }

    public void enableColorKeyTransparency(boolean enable) { colorKey = enable; }
    public boolean hasColorKeyTransparency() { return colorKey; }
    public int getBlendingType() { return blending; }
    public void setBlendingType(int b) {
        if (b < BLENDING_NONE || b > BLENDING_SUB) throw new IllegalArgumentException();
        blending = b;
    }
    public int getColor() { return color; }
    public void setColor(int color) { this.color = color; }

    public Vector3D getVector(int id) {
        if (id == FACE_NORMAL) return faceNormal;
        int i = (id & 15) - 1;
        if (i < 0 || i >= vertices.length || (id & ~(NORMAL | 15)) != 0) throw new IllegalArgumentException();
        return (id & NORMAL) != 0 ? normals[i] : vertices[i];
    }

    public void setVector(int id, Vector3D v) {
        if (v == null) throw new NullPointerException();
        if (id == FACE_NORMAL) { faceNormal = v; return; }
        int i = (id & 15) - 1;
        if (i < 0 || i >= vertices.length || (id & ~(NORMAL | 15)) != 0) throw new IllegalArgumentException();
        if ((id & NORMAL) != 0) normals[i] = v; else vertices[i] = v;
    }

    public int getTextureCoordinateX(int vertexID) { return tex[index(vertexID) * 2]; }
    public int getTextureCoordinateY(int vertexID) { return tex[index(vertexID) * 2 + 1]; }
    public void setTextureCoordinates(int vertexID, int x, int y) {
        int i = index(vertexID);
        tex[i * 2] = x; tex[i * 2 + 1] = y;
    }
    private int index(int vertexID) {
        if (vertexID < 1 || vertexID > vertices.length) throw new IllegalArgumentException();
        return vertexID - 1;
    }

    void setVertices(Vector3D[] v) {
        for (int i = 0; i < v.length; i++) {
            if (v[i] == null) throw new NullPointerException();
            vertices[i] = v[i];
        }
    }
    void setNormals(Vector3D[] n) {
        for (int i = 0; i < n.length; i++) {
            if (n[i] == null) throw new NullPointerException();
            normals[i] = n[i];
        }
    }

    /** The v3 primitive type; triangles and quadrangles share the rest. */
    abstract int type();

    void draw(Graphics3D g, int x, int y, int w, int h) {
        int n = vertices.length, cmd = type() | ((blending - BLENDING_NONE) << 5) | Graphics3D.PATTR_SPHERE_MAP;
        int[] v = new int[n * 3], nr = new int[0], col = { color };
        for (int i = 0; i < n; i++) { v[i * 3] = vertices[i].x; v[i * 3 + 1] = vertices[i].y; v[i * 3 + 2] = vertices[i].z; }
        boolean perVertex = true;
        for (int i = 0; i < n; i++) if (normals[i] == null) perVertex = false;
        if (faceNormal != null) {
            cmd |= Graphics3D.PDATA_NORMAL_PER_FACE | Graphics3D.PATTR_LIGHTING;
            nr = new int[] { faceNormal.x, faceNormal.y, faceNormal.z };
        } else if (perVertex && n >= 3) {
            cmd |= Graphics3D.PDATA_NORMAL_PER_VERTEX | Graphics3D.PATTR_LIGHTING;
            nr = new int[n * 3];
            for (int i = 0; i < n; i++) { nr[i * 3] = normals[i].x; nr[i * 3 + 1] = normals[i].y; nr[i * 3 + 2] = normals[i].z; }
        }
        com.mascotcapsule.micro3d.v3.Texture t = n >= 3 ? current() : null;
        if (t != null) cmd |= Graphics3D.PDATA_TEXURE_COORD; else cmd |= Graphics3D.PDATA_COLOR_PER_COMMAND;
        if (colorKey) cmd |= Graphics3D.PATTR_COLORKEY;
        g.renderPrimitives(t, x, y, layout.toV3(w, h), layout.effect(sphereTexture), cmd, 1, v, nr, tex, col);
    }
}
