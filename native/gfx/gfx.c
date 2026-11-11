/* amiga-j2me 2D rasteriser (see gfx.h). GPLv2. */
#include "gfx.h"
#include <stdlib.h>
#include <string.h>

void gfx_dst_init(GfxDst *d, uint16_t *px, int w, int h,
                  int clipx, int clipy, int clipw, int cliph)
{
    int x1 = clipx + clipw, y1 = clipy + cliph;
    d->px = px; d->w = w; d->h = h;
    d->cx0 = clipx < 0 ? 0 : clipx;
    d->cy0 = clipy < 0 ? 0 : clipy;
    d->cx1 = x1 > w ? w : x1;
    d->cy1 = y1 > h ? h : y1;
    if (d->cx1 < d->cx0) d->cx1 = d->cx0;
    if (d->cy1 < d->cy0) d->cy1 = d->cy0;
}

uint32_t gfx_565_to_rgb(uint16_t p)
{
    uint32_t r = (p >> 11) & 31, g = (p >> 5) & 63, b = p & 31;
    return ((r << 3 | r >> 2) << 16) | ((g << 2 | g >> 4) << 8) | (b << 3 | b >> 2);
}

static inline void fill_span(uint16_t *p, int n, uint16_t c)
{
    while (n >= 4) { p[0] = c; p[1] = c; p[2] = c; p[3] = c; p += 4; n -= 4; }
    while (n-- > 0) *p++ = c;
}

/* clipped horizontal span x0..x1 inclusive */
static inline void hspan(const GfxDst *d, int x0, int x1, int y, uint16_t c)
{
    if (y < d->cy0 || y >= d->cy1) return;
    if (x0 < d->cx0) x0 = d->cx0;
    if (x1 >= d->cx1) x1 = d->cx1 - 1;
    if (x0 <= x1) fill_span(d->px + (size_t)y * d->w + x0, x1 - x0 + 1, c);
}

static inline void pset(const GfxDst *d, int x, int y, uint16_t c)
{
    if (x >= d->cx0 && x < d->cx1 && y >= d->cy0 && y < d->cy1)
        d->px[(size_t)y * d->w + x] = c;
}

void gfx_fill_rect(const GfxDst *d, int x, int y, int w, int h, uint16_t c)
{
    int x1, y1;
    if (w <= 0 || h <= 0) return;
    x1 = x + w; y1 = y + h;
    if (x < d->cx0) x = d->cx0;
    if (y < d->cy0) y = d->cy0;
    if (x1 > d->cx1) x1 = d->cx1;
    if (y1 > d->cy1) y1 = d->cy1;
    if (x >= x1) return;
    for (; y < y1; y++) fill_span(d->px + (size_t)y * d->w + x, x1 - x, c);
}

void gfx_line(const GfxDst *d, int x0, int y0, int x1, int y1, uint16_t c, int dotted)
{
    int dx, dy, sx, sy, err, n = 0;
    if (!dotted) {
        if (y0 == y1) { if (x0 > x1) { int t = x0; x0 = x1; x1 = t; } hspan(d, x0, x1, y0, c); return; }
        if (x0 == x1) {
            if (y0 > y1) { int t = y0; y0 = y1; y1 = t; }
            if (x0 < d->cx0 || x0 >= d->cx1) return;
            if (y0 < d->cy0) y0 = d->cy0;
            if (y1 >= d->cy1) y1 = d->cy1 - 1;
            for (; y0 <= y1; y0++) d->px[(size_t)y0 * d->w + x0] = c;
            return;
        }
    }
    dx = abs(x1 - x0); dy = -abs(y1 - y0);
    sx = x0 < x1 ? 1 : -1; sy = y0 < y1 ? 1 : -1;
    err = dx + dy;
    for (;;) {
        if (!dotted || !(n++ & 1)) pset(d, x0, y0, c);
        if (x0 == x1 && y0 == y1) break;
        { int e2 = 2 * err;
          if (e2 >= dy) { err += dy; x0 += sx; }
          if (e2 <= dx) { err += dx; y0 += sy; } }
    }
}

