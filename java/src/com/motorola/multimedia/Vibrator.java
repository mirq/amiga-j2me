/*
 * amiga-j2me: MIDP 2.0 implementation for the Amiga J2ME runtime.
 * Written for this project; licensed under GPLv2 (see COPYING).
 */
package com.motorola.multimedia;

public class Vibrator {
    public static final int VIBRATE_SHORT = 0, VIBRATE_LONG = 1, VIBRATE_2SHORT = 2, VIBRATE_SHORT_LONG = 3,
        VIBRATE_PULSE = 4, VIBRATE_SILENT = 5, MAX_VIBRATE_TIME = 300000, MIN_PAUSE_TIME = 1000;
    protected Vibrator() {}
    public static void setVibrateTone(int tone) {}
    public static void vibrateFor(int timeInMs) {}
    public static void vibratePeriodicaly(int timeInMs) {}
    public static void vibratePeriodicaly(int timeOnInMs, int timeOffInMs) {}
    public static void vibratorOn() {}
    public static void vibratorOff() {}
}
