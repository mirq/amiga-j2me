/*
 * amiga-j2me: MIDP 2.0 implementation for the Amiga J2ME runtime.
 * Written for this project; licensed under GPLv2 (see COPYING).
 */
package javax.microedition.media.control;

import javax.microedition.media.Control;

public interface VolumeControl extends Control {
    void setMute(boolean mute);
    boolean isMuted();
    int setLevel(int level);
    int getLevel();
}
