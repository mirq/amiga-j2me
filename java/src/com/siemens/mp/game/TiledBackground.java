/*
 * amiga-j2me: MIDP 2.0 implementation for the Amiga J2ME runtime.
 * Written for this project; licensed under GPLv2 (see COPYING).
 */
package com.siemens.mp.game;

import javax.microedition.lcdui.Graphics;
import javax.microedition.lcdui.Image;

/** A map of 8x8 tiles. Map values: 0 transparent, 1 white, 2 black, 3.. the tiles of the image, top to bottom. */
public class TiledBackground extends GraphicObject {
    private final Image tiles;
    private final byte[] map;
    private final int cols, rows, tileCount;
    private int px, py;

    public TiledBackground(byte[] tilePixels, byte[] tileMask, byte[] map, int widthInTiles, int heightInTiles) {
        this(com.siemens.mp.ui.Image.createImageFromBitmap(tilePixels, tileMask, 8, tilePixels.length), null,
             map, widthInTiles, heightInTiles);
    }

    public TiledBackground(ExtendedImage tilePixels, ExtendedImage tileMask, byte[] map, int widthInTiles, int heightInTiles) {
        this(tilePixels.getImage(), tileMask == null ? null : tileMask.getImage(), map, widthInTiles, heightInTiles);
    }

    public TiledBackground(Image tilePixels, Image tileMask, byte[] map, int widthInTiles, int heightInTiles) {
        tiles = tileMask == null ? tilePixels : com.siemens.mp.ui.Image.createTransparentImageFromMask(tilePixels, tileMask);
        tileCount = tiles.getHeight() / 8;
        this.map = map;
        cols = widthInTiles;
        rows = heightInTiles;
    }

    public void setPositionInMap(int x, int y) { px = x; py = y; }

    protected void paint(Graphics g) {
        int x0 = g.getClipX(), y0 = g.getClipY(), x1 = x0 + g.getClipWidth(), y1 = y0 + g.getClipHeight();
        int c0 = Math.max(0, (x0 + px) / 8), r0 = Math.max(0, (y0 + py) / 8);
        int c1 = Math.min(cols - 1, (x1 + px) / 8), r1 = Math.min(rows - 1, (y1 + py) / 8);
        int old = g.getColor();
        for (int r = r0; r <= r1; r++) {
            for (int c = c0; c <= c1; c++) {
                int t = map[r * cols + c] & 0xFF, x = c * 8 - px, y = r * 8 - py;
                if (t == 0) continue;
                if (t < 3) {
                    g.setColor(t == 1 ? 0xFFFFFF : 0);
                    g.fillRect(x, y, 8, 8);
                } else if (t - 3 < tileCount) {
                    g.drawRegion(tiles, 0, (t - 3) * 8, 8, 8, 0, x, y, Graphics.TOP | Graphics.LEFT);
                }
            }
        }
        g.setColor(old);
    }
}
