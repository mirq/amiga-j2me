/*
 * amiga-j2me: MIDP 2.0 implementation for the Amiga J2ME runtime.
 * Written for this project; licensed under GPLv2 (see COPYING).
 */
package javax.microedition.lcdui;

import aj.Native;
import java.util.Vector;
import javax.microedition.midlet.MIDlet;

/**
 * The display, the input queue and the event thread's loop.
 *
 * Any thread may pump native input into the queue (GameCanvas does when it
 * reads key states or flushes); only the event thread (aj.Launcher) hands
 * events to the application.
 */
public class Display {
    public static final int LIST_ELEMENT = 1, CHOICE_GROUP_ELEMENT = 2, ALERT = 3;
    public static final int COLOR_BACKGROUND = 0, COLOR_FOREGROUND = 1, COLOR_HIGHLIGHTED_BACKGROUND = 2,
                            COLOR_HIGHLIGHTED_FOREGROUND = 3, COLOR_BORDER = 4, COLOR_HIGHLIGHTED_BORDER = 5;

    /** Guards the queue, the dirty region and the native decoder state. */
    static final Object LOCK = new Object();
    private static final Object PAINT_LOCK = new Object();

    static int width = 240, height = 320;
    static int SOFT_BAR = 18;        // set in open(): the small bold font's height + 4
    static Display instance;

    // HAL event types and raw key codes (hal.h)
    private static final int EV_KEY = 1, EV_POINTER = 2, EV_POINTER_MOVE = 3, EV_QUIT = 4;
    // queued event kinds
    private static final int Q_KEY_PRESSED = 0, Q_KEY_RELEASED = 1, Q_KEY_REPEATED = 2,
                             Q_PTR_PRESSED = 3, Q_PTR_RELEASED = 4, Q_PTR_DRAGGED = 5;

    Displayable current;
    private final Image screen;
    private final Graphics g;
    private boolean dirty;
    private int dx0, dy0, dx1, dy1;
    private final Vector serial = new Vector();
    private final int[] ev = new int[6];
    private final int[] queue = new int[3 * 128];
    private int qHead, qCount;
    private final boolean[] rawDown = new boolean[128];
    private boolean pointerDown;
    private int downBits, latchedBits;      // GameCanvas key states
    private volatile boolean quit;

    private Display() {
        screen = new Image(width, height, true);
        g = new Graphics(screen);
    }

    /** Called once by the launcher before the MIDlet is created. */
    public static void open(int w, int h) { open(w, h, 1); }

    public static void open(int w, int h, int scale) {
        width = w; height = h;
        SOFT_BAR = Font.getFont(Font.FACE_SYSTEM, Font.STYLE_BOLD, Font.SIZE_SMALL).getHeight() + 4;
        if (Native.videoOpen(w, h, scale) != 0) throw new RuntimeException("cannot open the display");
        instance = new Display();
    }

    public static void close() { Native.videoClose(); }

    public static Display getDisplay(MIDlet m) {
        if (m == null) throw new NullPointerException();
        return instance;
    }

    public Displayable getCurrent() { return current; }

    public void setCurrent(Displayable next) {
        if (next == null) return;       // "send to background": nothing to do here
        final Displayable old;
        synchronized (LOCK) {
            old = current;
            if (next instanceof Alert && ((Alert) next).next == null && old != next) ((Alert) next).next = old;
            if (old == next) { requestRepaint(0, 0, width, height); return; }
            current = next;
        }
        final Displayable shown = next;
        callSerially(new Runnable() {
            public void run() {
                if (old != null) { old.menuOpen = false; old.hideNotifyImpl(); }
                synchronized (PAINT_LOCK) {      // nothing of the old screen shows through
                    g.reset(0, 0, width, height);
                    g.setColor(0xFFFFFF);
                    g.fillRect(0, 0, width, height);
                }
                shown.showNotifyImpl();
                requestRepaint(0, 0, width, height);
            }
        });
    }

