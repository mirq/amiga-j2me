/*
 * amiga-j2me: JSR-184 (M3G 1.0/1.1) implementation for the Amiga J2ME runtime.
 * Written for this project; licensed under GPLv2 (see COPYING).
 */
package javax.microedition.m3g;

import java.util.Vector;

public class VertexBuffer extends Object3D {
    VertexArray positions, normals, colors;
    final VertexArray[] texCoords = new VertexArray[2];
    float posScale = 1;
    final float[] posBias = new float[3];
    final float[] texScale = { 1, 1 };
    final float[][] texBias = new float[2][3];
    int defaultColor = 0xFFFFFFFF;

    public VertexBuffer() {}

    public int getVertexCount() {
        if (positions != null) return positions.count;
        if (normals != null) return normals.count;
        if (colors != null) return colors.count;
        if (texCoords[0] != null) return texCoords[0].count;
        return 0;
    }

    public void setPositions(VertexArray p, float scale, float[] bias) {
        if (p != null && p.comps != 3) throw new IllegalArgumentException();
        positions = p; posScale = scale;
        posBias[0] = bias == null ? 0 : bias[0]; posBias[1] = bias == null ? 0 : bias[1]; posBias[2] = bias == null ? 0 : bias[2];
    }

    public void setTexCoords(int index, VertexArray t, float scale, float[] bias) {
        if (index < 0 || index > 1) throw new IndexOutOfBoundsException();
        if (t != null && t.comps != 2 && t.comps != 3) throw new IllegalArgumentException();
        texCoords[index] = t; texScale[index] = scale;
        for (int i = 0; i < 3; i++) texBias[index][i] = (bias == null || i >= bias.length) ? 0 : bias[i];
    }

    public void setNormals(VertexArray n) {
        if (n != null && n.comps != 3) throw new IllegalArgumentException();
        normals = n;
    }

    public void setColors(VertexArray c) {
        if (c != null && (c.size != 1 || c.comps < 3)) throw new IllegalArgumentException();
        colors = c;
    }

    public VertexArray getPositions(float[] scaleBias) {
        if (scaleBias != null) {
            scaleBias[0] = posScale;
            System.arraycopy(posBias, 0, scaleBias, 1, 3);
        }
        return positions;
    }

    public VertexArray getTexCoords(int index, float[] scaleBias) {
        if (index < 0 || index > 1) throw new IndexOutOfBoundsException();
        if (scaleBias != null) {
            scaleBias[0] = texScale[index];
            for (int i = 0; i < 3 && i + 1 < scaleBias.length; i++) scaleBias[i + 1] = texBias[index][i];
        }
        return texCoords[index];
    }

    public VertexArray getNormals() { return normals; }
    public VertexArray getColors() { return colors; }
    public void setDefaultColor(int ARGB) { defaultColor = ARGB; }
    public int getDefaultColor() { return defaultColor; }

    void setProperty(int property, float[] v) {
        if (property == AnimationTrack.COLOR) {
            defaultColor = (defaultColor & 0xFF000000) | ((int) (M3GMath.clamp(v[0], 0, 1) * 255) << 16)
                         | ((int) (M3GMath.clamp(v[1], 0, 1) * 255) << 8) | (int) (M3GMath.clamp(v[2], 0, 1) * 255);
        } else if (property == AnimationTrack.ALPHA) {
            defaultColor = (defaultColor & 0xFFFFFF) | ((int) (M3GMath.clamp(v[0], 0, 1) * 255) << 24);
        } else super.setProperty(property, v);
    }

    void collectReferences(Vector out) {
        add(out, positions); add(out, normals); add(out, colors); add(out, texCoords[0]); add(out, texCoords[1]);
    }

    Object3D duplicateImpl() {
        VertexBuffer b = new VertexBuffer();
        b.copyBase(this);
        b.positions = positions; b.normals = normals; b.colors = colors;
        b.texCoords[0] = texCoords[0]; b.texCoords[1] = texCoords[1];
        b.posScale = posScale; b.defaultColor = defaultColor;
        System.arraycopy(posBias, 0, b.posBias, 0, 3);
        b.texScale[0] = texScale[0]; b.texScale[1] = texScale[1];
        System.arraycopy(texBias[0], 0, b.texBias[0], 0, 3);
        System.arraycopy(texBias[1], 0, b.texBias[1], 0, 3);
        return b;
    }
}
