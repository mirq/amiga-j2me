/* KNI natives of aj.Native: graphics, display and input. GPLv2.
 *
 * Pixel data lives in Java arrays (short[] R5G6B5, byte[] alpha); the natives
 * work on the raw array memory. None of them allocates on the Java heap
 * while holding a raw pointer, so no GC can move the arrays under them. */
#include <stdlib.h>
#include <string.h>
#include "kni.h"
#include "sni.h"
#include "hal.h"
#include "gfx.h"

/* Parameters 1..7 of every drawing native: dst[], dw, dh, clip x, y, w, h */
#define DST_BEGIN(nhandles) \
    GfxDst dst; \
    KNI_StartHandles(nhandles); \
    KNI_DeclareHandle(hdst); \
    KNI_GetParameterAsObject(1, hdst); \
    gfx_dst_init(&dst, (uint16_t *)SNI_GetRawArrayPointer((jarray)hdst), \
                 KNI_GetParameterAsInt(2), KNI_GetParameterAsInt(3), \
                 KNI_GetParameterAsInt(4), KNI_GetParameterAsInt(5), \
                 KNI_GetParameterAsInt(6), KNI_GetParameterAsInt(7))
#define P(n) KNI_GetParameterAsInt(n)

KNIEXPORT KNI_RETURNTYPE_VOID Java_aj_Native_fillRect(void)
{
    DST_BEGIN(1);
    gfx_fill_rect(&dst, P(9), P(10), P(11), P(12), GFX_RGB565(P(8)));
    KNI_EndHandles();
    KNI_ReturnVoid();
}

KNIEXPORT KNI_RETURNTYPE_VOID Java_aj_Native_drawLine(void)
{
    DST_BEGIN(1);
    gfx_line(&dst, P(9), P(10), P(11), P(12), GFX_RGB565(P(8)), P(13));
    KNI_EndHandles();
    KNI_ReturnVoid();
}

KNIEXPORT KNI_RETURNTYPE_VOID Java_aj_Native_fillTriangle(void)
{
    DST_BEGIN(1);
    gfx_fill_triangle(&dst, P(9), P(10), P(11), P(12), P(13), P(14), GFX_RGB565(P(8)));
    KNI_EndHandles();
    KNI_ReturnVoid();
}

KNIEXPORT KNI_RETURNTYPE_VOID Java_aj_Native_arc(void)
{
    DST_BEGIN(1);
    gfx_arc(&dst, P(9), P(10), P(11), P(12), P(13), P(14), P(15), GFX_RGB565(P(8)), P(16));
    KNI_EndHandles();
    KNI_ReturnVoid();
}

/* blit(dst.., short[] src, byte[] alpha, sw, sh, sx, sy, w, h, transform, dx, dy) */
KNIEXPORT KNI_RETURNTYPE_VOID Java_aj_Native_blit(void)
{
    DST_BEGIN(3);
    KNI_DeclareHandle(hsrc);
    KNI_DeclareHandle(halpha);
    KNI_GetParameterAsObject(8, hsrc);
    KNI_GetParameterAsObject(9, halpha);
    gfx_blit(&dst, (const uint16_t *)SNI_GetRawArrayPointer((jarray)hsrc),
             KNI_IsNullHandle(halpha) ? NULL : (const uint8_t *)SNI_GetRawArrayPointer((jarray)halpha),
             P(10), P(11), P(12), P(13), P(14), P(15), P(16), P(17), P(18));
    KNI_EndHandles();
    KNI_ReturnVoid();
}

/* drawTiles(dst.., short[] src, byte[] alpha, sw, sh, tileW, tileH, tilesPerRow,
 *           int[] cells, columns, c0, r0, c1, r1, x, y, int[] animated)
 * TiledLayer.paint in one call: cells[row * columns + col] is 0 (empty), a
 * tile number from 1, or minus an index into animated[] (from 1). */
