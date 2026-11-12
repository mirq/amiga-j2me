/*
 * amiga-j2me: MIDP 2.0 implementation for the Amiga J2ME runtime.
 * Written for this project; licensed under GPLv2 (see COPYING).
 */
package com.siemens.mp.game;

import aj.Native;

/** A tune built by MelodyComposer; one plays at a time. */
public class Melody extends com.siemens.mp.misc.NativeMem {
    private static int playing;
    private final byte[] midi;

    Melody(byte[] midi) { this.midi = midi; }

    public void play() {
        synchronized (Melody.class) {
            stop();
            if (midi == null) return;
            playing = Native.sndCreate(midi, 0, midi.length);
            if (playing != 0) Native.sndPlay(playing, 1, 100);
        }
    }

    public static void stop() {
        synchronized (Melody.class) {
            if (playing != 0) { Native.sndStop(playing); Native.sndClose(playing); playing = 0; }
        }
    }
}
