/*
 * amiga-j2me: MIDP 2.0 implementation for the Amiga J2ME runtime.
 * Written for this project; licensed under GPLv2 (see COPYING).
 */
package com.sun.cldc.io.j2me.socket;

import com.sun.cldc.io.ConnectionBaseInterface;
import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import javax.microedition.io.Connection;
import javax.microedition.io.SocketConnection;

/** Connector.open("socket://host:port"): a TCP client connection. The calls block. */
public class Protocol implements ConnectionBaseInterface, SocketConnection {
    private int handle = -1;
    private String host;
    private int port;
    private boolean open;
    private final int[] options = { 0, 0, 0, 8192, 8192 };

    public Connection openPrim(String name, int mode, boolean timeouts) throws IOException {
        if (!name.startsWith("//")) throw new IllegalArgumentException("bad socket URL");
        int semi = name.indexOf(';');
        if (semi >= 0) name = name.substring(0, semi);
        int colon = name.lastIndexOf(':');
        if (colon < 3) throw new IllegalArgumentException("socket URL needs host:port");
        host = name.substring(2, colon);
        try {
            port = Integer.parseInt(name.substring(colon + 1));
        } catch (NumberFormatException e) {
            throw new IllegalArgumentException("bad port in socket URL");
        }
        byte[] cstring = new byte[host.length() + 1];
        for (int i = 0; i < host.length(); i++) cstring[i] = (byte) host.charAt(i);
        handle = open0(cstring, port, mode);
        if (handle < 0) throw new IOException("cannot connect to " + host + ":" + port);
        open = true;
        return this;
    }

    private void ensureOpen() throws IOException {
        if (!open) throw new IOException("connection closed");
    }

    public InputStream openInputStream() throws IOException {
        ensureOpen();
        return new InputStream() {
            public int read() throws IOException {
                ensureOpen();
                return readByte(handle);
            }
            public int read(byte[] b, int off, int len) throws IOException {
                ensureOpen();
                if (off < 0 || len < 0 || off + len > b.length) throw new IndexOutOfBoundsException();
                return len == 0 ? 0 : readBuf(handle, b, off, len);
            }
            public int available() throws IOException {
                ensureOpen();
                return available0(handle);
            }
        };
    }

    public OutputStream openOutputStream() throws IOException {
        ensureOpen();
        return new OutputStream() {
            public void write(int b) throws IOException {
                ensureOpen();
                if (writeByte(handle, b) < 0) throw new IOException("socket write failed");
            }
            public void write(byte[] b, int off, int len) throws IOException {
                ensureOpen();
                if (off < 0 || len < 0 || off + len > b.length) throw new IndexOutOfBoundsException();
                while (len > 0) {
                    int n = writeBuf(handle, b, off, len);
                    if (n <= 0) throw new IOException("socket write failed");
                    off += n;
                    len -= n;
                }
            }
        };
    }

    public DataInputStream openDataInputStream() throws IOException {
        return new DataInputStream(openInputStream());
    }

    public DataOutputStream openDataOutputStream() throws IOException {
        return new DataOutputStream(openOutputStream());
    }

    public synchronized void close() throws IOException {
        if (open) {
            open = false;
            close0(handle);
        }
    }

    public void setSocketOption(byte option, int value) throws IOException {
        ensureOpen();
        if (option < 0 || option >= options.length || value < 0) throw new IllegalArgumentException();
        options[option] = value;
    }

    public int getSocketOption(byte option) throws IOException {
        ensureOpen();
        if (option < 0 || option >= options.length) throw new IllegalArgumentException();
        return options[option];
    }

    public String getLocalAddress() throws IOException { ensureOpen(); return "127.0.0.1"; }
    public int getLocalPort() throws IOException { ensureOpen(); return 0; }
    public String getAddress() throws IOException { ensureOpen(); return host; }
    public int getPort() throws IOException { ensureOpen(); return port; }

    private static native int open0(byte[] hostname, int port, int mode);
    private static native int readBuf(int handle, byte[] b, int off, int len);
    private static native int readByte(int handle);
    private static native int writeBuf(int handle, byte[] b, int off, int len);
    private static native int writeByte(int handle, int b);
    private static native int available0(int handle);
    private static native void close0(int handle);
}
