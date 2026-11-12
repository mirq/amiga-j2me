/*
 * amiga-j2me: JSR-184 (M3G 1.0/1.1) implementation for the Amiga J2ME runtime.
 * Written for this project; licensed under GPLv2 (see COPYING).
 */
package javax.microedition.m3g;

import java.util.Vector;

public class Group extends Node {
    final Vector children = new Vector();

    public Group() {}

    public void addChild(Node child) {
        if (child == null) throw new NullPointerException();
        if (child == this || child instanceof World || (child.parent != null && child.parent != this)) {
            throw new IllegalArgumentException();
        }
        if (child.parent == null) {
            child.parent = this;
            children.addElement(child);
        }
    }

    public void removeChild(Node child) {
        if (child != null && children.removeElement(child)) child.parent = null;
    }

    public int getChildCount() { return children.size(); }
    public Node getChild(int index) { return (Node) children.elementAt(index); }

    /**
     * The nearest triangle of a pickable mesh under this group that the ray
     * hits. Distances are in units of the direction vector's length. Sprites
     * are not picked.
     */
    public boolean pick(int scope, float ox, float oy, float oz, float dx, float dy, float dz, RayIntersection ri) {
        if (dx == 0 && dy == 0 && dz == 0) throw new IllegalArgumentException();
        float[] best = { Float.MAX_VALUE };
        RayIntersection found = new RayIntersection();
        pickIn(this, new Transform(), scope, new float[] { ox, oy, oz, dx, dy, dz }, best, found);
        if (found.node == null) return false;
        if (ri != null) {
            ri.node = found.node; ri.distance = found.distance; ri.submesh = found.submesh;
            ri.ray[0] = ox; ri.ray[1] = oy; ri.ray[2] = oz; ri.ray[3] = dx; ri.ray[4] = dy; ri.ray[5] = dz;
            System.arraycopy(found.texS, 0, ri.texS, 0, 2);
            System.arraycopy(found.texT, 0, ri.texT, 0, 2);
            System.arraycopy(found.normal, 0, ri.normal, 0, 3);
        }
        return true;
    }

    /** x, y: a point of the viewport, 0..1 from its top-left corner. Distance 0 is the near plane, 1 the far. */
    public boolean pick(int scope, float x, float y, Camera camera, RayIntersection ri) {
        if (camera == null) throw new NullPointerException();
        Transform toGroup = new Transform();
        if (!camera.getTransformTo(this, toGroup)) throw new IllegalStateException("the camera is not under this group's root");
        Transform proj = new Transform();
        camera.getProjection(proj);
        float[] inv = new float[16];
        if (!Transform.invert(proj.m, inv)) throw new ArithmeticException("singular projection");
        // the near and far points under (x, y), from normalised device coordinates back to the camera
        float[] p = { 2 * x - 1, 1 - 2 * y, -1, 1, 2 * x - 1, 1 - 2 * y, 1, 1 };
        Transform t = new Transform();
        t.set(inv);
        t.transform(p);
        for (int i = 0; i < 8; i += 4) {
            float w = p[i + 3] == 0 ? 1 : p[i + 3];
            p[i] /= w; p[i + 1] /= w; p[i + 2] /= w; p[i + 3] = 1;
        }
        toGroup.transform(p);
        return pick(scope, p[0], p[1], p[2], p[4] - p[0], p[5] - p[1], p[6] - p[2], ri);
    }

    private static void pickIn(Node node, Transform toGroup, int scope, float[] ray, float[] best, RayIntersection out) {
        if (!node.pickingEnable) return;
        if (node instanceof Mesh) {
            if ((node.scope & scope) != 0) pickMesh((Mesh) node, toGroup, ray, best, out);
        } else if (node instanceof Group) {
            Group g = (Group) node;
            Transform child = new Transform();
            for (int i = 0; i < g.children.size(); i++) {
                Node c = (Node) g.children.elementAt(i);
                c.getCompositeTransform(child);
                Transform t = new Transform(toGroup);
                t.postMultiply(child);
                pickIn(c, t, scope, ray, best, out);
            }
        }
    }

