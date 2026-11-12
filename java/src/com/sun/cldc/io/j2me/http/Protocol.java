/*
 * amiga-j2me: MIDP 2.0 implementation for the Amiga J2ME runtime.
 * Written for this project; licensed under GPLv2 (see COPYING).
 */
package com.sun.cldc.io.j2me.http;

import com.sun.cldc.io.ConnectionBaseInterface;
import java.io.ByteArrayOutputStream;
import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.Vector;
import javax.microedition.io.Connection;
import javax.microedition.io.Connector;
import javax.microedition.io.HttpConnection;
import javax.microedition.io.StreamConnection;

/**
 * Connector.open("http://..."): HTTP/1.0 over a socket connection, one request
 * per connection. The request goes out when the response is first asked for.
 */
public class Protocol implements ConnectionBaseInterface, HttpConnection {
    private String url, host, file = "/", query, ref;
    private int port = 80;
    /** "http", or "https" in the subclass (then the socket is an ssl:// one). */
    protected String scheme = "http";
    private String method = GET;
    private final Vector reqKeys = new Vector(), reqValues = new Vector();
    private final Vector respKeys = new Vector(), respValues = new Vector();
    private ByteArrayOutputStream body;
    private StreamConnection socket;
    private InputStream in;
    private int status = -1;
    private String message;
    private boolean closed;

    public Connection openPrim(String name, int mode, boolean timeouts) throws IOException {
        if (!name.startsWith("//")) throw new IllegalArgumentException("bad URL");
        url = scheme + ":" + name;
        if (scheme.equals("https")) port = 443;
        String rest = name.substring(2);
        int slash = rest.indexOf('/');
        String hostPort = slash < 0 ? rest : rest.substring(0, slash);
        String path = slash < 0 ? "/" : rest.substring(slash);
        int hash = path.indexOf('#');
        if (hash >= 0) { ref = path.substring(hash + 1); path = path.substring(0, hash); }
        int q = path.indexOf('?');
        if (q >= 0) { query = path.substring(q + 1); path = path.substring(0, q); }
        file = path;
        int colon = hostPort.indexOf(':');
        if (colon >= 0) {
            port = Integer.parseInt(hostPort.substring(colon + 1));
            hostPort = hostPort.substring(0, colon);
        }
        if (hostPort.length() == 0) throw new IllegalArgumentException("no host in the URL");
        host = hostPort;
        return this;
    }

    public String getURL() { return url; }
    public String getProtocol() { return scheme; }
    public String getHost() { return host; }
    public String getFile() { return file; }
    public String getRef() { return ref; }
    public String getQuery() { return query; }
    public int getPort() { return port; }
    public String getRequestMethod() { return method; }

    public void setRequestMethod(String m) throws IOException {
        if (status >= 0 || in != null) throw new IOException("already connected");
        if (!m.equals(GET) && !m.equals(POST) && !m.equals(HEAD)) throw new IOException("unsupported method " + m);
        method = m;
    }

    private static int find(Vector keys, String key) {
        for (int i = 0; i < keys.size(); i++) {
            if (((String) keys.elementAt(i)).toLowerCase().equals(key.toLowerCase())) return i;
        }
        return -1;
    }

    public String getRequestProperty(String key) {
        int i = find(reqKeys, key);
        return i < 0 ? null : (String) reqValues.elementAt(i);
    }

    public void setRequestProperty(String key, String value) throws IOException {
        if (status >= 0 || in != null) throw new IOException("already connected");
        int i = find(reqKeys, key);
        if (i >= 0) reqValues.setElementAt(value, i);
        else { reqKeys.addElement(key); reqValues.addElement(value); }
    }

    public OutputStream openOutputStream() throws IOException {
        if (closed) throw new IOException("closed");
        if (status >= 0 || in != null) throw new IOException("the request was already sent");
        if (body == null) body = new ByteArrayOutputStream(256);
        return body;
    }

    public DataOutputStream openDataOutputStream() throws IOException {
        return new DataOutputStream(openOutputStream());
    }

    private static String readLine(InputStream s) throws IOException {
        StringBuffer sb = new StringBuffer();
        int c;
        while ((c = s.read()) >= 0 && c != '\n') {
            if (c != '\r') sb.append((char) c);
        }
        return c < 0 && sb.length() == 0 ? null : sb.toString();
    }

