/*
 * amiga-j2me: Motorola iDEN micro3d API for the Amiga J2ME runtime, a layer
 * over the Mascot Capsule v3 implementation.
 * Written for this project; licensed under GPLv2 (see COPYING).
 */
package com.motorola.iden.micro3d;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;

public class Figure extends Object3D {
    final com.mascotcapsule.micro3d.v3.Figure figure;
    private ActionTable table;
    private int action, frame, pattern;

    private Figure(byte[] d) throws IOException {
        try {
            figure = new com.mascotcapsule.micro3d.v3.Figure(d);
        } catch (RuntimeException e) {
            throw new IOException(e.getMessage());
        }
    }

    static byte[] resource(String name) throws IOException {
        if (name == null) throw new NullPointerException();
        InputStream in = Figure.class.getResourceAsStream(name.startsWith("/") ? name : "/" + name);
        if (in == null) throw new IOException(name + " not found");
        ByteArrayOutputStream out = new ByteArrayOutputStream(4096);
        byte[] buf = new byte[1024];
        int n;
        while ((n = in.read(buf)) > 0) out.write(buf, 0, n);
        in.close();
        return out.toByteArray();
    }

    public static Figure createFigure(byte[] data, int offset, int length) throws IOException {
        if (data == null) throw new NullPointerException();
        if (offset < 0 || length < 0 || offset + length > data.length) throw new ArrayIndexOutOfBoundsException();
        byte[] d = new byte[length];
        System.arraycopy(data, offset, d, 0, length);
        return new Figure(d);
    }

    public static Figure createFigure(String name) throws IOException { return new Figure(resource(name)); }

    public int getActionIndex() { return action; }
    public int getFrameIndex() { return frame; }
    public ActionTable getActionTable() { return table; }
    public int getNumberOfPatterns() { return figure.getNumPattern(); }
    public int getPattern() { return pattern; }
    public void setPattern(int pattern) { this.pattern = pattern; figure.setPattern(pattern); }
    public void setActionTable(ActionTable t) { table = t; action = 0; frame = 0; }

    public void setPosture(int actionIndex, int frameIndex) {
        if (table == null) throw new RuntimeException("no action table");
        if (actionIndex < 0 || actionIndex >= table.getNumberOfActions()) throw new IllegalArgumentException();
        action = actionIndex; frame = frameIndex;
        figure.setPosture(table.table, actionIndex, frameIndex);
    }

    /** Hands the textures to the v3 figure (without one only its coloured polygons show). */
    boolean prepare() {
        if (texture instanceof MultiTexture) {
            MultiTexture m = (MultiTexture) texture;
            if (m.isEmpty()) return true;
            com.mascotcapsule.micro3d.v3.Texture[] a = new com.mascotcapsule.micro3d.v3.Texture[m.size()];
            for (int i = 0; i < a.length; i++) a[i] = m.textureAt(i).model;
            figure.setTexture(a);
            figure.selectTexture(m.getCurrentIndex());
            return true;
        }
        if (texture == null || texture.model == null) return true;
        figure.setTexture(texture.model);
        return true;
    }
}
