/*
 * amiga-j2me: Mascot Capsule micro3d v3 API for the Amiga J2ME runtime.
 * Written for this project; licensed under GPLv2 (see COPYING).
 */
package com.mascotcapsule.micro3d.v3;

import aj.Native;
import javax.microedition.lcdui.GameAccess;
import javax.microedition.lcdui.Graphics;

/**
 * Draws figures and primitives into a Graphics, with a depth buffer. flush()
 * ends a layer: what is drawn after it lies on top of what came before.
 */
public class Graphics3D {
    public static final int COMMAND_AFFINE_INDEX = 0x87000000, COMMAND_AMBIENT_LIGHT = 0xA0000000,
        COMMAND_ATTRIBUTE = 0x83000000, COMMAND_CENTER = 0x85000000, COMMAND_CLIP = 0x84000000,
        COMMAND_DIRECTION_LIGHT = 0xA1000000, COMMAND_END = 0x80000000, COMMAND_FLUSH = 0x82000000,
        COMMAND_LIST_VERSION_1_0 = 0xFE000001, COMMAND_NOP = 0x81000000, COMMAND_PARALLEL_SCALE = 0x90000000,
        COMMAND_PARALLEL_SIZE = 0x91000000, COMMAND_PERSPECTIVE_FOV = 0x92000000, COMMAND_PERSPECTIVE_WH = 0x93000000,
        COMMAND_TEXTURE_INDEX = 0x86000000, COMMAND_THRESHOLD = 0xAF000000;
    public static final int ENV_ATTR_LIGHTING = 1, ENV_ATTR_SPHERE_MAP = 2, ENV_ATTR_TOON_SHADING = 4,
        ENV_ATTR_SEMI_TRANSPARENT = 8;
    public static final int PATTR_LIGHTING = 1, PATTR_SPHERE_MAP = 2, PATTR_COLORKEY = 16, PATTR_BLEND_NORMAL = 0,
        PATTR_BLEND_HALF = 32, PATTR_BLEND_ADD = 64, PATTR_BLEND_SUB = 96;
    public static final int PDATA_NORMAL_NONE = 0, PDATA_NORMAL_PER_FACE = 512, PDATA_NORMAL_PER_VERTEX = 768,
        PDATA_COLOR_NONE = 0, PDATA_COLOR_PER_COMMAND = 1024, PDATA_COLOR_PER_FACE = 2048,
        PDATA_TEXURE_COORD_NONE = 0, PDATA_TEXURE_COORD = 12288, PDATA_POINT_SPRITE_PARAMS_PER_CMD = 4096,
        PDATA_POINT_SPRITE_PARAMS_PER_FACE = 8192, PDATA_POINT_SPRITE_PARAMS_PER_VERTEX = 12288;
    public static final int POINT_SPRITE_LOCAL_SIZE = 0, POINT_SPRITE_PIXEL_SIZE = 1, POINT_SPRITE_PERSPECTIVE = 0,
        POINT_SPRITE_NO_PERS = 2;
    public static final int PRIMITVE_POINTS = 0x1000000, PRIMITVE_LINES = 0x2000000, PRIMITVE_TRIANGLES = 0x3000000,
        PRIMITVE_QUADS = 0x4000000, PRIMITVE_POINT_SPRITES = 0x5000000;

    private static final int REC = Figure.REC;
    private static final float[] IDENTITY = { 1, 0, 0, 0, 0, 1, 0, 0, 0, 0, 1, 0 };

    private Graphics target;
    private short[] pixels;
    private final int[] I = new int[10], opts = new int[11];
    private Texture sphere;
    private final float[] P = new float[27];
    private int width, height, originX, originY;

    public Graphics3D() {}

    public final synchronized void bind(Graphics graphics) {
        if (graphics == null) throw new NullPointerException();
        if (target != null) throw new IllegalStateException("a target is already bound");
        int[] info = new int[8];
        GameAccess.targetInfo(graphics, info);
        pixels = GameAccess.pixels(graphics);
        width = info[0]; height = info[1];
        originX = info[6]; originY = info[7];
        I[0] = width; I[1] = height; I[2] = info[2]; I[3] = info[3]; I[4] = info[4]; I[5] = info[5];
        I[6] = 0; I[7] = 0; I[8] = width; I[9] = height;
        target = graphics;
        Native.g3dClear(pixels, I, 0, 2);
    }

