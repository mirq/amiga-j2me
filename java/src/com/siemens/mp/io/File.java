/*
 * amiga-j2me: MIDP 2.0 implementation for the Amiga J2ME runtime.
 * Written for this project; licensed under GPLv2 (see COPYING).
 */
package com.siemens.mp.io;

import java.io.IOException;
import java.util.Hashtable;
import javax.microedition.rms.RecordStore;

/**
 * Siemens flat file API. The files live in the record store "siemens.files",
 * one record per file, so games keep their saves between runs.
 */
public class File {
    public static final int INSIDE_STORAGE_PATH = 1, OUTSIDE_STORAGE_PATH = 0;
    public static final String STORAGE_DRIVE = "a:";

    private static final String STORE = "siemens.files";
    private static final Hashtable ids = new Hashtable();       // name -> Integer record id
    private static boolean loaded;

    private final String[] names = new String[16];
    private final byte[][] data = new byte[16][];
    private final int[] size = new int[16], pos = new int[16];
    private final boolean[] dirty = new boolean[16];

    public File() {}

    private static synchronized void index() {
        if (loaded) return;
        loaded = true;
        try {
            RecordStore rs = RecordStore.openRecordStore(STORE, true);
            for (int id = 1; id < rs.getNextRecordID(); id++) {
                try {
                    byte[] r = rs.getRecord(id);
                    if (r != null && r.length >= 1 && r.length > (r[0] & 0xFF)) ids.put(new String(r, 1, r[0] & 0xFF), new Integer(id));
                } catch (Exception e) {
                    // a deleted record
                }
            }
            rs.closeRecordStore();
        } catch (Exception e) {
            // no storage: files last until the game ends
        }
    }

    private static String key(String name) {
        String n = name.replace('\\', '/').toLowerCase();
        int c = n.indexOf(':');
        if (c >= 0) n = n.substring(c + 1);
        while (n.startsWith("/")) n = n.substring(1);
        return n.length() > 200 ? n.substring(n.length() - 200) : n;
    }

    private static byte[] load(String key) {
        Integer id = (Integer) ids.get(key);
        if (id == null) return null;
        try {
            RecordStore rs = RecordStore.openRecordStore(STORE, true);
            byte[] r = rs.getRecord(id.intValue());
            rs.closeRecordStore();
            int n = r[0] & 0xFF;
            byte[] d = new byte[r.length - 1 - n];
            System.arraycopy(r, 1 + n, d, 0, d.length);
            return d;
        } catch (Exception e) {
            return null;
        }
    }

    private static void store(String key, byte[] d, int len) {
        byte[] k = key.getBytes();
        byte[] r = new byte[1 + k.length + len];
        r[0] = (byte) k.length;
        System.arraycopy(k, 0, r, 1, k.length);
        System.arraycopy(d, 0, r, 1 + k.length, len);
        try {
            RecordStore rs = RecordStore.openRecordStore(STORE, true);
            Integer id = (Integer) ids.get(key);
            if (id == null) ids.put(key, new Integer(rs.addRecord(r, 0, r.length)));
            else rs.setRecord(id.intValue(), r, 0, r.length);
            rs.closeRecordStore();
        } catch (Exception e) {
            // kept in memory only
        }
    }

    public static int exists(String fileName) {
        index();
        return ids.containsKey(key(fileName)) ? 1 : -1;
    }

    public static int delete(String fileName) {
        index();
        Integer id = (Integer) ids.remove(key(fileName));
        if (id == null) return -1;
        try {
            RecordStore rs = RecordStore.openRecordStore(STORE, true);
            rs.deleteRecord(id.intValue());
            rs.closeRecordStore();
        } catch (Exception e) {
            // already gone
        }
        return 1;
    }

    public static int rename(String source, String dest) {
        index();
        byte[] d = load(key(source));
        if (d == null) return -1;
        store(key(dest), d, d.length);
        delete(source);
        return 1;
    }

    public static int copy(String source, String dest) {
        index();
        byte[] d = load(key(source));
        if (d == null) return -1;
        store(key(dest), d, d.length);
        return 1;
    }

    public static int spaceAvailable() { return 1024 * 1024; }
    public static String[] list(String path) { return new String[0]; }
    public static boolean isDirectory(String path) { return false; }
    public static String getRelativePath(String path) { return path; }
    public static int checkFileName(String name) { return 0; }

    /** Opens the file, creating it when it does not exist; returns a descriptor. */
    public int open(String fileName) throws IOException {
        index();
        for (int fd = 0; fd < names.length; fd++) {
            if (names[fd] == null) {
                String k = key(fileName);
                byte[] d = load(k);
                names[fd] = k;
                data[fd] = d == null ? new byte[64] : d;
                size[fd] = d == null ? 0 : d.length;
                pos[fd] = 0;
                dirty[fd] = d == null;
                return fd;
            }
        }
        throw new IOException("too many open files");
    }

    private void check(int fd) throws IOException {
        if (fd < 0 || fd >= names.length || names[fd] == null) throw new IOException("bad file descriptor");
    }

    public int close(int fd) throws IOException {
        check(fd);
        if (dirty[fd]) store(names[fd], data[fd], size[fd]);
        names[fd] = null;
        data[fd] = null;
        return 1;
    }

    public int length(int fd) throws IOException { check(fd); return size[fd]; }

    public int seek(int fd, int position) throws IOException {
        check(fd);
        if (position < 0 || position > size[fd]) throw new IOException("seek past the end");
        pos[fd] = position;
        return position;
    }

    public int read(int fd, byte[] buf, int offset, int numBytes) throws IOException {
        check(fd);
        int n = Math.min(numBytes, size[fd] - pos[fd]);
        if (n <= 0) return -1;
        System.arraycopy(data[fd], pos[fd], buf, offset, n);
        pos[fd] += n;
        return n;
    }

    public int write(int fd, byte[] buf, int offset, int numBytes) throws IOException {
        check(fd);
        int end = pos[fd] + numBytes;
        if (end > data[fd].length) {
            byte[] bigger = new byte[Math.max(end, data[fd].length * 2)];
            System.arraycopy(data[fd], 0, bigger, 0, size[fd]);
            data[fd] = bigger;
        }
        System.arraycopy(buf, offset, data[fd], pos[fd], numBytes);
        pos[fd] = end;
        if (end > size[fd]) size[fd] = end;
        dirty[fd] = true;
        return numBytes;
    }

    public static int debugWrite(String fileName, String text) { return 1; }
}
