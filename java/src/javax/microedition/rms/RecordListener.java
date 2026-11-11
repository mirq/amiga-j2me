/*
 * amiga-j2me: MIDP 2.0 implementation for the Amiga J2ME runtime.
 * Written for this project; licensed under GPLv2 (see COPYING).
 */
package javax.microedition.rms;

public interface RecordListener {
    void recordAdded(RecordStore recordStore, int recordId);
    void recordChanged(RecordStore recordStore, int recordId);
    void recordDeleted(RecordStore recordStore, int recordId);
}
