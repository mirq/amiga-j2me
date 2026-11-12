/*
 * amiga-j2me: JSR-184 (M3G 1.0/1.1) implementation for the Amiga J2ME runtime.
 * Written for this project; licensed under GPLv2 (see COPYING).
 */
package javax.microedition.m3g;

import aj.Native;
import java.util.Hashtable;
import java.util.Vector;
import javax.microedition.lcdui.GameAccess;
import javax.microedition.lcdui.Graphics;

/**
 * The renderer. The scene graph, animation and state live in Java; transform,
 * lighting, clipping and rasterising of each submesh are one native call.
 * Not drawn: fog, specular highlights, the second texture unit, depth offset.
 */
public class Graphics3D {
    public static final int ANTIALIAS = 2, DITHER = 4, TRUE_COLOR = 8, OVERWRITE = 16;

    private static final Graphics3D instance = new Graphics3D();

    private Object target;
    private short[] pixels;
    private final int[] I = new int[Native.G3D_I_SIZE];
    private final float[] F = new float[Native.G3D_F_SIZE];
    private boolean depthEnabled = true;
    private int hints;
    private int vpX, vpY, vpW, vpH;
    private float depthNear = 0, depthFar = 1;

    private Camera camera;
    private final Transform cameraTransform = new Transform();
    private final float[] view = new float[16];         // world -> eye
    private final float[] proj = new float[16];
    private final Vector lights = new Vector();          // Light
    private final Vector lightTransforms = new Vector(); // Transform (light -> world)

    private Graphics3D() {
        Transform id = new Transform();
        id.get(view);
        id.get(proj);
    }

    public static final Graphics3D getInstance() { return instance; }

    // ---- target ----

    public void bindTarget(Object target) { bindTarget(target, true, 0); }

    public void bindTarget(Object target, boolean depthBuffer, int hints) {
        if (target == null) throw new NullPointerException();
        if (this.target != null) throw new IllegalStateException("a target is already bound");
        if (target instanceof Graphics) {
            Graphics g = (Graphics) target;
            int[] info = new int[8];
            GameAccess.targetInfo(g, info);
            pixels = GameAccess.pixels(g);
            I[0] = info[0]; I[1] = info[1]; I[2] = info[2]; I[3] = info[3]; I[4] = info[4]; I[5] = info[5];
            // the viewport defaults to the clip rectangle
            vpX = info[2]; vpY = info[3]; vpW = info[4]; vpH = info[5];
        } else if (target instanceof Image2D) {
            Image2D img = (Image2D) target;
            if (!img.isMutable() || (img.format != Image2D.RGB && img.format != Image2D.RGBA)) throw new IllegalArgumentException();
            pixels = img.pix;
            I[0] = img.width; I[1] = img.height; I[2] = 0; I[3] = 0; I[4] = img.width; I[5] = img.height;
            vpX = 0; vpY = 0; vpW = img.width; vpH = img.height;
        } else {
            throw new IllegalArgumentException();
        }
        this.target = target;
        depthEnabled = depthBuffer;
        this.hints = hints;
        setVp();
    }

    public Object getTarget() { return target; }
    public void releaseTarget() { target = null; pixels = null; }
    public int getHints() { return hints; }
    public boolean isDepthBufferEnabled() { return depthEnabled; }

    private void setVp() { I[6] = vpX; I[7] = vpY; I[8] = vpW; I[9] = vpH; }

    public void setViewport(int x, int y, int width, int height) {
        if (width <= 0 || height <= 0) throw new IllegalArgumentException();
        if (target instanceof Graphics) {       // viewport coordinates are the Graphics' own
            int[] info = new int[8];
            GameAccess.targetInfo((Graphics) target, info);
            x += info[6]; y += info[7];
        }
        vpX = x; vpY = y; vpW = width; vpH = height;
        setVp();
    }
    public int getViewportX() { return vpX; }
    public int getViewportY() { return vpY; }
    public int getViewportWidth() { return vpW; }
    public int getViewportHeight() { return vpH; }
    public void setDepthRange(float near, float far) {
        if (near < 0 || near > 1 || far < 0 || far > 1) throw new IllegalArgumentException();
        depthNear = near; depthFar = far;
    }
    public float getDepthRangeNear() { return depthNear; }
    public float getDepthRangeFar() { return depthFar; }

