/*
 * amiga-j2me: JSR-184 (M3G 1.0/1.1) implementation for the Amiga J2ME runtime.
 * Written for this project; licensed under GPLv2 (see COPYING).
 */
package javax.microedition.m3g;

import aj.Native;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.Vector;
import javax.microedition.lcdui.Image;

/** Reads .m3g files (the JSR-184 binary format) and image files. */
public class Loader {
    private static final int[] ID = { 0xAB, 'J', 'S', 'R', '1', '8', '4', 0xBB, 0x0D, 0x0A, 0x1A, 0x0A };

    private final Vector objects = new Vector();        // by object index; [0] = null
    private final Vector referenced = new Vector();
    private final String base;                          // directory of the file, for external references
    private byte[] d;
    private int p, end;

    private Loader(String base) {
        this.base = base;
        objects.addElement(null);
    }

    public static Object3D[] load(String name) throws IOException {
        if (name == null) throw new NullPointerException();
        return load(name, 0);
    }

    private static Object3D[] load(String name, int depth) throws IOException {
        if (depth > 8) throw new IOException("external references nest too deeply");
        InputStream in = Loader.class.getResourceAsStream(name.startsWith("/") ? name : "/" + name);
        if (in == null) throw new IOException("cannot open " + name);
        ByteArrayOutputStream out = new ByteArrayOutputStream(8192);
        byte[] buf = new byte[4096];
        int n;
        try {
            while ((n = in.read(buf)) > 0) out.write(buf, 0, n);
        } finally {
            in.close();
        }
        int slash = name.lastIndexOf('/');
        return new Loader(slash >= 0 ? name.substring(0, slash + 1) : "/").parse(out.toByteArray(), 0, depth);
    }

    public static Object3D[] load(byte[] data, int offset) throws IOException {
        if (data == null) throw new NullPointerException();
        if (offset < 0 || offset >= data.length) throw new IndexOutOfBoundsException();
        return new Loader("/").parse(data, offset, 0);
    }

    private Object3D[] parse(byte[] data, int offset, int depth) throws IOException {
        boolean m3g = data.length - offset >= 12;
        for (int i = 0; m3g && i < 12; i++) m3g = (data[offset + i] & 255) == ID[i];
        if (!m3g) {     // an image file
            try {
                Image img = Image.createImage(data, offset, data.length - offset);
                int[] one = new int[1];
                boolean alpha = false;
                for (int y = 0; y < img.getHeight() && !alpha; y++) {
                    for (int x = 0; x < img.getWidth(); x++) {
                        img.getRGB(one, 0, 1, x, y, 1, 1);
                        if ((one[0] >>> 24) != 255) { alpha = true; break; }
                    }
                }
                return new Object3D[] { new Image2D(alpha ? Image2D.RGBA : Image2D.RGB, img) };
            } catch (IllegalArgumentException e) {
                throw new IOException("neither an M3G file nor an image");
            }
        }
        try {
            int pos = offset + 12;
            while (pos + 13 <= data.length) {
                int scheme = data[pos] & 255;
                int total = int32(data, pos + 1), unc = int32(data, pos + 5);
                if (total < 13 || pos + total > data.length) throw new IOException("bad section length");
                if (scheme == 0) {
                    d = data; p = pos + 9; end = pos + total - 4;
                } else if (scheme == 1) {
                    d = Native.inflate(data, pos + 9, total - 13, unc);
                    if (d == null) throw new IOException("damaged compressed section");
                    p = 0; end = d.length;
                } else {
                    throw new IOException("unknown compression scheme " + scheme);
                }
                while (p < end) {
                    int type = d[p] & 255, len = int32(d, p + 1);
                    int start = p + 5, objEnd = start + len;
                    if (len < 0 || objEnd > end) throw new IOException("bad object length");
                    p = start;
                    objects.addElement(readObject(type, objEnd, depth));
                    p = objEnd;
                }
                pos += total;
            }
        } catch (IOException e) {
            throw e;
        } catch (RuntimeException e) {
            throw new IOException("malformed M3G file: " + e);
        }
        // the roots: objects nothing else in the file refers to
        Vector roots = new Vector();
        for (int i = 1; i < objects.size(); i++) {
            Object o = objects.elementAt(i);
            if (o != null && !referenced.contains(o)) roots.addElement(o);
        }
        Object3D[] r = new Object3D[roots.size()];
        roots.copyInto(r);
        return r;
    }

    // ---- primitive readers (little-endian) ----

