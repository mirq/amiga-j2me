/*
 * amiga-j2me: MIDP 2.0 implementation for the Amiga J2ME runtime.
 * Written for this project; licensed under GPLv2 (see COPYING).
 */
package javax.microedition.rms;

public class RecordStoreFullException extends RecordStoreException {
    public RecordStoreFullException() { super(); }
    public RecordStoreFullException(String message) { super(message); }
}