    public static final Hashtable getProperties() {
        Hashtable h = new Hashtable();
        h.put("supportAntialiasing", Boolean.FALSE);
        h.put("supportTrueColor", Boolean.FALSE);
        h.put("supportDithering", Boolean.FALSE);
        h.put("supportMipmapping", Boolean.FALSE);
        h.put("supportPerspectiveCorrection", Boolean.TRUE);
        h.put("supportLocalCameraLighting", Boolean.FALSE);
        h.put("maxLights", new Integer(Native.G3D_MAX_LIGHTS));
        h.put("maxViewportWidth", new Integer(1024));
        h.put("maxViewportHeight", new Integer(1024));
        h.put("maxViewportDimension", new Integer(1024));
        h.put("maxTextureDimension", new Integer(256));
        h.put("maxSpriteCropDimension", new Integer(256));
        h.put("maxTransformsPerVertex", new Integer(2));
        h.put("numTextureUnits", new Integer(1));
        return h;
    }

    private void checkBound() {
        if (target == null) throw new IllegalStateException("no target bound");
    }

    // ---- camera and lights ----

    public void setCamera(Camera camera, Transform transform) {
        this.camera = camera;
        if (transform == null) cameraTransform.setIdentity(); else cameraTransform.set(transform);
        updateCamera();
    }

    public Camera getCamera(Transform transform) {
        if (transform != null) transform.set(cameraTransform);
        return camera;
    }

    private void updateCamera() {
        Transform t = new Transform();
        if (camera != null) camera.getProjection(t);
        t.get(proj);
        if (!Transform.invert(cameraTransform.m, view)) new Transform().get(view);
    }

    public int addLight(Light light, Transform transform) {
        if (light == null) throw new NullPointerException();
        lights.addElement(light);
        lightTransforms.addElement(transform == null ? new Transform() : new Transform(transform));
        return lights.size() - 1;
    }

    public void setLight(int index, Light light, Transform transform) {
        if (index < 0 || index >= lights.size()) throw new IndexOutOfBoundsException();
        lights.setElementAt(light == null ? (Object) "" : light, index);
        lightTransforms.setElementAt(transform == null ? new Transform() : new Transform(transform), index);
    }

    public void resetLights() {
        lights.removeAllElements();
        lightTransforms.removeAllElements();
    }

    public int getLightCount() { return lights.size(); }

    public Light getLight(int index, Transform transform) {
        if (index < 0 || index >= lights.size()) throw new IndexOutOfBoundsException();
        if (transform != null) transform.set((Transform) lightTransforms.elementAt(index));
        Object l = lights.elementAt(index);
        return l instanceof Light ? (Light) l : null;
    }

    /** The lights in eye space, as the rasteriser takes them. */
    private void loadLights(int scope) {
        int n = 0;
        float[] m = new float[16];
        for (int i = 0; i < lights.size() && n < Native.G3D_MAX_LIGHTS; i++) {
            Object o = lights.elementAt(i);
            if (!(o instanceof Light)) continue;
            Light l = (Light) o;
            if ((l.scope & scope) == 0) continue;
            Transform.mul(view, ((Transform) lightTransforms.elementAt(i)).m, m);    // light -> eye
            int at = Native.G3D_F_LIGHTS + n * Native.G3D_LIGHT_FLOATS;
            float k = l.intensity / 255f;
            F[at + 1] = ((l.color >> 16) & 255) * k; F[at + 2] = ((l.color >> 8) & 255) * k; F[at + 3] = (l.color & 255) * k;
            if (l.mode == Light.AMBIENT) {
                F[at] = 0;
            } else if (l.mode == Light.DIRECTIONAL) {
                // the light shines along its -Z: the direction to it is its +Z axis
                float x = m[2], y = m[6], z = m[10];
                float len = (float) Math.sqrt(x * x + y * y + z * z);
                if (len == 0) len = 1;
                F[at] = 1; F[at + 4] = x / len; F[at + 5] = y / len; F[at + 6] = z / len;
            } else {
                F[at] = 2; F[at + 4] = m[3]; F[at + 5] = m[7]; F[at + 6] = m[11];
                F[at + 7] = l.constant; F[at + 8] = l.linear; F[at + 9] = l.quadratic;
            }
            n++;
        }
        I[Native.G3D_I_LIGHTS] = n;
    }

    // ---- clearing ----