void gfx_fill_triangle(const GfxDst *d, int x0, int y0, int x1, int y1, int x2, int y2, uint16_t c)
{
    int y, t;
    /* sort by y */
    if (y0 > y1) { t = y0; y0 = y1; y1 = t; t = x0; x0 = x1; x1 = t; }
    if (y1 > y2) { t = y1; y1 = y2; y2 = t; t = x1; x1 = x2; x2 = t; }
    if (y0 > y1) { t = y0; y0 = y1; y1 = t; t = x0; x0 = x1; x1 = t; }
    if (y0 == y2) {             /* degenerate: one row */
        int a = x0, b = x0;
        if (x1 < a) a = x1; if (x2 < a) a = x2;
        if (x1 > b) b = x1; if (x2 > b) b = x2;
        hspan(d, a, b, y0, c);
        return;
    }
    for (y = (y0 < d->cy0 ? d->cy0 : y0); y <= y2 && y < d->cy1; y++) {
        /* long edge 0-2, short edges 0-1 / 1-2; 16.16 would overflow less,
         * but 64-bit keeps it exact for any int coordinates */
        int xa = x0 + (int)(((int64_t)(x2 - x0) * (y - y0)) / (y2 - y0));
        int xb;
        if (y < y1)        xb = x0 + (int)(((int64_t)(x1 - x0) * (y - y0)) / (y1 - y0));
        else if (y1 == y2) xb = x1;
        else               xb = x1 + (int)(((int64_t)(x2 - x1) * (y - y1)) / (y2 - y1));
        if (xa > xb) { t = xa; xa = xb; xb = t; }
        hspan(d, xa, xb, y, c);
    }
}

/* ---- arcs ---- */

/* sin(deg) * 16384 for 0..90 */
static const int16_t sin_tab[91] = {
    0,286,572,857,1143,1428,1713,1997,2280,2563,2845,3126,3406,3686,3964,4240,
    4516,4790,5063,5334,5604,5872,6138,6402,6664,6924,7182,7438,7692,7943,8192,
    8438,8682,8923,9162,9397,9630,9860,10087,10311,10531,10749,10963,11174,11381,
    11585,11786,11982,12176,12365,12551,12733,12911,13085,13255,13421,13583,13741,
    13894,14044,14189,14330,14466,14598,14726,14849,14968,15082,15191,15296,15396,
    15491,15582,15668,15749,15826,15897,15964,16026,16083,16135,16182,16225,16262,
    16294,16322,16344,16362,16374,16382,16384
};

static int isin(int deg)
{
    deg %= 360; if (deg < 0) deg += 360;
    if (deg <= 90)  return sin_tab[deg];
    if (deg <= 180) return sin_tab[180 - deg];
    if (deg <= 270) return -sin_tab[deg - 180];
    return -sin_tab[360 - deg];
}
static int icos(int deg) { return isin(deg + 90); }

/* Is the direction (vx, vy), y up, inside the arc from `start` sweeping
 * `sweep` (> 0, < 360) degrees counter-clockwise? Cross-product tests against
 * the two boundary rays, scaled for the ellipse by the caller. */
static int in_sweep(int64_t vx, int64_t vy, int64_t ax, int64_t ay, int64_t bx, int64_t by, int sweep)
{
    int64_t ca = ax * vy - ay * vx;   /* > 0: v is counter-clockwise of a */
    int64_t cb = vx * by - vy * bx;   /* > 0: b is counter-clockwise of v */
    if (sweep <= 180) return ca >= 0 && cb >= 0;
    return ca >= 0 || cb >= 0;
}