KNIEXPORT KNI_RETURNTYPE_VOID Java_aj_Native_drawTiles(void)
{
    const uint16_t *src; const uint8_t *alpha; const jint *cells, *anim;
    int sw, sh, tw, th, per_row, columns, c0, r0, c1, r1, x, y, r, c;
    DST_BEGIN(5);
    KNI_DeclareHandle(hsrc); KNI_DeclareHandle(halpha); KNI_DeclareHandle(hcells); KNI_DeclareHandle(hanim);
    KNI_GetParameterAsObject(8, hsrc); KNI_GetParameterAsObject(9, halpha);
    KNI_GetParameterAsObject(15, hcells); KNI_GetParameterAsObject(23, hanim);
    src = (const uint16_t *)SNI_GetRawArrayPointer((jarray)hsrc);
    alpha = KNI_IsNullHandle(halpha) ? NULL : (const uint8_t *)SNI_GetRawArrayPointer((jarray)halpha);
    cells = (const jint *)SNI_GetRawArrayPointer((jarray)hcells);
    anim = KNI_IsNullHandle(hanim) ? NULL : (const jint *)SNI_GetRawArrayPointer((jarray)hanim);
    sw = P(10); sh = P(11); tw = P(12); th = P(13); per_row = P(14);
    columns = P(16); c0 = P(17); r0 = P(18); c1 = P(19); r1 = P(20); x = P(21); y = P(22);
    for (r = r0; r < r1; r++) {
        for (c = c0; c < c1; c++) {
            int t = cells[r * columns + c];
            if (t < 0) t = anim ? anim[-t - 1] : 0;
            if (t <= 0) continue;
            t--;
            gfx_blit(&dst, src, alpha, sw, sh, (t % per_row) * tw, (t / per_row) * th, tw, th, 0,
                     x + c * tw, y + r * th);
        }
    }
    KNI_EndHandles();
    KNI_ReturnVoid();
}

/* drawRGB(dst.., int[] rgb, off, scan, x, y, w, h, processAlpha) */
KNIEXPORT KNI_RETURNTYPE_VOID Java_aj_Native_drawRGB(void)
{
    DST_BEGIN(2);
    KNI_DeclareHandle(hrgb);
    KNI_GetParameterAsObject(8, hrgb);
    gfx_draw_argb(&dst, (const uint32_t *)SNI_GetRawArrayPointer((jarray)hrgb),
                  P(9), P(10), P(11), P(12), P(13), P(14), P(15));
    KNI_EndHandles();
    KNI_ReturnVoid();
}

static const GfxFont *font_of(int idx)
{
    if (idx < 0 || idx >= GFX_FONT_COUNT) idx = 6;
    return &gfx_fonts[idx];
}

/* int drawChars(dst.., rgb, font, char[] text, off, len, x, y) */
KNIEXPORT KNI_RETURNTYPE_INT Java_aj_Native_drawChars(void)
{
    int adv;
    DST_BEGIN(2);
    KNI_DeclareHandle(htext);
    KNI_GetParameterAsObject(10, htext);
    adv = gfx_text(&dst, font_of(P(9)),
                   (const uint16_t *)SNI_GetRawArrayPointer((jarray)htext) + P(11), P(12),
                   P(13), P(14), GFX_RGB565(P(8)));
    KNI_EndHandles();
    KNI_ReturnInt(adv);
}

/* int charsWidth(font, char[] text, off, len) */
KNIEXPORT KNI_RETURNTYPE_INT Java_aj_Native_charsWidth(void)
{
    int w;
    KNI_StartHandles(1);
    KNI_DeclareHandle(htext);
    KNI_GetParameterAsObject(2, htext);
    w = gfx_text_width(font_of(P(1)),
                       (const uint16_t *)SNI_GetRawArrayPointer((jarray)htext) + P(3), P(4));
    KNI_EndHandles();
    KNI_ReturnInt(w);
}

/* Strings are copied out in chunks: no Java allocation, any length. */
#define STR_CHUNK 128

/* int drawString(dst.., rgb, font, String text, off, len, x, y) */
KNIEXPORT KNI_RETURNTYPE_INT Java_aj_Native_drawString(void)
{
    jchar buf[STR_CHUNK];
    int off, len, x, adv = 0;
    const GfxFont *f;
    uint16_t c;
    DST_BEGIN(2);
    KNI_DeclareHandle(htext);
    KNI_GetParameterAsObject(10, htext);
    f = font_of(P(9)); c = GFX_RGB565(P(8));
    off = P(11); len = P(12); x = P(13);
    while (len > 0) {
        int n = len > STR_CHUNK ? STR_CHUNK : len;
        KNI_GetStringRegion((jstring)htext, off, n, buf);
        adv += gfx_text(&dst, f, (const uint16_t *)buf, n, x + adv, P(14), c);
        off += n; len -= n;
    }
    KNI_EndHandles();
    KNI_ReturnInt(adv);
}

/* int stringWidth(font, String text, off, len) */
KNIEXPORT KNI_RETURNTYPE_INT Java_aj_Native_stringWidth(void)
{
    jchar buf[STR_CHUNK];
    int off, len, w = 0;
    const GfxFont *f = font_of(P(1));
    KNI_StartHandles(1);
    KNI_DeclareHandle(htext);
    KNI_GetParameterAsObject(2, htext);
    off = P(3); len = P(4);
    while (len > 0) {
        int n = len > STR_CHUNK ? STR_CHUNK : len;
        KNI_GetStringRegion((jstring)htext, off, n, buf);
        w += gfx_text_width(f, (const uint16_t *)buf, n);
        off += n; len -= n;
    }
    KNI_EndHandles();
    KNI_ReturnInt(w);
}

