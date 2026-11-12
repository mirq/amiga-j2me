/*
 * amiga-j2me: MIDP 2.0 implementation for the Amiga J2ME runtime.
 * Written for this project; licensed under GPLv2 (see COPYING).
 */
package javax.microedition.io;

import java.io.IOException;

public interface SocketConnection extends StreamConnection {
    byte DELAY = 0;
    byte LINGER = 1;
    byte KEEPALIVE = 2;
    byte RCVBUF = 3;
    byte SNDBUF = 4;

    void setSocketOption(byte option, int value) throws IllegalArgumentException, IOException;
    int getSocketOption(byte option) throws IllegalArgumentException, IOException;
    String getLocalAddress() throws IOException;
    int getLocalPort() throws IOException;
    String getAddress() throws IOException;
    int getPort() throws IOException;
}
