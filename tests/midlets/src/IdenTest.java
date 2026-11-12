import com.motorola.iden.micro3d.*;
import javax.microedition.lcdui.*;
import javax.microedition.midlet.*;

/** Motorola iDEN micro3d: a figure, a lit triangle, a textured quadrangle, a line and a sprite through a Renderer. */
public class IdenTest extends MIDlet implements Runnable {
    Canvas canvas;
    volatile boolean running = true;
    int frame;

    protected void startApp() {
        try {
            final Texture tex = Texture.createTexture("/mc.bmp", false);
            final Texture shine = Texture.createTexture("/mc_sphere.bmp", true);
            final Figure fig = Figure.createFigure("/mc.mbac");
            final ActionTable act = ActionTable.createActionTable("/mc.mtra");
            MultiTexture mt = new MultiTexture();
            mt.addTexture(tex);
            fig.setTexture(mt);
            fig.setSphereTexture(shine);
            fig.setActionTable(act);
            System.out.println("iden: actions " + act.getNumberOfActions() + " patterns " + fig.getNumberOfPatterns()
                               + " textures " + mt.size() + " version " + Utility.getVersion());
            final Layout3D scene = new Layout3D(), flat = new Layout3D();
            scene.setLight(new Light(1500, new Vector3D(-2000, 2000, 3000), 3000));
            scene.setViewPoint(new Vector3D(0, -40, -420), new Vector3D(0, 300, 4096), new Vector3D(0, -4096, 0));
            scene.setProjection(Layout3D.PERSPECTIVE_FOV, new int[] { 10, 4000, 512 });
            fig.setLayout(scene);
            flat.setProjection(Layout3D.PARALLEL_SCALE, new int[] { 4096, 4096 });
            flat.setLight(new Light(1000, new Vector3D(0, 0, 4096), 4000));
            flat.setSemiTransparent(true);
            final Renderer r = new Renderer();
            canvas = new Canvas() {
                protected void paint(Graphics g) {
                    int w = getWidth(), h = getHeight();
                    g.setColor(0x402838); g.fillRect(0, 0, w, h);
                    g.setColor(0xFFFFFF); g.drawString("iden micro3d", 2, 2, Graphics.TOP | Graphics.LEFT);
                    scene.rotateY(frame * 24);
                    fig.setPosture(0, (frame % 32 < 16 ? frame % 32 : 32 - frame % 32) << 16);
                    r.draw(fig, w / 2, h / 2);
                    r.draw(new Triangle(new Vector3D(10, 40, 0), new Vector3D(60, 40, 0), new Vector3D(35, 80, 0),
                                        new Vector3D(0, 0, -4096), flat, 0x30D0FF), 0, 0);
                    Quadrangle q = new Quadrangle(new Vector3D(w - 70, 30, 0), new Vector3D(w - 10, 30, 0),
                                                  new Vector3D(w - 10, 90, 0), new Vector3D(w - 70, 90, 0),
                                                  0, 0, 63, 0, 63, 63, 0, 63, flat, tex);
                    q.enableColorKeyTransparency(true);
                    q.setBlendingType(Primitive.BLENDING_HALF);
                    r.draw(q, 0, 0);
                    r.draw(new Line(new Vector3D(5, h - 10, 0), new Vector3D(w - 5, h - 30, 0), flat, 0xFFFF00), 0, 0);
                    r.draw(new Point(new Vector3D(20, h - 40, 0), flat, 0xFFFFFF), 0, 0);
                    r.draw(new PointSprite(new Vector3D(40, h - 70, 0), 40, 40, frame * 50, 0, 0, 32, 32,
                                           PointSprite.PIXEL_SIZE | PointSprite.PARALLEL_PROJECTION, flat, tex), 0, 0);
                    r.paint(g);
                }
            };
            Display.getDisplay(this).setCurrent(canvas);
            new Thread(this).start();
        } catch (Throwable t) {
            System.out.println("iden failed: " + t);
            t.printStackTrace();
        }
    }

    public void run() {
        while (running) {
            frame++;
            canvas.repaint();
            canvas.serviceRepaints();
            try { Thread.sleep(20); } catch (InterruptedException e) {}
        }
    }
    protected void pauseApp() {}
    protected void destroyApp(boolean u) { running = false; }
}
