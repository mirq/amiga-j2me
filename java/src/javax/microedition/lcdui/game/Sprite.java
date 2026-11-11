/*
 * amiga-j2me: MIDP 2.0 implementation for the Amiga J2ME runtime.
 * Written for this project; licensed under GPLv2 (see COPYING).
 */
package javax.microedition.lcdui.game;

import javax.microedition.lcdui.GameAccess;
import javax.microedition.lcdui.Graphics;
import javax.microedition.lcdui.Image;

public class Sprite extends Layer {
    public static final int TRANS_NONE = 0, TRANS_ROT90 = 5, TRANS_ROT180 = 3, TRANS_ROT270 = 6,
                            TRANS_MIRROR = 2, TRANS_MIRROR_ROT90 = 7, TRANS_MIRROR_ROT180 = 1,
                            TRANS_MIRROR_ROT270 = 4;

    private Image image;
    private int frameW, frameH;         // untransformed frame size
    private int rawFrames, framesPerRow;
    private int[] sequence;             // null = 0..rawFrames-1
    private int seqIndex;
    private int refX, refY;             // reference pixel in the untransformed frame
    private int transform;
    private int colX, colY, colW, colH;         // collision rectangle, untransformed frame
    private int tColX, tColY, tColW, tColH;     // the same after the transform

    public Sprite(Image image) {
        super(image.getWidth(), image.getHeight());
        init(image, image.getWidth(), image.getHeight(), false);
    }

    public Sprite(Image image, int frameWidth, int frameHeight) {
        super(frameWidth, frameHeight);
        if (frameWidth < 1 || frameHeight < 1 || image.getWidth() % frameWidth != 0
            || image.getHeight() % frameHeight != 0) {
            throw new IllegalArgumentException();
        }
        init(image, frameWidth, frameHeight, false);
    }

    public Sprite(Sprite s) {
        super(s.width, s.height);
        image = s.image; frameW = s.frameW; frameH = s.frameH;
        rawFrames = s.rawFrames; framesPerRow = s.framesPerRow;
        if (s.sequence != null) {
            sequence = new int[s.sequence.length];
            System.arraycopy(s.sequence, 0, sequence, 0, sequence.length);
        }
        seqIndex = s.seqIndex; refX = s.refX; refY = s.refY; transform = s.transform;
        colX = s.colX; colY = s.colY; colW = s.colW; colH = s.colH;
        tColX = s.tColX; tColY = s.tColY; tColW = s.tColW; tColH = s.tColH;
        x = s.x; y = s.y; visible = s.visible;
    }

    private void init(Image img, int fw, int fh, boolean keepState) {
        image = img; frameW = fw; frameH = fh;
        framesPerRow = img.getWidth() / fw;
        rawFrames = framesPerRow * (img.getHeight() / fh);
        if (!keepState) {
            sequence = null; seqIndex = 0;
            colX = 0; colY = 0; colW = fw; colH = fh;
        }
        applyTransformBounds();
    }

    // ---- frames ----

    public void setFrame(int sequenceIndex) {
        if (sequenceIndex < 0 || sequenceIndex >= getFrameSequenceLength()) throw new IndexOutOfBoundsException();
        seqIndex = sequenceIndex;
    }
    public final int getFrame() { return seqIndex; }
    public int getRawFrameCount() { return rawFrames; }
    public int getFrameSequenceLength() { return sequence == null ? rawFrames : sequence.length; }
    public void nextFrame() { seqIndex = (seqIndex + 1) % getFrameSequenceLength(); }
    public void prevFrame() {
        int n = getFrameSequenceLength();
        seqIndex = (seqIndex + n - 1) % n;
    }

    public void setFrameSequence(int[] seq) {
        if (seq == null) { sequence = null; seqIndex = 0; return; }
        if (seq.length < 1) throw new IllegalArgumentException();
        for (int i = 0; i < seq.length; i++) {
            if (seq[i] < 0 || seq[i] >= rawFrames) throw new ArrayIndexOutOfBoundsException();
        }
        sequence = new int[seq.length];
        System.arraycopy(seq, 0, sequence, 0, seq.length);
        seqIndex = 0;
    }

