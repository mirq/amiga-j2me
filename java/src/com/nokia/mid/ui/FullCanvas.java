/*
 * amiga-j2me: MIDP 2.0 implementation for the Amiga J2ME runtime.
 * Written for this project; licensed under GPLv2 (see COPYING).
 */
package com.nokia.mid.ui;

import javax.microedition.lcdui.Canvas;
import javax.microedition.lcdui.Command;
import javax.microedition.lcdui.CommandListener;

/** Nokia UI API: a Canvas that always covers the whole screen. */
public abstract class FullCanvas extends Canvas {
    public static final int KEY_SOFTKEY1 = -6, KEY_SOFTKEY2 = -7, KEY_SEND = -10, KEY_END = -11,
                            KEY_SOFTKEY3 = -5, KEY_UP_ARROW = -1, KEY_DOWN_ARROW = -2,
                            KEY_LEFT_ARROW = -3, KEY_RIGHT_ARROW = -4;

    protected FullCanvas() { setFullScreenMode(true); }

    public void addCommand(Command cmd) { throw new IllegalStateException(); }
    public void setCommandListener(CommandListener l) { throw new IllegalStateException(); }
}
