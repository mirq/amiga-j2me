/*
 * amiga-j2me: JSR-184 (M3G 1.0/1.1) implementation for the Amiga J2ME runtime.
 * Written for this project; licensed under GPLv2 (see COPYING).
 */
package javax.microedition.m3g;

/** Result of Group.pick, which is not implemented: never filled in. */
public class RayIntersection {
    public RayIntersection() {}
    public Node getIntersected() { return null; }
    public void getRay(float[] ray) {}
    public float getDistance() { return 0; }
    public int getSubmeshIndex() { return 0; }
    public float getTextureS(int index) { return 0; }
    public float getTextureT(int index) { return 0; }
    public float getNormalX() { return 0; }
    public float getNormalY() { return 0; }
    public float getNormalZ() { return 1; }
}