void gfx_arc(const GfxDst *d, int x, int y, int w, int h, int start, int sweep,
             int fill, uint16_t c, int dotted)
{
    int full, px, py, n = 0;
    int64_t a2, b2, ax = 0, ay = 0, bx = 0, by = 0;
    int cx2, cy2;               /* centre * 2 (boxes may have odd sizes) */

    if (w < 0 || h < 0 || sweep == 0) return;
    if (fill) { if (w == 0 || h == 0) return; }
    else { w++; h++; }          /* an outline covers w+1 x h+1 pixels */
    if (sweep < 0) { start += sweep; sweep = -sweep; }
    full = sweep >= 360;
    if (!full) {
        /* boundary rays on the ellipse (not the circle): scale by the radii */
        ax = (int64_t)icos(start) * w;          ay = (int64_t)isin(start) * h;
        bx = (int64_t)icos(start + sweep) * w;  by = (int64_t)isin(start + sweep) * h;
    }
    a2 = (int64_t)w * w; b2 = (int64_t)h * h;
    cx2 = 2 * x + w - 1; cy2 = 2 * y + h - 1;

    /* Scan the box; a pixel centre (px, py) is inside when
     * (2px-cx2)^2 * h^2 + (2py-cy2)^2 * w^2 <= w^2 h^2 (coordinates doubled). */
    for (py = y; py < y + h; py++) {
        int64_t ddy = 2 * py - cy2, rem;
        int span0 = -1, span1 = -1, lx, rx;
        if (py < d->cy0 || py >= d->cy1) continue;
        rem = a2 * b2 - ddy * ddy * a2;
        if (rem < 0) continue;
        /* leftmost inside pixel on this row */
        lx = x; rx = x + w - 1;
        while (lx <= rx) { int64_t ddx = 2 * lx - cx2; if (ddx * ddx * b2 <= rem) break; lx++; }
        while (rx >= lx) { int64_t ddx = 2 * rx - cx2; if (ddx * ddx * b2 <= rem) break; rx--; }
        if (lx > rx) continue;
        if (fill && full) { hspan(d, lx, rx, py, c); continue; }
        for (px = lx; px <= rx; px++) {
            int on = 1;
            if (!fill) {
                /* outline: inside, with an outside 4-neighbour */
                int64_t ddx = 2 * px - cx2;
                int64_t l = (ddx - 2) * (ddx - 2) * b2, r = (ddx + 2) * (ddx + 2) * b2;
                int64_t u = (ddy - 2) * (ddy - 2) * a2, dn = (ddy + 2) * (ddy + 2) * a2;
                int64_t lim = a2 * b2;
                on = (l + ddy * ddy * a2 > lim) || (r + ddy * ddy * a2 > lim) ||
                     (ddx * ddx * b2 + u > lim) || (ddx * ddx * b2 + dn > lim);
            }
            if (on && !full)
                on = in_sweep((int64_t)(2 * px - cx2), -(int64_t)ddy, ax, ay, bx, by, sweep);
            if (!fill) {
                if (on && (!dotted || !(n++ & 1))) pset(d, px, py, c);
            } else if (on) {
                if (span0 < 0) span0 = px;
                span1 = px;
            } else if (span0 >= 0) {
                hspan(d, span0, span1, py, c); span0 = -1;
            }
        }
        if (fill && span0 >= 0) hspan(d, span0, span1, py, c);
    }
}

/* ---- images ---- */

static inline uint16_t blend565(uint16_t dst, uint16_t src, unsigned a /* 0..255 */)
{
    /* blend red+blue together and green apart, 5-bit alpha */
    unsigned a5 = (a + 4) >> 3;
    uint32_t drb = dst & 0xF81F, dg = dst & 0x07E0;
    uint32_t srb = src & 0xF81F, sg = src & 0x07E0;
    drb = ((srb * a5 + drb * (32 - a5)) >> 5) & 0xF81F;
    dg  = ((sg  * a5 + dg  * (32 - a5)) >> 5) & 0x07E0;
    return (uint16_t)(drb | dg);
}

