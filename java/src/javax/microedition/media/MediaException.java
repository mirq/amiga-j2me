/*
 * amiga-j2me: MIDP 2.0 implementation for the Amiga J2ME runtime.
 * Written for this project; licensed under GPLv2 (see COPYING).
 */
package javax.microedition.media;

public class MediaException extends Exception {
    public MediaException() { super(); }
    public MediaException(String reason) { super(reason); }
}