    private static int int32(byte[] b, int at) {
        return (b[at] & 255) | ((b[at + 1] & 255) << 8) | ((b[at + 2] & 255) << 16) | ((b[at + 3] & 255) << 24);
    }
    private int u8() { return d[p++] & 255; }
    private boolean bool() { return d[p++] != 0; }
    private int u16() { int v = (d[p] & 255) | ((d[p + 1] & 255) << 8); p += 2; return v; }
    private int i32() { int v = int32(d, p); p += 4; return v; }
    private float f32() { return Float.intBitsToFloat(i32()); }
    private int rgb() { int v = ((d[p] & 255) << 16) | ((d[p + 1] & 255) << 8) | (d[p + 2] & 255); p += 3; return v; }
    private int rgba() { int v = rgb(); return v | (u8() << 24); }

    private Object3D ref() throws IOException {
        int index = i32();
        if (index == 0) return null;
        if (index < 0 || index >= objects.size()) throw new IOException("forward or bad object reference " + index);
        Object3D o = (Object3D) objects.elementAt(index);
        if (o != null && !referenced.contains(o)) referenced.addElement(o);
        return o;
    }

    // ---- common parts ----

    private void object3D(Object3D o) throws IOException {
        o.setUserID(i32());
        int tracks = i32();
        for (int i = 0; i < tracks; i++) {
            Object3D t = ref();
            if (t instanceof AnimationTrack) o.addAnimationTrack((AnimationTrack) t);
        }
        int params = i32();
        for (int i = 0; i < params; i++) {     // user parameters are skipped
            i32();
            int n = i32();
            p += n;
        }
    }

    private void transformable(Transformable t) throws IOException {
        object3D(t);
        if (bool()) {
            float x = f32(), y = f32(), z = f32();
            t.setTranslation(x, y, z);
            x = f32(); y = f32(); z = f32();
            t.setScale(x, y, z);
            float angle = f32();
            x = f32(); y = f32(); z = f32();
            if (x != 0 || y != 0 || z != 0) t.setOrientation(angle, x, y, z);
        }
        if (bool()) {
            float[] m = new float[16];
            for (int i = 0; i < 16; i++) m[i] = f32();
            Transform tr = new Transform();
            tr.set(m);
            t.setTransform(tr);
        }
    }

    private void node(Node n) throws IOException {
        transformable(n);
        n.setRenderingEnable(bool());
        n.setPickingEnable(bool());
        n.setAlphaFactor(u8() / 255f);
        n.setScope(i32());
        if (bool()) { u8(); u8(); ref(); ref(); }       // alignment: not applied
    }

    private void group(Group g) throws IOException {
        node(g);
        int n = i32();
        for (int i = 0; i < n; i++) {
            Object3D c = ref();
            if (c instanceof Node && ((Node) c).getParent() == null && c != g) g.addChild((Node) c);
        }
    }

    // ---- objects ----

