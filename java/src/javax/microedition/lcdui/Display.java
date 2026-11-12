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
    static final int SOFT_BAR = 18;
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
    public static void open(int w, int h) {
        width = w; height = h;
        if (Native.videoOpen(w, h) != 0) throw new RuntimeException("cannot open the display");
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
    void presentBuffer(Image buffer, int x, int y, int w, int h) {
        synchronized (PAINT_LOCK) {
            Native.present(buffer.pix, buffer.w, buffer.h, x, y, w, h);
        }
        pump();
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

    /** Raw Amiga key code (+ the keymap's character) to a MIDP key code; 0 = none. */
    private static int mapKey(int raw, int ch) {
        if (raw >= 0x01 && raw <= 0x09) return Canvas.KEY_NUM1 + raw - 0x01;   // top row 1..9
        switch (raw) {
        case 0x0A: case 0x0F: return Canvas.KEY_NUM0;
        case 0x1D: return Canvas.KEY_NUM1;
        case 0x1E: return Canvas.KEY_NUM2;
        case 0x1F: return Canvas.KEY_NUM3;
        case 0x2D: return Canvas.KEY_NUM4;
        case 0x2E: return Canvas.KEY_NUM5;
        case 0x2F: return Canvas.KEY_NUM6;
        case 0x3D: return Canvas.KEY_NUM7;
        case 0x3E: return Canvas.KEY_NUM8;
        case 0x3F: return Canvas.KEY_NUM9;
        case 0x5D: return Canvas.KEY_STAR;      // numpad *
        case 0x5C: case 0x4A: return Canvas.KEY_POUND;  // numpad / and -
        case 0x4C: return Canvas.KEY_UP;
        case 0x4D: return Canvas.KEY_DOWN;
        case 0x4F: return Canvas.KEY_LEFT;
        case 0x4E: return Canvas.KEY_RIGHT;
        case 0x44: case 0x43: case 0x40: return Canvas.KEY_FIRE;   // Return, Enter, Space
        case 0x50: return Canvas.KEY_SOFT_LEFT;     // F1
        case 0x51: return Canvas.KEY_SOFT_RIGHT;    // F2
        case 0x41: case 0x46: return Canvas.KEY_CLEAR;  // Backspace, Del
        default: break;
        }
        if (ch == '*') return Canvas.KEY_STAR;
        if (ch == '#') return Canvas.KEY_POUND;
        return ch > 32 && ch < 256 ? ch : 0;
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
