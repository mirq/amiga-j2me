/* Image file decoding for amiga-j2me (PNG, JPEG, GIF, BMP) through stb_image
 * (public domain, third_party/stb). GPLv2. */
#include "gfx.h"
#include <stdlib.h>

#define STB_IMAGE_IMPLEMENTATION
#define STB_IMAGE_STATIC
#define STBI_NO_STDIO
#define STBI_NO_HDR
#define STBI_ONLY_PNG
#define STBI_ONLY_JPEG
#define STBI_ONLY_GIF
#define STBI_ONLY_BMP
#define STBI_NO_THREAD_LOCALS
#define STBI_NO_SIMD
#include "stb_image.h"

int gfx_decode(const uint8_t *data, int len, int *w, int *h,
               uint16_t **px, uint8_t **alpha)
{
    int comp, n, i, translucent = 0;
    unsigned char *rgba = stbi_load_from_memory(data, len, w, h, &comp, 4);
    uint16_t *p;
    uint8_t *a = NULL;

    *px = NULL; *alpha = NULL;
    if (!rgba) return 0;
    n = *w * *h;
    p = (uint16_t *)malloc((size_t)(n ? n : 1) * 2);
    if (!p) { stbi_image_free(rgba); return 0; }
    for (i = 0; i < n; i++) {
        const unsigned char *s = rgba + i * 4;
        p[i] = (uint16_t)(((s[0] & 0xF8) << 8) | ((s[1] & 0xFC) << 3) | (s[2] >> 3));
        if (s[3] != 0xFF) translucent = 1;
    }
    if (translucent) {
        a = (uint8_t *)malloc((size_t)n);
        if (!a) { free(p); stbi_image_free(rgba); return 0; }
        for (i = 0; i < n; i++) a[i] = rgba[i * 4 + 3];
    }
    stbi_image_free(rgba);
    *px = p; *alpha = a;
    return 1;
}
