/*
 * amiga-j2me: MIDP 2.0 implementation for the Amiga J2ME runtime.
 * Written for this project; licensed under GPLv2 (see COPYING).
 */
package javax.microedition.io;

import java.io.IOException;

public interface SecureConnection extends SocketConnection {
    SecurityInfo getSecurityInfo() throws IOException;
}
