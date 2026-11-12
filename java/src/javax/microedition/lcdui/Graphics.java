/*
 * amiga-j2me: MIDP 2.0 implementation for the Amiga J2ME runtime.
 * Written for this project; licensed under GPLv2 (see COPYING).
 */
package javax.microedition.lcdui;

import aj.Native;

public class Graphics {
    public static final int HCENTER = 1, VCENTER = 2, LEFT = 4, RIGHT = 8, TOP = 16, BOTTOM = 32, BASELINE = 64;
    public static final int SOLID = 0, DOTTED = 1;

    private final short[] pix;
    private final int sw, sh;       // surface size
    private int tx, ty;             // translation
    private int cx, cy, cw, ch;     // clip, surface coordinates
    private int color;              // 0x00RRGGBB
    private int stroke = SOLID;
    private Font font = Font.getDefaultFont();
    /** The area the event thread is repainting (set by Display). */
    int pendingX, pendingY, pendingW, pendingH;

    Graphics(Image target) {
        pix = target.pix; sw = target.w; sh = target.h;
        cw = sw; ch = sh;
    }

    /** Back to the state a fresh Graphics has, with the given clip (for repaints). */
    void reset(int x, int y, int w, int h) {
        tx = 0; ty = 0; color = 0; stroke = SOLID; font = Font.getDefaultFont();
        cx = 0; cy = 0; cw = sw; ch = sh;
        clipRect(x, y, w, h);
    }

    public void translate(int x, int y) { tx += x; ty += y; }
    public int getTranslateX() { return tx; }
    public int getTranslateY() { return ty; }

    public int getColor() { return color; }
    public int getRedComponent() { return (color >> 16) & 0xff; }
    public int getGreenComponent() { return (color >> 8) & 0xff; }
    public int getBlueComponent() { return color & 0xff; }
    public int getGrayScale() { return (getRedComponent() * 77 + getGreenComponent() * 150 + getBlueComponent() * 29) >> 8; }
    public void setColor(int RGB) { color = RGB & 0xffffff; }
    public void setColor(int red, int green, int blue) {
        if (((red | green | blue) & ~0xff) != 0) throw new IllegalArgumentException();
        color = (red << 16) | (green << 8) | blue;
    }
    public void setGrayScale(int value) {
        if ((value & ~0xff) != 0) throw new IllegalArgumentException();
        color = (value << 16) | (value << 8) | value;
    }
    public int getDisplayColor(int c) {
        return (c & 0xf80000) | ((c >> 5) & 0x070000) | (c & 0x00fc00) | ((c >> 6) & 0x000300)
             | (c & 0x0000f8) | ((c >> 5) & 0x000007);
    }

    public Font getFont() { return font; }
    public void setFont(Font f) { font = f == null ? Font.getDefaultFont() : f; }
    public int getStrokeStyle() { return stroke; }
    public void setStrokeStyle(int style) {
        if (style != SOLID && style != DOTTED) throw new IllegalArgumentException();
        stroke = style;
    }

    public int getClipX() { return cx - tx; }
    public int getClipY() { return cy - ty; }
    public int getClipWidth() { return cw; }
    public int getClipHeight() { return ch; }

    public void setClip(int x, int y, int width, int height) {
        int x0 = x + tx, y0 = y + ty, x1 = x0 + width, y1 = y0 + height;
        if (x0 < 0) x0 = 0;
        if (y0 < 0) y0 = 0;
        if (x1 > sw) x1 = sw;
        if (y1 > sh) y1 = sh;
        cx = x0; cy = y0;
        cw = x1 > x0 ? x1 - x0 : 0;
        ch = y1 > y0 ? y1 - y0 : 0;
    }

    public void clipRect(int x, int y, int width, int height) {
        int x0 = x + tx, y0 = y + ty, x1 = x0 + width, y1 = y0 + height;
        if (x0 < cx) x0 = cx;
        if (y0 < cy) y0 = cy;
        if (x1 > cx + cw) x1 = cx + cw;
        if (y1 > cy + ch) y1 = cy + ch;
        cx = x0; cy = y0;
        cw = x1 > x0 ? x1 - x0 : 0;
        ch = y1 > y0 ? y1 - y0 : 0;
    }

