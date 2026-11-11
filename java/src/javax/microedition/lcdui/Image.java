/*
 * amiga-j2me: MIDP 2.0 implementation for the Amiga J2ME runtime.
 * Written for this project; licensed under GPLv2 (see COPYING).
 */
package javax.microedition.lcdui;

import aj.Native;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;

/** An image: R5G6B5 pixels in a short[] and, for translucent ones, a byte[] alpha plane. */
public class Image {
    short[] pix;
    byte[] alpha;       // null = opaque
    int w, h;
    boolean mutable;

    Image(int w, int h, boolean mutable) {
        this.w = w; this.h = h; this.mutable = mutable;
        pix = new short[w * h];
    }

    public static Image createImage(int width, int height) {
        if (width <= 0 || height <= 0) throw new IllegalArgumentException();
        Image img = new Image(width, height, true);
        short[] p = img.pix;
        for (int i = p.length - 1; i >= 0; i--) p[i] = (short) 0xFFFF;   // white
        return img;
    }

    public static Image createImage(Image source) {
        if (!source.mutable) return source;
        Image img = new Image(source.w, source.h, false);
        System.arraycopy(source.pix, 0, img.pix, 0, source.pix.length);
        return img;
    }

    public static Image createImage(String name) throws IOException {
        if (name == null) throw new NullPointerException();
        InputStream in = Image.class.getResourceAsStream(name);
        if (in == null) throw new IOException("no resource " + name);
        try {
            return createImage(in);
        } finally {
            in.close();
        }
    }

    public static Image createImage(InputStream stream) throws IOException {
        if (stream == null) throw new NullPointerException();
        ByteArrayOutputStream out = new ByteArrayOutputStream(4096);
        byte[] buf = new byte[2048];
        int n;
        while ((n = stream.read(buf)) > 0) out.write(buf, 0, n);
        byte[] data = out.toByteArray();
        try {
            return createImage(data, 0, data.length);
        } catch (IllegalArgumentException e) {
            throw new IOException("bad image data");
        }
    }

    public static Image createImage(byte[] imageData, int imageOffset, int imageLength) {
        if (imageOffset < 0 || imageLength < 0 || imageOffset + imageLength > imageData.length) {
            throw new ArrayIndexOutOfBoundsException();
        }
        synchronized (Display.LOCK) {      // the decoder keeps one image between begin and end
            int r = Native.decodeBegin(imageData, imageOffset, imageLength);
            if (r == 0) throw new IllegalArgumentException("bad image data");
            Image img;
            byte[] a = null;
            try {
                img = new Image(r & 0x7FFF, (r >> 15) & 0x7FFF, false);
                if ((r & (1 << 30)) != 0) a = new byte[img.w * img.h];
            } catch (OutOfMemoryError e) {
                Native.decodeEnd(null, null);
                throw e;
            }
            img.alpha = a;
            Native.decodeEnd(img.pix, a);
            return img;
        }
    }

    public static Image createRGBImage(int[] rgb, int width, int height, boolean processAlpha) {
        if (width <= 0 || height <= 0) throw new IllegalArgumentException();
        if (rgb.length < width * height) throw new ArrayIndexOutOfBoundsException();
        Image img = new Image(width, height, false);
        if (processAlpha) {
            byte[] a = new byte[width * height];
            if (Native.fromRGB(rgb, 0, width * height, img.pix, a) != 0) img.alpha = a;
        } else {
            Native.fromRGB(rgb, 0, width * height, img.pix, null);
        }
        return img;
    }

    public static Image createImage(Image image, int x, int y, int width, int height, int transform) {
        if (transform < 0 || transform > 7) throw new IllegalArgumentException();
        if (width <= 0 || height <= 0 || x < 0 || y < 0 || x + width > image.w || y + height > image.h) {
            throw new IllegalArgumentException();
        }
        if (!image.mutable && x == 0 && y == 0 && width == image.w && height == image.h && transform == 0) {
            return image;
        }
        boolean swap = transform >= 4;
        int tw = swap ? height : width, th = swap ? width : height;
        Image img = new Image(tw, th, false);
        Native.blit(img.pix, tw, th, 0, 0, tw, th, image.pix, null, image.w, image.h,
                    x, y, width, height, transform, 0, 0);
        if (image.alpha != null) {
            img.alpha = transformAlpha(image, x, y, width, height, transform, tw, th);
        }
        return img;
    }

    // Alpha planes are transformed in Java: it happens once per created image.
    private static byte[] transformAlpha(Image src, int x, int y, int w, int h, int t, int tw, int th) {
        byte[] out = new byte[tw * th];
        byte[] a = src.alpha;
        for (int dy = 0; dy < th; dy++) {
            for (int dx = 0; dx < tw; dx++) {
                int u, v;
                switch (t) {
                case 2: u = w - 1 - dx; v = dy; break;              // MIRROR
                case 3: u = w - 1 - dx; v = h - 1 - dy; break;      // ROT180
                case 1: u = dx; v = h - 1 - dy; break;              // MIRROR_ROT180
                case 5: u = dy; v = h - 1 - dx; break;              // ROT90
                case 6: u = w - 1 - dy; v = dx; break;              // ROT270
                case 7: u = w - 1 - dy; v = h - 1 - dx; break;      // MIRROR_ROT90
                case 4: u = dy; v = dx; break;                      // MIRROR_ROT270
                default: u = dx; v = dy; break;
                }
                out[dy * tw + dx] = a[(y + v) * src.w + x + u];
            }
        }
        return out;
    }

    public Graphics getGraphics() {
        if (!mutable) throw new IllegalStateException();
        return new Graphics(this);
    }

    public int getWidth() { return w; }
    public int getHeight() { return h; }
    public boolean isMutable() { return mutable; }

    public void getRGB(int[] rgbData, int offset, int scanlength, int x, int y, int width, int height) {
        if (width <= 0 || height <= 0) return;
        if (x < 0 || y < 0 || x + width > w || y + height > h) throw new IllegalArgumentException();
        if (Math.abs(scanlength) < width) throw new IllegalArgumentException();
        int first = offset + (scanlength < 0 ? (height - 1) * scanlength : 0);
        int last = offset + (scanlength < 0 ? 0 : (height - 1) * scanlength) + width - 1;
        if (first < 0 || last >= rgbData.length) throw new ArrayIndexOutOfBoundsException();
        Native.toRGB(pix, alpha, w, rgbData, offset, scanlength, x, y, width, height);
    }
}