/* int fontInfo(font): height << 8 | ascent */
KNIEXPORT KNI_RETURNTYPE_INT Java_aj_Native_fontInfo(void)
{
    const GfxFont *f = font_of(P(1));
    KNI_ReturnInt((f->height << 8) | f->ascent);
}

/* int fromRGB(int[] rgb, off, n, short[] px, byte[] alpha): 1 = has translucency */
KNIEXPORT KNI_RETURNTYPE_INT Java_aj_Native_fromRGB(void)
{
    int r;
    KNI_StartHandles(3);
    KNI_DeclareHandle(hrgb);
    KNI_DeclareHandle(hpx);
    KNI_DeclareHandle(halpha);
    KNI_GetParameterAsObject(1, hrgb);
    KNI_GetParameterAsObject(4, hpx);
    KNI_GetParameterAsObject(5, halpha);
    r = gfx_from_argb((const uint32_t *)SNI_GetRawArrayPointer((jarray)hrgb) + P(2), P(3),
                      (uint16_t *)SNI_GetRawArrayPointer((jarray)hpx),
                      KNI_IsNullHandle(halpha) ? NULL : (uint8_t *)SNI_GetRawArrayPointer((jarray)halpha));
    KNI_EndHandles();
    KNI_ReturnInt(r);
}

/* toRGB(short[] px, byte[] alpha, sw, int[] rgb, off, scan, x, y, w, h) */
KNIEXPORT KNI_RETURNTYPE_VOID Java_aj_Native_toRGB(void)
{
    KNI_StartHandles(3);
    KNI_DeclareHandle(hpx);
    KNI_DeclareHandle(halpha);
    KNI_DeclareHandle(hrgb);
    KNI_GetParameterAsObject(1, hpx);
    KNI_GetParameterAsObject(2, halpha);
    KNI_GetParameterAsObject(4, hrgb);
    gfx_to_argb((const uint16_t *)SNI_GetRawArrayPointer((jarray)hpx),
                KNI_IsNullHandle(halpha) ? NULL : (const uint8_t *)SNI_GetRawArrayPointer((jarray)halpha),
                P(3), (uint32_t *)SNI_GetRawArrayPointer((jarray)hrgb),
                P(5), P(6), P(7), P(8), P(9), P(10));
    KNI_EndHandles();
    KNI_ReturnVoid();
}

/* ---- image decoding: decodeBegin keeps the planes until decodeEnd ---- */
static uint16_t *s_dec_px;
static uint8_t  *s_dec_alpha;
static int       s_dec_n;

/* int decodeBegin(byte[] data, off, len): w | h << 15 | hasAlpha << 30, 0 = failed */
KNIEXPORT KNI_RETURNTYPE_INT Java_aj_Native_decodeBegin(void)
{
    int w = 0, h = 0, ok;
    KNI_StartHandles(1);
    KNI_DeclareHandle(hdata);
    KNI_GetParameterAsObject(1, hdata);
    free(s_dec_px); free(s_dec_alpha); s_dec_px = NULL; s_dec_alpha = NULL;
    ok = gfx_decode((const uint8_t *)SNI_GetRawArrayPointer((jarray)hdata) + P(2), P(3),
                    &w, &h, &s_dec_px, &s_dec_alpha);
    KNI_EndHandles();
    if (!ok || w <= 0 || h <= 0 || w > 0x7FFF || h > 0x7FFF) {
        free(s_dec_px); free(s_dec_alpha); s_dec_px = NULL; s_dec_alpha = NULL;
        KNI_ReturnInt(0);
    }
    s_dec_n = w * h;
    KNI_ReturnInt(w | (h << 15) | (s_dec_alpha ? (1 << 30) : 0));
}

/* decodeEnd(short[] px, byte[] alpha): copies the planes and frees them */
KNIEXPORT KNI_RETURNTYPE_VOID Java_aj_Native_decodeEnd(void)
{
    KNI_StartHandles(2);
    KNI_DeclareHandle(hpx);
    KNI_DeclareHandle(halpha);
    KNI_GetParameterAsObject(1, hpx);
    KNI_GetParameterAsObject(2, halpha);
    if (s_dec_px && !KNI_IsNullHandle(hpx))
        memcpy(SNI_GetRawArrayPointer((jarray)hpx), s_dec_px, (size_t)s_dec_n * 2);
    if (s_dec_alpha && !KNI_IsNullHandle(halpha))
        memcpy(SNI_GetRawArrayPointer((jarray)halpha), s_dec_alpha, (size_t)s_dec_n);
    KNI_EndHandles();
    free(s_dec_px); free(s_dec_alpha); s_dec_px = NULL; s_dec_alpha = NULL;
    KNI_ReturnVoid();
}