    public void setCurrent(Alert alert, Displayable nextDisplayable) {
        if (alert == null || nextDisplayable == null) throw new NullPointerException();
        alert.next = nextDisplayable;
        setCurrent(alert);
    }

    public void setCurrentItem(Item item) {}

    public void callSerially(Runnable r) {
        if (r != null) serial.addElement(r);
    }

    public boolean isColor() { return true; }
    public int numColors() { return 65536; }
    public int numAlphaLevels() { return 256; }
    public boolean vibrate(int duration) { return false; }
    public boolean flashBacklight(int duration) { return false; }
    public int getBestImageWidth(int imageType) { return 16; }
    public int getBestImageHeight(int imageType) { return 16; }
    public int getBorderStyle(boolean highlighted) { return Graphics.SOLID; }

    public int getColor(int colorSpecifier) {
        switch (colorSpecifier) {
        case COLOR_BACKGROUND: return 0xFFFFFF;
        case COLOR_FOREGROUND: return 0x000000;
        case COLOR_HIGHLIGHTED_BACKGROUND: return 0x2040A0;
        case COLOR_HIGHLIGHTED_FOREGROUND: return 0xFFFFFF;
        case COLOR_BORDER: return 0x808080;
        case COLOR_HIGHLIGHTED_BORDER: return 0x2040A0;
        default: throw new IllegalArgumentException();
        }
    }

    // ---- repaint ----

    void requestRepaint(int x, int y, int w, int h) {
        if (w <= 0 || h <= 0) return;
        synchronized (LOCK) {
            int x1 = x + w, y1 = y + h;
            if (!dirty) { dx0 = x; dy0 = y; dx1 = x1; dy1 = y1; dirty = true; }
            else {
                if (x < dx0) dx0 = x;
                if (y < dy0) dy0 = y;
                if (x1 > dx1) dx1 = x1;
                if (y1 > dy1) dy1 = y1;
            }
        }
    }

    /** Paints the pending region now, in the calling thread. */
    void serviceRepaints() {
        synchronized (PAINT_LOCK) {
            int x, y, w, h;
            Displayable d;
            synchronized (LOCK) {
                if (!dirty) return;
                dirty = false;
                x = dx0 < 0 ? 0 : dx0; y = dy0 < 0 ? 0 : dy0;
                w = (dx1 > width ? width : dx1) - x; h = (dy1 > height ? height : dy1) - y;
                d = current;
            }
            if (d == null || w <= 0 || h <= 0) return;
            g.pendingX = x; g.pendingY = y; g.pendingW = w; g.pendingH = h;
            try {
                d.paintScreen(g);
            } catch (Throwable t) {
                System.out.println("paint: " + t);
                t.printStackTrace();
            }
            // screens with a soft key bar or their own chrome redraw it every time
            if (!(d instanceof Canvas) || ((Canvas) d).softBarVisible()) { x = 0; y = 0; w = width; h = height; }
            Native.present(screen.pix, width, height, x, y, w, h);
        }
    }

    /** GameCanvas.flushGraphics: the canvas' own buffer goes straight to the display. */
    /** Draws an image straight onto the screen (Siemens ExtendedImage.blitToScreen). */
    void blit(Image img, int x, int y) {
        synchronized (PAINT_LOCK) {
            g.reset(0, 0, width, height);
            g.drawImage(img, x, y, Graphics.TOP | Graphics.LEFT);
            Native.present(screen.pix, width, height, x, y, img.w, img.h);
        }
    }

    void presentBuffer(Image buffer, int x, int y, int w, int h) {
        synchronized (PAINT_LOCK) {
            Native.present(buffer.pix, buffer.w, buffer.h, x, y, w, h);
        }
        pump();
        Native.audioUpdate();
    }

