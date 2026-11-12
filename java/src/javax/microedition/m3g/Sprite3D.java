/*
 * amiga-j2me: JSR-184 (M3G 1.0/1.1) implementation for the Amiga J2ME runtime.
 * Written for this project; licensed under GPLv2 (see COPYING).
 */
package javax.microedition.m3g;

import java.util.Vector;

public class Sprite3D extends Node {
    final boolean scaled;
    Image2D image;
    Appearance appearance;
    int cropX, cropY, cropW, cropH;

    public Sprite3D(boolean scaled, Image2D image, Appearance appearance) {
        this.scaled = scaled;
        setImage(image);
        this.appearance = appearance;
    }

    public boolean isScaled() { return scaled; }
    public void setAppearance(Appearance a) { appearance = a; }
    public Appearance getAppearance() { return appearance; }
    public void setImage(Image2D img) {
        if (img == null) throw new NullPointerException();
        image = img;
        cropX = 0; cropY = 0; cropW = img.width; cropH = img.height;
    }
    public Image2D getImage() { return image; }
    public void setCrop(int x, int y, int w, int h) { cropX = x; cropY = y; cropW = w; cropH = h; }
    public int getCropX() { return cropX; }
    public int getCropY() { return cropY; }
    public int getCropWidth() { return cropW; }
    public int getCropHeight() { return cropH; }

    void setProperty(int property, float[] v) {
        if (property == AnimationTrack.CROP) {
            cropX = (int) v[0]; cropY = (int) v[1];
            if (v.length >= 4) { cropW = (int) v[2]; cropH = (int) v[3]; }
        } else super.setProperty(property, v);
    }

    void collectReferences(Vector out) { add(out, image); add(out, appearance); }

    Object3D duplicateImpl() {
        Sprite3D s = new Sprite3D(scaled, image, appearance);
        s.copyNode(this);
        s.cropX = cropX; s.cropY = cropY; s.cropW = cropW; s.cropH = cropH;
        return s;
    }
}