    public void clear(Background background) {
        checkBound();
        if (background == null) {
            Native.g3dClear(pixels, I, 0, depthEnabled ? 3 : 1);
            return;
        }
        int flags = (background.colorClear ? 1 : 0) | (background.depthClear && depthEnabled ? 2 : 0);
        if (flags != 0) Native.g3dClear(pixels, I, background.color, flags);
        if (background.colorClear && background.image != null) drawBackgroundImage(background);
    }

    private static final short[] QUAD_POS = { -1, -1, 0, 1, -1, 0, 1, 1, 0, -1, 1, 0 };
    private static final int[] QUAD_IDX = { 0, 1, 2, 0, 2, 3 };
    private final short[] quadTc = new short[8];

    private void identityMatrices() {
        for (int i = 0; i < 32; i++) F[i] = (i % 16) % 5 == 0 ? 1 : 0;
    }

    /** One textured quad in normalised device coordinates (x0..x1, y0..y1), texels s0..s1, t0..t1. */
    private void drawQuad(Image2D img, float x0, float y0, float x1, float y1, float z,
                          float s0, float t0, float s1, float t1, boolean repeat, int flags, int blend, int alphaRef) {
        identityMatrices();
        // positions -1..1 scaled into the rectangle: x = px * sx + bx
        F[Native.G3D_F_POS_SCALE] = 1;
        float sx = (x1 - x0) / 2, sy = (y1 - y0) / 2;
        F[0] = sx; F[3] = (x0 + x1) / 2; F[5] = sy; F[7] = (y0 + y1) / 2; F[11] = z;
        F[Native.G3D_F_POS_BIAS] = 0; F[Native.G3D_F_POS_BIAS + 1] = 0; F[Native.G3D_F_POS_BIAS + 2] = 0;
        // texture coordinates in 1/4096 of the image
        float su = 4096f / img.width, sv = 4096f / img.height;
        quadTc[0] = (short) (s0 * su); quadTc[1] = (short) (t1 * sv);
        quadTc[2] = (short) (s1 * su); quadTc[3] = (short) (t1 * sv);
        quadTc[4] = (short) (s1 * su); quadTc[5] = (short) (t0 * sv);
        quadTc[6] = (short) (s0 * su); quadTc[7] = (short) (t0 * sv);
        F[Native.G3D_F_TC_SCALE] = 1f / 4096; F[Native.G3D_F_TC_BIAS] = 0; F[Native.G3D_F_TC_BIAS + 1] = 0;
        I[Native.G3D_I_FLAGS] = flags;
        I[Native.G3D_I_VERTICES] = 4; I[Native.G3D_I_INDICES] = 6;
        I[Native.G3D_I_POS_TYPE] = 2; I[Native.G3D_I_NRM_TYPE] = 0; I[Native.G3D_I_COL_COMPS] = 0;
        I[Native.G3D_I_DEF_COLOR] = 0xFFFFFFFF;
        I[Native.G3D_I_TC_TYPE] = 2; I[Native.G3D_I_TC_COMPS] = 2;
        I[Native.G3D_I_TEX_W] = img.width; I[Native.G3D_I_TEX_H] = img.height;
        I[Native.G3D_I_TEX_BLEND] = 0; I[Native.G3D_I_WRAP_S] = repeat ? 1 : 0; I[Native.G3D_I_WRAP_T] = repeat ? 1 : 0;
        I[Native.G3D_I_BLEND] = blend; I[Native.G3D_I_ALPHA_REF] = alphaRef; I[Native.G3D_I_LIGHTS] = 0;
        Native.g3dDraw(pixels, I, F, QUAD_POS, null, null, quadTc, QUAD_IDX, img.pix, img.alpha);
    }

    private void drawBackgroundImage(Background b) {
        if (b.cropW <= 0 || b.cropH <= 0) return;
        // the crop rectangle of the image fills the viewport
        drawQuad(b.image, -1, -1, 1, 1, 0, b.cropX, b.cropY, b.cropX + b.cropW, b.cropY + b.cropH,
                 false, 0, b.image.alpha != null ? 1 : 0, 0);
    }

    // ---- drawing ----

    public void render(VertexBuffer vertices, IndexBuffer triangles, Appearance appearance, Transform transform) {
        render(vertices, triangles, appearance, transform, -1);
    }

