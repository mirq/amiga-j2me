/*
 * amiga-j2me: MIDP 2.0 implementation for the Amiga J2ME runtime.
 * Written for this project; licensed under GPLv2 (see COPYING).
 */
package javax.microedition.media.control;

import javax.microedition.media.Control;

public interface ToneControl extends Control {
    byte VERSION = -2, TEMPO = -3, RESOLUTION = -4, BLOCK_START = -5, BLOCK_END = -6,
         PLAY_BLOCK = -7, SET_VOLUME = -8, REPEAT = -9, C4 = 60, SILENCE = -1;
    void setSequence(byte[] sequence);
}
