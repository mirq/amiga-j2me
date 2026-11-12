/*
 * amiga-j2me: MIDP 2.0 implementation for the Amiga J2ME runtime.
 * Written for this project; licensed under GPLv2 (see COPYING).
 */
package com.sun.cldc.io.j2me.ssl;

import java.io.IOException;
import javax.microedition.io.SecureConnection;
import javax.microedition.io.SecurityInfo;
import javax.microedition.pki.Certificate;

/** Connector.open("ssl://host:port"): a TCP connection inside TLS (AmiSSL does the work). */
public class Protocol extends com.sun.cldc.io.j2me.socket.Protocol implements SecureConnection {
    /** What can be said without asking the TLS library: the certificate itself is not exposed. */
    public static final SecurityInfo INFO = new SecurityInfo() {
        public Certificate getServerCertificate() { return null; }
        public String getProtocolVersion() { return "3.1"; }
        public String getProtocolName() { return "TLS"; }
        public String getCipherSuite() { return "TLS"; }
    };

    public Protocol() { tls = true; }
    public SecurityInfo getSecurityInfo() throws IOException { return INFO; }
}