/* ---- display and input ---- */

/* Integer scaling: the game's picture is shown s times as large (pixels
 * repeated), for cards without small screen modes. */
static int s_scale = 1, s_src_w, s_src_h;
static uint16_t *s_scaled;

/* int videoOpen(w, h, scale) */
KNIEXPORT KNI_RETURNTYPE_INT Java_aj_Native_videoOpen(void)
{
    int w = P(1), h = P(2), scale = P(3);
    if (scale < 1) scale = 1;
    if (scale > 4) scale = 4;
    free(s_scaled); s_scaled = NULL;
    if (scale > 1) {
        s_scaled = (uint16_t *)malloc((size_t)w * scale * h * scale * 2);
        if (!s_scaled) scale = 1;
    }
    s_scale = scale; s_src_w = w; s_src_h = h;
    KNI_ReturnInt(hal_video_open(w * scale, h * scale));
}

KNIEXPORT KNI_RETURNTYPE_VOID Java_aj_Native_videoClose(void)
{
    hal_video_close();
    free(s_scaled); s_scaled = NULL; s_scale = 1;
    KNI_ReturnVoid();
}

/* present(short[] px, w, h, x, y, rw, rh) */
KNIEXPORT KNI_RETURNTYPE_VOID Java_aj_Native_present(void)
{
    HalRect r;
    int w, h;
    KNI_StartHandles(1);
    KNI_DeclareHandle(hpx);
    KNI_GetParameterAsObject(1, hpx);
    w = P(2); h = P(3);
    r.x = P(4); r.y = P(5); r.w = P(6); r.h = P(7);
    if (r.x < 0) { r.w += r.x; r.x = 0; }
    if (r.y < 0) { r.h += r.y; r.y = 0; }
    if (r.x + r.w > w) r.w = w - r.x;
    if (r.y + r.h > h) r.h = h - r.y;
    if (r.w > 0 && r.h > 0) {
        const uint16_t *px = (const uint16_t *)SNI_GetRawArrayPointer((jarray)hpx);
        if (s_scale == 1 || !s_scaled || w != s_src_w || h != s_src_h) {
            hal_video_present(px, w * 2, &r, 1);
        } else {
            int s = s_scale, dw = w * s, x, y, k;
            for (y = r.y; y < r.y + r.h; y++) {
                const uint16_t *sp = px + (size_t)y * w + r.x;
                uint16_t *row = s_scaled + (size_t)y * s * dw + (size_t)r.x * s, *dp = row;
                for (x = 0; x < r.w; x++) { uint16_t c = sp[x]; for (k = 0; k < s; k++) *dp++ = c; }
                for (k = 1; k < s; k++) memcpy(row + (size_t)k * dw, row, (size_t)r.w * s * 2);
            }
            r.x *= s; r.y *= s; r.w *= s; r.h *= s;
            hal_video_present(s_scaled, dw * 2, &r, 1);
        }
    }
    KNI_EndHandles();
    KNI_ReturnVoid();
}

/* int pollEvent(int[] ev): ev = {type, code, down, x, y, ch}; 0 = none */
KNIEXPORT KNI_RETURNTYPE_INT Java_aj_Native_pollEvent(void)
{
    HalEvent e;
    int got;
    memset(&e, 0, sizeof(e));
    got = hal_poll_event(&e);
    if (got) {
        KNI_StartHandles(1);
        KNI_DeclareHandle(hev);
        KNI_GetParameterAsObject(1, hev);
        {
            jint *p = (jint *)SNI_GetRawArrayPointer((jarray)hev);
            p[0] = e.type; p[1] = e.code; p[2] = e.down; p[3] = e.x / s_scale; p[4] = e.y / s_scale; p[5] = e.ch;
        }
        KNI_EndHandles();
    }
    KNI_ReturnInt(got);
}

KNIEXPORT KNI_RETURNTYPE_VOID Java_aj_Native_waitEvent(void)
{
    hal_wait_event((uint32_t)P(1));
    KNI_ReturnVoid();
}

/* int screenshot(): host test hook, writes $AJ_SHOT; -1 elsewhere */
KNIEXPORT KNI_RETURNTYPE_INT Java_aj_Native_screenshot(void)
{
    const char *path = getenv("AJ_SHOT");
    KNI_ReturnInt(path ? hal_video_screenshot(path) : -1);
}