    /** Sends the request and reads the status line and the headers. */
    private void connect() throws IOException {
        if (closed) throw new IOException("closed");
        if (in != null) return;
        socket = (StreamConnection) Connector.open((scheme.equals("https") ? "ssl://" : "socket://") + host + ":" + port);
        OutputStream out = socket.openOutputStream();
        StringBuffer rq = new StringBuffer();
        rq.append(method).append(' ').append(file);
        if (query != null) rq.append('?').append(query);
        rq.append(" HTTP/1.0\r\nHost: ").append(host);
        if (port != (scheme.equals("https") ? 443 : 80)) rq.append(':').append(port);
        rq.append("\r\n");
        byte[] data = body == null ? null : body.toByteArray();
        if (data != null && find(reqKeys, "Content-Length") < 0) {
            rq.append("Content-Length: ").append(data.length).append("\r\n");
        }
        for (int i = 0; i < reqKeys.size(); i++) {
            rq.append((String) reqKeys.elementAt(i)).append(": ").append((String) reqValues.elementAt(i)).append("\r\n");
        }
        rq.append("Connection: close\r\n\r\n");
        out.write(rq.toString().getBytes());
        if (data != null) out.write(data);
        out.flush();

        in = new Buffered(socket.openInputStream());
        String line = readLine(in);
        if (line == null || !line.startsWith("HTTP/")) throw new IOException("no HTTP response");
        int sp = line.indexOf(' ');
        int sp2 = sp < 0 ? -1 : line.indexOf(' ', sp + 1);
        try {
            status = Integer.parseInt(sp2 < 0 ? line.substring(sp + 1).trim() : line.substring(sp + 1, sp2));
        } catch (RuntimeException e) {
            throw new IOException("bad HTTP status line");
        }
        message = sp2 < 0 ? "" : line.substring(sp2 + 1);
        while ((line = readLine(in)) != null && line.length() > 0) {
            int colon = line.indexOf(':');
            if (colon > 0) {
                respKeys.addElement(line.substring(0, colon).trim());
                respValues.addElement(line.substring(colon + 1).trim());
            }
        }
    }

    public InputStream openInputStream() throws IOException {
        connect();
        final InputStream src = in;
        final long length = getLength();
        // stops at Content-Length when the server gave one
        return new InputStream() {
            private long left = length;
            public int read() throws IOException {
                if (left == 0) return -1;
                int c = src.read();
                if (c >= 0 && left > 0) left--;
                return c;
            }
            public int read(byte[] b, int off, int len) throws IOException {
                if (off < 0 || len < 0 || off + len > b.length) throw new IndexOutOfBoundsException();
                if (len == 0) return 0;
                if (left == 0) return -1;
                if (left > 0 && len > left) len = (int) left;
                int n = src.read(b, off, len);
                if (n > 0 && left > 0) left -= n;
                return n;
            }
            public int available() throws IOException {
                int a = src.available();
                return left >= 0 && a > left ? (int) left : a;
            }
            public void close() throws IOException { src.close(); }
        };
    }

    /**
     * Reads the socket in blocks: a native call per byte is slow, and for a TLS
     * connection under WarpOS each one is a switch to the 68k and back.
     */
    private static final class Buffered extends InputStream {
        private final InputStream src;
        private final byte[] buf = new byte[1024];
        private int pos, end;
        private boolean eof;

        Buffered(InputStream src) { this.src = src; }

        private boolean fill() throws IOException {
            if (eof) return false;
            int n = src.read(buf, 0, buf.length);
            if (n <= 0) { eof = true; return false; }
            pos = 0; end = n;
            return true;
        }
        public int read() throws IOException {
            if (pos >= end && !fill()) return -1;
            return buf[pos++] & 0xFF;
        }
        public int read(byte[] b, int off, int len) throws IOException {
            if (len == 0) return 0;
            if (pos >= end && !fill()) return -1;
            int n = Math.min(len, end - pos);
            System.arraycopy(buf, pos, b, off, n);
            pos += n;
            return n;
        }
        public int available() throws IOException { return end - pos + (eof ? 0 : src.available()); }
        public void close() throws IOException { src.close(); }
    }

    public DataInputStream openDataInputStream() throws IOException {
        return new DataInputStream(openInputStream());
    }

    public int getResponseCode() throws IOException { connect(); return status; }
    public String getResponseMessage() throws IOException { connect(); return message; }

    public String getHeaderField(String name) throws IOException {
        connect();
        int i = find(respKeys, name);
        return i < 0 ? null : (String) respValues.elementAt(i);
    }
    public String getHeaderField(int n) throws IOException {
        connect();
        return n < 0 || n >= respValues.size() ? null : (String) respValues.elementAt(n);
    }
    public String getHeaderFieldKey(int n) throws IOException {
        connect();
        return n < 0 || n >= respKeys.size() ? null : (String) respKeys.elementAt(n);
    }
    public int getHeaderFieldInt(String name, int def) throws IOException {
        String v = getHeaderField(name);
        try {
            return v == null ? def : Integer.parseInt(v);
        } catch (NumberFormatException e) {
            return def;
        }
    }
    public long getHeaderFieldDate(String name, long def) throws IOException { return def; }
    public long getExpiration() throws IOException { return 0; }
    public long getDate() throws IOException { return 0; }
    public long getLastModified() throws IOException { return 0; }

    public String getType() {
        try {
            return getHeaderField("Content-Type");
        } catch (IOException e) {
            return null;
        }
    }
    public String getEncoding() {
        try {
            return getHeaderField("Content-Encoding");
        } catch (IOException e) {
            return null;
        }
    }
    public long getLength() {
        try {
            return getHeaderFieldInt("Content-Length", -1);
        } catch (IOException e) {
            return -1;
        }
    }

    public void close() throws IOException {
        if (closed) return;
        closed = true;
        if (in != null) in.close();
        if (socket != null) socket.close();
    }
}
