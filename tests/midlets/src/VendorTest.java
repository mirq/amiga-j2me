import com.siemens.mp.game.ExtendedImage;
import com.siemens.mp.game.GraphicObjectManager;
import com.siemens.mp.game.MelodyComposer;
import com.siemens.mp.game.Sprite;
import com.siemens.mp.game.TiledBackground;
import com.siemens.mp.io.File;
import javax.microedition.lcdui.*;
import javax.microedition.midlet.*;

/** Siemens game API (sprites, tiles, files, melody), Samsung and Motorola classes. */
public class VendorTest extends MIDlet {
    protected void startApp() {
        // 1-bit sprite, two 8x8 frames: a box and a cross
        byte[] px = { (byte) 0xFF, (byte) 0x81, (byte) 0x81, (byte) 0x81, (byte) 0x81, (byte) 0x81, (byte) 0x81, (byte) 0xFF,
                      (byte) 0x81, 0x42, 0x24, 0x18, 0x18, 0x24, 0x42, (byte) 0x81 };
        byte[] mask = new byte[16];
        for (int i = 0; i < 16; i++) mask[i] = (byte) (i < 8 ? 0xFF : px[i]);
        final Sprite a = new Sprite(px, 0, 8, 8, mask, 0, 2), b = new Sprite(px, 0, 8, 8, mask, 0, 2);
        a.setPosition(20, 20);
        b.setFrame(1); b.setPosition(25, 24);
        System.out.println("collide " + a.isCollidingWith(b) + " pos " + a.isCollidingWithPos(21, 21) + "/" + a.isCollidingWithPos(40, 40));

        // tiles from a colour image: red and green, on a 6x4 map with white/black/empty cells
        Image tiles = Image.createImage(8, 16);
        Graphics tg = tiles.getGraphics();
        tg.setColor(0xD03030); tg.fillRect(0, 0, 8, 8);
        tg.setColor(0x30B040); tg.fillRect(0, 8, 8, 8);
        tg.setColor(0xFFFF00); tg.drawLine(0, 8, 7, 15);
        byte[] map = { 3, 4, 3, 4, 3, 4,  4, 0, 0, 0, 0, 3,  3, 1, 2, 1, 2, 4,  4, 3, 4, 3, 4, 3 };
        final TiledBackground bg = new TiledBackground(tiles, null, map, 6, 4);
        final GraphicObjectManager gom = new GraphicObjectManager();
        gom.addObject(bg); gom.addObject(a); gom.addObject(b);
        System.out.println("objects " + gom.getObjectPosition(b) + " " + (gom.getObjectAt(0) == bg));

        final ExtendedImage ei = new ExtendedImage(Image.createImage(48, 40));
        ei.clear((byte) 0);
        gom.paint(ei, 0, 0);
        ei.setPixel(47, 39, (byte) 1);
        byte[] row = new byte[1];
        ei.getPixelBytes(row, 16, 20, 8, 1);        // the top edge of sprite a, from x = 16: its 4 right pixels are at 20..23
        System.out.println("pixels " + ei.getPixel(47, 39) + ei.getPixel(46, 39) + " row " + Integer.toHexString(row[0] & 0xFF));

        // files
        try {
            File f = new File();
            File.delete("save.dat");
            System.out.println("exists before " + File.exists("save.dat"));
            int fd = f.open("save.dat");
            f.write(fd, "hello siemens".getBytes(), 0, 13);
            f.close(fd);
            fd = f.open("A:\\save.dat");
            byte[] r = new byte[32];
            f.seek(fd, 6);
            int n = f.read(fd, r, 0, 32);
            System.out.println("file len " + f.length(fd) + " read [" + new String(r, 0, n) + "] exists " + File.exists("save.dat"));
            f.close(fd);
            File.rename("save.dat", "other.dat");
            System.out.println("renamed " + File.exists("save.dat") + " " + File.exists("other.dat"));
            File.delete("other.dat");
        } catch (Throwable t) {
            System.out.println("file failed: " + t);
        }

        // melody: C E G C
        MelodyComposer mc = new MelodyComposer();
        mc.setBPM(120);
        mc.appendNote(MelodyComposer.TONE_C1, MelodyComposer.TONELENGTH_1_8);
        mc.appendNote(MelodyComposer.TONE_E1, MelodyComposer.TONELENGTH_1_8);
        mc.appendNote(MelodyComposer.TONE_G1, MelodyComposer.TONELENGTH_1_8);
        mc.appendNote(MelodyComposer.TONE_C2, MelodyComposer.TONELENGTH_DOTTED_1_4);
        System.out.println("melody notes " + mc.length() + " A1=" + MelodyComposer.TONE_A1);
        mc.getMelody().play();

        try {
            new com.samsung.util.AudioClip(com.samsung.util.AudioClip.TYPE_MIDI, "tune.mid");
            System.out.println("samsung clip ok, vib " + com.samsung.util.Vibration.isSupported());
        } catch (Throwable t) {
            System.out.println("samsung failed: " + t);
        }
        com.motorola.multimedia.Vibrator.vibrateFor(100);
        System.out.println("motorola regions " + com.motorola.funlight.FunLight.getRegions().length);

        Canvas c = new Canvas() {
            protected void paint(Graphics g) {
                g.setColor(0x6080A0); g.fillRect(0, 0, getWidth(), getHeight());
                g.setColor(0xFFFFFF); g.drawString("siemens game api", 4, 2, Graphics.TOP | Graphics.LEFT);
                gom.paint(g, 8, 20);                                  // straight to the screen
                g.drawImage(ei.getImage(), 70, 20, Graphics.TOP | Graphics.LEFT);     // through an ExtendedImage
                // scaled up 3x so it can be seen
                int[] rgb = new int[48 * 40];
                ei.getImage().getRGB(rgb, 0, 48, 0, 0, 48, 40);
                for (int y = 0; y < 40; y++) for (int x = 0; x < 48; x++) {
                    g.setColor(rgb[y * 48 + x]); g.fillRect(8 + x * 3, 70 + y * 3, 3, 3);
                }
            }
        };
        Display.getDisplay(this).setCurrent(c);
    }
    protected void pauseApp() {}
    protected void destroyApp(boolean u) {}
}
