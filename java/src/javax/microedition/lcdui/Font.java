/*
 * amiga-j2me: MIDP 2.0 implementation for the Amiga J2ME runtime.
 * Written for this project; licensed under GPLv2 (see COPYING).
 */
package javax.microedition.lcdui;

import aj.Native;

public final class Font {
    public static final int STYLE_PLAIN = 0, STYLE_BOLD = 1, STYLE_ITALIC = 2, STYLE_UNDERLINED = 4;
    public static final int SIZE_SMALL = 8, SIZE_MEDIUM = 0, SIZE_LARGE = 16;
    public static final int FACE_SYSTEM = 0, FACE_MONOSPACE = 32, FACE_PROPORTIONAL = 64;
    public static final int FONT_STATIC_TEXT = 0, FONT_INPUT_TEXT = 1;

    private static final Font[] cache = new Font[6 * 8];
    private final int face, style, size;
    final int index;            // native font: size (0..4) * 2 + bold
    private final int height, ascent;

    private Font(int face, int style, int size) {
        this.face = face; this.style = style; this.size = size;
        // Five native sizes (7, 8, 9, 11, 14 px); small screens get the smaller three
        int tier = Display.height < 110 ? 0 : (Display.height < 200 ? 1 : 2);
        int s = tier + (size == SIZE_SMALL ? 0 : (size == SIZE_LARGE ? 2 : 1));
        index = s * 2 + ((style & STYLE_BOLD) != 0 ? 1 : 0);
        int info = Native.fontInfo(index);
        height = info >> 8; ascent = info & 0xff;
    }

    public static Font getDefaultFont() { return getFont(FACE_SYSTEM, STYLE_PLAIN, SIZE_MEDIUM); }
    public static Font getFont(int fontSpecifier) { return getDefaultFont(); }

    public static Font getFont(int face, int style, int size) {
        if ((face != FACE_SYSTEM && face != FACE_MONOSPACE && face != FACE_PROPORTIONAL)
            || (style & ~7) != 0
            || (size != SIZE_SMALL && size != SIZE_MEDIUM && size != SIZE_LARGE)) {
            throw new IllegalArgumentException();
        }
        int s = size == SIZE_SMALL ? 0 : (size == SIZE_LARGE ? 2 : 1);
        int slot = s * 16 + (style & 7) * 2 + (face == FACE_MONOSPACE ? 1 : 0);
        synchronized (cache) {
            Font f = slot < cache.length ? cache[slot] : null;
            if (f == null || f.face != face) {
                f = new Font(face, style, size);
                if (slot < cache.length) cache[slot] = f;
            }
            return f;
        }
    }

    public int getStyle() { return style; }
    public int getSize() { return size; }
    public int getFace() { return face; }
    public boolean isPlain() { return style == STYLE_PLAIN; }
    public boolean isBold() { return (style & STYLE_BOLD) != 0; }
    public boolean isItalic() { return (style & STYLE_ITALIC) != 0; }
    public boolean isUnderlined() { return (style & STYLE_UNDERLINED) != 0; }
    public int getHeight() { return height; }
    public int getBaselinePosition() { return ascent; }

    public int charWidth(char ch) {
        return Native.stringWidth(index, String.valueOf(ch), 0, 1);
    }
    public int charsWidth(char[] ch, int offset, int length) {
        if (offset < 0 || length < 0 || offset + length > ch.length) throw new ArrayIndexOutOfBoundsException();
        return Native.charsWidth(index, ch, offset, length);
    }
    public int stringWidth(String str) { return Native.stringWidth(index, str, 0, str.length()); }
    public int substringWidth(String str, int offset, int len) {
        if (offset < 0 || len < 0 || offset + len > str.length()) throw new StringIndexOutOfBoundsException();
        return Native.stringWidth(index, str, offset, len);
    }
}
