/*
 * amiga-j2me: Motorola iDEN micro3d API for the Amiga J2ME runtime, a layer
 * over the Mascot Capsule v3 implementation.
 * Written for this project; licensed under GPLv2 (see COPYING).
 */
package com.motorola.iden.micro3d;

import java.util.Enumeration;
import java.util.Vector;

/** Up to 16 model textures, one of them current. */
public class MultiTexture extends Texture {
    public static final int MAX_TEXTURES = 16;
    private final Vector list = new Vector();
    private final int capacity;
    private int current;

    public MultiTexture() { this(MAX_TEXTURES); }
    public MultiTexture(int capacity) {
        if (capacity < 1 || capacity > MAX_TEXTURES) throw new IllegalArgumentException();
        this.capacity = capacity;
    }

    private static void check(Texture t) {
        if (t == null) throw new NullPointerException();
        if (t instanceof MultiTexture || t.isSphereTexture()) throw new IllegalArgumentException();
    }

    public boolean addTexture(Texture t) {
        check(t);
        if (list.size() >= capacity) return false;
        list.addElement(t);
        return true;
    }
    public boolean insertTextureAt(Texture t, int index) {
        check(t);
        if (list.size() >= capacity) return false;
        list.insertElementAt(t, index);
        return true;
    }
    public void setTextureAt(Texture t, int index) { check(t); list.setElementAt(t, index); }
    public boolean removeTexture(Texture t) { boolean r = list.removeElement(t); fix(); return r; }
    public void removeTextureAt(int index) { list.removeElementAt(index); fix(); }
    public void removeAllTextures() { list.removeAllElements(); current = 0; }
    private void fix() { if (current >= list.size()) current = Math.max(0, list.size() - 1); }

    public int capacity() { return capacity; }
    public int size() { return list.size(); }
    public boolean isEmpty() { return list.isEmpty(); }
    public boolean contains(Texture t) { return list.contains(t); }
    public int indexOf(Texture t) { return list.indexOf(t); }
    public int indexOf(Texture t, int index) { return list.indexOf(t, index); }
    public int lastIndexOf(Texture t) { return list.lastIndexOf(t); }
    public int lastIndexOf(Texture t, int index) { return list.lastIndexOf(t, index); }
    public Texture textureAt(int index) { return (Texture) list.elementAt(index); }
    public Enumeration textures() { return list.elements(); }
    public int getCurrentIndex() { return list.isEmpty() ? -1 : current; }
    public Texture getCurrentTexture() { return list.isEmpty() ? null : (Texture) list.elementAt(current); }
    public void setCurrentIndex(int index) {
        if (index < 0 || index >= list.size()) throw new ArrayIndexOutOfBoundsException();
        current = index;
    }
}