    private Object3D readObject(int type, int objEnd, int depth) throws IOException {
        switch (type) {
        case 0:         // header
            return null;
        case 255: {     // external reference
            StringBuffer uri = new StringBuffer();
            while (p < objEnd && d[p] != 0) uri.append((char) (d[p++] & 255));
            String name = uri.toString();
            Object3D[] r = load(name.startsWith("/") ? name : base + name, depth + 1);
            return r.length > 0 ? r[0] : null;
        }
        case 1: {
            AnimationController c = new AnimationController();
            object3D(c);
            float speed = f32(), weight = f32();
            int start = i32(), stop = i32();
            float refSeq = f32();
            int refWorld = i32();
            c.setActiveInterval(start, stop);
            c.setPosition(refSeq, refWorld);
            c.setSpeed(speed, refWorld);
            c.setWeight(weight);
            return c;
        }
        case 2: {
            // the sequence comes after the Object3D data: read that into a holder first
            Holder h = new Holder();
            object3D(h);
            Object3D seq = ref(), ctl = ref();
            AnimationTrack t = new AnimationTrack((KeyframeSequence) seq, i32());
            t.copyBase(h);
            t.setController((AnimationController) ctl);
            return t;
        }
        case 3: {
            Appearance a = new Appearance();
            object3D(a);
            a.setLayer((byte) u8());
            a.setCompositingMode((CompositingMode) ref());
            a.setFog((Fog) ref());
            a.setPolygonMode((PolygonMode) ref());
            a.setMaterial((Material) ref());
            int n = i32();
            for (int i = 0; i < n; i++) {
                Object3D t = ref();
                if (i < 2) a.setTexture(i, (Texture2D) t);
            }
            return a;
        }
        case 4: {
            Background b = new Background();
            object3D(b);
            b.setColor(rgba());
            b.setImage((Image2D) ref());
            int mx = u8(), my = u8();
            b.setImageMode(mx, my);
            int x = i32(), y = i32(), w = i32(), h = i32();
            b.setCrop(x, y, w, h);
            b.setDepthClearEnable(bool());
            b.setColorClearEnable(bool());
            return b;
        }
        case 5: {
            Camera c = new Camera();
            node(c);
            int proj = u8();
            if (proj == Camera.GENERIC) {
                float[] m = new float[16];
                for (int i = 0; i < 16; i++) m[i] = f32();
                Transform t = new Transform();
                t.set(m);
                c.setGeneric(t);
            } else {
                float fovy = f32(), aspect = f32(), near = f32(), far = f32();
                if (proj == Camera.PERSPECTIVE) c.setPerspective(fovy, aspect, near, far);
                else c.setParallel(fovy, aspect, near, far);
            }
            return c;
        }
        case 6: {
            CompositingMode c = new CompositingMode();
            object3D(c);
            c.setDepthTestEnable(bool());
            c.setDepthWriteEnable(bool());
            c.setColorWriteEnable(bool());
            c.setAlphaWriteEnable(bool());
            c.setBlending(u8());
            c.setAlphaThreshold(u8() / 255f);
            float factor = f32(), units = f32();
            c.setDepthOffset(factor, units);
            return c;
        }
        case 7: {
            Fog f = new Fog();
            object3D(f);
            f.setColor(rgb());
            int mode = u8();
            f.setMode(mode);
            if (mode == Fog.EXPONENTIAL) f.setDensity(f32());
            else { float near = f32(), far = f32(); f.setLinear(near, far); }
            return f;
        }
        case 8: {
            PolygonMode m = new PolygonMode();
            object3D(m);
            m.setCulling(u8());
            m.setShading(u8());
            m.setWinding(u8());
            m.setTwoSidedLightingEnable(bool());
            m.setLocalCameraLightingEnable(bool());
            m.setPerspectiveCorrectionEnable(bool());
            return m;
        }
        case 9: {
            Group g = new Group();
            group(g);
            return g;
        }
        case 10: {
            Holder h = new Holder();
            object3D(h);
            int format = u8();
            boolean mutable = bool();
            int w = i32(), ht = i32();
            Image2D img;
            if (mutable) {
                img = new Image2D(format, w, ht);
            } else {
                int pn = i32();
                byte[] palette = new byte[pn];
                System.arraycopy(d, p, palette, 0, pn);
                p += pn;
                int n = i32();
                byte[] pixels = new byte[n];
                System.arraycopy(d, p, pixels, 0, n);
                p += n;
                img = pn > 0 ? new Image2D(format, w, ht, pixels, palette) : new Image2D(format, w, ht, pixels);
            }
            img.copyBase(h);
            return img;
        }
        case 11: {
            Holder h = new Holder();
            object3D(h);
            int enc = u8(), first = 0;
            int[] indices = null;
            switch (enc) {
            case 0: first = i32(); break;
            case 1: first = u8(); break;
            case 2: first = u16(); break;
            case 128: { int n = i32(); indices = new int[n]; for (int i = 0; i < n; i++) indices[i] = i32(); } break;
            case 129: { int n = i32(); indices = new int[n]; for (int i = 0; i < n; i++) indices[i] = u8(); } break;
            case 130: { int n = i32(); indices = new int[n]; for (int i = 0; i < n; i++) indices[i] = u16(); } break;
            default: throw new IOException("bad index encoding " + enc);
            }
            int ns = i32();
            int[] strips = new int[ns];
            for (int i = 0; i < ns; i++) strips[i] = i32();
            TriangleStripArray t = indices == null ? new TriangleStripArray(first, strips) : new TriangleStripArray(indices, strips);
            t.copyBase(h);
            return t;
        }
        case 12: {
            Light l = new Light();
            node(l);
            float c = f32(), lin = f32(), q = f32();
            l.setAttenuation(c, lin, q);
            l.setColor(rgb());
            l.setMode(u8());
            l.setIntensity(f32());
            l.setSpotAngle(f32());
            l.setSpotExponent(f32());
            return l;
        }
        case 13: {
            Material m = new Material();
            object3D(m);
            m.setColor(Material.AMBIENT, rgb());
            m.setColor(Material.DIFFUSE, rgba());
            m.setColor(Material.EMISSIVE, rgb());
            m.setColor(Material.SPECULAR, rgb());
            m.setShininess(f32());
            m.setVertexColorTrackingEnable(bool());
            return m;
        }
        case 14: case 15: case 16: {
            Holder h = new Holder();
            node(h);
            VertexBuffer vb = (VertexBuffer) ref();
            int n = i32();
            IndexBuffer[] ib = new IndexBuffer[n];
            Appearance[] ap = new Appearance[n];
            for (int i = 0; i < n; i++) { ib[i] = (IndexBuffer) ref(); ap[i] = (Appearance) ref(); }
            Mesh m;
            if (type == 15) {
                int targets = i32();
                VertexBuffer[] t = new VertexBuffer[targets];
                float[] w = new float[targets];
                for (int i = 0; i < targets; i++) { t[i] = (VertexBuffer) ref(); w[i] = f32(); }
                MorphingMesh mm = new MorphingMesh(vb, t, ib, ap);
                mm.setWeights(w);
                m = mm;
            } else if (type == 16) {
                Group skeleton = (Group) ref();
                SkinnedMesh sm = new SkinnedMesh(vb, ib, ap, skeleton);
                int refs = i32();
                for (int i = 0; i < refs; i++) {
                    Object3D bone = ref();
                    int first = i32(), count = i32(), weight = i32();
                    if (bone instanceof Node && weight > 0 && count > 0) sm.addTransform((Node) bone, weight, first, count);
                }
                m = sm;
            } else {
                m = new Mesh(vb, ib, ap);
            }
            m.copyNode(h);
            return m;
        }
        case 17: {
            Holder h = new Holder();
            transformable(h);
            Texture2D t = new Texture2D((Image2D) ref());
            t.copyTransformable(h);
            t.setBlendColor(rgb());
            t.setBlending(u8());
            int ws = u8(), wt = u8();
            t.setWrapping(ws, wt);
            int lf = u8(), imf = u8();
            t.setFiltering(lf, imf);
            return t;
        }
        case 18: {
            Holder h = new Holder();
            node(h);
            Image2D img = (Image2D) ref();
            Appearance ap = (Appearance) ref();
            Sprite3D s = new Sprite3D(bool(), img, ap);
            s.copyNode(h);
            int x = i32(), y = i32(), w = i32(), ht = i32();
            s.setCrop(x, y, w, ht);
            return s;
        }
        case 19: {
            Holder h = new Holder();
            object3D(h);
            int interp = u8(), repeat = u8(), enc = u8();
            int duration = i32(), first = i32(), last = i32(), comps = i32(), n = i32();
            KeyframeSequence k = new KeyframeSequence(n, comps, interp);
            k.copyBase(h);
            float[] v = new float[comps];
            if (enc == 0) {
                for (int i = 0; i < n; i++) {
                    int time = i32();
                    for (int c = 0; c < comps; c++) v[c] = f32();
                    k.setKeyframe(i, time, v);
                }
            } else {
                float[] bias = new float[comps], scale = new float[comps];
                for (int c = 0; c < comps; c++) bias[c] = f32();
                for (int c = 0; c < comps; c++) scale[c] = f32();
                for (int i = 0; i < n; i++) {
                    int time = i32();
                    for (int c = 0; c < comps; c++) {
                        v[c] = enc == 1 ? bias[c] + scale[c] * u8() / 255f : bias[c] + scale[c] * u16() / 65535f;
                    }
                    k.setKeyframe(i, time, v);
                }
            }
            k.setDuration(duration);
            k.setRepeatMode(repeat);
            k.setValidRange(first, last);
            return k;
        }
        case 20: {
            Holder h = new Holder();
            object3D(h);
            int size = u8(), comps = u8(), enc = u8(), n = u16();
            VertexArray a = new VertexArray(n, comps, size);
            a.copyBase(h);
            if (size == 1) {
                byte[] v = new byte[n * comps];
                for (int i = 0; i < v.length; i++) {
                    v[i] = (byte) u8();
                    if (enc == 1 && i >= comps) v[i] += v[i - comps];      // deltas from the vertex before
                }
                a.set(0, n, v);
            } else {
                short[] v = new short[n * comps];
                for (int i = 0; i < v.length; i++) {
                    v[i] = (short) u16();
                    if (enc == 1 && i >= comps) v[i] += v[i - comps];
                }
                a.set(0, n, v);
            }
            return a;
        }
        case 21: {
            VertexBuffer b = new VertexBuffer();
            object3D(b);
            b.setDefaultColor(rgba());
            VertexArray pos = (VertexArray) ref();
            float[] bias = { f32(), f32(), f32() };
            float scale = f32();
            b.setPositions(pos, scale, bias);
            b.setNormals((VertexArray) ref());
            b.setColors((VertexArray) ref());
            int n = i32();
            for (int i = 0; i < n; i++) {
                VertexArray tc = (VertexArray) ref();
                float[] tb = { f32(), f32(), f32() };
                float ts = f32();
                if (i < 2) b.setTexCoords(i, tc, ts, tb);
            }
            return b;
        }
        case 22: {
            World w = new World();
            group(w);
            Object3D cam = ref(), bg = ref();
            if (cam instanceof Camera) w.setActiveCamera((Camera) cam);
            w.setBackground((Background) bg);
            return w;
        }
        default:
            throw new IOException("unknown M3G object type " + type);
        }
    }

    /** Carries the common fields read before the object itself can be built. */
    private static final class Holder extends Node {
    }
}
