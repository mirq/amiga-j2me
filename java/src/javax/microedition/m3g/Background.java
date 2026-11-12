/*
 * amiga-j2me: JSR-184 (M3G 1.0/1.1) implementation for the Amiga J2ME runtime.
 * Written for this project; licensed under GPLv2 (see COPYING).
 */
package javax.microedition.m3g;

import java.util.Vector;

public class Background extends Object3D {
    public static final int BORDER = 32, REPEAT = 33;

    int color;
    Image2D image;
    boolean colorClear = true, depthClear = true;
    int modeX = BORDER, modeY = BORDER, cropX, cropY, cropW, cropH;

    public Background() {}

    public void setColorClearEnable(boolean enable) { colorClear = enable; }
    public boolean isColorClearEnabled() { return colorClear; }
    public void setDepthClearEnable(boolean enable) { depthClear = enable; }
    public boolean isDepthClearEnabled() { return depthClear; }
    public void setColor(int ARGB) { color = ARGB; }
    public int getColor() { return color; }
    public void setImage(Image2D img) {
        if (img != null && img.format != Image2D.RGB && img.format != Image2D.RGBA) throw new IllegalArgumentException();
        image = img;
        if (img != null) { cropX = 0; cropY = 0; cropW = img.width; cropH = img.height; }
    }
    public Image2D getImage() { return image; }
    public void setImageMode(int modeX, int modeY) {
        if ((modeX != BORDER && modeX != REPEAT) || (modeY != BORDER && modeY != REPEAT)) throw new IllegalArgumentException();
        this.modeX = modeX; this.modeY = modeY;
    }
    public int getImageModeX() { return modeX; }
    public int getImageModeY() { return modeY; }
    public void setCrop(int x, int y, int w, int h) {
        if (w < 0 || h < 0) throw new IllegalArgumentException();
        cropX = x; cropY = y; cropW = w; cropH = h;
    }
    public int getCropX() { return cropX; }
    public int getCropY() { return cropY; }
    public int getCropWidth() { return cropW; }
    public int getCropHeight() { return cropH; }

    void setProperty(int property, float[] v) {
        if (property == AnimationTrack.COLOR) {
            color = (color & 0xFF000000) | ((int) (M3GMath.clamp(v[0], 0, 1) * 255) << 16)
                  | ((int) (M3GMath.clamp(v[1], 0, 1) * 255) << 8) | (int) (M3GMath.clamp(v[2], 0, 1) * 255);
        } else if (property == AnimationTrack.ALPHA) {
            color = (color & 0xFFFFFF) | ((int) (M3GMath.clamp(v[0], 0, 1) * 255) << 24);
        } else if (property == AnimationTrack.CROP) {
            cropX = (int) v[0]; cropY = (int) v[1];
            if (v.length >= 4) { cropW = (int) v[2]; cropH = (int) v[3]; }
        } else super.setProperty(property, v);
    }

    void collectReferences(Vector out) { add(out, image); }
}
