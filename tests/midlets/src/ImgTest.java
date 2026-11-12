import javax.microedition.lcdui.*;
import javax.microedition.midlet.*;

/** Decodes image files from resources and draws them. */
public class ImgTest extends MIDlet {
    protected void startApp() {
        final String[] names = { "/pal.png", "/rgba.png", "/photo.jpg" };
        final Image[] imgs = new Image[names.length];
        for (int i = 0; i < names.length; i++) {
            try {
                System.out.println("decoding " + names[i]);
                imgs[i] = Image.createImage(names[i]);
                System.out.println("  " + imgs[i].getWidth() + "x" + imgs[i].getHeight());
            } catch (Throwable t) {
                System.out.println("  failed: " + t);
            }
        }
        Canvas c = new Canvas() {
            protected void paint(Graphics g) {
                g.setColor(0x808080); g.fillRect(0, 0, getWidth(), getHeight());
                int y = 4;
                for (int i = 0; i < imgs.length; i++) {
                    if (imgs[i] == null) continue;
                    g.drawImage(imgs[i], 4, y, Graphics.TOP | Graphics.LEFT);
                    g.drawImage(imgs[i], 70, y, Graphics.TOP | Graphics.LEFT);
                    y += imgs[i].getHeight() + 4;
                }
            }
        };
        Display.getDisplay(this).setCurrent(c);
    }
    protected void pauseApp() {}
    protected void destroyApp(boolean u) {}
}
