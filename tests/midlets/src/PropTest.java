import javax.microedition.lcdui.*;
import javax.microedition.midlet.*;

/** Prints the system properties a game may ask for and the screen size it got. */
public class PropTest extends MIDlet {
    protected void startApp() {
        String[] k = { "microedition.configuration", "microedition.profiles", "microedition.platform", "microedition.locale",
                       "microedition.m3g.version", "game.special", "nothing.here" };
        for (int i = 0; i < k.length; i++) System.out.println(k[i] + " = " + System.getProperty(k[i]));
        Canvas c = new Canvas() { protected void paint(Graphics g) {} };
        System.out.println("canvas " + c.getWidth() + "x" + c.getHeight());
        notifyDestroyed();
    }
    protected void pauseApp() {}
    protected void destroyApp(boolean u) {}
}
