/*
 * amiga-j2me: Motorola iDEN micro3d API for the Amiga J2ME runtime, a layer
 * over the Mascot Capsule v3 implementation.
 * Written for this project; licensed under GPLv2 (see COPYING).
 */
package com.motorola.iden.micro3d;

import java.io.IOException;

public class ActionTable {
    final com.mascotcapsule.micro3d.v3.ActionTable table;

    private ActionTable(byte[] d) throws IOException {
        try {
            table = new com.mascotcapsule.micro3d.v3.ActionTable(d);
        } catch (RuntimeException e) {
            throw new IOException(e.getMessage());
        }
    }

    public static ActionTable createActionTable(byte[] data, int offset, int length) throws IOException {
        if (data == null) throw new NullPointerException();
        if (offset < 0 || length < 0 || offset + length > data.length) throw new ArrayIndexOutOfBoundsException();
        byte[] d = new byte[length];
        System.arraycopy(data, offset, d, 0, length);
        return new ActionTable(d);
    }

    public static ActionTable createActionTable(String name) throws IOException { return new ActionTable(Figure.resource(name)); }

    public int getNumberOfActions() { return table.getNumActions(); }
    public int getNumberOfFrames(int actionIndex) { return table.getNumFrames(actionIndex); }
}
