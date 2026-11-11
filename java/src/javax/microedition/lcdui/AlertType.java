/*
 * amiga-j2me: MIDP 2.0 implementation for the Amiga J2ME runtime.
 * Written for this project; licensed under GPLv2 (see COPYING).
 */
package javax.microedition.lcdui;

public class AlertType {
    public static final AlertType INFO = new AlertType(), WARNING = new AlertType(), ERROR = new AlertType(),
                                  ALARM = new AlertType(), CONFIRMATION = new AlertType();
    protected AlertType() {}
    public boolean playSound(Display display) { return false; }
}