    public final synchronized void release(Graphics graphics) {
        if (graphics == null) throw new NullPointerException();
        if (graphics != target) return;
        target = null;
        pixels = null;
    }

    public final void dispose() {}

    private void check() {
        if (target == null) throw new IllegalStateException("no target is bound");
    }

    public final synchronized void flush() {
        check();
        Native.g3dClear(pixels, I, 0, 2);
    }

    // ---- state for the rasteriser ----

    private void setView(AffineTrans a) {
        final float f = 1f / 4096;
        P[0] = a.m00 * f; P[1] = a.m01 * f; P[2] = a.m02 * f; P[3] = a.m03;
        P[4] = a.m10 * f; P[5] = a.m11 * f; P[6] = a.m12 * f; P[7] = a.m13;
        P[8] = a.m20 * f; P[9] = a.m21 * f; P[10] = a.m22 * f; P[11] = a.m23;
    }

    private void setCenter(int cx, int cy) { P[15] = originX + cx; P[16] = originY + cy; }

    private void scale(int sx, int sy) { P[12] = 0; P[13] = sx / 4096f; P[14] = sy / 4096f; }

    private void parallel(int w, int h) {
        if (w <= 0 || h <= 0) return;
        P[12] = 0; P[13] = (float) width / w; P[14] = (float) height / h;
    }

    private void perspectiveFov(int near, int far, int angle) {
        if (near <= 0 || near >= far) return;
        if (angle < 2) angle = 2;
        if (angle > 2046) angle = 2046;
        P[12] = 1; P[17] = near; P[18] = far;
        P[13] = P[14] = width * 0.5f / (float) Math.tan(angle * (Math.PI / 4096));
    }

    private void perspectiveSize(int near, int far, int w, int h) {
        if (near <= 0 || near >= far || w <= 0 || h <= 0) return;
        P[12] = 1; P[17] = near; P[18] = far;
        P[13] = near * 4096f * width / w; P[14] = near * 4096f * height / h;
    }

    private void setLayout(FigureLayout l, int x, int y) {
        setView(l.affine);
        setCenter(l.centerX + x, l.centerY + y);
        P[13] = 0;
        switch (l.mode) {
        case FigureLayout.PARALLEL: parallel(l.parallelW, l.parallelH); break;
        case FigureLayout.PERSPECTIVE_FOV: perspectiveFov(l.near, l.far, l.angle); break;
        case FigureLayout.PERSPECTIVE_WH: perspectiveSize(l.near, l.far, l.perspW, l.perspH); break;
        default: break;
        }
        if (P[13] == 0) scale(l.scaleX, l.scaleY);
    }

    private void setLight(int amb, int dirIntensity, int x, int y, int z) {
        float len = (float) Math.sqrt((float) x * x + (float) y * y + (float) z * z);
        if (amb < 0) amb = 0;
        if (amb > 4096) amb = 4096;
        if (dirIntensity < 0) dirIntensity = 0;
        if (dirIntensity > 16384) dirIntensity = 16384;
        P[19] = amb / 4096f;
        P[20] = len == 0 ? 0 : dirIntensity / 4096f;
        if (len == 0) len = 1;
        P[21] = -x / len; P[22] = -y / len; P[23] = -z / len;
    }

    private void setToon(boolean on, int threshold, int high, int low) {
        P[24] = on ? threshold / 255f : -1f;
        P[25] = high / 255f; P[26] = low / 255f;
    }

    /** Returns whether blended polygons are wanted. */
    private boolean setEffect(Effect3D e) {
        Light l = e.light;
        if (l == null) P[19] = -1f;
        else {
            Vector3D d = l.getDirection();
            setLight(l.getAmbIntensity(), l.getDirIntensity(), d.x, d.y, d.z);
        }
        setToon(e.shading == Effect3D.TOON_SHADING, e.toonThreshold, e.toonHigh, e.toonLow);
        sphere = e.sphere;
        return e.transparency;
    }

