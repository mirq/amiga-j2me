/*
 * amiga-j2me: Mascot Capsule micro3d v3 API for the Amiga J2ME runtime.
 * Written for this project; licensed under GPLv2 (see COPYING).
 */
package com.mascotcapsule.micro3d.v3;

public class Light {
    private Vector3D dir;
    private int dirIntensity, ambIntensity;

    public Light() { this(new Vector3D(0, 0, 4096), 4096, 0); }
    public Light(Vector3D dir, int dirIntensity, int ambIntensity) {
        if (dir == null) throw new NullPointerException();
        this.dir = dir; this.dirIntensity = dirIntensity; this.ambIntensity = ambIntensity;
    }

    public final int getAmbientIntensity() { return ambIntensity; }
    public final int getAmbIntensity() { return ambIntensity; }
    public Vector3D getDirection() { return dir; }
    public final Vector3D getParallelLightDirection() { return dir; }
    public final int getDirIntensity() { return dirIntensity; }
    public final int getParallelLightIntensity() { return dirIntensity; }
    public final void setAmbientIntensity(int p) { ambIntensity = p; }
    public final void setAmbIntensity(int p) { ambIntensity = p; }
    public final void setDirIntensity(int p) { dirIntensity = p; }
    public final void setParallelLightIntensity(int p) { dirIntensity = p; }
    public final void setDirection(Vector3D v) { if (v == null) throw new NullPointerException(); dir = v; }
    public final void setParallelLightDirection(Vector3D v) { setDirection(v); }
}
