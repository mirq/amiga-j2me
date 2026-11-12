import javax.microedition.lcdui.*;
import javax.microedition.midlet.MIDlet;

/** Prints every key event the canvas gets (tests the key map and diagonals). */
public class KeyEcho extends MIDlet {
    class C extends Canvas {
        protected void paint(Graphics g) { g.setColor(0); g.fillRect(0, 0, getWidth(), getHeight()); }
        protected void keyPressed(int k) { System.out.println("pressed " + k + " " + getKeyName(k)); }
        protected void keyReleased(int k) { System.out.println("released " + k); }
        protected void keyRepeated(int k) { System.out.println("repeated " + k); }
    }
    protected void startApp() { Display.getDisplay(this).setCurrent(new C()); }
    protected void pauseApp() { }
    protected void destroyApp(boolean u) { }
}
