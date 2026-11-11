/*
 * amiga-j2me: MIDP 2.0 implementation for the Amiga J2ME runtime.
 * Written for this project; licensed under GPLv2 (see COPYING).
 */
package javax.microedition.lcdui;

public class Alert extends Screen {
    public static final int FOREVER = -2;
    public static final Command DISMISS_COMMAND = new Command("OK", Command.OK, 0);

    Displayable next;
    private String text;
    private Image image;
    private AlertType type;
    private int timeout = FOREVER;

    public Alert(String title) { this(title, null, null, null); }

    public Alert(String title, String alertText, Image alertImage, AlertType alertType) {
        setTitle(title);
        text = alertText; image = alertImage; type = alertType;
    }

    public int getDefaultTimeout() { return FOREVER; }
    public int getTimeout() { return timeout; }
    public void setTimeout(int time) { timeout = time; }
    public AlertType getType() { return type; }
    public void setType(AlertType t) { type = t; }
    public String getString() { return text; }
    public void setString(String s) { text = s; requestRepaint(); }
    public Image getImage() { return image; }
    public void setImage(Image img) { image = img; requestRepaint(); }

    void paintScreen(Graphics g) {
        g.reset(0, 0, Display.width, Display.height);
        g.setColor(0xFFFFFF);
        g.fillRect(0, 0, Display.width, Display.height);
        g.setColor(0);
        if (getTitle() != null) g.drawString(getTitle(), Display.width / 2, 4, Graphics.TOP | Graphics.HCENTER);
        if (text != null) g.drawString(text, 4, 30, Graphics.TOP | Graphics.LEFT);
    }

    void keyEvent(int kind, int keyCode) {
        if (kind == 0 && next != null) Display.instance.setCurrent(next);
    }
}
