/* amiga-j2me 2D rasteriser: R5G6B5 surfaces (native-endian 16-bit words),
 * optional 8-bit alpha plane per image. Plain C, no OS calls. GPLv2. */
#ifndef AJ_GFX_H
#define AJ_GFX_H
#include <stddef.h>
#include <stdint.h>

#ifdef __cplusplus
extern "C" {
#endif

/* Destination surface with its clip rectangle (x1/y1 exclusive). gfx_dst_init
 * clamps the clip to the surface, so the primitives only test the clip. */
typedef struct GfxDst {
    uint16_t *px;
    int w, h;
    int cx0, cy0, cx1, cy1;
} GfxDst;

void gfx_dst_init(GfxDst *d, uint16_t *px, int w, int h,
                  int clipx, int clipy, int clipw, int cliph);

#define GFX_RGB565(rgb) ((uint16_t)((((rgb) >> 8) & 0xF800) | (((rgb) >> 5) & 0x07E0) | (((rgb) >> 3) & 0x001F)))
/* 565 -> 0x00RRGGBB with the low bits replicated */
uint32_t gfx_565_to_rgb(uint16_t p);

void gfx_fill_rect(const GfxDst *d, int x, int y, int w, int h, uint16_t c);
/* dotted: MIDP DOTTED stroke (every other pixel) */
void gfx_line(const GfxDst *d, int x0, int y0, int x1, int y1, uint16_t c, int dotted);
void gfx_fill_triangle(const GfxDst *d, int x0, int y0, int x1, int y1, int x2, int y2, uint16_t c);
/* Elliptical arc in the box x,y,w,h; angles in degrees as in MIDP
 * (0 = 3 o'clock, positive = counter-clockwise). */
void gfx_arc(const GfxDst *d, int x, int y, int w, int h, int start, int sweep,
             int fill, uint16_t c, int dotted);

/* MIDP Sprite transforms */
enum {
    GFX_TRANS_NONE = 0, GFX_TRANS_MIRROR_ROT180 = 1, GFX_TRANS_MIRROR = 2,
    GFX_TRANS_ROT180 = 3, GFX_TRANS_MIRROR_ROT270 = 4, GFX_TRANS_ROT90 = 5,
    GFX_TRANS_ROT270 = 6, GFX_TRANS_MIRROR_ROT90 = 7
};

/* Copies the w x h region at sx,sy of a source image (alpha may be NULL =
 * opaque), transformed, with its top-left corner at dx,dy. */
void gfx_blit(const GfxDst *d, const uint16_t *src, const uint8_t *alpha,
              int sw, int sh, int sx, int sy, int w, int h,
              int transform, int dx, int dy);

/* 0xAARRGGBB pixels (scanlength may be negative) to the surface. */
void gfx_draw_argb(const GfxDst *d, const uint32_t *argb, int offset, int scanlength,
                   int x, int y, int w, int h, int process_alpha);
/* 0xAARRGGBB -> pixels (+ alpha plane when alpha != NULL). Returns 1 when
 * any pixel is not fully opaque. */
int  gfx_from_argb(const uint32_t *argb, int n, uint16_t *px, uint8_t *alpha);
void gfx_to_argb(const uint16_t *px, const uint8_t *alpha, int sw,
                 uint32_t *argb, int offset, int scanlength,
                 int x, int y, int w, int h);

/* ---- fonts ---- */
typedef struct { uint16_t code; uint16_t width; uint32_t first_row; } GfxGlyph;
typedef struct GfxFont {
    int height, ascent, count;
    const GfxGlyph *glyphs;     /* sorted by code */
    const uint16_t *rows;       /* height rows per glyph, MSB = left pixel */
} GfxFont;

/* index = size * 2 + bold; size 0 = small, 1 = medium, 2 = large */
#define GFX_FONT_COUNT 6
extern const GfxFont gfx_fonts[GFX_FONT_COUNT];

int  gfx_char_width(const GfxFont *f, unsigned ch);
int  gfx_text_width(const GfxFont *f, const uint16_t *text, int n);
/* x,y = top-left of the text line. Returns the advance in pixels. */
int  gfx_text(const GfxDst *d, const GfxFont *f, const uint16_t *text, int n,
              int x, int y, uint16_t c);

/* ---- image files (PNG, JPEG, GIF, BMP) ---- */
/* Decodes into newly malloc'd planes (*alpha is NULL for an opaque image).
 * Returns 1 on success; free the planes with free(). */
int gfx_decode(const uint8_t *data, int len, int *w, int *h,
               uint16_t **px, uint8_t **alpha);

#ifdef __cplusplus
}
#endif
#endif
