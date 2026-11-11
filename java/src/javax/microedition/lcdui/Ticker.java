/*
 * amiga-j2me: MIDP 2.0 implementation for the Amiga J2ME runtime.
 * Written for this project; licensed under GPLv2 (see COPYING).
 */
package javax.microedition.lcdui;

public class Ticker {
    private String text;
    public Ticker(String str) { setString(str); }
    public String getString() { return text; }
    public void setString(String str) {
        if (str == null) throw new NullPointerException();
        text = str;
    }
}
