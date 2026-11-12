/*
 * amiga-j2me: MIDP 2.0 implementation for the Amiga J2ME runtime.
 * Written for this project; licensed under GPLv2 (see COPYING).
 */
package javax.microedition.lcdui;

public class Gauge extends Item {
    public static final int INDEFINITE = -1, CONTINUOUS_IDLE = 0, INCREMENTAL_IDLE = 1,
                            CONTINUOUS_RUNNING = 2, INCREMENTAL_UPDATING = 3;
    private final boolean interactive;
    private int max, value;

    public Gauge(String label, boolean interactive, int maxValue, int initialValue) {
        this.label = label; this.interactive = interactive;
        setMaxValue(maxValue);
        setValue(initialValue);
    }

    public void setValue(int v) {
        if (max != INDEFINITE) value = v < 0 ? 0 : (v > max ? max : v);
        else value = v;
        changed();
    }
    public int getValue() { return value; }
    public void setMaxValue(int m) {
        if (m <= 0 && !(m == INDEFINITE && !interactive)) throw new IllegalArgumentException();
        max = m;
        if (max != INDEFINITE && value > max) value = max;
        changed();
    }
    public int getMaxValue() { return max; }
    public boolean isInteractive() { return interactive; }

    int contentHeight(int w) { return 12; }

    void paintContent(Graphics g, int x, int y, int w, boolean focused) {
        g.setColor(focused ? Screen.HI_BG : 0x808080);
        g.drawRect(x, y + 1, w - 1, 9);
        int fill = max > 0 ? (w - 4) * value / max : (w - 4) / 2;
        g.setColor(Screen.HI_BG);
        g.fillRect(x + 2, y + 3, fill, 6);
    }

    boolean focusable() { return interactive || super.focusable(); }

    boolean key(int type, int keyCode, int ch) {
        if (interactive && type != 1 && (keyCode == Canvas.KEY_LEFT || keyCode == Canvas.KEY_RIGHT)) {
            int v = value + (keyCode == Canvas.KEY_LEFT ? -1 : 1);
            if (v >= 0 && v <= max) { value = v; notifyStateChanged(); }
            return true;
        }
        return super.key(type, keyCode, ch);
    }
}