    public void setImage(Image img, int frameWidth, int frameHeight) {
        if (frameWidth < 1 || frameHeight < 1 || img.getWidth() % frameWidth != 0
            || img.getHeight() % frameHeight != 0) {
            throw new IllegalArgumentException();
        }
        int newRaw = (img.getWidth() / frameWidth) * (img.getHeight() / frameHeight);
        boolean sameSize = frameWidth == frameW && frameHeight == frameH;
        boolean keepSeq = newRaw >= rawFrames;      // fewer frames: sequence reset
        int oldRefX = getRefPixelX(), oldRefY = getRefPixelY();
        init(img, frameWidth, frameHeight, true);
        if (!keepSeq) { sequence = null; seqIndex = 0; }
        if (!sameSize) {
            colX = 0; colY = 0; colW = frameWidth; colH = frameHeight;
            applyTransformBounds();
            // the reference pixel stays where it was on the screen
            x = oldRefX - transX(refX, refY, transform);
            y = oldRefY - transY(refX, refY, transform);
        }
    }

    // ---- reference pixel and transform ----

    public void defineReferencePixel(int x, int y) { refX = x; refY = y; }
    public void setRefPixelPosition(int px, int py) {
        x = px - transX(refX, refY, transform);
        y = py - transY(refX, refY, transform);
    }
    public int getRefPixelX() { return x + transX(refX, refY, transform); }
    public int getRefPixelY() { return y + transY(refX, refY, transform); }

    public void setTransform(int t) {
        if (t < 0 || t > 7) throw new IllegalArgumentException();
        int rx = getRefPixelX(), ry = getRefPixelY();
        transform = t;
        applyTransformBounds();
        x = rx - transX(refX, refY, t);
        y = ry - transY(refX, refY, t);
    }

    /** Where the frame pixel (px, py) lands in the transformed frame. */
    private int transX(int px, int py, int t) {
        switch (t) {
        case TRANS_ROT90: case TRANS_MIRROR_ROT90: return frameH - py - 1;
        case TRANS_ROT180: case TRANS_MIRROR: return frameW - px - 1;
        case TRANS_ROT270: case TRANS_MIRROR_ROT270: return py;
        default: return px;
        }
    }

    private int transY(int px, int py, int t) {
        switch (t) {
        case TRANS_ROT90: case TRANS_MIRROR_ROT270: return px;
        case TRANS_ROT180: case TRANS_MIRROR_ROT180: return frameH - py - 1;
        case TRANS_ROT270: case TRANS_MIRROR_ROT90: return frameW - px - 1;
        default: return py;
        }
    }

    private void applyTransformBounds() {
        boolean swap = transform >= 4;
        width = swap ? frameH : frameW;
        height = swap ? frameW : frameH;
        switch (transform) {
        case TRANS_MIRROR:        tColX = frameW - (colX + colW); tColY = colY; break;
        case TRANS_MIRROR_ROT180: tColX = colX; tColY = frameH - (colY + colH); break;
        case TRANS_ROT90:         tColX = frameH - (colY + colH); tColY = colX; break;
        case TRANS_ROT180:        tColX = frameW - (colX + colW); tColY = frameH - (colY + colH); break;
        case TRANS_ROT270:        tColX = colY; tColY = frameW - (colX + colW); break;
        case TRANS_MIRROR_ROT90:  tColX = frameH - (colY + colH); tColY = frameW - (colX + colW); break;
        case TRANS_MIRROR_ROT270: tColX = colY; tColY = colX; break;
        default:                  tColX = colX; tColY = colY; break;
        }
        tColW = swap ? colH : colW;
        tColH = swap ? colW : colH;
    }

    public final void paint(Graphics g) {
        if (g == null) throw new NullPointerException();
        if (!visible) return;
        int f = sequence == null ? seqIndex : sequence[seqIndex];
        g.drawRegion(image, (f % framesPerRow) * frameW, (f / framesPerRow) * frameH, frameW, frameH,
                     transform, x, y, Graphics.TOP | Graphics.LEFT);
    }

    // ---- collisions ----

    public void defineCollisionRectangle(int x, int y, int width, int height) {
        if (width < 0 || height < 0) throw new IllegalArgumentException();
        colX = x; colY = y; colW = width; colH = height;
        applyTransformBounds();
    }

