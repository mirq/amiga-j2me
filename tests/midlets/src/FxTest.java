import javax.microedition.lcdui.*;
import javax.microedition.m3g.*;
import javax.microedition.midlet.*;

/** M3G fog, specular highlight, second texture unit and picking. */
public class FxTest extends MIDlet {
    static Image2D checker(int a, int b) {
        byte[] t = new byte[16 * 16 * 3];
        for (int y = 0; y < 16; y++) for (int x = 0; x < 16; x++) {
            int c = ((x >> 2) + (y >> 2)) % 2 == 0 ? a : b, i = (y * 16 + x) * 3;
            t[i] = (byte) (c >> 16); t[i + 1] = (byte) (c >> 8); t[i + 2] = (byte) c;
        }
        return new Image2D(Image2D.RGB, 16, 16, t);
    }

    static Mesh sphere(Appearance ap) {
        int seg = 14, n = (seg + 1) * (seg + 1);
        short[] p = new short[n * 3]; byte[] nr = new byte[n * 3];
        for (int i = 0; i <= seg; i++) for (int j = 0; j <= seg; j++) {
            double th = Math.PI * i / seg, ph = 2 * Math.PI * j / seg;
            double x = Math.sin(th) * Math.cos(ph), y = Math.cos(th), z = Math.sin(th) * Math.sin(ph);
            int k = (i * (seg + 1) + j) * 3;
            p[k] = (short) (x * 1000); p[k + 1] = (short) (y * 1000); p[k + 2] = (short) (z * 1000);
            nr[k] = (byte) (x * 127); nr[k + 1] = (byte) (y * 127); nr[k + 2] = (byte) (z * 127);
        }
        int[] idx = new int[seg * (seg + 1) * 2], len = new int[seg];
        for (int i = 0, o = 0; i < seg; i++) {
            len[i] = (seg + 1) * 2;
            for (int j = 0; j <= seg; j++) { idx[o++] = (i + 1) * (seg + 1) + j; idx[o++] = i * (seg + 1) + j; }
        }
        VertexArray pa = new VertexArray(n, 3, 2); pa.set(0, n, p);
        VertexArray na = new VertexArray(n, 3, 1); na.set(0, n, nr);
        VertexBuffer vb = new VertexBuffer(); vb.setPositions(pa, 0.001f, null); vb.setNormals(na);
        return new Mesh(vb, new TriangleStripArray(idx, len), ap);
    }

