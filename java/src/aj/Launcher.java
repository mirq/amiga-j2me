/*
 * amiga-j2me: MIDP 2.0 implementation for the Amiga J2ME runtime.
 * Written for this project; licensed under GPLv2 (see COPYING).
 */
package aj;

import java.io.InputStream;
import java.util.Hashtable;
import javax.microedition.lcdui.Display;
import javax.microedition.midlet.MIDlet;

/**
 * Starts a MIDlet and runs the event thread.
 *
 *   aj.Launcher [-v] [-w WIDTH] [-h HEIGHT] [-scale N] [-jad FILE] [-rms DIR] [MIDletClass]
 *
 * Without a class name, the first MIDlet of the JAR's manifest is started
 * (MIDlet-1: name, icon, class).
 */
public final class Launcher {
    private static final Hashtable props = new Hashtable();
    private static volatile boolean destroyed;
    private static String storage = "rms";
    private static boolean verbose;

    private static void log(String s) { if (verbose) System.out.println("[launcher] " + s); }

    /** Directory of the record store files (-rms DIR). */
    public static String storageDir() { return storage; }

    private Launcher() {}

    public static String getAppProperty(String key) {
        if (key == null) throw new NullPointerException();
        return (String) props.get(key);
    }

    public static void midletDestroyed() { destroyed = true; }

    /** "Key: value" lines, with continuation lines that start with a space. */
    private static void readManifest(InputStream in) throws java.io.IOException {
        StringBuffer line = new StringBuffer();
        int c;
        String key = null;
        do {
            c = in.read();
            if (c == '\r') continue;
            if (c == '\n' || c < 0) {
                String s = line.toString();
                line.setLength(0);
                if (s.length() > 0 && s.charAt(0) == ' ' && key != null) {
                    props.put(key, (String) props.get(key) + s.substring(1));
                } else {
                    int colon = s.indexOf(':');
                    if (colon > 0) {
                        key = s.substring(0, colon).trim();
                        props.put(key, s.substring(colon + 1).trim());
                    }
                }
            } else {
                line.append((char) c);     // manifests are UTF-8; ASCII keys are what matter here
            }
        } while (c >= 0);
    }

    public static void main(String[] args) {
        int w = 240, h = 320, scale = 1;
        String jad = null;
        String className = null;
        for (int i = 0; i < args.length; i++) {
            if (args[i].equals("-w") && i + 1 < args.length) w = Integer.parseInt(args[++i]);
            else if (args[i].equals("-h") && i + 1 < args.length) h = Integer.parseInt(args[++i]);
            else if (args[i].equals("-rms") && i + 1 < args.length) storage = args[++i];
            else if (args[i].equals("-v")) verbose = true;
            else if (args[i].equals("-scale") && i + 1 < args.length) scale = Integer.parseInt(args[++i]);
            else if (args[i].equals("-jad") && i + 1 < args.length) jad = args[++i];
            else className = args[i];
        }

        log("reading the manifest");
        try {
            InputStream in = Launcher.class.getResourceAsStream("/META-INF/MANIFEST.MF");
            if (in != null) {
                readManifest(in);
                in.close();
            }
        } catch (Throwable t) {
            System.out.println("manifest: " + t);
        }
        if (jad != null) {      // the descriptor's attributes come on top of the manifest's
            byte[] d = Native.readFile(jad);
            if (d == null) System.out.println("cannot read " + jad);
            else {
                try {
                    readManifest(new java.io.ByteArrayInputStream(d));
                } catch (Throwable t) {
                    System.out.println("jad: " + t);
                }
            }
        }
        if (className == null) {
            String m1 = (String) props.get("MIDlet-1");
            if (m1 != null) className = m1.substring(m1.lastIndexOf(',') + 1).trim();
        }
        if (className == null) {
            System.out.println("No MIDlet class: give one, or a JAR with MIDlet-1 in its manifest");
            return;
        }

        log("MIDlet class " + className + ", display " + w + "x" + h);
        Display.open(w, h, scale);
        MIDlet midlet = null;
        try {
            Class c = Class.forName(className);
            log("class loaded");
            midlet = (MIDlet) c.newInstance();
            log("MIDlet created");
            MIDlet.start(midlet);
            log("startApp returned");
            Display d = Display.getDisplay(midlet);
            while (!destroyed && !d.quitRequested()) {
                if (!d.runOnce()) {
                    try {
                        Thread.sleep(5);
                    } catch (InterruptedException e) {
                        // keep looping
                    }
                } else {
                    Thread.yield();
                }
            }
        } catch (Throwable t) {
            System.out.println("MIDlet failed: " + t);
            t.printStackTrace();
        }
        if (midlet != null && !destroyed) MIDlet.destroy(midlet);
        javax.microedition.rms.RecordStore.flushAll();
        Display.close();
        System.exit(0);     // the MIDlet's own threads may still be running
    }
}
