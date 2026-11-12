import com.mascotcapsule.micro3d.v3.*;
import javax.microedition.lcdui.*;
import javax.microedition.midlet.*;

/** Mascot Capsule micro3d v3: a posed figure, an old-format figure, primitives and a command list. */
public class McTest extends MIDlet implements Runnable {
    Figure fig, old;
    ActionTable act;
    Texture tex, shine;
    com.motorola.graphics.j3d.Figure motoFig;
    com.motorola.graphics.j3d.Texture motoTex;
    volatile boolean running = true;
    Canvas canvas;
    int frame;
    long t0;
    int shown;

    protected void startApp() {
        try {
            tex = new Texture("/mc.bmp", true);
            fig = new Figure("/mc.mbac");
            fig.setTexture(tex);
            old = new Figure("/mc_old.mbac");
            old.setTexture(tex);
            act = new ActionTable("/mc.mtra");
            shine = new Texture("/mc_sphere.bmp", false);
            motoTex = new com.motorola.graphics.j3d.Texture("/mc.bmp", true);
            motoFig = new com.motorola.graphics.j3d.Figure("/mc_old.mbac");
            motoFig.setTexture(motoTex);
            System.out.println("figure patterns " + fig.getNumPattern() + " actions " + act.getNumActions()
                               + " frames " + (act.getNumFrames(0) >> 16));
        } catch (Throwable t) {
            System.out.println("load failed: " + t);
            t.printStackTrace();
        }
        Vector3D v = new Vector3D(3, 4, 0);
        v.unit();
        AffineTrans r = new AffineTrans();
        r.setIdentity();
        r.setRotationZ(1024);
        Vector3D q = r.transform(new Vector3D(100, 0, 0));
        System.out.println("unit " + v.x + "," + v.y + " rotZ " + q.x + "," + q.y + " sin " + Util3D.sin(1024) + " sqrt " + Util3D.sqrt(144));

        canvas = new Canvas() {
            final Graphics3D g3 = new Graphics3D();
            final FigureLayout layout = new FigureLayout();
            final Effect3D effect = new Effect3D();
            final AffineTrans view = new AffineTrans(), turn = new AffineTrans(), m = new AffineTrans();

            protected void paint(Graphics g) {
                int w = getWidth(), h = getHeight();
                g.setColor(0x203050); g.fillRect(0, 0, w, h);
                g.setColor(0xFFFFFF); g.drawString("mascot capsule v3", 2, 2, Graphics.TOP | Graphics.LEFT);
                if (fig == null) return;
                effect.setLight(new Light(new Vector3D(-2000, 2000, 3000), 3000, 1400));
                view.setViewTrans(new Vector3D(0, -40, -420), new Vector3D(0, 300, 4096), new Vector3D(0, -4096, 0));
                turn.setIdentity();
                turn.setRotationY(frame * 24);
                m.multiply(view, turn);
                layout.setAffineTrans(m);
                layout.setCenter(w / 2 - 40, h / 2);
                layout.setPerspective(10, 4000, 512);
                fig.setPosture(act, 0, (frame % 32 < 16 ? frame % 32 : 32 - frame % 32) << 16);
                effect.setSphereMap(shine);
                g3.bind(g);
                g3.renderFigure(fig, 0, 0, layout, effect);
                effect.setSphereMap(null);

                // the old-format figure, small, drawn in parallel projection with toon shading
                effect.setShadingType(Effect3D.TOON_SHADING);
                effect.setToonParams(150, 255, 110);
                turn.setRotationX(300);
                m.multiply(view, turn);
                layout.setAffineTrans(m);
                layout.setScale(1700, 1700);
                g3.renderFigure(old, 95, -60, layout, effect);
                effect.setShadingType(Effect3D.NORMAL_SHADING);

                // primitives: a coloured triangle, a see-through textured quad, lines, points, a sprite
                AffineTrans id = new AffineTrans();
                id.setIdentity();
                layout.setAffineTrans(id);
                layout.setScale(4096, 4096);
                layout.setCenter(0, 0);
                int[] none = new int[0];
                g3.renderPrimitives(null, 0, 0, layout, effect, Graphics3D.PRIMITVE_TRIANGLES | Graphics3D.PDATA_COLOR_PER_COMMAND, 1,
                    new int[] { w - 60, h - 50, 0, w - 10, h - 50, 0, w - 35, h - 10, 0 }, none, none, new int[] { 0xFF8020 });
                g3.renderPrimitives(tex, 0, 0, layout, effect,
                    Graphics3D.PRIMITVE_QUADS | Graphics3D.PDATA_TEXURE_COORD | Graphics3D.PATTR_BLEND_HALF | Graphics3D.PATTR_COLORKEY, 1,
                    new int[] { w - 70, h - 70, -10, w - 20, h - 70, -10, w - 20, h - 20, -10, w - 70, h - 20, -10 }, none,
                    new int[] { 0, 0, 63, 0, 63, 63, 0, 63 }, none);
                g3.renderPrimitives(null, 0, 0, layout, effect, Graphics3D.PRIMITVE_LINES | Graphics3D.PDATA_COLOR_PER_FACE, 2,
                    new int[] { 5, h - 40, 0, 60, h - 10, 0, 5, h - 10, 0, 60, h - 40, 0 }, none, none, new int[] { 0x00FF00, 0xFFFF00 });
                g3.renderPrimitives(null, 0, 0, layout, effect, Graphics3D.PRIMITVE_POINTS | Graphics3D.PDATA_COLOR_PER_COMMAND, 3,
                    new int[] { 10, h - 50, 0, 20, h - 50, 0, 30, h - 50, 0 }, none, none, new int[] { 0xFFFFFF });
                g3.renderPrimitives(tex, 0, 0, layout, effect,
                    Graphics3D.PRIMITVE_POINT_SPRITES | Graphics3D.PDATA_POINT_SPRITE_PARAMS_PER_CMD, 1,
                    new int[] { 30, 50, 0 }, none,
                    new int[] { 32, 32, frame * 40, 0, 0, 31, 31, Graphics3D.POINT_SPRITE_PIXEL_SIZE }, none);
                // a command list: a new centre, then two coloured triangles with one colour each
                g3.drawCommandList((Texture) null, 0, 0, layout, effect, new int[] {
                    Graphics3D.COMMAND_LIST_VERSION_1_0,
                    Graphics3D.COMMAND_CENTER, w - 30, 30,
                    Graphics3D.PRIMITVE_TRIANGLES | Graphics3D.PDATA_COLOR_PER_FACE | (2 << 16),
                    -20, -10, 0, 0, -10, 0, -10, 10, 0,   0, -10, 0, 20, -10, 0, 10, 10, 0,
                    0x40C0FF, 0xFF40C0,
                    Graphics3D.COMMAND_END });
                g3.flush();
                g3.release(g);

                // the Motorola flavour of the API: the Graphics itself draws the figure
                com.motorola.graphics.j3d.AffineTrans mt = new com.motorola.graphics.j3d.AffineTrans();
                mt.setIdentity();
                mt.rotationY(frame * 30);
                mt.m23 = 300;
                com.motorola.graphics.j3d.FigureLayout ml = new com.motorola.graphics.j3d.FigureLayout(mt, 1400, 1400, w / 2 + 20, h - 45);
                ((com.motorola.graphics.j3d.Graphics3D) g).drawFigure(motoFig, 0, 0, ml, new com.motorola.graphics.j3d.Effect3D());
                shown++;
            }
        };
        Display.getDisplay(this).setCurrent(canvas);
        new Thread(this).start();
    }

    public void run() {
        t0 = System.currentTimeMillis();
        while (running) {
            frame++;
            canvas.repaint();
            canvas.serviceRepaints();
            if (frame % 50 == 0) {
                long t = System.currentTimeMillis();
                System.out.println("fps " + (50 * 1000 / Math.max(1, t - t0)));
                t0 = t;
            }
            try { Thread.sleep(10); } catch (InterruptedException e) {}
        }
    }
    protected void pauseApp() {}
    protected void destroyApp(boolean u) { running = false; }
}
