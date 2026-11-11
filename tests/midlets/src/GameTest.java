import java.util.Timer;
import java.util.TimerTask;
import javax.microedition.lcdui.*;
import javax.microedition.lcdui.game.*;
import javax.microedition.midlet.*;
import javax.microedition.rms.RecordStore;

/** GameCanvas loop with a tiled background, sprites, collisions, RMS and a Timer. */
public class GameTest extends MIDlet implements Runnable {
    class G extends GameCanvas {
        G() { super(true); }
        Graphics g() { return getGraphics(); }
    }

    G canvas;
    volatile boolean running = true;
    volatile int timerTicks;
    int runs;

    static Image tiles() {
        Image img = Image.createImage(64, 16);      // 4 tiles of 16x16
        Graphics g = img.getGraphics();
        int[] c = { 0x206020, 0x308030, 0x705020, 0x3050A0 };
        for (int i = 0; i < 4; i++) {
            g.setColor(c[i]); g.fillRect(i * 16, 0, 16, 16);
            g.setColor(0x000000); g.drawRect(i * 16, 0, 15, 15);
        }
        return img;
    }

    static Image ship() {
        // 2 frames of 16x16 with transparent corners
        int[] rgb = new int[32 * 16];
        for (int f = 0; f < 2; f++)
            for (int y = 0; y < 16; y++)
                for (int x = 0; x < 16; x++) {
                    int dx = x - 8, dy = y - 8;
                    boolean in = dx * dx + dy * dy < 60 && !(x > 10 && y < 6);
                    rgb[y * 32 + f * 16 + x] = in ? (f == 0 ? 0xFFFFD000 : 0xFFFF4000) : 0;
                }
        return Image.createRGBImage(rgb, 32, 16, true);
    }

    protected void startApp() {
        try {
            RecordStore rs = RecordStore.openRecordStore("gametest", true);
            if (rs.getNumRecords() == 0) rs.addRecord(new byte[] { 1 }, 0, 1);
            else {
                byte[] b = rs.getRecord(1);
                runs = b[0];
                b[0]++;
                rs.setRecord(1, b, 0, 1);
            }
            rs.closeRecordStore();
            String[] names = RecordStore.listRecordStores();
            System.out.println("RMS: run " + (runs + 1) + ", stores " + (names == null ? 0 : names.length));
        } catch (Exception e) {
            System.out.println("RMS failed: " + e);
        }
        new Timer().schedule(new TimerTask() { public void run() { timerTicks++; } }, 50, 50);
        canvas = new G();
        canvas.setFullScreenMode(true);
        Display.getDisplay(this).setCurrent(canvas);
        new Thread(this).start();
    }

    public void run() {
        int w = canvas.getWidth(), h = canvas.getHeight();
        TiledLayer bg = new TiledLayer(w / 16 + 1, h / 16 + 1, tiles(), 16, 16);
        for (int r = 0; r < bg.getRows(); r++)
            for (int c = 0; c < bg.getColumns(); c++) bg.setCell(c, r, 1 + ((c * 7 + r * 3) % 4));
        int anim = bg.createAnimatedTile(1);
        bg.fillCells(2, 2, 3, 1, anim);
        Image shipImg = ship();
        Sprite a = new Sprite(shipImg, 16, 16), b = new Sprite(shipImg, 16, 16);
        a.defineReferencePixel(8, 8); b.defineReferencePixel(8, 8);
        a.setRefPixelPosition(40, 100); b.setRefPixelPosition(w - 40, 100);
        LayerManager lm = new LayerManager();
        lm.append(a); lm.append(b); lm.append(bg);
        Graphics g = canvas.g();
        Font f = Font.getFont(Font.FACE_SYSTEM, Font.STYLE_BOLD, Font.SIZE_SMALL);
        int frame = 0, hits = 0, fps = 0, fpsFrames = 0;
        long t0 = System.currentTimeMillis(), fpsT = t0;
        int ax = 2, trans = 0;
        while (running) {
            int keys = canvas.getKeyStates();
            if ((keys & GameCanvas.LEFT_PRESSED) != 0) a.move(-3, 0);
            if ((keys & GameCanvas.RIGHT_PRESSED) != 0) a.move(3, 0);
            if ((keys & GameCanvas.UP_PRESSED) != 0) a.move(0, -3);
            if ((keys & GameCanvas.DOWN_PRESSED) != 0) a.move(0, 3);
            if ((keys & GameCanvas.FIRE_PRESSED) != 0) { trans = (trans + 1) & 7; a.setTransform(trans); }
            a.move(ax, 0);
            if (a.getX() > w - 16 || a.getX() < 0) ax = -ax;
            b.move(-1, 0);
            if (b.getX() < -16) b.setPosition(w, 100);
            if ((frame & 7) == 0) { a.nextFrame(); bg.setAnimatedTile(anim, 1 + ((frame >> 3) & 3)); }
            boolean hit = a.collidesWith(b, true);
            if (hit) hits++;
            lm.paint(g, 0, 0);
            g.setFont(f);
            g.setColor(hit ? 0xFF4040 : 0xFFFFFF);
            g.drawString("frame " + frame + " fps " + fps + " hits " + hits, 2, 2, Graphics.TOP | Graphics.LEFT);
            g.drawString("timer " + timerTicks + " runs " + runs + " t" + trans, 2, h - 2, Graphics.BOTTOM | Graphics.LEFT);
            canvas.flushGraphics();
            frame++; fpsFrames++;
            long now = System.currentTimeMillis();
            if (now - fpsT >= 1000) { fps = (int) (fpsFrames * 1000 / (now - fpsT)); fpsFrames = 0; fpsT = now; System.out.println("fps " + fps); }
            try { Thread.sleep(10); } catch (InterruptedException e) { }
        }
    }

    protected void pauseApp() {}
    protected void destroyApp(boolean u) { running = false; }
}
