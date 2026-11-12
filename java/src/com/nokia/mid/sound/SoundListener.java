/*
 * amiga-j2me: MIDP 2.0 implementation for the Amiga J2ME runtime.
 * Written for this project; licensed under GPLv2 (see COPYING).
 */
package com.nokia.mid.sound;

public interface SoundListener {
    void soundStateChanged(Sound sound, int event);
}
