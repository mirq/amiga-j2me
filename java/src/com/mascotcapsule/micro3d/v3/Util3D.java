/*
 * amiga-j2me: Mascot Capsule micro3d v3 API for the Amiga J2ME runtime.
 * Written for this project; licensed under GPLv2 (see COPYING).
 */
package com.mascotcapsule.micro3d.v3;

public class Util3D {
    private Util3D() {}

    /** Square root of p taken as an unsigned number. */
    public static int sqrt(int p) {
        if (p == 0) return 0;
        double a = p < 0 ? p + 4294967296.0 : p;
        return (int) (Math.sqrt(a) + 0.5);
    }

    /** 4096 units make a full turn; the result is scaled by 4096. */
    public static int sin(int p) {
        double v = Math.sin(p * (Math.PI / 2048)) * 4096;
        return (int) (v < 0 ? v - 0.5 : v + 0.5);
    }

    public static int cos(int p) { return sin(p + 1024); }
}