    private static void pickMesh(Mesh m, Transform toGroup, float[] ray, float[] best, RayIntersection out) {
        VertexBuffer vb = m.vertices;
        VertexArray pos = vb.positions;
        float[] inv = new float[16];
        if (pos == null || !Transform.invert(toGroup.m, inv)) return;
        // the ray in the mesh's own coordinates (the parameter along it stays the same)
        float ox = inv[0] * ray[0] + inv[1] * ray[1] + inv[2] * ray[2] + inv[3];
        float oy = inv[4] * ray[0] + inv[5] * ray[1] + inv[6] * ray[2] + inv[7];
        float oz = inv[8] * ray[0] + inv[9] * ray[1] + inv[10] * ray[2] + inv[11];
        float dx = inv[0] * ray[3] + inv[1] * ray[4] + inv[2] * ray[5];
        float dy = inv[4] * ray[3] + inv[5] * ray[4] + inv[6] * ray[5];
        float dz = inv[8] * ray[3] + inv[9] * ray[4] + inv[10] * ray[5];
        float sc = vb.posScale, bx = vb.posBias[0], by = vb.posBias[1], bz = vb.posBias[2];
        for (int sm = 0; sm < m.submeshes.length; sm++) {
            int[] tri = m.submeshes[sm].triangles;
            for (int i = 0; i + 2 < tri.length; i += 3) {
                int a = tri[i], b = tri[i + 1], c = tri[i + 2];
                if (a >= pos.count || b >= pos.count || c >= pos.count) continue;
                float ax = pos.component(a, 0) * sc + bx, ay = pos.component(a, 1) * sc + by, az = pos.component(a, 2) * sc + bz;
                float e1x = pos.component(b, 0) * sc + bx - ax, e1y = pos.component(b, 1) * sc + by - ay, e1z = pos.component(b, 2) * sc + bz - az;
                float e2x = pos.component(c, 0) * sc + bx - ax, e2y = pos.component(c, 1) * sc + by - ay, e2z = pos.component(c, 2) * sc + bz - az;
                // Moeller-Trumbore
                float px = dy * e2z - dz * e2y, py = dz * e2x - dx * e2z, pz = dx * e2y - dy * e2x;
                float det = e1x * px + e1y * py + e1z * pz;
                if (det > -1e-9f && det < 1e-9f) continue;
                float id = 1 / det;
                float tx = ox - ax, ty = oy - ay, tz = oz - az;
                float u = (tx * px + ty * py + tz * pz) * id;
                if (u < 0 || u > 1) continue;
                float qx = ty * e1z - tz * e1y, qy = tz * e1x - tx * e1z, qz = tx * e1y - ty * e1x;
                float v = (dx * qx + dy * qy + dz * qz) * id;
                if (v < 0 || u + v > 1) continue;
                float t = (e2x * qx + e2y * qy + e2z * qz) * id;
                if (t <= 0 || t >= best[0]) continue;
                best[0] = t;
                out.node = m; out.distance = t; out.submesh = sm;
                float w = 1 - u - v;
                VertexArray nrm = vb.normals;
                float nx, ny, nz;
                if (nrm != null) {
                    nx = w * nrm.component(a, 0) + u * nrm.component(b, 0) + v * nrm.component(c, 0);
                    ny = w * nrm.component(a, 1) + u * nrm.component(b, 1) + v * nrm.component(c, 1);
                    nz = w * nrm.component(a, 2) + u * nrm.component(b, 2) + v * nrm.component(c, 2);
                } else {
                    nx = e1y * e2z - e1z * e2y; ny = e1z * e2x - e1x * e2z; nz = e1x * e2y - e1y * e2x;
                }
                float len = (float) Math.sqrt(nx * nx + ny * ny + nz * nz);
                if (len > 0) { nx /= len; ny /= len; nz /= len; }
                out.normal[0] = nx; out.normal[1] = ny; out.normal[2] = nz;
                for (int unit = 0; unit < 2; unit++) {
                    VertexArray tc = vb.texCoords[unit];
                    if (tc == null) { out.texS[unit] = 0; out.texT[unit] = 0; continue; }
                    float ts = vb.texScale[unit];
                    out.texS[unit] = (w * tc.component(a, 0) + u * tc.component(b, 0) + v * tc.component(c, 0)) * ts + vb.texBias[unit][0];
                    out.texT[unit] = (w * tc.component(a, 1) + u * tc.component(b, 1) + v * tc.component(c, 1)) * ts + vb.texBias[unit][1];
                }
            }
        }
    }

    void collectReferences(Vector out) {
        for (int i = 0; i < children.size(); i++) add(out, (Object3D) children.elementAt(i));
    }

    /** Nodes are copied with their whole subtree. */
    Object3D duplicateImpl() {
        Group g = new Group();
        copyGroup(g);
        return g;
    }

    void copyGroup(Group to) {
        to.copyNode(this);
        for (int i = 0; i < children.size(); i++) {
            Node c = (Node) ((Node) children.elementAt(i)).duplicateImpl();
            c.parent = to;
            to.children.addElement(c);
        }
    }
}
