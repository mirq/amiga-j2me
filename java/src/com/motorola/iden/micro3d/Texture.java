/*
 * amiga-j2me: Motorola iDEN micro3d API for the Amiga J2ME runtime, a layer
 * over the Mascot Capsule v3 implementation.
 * Written for this project; licensed under GPLv2 (see COPYING).
 */
package com.motorola.iden.micro3d;

import java.io.IOException;

public class Texture {
    com.mascotcapsule.micro3d.v3.Texture model, sphere;    // one of the two
    private final boolean isSphere;

    Texture() { isSphere = false; }

    public Texture(byte[] data, int offset, int length, boolean sphereTexture) throws IOException {
        if (data == null) throw new NullPointerException();
        if (offset < 0 || length < 0 || offset + length > data.length) throw new ArrayIndexOutOfBoundsException();
        byte[] d = new byte[length];
        System.arraycopy(data, offset, d, 0, length);
        isSphere = sphereTexture;
        try {
            if (sphereTexture) sphere = new com.mascotcapsule.micro3d.v3.Texture(d, false);
            else model = new com.mascotcapsule.micro3d.v3.Texture(d, true);
        } catch (RuntimeException e) {
            throw new IOException(e.getMessage());
        }
    }

    public static Texture createTexture(String name, boolean sphereTexture) throws IOException {
        byte[] d = Figure.resource(name);
        return new Texture(d, 0, d.length, sphereTexture);
    }

    public boolean isSphereTexture() { return isSphere; }
}