    protected void startApp() {
        final World world = new World();
        PolygonMode both = new PolygonMode(); both.setCulling(PolygonMode.CULL_NONE); both.setPerspectiveCorrectionEnable(true);

        // 1. a floor running into red fog
        Appearance floorAp = new Appearance();
        Fog fog = new Fog(); fog.setMode(Fog.LINEAR); fog.setLinear(4, 22); fog.setColor(0xC03030);
        floorAp.setFog(fog); floorAp.setPolygonMode(both);
        Texture2D ft = new Texture2D(checker(0xFFFFFF, 0x2040C0)); ft.setBlending(Texture2D.FUNC_REPLACE);
        floorAp.setTexture(0, ft);
        VertexArray fp = new VertexArray(4, 3, 2); fp.set(0, 4, new short[] { -4, 0, 0, 4, 0, 0, -4, 0, -30, 4, 0, -30 });
        VertexArray ftc = new VertexArray(4, 2, 2); ftc.set(0, 4, new short[] { 0, 0, 2, 0, 0, 8, 2, 8 });
        VertexBuffer fvb = new VertexBuffer(); fvb.setPositions(fp, 1f, null); fvb.setTexCoords(0, ftc, 1f, null);
        Mesh floor = new Mesh(fvb, new TriangleStripArray(0, new int[] { 4 }), floorAp);
        floor.setTranslation(0, -2.2f, 2);
        world.addChild(floor);

        // 2. a shiny sphere
        Appearance sAp = new Appearance();
        Material m = new Material();
        m.setColor(Material.DIFFUSE, 0xFF2060A0); m.setColor(Material.AMBIENT, 0x102030);
        m.setColor(Material.SPECULAR, 0xFFFFFF); m.setShininess(40);
        sAp.setMaterial(m);
        Mesh ball = sphere(sAp);
        ball.setTranslation(-1.6f, 0.6f, 0);
        world.addChild(ball);

        // 3. two texture units: a checker times a dark-to-light map
        Appearance tAp = new Appearance(); tAp.setPolygonMode(both);
        Texture2D t0 = new Texture2D(checker(0xFFE040, 0x40C040)); t0.setBlending(Texture2D.FUNC_REPLACE);
        byte[] lm = new byte[16 * 16 * 3];
        for (int y = 0; y < 16; y++) for (int x = 0; x < 16; x++) { int i = (y * 16 + x) * 3; lm[i] = lm[i + 1] = lm[i + 2] = (byte) (x * 17); }
        Texture2D t1 = new Texture2D(new Image2D(Image2D.RGB, 16, 16, lm)); t1.setBlending(Texture2D.FUNC_MODULATE);
        t1.setWrapping(Texture2D.WRAP_CLAMP, Texture2D.WRAP_CLAMP);
        tAp.setTexture(0, t0); tAp.setTexture(1, t1);
        VertexArray qp = new VertexArray(4, 3, 2); qp.set(0, 4, new short[] { -1, -1, 0, 1, -1, 0, -1, 1, 0, 1, 1, 0 });
        VertexArray qt = new VertexArray(4, 2, 2); qt.set(0, 4, new short[] { 0, 1, 1, 1, 0, 0, 1, 0 });
        VertexBuffer qvb = new VertexBuffer(); qvb.setPositions(qp, 1f, null);
        qvb.setTexCoords(0, qt, 1f, null); qvb.setTexCoords(1, qt, 1f, null);
        final Mesh quad = new Mesh(qvb, new TriangleStripArray(0, new int[] { 4 }), tAp);
        quad.setTranslation(1.7f, 0.6f, 0);
        quad.setUserID(77);
        world.addChild(quad);

        final Camera cam = new Camera();
        cam.setPerspective(55f, 1f, 0.5f, 60f);
        cam.setTranslation(0, 0, 6);
        world.addChild(cam); world.setActiveCamera(cam);
        Light amb = new Light(); amb.setMode(Light.AMBIENT); amb.setIntensity(0.3f); world.addChild(amb);
        Light dir = new Light(); dir.setMode(Light.DIRECTIONAL); dir.setOrientation(-35, 1, 0, 0); dir.postRotate(-30, 0, 1, 0);
        world.addChild(dir);
        Background bg = new Background(); bg.setColor(0x00101018); world.setBackground(bg);

        // 4. picking: through the quad, through the ball, and into nothing
        RayIntersection ri = new RayIntersection();
        float[][] pts = { { 0.76f, 0.42f }, { 0.26f, 0.42f }, { 0.5f, 0.05f } };
        for (int i = 0; i < pts.length; i++) {
            boolean hit = world.pick(-1, pts[i][0], pts[i][1], cam, ri);
            System.out.println("pick " + i + ": " + (hit ? (ri.getIntersected() == quad ? "quad" : (ri.getIntersected() == ball ? "ball" : "other"))
                + " s=" + (int) (ri.getTextureS(0) * 100) + " nz=" + (int) (ri.getNormalZ() * 100) + " d=" + (int) (ri.getDistance() * 1000) : "nothing"));
        }

        Canvas c = new Canvas() {
            protected void paint(Graphics g) {
                Graphics3D g3d = Graphics3D.getInstance();
                g3d.bindTarget(g);
                g3d.render(world);
                g3d.releaseTarget();
                g.setColor(0xFFFFFF);
                g.drawString("fog  specular  2 textures", 2, 2, 0);
            }
        };
        c.setFullScreenMode(true);
        Display.getDisplay(this).setCurrent(c);
    }
    protected void pauseApp() {}
    protected void destroyApp(boolean u) {}
}
