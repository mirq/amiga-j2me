/*
 * amiga-j2me: MIDP 2.0 implementation for the Amiga J2ME runtime.
 * Written for this project; licensed under GPLv2 (see COPYING).
 */
package javax.microedition.lcdui;

public abstract class Canvas extends Displayable {
    public static final int UP = 1, DOWN = 6, LEFT = 2, RIGHT = 5, FIRE = 8;
    public static final int GAME_A = 9, GAME_B = 10, GAME_C = 11, GAME_D = 12;
    public static final int KEY_NUM0 = 48, KEY_NUM1 = 49, KEY_NUM2 = 50, KEY_NUM3 = 51, KEY_NUM4 = 52,
                            KEY_NUM5 = 53, KEY_NUM6 = 54, KEY_NUM7 = 55, KEY_NUM8 = 56, KEY_NUM9 = 57;
    public static final int KEY_STAR = 42, KEY_POUND = 35;

    // Key codes of the non-ITU keys, as on Nokia phones (what most games test for)
    static final int KEY_UP = -1, KEY_DOWN = -2, KEY_LEFT = -3, KEY_RIGHT = -4, KEY_FIRE = -5,
                     KEY_SOFT_LEFT = -6, KEY_SOFT_RIGHT = -7, KEY_CLEAR = -8;

    boolean fullScreen;

    protected Canvas() {}

    protected abstract void paint(Graphics g);

    public boolean isDoubleBuffered() { return true; }
    public boolean hasPointerEvents() { return true; }
    public boolean hasPointerMotionEvents() { return true; }
    public boolean hasRepeatEvents() { return true; }

    public int getWidth() { return Display.width; }
    public int getHeight() { return Display.height - (softBarVisible() ? Display.SOFT_BAR : 0); }

    public void setFullScreenMode(boolean mode) {
        if (fullScreen != mode) {
            fullScreen = mode;
            sizeChanged(getWidth(), getHeight());
            repaint();
        }
    }

    boolean softBarVisible() { return !fullScreen && commands.size() > 0; }

    public int getKeyCode(int gameAction) {
        switch (gameAction) {
        case UP: return KEY_UP;
        case DOWN: return KEY_DOWN;
        case LEFT: return KEY_LEFT;
        case RIGHT: return KEY_RIGHT;
        case FIRE: return KEY_FIRE;
        case GAME_A: return KEY_NUM1;
        case GAME_B: return KEY_NUM3;
        case GAME_C: return KEY_NUM7;
        case GAME_D: return KEY_NUM9;
        default: throw new IllegalArgumentException();
        }
    }

    public int getGameAction(int keyCode) {
        switch (keyCode) {
        case KEY_UP: case KEY_NUM2: return UP;
        case KEY_DOWN: case KEY_NUM8: return DOWN;
        case KEY_LEFT: case KEY_NUM4: return LEFT;
        case KEY_RIGHT: case KEY_NUM6: return RIGHT;
        case KEY_FIRE: case KEY_NUM5: return FIRE;
        case KEY_NUM1: return GAME_A;
        case KEY_NUM3: return GAME_B;
        case KEY_NUM7: return GAME_C;
        case KEY_NUM9: return GAME_D;
        default: return 0;
        }
    }

    public String getKeyName(int keyCode) {
        switch (keyCode) {
        case KEY_UP: return "UP";
        case KEY_DOWN: return "DOWN";
        case KEY_LEFT: return "LEFT";
        case KEY_RIGHT: return "RIGHT";
        case KEY_FIRE: return "SELECT";
        case KEY_SOFT_LEFT: return "SOFT1";
        case KEY_SOFT_RIGHT: return "SOFT2";
        case KEY_CLEAR: return "CLEAR";
        default:
            if (keyCode > 32 && keyCode < 0xFFFF) return String.valueOf((char) keyCode);
            throw new IllegalArgumentException();
        }
    }

    protected void keyPressed(int keyCode) {}
    protected void keyRepeated(int keyCode) {}
    protected void keyReleased(int keyCode) {}
    protected void pointerPressed(int x, int y) {}
    protected void pointerReleased(int x, int y) {}
    protected void pointerDragged(int x, int y) {}
    protected void showNotify() {}
    protected void hideNotify() {}
    protected void sizeChanged(int w, int h) {}

    public final void repaint() { repaint(0, 0, getWidth(), getHeight()); }

    public final void repaint(int x, int y, int width, int height) {
        Display d = Display.instance;
        if (d != null && d.current == this) d.requestRepaint(x, y, width, height);
    }

    public final void serviceRepaints() {
        Display d = Display.instance;
        if (d != null && d.current == this) d.serviceRepaints();
    }

    // ---- event thread hooks ----
    void showNotifyImpl() { showNotify(); }
    void hideNotifyImpl() { hideNotify(); }

    void paintScreen(Graphics g) {
        int h = getHeight();
        g.reset(g.pendingX, g.pendingY, g.pendingW, g.pendingH);
        g.clipRect(0, 0, getWidth(), h);
        paint(g);
        if (softBarVisible()) Display.instance.paintSoftBar(g, this);
    }

    void keyEvent(int type, int keyCode) {
        if (!fullScreen && (keyCode == KEY_SOFT_LEFT || keyCode == KEY_SOFT_RIGHT) && commands.size() > 0) {
            if (type == 0) fireCommand(softCommand(keyCode == KEY_SOFT_LEFT ? 0 : 1));
            return;
        }
        switch (type) {
        case 0: keyPressed(keyCode); break;
        case 1: keyReleased(keyCode); break;
        default: keyRepeated(keyCode); break;
        }
    }

    void pointerEvent(int type, int x, int y) {
        switch (type) {
        case 0: pointerPressed(x, y); break;
        case 1: pointerReleased(x, y); break;
        default: pointerDragged(x, y); break;
        }
    }
}
