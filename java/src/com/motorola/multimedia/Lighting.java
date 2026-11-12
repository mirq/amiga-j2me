/*
 * amiga-j2me: MIDP 2.0 implementation for the Amiga J2ME runtime.
 * Written for this project; licensed under GPLv2 (see COPYING).
 */
package com.motorola.multimedia;

public class Lighting {
    public static final int LIGHT_STATE_OFF = 0, LIGHT_STATE_ON = 1, LIGHT_STATE_TOGGLE = 2;
    protected Lighting() {}
    public static void backlightOn() {}
    public static void backlightOff() {}
    public static void fxOn(int id) {}
    public static void fxOff(int id) {}
    public static int getNumOfFunLights() { return 0; }
    public static void lightPattern(int id, int state) {}
}
