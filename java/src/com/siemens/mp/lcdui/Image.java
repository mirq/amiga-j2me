/*
 * amiga-j2me: MIDP 2.0 implementation for the Amiga J2ME runtime.
 * Written for this project; licensed under GPLv2 (see COPYING).
 */
package com.siemens.mp.lcdui;

import java.io.IOException;

public class Image extends com.siemens.mp.ui.Image {
    protected Image() {}

    public static javax.microedition.lcdui.Image createImageFromFile(String name, int width, int height) throws IOException {
        return javax.microedition.lcdui.Image.createImage(name);
    }
}