    private void draw(float[] pos, float[] nrm, int nverts, int[] tris, int ntris, int patternMask, int face,
                      boolean blend, int pass, boolean reuse, Texture tex) {
        opts[0] = nverts; opts[1] = ntris; opts[2] = patternMask; opts[3] = face;
        opts[4] = blend ? 1 : 0; opts[5] = pass; opts[6] = reuse ? 1 : 0;
        opts[7] = tex == null ? 0 : tex.width; opts[8] = tex == null ? 0 : tex.height;
        opts[9] = sphere == null ? 0 : sphere.width; opts[10] = sphere == null ? 0 : sphere.height;
        Native.g3dFigure(pixels, I, pos, nrm, P, tris, opts, tex == null ? null : tex.pix, tex == null ? null : tex.alpha,
                         sphere == null ? null : sphere.pix);
    }

    private void figure(Figure f, boolean blend) {
        boolean reuse = false;
        f.pose(P[19] >= 0 || sphere != null);
        for (int pass = 0; pass < (blend ? 2 : 1); pass++) {
            if (f.hasColored) {
                draw(f.pos, f.nrm, f.numVertices, f.tris, f.numTris, f.pattern, -1, blend, pass, reuse, null);
                reuse = true;
            }
            for (int face = 0; face < f.numTextures; face++) {
                Texture t = f.textureOf(face);
                if (t == null) continue;
                draw(f.pos, f.nrm, f.numVertices, f.tris, f.numTris, f.pattern, face, blend, pass, reuse, t);
                reuse = true;
            }
        }
    }

    /** Adds the figure to the current layer. */
    public final synchronized void renderFigure(Figure figure, int x, int y, FigureLayout layout, Effect3D effect) {
        check();
        if (figure == null || layout == null || effect == null) throw new NullPointerException();
        setLayout(layout, x, y);
        figure(figure, setEffect(effect));
    }

    /** Draws the figure at once, on top of what is there. */
    public final synchronized void drawFigure(Figure figure, int x, int y, FigureLayout layout, Effect3D effect) {
        check();
        if (figure == null || layout == null || effect == null) throw new NullPointerException();
        Native.g3dClear(pixels, I, 0, 2);
        setLayout(layout, x, y);
        figure(figure, setEffect(effect));
        Native.g3dClear(pixels, I, 0, 2);
    }

    // ---- primitives ----

    private float[] pp = new float[0], pn;
    private int[] pt = new int[0];
    private final float[] savedView = new float[12];

    /** A model space point into eye space. */
    private void eye(int[] v, int o, float[] out) {
        float x = v[o], y = v[o + 1], z = v[o + 2];
        out[0] = P[0] * x + P[1] * y + P[2] * z + P[3];
        out[1] = P[4] * x + P[5] * y + P[6] * z + P[7];
        out[2] = P[8] * x + P[9] * y + P[10] * z + P[11];
    }

    /** Eye space units per pixel at depth z. */
    private float pixel(float z) { return P[12] != 0 ? z / P[13] : 1f / P[13]; }

    private void quad(int v, int t, float ax, float ay, float bx, float by, float cx, float cy, float dx, float dy,
                      float z, int flags, int face, int color, int uv0, int uv1, int uv2, int uv3) {
        pp[v * 3] = ax; pp[v * 3 + 1] = ay; pp[v * 3 + 2] = z;
        pp[v * 3 + 3] = bx; pp[v * 3 + 4] = by; pp[v * 3 + 5] = z;
        pp[v * 3 + 6] = cx; pp[v * 3 + 7] = cy; pp[v * 3 + 8] = z;
        pp[v * 3 + 9] = dx; pp[v * 3 + 10] = dy; pp[v * 3 + 11] = z;
        int o = t * REC;
        pt[o] = v; pt[o + 1] = v + 1; pt[o + 2] = v + 2;
        pt[o + 3] = face < 0 ? color : uv0; pt[o + 4] = uv1; pt[o + 5] = uv2;
        pt[o + 6] = flags; pt[o + 7] = 0; pt[o + 8] = face;
        o += REC;
        pt[o] = v; pt[o + 1] = v + 2; pt[o + 2] = v + 3;
        pt[o + 3] = face < 0 ? color : uv0; pt[o + 4] = uv2; pt[o + 5] = uv3;
        pt[o + 6] = flags; pt[o + 7] = 0; pt[o + 8] = face;
    }

