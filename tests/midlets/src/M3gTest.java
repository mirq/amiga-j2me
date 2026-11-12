import javax.microedition.lcdui.*;
import javax.microedition.m3g.*;
import javax.microedition.midlet.*;

/** M3G: immediate mode (lit textured cube, coloured triangle) and a World with camera, light and sprite. */
public class M3gTest extends MIDlet implements Runnable {
    static final short[] POS = { 1,1,1, -1,1,1, 1,-1,1, -1,-1,1,   -1,1,-1, 1,1,-1, -1,-1,-1, 1,-1,-1,
        -1,1,1, -1,1,-1, -1,-1,1, -1,-1,-1,   1,1,-1, 1,1,1, 1,-1,-1, 1,-1,1,
        1,1,-1, -1,1,-1, 1,1,1, -1,1,1,   1,-1,1, -1,-1,1, 1,-1,-1, -1,-1,-1 };
    static final byte[] NRM = { 0,0,127, 0,0,127, 0,0,127, 0,0,127,   0,0,-127, 0,0,-127, 0,0,-127, 0,0,-127,
        -127,0,0, -127,0,0, -127,0,0, -127,0,0,   127,0,0, 127,0,0, 127,0,0, 127,0,0,
        0,127,0, 0,127,0, 0,127,0, 0,127,0,   0,-127,0, 0,-127,0, 0,-127,0, 0,-127,0 };
    static final short[] TEX = { 1,0, 0,0, 1,1, 0,1,  1,0, 0,0, 1,1, 0,1,  1,0, 0,0, 1,1, 0,1,
        1,0, 0,0, 1,1, 0,1,  1,0, 0,0, 1,1, 0,1,  1,0, 0,0, 1,1, 0,1 };

    GameCanvas canvas;
    volatile boolean running = true;
    Graphics3D g3d = Graphics3D.getInstance();
    VertexBuffer cubeVb; IndexBuffer cubeIb; Appearance cubeAp;
    VertexBuffer triVb; IndexBuffer triIb; Appearance triAp;
    Camera camera = new Camera();
    Light light = new Light();
    World world;
    Mesh worldCube;
    Background bg = new Background();

    static class GameCanvas extends javax.microedition.lcdui.game.GameCanvas {
        GameCanvas() { super(true); }
        Graphics g() { return getGraphics(); }
    }

    void build() {
        VertexArray p = new VertexArray(24, 3, 2); p.set(0, 24, POS);
        VertexArray n = new VertexArray(24, 3, 1); n.set(0, 24, NRM);
        VertexArray t = new VertexArray(24, 2, 2); t.set(0, 24, TEX);
        cubeVb = new VertexBuffer();
        cubeVb.setPositions(p, 1.0f, null);
        cubeVb.setNormals(n);
        cubeVb.setTexCoords(0, t, 1.0f, null);
        cubeIb = new TriangleStripArray(0, new int[] { 4, 4, 4, 4, 4, 4 });
        byte[] tex = new byte[32 * 32 * 3];
        for (int y = 0; y < 32; y++) for (int x = 0; x < 32; x++) {
            boolean on = ((x >> 3) + (y >> 3)) % 2 == 0;
            int i = (y * 32 + x) * 3;
            tex[i] = (byte) (on ? 255 : 40); tex[i + 1] = (byte) (on ? 220 : 90); tex[i + 2] = (byte) (on ? 80 : 200);
        }
        Texture2D texture = new Texture2D(new Image2D(Image2D.RGB, 32, 32, tex));
        texture.setBlending(Texture2D.FUNC_MODULATE);
        texture.setWrapping(Texture2D.WRAP_CLAMP, Texture2D.WRAP_CLAMP);
        cubeAp = new Appearance();
        cubeAp.setTexture(0, texture);
        Material m = new Material();
        m.setColor(Material.DIFFUSE, 0xFFFFFFFF);
        m.setColor(Material.AMBIENT, 0x00606060);
        cubeAp.setMaterial(m);
        PolygonMode pm = new PolygonMode();
        pm.setPerspectiveCorrectionEnable(true);
        cubeAp.setPolygonMode(pm);

        VertexArray tp = new VertexArray(3, 3, 2); tp.set(0, 3, new short[] { -1, -1, 0, 1, -1, 0, 0, 1, 0 });
        VertexArray tc = new VertexArray(3, 3, 1); tc.set(0, 3, new byte[] { (byte) 255, 0, 0, 0, (byte) 255, 0, 0, 0, (byte) 255 });
        triVb = new VertexBuffer(); triVb.setPositions(tp, 1.0f, null); triVb.setColors(tc);
        triIb = new TriangleStripArray(0, new int[] { 3 });
        triAp = new Appearance();
        PolygonMode none = new PolygonMode(); none.setCulling(PolygonMode.CULL_NONE);
        triAp.setPolygonMode(none);

        light.setMode(Light.DIRECTIONAL);
        light.setIntensity(1.2f);
        bg.setColor(0x00203050);

        // retained mode: a world with its own camera, two lights, a cube and a sprite
        world = new World();
        Camera wc = new Camera();
        wc.setPerspective(50f, 1f, 0.5f, 50f);
        wc.setTranslation(0, 0, 6);
        world.addChild(wc);
        world.setActiveCamera(wc);
        Light amb = new Light(); amb.setMode(Light.AMBIENT); amb.setIntensity(0.4f); world.addChild(amb);
        Light dir = new Light(); dir.setMode(Light.DIRECTIONAL); dir.setOrientation(-40, 1, 0, 0); world.addChild(dir);
        worldCube = new Mesh(cubeVb, cubeIb, cubeAp);
        world.addChild(worldCube);
        byte[] sp = new byte[16 * 16 * 4];
        for (int y = 0; y < 16; y++) for (int x = 0; x < 16; x++) {
            int dx = x - 8, dy = y - 8, i = (y * 16 + x) * 4;
            boolean in = dx * dx + dy * dy < 49;
            sp[i] = (byte) 255; sp[i + 1] = (byte) (in ? 60 : 0); sp[i + 2] = (byte) 60; sp[i + 3] = (byte) (in ? 255 : 0);
        }
        Sprite3D sprite = new Sprite3D(true, new Image2D(Image2D.RGBA, 16, 16, sp), new Appearance());
        sprite.setTranslation(2.2f, 1.6f, 0);
        world.addChild(sprite);
        Background wb = new Background(); wb.setColor(0x00402030); world.setBackground(wb);
    }