    /** Is the pixel at layer-relative (lx, ly) of the current, transformed frame opaque? */
    private boolean opaqueAt(int lx, int ly) {
        int u, v;       // back to the untransformed frame
        switch (transform) {
        case TRANS_MIRROR:        u = frameW - 1 - lx; v = ly; break;
        case TRANS_ROT180:        u = frameW - 1 - lx; v = frameH - 1 - ly; break;
        case TRANS_MIRROR_ROT180: u = lx; v = frameH - 1 - ly; break;
        case TRANS_ROT90:         u = ly; v = frameH - 1 - lx; break;
        case TRANS_ROT270:        u = frameW - 1 - ly; v = lx; break;
        case TRANS_MIRROR_ROT90:  u = frameW - 1 - ly; v = frameH - 1 - lx; break;
        case TRANS_MIRROR_ROT270: u = ly; v = lx; break;
        default:                  u = lx; v = ly; break;
        }
        if (u < 0 || v < 0 || u >= frameW || v >= frameH) return false;
        int f = sequence == null ? seqIndex : sequence[seqIndex];
        return GameAccess.isOpaque(image, (f % framesPerRow) * frameW + u, (f / framesPerRow) * frameH + v);
    }

    public final boolean collidesWith(Sprite s, boolean pixelLevel) {
        if (!visible || !s.visible) return false;
        int ax0 = x + tColX, ay0 = y + tColY, ax1 = ax0 + tColW, ay1 = ay0 + tColH;
        int bx0 = s.x + s.tColX, by0 = s.y + s.tColY, bx1 = bx0 + s.tColW, by1 = by0 + s.tColH;
        int ix0 = Math.max(ax0, bx0), iy0 = Math.max(ay0, by0);
        int ix1 = Math.min(ax1, bx1), iy1 = Math.min(ay1, by1);
        if (ix0 >= ix1 || iy0 >= iy1) return false;
        if (!pixelLevel) return true;
        for (int py = iy0; py < iy1; py++) {
            for (int px = ix0; px < ix1; px++) {
                if (opaqueAt(px - x, py - y) && s.opaqueAt(px - s.x, py - s.y)) return true;
            }
        }
        return false;
    }

    public final boolean collidesWith(Image img, int ix, int iy, boolean pixelLevel) {
        if (!visible) return false;
        int ax0 = x + tColX, ay0 = y + tColY, ax1 = ax0 + tColW, ay1 = ay0 + tColH;
        int x0 = Math.max(ax0, ix), y0 = Math.max(ay0, iy);
        int x1 = Math.min(ax1, ix + img.getWidth()), y1 = Math.min(ay1, iy + img.getHeight());
        if (x0 >= x1 || y0 >= y1) return false;
        if (!pixelLevel) return true;
        for (int py = y0; py < y1; py++) {
            for (int px = x0; px < x1; px++) {
                if (opaqueAt(px - x, py - y) && GameAccess.isOpaque(img, px - ix, py - iy)) return true;
            }
        }
        return false;
    }

    public final boolean collidesWith(TiledLayer t, boolean pixelLevel) {
        if (!visible || !t.visible) return false;
        int ax0 = x + tColX, ay0 = y + tColY, ax1 = ax0 + tColW, ay1 = ay0 + tColH;
        int x0 = Math.max(ax0, t.x), y0 = Math.max(ay0, t.y);
        int x1 = Math.min(ax1, t.x + t.width), y1 = Math.min(ay1, t.y + t.height);
        if (x0 >= x1 || y0 >= y1) return false;
        int cw = t.getCellWidth(), ch = t.getCellHeight();
        int c0 = (x0 - t.x) / cw, c1 = (x1 - 1 - t.x) / cw;
        int r0 = (y0 - t.y) / ch, r1 = (y1 - 1 - t.y) / ch;
        for (int r = r0; r <= r1; r++) {
            for (int c = c0; c <= c1; c++) {
                int tile = t.tileAt(c, r);
                if (tile == 0) continue;
                if (!pixelLevel) return true;
                int tx0 = t.x + c * cw, ty0 = t.y + r * ch;
                int px0 = Math.max(x0, tx0), py0 = Math.max(y0, ty0);
                int px1 = Math.min(x1, tx0 + cw), py1 = Math.min(y1, ty0 + ch);
                int sx = t.tileSrcX(tile), sy = t.tileSrcY(tile);
                for (int py = py0; py < py1; py++) {
                    for (int px = px0; px < px1; px++) {
                        if (opaqueAt(px - x, py - y)
                            && GameAccess.isOpaque(t.image, sx + px - tx0, sy + py - ty0)) return true;
                    }
                }
            }
        }
        return false;
    }
}