    public void render(VertexBuffer vertices, IndexBuffer triangles, Appearance appearance, Transform transform, int scope) {
        if (vertices == null || triangles == null || appearance == null) throw new NullPointerException();
        checkBound();
        loadLights(scope);
        draw(vertices, triangles, appearance, transform == null ? null : transform.m, 1f);
    }

    private final float[] tmp = new float[16];

    private void draw(VertexBuffer vb, IndexBuffer ib, Appearance ap, float[] model, float alphaFactor) {
        VertexArray pos = vb.positions;
        if (pos == null || ap == null) return;
        if (model == null) System.arraycopy(view, 0, F, Native.G3D_F_MV, 16);
        else { Transform.mul(view, model, tmp); System.arraycopy(tmp, 0, F, Native.G3D_F_MV, 16); }
        System.arraycopy(proj, 0, F, Native.G3D_F_PROJ, 16);
        F[Native.G3D_F_POS_SCALE] = vb.posScale;
        System.arraycopy(vb.posBias, 0, F, Native.G3D_F_POS_BIAS, 3);

        PolygonMode pm = ap.polygonMode;
        CompositingMode cm = ap.compositingMode;
        Material mat = ap.material;
        int flags = 0;
        int culling = pm == null ? PolygonMode.CULL_BACK : pm.culling;
        if (culling == PolygonMode.CULL_BACK) flags |= 0x1;
        else if (culling == PolygonMode.CULL_FRONT) flags |= 0x2;
        if (pm != null && pm.winding == PolygonMode.WINDING_CW) flags |= 0x4;
        if (pm != null && pm.shading == PolygonMode.SHADE_FLAT) flags |= 0x40;
        if (pm != null && pm.perspective) flags |= 0x80;
        if (pm != null && pm.twoSided) flags |= 0x100;
        if (depthEnabled && (cm == null || cm.depthTest)) flags |= 0x8;
        if (depthEnabled && (cm == null || cm.depthWrite)) flags |= 0x10;
        if (cm != null && !cm.colorWrite) flags |= 0x800;

        int color = vb.defaultColor;
        if (mat != null && vb.normals != null) {
            flags |= 0x20;
            if (mat.vertexColorTracking) flags |= 0x400;
            int at = Native.G3D_F_MATERIAL;
            F[at] = ((mat.ambient >> 16) & 255) / 255f; F[at + 1] = ((mat.ambient >> 8) & 255) / 255f; F[at + 2] = (mat.ambient & 255) / 255f;
            F[at + 3] = ((mat.diffuse >> 16) & 255) / 255f; F[at + 4] = ((mat.diffuse >> 8) & 255) / 255f; F[at + 5] = (mat.diffuse & 255) / 255f;
            F[at + 6] = (mat.diffuse >>> 24) / 255f * alphaFactor;
            F[at + 7] = ((mat.emissive >> 16) & 255) / 255f; F[at + 8] = ((mat.emissive >> 8) & 255) / 255f; F[at + 9] = (mat.emissive & 255) / 255f;
        }
        if (alphaFactor < 1f) color = (color & 0xFFFFFF) | ((int) ((color >>> 24) * alphaFactor) << 24);

        Texture2D tex = ap.textures[0];
        VertexArray tc = vb.texCoords[0];
        short[] texPix = null;
        byte[] texAlpha = null;
        if (tex != null && tc != null) {
            Image2D img = tex.image;
            texPix = img.pix; texAlpha = img.alpha;
            I[Native.G3D_I_TEX_W] = img.width; I[Native.G3D_I_TEX_H] = img.height;
            int func;
            switch (tex.blending) {
            case Texture2D.FUNC_REPLACE: func = 0; break;
            case Texture2D.FUNC_DECAL: func = 2; break;
            case Texture2D.FUNC_ADD: func = 3; break;
            case Texture2D.FUNC_BLEND: func = 4; break;
            default: func = 1; break;
            }
            I[Native.G3D_I_TEX_BLEND] = func; I[Native.G3D_I_TEX_COLOR] = tex.blendColor;
            I[Native.G3D_I_WRAP_S] = tex.wrapS == Texture2D.WRAP_REPEAT ? 1 : 0;
            I[Native.G3D_I_WRAP_T] = tex.wrapT == Texture2D.WRAP_REPEAT ? 1 : 0;
            I[Native.G3D_I_TC_TYPE] = tc.size; I[Native.G3D_I_TC_COMPS] = tc.comps;
            F[Native.G3D_F_TC_SCALE] = vb.texScale[0];
            System.arraycopy(vb.texBias[0], 0, F, Native.G3D_F_TC_BIAS, 3);
            Transform tt = new Transform();
            tex.getCompositeTransform(tt);
            System.arraycopy(tt.m, 0, F, Native.G3D_F_TEX_XF, 16);
            flags |= 0x200;
        }

        int blend;
        switch (cm == null ? CompositingMode.REPLACE : cm.blending) {
        case CompositingMode.ALPHA: blend = 1; break;
        case CompositingMode.ALPHA_ADD: blend = 2; break;
        case CompositingMode.MODULATE: blend = 3; break;
        case CompositingMode.MODULATE_X2: blend = 4; break;
        default: blend = 0; break;
        }
        I[Native.G3D_I_BLEND] = blend;
        I[Native.G3D_I_ALPHA_REF] = cm == null ? 0 : (int) (cm.alphaThreshold * 255);
        I[Native.G3D_I_FLAGS] = flags;
        I[Native.G3D_I_VERTICES] = pos.count;
        I[Native.G3D_I_INDICES] = ib.triangles.length;
        I[Native.G3D_I_POS_TYPE] = pos.size;
        I[Native.G3D_I_NRM_TYPE] = vb.normals == null ? 0 : vb.normals.size;
        I[Native.G3D_I_COL_COMPS] = vb.colors == null ? 0 : vb.colors.comps;
        I[Native.G3D_I_DEF_COLOR] = color;
        Native.g3dDraw(pixels, I, F, pos.data(), vb.normals == null ? null : vb.normals.data(),
                       vb.colors == null ? null : vb.colors.bytes, tc == null ? null : tc.data(),
                       ib.triangles, texPix, texAlpha);
    }