    /**
     * One primitive command. attrs are the environment attributes (ENV_ATTR_*).
     * Returns the number of ints the command's data takes in each array (a
     * command list keeps all four in one array).
     */
    private void primitives(Texture texture, int command, int[] vtx, int vo, int[] nor, int no, int[] tc, int to,
                            int[] col, int co, int attrs) {
        int type = command & 0x7000000, num = (command >> 16) & 0xFF;
        if (num == 0) return;
        int colorMode = command & (PDATA_COLOR_PER_COMMAND | PDATA_COLOR_PER_FACE);
        int normalMode = command & PDATA_NORMAL_PER_VERTEX, texMode = command & PDATA_TEXURE_COORD;
        boolean textured = texMode != 0 && texture != null;
        if (!textured && colorMode == PDATA_COLOR_NONE) return;
        int flags = Native.G3D_FIG_TWO_SIDED;
        if ((command & PATTR_COLORKEY) != 0) flags |= Native.G3D_FIG_COLORKEY;
        flags |= ((command & PATTR_BLEND_SUB) >> 5) << 1;
        boolean lit = (command & PATTR_LIGHTING) != 0 && (attrs & ENV_ATTR_LIGHTING) != 0 && normalMode != 0
                      && (type == PRIMITVE_TRIANGLES || type == PRIMITVE_QUADS);
        if (lit) flags |= Native.G3D_FIG_LIGHTING;
        boolean shiny = (command & PATTR_SPHERE_MAP) != 0 && (attrs & ENV_ATTR_SPHERE_MAP) != 0 && normalMode != 0 && sphere != null
                        && (type == PRIMITVE_TRIANGLES || type == PRIMITVE_QUADS);
        if (shiny) flags |= 0x40;
        boolean blend = (attrs & ENV_ATTR_SEMI_TRANSPARENT) != 0;
        int face = textured ? 0 : -1;
        int nv, nt;
        float amb = P[19];
        if (!lit) P[19] = -1f;

        if (type == PRIMITVE_TRIANGLES || type == PRIMITVE_QUADS) {
            int per = type == PRIMITVE_TRIANGLES ? 3 : 4;
            nv = num * per; nt = num * (per - 2);
            room(nv, nt, lit || shiny);
            for (int i = 0; i < nv * 3; i++) pp[i] = vtx[vo + i];
            if (lit || shiny) {
                for (int i = 0; i < nv; i++) {
                    int s = no + (normalMode == PDATA_NORMAL_PER_FACE ? (i / per) * 3 : i * 3);
                    pn[i * 3] = nor[s]; pn[i * 3 + 1] = nor[s + 1]; pn[i * 3 + 2] = nor[s + 2];
                }
            }
            for (int i = 0, t = 0; i < num; i++) {
                int v = i * per;
                int color = colorMode == PDATA_COLOR_PER_FACE ? col[co + i] : (colorMode != 0 ? col[co] : 0xFFFFFF);
                for (int k = 0; k < per - 2; k++, t++) {
                    int o = t * REC, b = v + 1 + k, c = v + 2 + k;
                    pt[o] = v; pt[o + 1] = b; pt[o + 2] = c;
                    if (textured) {
                        pt[o + 3] = uv(tc, to + v * 2); pt[o + 4] = uv(tc, to + b * 2); pt[o + 5] = uv(tc, to + c * 2);
                    } else {
                        pt[o + 3] = color; pt[o + 4] = 0; pt[o + 5] = 0;
                    }
                    pt[o + 6] = flags; pt[o + 7] = 0; pt[o + 8] = face;
                }
            }
            for (int pass = 0; pass < (blend ? 2 : 1); pass++) {
                draw(pp, lit || shiny ? pn : null, nv, pt, nt, 0, face, blend, pass, pass > 0, textured ? texture : null);
            }
            P[19] = amb;
            return;
        }

        // points, lines and sprites become quads facing the camera, built in eye space
        float[] a = new float[3], b = new float[3];
        nv = num * 4; nt = num * 2;
        room(nv, nt, false);
        int made = 0;
        for (int i = 0; i < num; i++) {
            int color = colorMode == PDATA_COLOR_PER_FACE ? col[co + i] : (colorMode != 0 ? col[co] : 0xFFFFFF);
            if (type == PRIMITVE_POINTS) {
                eye(vtx, vo + i * 3, a);
                float h = pixel(a[2]) * 0.5f;
                quad(made * 4, made * 2, a[0] - h, a[1] - h, a[0] + h, a[1] - h, a[0] + h, a[1] + h, a[0] - h, a[1] + h,
                     a[2], flags, -1, color, 0, 0, 0, 0);
            } else if (type == PRIMITVE_LINES) {
                eye(vtx, vo + i * 6, a);
                eye(vtx, vo + i * 6 + 3, b);
                float dx = b[0] - a[0], dy = b[1] - a[1], len = (float) Math.sqrt(dx * dx + dy * dy);
                if (len == 0) { dx = 1; dy = 0; len = 1; }
                float ha = pixel(a[2]) * 0.5f / len, hb = pixel(b[2]) * 0.5f / len;
                int v = made * 4, o = made * 2 * REC;
                pp[v * 3] = a[0] + dy * ha; pp[v * 3 + 1] = a[1] - dx * ha; pp[v * 3 + 2] = a[2];
                pp[v * 3 + 3] = b[0] + dy * hb; pp[v * 3 + 4] = b[1] - dx * hb; pp[v * 3 + 5] = b[2];
                pp[v * 3 + 6] = b[0] - dy * hb; pp[v * 3 + 7] = b[1] + dx * hb; pp[v * 3 + 8] = b[2];
                pp[v * 3 + 9] = a[0] - dy * ha; pp[v * 3 + 10] = a[1] + dx * ha; pp[v * 3 + 11] = a[2];
                for (int k = 0; k < 2; k++, o += REC) {
                    pt[o] = v; pt[o + 1] = v + 1 + k; pt[o + 2] = v + 2 + k;
                    pt[o + 3] = color; pt[o + 4] = 0; pt[o + 5] = 0; pt[o + 6] = flags; pt[o + 7] = 0; pt[o + 8] = -1;
                }
            } else if (type == PRIMITVE_POINT_SPRITES) {
                if (!textured) continue;
                int s = to + (texMode == PDATA_POINT_SPRITE_PARAMS_PER_CMD ? 0 : i * 8);
                eye(vtx, vo + i * 3, a);
                float hw = tc[s] * 0.5f, hh = tc[s + 1] * 0.5f;
                int mode = tc[s + 7];
                if ((mode & POINT_SPRITE_PIXEL_SIZE) != 0) { float px = pixel(a[2]); hw *= px; hh *= px; }
                else if ((mode & POINT_SPRITE_NO_PERS) != 0 && P[12] != 0) { float k = a[2] / P[17]; hw *= k; hh *= k; }
                double ang = tc[s + 2] * (Math.PI / 2048);
                float sn = (float) Math.sin(ang), cs = (float) Math.cos(ang);
                float xx = hw * cs, xy = hw * sn, yx = -hh * sn, yy = hh * cs;
                int u0 = tc[s + 3], v0 = tc[s + 4], u1 = tc[s + 5], v1 = tc[s + 6];
                quad(made * 4, made * 2, a[0] - xx - yx, a[1] - xy - yy, a[0] + xx - yx, a[1] + xy - yy,
                     a[0] + xx + yx, a[1] + xy + yy, a[0] - xx + yx, a[1] - xy + yy, a[2], flags, 0, 0,
                     u0 | (v0 << 16), u1 | (v0 << 16), u1 | (v1 << 16), u0 | (v1 << 16));
            } else {
                P[19] = amb;
                throw new IllegalArgumentException();
            }
            made++;
        }
        System.arraycopy(P, 0, savedView, 0, 12);
        System.arraycopy(IDENTITY, 0, P, 0, 12);
        boolean sprite = type == PRIMITVE_POINT_SPRITES;
        for (int pass = 0; pass < (blend ? 2 : 1) && made > 0; pass++) {
            draw(pp, null, made * 4, pt, made * 2, 0, sprite ? 0 : -1, blend, pass, pass > 0, sprite ? texture : null);
        }
        System.arraycopy(savedView, 0, P, 0, 12);
        P[19] = amb;
    }

