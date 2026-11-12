/*
 * amiga-j2me: MIDP 2.0 implementation for the Amiga J2ME runtime.
 * Written for this project; licensed under GPLv2 (see COPYING).
 */
package com.motorola.midi;

import aj.Native;
import java.io.ByteArrayOutputStream;
import java.io.InputStream;

/** Plays MIDI files from the game's JAR. */
public class MidiPlayer {
    private static int handle;

    private static synchronized void drop() {
        if (handle != 0) { Native.sndStop(handle); Native.sndClose(handle); handle = 0; }
    }

    public static synchronized void play(String file, boolean repeat) {
        drop();
        try {
            InputStream in = MidiPlayer.class.getResourceAsStream(file.startsWith("/") ? file : "/" + file);
            if (in == null) return;
            ByteArrayOutputStream out = new ByteArrayOutputStream(4096);
            byte[] buf = new byte[1024];
            int n;
            while ((n = in.read(buf)) > 0) out.write(buf, 0, n);
            in.close();
            byte[] d = out.toByteArray();
            handle = Native.sndCreate(d, 0, d.length);
            if (handle != 0) Native.sndPlay(handle, repeat ? -1 : 1, 100);
        } catch (java.io.IOException e) {
            // stays silent
        }
    }

    public static void stop() { drop(); }
    public static void playEffect(int note, int key, int velocity) {}
}