    public void run() {
        Graphics g = canvas.g();
        int w = canvas.getWidth(), h = canvas.getHeight(), frame = 0, fps = 0, n = 0;
        long t0 = System.currentTimeMillis();
        Transform t = new Transform();
        while (running) {
            float angle = frame * 3f;
            // top half: immediate mode
            g.setClip(0, 0, w, h / 2);
            g3d.bindTarget(g);
            g3d.setViewport(0, 0, w, h / 2);
            camera.setPerspective(50f, (float) w / (h / 2), 0.5f, 50f);
            Transform ct = new Transform(); ct.postTranslate(0, 0, 6);
            g3d.setCamera(camera, ct);
            g3d.resetLights();
            Transform lt = new Transform(); lt.postRotate(-30, 1, 0, 0); lt.postRotate(25, 0, 1, 0);
            g3d.addLight(light, lt);
            g3d.clear(bg);
            t.setIdentity(); t.postTranslate(-1.6f, 0, 0); t.postRotate(angle, 1, 1, 0);
            g3d.render(cubeVb, cubeIb, cubeAp, t);
            t.setIdentity(); t.postTranslate(1.8f, 0, 0); t.postRotate(angle * 2, 0, 1, 0);
            g3d.render(triVb, triIb, triAp, t);
            g3d.releaseTarget();
            // bottom half: retained mode
            g.setClip(0, h / 2, w, h - h / 2);
            worldCube.setOrientation(angle, 0, 1, 0.3f);
            g3d.bindTarget(g);
            g3d.setViewport(0, h / 2, w, h - h / 2);
            g3d.render(world);
            g3d.releaseTarget();
            g.setClip(0, 0, w, h);
            g.setColor(0xFFFFFF);
            g.drawString("M3G frame " + frame + " fps " + fps, 2, 2, 0);
            canvas.flushGraphics();
            frame++; n++;
            long now = System.currentTimeMillis();
            if (now - t0 >= 1000) { fps = (int) (n * 1000 / (now - t0)); n = 0; t0 = now; System.out.println("fps " + fps); }
            try { Thread.sleep(5); } catch (InterruptedException e) { }
        }
    }

    protected void startApp() {
        canvas = new GameCanvas();
        canvas.setFullScreenMode(true);
        try {
            build();
        } catch (Throwable e) {
            System.out.println("build failed: " + e);
            e.printStackTrace();
        }
        Display.getDisplay(this).setCurrent(canvas);
        new Thread(this).start();
    }
    protected void pauseApp() {}
    protected void destroyApp(boolean u) { running = false; }
}
