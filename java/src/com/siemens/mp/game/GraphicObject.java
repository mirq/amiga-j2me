/*
 * amiga-j2me: MIDP 2.0 implementation for the Amiga J2ME runtime.
 * Written for this project; licensed under GPLv2 (see COPYING).
 */
package com.siemens.mp.game;

import javax.microedition.lcdui.Graphics;

public abstract class GraphicObject extends com.siemens.mp.misc.NativeMem {
    private boolean visible = true;

    protected GraphicObject() {}
    public boolean getVisible() { return visible; }
    public void setVisible(boolean v) { visible = v; }
    protected abstract void paint(Graphics g);
    void draw(Graphics g) { if (visible) paint(g); }
}
