/*
 * amiga-j2me: MIDP 2.0 implementation for the Amiga J2ME runtime.
 * Written for this project; licensed under GPLv2 (see COPYING).
 */
package com.samsung.util;

public class SMS {
    public static boolean isSupported() { return false; }
    public static void send(SM sm) { throw new IllegalStateException("no SMS on this device"); }
}
