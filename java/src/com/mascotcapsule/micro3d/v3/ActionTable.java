/*
 * amiga-j2me: Mascot Capsule micro3d v3 API for the Amiga J2ME runtime.
 * Written for this project; licensed under GPLv2 (see COPYING).
 */
package com.mascotcapsule.micro3d.v3;

import java.io.IOException;

/** Animations of a figure's bones (MTRA data). Frames are 16.16 fixed point key frame numbers. */
public class ActionTable {
    /** One bone in one action: type 0 fixed matrix, 1 identity, 2..6 animated (see pose). */
    static final class Bone {
        int type;
        float[] matrix;                 // type 0
        int[] tKeys, sKeys, rKeys, lKeys;
        float[] tVals, sVals, rVals, lVals;     // translate, scale, rotate (xyz each), roll (radians)
    }

    private int[] frames;
    Bone[][] bones;
    int[][] patternFrames, patternValues;      // version 5: figure patterns that switch at key frames

    public ActionTable(byte[] b) {
        if (b == null) throw new NullPointerException();
        try {
            load(b);
        } catch (ArrayIndexOutOfBoundsException e) {
            throw new RuntimeException("damaged MTRA data");
        }
    }

    public ActionTable(String name) throws IOException { this(Texture.readResource(name)); }

    public final void dispose() {}
    public final int getNumAction() { return frames.length; }
    public final int getNumActions() { return frames.length; }
    public final int getNumFrame(int idx) { return getNumFrames(idx); }
    public final int getNumFrames(int idx) {
        if (idx < 0 || idx >= frames.length) throw new IllegalArgumentException();
        return frames[idx] << 16;
    }

    private byte[] d;
    private int pos;

    private int u8() { return d[pos++] & 0xFF; }
    private int u16() { int v = (d[pos] & 0xFF) | ((d[pos + 1] & 0xFF) << 8); pos += 2; return v; }
    private int s16() { return (short) u16(); }

    private void keys(int count, int[] k, float[] v, float scale) {
        for (int j = 0; j < count; j++) {
            k[j] = u16();
            v[j * 3] = s16() * scale; v[j * 3 + 1] = s16() * scale; v[j * 3 + 2] = s16() * scale;
        }
    }

    private void load(byte[] data) {
        d = data;
        if (u8() != 'M' || u8() != 'T') throw new RuntimeException("not MTRA data");
        int version = u8();
        if (u8() != 0 || version < 2 || version > 5) throw new RuntimeException("unsupported MTRA version " + version);
        int numActions = u16(), numBones = u16();
        pos += 8 * 2 + 4;           // counts per bone type, data size
        frames = new int[numActions];
        bones = new Bone[numActions][numBones];
        patternFrames = new int[numActions][];
        patternValues = new int[numActions][];
        final float fix = 1f / 4096, rad = (float) (Math.PI / 2048);
        for (int a = 0; a < numActions; a++) {
            frames[a] = u16();
            for (int i = 0; i < numBones; i++) {
                Bone bn = bones[a][i] = new Bone();
                int n;
                bn.type = u8();
                switch (bn.type) {
                case 0:
                    bn.matrix = new float[12];
                    for (int k = 0; k < 12; k++) bn.matrix[k] = (k & 3) == 3 ? s16() : s16() * fix;
                    break;
                case 1:
                    break;
                case 2:
                    n = u16(); bn.tKeys = new int[n]; bn.tVals = new float[n * 3]; keys(n, bn.tKeys, bn.tVals, 1f);
                    n = u16(); bn.sKeys = new int[n]; bn.sVals = new float[n * 3]; keys(n, bn.sKeys, bn.sVals, fix);
                    n = u16(); bn.rKeys = new int[n]; bn.rVals = new float[n * 3]; keys(n, bn.rKeys, bn.rVals, 1f);
                    n = u16(); bn.lKeys = new int[n]; bn.lVals = new float[n];
                    for (int j = 0; j < n; j++) { bn.lKeys[j] = u16(); bn.lVals[j] = s16() * rad; }
                    break;
                case 3:
                    bn.tKeys = new int[1]; bn.tVals = new float[] { s16(), s16(), s16() };
                    n = u16(); bn.rKeys = new int[n]; bn.rVals = new float[n * 3]; keys(n, bn.rKeys, bn.rVals, 1f);
                    bn.lKeys = new int[1]; bn.lVals = new float[] { s16() * rad };
                    break;
                case 4:
                    n = u16(); bn.rKeys = new int[n]; bn.rVals = new float[n * 3]; keys(n, bn.rKeys, bn.rVals, 1f);
                    n = u16(); bn.lKeys = new int[n]; bn.lVals = new float[n];
                    for (int j = 0; j < n; j++) { bn.lKeys[j] = u16(); bn.lVals[j] = s16() * rad; }
                    break;
                case 5:
                    n = u16(); bn.rKeys = new int[n]; bn.rVals = new float[n * 3]; keys(n, bn.rKeys, bn.rVals, 1f);
                    break;
                case 6:
                    n = u16(); bn.tKeys = new int[n]; bn.tVals = new float[n * 3]; keys(n, bn.tKeys, bn.tVals, 1f);
                    n = u16(); bn.rKeys = new int[n]; bn.rVals = new float[n * 3]; keys(n, bn.rKeys, bn.rVals, 1f);
                    n = u16(); bn.lKeys = new int[n]; bn.lVals = new float[n];
                    for (int j = 0; j < n; j++) { bn.lKeys[j] = u16(); bn.lVals[j] = s16() * rad; }
                    break;
                default:
                    throw new RuntimeException("unsupported bone animation type " + bn.type);
                }
            }
            if (version == 5) {
                int n = u16();
                patternFrames[a] = new int[n];
                patternValues[a] = new int[n];
                for (int j = 0; j < n; j++) {
                    patternFrames[a][j] = u16();
                    patternValues[a][j] = u16() | (u16() << 16);
                }
            }
        }
        d = null;
    }

