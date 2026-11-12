/*
 * amiga-j2me: MIDP 2.0 implementation for the Amiga J2ME runtime.
 * Written for this project; licensed under GPLv2 (see COPYING).
 */
package javax.microedition.io;

import javax.microedition.pki.Certificate;

public interface SecurityInfo {
    Certificate getServerCertificate();
    String getProtocolVersion();
    String getProtocolName();
    String getCipherSuite();
}