    public void render(Node node, Transform transform) {
        if (node == null) throw new NullPointerException();
        checkBound();
        loadLights(-1);
        Vector items = new Vector();
        gather(node, transform == null ? new Transform() : new Transform(transform), 1f, items, null);
        drawItems(items);
    }

    public void render(World world) {
        if (world == null) throw new NullPointerException();
        checkBound();
        Camera cam = world.getActiveCamera();
        if (cam == null) throw new IllegalStateException("the world has no active camera");
        Transform camToWorld = new Transform();
        if (!cam.getTransformTo(world, camToWorld)) throw new IllegalStateException("the camera is not in the world");
        // the world's own camera and lights replace the immediate-mode ones for this call
        Camera savedCam = camera;
        Transform savedT = new Transform(cameraTransform);
        Vector savedLights = new Vector(), savedLT = new Vector();
        for (int i = 0; i < lights.size(); i++) { savedLights.addElement(lights.elementAt(i)); savedLT.addElement(lightTransforms.elementAt(i)); }
        setCamera(cam, camToWorld);
        resetLights();
        clear(world.getBackground());
        Vector items = new Vector();
        gather(world, new Transform(), 1f, items, this);
        loadLights(-1);
        drawItems(items);
        resetLights();
        for (int i = 0; i < savedLights.size(); i++) { lights.addElement(savedLights.elementAt(i)); lightTransforms.addElement(savedLT.elementAt(i)); }
        setCamera(savedCam, savedT);
    }

    /** One thing to draw: a submesh or a sprite, with its node -> world transform. */
    private static final class Item {
        Node node;
        int submesh;
        VertexBuffer vertices;
        Transform toWorld;
        float alpha;
        int layer;
        boolean blended;
    }

