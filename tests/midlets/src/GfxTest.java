import javax.microedition.lcdui.*;
import javax.microedition.midlet.*;

/** Draws one of everything; the host test compares the screenshot. */
public class GfxTest extends MIDlet {
    class C extends Canvas {
        int lastKey;
        protected void paint(Graphics g) {
            int w = getWidth(), h = getHeight();
            g.setColor(0x102040); g.fillRect(0, 0, w, h);
            g.setColor(0xFFFFFF); g.drawString("amiga-j2me GfxTest", w / 2, 4, Graphics.TOP | Graphics.HCENTER);
            g.setColor(0xFF0000); g.fillRect(10, 30, 60, 40);
            g.setColor(0x00FF00); g.drawRect(80, 30, 60, 40);
            g.setColor(0x0080FF); g.fillRoundRect(150, 30, 70, 40, 20, 20);
            g.setColor(0xFFFF00); g.drawRoundRect(150, 30, 70, 40, 20, 20);
            g.setColor(0xFF8000); g.fillArc(10, 80, 60, 60, 30, 300);
            g.setColor(0xFFFFFF); g.drawArc(80, 80, 60, 60, 0, 360);
            g.setColor(0x00FFFF); g.drawArc(85, 85, 50, 50, 45, 180);
            g.setColor(0xFF00FF); g.fillTriangle(150, 140, 185, 80, 220, 140);
            g.setColor(0xC0C0C0);
            for (int i = 0; i < 8; i++) g.drawLine(10, 150, 10 + i * 30, 190);
            g.setStrokeStyle(Graphics.DOTTED); g.drawLine(10, 195, 230, 195); g.setStrokeStyle(Graphics.SOLID);

            Font[] fonts = { Font.getFont(Font.FACE_SYSTEM, Font.STYLE_PLAIN, Font.SIZE_SMALL),
                             Font.getFont(Font.FACE_SYSTEM, Font.STYLE_BOLD, Font.SIZE_MEDIUM),
                             Font.getFont(Font.FACE_SYSTEM, Font.STYLE_PLAIN | Font.STYLE_UNDERLINED, Font.SIZE_LARGE) };
            int y = 200;
            g.setColor(0xFFFFFF);
            for (int i = 0; i < fonts.length; i++) {
                g.setFont(fonts[i]);
                g.drawString("Zażółć gęślą Привет 123", 10, y, Graphics.TOP | Graphics.LEFT);
                y += fonts[i].getHeight() + 2;
            }

            // images: mutable, RGB with alpha, transforms, clip
            Image img = Image.createImage(24, 16);
            Graphics ig = img.getGraphics();
            ig.setColor(0x2060C0); ig.fillRect(0, 0, 24, 16);
            ig.setColor(0xFFFF00); ig.fillTriangle(0, 15, 0, 0, 23, 15);
            ig.setColor(0xFF0000); ig.fillRect(18, 0, 6, 4);
            for (int t = 0; t < 8; t++)
                g.drawRegion(img, 0, 0, 24, 16, t, 10 + t * 28, 255, Graphics.TOP | Graphics.LEFT);
            int[] rgb = new int[32 * 32];
            for (int j = 0; j < 32; j++) for (int i = 0; i < 32; i++)
                rgb[j * 32 + i] = ((i * 8) << 24) | 0x00FF40 | ((j * 8) << 16);
            Image a = Image.createRGBImage(rgb, 32, 32, true);
            g.drawImage(a, 10, 285, Graphics.TOP | Graphics.LEFT);
            g.drawRGB(rgb, 0, 32, 50, 285, 32, 32, true);
            g.drawRGB(rgb, 0, 32, 90, 285, 32, 32, false);
            g.setClip(130, 285, 16, 16);
            g.drawImage(a, 130, 285, 0);
            g.setClip(0, 0, w, h);
            g.setColor(0xFFFFFF); g.setFont(Font.getDefaultFont());
            g.drawString("key " + lastKey, w - 4, h - 2, Graphics.BOTTOM | Graphics.RIGHT);
        }
        protected void keyPressed(int k) { lastKey = k; repaint(); }
    }

    protected void startApp() {
        C c = new C();
        c.setFullScreenMode(true);
        Display.getDisplay(this).setCurrent(c);
    }
    protected void pauseApp() {}
    protected void destroyApp(boolean u) {}
}