    void paintSoftBar(Graphics gr, Displayable d) {
        Font f = Font.getFont(Font.FACE_SYSTEM, Font.STYLE_BOLD, Font.SIZE_SMALL);
        int y = height - SOFT_BAR;
        gr.reset(0, y, width, SOFT_BAR);
        gr.setColor(0x303848);
        gr.fillRect(0, y, width, SOFT_BAR);
        gr.setColor(0xFFFFFF);
        gr.setFont(f);
        String l = d.softLabel(0), r = d.softLabel(1);
        int ty = y + (SOFT_BAR - f.getHeight()) / 2;
        if (l != null) gr.drawString(l, 3, ty, Graphics.TOP | Graphics.LEFT);
        if (r != null) gr.drawString(r, width - 3, ty, Graphics.TOP | Graphics.RIGHT);
        if (d.menuOpen) d.paintMenu(gr);
    }

    // ---- input ----

    /* ---- key mapping ----
     * Defaults: arrows, Return/Enter = fire, Space = 0, Ctrl = 5, F1/F2 = soft
     * keys, digit keys and the numeric pad = 0..9. Two arrows held together
     * (up+right and so on) send the phone's diagonal key instead (1, 3, 7, 9).
     * Per game (<game.jar>.cfg, or -D on the VM's command line):
     *   aj.keys=space:fire,ctrl:5,a:1      key:phone key, comma separated. Keys:
     *       space return enter ctrl lshift rshift lalt ralt lamiga ramiga tab
     *       del backspace up down left right f1..f10, a..z, or a raw code (0x40).
     *       Phone keys: 0..9 * # fire up down left right soft1 soft2 clear none
     *   aj.diagonals=1379                   up-left, up-right, down-left,
     *       down-right; "off" (or "none") sends the two arrows as they are. */
    private static final int[] KEYMAP = new int[128];
    private static int[] DIAG = { Canvas.KEY_NUM1, Canvas.KEY_NUM3, Canvas.KEY_NUM7, Canvas.KEY_NUM9 };
    private static final String KEYS = "qwertyuiop", KEYS2 = "asdfghjkl", KEYS3 = "zxcvbnm";
    static {
        for (int i = 0x01; i <= 0x09; i++) KEYMAP[i] = Canvas.KEY_NUM1 + i - 0x01;   // top row 1..9
        KEYMAP[0x0A] = Canvas.KEY_NUM0; KEYMAP[0x0F] = Canvas.KEY_NUM0;
        KEYMAP[0x1D] = Canvas.KEY_NUM1; KEYMAP[0x1E] = Canvas.KEY_NUM2; KEYMAP[0x1F] = Canvas.KEY_NUM3;
        KEYMAP[0x2D] = Canvas.KEY_NUM4; KEYMAP[0x2E] = Canvas.KEY_NUM5; KEYMAP[0x2F] = Canvas.KEY_NUM6;
        KEYMAP[0x3D] = Canvas.KEY_NUM7; KEYMAP[0x3E] = Canvas.KEY_NUM8; KEYMAP[0x3F] = Canvas.KEY_NUM9;
        KEYMAP[0x5D] = Canvas.KEY_STAR;                                     // numpad *
        KEYMAP[0x5C] = Canvas.KEY_POUND; KEYMAP[0x4A] = Canvas.KEY_POUND;   // numpad / and -
        KEYMAP[0x4C] = Canvas.KEY_UP; KEYMAP[0x4D] = Canvas.KEY_DOWN;
        KEYMAP[0x4F] = Canvas.KEY_LEFT; KEYMAP[0x4E] = Canvas.KEY_RIGHT;
        KEYMAP[0x44] = Canvas.KEY_FIRE; KEYMAP[0x43] = Canvas.KEY_FIRE;     // Return, Enter
        KEYMAP[0x40] = Canvas.KEY_NUM0;                                     // Space
        KEYMAP[0x63] = Canvas.KEY_NUM5;                                     // Ctrl
        KEYMAP[0x50] = Canvas.KEY_SOFT_LEFT; KEYMAP[0x51] = Canvas.KEY_SOFT_RIGHT;   // F1, F2
        KEYMAP[0x41] = Canvas.KEY_CLEAR; KEYMAP[0x46] = Canvas.KEY_CLEAR;   // Backspace, Del
        String k = null, d = null;
        try { k = System.getProperty("aj.keys"); d = System.getProperty("aj.diagonals"); } catch (Throwable t) { }
        if (k != null) parseKeys(k);
        if (d != null) {
            d = d.trim().toLowerCase();
            if (d.equals("off") || d.equals("none") || d.equals("0")) DIAG = null;
            else if (d.length() == 4) {
                int[] m = new int[4];
                for (int i = 0; i < 4; i++) m[i] = phoneKey(d.substring(i, i + 1));
                DIAG = m;
            } else System.out.println("aj.diagonals: four keys (like 1379) or off, not " + d);
        }
    }

