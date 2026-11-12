import java.util.*;
import javax.microedition.lcdui.*;
import javax.microedition.midlet.*;
import javax.microedition.rms.RecordStore;
import com.nokia.mid.ui.*;
import com.nokia.mid.sound.Sound;

/** Calls the APIs games use at start-up, printing a marker before each. */
public class ApiProbe extends MIDlet {
    static void p(String s) { System.out.println(s); }

    class FC extends FullCanvas {
        protected void paint(Graphics g) {
            g.setColor(0x004080); g.fillRect(0, 0, getWidth(), getHeight());
            DirectGraphics dg = DirectUtils.getDirectGraphics(g);
            dg.fillPolygon(new int[] { 5, 60, 30 }, 0, new int[] { 5, 10, 50 }, 0, 3, 0xFFFFFF00);
            g.setColor(0xFFFFFF); g.drawString("probe ok", 2, 2, 0);
        }
    }

    public ApiProbe() {
        p("1 properties");
        String[] props = { "microedition.platform", "microedition.locale", "microedition.encoding",
                           "microedition.configuration", "microedition.profiles", "com.nokia.mid.imei", "nothing" };
        for (int i = 0; i < props.length; i++) p("  " + props[i] + "=" + System.getProperty(props[i]));
        p("2 memory");
        Runtime r = Runtime.getRuntime();
        p("  free " + r.freeMemory() + " total " + r.totalMemory());
        System.gc();
        p("3 time");
        p("  millis " + System.currentTimeMillis());
        Calendar c = Calendar.getInstance();
        p("  year " + c.get(Calendar.YEAR) + " hour " + c.get(Calendar.HOUR_OF_DAY));
        p("  tz " + TimeZone.getDefault().getID() + " date " + new Date().toString());
        p("4 random");
        Random rnd = new Random();
        p("  " + (rnd.nextInt() != rnd.nextInt()));
        p("5 app properties");
        p("  MIDlet-Name=" + getAppProperty("MIDlet-Name") + " x=" + getAppProperty("x"));
        p("6 display");
        Display d = Display.getDisplay(this);
        p("  color " + d.isColor() + " " + d.numColors());
        p("7 fullcanvas");
        FC fc = new FC();
        p("  " + fc.getWidth() + "x" + fc.getHeight());
        p("8 fonts");
        Font f = Font.getFont(Font.FACE_PROPORTIONAL, Font.STYLE_BOLD, Font.SIZE_SMALL);
        p("  h " + f.getHeight() + " w " + f.stringWidth("Hello"));
        p("9 images");
        Image im = Image.createImage(20, 20);
        im.getGraphics().fillRect(0, 0, 5, 5);
        try { Image.createImage("/missing.png"); } catch (Exception e) { p("  missing: " + e); }
        p("10 rms");
        try {
            RecordStore rs = RecordStore.openRecordStore("probe", true);
            p("  records " + rs.getNumRecords());
            rs.closeRecordStore();
        } catch (Exception e) { p("  " + e); }
        p("11 sound");
        try {
            Sound s = new Sound(440, 100L);
            s.play(1);
            p("  state " + s.getState());
            DeviceControl.setLights(0, 100);
        } catch (Throwable t) { p("  " + t); }
        p("12 timer+thread");
        new Timer().schedule(new TimerTask() { public void run() { p("  timer fired"); } }, 100);
        new Thread() { public void run() { p("  thread ran"); } }.start();
        p("13 class lookup");
        try { Class.forName("com.nokia.mid.ui.DeviceControl"); p("  found"); } catch (Throwable t) { p("  " + t); }
        try { Class.forName("com.siemens.mp.game.Light"); p("  found?"); } catch (Throwable t) { p("  " + t); }
        p("14 setCurrent");
        d.setCurrent(fc);
        p("constructor done");
    }

    protected void startApp() { p("startApp"); }
    protected void pauseApp() {}
    protected void destroyApp(boolean u) {}
}
