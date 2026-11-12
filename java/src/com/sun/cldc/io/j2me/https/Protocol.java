/*
 * amiga-j2me: MIDP 2.0 implementation for the Amiga J2ME runtime.
 * Written for this project; licensed under GPLv2 (see COPYING).
 */
package com.sun.cldc.io.j2me.https;

import java.io.IOException;
import javax.microedition.io.HttpsConnection;
import javax.microedition.io.SecurityInfo;

/** Connector.open("https://..."): the http connection over an ssl:// socket. */
public class Protocol extends com.sun.cldc.io.j2me.http.Protocol implements HttpsConnection {
    public Protocol() { scheme = "https"; }
    public SecurityInfo getSecurityInfo() throws IOException {
        getResponseCode();      // connects
        return com.sun.cldc.io.j2me.ssl.Protocol.INFO;
    }
}
