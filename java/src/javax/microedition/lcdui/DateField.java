/*
 * amiga-j2me: MIDP 2.0 implementation for the Amiga J2ME runtime.
 * Written for this project; licensed under GPLv2 (see COPYING).
 */
package javax.microedition.lcdui;

import java.util.Calendar;
import java.util.Date;
import java.util.TimeZone;

/** Shows its date; editing dates is not implemented. */
public class DateField extends Item {
    public static final int DATE = 1, TIME = 2, DATE_TIME = 3;
    private Date date;
    private int mode;

    public DateField(String label, int mode) { this(label, mode, null); }
    public DateField(String label, int mode, TimeZone timeZone) {
        if (mode < DATE || mode > DATE_TIME) throw new IllegalArgumentException();
        this.label = label; this.mode = mode;
    }

    public Date getDate() { return date; }
    public void setDate(Date d) { date = d; changed(); }
    public int getInputMode() { return mode; }
    public void setInputMode(int m) { mode = m; changed(); }

    private static String two(int v) { return v < 10 ? "0" + v : String.valueOf(v); }

    int contentHeight(int w) { return Screen.TEXT_FONT.getHeight() + 2; }

    void paintContent(Graphics g, int x, int y, int w, boolean focused) {
        String s = "--";
        if (date != null) {
            Calendar c = Calendar.getInstance();
            c.setTime(date);
            String d = c.get(Calendar.YEAR) + "-" + two(c.get(Calendar.MONTH) + 1) + "-" + two(c.get(Calendar.DAY_OF_MONTH));
            String t = two(c.get(Calendar.HOUR_OF_DAY)) + ":" + two(c.get(Calendar.MINUTE));
            s = mode == DATE ? d : (mode == TIME ? t : d + " " + t);
        }
        g.setColor(Screen.FG);
        g.setFont(Screen.TEXT_FONT);
        g.drawString(s, x, y + 1, Graphics.TOP | Graphics.LEFT);
    }
}