    private static int uv(int[] tc, int o) { return (tc[o] & 0xFFFF) | ((tc[o + 1] & 0xFFFF) << 16); }

    private void room(int nv, int nt, boolean normals) {
        if (pp.length < nv * 3) pp = new float[nv * 3];
        if (normals && (pn == null || pn.length < nv * 3)) pn = new float[nv * 3];
        if (pt.length < nt * REC) pt = new int[nt * REC];
    }

    private static int attrsOf(Effect3D e) {
        int a = 0;
        if (e.light != null) a |= ENV_ATTR_LIGHTING;
        if (e.transparency) a |= ENV_ATTR_SEMI_TRANSPARENT;
        if (e.shading == Effect3D.TOON_SHADING) a |= ENV_ATTR_TOON_SHADING;
        if (e.sphere != null) a |= ENV_ATTR_SPHERE_MAP;
        return a;
    }

    public final synchronized void renderPrimitives(Texture texture, int x, int y, FigureLayout layout, Effect3D effect,
                                                    int command, int numPrimitives, int[] vertexCoords, int[] normals,
                                                    int[] textureCoords, int[] colors) {
        check();
        if (layout == null || effect == null || vertexCoords == null || normals == null || textureCoords == null || colors == null) {
            throw new NullPointerException();
        }
        if (command < 0 || numPrimitives <= 0 || numPrimitives >= 256) throw new IllegalArgumentException();
        setLayout(layout, x, y);
        setEffect(effect);
        try {
            primitives(texture, (command & ~0xFF0000) | (numPrimitives << 16), vertexCoords, 0, normals, 0,
                       textureCoords, 0, colors, 0, attrsOf(effect));
        } catch (ArrayIndexOutOfBoundsException e) {
            throw new IllegalArgumentException("primitive data is too short");
        }
    }

