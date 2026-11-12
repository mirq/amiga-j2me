/*
 * amiga-j2me: Mascot Capsule micro3d v3 API for the Amiga J2ME runtime.
 * Written for this project; licensed under GPLv2 (see COPYING).
 */
package com.mascotcapsule.micro3d.v3;

import java.io.IOException;

/** A model (MBAC data): vertices grouped by bone, coloured and textured polygons, patterns. */
public class Figure {
    static final int REC = 9;       // ints per triangle, see Native.g3dFigure

    int numVertices, numTris, numTextures = 1, numPatterns = 1;
    boolean hasColored;
    int[] tris;
    float[] pos, nrm;               // posed, model space
    private float[] basePos, baseNrm;
    private int[] boneCount, boneParent;
    private float[] boneMatrix, posed;      // 12 floats per bone
    private boolean fileNormals, dirty = true;
    private ActionTable action;
    private int actionIndex, actionFrame;
    private Texture[] textures;
    private int selected;
    int pattern;

    public Figure(byte[] b) {
        if (b == null) throw new NullPointerException();
        try {
            load(b);
        } catch (ArrayIndexOutOfBoundsException e) {
            throw new RuntimeException("damaged MBAC data");
        }
    }

    public Figure(String name) throws IOException { this(Texture.readResource(name)); }

    public final void dispose() {}
    public final int getNumPattern() { return numPatterns; }
    public final int getNumTextures() { return textures == null ? 0 : textures.length; }
    public final Texture getTexture() { return textures == null ? null : textures[selected]; }

    public final void setTexture(Texture tex) {
        if (tex == null) throw new NullPointerException();
        if (!tex.forModel) throw new IllegalArgumentException();
        textures = new Texture[] { tex };
        selected = 0;
    }

    public final void setTexture(Texture[] t) {
        if (t == null) throw new NullPointerException();
        if (t.length == 0) throw new IllegalArgumentException();
        for (int i = 0; i < t.length; i++) {
            if (t[i] == null) throw new NullPointerException();
            if (!t[i].forModel) throw new IllegalArgumentException();
        }
        textures = t;
        selected = 0;
    }

    public final void selectTexture(int idx) {
        if (textures == null || idx < 0 || idx >= textures.length) throw new IllegalArgumentException();
        selected = idx;
    }

    /** The texture of face n: figures with several texture slots take them in order. */
    Texture textureOf(int face) {
        if (textures == null) return null;
        return numTextures > 1 && face < textures.length ? textures[face] : textures[selected];
    }

    public final void setPattern(int idx) { pattern = idx; }

    public final void setPosture(ActionTable act, int action, int frame) {
        if (act == null) throw new NullPointerException();
        if (action < 0 || action >= act.getNumActions()) throw new IllegalArgumentException();
        if (frame < 0) frame = 0;
        int[] pf = act.patternFrames[action];
        if (pf != null) {
            for (int i = pf.length - 1; i >= 0; i--) {
                if (pf[i] <= (frame >> 16)) { pattern = act.patternValues[action][i]; break; }
            }
        }
        if (this.action != act || actionIndex != action || actionFrame != frame) {
            this.action = act; actionIndex = action; actionFrame = frame;
            dirty = true;
        }
    }

    // ---- posing ----

    /** a = a * b for 3x4 matrices at offsets ao and bo */
    private static void mul(float[] a, int ao, float[] b, int bo, float[] out, int oo) {
        for (int r = 0; r < 3; r++) {
            float l0 = a[ao + r * 4], l1 = a[ao + r * 4 + 1], l2 = a[ao + r * 4 + 2], l3 = a[ao + r * 4 + 3];
            out[oo + r * 4] = l0 * b[bo] + l1 * b[bo + 4] + l2 * b[bo + 8];
            out[oo + r * 4 + 1] = l0 * b[bo + 1] + l1 * b[bo + 5] + l2 * b[bo + 9];
            out[oo + r * 4 + 2] = l0 * b[bo + 2] + l1 * b[bo + 6] + l2 * b[bo + 10];
            out[oo + r * 4 + 3] = l0 * b[bo + 3] + l1 * b[bo + 7] + l2 * b[bo + 11] + l3;
        }
    }

