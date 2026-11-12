/*
 * amiga-j2me: JSR-184 (M3G 1.0/1.1) implementation for the Amiga J2ME runtime.
 * Written for this project; licensed under GPLv2 (see COPYING).
 */
package javax.microedition.m3g;

/** What CLDC's Math lacks. Not M3G API. */
final class M3GMath {
    private M3GMath() {}

    /** arccos for x in -1..1 (polynomial, error below 1e-4 rad). */
    static double acos(double x) {
        boolean neg = x < 0;
        if (neg) x = -x;
        if (x > 1) x = 1;
        double r = ((-0.0187293 * x + 0.0742610) * x - 0.2121144) * x + 1.5707288;
        r *= Math.sqrt(1.0 - x);
        return neg ? Math.PI - r : r;
    }

    static float clamp(float v, float lo, float hi) { return v < lo ? lo : (v > hi ? hi : v); }
}