    public final void drawCommandList(Texture texture, int x, int y, FigureLayout layout, Effect3D effect, int[] commandList) {
        drawCommandList(texture == null ? null : new Texture[] { texture }, x, y, layout, effect, commandList);
    }

    private static final int[] VERTS = { 0, 1, 2, 3, 4, 1 };

    public final synchronized void drawCommandList(Texture[] textures, int x, int y, FigureLayout layout, Effect3D effect,
                                                   int[] cmds) {
        check();
        if (layout == null || effect == null || cmds == null) throw new NullPointerException();
        if (cmds.length == 0 || cmds[0] != COMMAND_LIST_VERSION_1_0) throw new IllegalArgumentException("unknown command list version");
        setLayout(layout, x, y);
        setEffect(effect);
        int attrs = attrsOf(effect), tex = 0;
        int ox = x, oy = y;
        Light light = effect.light;
        int amb = light == null ? 0 : light.getAmbIntensity(), dirI = light == null ? 0 : light.getDirIntensity();
        int lx = light == null ? 0 : light.getDirection().x, ly = light == null ? 0 : light.getDirection().y,
            lz = light == null ? 4096 : light.getDirection().z;
        int saved2 = I[2], saved3 = I[3], saved4 = I[4], saved5 = I[5];
        try {
            for (int i = 1; i < cmds.length; ) {
                int cmd = cmds[i++];
                switch (cmd & 0xFF000000) {
                case COMMAND_AFFINE_INDEX: {
                    AffineTrans t = layout.transAt(cmd & 0xFFFFFF);
                    if (t == null) throw new IllegalArgumentException();
                    setView(t);
                    break;
                }
                case COMMAND_AMBIENT_LIGHT:
                    amb = cmds[i++];
                    setLight(amb, dirI, lx, ly, lz);
                    break;
                case COMMAND_DIRECTION_LIGHT:
                    lx = cmds[i++]; ly = cmds[i++]; lz = cmds[i++]; dirI = cmds[i++];
                    setLight(amb, dirI, lx, ly, lz);
                    break;
                case COMMAND_ATTRIBUTE:
                    attrs = cmd & 0xFFFFFF;
                    if ((attrs & ENV_ATTR_LIGHTING) != 0) setLight(amb, dirI, lx, ly, lz); else P[19] = -1f;
                    setToon((attrs & ENV_ATTR_TOON_SHADING) != 0, effect.toonThreshold, effect.toonHigh, effect.toonLow);
                    break;
                case COMMAND_CENTER:
                    setCenter(cmds[i] + ox, cmds[i + 1] + oy);
                    i += 2;
                    break;
                case COMMAND_CLIP: {
                    int x0 = Math.max(saved2, originX + cmds[i]), y0 = Math.max(saved3, originY + cmds[i + 1]);
                    int x1 = Math.min(saved2 + saved4, originX + cmds[i + 2]), y1 = Math.min(saved3 + saved5, originY + cmds[i + 3]);
                    I[2] = x0; I[3] = y0; I[4] = Math.max(0, x1 - x0); I[5] = Math.max(0, y1 - y0);
                    i += 4;
                    break;
                }
                case COMMAND_FLUSH:
                    Native.g3dClear(pixels, I, 0, 2);
                    break;
                case COMMAND_NOP:
                    i += cmd & 0xFFFFFF;
                    break;
                case COMMAND_PARALLEL_SCALE: scale(cmds[i], cmds[i + 1]); i += 2; break;
                case COMMAND_PARALLEL_SIZE: parallel(cmds[i], cmds[i + 1]); i += 2; break;
                case COMMAND_PERSPECTIVE_FOV: perspectiveFov(cmds[i], cmds[i + 1], cmds[i + 2]); i += 3; break;
                case COMMAND_PERSPECTIVE_WH: perspectiveSize(cmds[i], cmds[i + 1], cmds[i + 2], cmds[i + 3]); i += 4; break;
                case COMMAND_TEXTURE_INDEX:
                    tex = cmd & 0xFFFFFF;
                    break;
                case COMMAND_THRESHOLD:
                    setToon((attrs & ENV_ATTR_TOON_SHADING) != 0, cmds[i], cmds[i + 1], cmds[i + 2]);
                    i += 3;
                    break;
                case COMMAND_END:
                    return;
                default: {
                    int type = cmd & 0x7000000;
                    if (cmd < 0 || type == 0 || type > PRIMITVE_POINT_SPRITES) throw new IllegalArgumentException("bad command");
                    int num = (cmd >> 16) & 0xFF, per = VERTS[type >> 24];
                    int vo = i, len = num * per * 3;
                    i += len;
                    int no = i;
                    if ((cmd & PDATA_NORMAL_PER_VERTEX) == PDATA_NORMAL_PER_FACE) i += num * 3;
                    else if ((cmd & PDATA_NORMAL_PER_VERTEX) == PDATA_NORMAL_PER_VERTEX) i += len;
                    int to = i;
                    if (type == PRIMITVE_POINT_SPRITES) {
                        if ((cmd & PDATA_TEXURE_COORD) == PDATA_POINT_SPRITE_PARAMS_PER_CMD) i += 8;
                        else if ((cmd & PDATA_TEXURE_COORD) != 0) i += num * 8;
                    } else if ((cmd & PDATA_TEXURE_COORD) == PDATA_TEXURE_COORD) {
                        i += num * per * 2;
                    }
                    int co = i;
                    if ((cmd & (PDATA_COLOR_PER_COMMAND | PDATA_COLOR_PER_FACE)) == PDATA_COLOR_PER_COMMAND) i++;
                    else if ((cmd & (PDATA_COLOR_PER_COMMAND | PDATA_COLOR_PER_FACE)) == PDATA_COLOR_PER_FACE) i += num;
                    if (i > cmds.length) throw new IllegalArgumentException("command list is too short");
                    Texture t = textures != null && tex >= 0 && tex < textures.length ? textures[tex] : null;
                    primitives(t, cmd, cmds, vo, cmds, no, cmds, to, cmds, co, attrs);
                    break;
                }
                }
            }
        } catch (ArrayIndexOutOfBoundsException e) {
            throw new IllegalArgumentException("command list is too short");
        } finally {
            I[2] = saved2; I[3] = saved3; I[4] = saved4; I[5] = saved5;
        }
    }
}
