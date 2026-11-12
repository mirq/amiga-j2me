/*
 * amiga-j2me: MIDP 2.0 implementation for the Amiga J2ME runtime.
 * Written for this project; licensed under GPLv2 (see COPYING).
 */
package com.motorola.multimedia;

public class FunLight {
    public static final int BLACK = 0, BLANK = 0, BLUE = 0x0000FF, CYAN = 0x00FFFF, GREEN = 0x00FF00,
        MAGENTA = 0xFF00FF, OFF = 0, ON = 0xFFFFFF, RED = 0xFF0000, WHITE = 0xFFFFFF, YELLOW = 0xFFFF00,
        IGNORED = 2, QUEUED = 1, SUCCESS = 0;
    protected FunLight() {}
    public static int getControl() { return IGNORED; }
    public static void releaseControl() {}
    public static int[] getRegionsIDs() { return new int[0]; }
    public static int setRegion(int region, int color) { return IGNORED; }
    public static int setRegion(int region, byte red, byte green, byte blue) { return IGNORED; }
}
