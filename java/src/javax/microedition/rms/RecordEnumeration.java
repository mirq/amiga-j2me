/*
 * amiga-j2me: MIDP 2.0 implementation for the Amiga J2ME runtime.
 * Written for this project; licensed under GPLv2 (see COPYING).
 */
package javax.microedition.rms;

public interface RecordEnumeration {
    int numRecords();
    byte[] nextRecord() throws InvalidRecordIDException, RecordStoreNotOpenException, RecordStoreException;
    int nextRecordId() throws InvalidRecordIDException;
    byte[] previousRecord() throws InvalidRecordIDException, RecordStoreNotOpenException, RecordStoreException;
    int previousRecordId() throws InvalidRecordIDException;
    boolean hasNextElement();
    boolean hasPreviousElement();
    void reset();
    void rebuild();
    void keepUpdated(boolean keepUpdated);
    boolean isKeptUpdated();
    void destroy();
}