    private static int amigaKey(String n) {
        if (n.startsWith("0x")) { try { return Integer.parseInt(n.substring(2), 16) & 127; } catch (Exception e) { return -1; } }
        if (n.length() == 1) {
            char c = n.charAt(0);
            int i;
            if (c >= '1' && c <= '9') return 0x01 + c - '1';
            if (c == '0') return 0x0A;
            if ((i = KEYS.indexOf(c)) >= 0) return 0x10 + i;
            if ((i = KEYS2.indexOf(c)) >= 0) return 0x20 + i;
            if ((i = KEYS3.indexOf(c)) >= 0) return 0x31 + i;
            return -1;
        }
        if (n.length() >= 2 && n.charAt(0) == 'f') {
            try { int f = Integer.parseInt(n.substring(1)); if (f >= 1 && f <= 10) return 0x50 + f - 1; } catch (Exception e) { }
        }
        String[] names = { "space", "return", "enter", "ctrl", "lshift", "rshift", "lalt", "ralt",
                           "lamiga", "ramiga", "tab", "del", "backspace", "up", "down", "left", "right", "esc" };
        int[] codes =    { 0x40, 0x44, 0x43, 0x63, 0x60, 0x61, 0x64, 0x65,
                           0x66, 0x67, 0x42, 0x46, 0x41, 0x4C, 0x4D, 0x4F, 0x4E, 0x45 };
        for (int i = 0; i < names.length; i++) if (names[i].equals(n)) return codes[i];
        return -1;
    }

    /** A phone key by name; -1: not one. 0 is "none". */
    private static int phoneKey(String n) {
        if (n.length() == 1) {
            char c = n.charAt(0);
            if (c >= '0' && c <= '9') return Canvas.KEY_NUM0 + c - '0';
            if (c == '*') return Canvas.KEY_STAR;
            if (c == '#') return Canvas.KEY_POUND;
        }
        String[] names = { "fire", "up", "down", "left", "right", "soft1", "soft2", "clear", "none" };
        int[] codes = { Canvas.KEY_FIRE, Canvas.KEY_UP, Canvas.KEY_DOWN, Canvas.KEY_LEFT, Canvas.KEY_RIGHT,
                        Canvas.KEY_SOFT_LEFT, Canvas.KEY_SOFT_RIGHT, Canvas.KEY_CLEAR, 0 };
        for (int i = 0; i < names.length; i++) if (names[i].equals(n)) return codes[i];
        return -1;
    }

    private static void parseKeys(String spec) {
        int at = 0;
        while (at < spec.length()) {
            int end = spec.indexOf(',', at);
            if (end < 0) end = spec.length();
            String item = spec.substring(at, end).trim().toLowerCase();
            at = end + 1;
            if (item.length() == 0) continue;
            int colon = item.indexOf(':');
            int from = colon > 0 ? amigaKey(item.substring(0, colon).trim()) : -1;
            int to = colon > 0 ? phoneKey(item.substring(colon + 1).trim()) : -1;
            if (from < 0 || from == 0x45 || to == -1) { System.out.println("aj.keys: cannot use \"" + item + "\""); continue; }
            KEYMAP[from] = to;
        }
    }

