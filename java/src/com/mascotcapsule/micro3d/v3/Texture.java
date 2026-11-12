/*
 * amiga-j2me: Mascot Capsule micro3d v3 API for the Amiga J2ME runtime.
 * Written for this project; licensed under GPLv2 (see COPYING).
 */
package com.mascotcapsule.micro3d.v3;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;

/** A 256-colour BMP; for model textures palette entry 0 is the transparent colour. */
public class Texture {
    final boolean forModel;
    short[] pix;        // R5G6B5
    byte[] alpha;       // 0 where the palette index is 0
    int width, height;

    public Texture(byte[] b, boolean isForModel) {
        if (b == null) throw new NullPointerException();
        forModel = isForModel;
        try {
            decode(b);
        } catch (ArrayIndexOutOfBoundsException e) {
            throw new RuntimeException("damaged BMP");
        }
    }

    public Texture(String name, boolean isForModel) throws IOException {
        this(readResource(name), isForModel);
    }

    static byte[] readResource(String name) throws IOException {
        if (name == null) throw new NullPointerException();
        InputStream in = Texture.class.getResourceAsStream(name.startsWith("/") ? name : "/" + name);
        if (in == null) throw new IOException(name + " not found");
        ByteArrayOutputStream out = new ByteArrayOutputStream(4096);
        byte[] buf = new byte[1024];
        int n;
        while ((n = in.read(buf)) > 0) out.write(buf, 0, n);
        in.close();
        return out.toByteArray();
    }

    private static int le16(byte[] b, int o) { return (b[o] & 0xFF) | ((b[o + 1] & 0xFF) << 8); }
    private static int le32(byte[] b, int o) { return le16(b, o) | (le16(b, o + 2) << 16); }

    private void decode(byte[] b) {
        if (b.length < 30 || b[0] != 'B' || b[1] != 'M') throw new RuntimeException("not a BMP");
        int raster = le32(b, 10), header = le32(b, 14), bpp, entry;
        boolean bottomUp = true;
        if (header == 12) {
            width = le16(b, 18); height = le16(b, 20); bpp = le16(b, 24); entry = 3;
        } else {
            width = le32(b, 18); height = le32(b, 22); bpp = le16(b, 28); entry = 4;
            if (height < 0) { height = -height; bottomUp = false; }
            if (le32(b, 30) != 0) throw new RuntimeException("compressed BMPs are not supported");
        }
        if (bpp != 8 || width <= 0 || height <= 0 || width > 1024 || height > 1024) throw new RuntimeException("unsupported BMP");
        int palette = 14 + header, stride = (width + 3) & ~3;
        pix = new short[width * height];
        alpha = new byte[width * height];
        for (int y = 0; y < height; y++) {
            int src = raster + (bottomUp ? height - 1 - y : y) * stride, dst = y * width;
            for (int x = 0; x < width; x++) {
                int idx = b[src + x] & 0xFF, p = palette + idx * entry;
                int bl = b[p] & 0xFF, g = b[p + 1] & 0xFF, r = b[p + 2] & 0xFF;
                pix[dst + x] = (short) (((r & 0xF8) << 8) | ((g & 0xFC) << 3) | (bl >> 3));
                alpha[dst + x] = (byte) (idx == 0 ? 0 : 255);
            }
        }
    }

    public final void dispose() {}
}
