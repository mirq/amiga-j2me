/*
 * amiga-j2me: Motorola iDEN micro3d API for the Amiga J2ME runtime, a layer
 * over the Mascot Capsule v3 implementation.
 * Written for this project; licensed under GPLv2 (see COPYING).
 */
package com.motorola.iden.micro3d;

import com.mascotcapsule.micro3d.v3.Util3D;

public class Utility {
    private Utility() {}
    public static int cos(int angle) { return Util3D.cos(angle); }
    public static int sin(int angle) { return Util3D.sin(angle); }
    public static int squareRoot(int x) {
        if (x < 0) throw new IllegalArgumentException();
        return Util3D.sqrt(x);
    }
    public static String getVersion() { return "3.0"; }
}
