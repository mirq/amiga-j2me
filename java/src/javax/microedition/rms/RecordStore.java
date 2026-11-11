/*
 * amiga-j2me: MIDP 2.0 implementation for the Amiga J2ME runtime.
 * Written for this project; licensed under GPLv2 (see COPYING).
 */
package javax.microedition.rms;

import aj.Launcher;
import aj.Native;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.util.Hashtable;
import java.util.Vector;

/**
 * A record store kept in memory and written as one file,
 * <storage dir>/<encoded name>.rms, after every change while it is small and
 * on close (and at exit) otherwise.
 */
public class RecordStore {
    public static final int AUTHMODE_PRIVATE = 0, AUTHMODE_ANY = 1;

    private static final int MAGIC = 0x414A5253;        // "AJRS"
    private static final int SMALL = 64 * 1024;         // write-through below this size
    private static final Hashtable openStores = new Hashtable();

    private final String name;
    private int openCount;
    private int nextId = 1, version;
    private long lastModified;
    private int[] ids = new int[8];
    private byte[][] recs = new byte[8][];
    private int count, bytes;
    private boolean unsaved;
    private final Vector listeners = new Vector();

    private RecordStore(String name) { this.name = name; }

    // ---- files ----

    private static String path(String storeName) {
        StringBuffer sb = new StringBuffer(Launcher.storageDir());
        char last = sb.length() > 0 ? sb.charAt(sb.length() - 1) : '/';
        if (last != '/' && last != ':') sb.append('/');
        for (int i = 0; i < storeName.length(); i++) {
            char c = storeName.charAt(i);
            if ((c >= 'a' && c <= 'z') || (c >= '0' && c <= '9') || c == '_' || c == '-') sb.append(c);
            else { sb.append('%'); sb.append(Integer.toHexString(0x10000 | c).substring(1)); }
        }
        return sb.append(".rms").toString();
    }

    private static String decode(String file) {
        StringBuffer sb = new StringBuffer();
        for (int i = 0; i < file.length(); i++) {
            char c = file.charAt(i);
            if (c == '%' && i + 4 < file.length()) {
                sb.append((char) Integer.parseInt(file.substring(i + 1, i + 5), 16));
                i += 4;
            } else {
                sb.append(c);
            }
        }
        return sb.toString();
    }

    private boolean load() {
        byte[] data = Native.readFile(path(name));
        if (data == null) return false;
        try {
            DataInputStream in = new DataInputStream(new ByteArrayInputStream(data));
            if (in.readInt() != MAGIC) return false;
            version = in.readInt();
            nextId = in.readInt();
            lastModified = in.readLong();
            int n = in.readInt();
            ids = new int[n + 8];
            recs = new byte[n + 8][];
            for (int i = 0; i < n; i++) {
                ids[i] = in.readInt();
                byte[] r = new byte[in.readInt()];
                in.readFully(r);
                recs[i] = r;
                bytes += r.length;
            }
            count = n;
            return true;
        } catch (IOException e) {
            return false;       // damaged file: treated as missing
        }
    }

    private void save() throws RecordStoreException {
        try {
            ByteArrayOutputStream bo = new ByteArrayOutputStream(bytes + count * 8 + 32);
            DataOutputStream out = new DataOutputStream(bo);
            out.writeInt(MAGIC);
            out.writeInt(version);
            out.writeInt(nextId);
            out.writeLong(lastModified);
            out.writeInt(count);
            for (int i = 0; i < count; i++) {
                out.writeInt(ids[i]);
                out.writeInt(recs[i].length);
                out.write(recs[i]);
            }
            byte[] data = bo.toByteArray();
            Native.makeDir(Launcher.storageDir());
            if (Native.writeFile(path(name), data, data.length) != 0) {
                throw new RecordStoreException("cannot write " + path(name));
            }
            unsaved = false;
        } catch (IOException e) {
            throw new RecordStoreException(e.toString());
        }
    }

    private void changed() throws RecordStoreException {
        version++;
        lastModified = System.currentTimeMillis();
        unsaved = true;
        if (bytes < SMALL) save();
    }

    /** Called by the launcher at exit: stores that were never closed. */
    public static void flushAll() {
        synchronized (openStores) {
            java.util.Enumeration e = openStores.elements();
            while (e.hasMoreElements()) {
                RecordStore rs = (RecordStore) e.nextElement();
                try {
                    if (rs.unsaved) rs.save();
                } catch (RecordStoreException ex) {
                    System.out.println("RMS: " + ex);
                }
            }
        }
    }

    // ---- static API ----