    /** Raw Amiga key code (+ the keymap's character) to a MIDP key code; 0 = none. */
    private static int mapKey(int raw, int ch) {
        if (KEYMAP[raw] != 0) return KEYMAP[raw];
        if (raw == 0x40) return 0;          // Space mapped to none: no character either
        if (ch == '*') return Canvas.KEY_STAR;
        if (ch == '#') return Canvas.KEY_POUND;
        return ch > 32 && ch < 256 ? ch : 0;
    }

    /* Arrows held now (bit per direction, by how many keys hold it) and the
     * direction keys the game has been told are down. */
    private final int[] dirHeld = new int[4];       // up, down, left, right
    private int sentDirs;                           // bits 0-3 arrows, 4-7 diagonals 1 3 7 9

    private static int dirIndex(int key) {
        switch (key) {
        case Canvas.KEY_UP: return 0;
        case Canvas.KEY_DOWN: return 1;
        case Canvas.KEY_LEFT: return 2;
        case Canvas.KEY_RIGHT: return 3;
        default: return -1;
        }
    }

    private static int sentKey(int bit) {
        switch (bit) {
        case 0: return Canvas.KEY_UP;
        case 1: return Canvas.KEY_DOWN;
        case 2: return Canvas.KEY_LEFT;
        case 3: return Canvas.KEY_RIGHT;
        default: return DIAG[bit - 4];
        }
    }

    /** What the game should see for the arrows held now: one diagonal key, or the arrows. */
    private int wantedDirs() {
        boolean u = dirHeld[0] > 0, d = dirHeld[1] > 0, l = dirHeld[2] > 0, r = dirHeld[3] > 0;
        if (DIAG != null && (u ^ d) && (l ^ r)) return 1 << (4 + (u ? 0 : 2) + (l ? 0 : 1));
        return (u ? 1 : 0) | (d ? 2 : 0) | (l ? 4 : 0) | (r ? 8 : 0);
    }

    /** Tells the game about the change from sentDirs to what is wanted now;
     *  a repeat of a held arrow repeats whatever is being sent. */
    private void updateDirs(boolean repeat) {
        int want = wantedDirs();
        for (int b = 0; b < 8; b++)
            if ((sentDirs & ~want & (1 << b)) != 0 && sentKey(b) != 0) enqueue(Q_KEY_RELEASED, sentKey(b), 0);
        for (int b = 0; b < 8; b++) {
            if ((want & (1 << b)) == 0 || sentKey(b) == 0) continue;
            if ((sentDirs & (1 << b)) == 0) enqueue(Q_KEY_PRESSED, sentKey(b), 0);
            else if (repeat) enqueue(Q_KEY_REPEATED, sentKey(b), 0);
        }
        sentDirs = want;
    }

    /** The character a key types in a text field: printable, or 8 (backspace), 10 (return). */
    private static int typed(int raw, int ch) {
        if (raw == 0x41) return 8;
        if (raw == 0x44 || raw == 0x43) return 10;
        return ch >= 32 && ch < 256 && ch != 127 ? ch : 0;
    }

    private void enqueue(int kind, int a, int b) {
        if (qCount == queue.length / 3) return;     // full: drop
        int i = ((qHead + qCount) % (queue.length / 3)) * 3;
        queue[i] = kind; queue[i + 1] = a; queue[i + 2] = b;
        qCount++;
    }

