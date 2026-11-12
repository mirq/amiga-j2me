/*
 * amiga-j2me: MIDP 2.0 implementation for the Amiga J2ME runtime.
 * Written for this project; licensed under GPLv2 (see COPYING).
 */
package com.motorola.funlight;

public interface Region {
    int getColor();
    int getControl();
    int getID();
    void releaseControl();
    int setColor(int color);
    int setColor(byte red, byte green, byte blue);
}