    public static RecordStore openRecordStore(String recordStoreName, boolean createIfNecessary)
            throws RecordStoreException, RecordStoreFullException, RecordStoreNotFoundException {
        if (recordStoreName == null || recordStoreName.length() == 0 || recordStoreName.length() > 32) {
            throw new IllegalArgumentException();
        }
        synchronized (openStores) {
            RecordStore rs = (RecordStore) openStores.get(recordStoreName);
            if (rs == null) {
                rs = new RecordStore(recordStoreName);
                if (!rs.load()) {
                    if (!createIfNecessary) throw new RecordStoreNotFoundException(recordStoreName);
                    rs.lastModified = System.currentTimeMillis();
                    rs.save();
                }
                openStores.put(recordStoreName, rs);
            }
            rs.openCount++;
            return rs;
        }
    }

    public static RecordStore openRecordStore(String recordStoreName, boolean createIfNecessary,
                                              int authmode, boolean writable)
            throws RecordStoreException, RecordStoreFullException, RecordStoreNotFoundException {
        return openRecordStore(recordStoreName, createIfNecessary);
    }

    public static RecordStore openRecordStore(String recordStoreName, String vendorName, String suiteName)
            throws RecordStoreException, RecordStoreNotFoundException {
        return openRecordStore(recordStoreName, false);
    }

    public static void deleteRecordStore(String recordStoreName)
            throws RecordStoreException, RecordStoreNotFoundException {
        synchronized (openStores) {
            if (openStores.get(recordStoreName) != null) throw new RecordStoreException("store is open");
            if (Native.deleteFile(path(recordStoreName)) != 0) throw new RecordStoreNotFoundException(recordStoreName);
        }
    }

    public static String[] listRecordStores() {
        String all = Native.listFiles(Launcher.storageDir());
        Vector v = new Vector();
        int start = 0;
        while (start < all.length()) {
            int nl = all.indexOf('\n', start);
            if (nl < 0) nl = all.length();
            String f = all.substring(start, nl);
            if (f.endsWith(".rms")) v.addElement(decode(f.substring(0, f.length() - 4)));
            start = nl + 1;
        }
        if (v.size() == 0) return null;
        String[] names = new String[v.size()];
        v.copyInto(names);
        return names;
    }

    // ---- instance API ----

    private void checkOpen() throws RecordStoreNotOpenException {
        if (openCount <= 0) throw new RecordStoreNotOpenException();
    }

    private int find(int recordId) throws InvalidRecordIDException {
        for (int i = 0; i < count; i++) {
            if (ids[i] == recordId) return i;
        }
        throw new InvalidRecordIDException("no record " + recordId);
    }

    public void closeRecordStore() throws RecordStoreNotOpenException, RecordStoreException {
        synchronized (openStores) {
            checkOpen();
            if (--openCount == 0) {
                if (unsaved) save();
                openStores.remove(name);
                listeners.removeAllElements();
            }
        }
    }

    public String getName() throws RecordStoreNotOpenException { checkOpen(); return name; }
    public int getVersion() throws RecordStoreNotOpenException { checkOpen(); return version; }
    public int getNumRecords() throws RecordStoreNotOpenException { checkOpen(); return count; }
    public int getSize() throws RecordStoreNotOpenException { checkOpen(); return bytes + count * 8 + 24; }
    public int getSizeAvailable() throws RecordStoreNotOpenException { checkOpen(); return 4 * 1024 * 1024; }
    public long getLastModified() throws RecordStoreNotOpenException { checkOpen(); return lastModified; }
    public int getNextRecordID() throws RecordStoreNotOpenException, RecordStoreException { checkOpen(); return nextId; }
    public void setMode(int authmode, boolean writable) throws RecordStoreException {}

    public void addRecordListener(RecordListener listener) {
        if (listener != null && !listeners.contains(listener)) listeners.addElement(listener);
    }
    public void removeRecordListener(RecordListener listener) { listeners.removeElement(listener); }

    private void notify(int what, int id) {
        for (int i = 0; i < listeners.size(); i++) {
            RecordListener l = (RecordListener) listeners.elementAt(i);
            if (what == 0) l.recordAdded(this, id);
            else if (what == 1) l.recordChanged(this, id);
            else l.recordDeleted(this, id);
        }
    }

    private static byte[] copy(byte[] data, int offset, int numBytes) {
        byte[] r = new byte[numBytes];
        if (numBytes > 0) System.arraycopy(data, offset, r, 0, numBytes);
        return r;
    }

    public int addRecord(byte[] data, int offset, int numBytes)
            throws RecordStoreNotOpenException, RecordStoreException, RecordStoreFullException {
        int id;
        synchronized (this) {
            checkOpen();
            byte[] r = copy(data, offset, numBytes);
            if (count == ids.length) {
                int[] ni = new int[count * 2];
                byte[][] nr = new byte[count * 2][];
                System.arraycopy(ids, 0, ni, 0, count);
                System.arraycopy(recs, 0, nr, 0, count);
                ids = ni; recs = nr;
            }
            id = nextId++;
            ids[count] = id; recs[count] = r; count++;
            bytes += numBytes;
            changed();
        }
        notify(0, id);
        return id;
    }

