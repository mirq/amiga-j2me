import javax.microedition.lcdui.*;
import javax.microedition.media.*;
import javax.microedition.media.control.*;
import javax.microedition.midlet.*;
import com.nokia.mid.sound.Sound;

/** Plays one of each kind of sound, one second apart. */
public class SoundTest extends MIDlet implements Runnable, PlayerListener {
    String status = "starting";
    Canvas c;

    public void playerUpdate(Player p, String event, Object data) {
        System.out.println("  event " + event);
    }

    void say(String s) { status = s; System.out.println(s); c.repaint(); }

    static void sleep(int ms) { try { Thread.sleep(ms); } catch (InterruptedException e) { } }

    public void run() {
        try {
            say("1 wav");
            Player w = Manager.createPlayer(getClass().getResourceAsStream("/beep.wav"), "audio/x-wav");
            w.addPlayerListener(this);
            w.start();
            sleep(1000);
            say("2 midi, duration " + 0);
            Player m = Manager.createPlayer(getClass().getResourceAsStream("/tune.mid"), "audio/midi");
            m.addPlayerListener(this);
            m.prefetch();
            System.out.println("  duration ms " + m.getDuration() / 1000);
            m.start();
            sleep(2500);
            say("3 tone sequence");
            Player t = Manager.createPlayer(Manager.TONE_DEVICE_LOCATOR);
            t.realize();
            ToneControl tc = (ToneControl) t.getControl("ToneControl");
            tc.setSequence(new byte[] { ToneControl.VERSION, 1, ToneControl.TEMPO, 40, ToneControl.BLOCK_START, 0,
                60, 8, 64, 8, 67, 8, ToneControl.BLOCK_END, 0, ToneControl.PLAY_BLOCK, 0, ToneControl.SILENCE, 8,
                ToneControl.PLAY_BLOCK, 0, 72, 16 });
            t.addPlayerListener(this);
            t.start();
            sleep(2500);
            say("4 playTone");
            Manager.playTone(76, 300, 100);
            sleep(800);
            say("5 nokia tone");
            Sound s = new Sound(880, 300L);
            s.play(1);
            sleep(800);
            System.out.println("  nokia state " + s.getState());
            say("done");
        } catch (Throwable e) {
            say("failed: " + e);
            e.printStackTrace();
        }
    }

    protected void startApp() {
        c = new Canvas() {
            protected void paint(Graphics g) {
                g.setColor(0x203020); g.fillRect(0, 0, getWidth(), getHeight());
                g.setColor(0xFFFFFF); g.drawString(status, 4, 4, 0);
            }
        };
        Display.getDisplay(this).setCurrent(c);
        new Thread(this).start();
    }
    protected void pauseApp() {}
    protected void destroyApp(boolean u) {}
}
