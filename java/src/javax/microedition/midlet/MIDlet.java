/*
 * amiga-j2me: MIDP 2.0 implementation for the Amiga J2ME runtime.
 * Written for this project; licensed under GPLv2 (see COPYING).
 */
package javax.microedition.midlet;

import aj.Launcher;
import javax.microedition.io.ConnectionNotFoundException;

public abstract class MIDlet {
    protected MIDlet() {}

    protected abstract void startApp() throws MIDletStateChangeException;
    protected abstract void pauseApp();
    protected abstract void destroyApp(boolean unconditional) throws MIDletStateChangeException;

    public final void notifyDestroyed() { Launcher.midletDestroyed(); }
    public final void notifyPaused() {}
    public final void resumeRequest() {}
    public final String getAppProperty(String key) { return Launcher.getAppProperty(key); }
    public final boolean platformRequest(String URL) throws ConnectionNotFoundException { return false; }
    public final int checkPermission(String permission) { return 0; }

    // The launcher is in another package: these give it the lifecycle calls.
    public static void start(MIDlet m) throws MIDletStateChangeException { m.startApp(); }
    public static void destroy(MIDlet m) {
        try {
            m.destroyApp(true);
        } catch (Throwable t) {
            // leaving anyway
        }
    }
}
