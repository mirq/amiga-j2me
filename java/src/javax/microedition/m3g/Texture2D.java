/*
 * amiga-j2me: JSR-184 (M3G 1.0/1.1) implementation for the Amiga J2ME runtime.
 * Written for this project; licensed under GPLv2 (see COPYING).
 */
package javax.microedition.m3g;

import java.util.Vector;

public class Texture2D extends Transformable {
    public static final int FILTER_BASE_LEVEL = 208, FILTER_LINEAR = 209, FILTER_NEAREST = 210;
    public static final int FUNC_ADD = 224, FUNC_BLEND = 225, FUNC_DECAL = 226, FUNC_MODULATE = 227, FUNC_REPLACE = 228;
    public static final int WRAP_CLAMP = 240, WRAP_REPEAT = 241;

    Image2D image;
    int blending = FUNC_MODULATE, blendColor, wrapS = WRAP_REPEAT, wrapT = WRAP_REPEAT;
    private int levelFilter = FILTER_BASE_LEVEL, imageFilter = FILTER_NEAREST;

    public Texture2D(Image2D image) { setImage(image); }

    private static boolean pow2(int v) { return v > 0 && (v & (v - 1)) == 0; }

    public void setImage(Image2D image) {
        if (image == null) throw new NullPointerException();
        if (!pow2(image.width) || !pow2(image.height)) throw new IllegalArgumentException("texture size must be a power of two");
        this.image = image;
    }
    public Image2D getImage() { return image; }
    public void setFiltering(int levelFilter, int imageFilter) {
        if (levelFilter < FILTER_BASE_LEVEL || levelFilter > FILTER_NEAREST
            || imageFilter < FILTER_LINEAR || imageFilter > FILTER_NEAREST) {
            throw new IllegalArgumentException();
        }
        this.levelFilter = levelFilter; this.imageFilter = imageFilter;   // always drawn with nearest
    }
    public void setWrapping(int s, int t) {
        if ((s != WRAP_CLAMP && s != WRAP_REPEAT) || (t != WRAP_CLAMP && t != WRAP_REPEAT)) throw new IllegalArgumentException();
        wrapS = s; wrapT = t;
    }
    public int getWrappingS() { return wrapS; }
    public int getWrappingT() { return wrapT; }
    public int getLevelFilter() { return levelFilter; }
    public int getImageFilter() { return imageFilter; }
    public void setBlending(int func) {
        if (func < FUNC_ADD || func > FUNC_REPLACE) throw new IllegalArgumentException();
        blending = func;
    }
    public int getBlending() { return blending; }
    public void setBlendColor(int RGB) { blendColor = RGB & 0xFFFFFF; }
    public int getBlendColor() { return blendColor; }

    void setProperty(int property, float[] v) {
        if (property == AnimationTrack.COLOR) {
            blendColor = ((int) (M3GMath.clamp(v[0], 0, 1) * 255) << 16) | ((int) (M3GMath.clamp(v[1], 0, 1) * 255) << 8)
                       | (int) (M3GMath.clamp(v[2], 0, 1) * 255);
        } else super.setProperty(property, v);
    }

    void collectReferences(Vector out) { add(out, image); }

    Object3D duplicateImpl() {
        Texture2D t = new Texture2D(image);
        t.copyTransformable(this);
        t.blending = blending; t.blendColor = blendColor; t.wrapS = wrapS; t.wrapT = wrapT;
        t.levelFilter = levelFilter; t.imageFilter = imageFilter;
        return t;
    }
}