    public void deleteRecord(int recordId)
            throws RecordStoreNotOpenException, InvalidRecordIDException, RecordStoreException {
        synchronized (this) {
            checkOpen();
            int i = find(recordId);
            bytes -= recs[i].length;
            System.arraycopy(ids, i + 1, ids, i, count - i - 1);
            System.arraycopy(recs, i + 1, recs, i, count - i - 1);
            recs[--count] = null;
            changed();
        }
        notify(2, recordId);
    }

    public int getRecordSize(int recordId)
            throws RecordStoreNotOpenException, InvalidRecordIDException, RecordStoreException {
        synchronized (this) {
            checkOpen();
            return recs[find(recordId)].length;
        }
    }

    public int getRecord(int recordId, byte[] buffer, int offset)
            throws RecordStoreNotOpenException, InvalidRecordIDException, RecordStoreException {
        synchronized (this) {
            checkOpen();
            byte[] r = recs[find(recordId)];
            System.arraycopy(r, 0, buffer, offset, r.length);
            return r.length;
        }
    }

    public byte[] getRecord(int recordId)
            throws RecordStoreNotOpenException, InvalidRecordIDException, RecordStoreException {
        synchronized (this) {
            checkOpen();
            byte[] r = recs[find(recordId)];
            return r.length == 0 ? null : copy(r, 0, r.length);
        }
    }

    public void setRecord(int recordId, byte[] newData, int offset, int numBytes)
            throws RecordStoreNotOpenException, InvalidRecordIDException, RecordStoreException,
                   RecordStoreFullException {
        synchronized (this) {
            checkOpen();
            int i = find(recordId);
            bytes += numBytes - recs[i].length;
            recs[i] = copy(newData, offset, numBytes);
            changed();
        }
        notify(1, recordId);
    }

    public RecordEnumeration enumerateRecords(RecordFilter filter, RecordComparator comparator,
                                              boolean keepUpdated) throws RecordStoreNotOpenException {
        checkOpen();
        return new Enumerator(filter, comparator, keepUpdated);
    }

    private final class Enumerator implements RecordEnumeration, RecordListener {
        private final RecordFilter filter;
        private final RecordComparator comparator;
        private boolean keep;
        private int[] order = new int[0];
        private int pos;        // index of the next element

        Enumerator(RecordFilter f, RecordComparator c, boolean keepUpdated) {
            filter = f; comparator = c;
            rebuild();
            keepUpdated(keepUpdated);
        }

        public void rebuild() {
            synchronized (RecordStore.this) {
                int[] o = new int[count];
                byte[][] d = new byte[count][];
                int n = 0;
                for (int i = 0; i < count; i++) {
                    if (filter == null || filter.matches(recs[i])) { o[n] = ids[i]; d[n] = recs[i]; n++; }
                }
                if (comparator != null) {       // insertion sort: stores are small
                    for (int i = 1; i < n; i++) {
                        int id = o[i];
                        byte[] r = d[i];
                        int j = i - 1;
                        while (j >= 0 && comparator.compare(d[j], r) == RecordComparator.FOLLOWS) {
                            o[j + 1] = o[j]; d[j + 1] = d[j]; j--;
                        }
                        o[j + 1] = id; d[j + 1] = r;
                    }
                }
                order = new int[n];
                System.arraycopy(o, 0, order, 0, n);
                pos = 0;
            }
        }

        public int numRecords() { return order.length; }
        public boolean hasNextElement() { return pos < order.length; }
        public boolean hasPreviousElement() { return pos > 0; }
        public void reset() { pos = 0; }

        public int nextRecordId() throws InvalidRecordIDException {
            if (pos >= order.length) throw new InvalidRecordIDException();
            return order[pos++];
        }

        public int previousRecordId() throws InvalidRecordIDException {
            if (pos <= 0) throw new InvalidRecordIDException();
            return order[--pos];
        }

        public byte[] nextRecord() throws InvalidRecordIDException, RecordStoreNotOpenException, RecordStoreException {
            return getRecord(nextRecordId());
        }

        public byte[] previousRecord() throws InvalidRecordIDException, RecordStoreNotOpenException, RecordStoreException {
            return getRecord(previousRecordId());
        }

        public void keepUpdated(boolean keepUpdated) {
            if (keepUpdated && !keep) addRecordListener(this);
            if (!keepUpdated && keep) removeRecordListener(this);
            keep = keepUpdated;
        }

        public boolean isKeptUpdated() { return keep; }
        public void destroy() { removeRecordListener(this); }
        public void recordAdded(RecordStore rs, int id) { rebuild(); }
        public void recordChanged(RecordStore rs, int id) { rebuild(); }
        public void recordDeleted(RecordStore rs, int id) { rebuild(); }
    }
}
