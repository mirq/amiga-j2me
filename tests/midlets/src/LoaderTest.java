import javax.microedition.lcdui.*;
import javax.microedition.m3g.*;
import javax.microedition.midlet.*;

/** Loads /scene.m3g, animates and renders its World. */
public class LoaderTest extends MIDlet implements Runnable {
    static class GC extends javax.microedition.lcdui.game.GameCanvas {
        GC() { super(true); }
        Graphics g() { return getGraphics(); }
    }
    GC canvas;
    World world;
    volatile boolean running = true;

    public void run() {
        Graphics g = canvas.g();
        Graphics3D g3d = Graphics3D.getInstance();
        int time = 0;
        while (running) {
            if (world != null) {
                world.animate(time);
                g3d.bindTarget(g);
                g3d.render(world);
                g3d.releaseTarget();
            }
            g.setColor(0xFFFFFF);
            g.drawString(world == null ? "no world" : "scene.m3g t=" + time, 2, 2, 0);
            canvas.flushGraphics();
            time += 100;
            try { Thread.sleep(20); } catch (InterruptedException e) { }
        }
    }

    protected void startApp() {
        canvas = new GC();
        canvas.setFullScreenMode(true);
        try {
            Object3D[] roots = Loader.load("/scene.m3g");
            System.out.println("roots " + roots.length);
            for (int i = 0; i < roots.length; i++) {
                System.out.println("  " + roots[i].getClass().getName());
                if (roots[i] instanceof World) world = (World) roots[i];
            }
            if (world != null) System.out.println("children " + world.getChildCount() + " camera " + (world.getActiveCamera() != null));
        } catch (Throwable e) {
            System.out.println("load failed: " + e);
            e.printStackTrace();
        }
        Display.getDisplay(this).setCurrent(canvas);
        new Thread(this).start();
    }
    protected void pauseApp() {}
    protected void destroyApp(boolean u) { running = false; }
}