    public void drawLine(int x1, int y1, int x2, int y2) {
        Native.drawLine(pix, sw, sh, cx, cy, cw, ch, color, x1 + tx, y1 + ty, x2 + tx, y2 + ty, stroke);
    }

    public void fillRect(int x, int y, int width, int height) {
        Native.fillRect(pix, sw, sh, cx, cy, cw, ch, color, x + tx, y + ty, width, height);
    }

    public void drawRect(int x, int y, int width, int height) {
        if (width < 0 || height < 0) return;
        int x0 = x + tx, y0 = y + ty, x1 = x0 + width, y1 = y0 + height;
        Native.drawLine(pix, sw, sh, cx, cy, cw, ch, color, x0, y0, x1, y0, stroke);
        Native.drawLine(pix, sw, sh, cx, cy, cw, ch, color, x0, y1, x1, y1, stroke);
        Native.drawLine(pix, sw, sh, cx, cy, cw, ch, color, x0, y0, x0, y1, stroke);
        Native.drawLine(pix, sw, sh, cx, cy, cw, ch, color, x1, y0, x1, y1, stroke);
    }

    public void drawRoundRect(int x, int y, int width, int height, int arcWidth, int arcHeight) {
        if (width < 0 || height < 0) return;
        int aw = Math.min(Math.max(arcWidth, 0), width), ah = Math.min(Math.max(arcHeight, 0), height);
        if (aw < 2 || ah < 2) { drawRect(x, y, width, height); return; }
        int x0 = x + tx, y0 = y + ty, x1 = x0 + width, y1 = y0 + height, rx = aw / 2, ry = ah / 2;
        Native.drawLine(pix, sw, sh, cx, cy, cw, ch, color, x0 + rx, y0, x1 - rx, y0, stroke);
        Native.drawLine(pix, sw, sh, cx, cy, cw, ch, color, x0 + rx, y1, x1 - rx, y1, stroke);
        Native.drawLine(pix, sw, sh, cx, cy, cw, ch, color, x0, y0 + ry, x0, y1 - ry, stroke);
        Native.drawLine(pix, sw, sh, cx, cy, cw, ch, color, x1, y0 + ry, x1, y1 - ry, stroke);
        Native.arc(pix, sw, sh, cx, cy, cw, ch, color, x0, y0, aw, ah, 90, 90, 0, stroke);
        Native.arc(pix, sw, sh, cx, cy, cw, ch, color, x1 - aw, y0, aw, ah, 0, 90, 0, stroke);
        Native.arc(pix, sw, sh, cx, cy, cw, ch, color, x0, y1 - ah, aw, ah, 180, 90, 0, stroke);
        Native.arc(pix, sw, sh, cx, cy, cw, ch, color, x1 - aw, y1 - ah, aw, ah, 270, 90, 0, stroke);
    }

    public void fillRoundRect(int x, int y, int width, int height, int arcWidth, int arcHeight) {
        if (width <= 0 || height <= 0) return;
        int aw = Math.min(Math.max(arcWidth, 0), width), ah = Math.min(Math.max(arcHeight, 0), height);
        if (aw < 2 || ah < 2) { fillRect(x, y, width, height); return; }
        int x0 = x + tx, y0 = y + ty, rx = aw / 2, ry = ah / 2;
        Native.fillRect(pix, sw, sh, cx, cy, cw, ch, color, x0 + rx, y0, width - 2 * rx, height);
        Native.fillRect(pix, sw, sh, cx, cy, cw, ch, color, x0, y0 + ry, rx, height - 2 * ry);
        Native.fillRect(pix, sw, sh, cx, cy, cw, ch, color, x0 + width - rx, y0 + ry, rx, height - 2 * ry);
        Native.arc(pix, sw, sh, cx, cy, cw, ch, color, x0, y0, aw, ah, 90, 90, 1, 0);
        Native.arc(pix, sw, sh, cx, cy, cw, ch, color, x0 + width - aw, y0, aw, ah, 0, 90, 1, 0);
        Native.arc(pix, sw, sh, cx, cy, cw, ch, color, x0, y0 + height - ah, aw, ah, 180, 90, 1, 0);
        Native.arc(pix, sw, sh, cx, cy, cw, ch, color, x0 + width - aw, y0 + height - ah, aw, ah, 270, 90, 1, 0);
    }