    /** Brings pos (and nrm, when wanted) up to date with the posture. */
    void pose(boolean wantNormals) {
        if (dirty) {
            dirty = false;
            int bones = boneCount.length, v = 0;
            float[] am = new float[12], tmp = new float[3], t2 = new float[12];
            for (int i = 0; i < bones; i++) {
                int o = i * 12;
                if (boneParent[i] < 0) System.arraycopy(boneMatrix, o, posed, o, 12);
                else mul(posed, boneParent[i] * 12, boneMatrix, o, posed, o);
                if (action != null && i < action.bones[actionIndex].length) {
                    action.pose(actionIndex, i, actionFrame, am, 0, tmp);
                    mul(posed, o, am, 0, t2, 0);
                    System.arraycopy(t2, 0, posed, o, 12);
                }
                float m0 = posed[o], m1 = posed[o + 1], m2 = posed[o + 2], m3 = posed[o + 3], m4 = posed[o + 4], m5 = posed[o + 5],
                      m6 = posed[o + 6], m7 = posed[o + 7], m8 = posed[o + 8], m9 = posed[o + 9], m10 = posed[o + 10], m11 = posed[o + 11];
                for (int end = v + boneCount[i] * 3; v < end; v += 3) {
                    float x = basePos[v], y = basePos[v + 1], z = basePos[v + 2];
                    pos[v] = m0 * x + m1 * y + m2 * z + m3;
                    pos[v + 1] = m4 * x + m5 * y + m6 * z + m7;
                    pos[v + 2] = m8 * x + m9 * y + m10 * z + m11;
                    if (fileNormals) {
                        x = baseNrm[v]; y = baseNrm[v + 1]; z = baseNrm[v + 2];
                        nrm[v] = m0 * x + m1 * y + m2 * z;
                        nrm[v + 1] = m4 * x + m5 * y + m6 * z;
                        nrm[v + 2] = m8 * x + m9 * y + m10 * z;
                    }
                }
            }
            if (!fileNormals) nrm = null;
        }
        if (wantNormals && nrm == null) faceNormals();
    }

    /** Old files carry no normals: every vertex gets the sum of its faces' normals. */
    private void faceNormals() {
        nrm = new float[numVertices * 3];
        for (int t = 0; t < numTris; t++) {
            int a = tris[t * REC] * 3, b = tris[t * REC + 1] * 3, c = tris[t * REC + 2] * 3;
            float ux = pos[b] - pos[a], uy = pos[b + 1] - pos[a + 1], uz = pos[b + 2] - pos[a + 2];
            float vx = pos[c] - pos[a], vy = pos[c + 1] - pos[a + 1], vz = pos[c + 2] - pos[a + 2];
            float nx = uy * vz - uz * vy, ny = uz * vx - ux * vz, nz = ux * vy - uy * vx;
            nrm[a] += nx; nrm[a + 1] += ny; nrm[a + 2] += nz;
            nrm[b] += nx; nrm[b + 1] += ny; nrm[b + 2] += nz;
            nrm[c] += nx; nrm[c + 1] += ny; nrm[c + 2] += nz;
        }
    }

    // ---- MBAC loader ----

    private byte[] d;
    private int at, cache, cached;

    private int u8() { return d[at++] & 0xFF; }
    private int u16() { int v = (d[at] & 0xFF) | ((d[at + 1] & 0xFF) << 8); at += 2; return v; }
    private int s16() { return (short) u16(); }

    /** bit fields are packed starting at the low bits of each byte */
    private int ubits(int n) {
        while (cached < n) { cache |= u8() << cached; cached += 8; }
        int r = cache & ((1 << n) - 1);
        cache >>>= n;
        cached -= n;
        return r;
    }
    private int sbits(int n) { return (ubits(n) << (32 - n)) >> (32 - n); }
    private void align() { cache = 0; cached = 0; }

    private int vertex(int i) {
        if (i >= numVertices) throw new RuntimeException("bad vertex index in MBAC data");
        return i;
    }

    private static final int[] VERTEX_BITS = { 8, 10, 13, 16 };
    private static final int[] AXIS_NORMALS = { 0, 0, 4096, 0, 0, -4096, 0, 0 };