    /** Moves pending native input into the queue. Any thread. */
    void pump() {
        synchronized (LOCK) {
            while (Native.pollEvent(ev) != 0) {
                switch (ev[0]) {
                case EV_KEY: {
                    int raw = ev[1] & 127;
                    boolean down = ev[2] != 0;
                    if (raw == 0x45) { if (down) quit = true; break; }    // Esc leaves the MIDlet
                    int key = mapKey(raw, ev[5]);
                    if (key == 0 && typed(raw, ev[5]) == 0) break;
                    int bit = gameBit(key);
                    int dir = dirIndex(key);
                    if (dir >= 0) {
                        // arrows go through the diagonal logic; getKeyStates sees the arrows
                        if (down) {
                            boolean repeat = rawDown[raw];
                            if (!repeat) dirHeld[dir]++;
                            rawDown[raw] = true;
                            downBits |= bit; latchedBits |= bit;
                            updateDirs(repeat);
                        } else if (rawDown[raw]) {
                            rawDown[raw] = false;
                            if (--dirHeld[dir] == 0) downBits &= ~bit;
                            updateDirs(false);
                        }
                        break;
                    }
                    if (down) {
                        enqueue(rawDown[raw] ? Q_KEY_REPEATED : Q_KEY_PRESSED, key, typed(raw, ev[5]));
                        rawDown[raw] = true;
                        downBits |= bit; latchedBits |= bit;
                    } else {
                        rawDown[raw] = false;
                        downBits &= ~bit;
                        enqueue(Q_KEY_RELEASED, key, 0);
                    }
                    break;
                }
                case EV_POINTER:
                    if (ev[1] != 0) break;      // left button only
                    pointerDown = ev[2] != 0;
                    enqueue(pointerDown ? Q_PTR_PRESSED : Q_PTR_RELEASED, ev[3], ev[4]);
                    break;
                case EV_POINTER_MOVE:
                    if (pointerDown) enqueue(Q_PTR_DRAGGED, ev[3], ev[4]);
                    break;
                case EV_QUIT:
                    quit = true;
                    break;
                default:
                    break;
                }
            }
        }
    }

    private static int gameBit(int key) {
        switch (key) {
        case Canvas.KEY_UP: case Canvas.KEY_NUM2: return 1 << Canvas.UP;
        case Canvas.KEY_DOWN: case Canvas.KEY_NUM8: return 1 << Canvas.DOWN;
        case Canvas.KEY_LEFT: case Canvas.KEY_NUM4: return 1 << Canvas.LEFT;
        case Canvas.KEY_RIGHT: case Canvas.KEY_NUM6: return 1 << Canvas.RIGHT;
        case Canvas.KEY_FIRE: case Canvas.KEY_NUM5: return 1 << Canvas.FIRE;
        case Canvas.KEY_NUM1: return 1 << Canvas.GAME_A;
        case Canvas.KEY_NUM3: return 1 << Canvas.GAME_B;
        case Canvas.KEY_NUM7: return 1 << Canvas.GAME_C;
        case Canvas.KEY_NUM9: return 1 << Canvas.GAME_D;
        default: return 0;
        }
    }

    /** GameCanvas.getKeyStates: keys down now or pressed since the last call. */
    int keyStates() {
        pump();
        synchronized (LOCK) {
            int s = downBits | latchedBits;
            latchedBits = 0;
            return s;
        }
    }

    public void requestQuit() { quit = true; }
    public boolean quitRequested() { return quit; }

    /** One round of the event thread. Returns true when it did something. */
    public boolean runOnce() {
        boolean busy = false;
        pump();
        Native.audioUpdate();
        aj.media.BasicPlayer.pollAll();
        for (;;) {
            int kind, a, b;
            Displayable d;
            synchronized (LOCK) {
                if (qCount == 0) break;
                int i = qHead * 3;
                kind = queue[i]; a = queue[i + 1]; b = queue[i + 2];
                qHead = (qHead + 1) % (queue.length / 3);
                qCount--;
                d = current;
            }
            busy = true;
            if (d == null) continue;
            try {
                if (kind <= Q_KEY_REPEATED) d.keyEvent(kind, a, b);
                else d.pointerEvent(kind - Q_PTR_PRESSED, a, b);
            } catch (Throwable t) {
                System.out.println("event: " + t);
                t.printStackTrace();
            }
        }
        while (serial.size() > 0) {
            Runnable r = (Runnable) serial.elementAt(0);
            serial.removeElementAt(0);
            busy = true;
            try {
                r.run();
            } catch (Throwable t) {
                System.out.println("callSerially: " + t);
                t.printStackTrace();
            }
        }
        if (dirty) { serviceRepaints(); busy = true; }
        return busy;
    }
}
