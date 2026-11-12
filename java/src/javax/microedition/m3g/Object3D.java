/*
 * amiga-j2me: JSR-184 (M3G 1.0/1.1) implementation for the Amiga J2ME runtime.
 * Written for this project; licensed under GPLv2 (see COPYING).
 */
package javax.microedition.m3g;

import java.util.Vector;

public abstract class Object3D {
    int userID;
    Object userObject;
    Vector tracks;          // AnimationTrack

    Object3D() {}

    public final int animate(int time) {
        animateThis(time);
        Vector refs = new Vector();
        collectReferences(refs);
        for (int i = 0; i < refs.size(); i++) ((Object3D) refs.elementAt(i)).animate(time);
        return 0;       // "validity": the caller may animate again at once
    }

    void animateThis(int time) {
        if (tracks == null) return;
        for (int i = 0; i < tracks.size(); i++) ((AnimationTrack) tracks.elementAt(i)).apply(this, time);
    }

    /** Sets an animated property; subclasses handle the ones they have. */
    void setProperty(int property, float[] v) {}

    /** The objects this one refers to (children, appearance, ...). */
    void collectReferences(Vector out) {}

    static void add(Vector out, Object3D o) { if (o != null && !out.contains(o)) out.addElement(o); }

    /** A shallow copy: references are shared, as M3G's duplicate does for non-node objects. */
    public final Object3D duplicate() { return duplicateImpl(); }
    Object3D duplicateImpl() { return this; }

    public Object3D find(int userID) {
        if (this.userID == userID) return this;
        Vector refs = new Vector();
        collectReferences(refs);
        for (int i = 0; i < refs.size(); i++) {
            Object3D r = ((Object3D) refs.elementAt(i)).find(userID);
            if (r != null) return r;
        }
        return null;
    }

    public int getReferences(Object3D[] references) {
        Vector refs = new Vector();
        collectReferences(refs);
        if (references != null) {
            if (references.length < refs.size()) throw new IllegalArgumentException();
            refs.copyInto(references);
        }
        return refs.size();
    }

    public void setUserID(int id) { userID = id; }
    public int getUserID() { return userID; }
    public void setUserObject(Object o) { userObject = o; }
    public Object getUserObject() { return userObject; }

    public void addAnimationTrack(AnimationTrack t) {
        if (t == null) throw new NullPointerException();
        if (tracks == null) tracks = new Vector();
        if (!tracks.contains(t)) tracks.addElement(t);
    }
    public AnimationTrack getAnimationTrack(int index) {
        if (tracks == null) throw new IndexOutOfBoundsException();
        return (AnimationTrack) tracks.elementAt(index);
    }
    public void removeAnimationTrack(AnimationTrack t) { if (tracks != null) tracks.removeElement(t); }
    public int getAnimationTrackCount() { return tracks == null ? 0 : tracks.size(); }

    void copyBase(Object3D from) {
        userID = from.userID; userObject = from.userObject;
        if (from.tracks != null) {
            tracks = new Vector();
            for (int i = 0; i < from.tracks.size(); i++) tracks.addElement(from.tracks.elementAt(i));
        }
    }
}
