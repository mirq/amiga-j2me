/*
 * amiga-j2me: MIDP 2.0 implementation for the Amiga J2ME runtime.
 * Written for this project; licensed under GPLv2 (see COPYING).
 */
package com.nokia.mid.ui;

public class DeviceControl {
    private DeviceControl() {}
    public static void setLights(int num, int level) {}
    public static void flashLights(long duration) {}
    public static void startVibra(int freq, long duration) {}
    public static void stopVibra() {}
}
