/*
 * amiga-j2me: JSR-184 (M3G 1.0/1.1) implementation for the Amiga J2ME runtime.
 * Written for this project; licensed under GPLv2 (see COPYING).
 */
package javax.microedition.m3g;

/** What Group.pick found. */
public class RayIntersection {
    Node node;
    final float[] ray = new float[6];       // origin, direction (in the picking group's coordinates)
    float distance;
    int submesh;
    final float[] texS = new float[2], texT = new float[2];
    final float[] normal = { 0, 0, 1 };

    public RayIntersection() {}

    public Node getIntersected() { return node; }
    public void getRay(float[] r) {
        if (r.length < 6) throw new IllegalArgumentException();
        System.arraycopy(ray, 0, r, 0, 6);
    }
    public float getDistance() { return distance; }
    public int getSubmeshIndex() { return submesh; }
    public float getTextureS(int index) {
        if (index < 0 || index > 1) throw new IndexOutOfBoundsException();
        return texS[index];
    }
    public float getTextureT(int index) {
        if (index < 0 || index > 1) throw new IndexOutOfBoundsException();
        return texT[index];
    }
    public float getNormalX() { return normal[0]; }
    public float getNormalY() { return normal[1]; }
    public float getNormalZ() { return normal[2]; }
}