    public void drawArc(int x, int y, int width, int height, int startAngle, int arcAngle) {
        Native.arc(pix, sw, sh, cx, cy, cw, ch, color, x + tx, y + ty, width, height, startAngle, arcAngle, 0, stroke);
    }

    public void fillArc(int x, int y, int width, int height, int startAngle, int arcAngle) {
        Native.arc(pix, sw, sh, cx, cy, cw, ch, color, x + tx, y + ty, width, height, startAngle, arcAngle, 1, 0);
    }

    public void fillTriangle(int x1, int y1, int x2, int y2, int x3, int y3) {
        Native.fillTriangle(pix, sw, sh, cx, cy, cw, ch, color, x1 + tx, y1 + ty, x2 + tx, y2 + ty, x3 + tx, y3 + ty);
    }

    void readRGB(int[] argb, int offset, int scanlength, int x, int y, int w, int h) {
        int sx = x + tx, sy = y + ty;
        if (w <= 0 || h <= 0) return;
        if (sx < 0 || sy < 0 || sx + w > sw || sy + h > sh) throw new IllegalArgumentException();
        Native.toRGB(pix, null, sw, argb, offset, scanlength, sx, sy, w, h);
    }

    /** TiledLayer.paint: all visible cells in one native call. */
    void drawTiles(Image src, int tileW, int tileH, int tilesPerRow, int[] cells, int columns,
                   int c0, int r0, int c1, int r1, int x, int y, int[] animated) {
        Native.drawTiles(pix, sw, sh, cx, cy, cw, ch, src.pix, src.alpha, src.w, src.h, tileW, tileH,
                         tilesPerRow, cells, columns, c0, r0, c1, r1, x + tx, y + ty, animated);
    }

    short[] targetPixels() { return pix; }

    void targetInfo(int[] out) {
        out[0] = sw; out[1] = sh; out[2] = cx; out[3] = cy; out[4] = cw; out[5] = ch; out[6] = tx; out[7] = ty;
    }

    // ---- text ----

    private int anchorX(int x, int width, int anchor) {
        if ((anchor & RIGHT) != 0) return x - width;
        if ((anchor & HCENTER) != 0) return x - width / 2;
        return x;
    }

    private int textY(int y, int anchor) {
        if ((anchor & BOTTOM) != 0) return y - font.getHeight();
        if ((anchor & BASELINE) != 0) return y - font.getBaselinePosition();
        return y;
    }

    private static void checkTextAnchor(int anchor) {
        if ((anchor & ~(HCENTER | LEFT | RIGHT | TOP | BOTTOM | BASELINE)) != 0) throw new IllegalArgumentException();
    }

    private void underline(int x, int y, int width) {
        if (font.isUnderlined() && width > 0) {
            int uy = y + font.getBaselinePosition() + 1;
            Native.fillRect(pix, sw, sh, cx, cy, cw, ch, color, x, uy, width, 1);
        }
    }

    public void drawString(String str, int x, int y, int anchor) {
        drawSubstring(str, 0, str.length(), x, y, anchor);
    }

    public void drawSubstring(String str, int offset, int len, int x, int y, int anchor) {
        if (offset < 0 || len < 0 || offset + len > str.length()) throw new StringIndexOutOfBoundsException();
        checkTextAnchor(anchor);
        int px = x + tx, py = textY(y + ty, anchor);
        if ((anchor & (RIGHT | HCENTER)) != 0 || font.isUnderlined()) {
            int width = Native.stringWidth(font.index, str, offset, len);
            px = anchorX(px, width, anchor);
            underline(px, py, width);
        }
        if (py >= cy + ch || py + font.getHeight() <= cy) return;
        Native.drawString(pix, sw, sh, cx, cy, cw, ch, color, font.index, str, offset, len, px, py);
    }