    private void load(byte[] data) {
        d = data;
        if (u8() != 'M' || u8() != 'B') throw new RuntimeException("not MBAC data");
        int version = u8();
        if (u8() != 0 || version < 2 || version > 5) throw new RuntimeException("unsupported MBAC version " + version);
        int vertexFormat = 1, normalFormat = 0, polygonFormat = 1, boneFormat = 1;
        if (version > 3) { vertexFormat = u8(); normalFormat = u8(); polygonFormat = u8(); boneFormat = u8(); }
        if (boneFormat != 1) throw new RuntimeException("unsupported MBAC bone format");
        numVertices = u16();
        int numT3 = u16(), numT4 = u16(), numBones = u16(), numC3 = 0, numC4 = 0, numColors = 0;
        if (polygonFormat >= 3) {
            numC3 = u16(); numC4 = u16(); numTextures = u16(); numPatterns = u16(); numColors = u16();
        }
        if (numVertices > 21845 || numTextures > 16 || numPatterns > 33 || numColors > 256) throw new RuntimeException("damaged MBAC data");
        // how many polygons every pattern has: [pattern][0 = coloured, 1.. = texture][triangles, quads]
        int[][][] counts = new int[numPatterns][numTextures + 1][2];
        if (version == 5) {
            for (int i = 0; i < numPatterns; i++) {
                for (int j = 0; j <= numTextures; j++) { counts[i][j][0] = u16(); counts[i][j][1] = u16(); }
            }
        } else {
            counts[0][0][0] = numC3; counts[0][0][1] = numC4; counts[0][1][0] = numT3; counts[0][1][1] = numT4;
        }

        basePos = new float[numVertices * 3];
        if (vertexFormat == 1) {
            for (int i = 0; i < basePos.length; i++) basePos[i] = s16();
        } else if (vertexFormat == 2) {
            for (int i = 0; i < basePos.length; ) {
                int chunk = ubits(8), bits = VERTEX_BITS[chunk >> 6], n = (chunk & 0x3F) + 1;
                if (i + n * 3 > basePos.length) throw new RuntimeException("damaged MBAC vertices");
                for (int k = 0; k < n * 3; k++) basePos[i++] = sbits(bits);
            }
        } else {
            throw new RuntimeException("unsupported MBAC vertex format");
        }
        align();
        if (normalFormat == 1) {
            baseNrm = new float[numVertices * 3];
            for (int i = 0; i < baseNrm.length; i++) baseNrm[i] = s16();
        } else if (normalFormat == 2) {
            baseNrm = new float[numVertices * 3];
            for (int i = 0; i < numVertices; i++) {
                int x = ubits(7), y, z;
                if (x == 64) {
                    int type = ubits(3);
                    if (type > 5) throw new RuntimeException("damaged MBAC normals");
                    z = AXIS_NORMALS[type]; y = AXIS_NORMALS[type + 1]; x = AXIS_NORMALS[type + 2];
                } else {
                    x = (x << 25) >> 19;
                    y = (ubits(7) << 25) >> 19;
                    int neg = ubits(1), rest = 4096 * 4096 - x * x - y * y;
                    z = rest > 0 ? (int) (Math.sqrt(rest) + 0.5) : 0;
                    if (neg == 1) z = -z;
                }
                baseNrm[i * 3] = x; baseNrm[i * 3 + 1] = y; baseNrm[i * 3 + 2] = z;
            }
        } else if (normalFormat != 0) {
            throw new RuntimeException("unsupported MBAC normal format");
        }
        align();
        fileNormals = baseNrm != null;

        // polygons, in file order: coloured triangles, coloured quads, textured triangles, textured quads.
        // poly[i] = { flags, a, b, c, d (-1: triangle), 4 x data }
        int numC = numC3 + numC4, numT = numT3 + numT4;
        int[][] poly = new int[numC + numT][];
        if (numC > 0) {
            int matBits = u8(), idxBits = u8(), colorBits = u8(), colorIdBits = u8();
            u8();
            int[] colors = new int[numColors];
            for (int i = 0; i < numColors; i++) {
                int r = ubits(colorBits), g = ubits(colorBits), b = ubits(colorBits);
                colors[i] = (r << 16) | (g << 8) | b;
            }
            for (int i = 0; i < numC; i++) {
                int flags = ubits(matBits) << 1;
                int a = vertex(ubits(idxBits)), b = vertex(ubits(idxBits)), c = vertex(ubits(idxBits));
                int dd = i < numC3 ? -1 : vertex(ubits(idxBits));
                int color = colors[ubits(colorIdBits)];
                poly[i] = new int[] { flags, a, b, c, dd, color, 0, 0, 0 };
            }
        }
        if (numT > 0) {
            int matBits = 16, idxBits = 16, uvBits = 8;
            if (polygonFormat == 2) { matBits = u8(); idxBits = u8(); uvBits = 7; }
            else if (polygonFormat == 3) { matBits = ubits(8); idxBits = ubits(8); uvBits = ubits(8); ubits(8); }
            else if (polygonFormat != 1) throw new RuntimeException("unsupported MBAC polygon format");
            for (int i = 0; i < numT; i++) {
                boolean quad = i >= numT3;
                int flags, a, b, c, dd, n = quad ? 4 : 3;
                int[] p = new int[9];
                if (polygonFormat == 1) {
                    int m = u16();
                    flags = ((m & 4) << 2) | ((m & 2) >> 1) | 0x20;     // two-sided, colour key; always lit
                    a = vertex(u16()); b = vertex(u16()); c = vertex(u16()); dd = quad ? vertex(u16()) : -1;
                    for (int k = 0; k < n; k++) { int u = u8(), v = u8(); p[5 + k] = u | (v << 16); }
                } else {
                    flags = ubits(matBits);
                    a = vertex(ubits(idxBits)); b = vertex(ubits(idxBits)); c = vertex(ubits(idxBits));
                    dd = quad ? vertex(ubits(idxBits)) : -1;
                    for (int k = 0; k < n; k++) { int u = ubits(uvBits), v = ubits(uvBits); p[5 + k] = u | (v << 16); }
                }
                p[0] = flags; p[1] = a; p[2] = b; p[3] = c; p[4] = dd;
                poly[numC + i] = p;
            }
        }
        align();

        // which pattern and texture every polygon belongs to
        int[] polyPattern = new int[numC + numT], polyFace = new int[numC + numT];
        int c3 = 0, c4 = numC3, t3 = numC, t4 = numC + numT3;
        for (int i = 0; i < numPatterns; i++) {
            int bit = i == 0 ? 0 : 1 << i;
            for (int k = counts[i][0][0]; k > 0 && c3 < numC3; k--) { polyPattern[c3] = bit; polyFace[c3++] = -1; }
            for (int k = counts[i][0][1]; k > 0 && c4 < numC; k--) { polyPattern[c4] = bit; polyFace[c4++] = -1; }
            for (int j = 0; j < numTextures; j++) {
                for (int k = counts[i][j + 1][0]; k > 0 && t3 < numC + numT3; k--) { polyPattern[t3] = bit; polyFace[t3++] = j; }
                for (int k = counts[i][j + 1][1]; k > 0 && t4 < numC + numT; k--) { polyPattern[t4] = bit; polyFace[t4++] = j; }
            }
        }
        for (int i = 0; i < numC; i++) polyFace[i] = -1;
        hasColored = numC > 0;

        // triangles for the renderer; a quad a-b-c-d is the triangles a-b-c and c-b-d
        numTris = numC3 + numT3 + 2 * (numC4 + numT4);
        tris = new int[numTris * REC];
        int o = 0;
        for (int i = 0; i < poly.length; i++) {
            int[] p = poly[i];
            boolean tex = polyFace[i] >= 0;
            tris[o] = p[1]; tris[o + 1] = p[2]; tris[o + 2] = p[3];
            tris[o + 3] = p[5]; tris[o + 4] = tex ? p[6] : 0; tris[o + 5] = tex ? p[7] : 0;
            tris[o + 6] = p[0]; tris[o + 7] = polyPattern[i]; tris[o + 8] = polyFace[i];
            o += REC;
            if (p[4] >= 0) {
                tris[o] = p[3]; tris[o + 1] = p[2]; tris[o + 2] = p[4];
                tris[o + 3] = tex ? p[7] : p[5]; tris[o + 4] = tex ? p[6] : 0; tris[o + 5] = tex ? p[8] : 0;
                tris[o + 6] = p[0]; tris[o + 7] = polyPattern[i]; tris[o + 8] = polyFace[i];
                o += REC;
            }
        }

        // bones: each owns the next run of vertices
        boneCount = new int[numBones];
        boneParent = new int[numBones];
        boneMatrix = new float[numBones * 12];
        posed = new float[numBones * 12];
        int sum = 0;
        for (int i = 0; i < numBones; i++) {
            boneCount[i] = u16();
            boneParent[i] = s16();
            if (boneParent[i] >= i) throw new RuntimeException("damaged MBAC bones");
            for (int k = 0; k < 12; k++) boneMatrix[i * 12 + k] = (k & 3) == 3 ? s16() : s16() * (1f / 4096);
            sum += boneCount[i];
        }
        if (sum != numVertices) throw new RuntimeException("damaged MBAC bones");
        pos = new float[numVertices * 3];
        if (fileNormals) nrm = new float[numVertices * 3];
        d = null;
    }
}
