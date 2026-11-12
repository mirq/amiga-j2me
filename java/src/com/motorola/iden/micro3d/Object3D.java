/*
 * amiga-j2me: Motorola iDEN micro3d API for the Amiga J2ME runtime, a layer
 * over the Mascot Capsule v3 implementation.
 * Written for this project; licensed under GPLv2 (see COPYING).
 */
package com.motorola.iden.micro3d;

public abstract class Object3D {
    Layout3D layout;
    Texture texture, sphereTexture;

    Object3D() {}
    public Layout3D getLayout() { return layout; }
    public void setLayout(Layout3D layout) { this.layout = layout; }
    public Texture getTexture() { return texture; }
    public Texture getSphereTexture() { return sphereTexture; }
    public void setSphereTexture(Texture t) {
        if (t != null && !t.isSphereTexture()) throw new IllegalArgumentException();
        sphereTexture = t;
    }
    public void setTexture(Texture t) {
        if (t != null && t.isSphereTexture()) throw new IllegalArgumentException();
        texture = t;
    }

    /** The model texture to draw with now (the current one of a MultiTexture). */
    com.mascotcapsule.micro3d.v3.Texture current() {
        Texture t = texture instanceof MultiTexture ? ((MultiTexture) texture).getCurrentTexture() : texture;
        return t == null ? null : t.model;
    }
}
