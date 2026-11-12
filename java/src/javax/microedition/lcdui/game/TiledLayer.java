/*
 * amiga-j2me: MIDP 2.0 implementation for the Amiga J2ME runtime.
 * Written for this project; licensed under GPLv2 (see COPYING).
 */
package javax.microedition.lcdui.game;

import javax.microedition.lcdui.GameAccess;
import javax.microedition.lcdui.Graphics;
import javax.microedition.lcdui.Image;

public class TiledLayer extends Layer {
    private final int columns, rows;
    private int cellWidth, cellHeight;
    Image image;
    private int tilesPerRow, tileCount;
    private final int[] cells;          // row-major; 0 empty, > 0 static, < 0 animated
    private int[] animated = new int[4];
    private int animatedCount;

    public TiledLayer(int columns, int rows, Image image, int tileWidth, int tileHeight) {
        super(columns < 1 ? 1 : columns * (tileWidth < 1 ? 1 : tileWidth),
              rows < 1 ? 1 : rows * (tileHeight < 1 ? 1 : tileHeight));
        if (columns < 1 || rows < 1) throw new IllegalArgumentException();
        this.columns = columns; this.rows = rows;
        cells = new int[columns * rows];
        setTiles(image, tileWidth, tileHeight);
    }

    private void setTiles(Image img, int tw, int th) {
        if (img == null) throw new NullPointerException();
        if (tw < 1 || th < 1 || img.getWidth() % tw != 0 || img.getHeight() % th != 0) {
            throw new IllegalArgumentException();
        }
        image = img; cellWidth = tw; cellHeight = th;
        tilesPerRow = img.getWidth() / tw;
        tileCount = tilesPerRow * (img.getHeight() / th);
        setSize(columns * tw, rows * th);
    }

    public int createAnimatedTile(int staticTileIndex) {
        if (staticTileIndex < 0 || staticTileIndex > tileCount) throw new IndexOutOfBoundsException();
        if (animatedCount == animated.length) {
            int[] n = new int[animated.length * 2];
            System.arraycopy(animated, 0, n, 0, animatedCount);
            animated = n;
        }
        animated[animatedCount++] = staticTileIndex;
        return -animatedCount;
    }

    public void setAnimatedTile(int animatedTileIndex, int staticTileIndex) {
        int i = -animatedTileIndex - 1;
        if (i < 0 || i >= animatedCount || staticTileIndex < 0 || staticTileIndex > tileCount) {
            throw new IndexOutOfBoundsException();
        }
        animated[i] = staticTileIndex;
    }

    public int getAnimatedTile(int animatedTileIndex) {
        int i = -animatedTileIndex - 1;
        if (i < 0 || i >= animatedCount) throw new IndexOutOfBoundsException();
        return animated[i];
    }

    public void setCell(int col, int row, int tileIndex) {
        if (col < 0 || col >= columns || row < 0 || row >= rows) throw new IndexOutOfBoundsException();
        if (tileIndex > tileCount || -tileIndex > animatedCount) throw new IndexOutOfBoundsException();
        cells[row * columns + col] = tileIndex;
    }

    public int getCell(int col, int row) {
        if (col < 0 || col >= columns || row < 0 || row >= rows) throw new IndexOutOfBoundsException();
        return cells[row * columns + col];
    }

    public void fillCells(int col, int row, int numCols, int numRows, int tileIndex) {
        if (numCols < 0 || numRows < 0) throw new IllegalArgumentException();
        if (col < 0 || row < 0 || col + numCols > columns || row + numRows > rows) throw new IndexOutOfBoundsException();
        if (tileIndex > tileCount || -tileIndex > animatedCount) throw new IndexOutOfBoundsException();
        for (int r = row; r < row + numRows; r++) {
            for (int c = col; c < col + numCols; c++) cells[r * columns + c] = tileIndex;
        }
    }

    public final int getCellWidth() { return cellWidth; }
    public final int getCellHeight() { return cellHeight; }
    public final int getColumns() { return columns; }
    public final int getRows() { return rows; }

    public void setStaticTileSet(Image image, int tileWidth, int tileHeight) {
        int old = tileCount;
        setTiles(image, tileWidth, tileHeight);
        if (tileCount < old) {          // fewer tiles: the layer is emptied, as MIDP says
            for (int i = 0; i < cells.length; i++) cells[i] = 0;
            animatedCount = 0;
        }
    }

    /** The static tile shown in a cell (animated tiles resolved), 0 = empty. */
    int tileAt(int col, int row) {
        int t = cells[row * columns + col];
        return t < 0 ? animated[-t - 1] : t;
    }

    public final void paint(Graphics g) {
        if (g == null) throw new NullPointerException();
        if (!visible) return;
        // only the cells under the clip
        int c0 = (g.getClipX() - x) / cellWidth, r0 = (g.getClipY() - y) / cellHeight;
        int c1 = (g.getClipX() + g.getClipWidth() - x + cellWidth - 1) / cellWidth;
        int r1 = (g.getClipY() + g.getClipHeight() - y + cellHeight - 1) / cellHeight;
        if (c0 < 0) c0 = 0;
        if (r0 < 0) r0 = 0;
        if (c1 > columns) c1 = columns;
        if (r1 > rows) r1 = rows;
        if (c0 < c1 && r0 < r1) {
            GameAccess.drawTiles(g, image, cellWidth, cellHeight, tilesPerRow, cells, columns,
                                 c0, r0, c1, r1, x, y, animated);
        }
    }

    /** Source position of a tile (1-based) in the tile image. */
    int tileSrcX(int t) { return ((t - 1) % tilesPerRow) * cellWidth; }
    int tileSrcY(int t) { return ((t - 1) / tilesPerRow) * cellHeight; }
}