void gfx_blit(const GfxDst *d, const uint16_t *src, const uint8_t *alpha,
              int sw, int sh, int sx, int sy, int w, int h,
              int transform, int dx, int dy)
{
    int tw, th, x0, y0, x1, y1, xx, yy;
    int stepx, stepy, base;     /* source index = base + i*stepx + j*stepy */

    if (w <= 0 || h <= 0) return;
    /* keep the source region inside the image */
    if (sx < 0) { w += sx; dx -= (transform == GFX_TRANS_NONE) ? sx : 0; sx = 0; }
    if (sy < 0) { h += sy; dy -= (transform == GFX_TRANS_NONE) ? sy : 0; sy = 0; }
    if (sx + w > sw) w = sw - sx;
    if (sy + h > sh) h = sh - sy;
    if (w <= 0 || h <= 0) return;

    switch (transform) {
    default:
    case GFX_TRANS_NONE:          tw = w; th = h; base = sy * sw + sx;                     stepx = 1;   stepy = sw;  break;
    case GFX_TRANS_MIRROR:        tw = w; th = h; base = sy * sw + sx + w - 1;             stepx = -1;  stepy = sw;  break;
    case GFX_TRANS_ROT180:        tw = w; th = h; base = (sy + h - 1) * sw + sx + w - 1;   stepx = -1;  stepy = -sw; break;
    case GFX_TRANS_MIRROR_ROT180: tw = w; th = h; base = (sy + h - 1) * sw + sx;           stepx = 1;   stepy = -sw; break;
    case GFX_TRANS_ROT90:         tw = h; th = w; base = (sy + h - 1) * sw + sx;           stepx = -sw; stepy = 1;   break;
    case GFX_TRANS_ROT270:        tw = h; th = w; base = sy * sw + sx + w - 1;             stepx = sw;  stepy = -1;  break;
    case GFX_TRANS_MIRROR_ROT90:  tw = h; th = w; base = (sy + h - 1) * sw + sx + w - 1;   stepx = -sw; stepy = -1;  break;
    case GFX_TRANS_MIRROR_ROT270: tw = h; th = w; base = sy * sw + sx;                     stepx = sw;  stepy = 1;   break;
    }

    x0 = dx; y0 = dy; x1 = dx + tw; y1 = dy + th;
    if (x0 < d->cx0) x0 = d->cx0;
    if (y0 < d->cy0) y0 = d->cy0;
    if (x1 > d->cx1) x1 = d->cx1;
    if (y1 > d->cy1) y1 = d->cy1;
    if (x0 >= x1 || y0 >= y1) return;

    for (yy = y0; yy < y1; yy++) {
        uint16_t *dp = d->px + (size_t)yy * d->w + x0;
        int si = base + (x0 - dx) * stepx + (yy - dy) * stepy;
        int n = x1 - x0;
        if (!alpha) {
            if (stepx == 1) memcpy(dp, src + si, (size_t)n * 2);
            else for (xx = 0; xx < n; xx++, si += stepx) dp[xx] = src[si];
        } else {
            for (xx = 0; xx < n; xx++, si += stepx) {
                unsigned a = alpha[si];
                if (a >= 0xF8) dp[xx] = src[si];
                else if (a >= 8) dp[xx] = blend565(dp[xx], src[si], a);
            }
        }
    }
}

static inline uint16_t argb_to_565(uint32_t p)
{
    return (uint16_t)(((p >> 8) & 0xF800) | ((p >> 5) & 0x07E0) | ((p >> 3) & 0x001F));
}