    public void drawChar(char character, int x, int y, int anchor) {
        drawSubstring(String.valueOf(character), 0, 1, x, y, anchor);
    }

    public void drawChars(char[] data, int offset, int length, int x, int y, int anchor) {
        if (offset < 0 || length < 0 || offset + length > data.length) throw new ArrayIndexOutOfBoundsException();
        checkTextAnchor(anchor);
        int px = x + tx, py = textY(y + ty, anchor);
        if ((anchor & (RIGHT | HCENTER)) != 0 || font.isUnderlined()) {
            int width = Native.charsWidth(font.index, data, offset, length);
            px = anchorX(px, width, anchor);
            underline(px, py, width);
        }
        Native.drawChars(pix, sw, sh, cx, cy, cw, ch, color, font.index, data, offset, length, px, py);
    }

    // ---- images ----

    public void drawImage(Image img, int x, int y, int anchor) {
        if (img == null) throw new NullPointerException();
        int px = anchorX(x + tx, img.w, anchor), py = y + ty;
        if ((anchor & BOTTOM) != 0) py -= img.h;
        else if ((anchor & VCENTER) != 0) py -= img.h / 2;
        Native.blit(pix, sw, sh, cx, cy, cw, ch, img.pix, img.alpha, img.w, img.h,
                    0, 0, img.w, img.h, 0, px, py);
    }

    public void drawRegion(Image src, int x_src, int y_src, int width, int height,
                           int transform, int x_dest, int y_dest, int anchor) {
        if (src == null) throw new NullPointerException();
        if (src.pix == pix) throw new IllegalArgumentException();
        if (transform < 0 || transform > 7) throw new IllegalArgumentException();
        if (width < 0 || height < 0 || x_src < 0 || y_src < 0
            || x_src + width > src.w || y_src + height > src.h) {
            throw new IllegalArgumentException();
        }
        int tw = transform >= 4 ? height : width, th = transform >= 4 ? width : height;
        int px = anchorX(x_dest + tx, tw, anchor), py = y_dest + ty;
        if ((anchor & BOTTOM) != 0) py -= th;
        else if ((anchor & VCENTER) != 0) py -= th / 2;
        Native.blit(pix, sw, sh, cx, cy, cw, ch, src.pix, src.alpha, src.w, src.h,
                    x_src, y_src, width, height, transform, px, py);
    }

    public void copyArea(int x_src, int y_src, int width, int height, int x_dest, int y_dest, int anchor) {
        if (width <= 0 || height <= 0) return;
        int sx = x_src + tx, sy = y_src + ty;
        if (sx < 0 || sy < 0 || sx + width > sw || sy + height > sh) throw new IllegalArgumentException();
        short[] tmp = new short[width * height];
        for (int row = 0; row < height; row++) {
            System.arraycopy(pix, (sy + row) * sw + sx, tmp, row * width, width);
        }
        int px = anchorX(x_dest + tx, width, anchor), py = y_dest + ty;
        if ((anchor & BOTTOM) != 0) py -= height;
        else if ((anchor & VCENTER) != 0) py -= height / 2;
        Native.blit(pix, sw, sh, cx, cy, cw, ch, tmp, null, width, height, 0, 0, width, height, 0, px, py);
    }

    public void drawRGB(int[] rgbData, int offset, int scanlength, int x, int y,
                        int width, int height, boolean processAlpha) {
        if (rgbData == null) throw new NullPointerException();
        if (width <= 0 || height <= 0) return;
        int first = offset + (scanlength < 0 ? (height - 1) * scanlength : 0);
        int last = offset + (scanlength < 0 ? 0 : (height - 1) * scanlength) + width - 1;
        if (first < 0 || last >= rgbData.length) throw new ArrayIndexOutOfBoundsException();
        Native.drawRGB(pix, sw, sh, cx, cy, cw, ch, rgbData, offset, scanlength,
                       x + tx, y + ty, width, height, processAlpha ? 1 : 0);
    }
}
