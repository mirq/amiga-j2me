/*
 * amiga-j2me: MIDP 2.0 implementation for the Amiga J2ME runtime.
 * Written for this project; licensed under GPLv2 (see COPYING).
 */
package javax.microedition.lcdui;

import java.util.Vector;

public class Alert extends Screen {
    public static final int FOREVER = -2;
    public static final Command DISMISS_COMMAND = new Command("OK", Command.OK, 0);
    private static final int DEFAULT_TIMEOUT = 2500;

    Displayable next;
    private String text;
    private Image image;
    private AlertType type;
    private Gauge indicator;
    private int timeout = DEFAULT_TIMEOUT;
    private long shownAt;

    public Alert(String title) { this(title, null, null, null); }

    public Alert(String title, String alertText, Image alertImage, AlertType alertType) {
        setTitle(title);
        text = alertText; image = alertImage; type = alertType;
    }

    public int getDefaultTimeout() { return DEFAULT_TIMEOUT; }
    public int getTimeout() { return timeout; }
    public void setTimeout(int time) {
        if (time <= 0 && time != FOREVER) throw new IllegalArgumentException();
        timeout = time;
    }
    public AlertType getType() { return type; }
    public void setType(AlertType t) { type = t; }
    public String getString() { return text; }
    public void setString(String s) { text = s; requestRepaint(); }
    public Image getImage() { return image; }
    public void setImage(Image img) { image = img; requestRepaint(); }
    public void setIndicator(Gauge g) { indicator = g; requestRepaint(); }
    public Gauge getIndicator() { return indicator; }

    /** Without commands of its own an alert has the dismiss command. */
    Command implicitCommand() { return commands.size() == 0 ? DISMISS_COMMAND : null; }

    void fireCommand(Command c) {
        if (c == DISMISS_COMMAND && listener == null) dismiss();
        else super.fireCommand(c);
    }

    private void dismiss() {
        Displayable n = next;
        if (n != null && Display.instance.current == this) Display.instance.setCurrent(n);
    }

    void showNotifyImpl() {
        shownAt = System.currentTimeMillis();
        if (timeout != FOREVER && commands.size() == 0) {
            final Alert self = this;
            final long stamp = shownAt;
            new Thread(new Runnable() {
                public void run() {
                    try { Thread.sleep(timeout); } catch (InterruptedException e) { }
                    if (self.shownAt == stamp) self.dismiss();
                }
            }).start();
        }
    }

    void hideNotifyImpl() { shownAt = 0; }

    void paintContent(Graphics g, int top, int w, int h) {
        int y = top + 6;
        if (image != null) {
            g.drawImage(image, w / 2, y, Graphics.TOP | Graphics.HCENTER);
            y += image.getHeight() + 6;
        }
        g.setColor(FG); g.setFont(TEXT_FONT);
        Vector lines = wrap(text, TEXT_FONT, w - 12);
        for (int i = 0; i < lines.size(); i++) {
            g.drawString((String) lines.elementAt(i), 6, y, Graphics.TOP | Graphics.LEFT);
            y += TEXT_FONT.getHeight();
        }
        if (indicator != null) indicator.paintContent(g, 6, y + 6, w - 12, false);
    }

    boolean contentKey(int type, int keyCode, int ch) {
        if (type == 0 && keyCode == Canvas.KEY_FIRE && commands.size() == 0) { dismiss(); return true; }
        return false;
    }
}
