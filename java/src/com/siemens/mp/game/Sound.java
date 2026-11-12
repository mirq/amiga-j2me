/*
 * amiga-j2me: MIDP 2.0 implementation for the Amiga J2ME runtime.
 * Written for this project; licensed under GPLv2 (see COPYING).
 */
package com.siemens.mp.game;

import aj.Native;
import aj.media.Smf;

public class Sound extends com.siemens.mp.misc.NativeMem {
    private static int handle;

    /** time is in units of 100 ms */
    public static synchronized void playTone(int freq, int time) {
        if (handle != 0) { Native.sndStop(handle); Native.sndClose(handle); handle = 0; }
        if (freq <= 0 || time <= 0) return;
        byte[] d = Smf.tone(Smf.noteOfFrequency(freq), time * 100, 100);
        handle = Native.sndCreate(d, 0, d.length);
        if (handle != 0) Native.sndPlay(handle, 1, 100);
    }
}