void gfx_draw_argb(const GfxDst *d, const uint32_t *argb, int offset, int scanlength,
                   int x, int y, int w, int h, int process_alpha)
{
    int x0 = x, y0 = y, x1 = x + w, y1 = y + h, xx, yy;
    if (x0 < d->cx0) x0 = d->cx0;
    if (y0 < d->cy0) y0 = d->cy0;
    if (x1 > d->cx1) x1 = d->cx1;
    if (y1 > d->cy1) y1 = d->cy1;
    if (x0 >= x1 || y0 >= y1) return;
    for (yy = y0; yy < y1; yy++) {
        const uint32_t *sp = argb + offset + (yy - y) * scanlength + (x0 - x);
        uint16_t *dp = d->px + (size_t)yy * d->w + x0;
        int n = x1 - x0;
        if (!process_alpha) {
            for (xx = 0; xx < n; xx++) dp[xx] = argb_to_565(sp[xx]);
        } else {
            for (xx = 0; xx < n; xx++) {
                uint32_t p = sp[xx];
                unsigned a = p >> 24;
                if (a >= 0xF8) dp[xx] = argb_to_565(p);
                else if (a >= 8) dp[xx] = blend565(dp[xx], argb_to_565(p), a);
            }
        }
    }
}

int gfx_from_argb(const uint32_t *argb, int n, uint16_t *px, uint8_t *alpha)
{
    int i, translucent = 0;
    for (i = 0; i < n; i++) {
        uint32_t p = argb[i];
        px[i] = argb_to_565(p);
        if (alpha) { alpha[i] = (uint8_t)(p >> 24); if ((p >> 24) != 0xFF) translucent = 1; }
    }
    return translucent;
}

void gfx_to_argb(const uint16_t *px, const uint8_t *alpha, int sw,
                 uint32_t *argb, int offset, int scanlength,
                 int x, int y, int w, int h)
{
    int xx, yy;
    for (yy = 0; yy < h; yy++) {
        const uint16_t *sp = px + (size_t)(y + yy) * sw + x;
        const uint8_t *ap = alpha ? alpha + (size_t)(y + yy) * sw + x : NULL;
        uint32_t *dp = argb + offset + yy * scanlength;
        for (xx = 0; xx < w; xx++)
            dp[xx] = gfx_565_to_rgb(sp[xx]) | ((uint32_t)(ap ? ap[xx] : 0xFF) << 24);
    }
}

/* ---- text ---- */

static const GfxGlyph *find_glyph(const GfxFont *f, unsigned ch)
{
    int lo = 0, hi = f->count - 1;
    if (ch >= 0x20 && ch <= 0x7E) return &f->glyphs[ch - 0x20];   /* table starts with ASCII */
    while (lo <= hi) {
        int mid = (lo + hi) >> 1;
        unsigned c = f->glyphs[mid].code;
        if (c == ch) return &f->glyphs[mid];
        if (c < ch) lo = mid + 1; else hi = mid - 1;
    }
    return NULL;
}

int gfx_char_width(const GfxFont *f, unsigned ch)
{
    const GfxGlyph *g = find_glyph(f, ch);
    if (!g) g = find_glyph(f, '?');
    return g ? g->width : 0;
}

int gfx_text_width(const GfxFont *f, const uint16_t *text, int n)
{
    int i, w = 0;
    for (i = 0; i < n; i++) w += gfx_char_width(f, text[i]);
    return w;
}

int gfx_text(const GfxDst *d, const GfxFont *f, const uint16_t *text, int n,
             int x, int y, uint16_t c)
{
    int i, x_start = x;
    for (i = 0; i < n; i++) {
        const GfxGlyph *g = find_glyph(f, text[i]);
        int gy, y0 = y, y1 = y + f->height;
        if (!g) g = find_glyph(f, '?');
        if (!g) continue;
        if (x + 16 > d->cx0 && x < d->cx1) {
            const uint16_t *rows = f->rows + g->first_row;
            if (y0 < d->cy0) y0 = d->cy0;
            if (y1 > d->cy1) y1 = d->cy1;
            for (gy = y0; gy < y1; gy++) {
                unsigned bits = rows[gy - y];
                uint16_t *dp = d->px + (size_t)gy * d->w;
                int gx = x;
                for (; bits; bits = (bits << 1) & 0xFFFF, gx++)
                    if ((bits & 0x8000) && gx >= d->cx0 && gx < d->cx1) dp[gx] = c;
            }
        }
        x += g->width;
    }
    return x - x_start;
}
