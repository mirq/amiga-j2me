/*
 * amiga-j2me: MIDP 2.0 implementation for the Amiga J2ME runtime.
 * Written for this project; licensed under GPLv2 (see COPYING).
 */
package com.samsung.util;

import aj.Native;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;

/** Samsung sound clips: MIDI and WAV play; MMF and MP3 clips load but stay silent. */
public class AudioClip {
    public static final int TYPE_MMF = 1, TYPE_MP3 = 2, TYPE_MIDI = 3;

    private int handle;

    public AudioClip(int type, String filename) throws IOException {
        if (filename == null) throw new NullPointerException();
        if (!filename.startsWith("/")) filename = "/" + filename;
        InputStream in = AudioClip.class.getResourceAsStream(filename);
        if (in == null) throw new IOException(filename + " not found");
        ByteArrayOutputStream out = new ByteArrayOutputStream(4096);
        byte[] buf = new byte[1024];
        int n;
        while ((n = in.read(buf)) > 0) out.write(buf, 0, n);
        in.close();
        byte[] d = out.toByteArray();
        handle = Native.sndCreate(d, 0, d.length);
    }

    public AudioClip(int type, byte[] data, int offset, int length) {
        if (data == null) throw new NullPointerException();
        handle = Native.sndCreate(data, offset, length);
    }

    public static boolean isSupported() { return true; }

    /** volume 0..5 */
    public void play(int loop, int volume) {
        if (loop < 0 || loop > 255 || volume < 0 || volume > 5) throw new IllegalArgumentException();
        if (handle == 0) return;
        Native.sndStop(handle);
        Native.sndRewind(handle);
        Native.sndPlay(handle, loop == 255 ? -1 : (loop == 0 ? 1 : loop), volume * 20);
    }

    public void stop() { if (handle != 0) Native.sndStop(handle); }
    public void pause() { if (handle != 0) Native.sndStop(handle); }
    public void resume() { if (handle != 0) Native.sndPlay(handle, 1, 100); }
}
