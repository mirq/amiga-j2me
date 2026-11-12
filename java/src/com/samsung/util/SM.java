/*
 * amiga-j2me: MIDP 2.0 implementation for the Amiga J2ME runtime.
 * Written for this project; licensed under GPLv2 (see COPYING).
 */
package com.samsung.util;

/** A short message; sending is not supported. */
public class SM {
    private String dest, callback, data;

    public SM() {}
    public SM(String dest, String callback, String textMessage) {
        setDestAddress(dest); setCallbackAddress(callback); setData(textMessage);
    }

    public String getDestAddress() { return dest; }
    public String getCallbackAddress() { return callback; }
    public String getData() { return data; }
    public void setDestAddress(String a) { dest = a; }
    public void setCallbackAddress(String a) { callback = a; }
    public void setData(String d) {
        if (d != null && d.length() > 80) throw new IllegalArgumentException();
        data = d;
    }
}
