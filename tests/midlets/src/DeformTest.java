import javax.microedition.lcdui.*;
import javax.microedition.m3g.*;
import javax.microedition.midlet.*;

/** A two-bone skinned strip that bends and a mesh morphing between two shapes. */
public class DeformTest extends MIDlet implements Runnable {
    static class GC extends javax.microedition.lcdui.game.GameCanvas {
        GC() { super(true); }
        Graphics g() { return getGraphics(); }
    }
    GC canvas;
    volatile boolean running = true;
    World world = new World();
    Group upperBone;
    MorphingMesh morph;

    void build() {
        Appearance ap = new Appearance();
        PolygonMode pm = new PolygonMode(); pm.setCulling(PolygonMode.CULL_NONE); ap.setPolygonMode(pm);

        // a strip 2 wide, 6 high, in 6 segments: vertices (x = -1 / +1, y = 0..6)
        int segs = 6, n = (segs + 1) * 2;
        short[] p = new short[n * 3];
        byte[] c = new byte[n * 3];
        for (int i = 0; i <= segs; i++) {
            p[i * 6] = -1; p[i * 6 + 1] = (short) i; p[i * 6 + 3] = 1; p[i * 6 + 4] = (short) i;
            for (int k = 0; k < 2; k++) {
                c[(i * 2 + k) * 3] = (byte) (40 * i); c[(i * 2 + k) * 3 + 1] = (byte) 200; c[(i * 2 + k) * 3 + 2] = (byte) (255 - 40 * i);
            }
        }
        VertexArray pa = new VertexArray(n, 3, 2); pa.set(0, n, p);
        VertexArray ca = new VertexArray(n, 3, 1); ca.set(0, n, c);
        VertexBuffer vb = new VertexBuffer(); vb.setPositions(pa, 1f, null); vb.setColors(ca);
        Group skeleton = new Group();
        Group lower = new Group();
        upperBone = new Group();
        upperBone.setTranslation(0, 3, 0);
        skeleton.addChild(lower);
        lower.addChild(upperBone);
        SkinnedMesh arm = new SkinnedMesh(vb, new TriangleStripArray(0, new int[] { n }), ap, skeleton);
        arm.addTransform(lower, 1, 0, 8);          // rows 0..3
        arm.addTransform(upperBone, 1, 6, 8);      // rows 3..6: row 3 is shared, half and half
        arm.setTranslation(-3, -3, 0);
        world.addChild(arm);

        // morph: a square towards a tall diamond
        short[] sq = { -1, -1, 0, 1, -1, 0, -1, 1, 0, 1, 1, 0 };
        short[] di = { 0, -2, 0, 2, 0, 0, -2, 0, 0, 0, 2, 0 };
        VertexArray sa = new VertexArray(4, 3, 2); sa.set(0, 4, sq);
        VertexArray da = new VertexArray(4, 3, 2); da.set(0, 4, di);
        VertexBuffer base = new VertexBuffer(); base.setPositions(sa, 1f, null); base.setDefaultColor(0xFFFFC020);
        VertexBuffer target = new VertexBuffer(); target.setPositions(da, 1f, null);
        morph = new MorphingMesh(base, new VertexBuffer[] { target }, new TriangleStripArray(0, new int[] { 4 }), ap);
        morph.setTranslation(3, 0, 0);
        world.addChild(morph);

        Camera cam = new Camera();
        cam.setPerspective(60f, 1f, 0.5f, 50f);
        cam.setTranslation(0, 0, 9);
        world.addChild(cam);
        world.setActiveCamera(cam);
        Background bg = new Background(); bg.setColor(0x00181830); world.setBackground(bg);
    }

    public void run() {
        Graphics g = canvas.g();
        Graphics3D g3d = Graphics3D.getInstance();
        int frame = 0;
        while (running) {
            // the bend and the morph weight swing back and forth
            int phase = frame % 120;
            float s = (phase < 60 ? phase : 120 - phase) / 60f;
            upperBone.setOrientation(s * 80f, 0, 0, 1);
            morph.setWeights(new float[] { s });
            g3d.bindTarget(g);
            g3d.render(world);
            g3d.releaseTarget();
            g.setColor(0xFFFFFF);
            g.drawString("skin + morph " + frame, 2, 2, 0);
            canvas.flushGraphics();
            frame++;
            try { Thread.sleep(10); } catch (InterruptedException e) { }
        }
    }

    protected void startApp() {
        canvas = new GC();
        canvas.setFullScreenMode(true);
        try { build(); } catch (Throwable e) { System.out.println("build failed: " + e); e.printStackTrace(); }
        Display.getDisplay(this).setCurrent(canvas);
        new Thread(this).start();
    }
    protected void pauseApp() {}
    protected void destroyApp(boolean u) { running = false; }
}