    /** Value of a key framed track at frame f (in key frames), linear between keys. */
    private static void sample(int[] keys, float[] vals, int comps, float f, float[] out) {
        int last = keys.length - 1;
        if (last < 0) { for (int c = 0; c < comps; c++) out[c] = 0; return; }
        int i = last;
        while (i > 0 && keys[i] > f) i--;
        if (i == last || keys[i] >= f) {
            for (int c = 0; c < comps; c++) out[c] = vals[i * comps + c];
            return;
        }
        float t = (f - keys[i]) / (keys[i + 1] - keys[i]);
        for (int c = 0; c < comps; c++) {
            float v = vals[i * comps + c];
            out[c] = v + (vals[(i + 1) * comps + c] - v) * t;
        }
    }

    /** Writes the 3x4 matrix of bone i of an action at a frame (16.16) into m[o..o+11]. */
    void pose(int action, int i, int frame, float[] m, int o, float[] tmp) {
        Bone b = bones[action][i];
        float f = frame / 65536f;
        for (int k = 0; k < 12; k++) m[o + k] = (k == 0 || k == 5 || k == 10) ? 1f : 0f;
        if (b.type == 0) { System.arraycopy(b.matrix, 0, m, o, 12); return; }
        if (b.type == 1) return;
        if (b.tKeys != null) {
            sample(b.tKeys, b.tVals, 3, f, tmp);
            m[o + 3] = tmp[0]; m[o + 7] = tmp[1]; m[o + 11] = tmp[2];
        }
        // the rotation turns the z axis towards the given vector...
        sample(b.rKeys, b.rVals, 3, f, tmp);
        float x = tmp[0], y = tmp[1], z = tmp[2];
        float len = (float) Math.sqrt(x * x + y * y + z * z);
        if (len > 0) { x /= len; y /= len; z /= len; } else { x = 0; y = 0; z = 1; }
        float xx = x * x, yy = y * y;
        if (xx > 0f || yy > 0f) {
            float a = (1f - z) / (xx + yy), c = -a * x * y;
            m[o] = z + yy * a; m[o + 1] = c; m[o + 2] = x;
            m[o + 4] = c; m[o + 5] = z + xx * a; m[o + 6] = y;
            m[o + 8] = -x; m[o + 9] = -y;
        } else {
            m[o + 5] = z;
        }
        m[o + 10] = z;
        // ...then rolls about it
        if (b.lKeys != null) {
            sample(b.lKeys, b.lVals, 1, f, tmp);
            float s = (float) Math.sin(tmp[0]), c = (float) Math.cos(tmp[0]);
            for (int r = 0; r < 3; r++) {
                float m0 = m[o + r * 4], m1 = m[o + r * 4 + 1];
                m[o + r * 4] = m0 * c + m1 * s;
                m[o + r * 4 + 1] = m1 * c - m0 * s;
            }
        }
        if (b.sKeys != null) {
            sample(b.sKeys, b.sVals, 3, f, tmp);
            for (int r = 0; r < 3; r++) {
                m[o + r * 4] *= tmp[0]; m[o + r * 4 + 1] *= tmp[1]; m[o + r * 4 + 2] *= tmp[2];
            }
        }
    }
}
