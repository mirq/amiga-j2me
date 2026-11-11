/*
 * amiga-j2me: MIDP 2.0 implementation for the Amiga J2ME runtime.
 * Written for this project; licensed under GPLv2 (see COPYING).
 */
package javax.microedition.rms;

public interface RecordComparator {
    int EQUIVALENT = 0, FOLLOWS = 1, PRECEDES = -1;
    int compare(byte[] rec1, byte[] rec2);
}
