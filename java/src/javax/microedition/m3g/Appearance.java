/*
 * amiga-j2me: JSR-184 (M3G 1.0/1.1) implementation for the Amiga J2ME runtime.
 * Written for this project; licensed under GPLv2 (see COPYING).
 */
package javax.microedition.m3g;

import java.util.Vector;

public class Appearance extends Object3D {
    int layer;
    CompositingMode compositingMode;
    Fog fog;
    PolygonMode polygonMode;
    Material material;
    final Texture2D[] textures = new Texture2D[2];

    public Appearance() {}

    public void setLayer(int l) {
        if (l < -63 || l > 63) throw new IndexOutOfBoundsException();
        layer = l;
    }
    public int getLayer() { return layer; }
    public void setFog(Fog f) { fog = f; }
    public Fog getFog() { return fog; }
    public void setPolygonMode(PolygonMode p) { polygonMode = p; }
    public PolygonMode getPolygonMode() { return polygonMode; }
    public void setCompositingMode(CompositingMode c) { compositingMode = c; }
    public CompositingMode getCompositingMode() { return compositingMode; }
    public void setTexture(int index, Texture2D t) {
        if (index < 0 || index > 1) throw new IndexOutOfBoundsException();
        textures[index] = t;
    }
    public Texture2D getTexture(int index) {
        if (index < 0 || index > 1) throw new IndexOutOfBoundsException();
        return textures[index];
    }
    public void setMaterial(Material m) { material = m; }
    public Material getMaterial() { return material; }

    void collectReferences(Vector out) {
        add(out, compositingMode); add(out, fog); add(out, polygonMode); add(out, material);
        add(out, textures[0]); add(out, textures[1]);
    }

    Object3D duplicateImpl() {
        Appearance a = new Appearance();
        a.copyBase(this);
        a.layer = layer; a.compositingMode = compositingMode; a.fog = fog; a.polygonMode = polygonMode;
        a.material = material; a.textures[0] = textures[0]; a.textures[1] = textures[1];
        return a;
    }
}