    private void gather(Node node, Transform toWorld, float alpha, Vector items, Graphics3D lightSink) {
        if (!node.renderingEnable) return;
        alpha *= node.alphaFactor;
        if (node instanceof Light) {
            if (lightSink != null) lightSink.addLight((Light) node, toWorld);
        } else if (node instanceof Mesh) {
            Mesh m = (Mesh) node;
            if (m instanceof SkinnedMesh) {         // what hangs off the bones is drawn too
                Group sk = ((SkinnedMesh) m).getSkeleton();
                Transform st = new Transform();
                sk.getCompositeTransform(st);
                Transform t = new Transform(toWorld);
                t.postMultiply(st);
                gather(sk, t, alpha, items, lightSink);
            }
            for (int i = 0; i < m.submeshes.length; i++) {
                Appearance ap = m.appearances[i];
                if (ap == null) continue;
                Item it = new Item();
                it.node = m; it.submesh = i; it.toWorld = toWorld; it.alpha = alpha; it.layer = ap.layer;
                it.blended = ap.compositingMode != null && ap.compositingMode.blending != CompositingMode.REPLACE;
                insert(items, it);
            }
        } else if (node instanceof Sprite3D) {
            Sprite3D s = (Sprite3D) node;
            if (s.appearance == null) return;
            Item it = new Item();
            it.node = s; it.toWorld = toWorld; it.alpha = alpha; it.layer = s.appearance.layer;
            it.blended = s.image.alpha != null;
            insert(items, it);
        } else if (node instanceof Group) {
            Group g = (Group) node;
            Transform child = new Transform();
            for (int i = 0; i < g.children.size(); i++) {
                Node c = (Node) g.children.elementAt(i);
                c.getCompositeTransform(child);
                Transform t = new Transform(toWorld);
                t.postMultiply(child);
                gather(c, t, alpha, items, lightSink);
            }
        }
    }

    /** By layer, opaque before blended within a layer. */
    private static void insert(Vector items, Item it) {
        int key = it.layer * 2 + (it.blended ? 1 : 0), i = items.size();
        while (i > 0) {
            Item o = (Item) items.elementAt(i - 1);
            if (o.layer * 2 + (o.blended ? 1 : 0) <= key) break;
            i--;
        }
        items.insertElementAt(it, i);
    }

    private void drawItems(Vector items) {
        for (int i = 0; i < items.size(); i++) {
            Item it = (Item) items.elementAt(i);
            if (it.node instanceof Mesh) {
                Mesh m = (Mesh) it.node;
                // deformed vertices are computed once per mesh, at its first submesh
                if (it.submesh == 0 || it.vertices == null) it.vertices = m.renderVertices();
                draw(it.vertices, m.submeshes[it.submesh], m.appearances[it.submesh], it.toWorld.m, it.alpha);
            } else {
                drawSprite((Sprite3D) it.node, it.toWorld);
            }
        }
    }

    private void drawSprite(Sprite3D s, Transform toWorld) {
        if (s.cropW == 0 || s.cropH == 0) return;
        // the sprite's origin and its unit X and Y extents through the modelview and projection
        float[] mv = new float[16];
        Transform.mul(view, toWorld.m, mv);
        float ex = mv[3], ey = mv[7], ez = mv[11];
        float cx = proj[0] * ex + proj[1] * ey + proj[2] * ez + proj[3];
        float cy = proj[4] * ex + proj[5] * ey + proj[6] * ez + proj[7];
        float cz = proj[8] * ex + proj[9] * ey + proj[10] * ez + proj[11];
        float cw = proj[12] * ex + proj[13] * ey + proj[14] * ez + proj[15];
        if (cw <= 0 || cz < -cw || cz > cw) return;
        float nx = cx / cw, ny = cy / cw, nz = cz / cw;
        float hw, hh;       // half size in normalised device coordinates
        int w = Math.abs(s.cropW), h = Math.abs(s.cropH);
        if (s.scaled) {
            float sx = (float) Math.sqrt(mv[0] * mv[0] + mv[4] * mv[4] + mv[8] * mv[8]);
            float sy = (float) Math.sqrt(mv[1] * mv[1] + mv[5] * mv[5] + mv[9] * mv[9]);
            hw = sx * proj[0] / cw / 2; hh = sy * proj[5] / cw / 2;
        } else {
            hw = (float) w / vpW; hh = (float) h / vpH;
        }
        CompositingMode cm = s.appearance.compositingMode;
        int flags = 0;
        if (depthEnabled && (cm == null || cm.depthTest)) flags |= 0x8;
        if (depthEnabled && (cm == null || cm.depthWrite)) flags |= 0x10;
        float s0 = s.cropX, s1 = s.cropX + w, t0 = s.cropY, t1 = s.cropY + h;
        if (s.cropW < 0) { float t = s0; s0 = s1; s1 = t; }
        if (s.cropH < 0) { float t = t0; t0 = t1; t1 = t; }
        drawQuad(s.image, nx - hw, ny - hh, nx + hw, ny + hh, nz, s0, t0, s1, t1, false, flags,
                 s.image.alpha != null ? 1 : 0, cm == null ? 1 : Math.max(1, (int) (cm.alphaThreshold * 255)));
    }
}
