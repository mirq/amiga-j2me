/* amiga-j2me 3D rasteriser (see g3d.h). GPLv2. */
#include "g3d.h"
#include <math.h>
#include <stdlib.h>
#include <string.h>

/* x^e for x in 0..1 and e >= 0, and e^x, without libm: pow() never returns on
   WarpOS, and both are slow on a 68060. Exact for whole exponents; the
   fraction is a straight line between the two nearest whole powers, which is
   plenty for a specular highlight. */
static float g3d_pow01(float x, float e)
{
    int n = (int)e;
    float frac = e - (float)n, r = 1.0f, b = x;
    if (x <= 0.0f) return e > 0.0f ? 0.0f : 1.0f;
    while (n > 0) {
        if (n & 1) r *= b;
        b *= b;
        n >>= 1;
    }
    return r * (1.0f + frac * (x - 1.0f));
}

/* ceil() to int without libm */
static inline int iceil(float v)
{
    int i = (int)v;
    return v > (float)i ? i + 1 : i;
}

static float g3d_exp(float x)
{
    int i;
    float r;
    if (x < -40.0f) return 0.0f;
    if (x > 40.0f) x = 40.0f;
    r = 1.0f + x * (1.0f / 1024.0f);            /* (1 + x/1024)^1024 */
    for (i = 0; i < 10; i++) r *= r;
    return r;
}

typedef struct { float x, y, z, w; float r, g, b, a; float u, v; float f; /* fog: 1 = clear, 0 = all fog */ } Vtx;

static uint16_t *s_px;
static int s_w, s_h;
static int s_cx0, s_cy0, s_cx1, s_cy1;      /* scissor */
static int s_vx, s_vy, s_vw, s_vh;          /* viewport */
static uint16_t *s_z;
static int s_zcap;
static Vtx *s_vtx;
static int s_vcap;

int g3d_bind(uint16_t *px, int w, int h, int clipx, int clipy, int clipw, int cliph)
{
    s_px = px; s_w = w; s_h = h;
    s_cx0 = clipx < 0 ? 0 : clipx; s_cy0 = clipy < 0 ? 0 : clipy;
    s_cx1 = clipx + clipw > w ? w : clipx + clipw;
    s_cy1 = clipy + cliph > h ? h : clipy + cliph;
    if (w * h > s_zcap) {
        free(s_z);
        s_z = (uint16_t *)malloc((size_t)w * h * 2);
        s_zcap = s_z ? w * h : 0;
        if (s_z) memset(s_z, 0xFF, (size_t)w * h * 2);
    }
    s_vx = 0; s_vy = 0; s_vw = w; s_vh = h;
    return s_z ? 0 : -1;
}

void g3d_viewport(int x, int y, int w, int h) { s_vx = x; s_vy = y; s_vw = w; s_vh = h; }

/* the area drawing may touch: scissor within the viewport */
static void bounds(int *x0, int *y0, int *x1, int *y1)
{
    *x0 = s_cx0 > s_vx ? s_cx0 : s_vx; *y0 = s_cy0 > s_vy ? s_cy0 : s_vy;
    *x1 = s_cx1 < s_vx + s_vw ? s_cx1 : s_vx + s_vw; *y1 = s_cy1 < s_vy + s_vh ? s_cy1 : s_vy + s_vh;
}

void g3d_clear(uint32_t argb, int clear_color, int clear_depth)
{
    int x0, y0, x1, y1, x, y;
    uint16_t c = (uint16_t)(((argb >> 8) & 0xF800) | ((argb >> 5) & 0x07E0) | ((argb >> 3) & 0x001F));
    if (!s_px || !s_z) return;
    bounds(&x0, &y0, &x1, &y1);
    for (y = y0; y < y1; y++) {
        if (clear_color) { uint16_t *p = s_px + (size_t)y * s_w; for (x = x0; x < x1; x++) p[x] = c; }
        if (clear_depth) memset(s_z + (size_t)y * s_w + x0, 0xFF, (size_t)(x1 > x0 ? x1 - x0 : 0) * 2);
    }
}

/* ---- transform and lighting ---- */

static inline float component(const void *a, int type, int i)
{
    return type == 1 ? (float)((const int8_t *)a)[i] : (float)((const int16_t *)a)[i];
}

static inline float clamp01(float v) { return v < 0.0f ? 0.0f : (v > 1.0f ? 1.0f : v); }

static void transform_vertices(const G3dDraw *d)
{
    const float *m = d->mv, *p = d->proj;
    int i, l;
    int lit = (d->flags & G3D_LIGHTING) && d->nrm && d->material;
    for (i = 0; i < d->vertex_count; i++) {
        Vtx *o = &s_vtx[i];
        float x = component(d->pos, d->pos_type, i * 3) * d->pos_scale + d->pos_bias[0];
        float y = component(d->pos, d->pos_type, i * 3 + 1) * d->pos_scale + d->pos_bias[1];
        float z = component(d->pos, d->pos_type, i * 3 + 2) * d->pos_scale + d->pos_bias[2];
        float ex = m[0] * x + m[1] * y + m[2] * z + m[3];
        float ey = m[4] * x + m[5] * y + m[6] * z + m[7];
        float ez = m[8] * x + m[9] * y + m[10] * z + m[11];
        float ew = m[12] * x + m[13] * y + m[14] * z + m[15];
        o->x = p[0] * ex + p[1] * ey + p[2] * ez + p[3] * ew;
        o->y = p[4] * ex + p[5] * ey + p[6] * ez + p[7] * ew;
        o->z = p[8] * ex + p[9] * ey + p[10] * ez + p[11] * ew;
        o->w = p[12] * ex + p[13] * ey + p[14] * ez + p[15] * ew;

        /* base colour */
        {
            float cr, cg, cb, ca;
            if (d->col) {
                const uint8_t *c = d->col + i * d->col_comps;
                cr = c[0] * (1.0f / 255); cg = c[1] * (1.0f / 255); cb = c[2] * (1.0f / 255);
                ca = d->col_comps == 4 ? c[3] * (1.0f / 255) : 1.0f;
            } else {
                cr = ((d->def_color >> 16) & 255) * (1.0f / 255); cg = ((d->def_color >> 8) & 255) * (1.0f / 255);
                cb = (d->def_color & 255) * (1.0f / 255); ca = (d->def_color >> 24) * (1.0f / 255);
            }
            if (!lit) { o->r = cr; o->g = cg; o->b = cb; o->a = ca; }
            else {
                const float *mat = d->material;
                float ar = mat[0], ag = mat[1], ab = mat[2];
                float dr = mat[3], dg = mat[4], db = mat[5], da = mat[6];
                float nx = component(d->nrm, d->nrm_type, i * 3), ny = component(d->nrm, d->nrm_type, i * 3 + 1),
                      nz = component(d->nrm, d->nrm_type, i * 3 + 2);
                float tx = m[0] * nx + m[1] * ny + m[2] * nz, ty = m[4] * nx + m[5] * ny + m[6] * nz,
                      tz = m[8] * nx + m[9] * ny + m[10] * nz;
                float len = (float)sqrt(tx * tx + ty * ty + tz * tz);
                float r, g, b;
                if (len > 0.0f) { tx /= len; ty /= len; tz /= len; }
                if (d->flags & G3D_COLOR_TRACK) { ar = dr = cr; ag = dg = cg; ab = db = cb; da = ca; }
                r = mat[7]; g = mat[8]; b = mat[9];       /* emissive */
                for (l = 0; l < d->nlights; l++) {
                    const float *L = d->lights + l * G3D_LIGHT_FLOATS;
                    int type = (int)L[0];
                    float lx, ly, lz, ndl, att = 1.0f;
                    if (type == G3D_LIGHT_AMBIENT) { r += ar * L[1]; g += ag * L[2]; b += ab * L[3]; continue; }
                    if (type == G3D_LIGHT_DIRECTIONAL) { lx = L[4]; ly = L[5]; lz = L[6]; }
                    else {
                        float dist;
                        lx = L[4] - ex; ly = L[5] - ey; lz = L[6] - ez;
                        dist = (float)sqrt(lx * lx + ly * ly + lz * lz);
                        if (dist > 0.0f) { lx /= dist; ly /= dist; lz /= dist; }
                        att = L[7] + L[8] * dist + L[9] * dist * dist;
                        att = att > 0.0f ? 1.0f / att : 1.0f;
                    }
                    ndl = tx * lx + ty * ly + tz * lz;
                    if (ndl < 0.0f) ndl = (d->flags & G3D_TWO_SIDED) ? -ndl : 0.0f;
                    ndl *= att;
                    r += dr * L[1] * ndl; g += dg * L[2] * ndl; b += db * L[3] * ndl;
                    if (ndl > 0.0f && mat[13] > 0.0f && (mat[10] > 0.0f || mat[11] > 0.0f || mat[12] > 0.0f)) {
                        /* Blinn highlight, viewer at infinity along +Z of eye space */
                        float hx = lx, hy = ly, hz = lz + 1.0f;
                        float hl = (float)sqrt(hx * hx + hy * hy + hz * hz);
                        if (hl > 0.0f) {
                            float ndh = (tx * hx + ty * hy + tz * hz) / hl;
                            if (ndh > 0.0f) {
                                float sp = g3d_pow01(ndh, mat[13]) * att;
                                r += mat[10] * L[1] * sp; g += mat[11] * L[2] * sp; b += mat[12] * L[3] * sp;
                            }
                        }
                    }
                }
                o->r = clamp01(r); o->g = clamp01(g); o->b = clamp01(b); o->a = da;
            }
        }

        if (d->fog_mode == 1) {
            float span = d->fog_b - d->fog_a;
            o->f = span != 0.0f ? clamp01((d->fog_b + ez) / span) : 1.0f;      /* distance = -ez */
        } else if (d->fog_mode == 2) {
            o->f = clamp01(g3d_exp(d->fog_a * ez));
        } else o->f = 1.0f;

        if (d->tc) {
            float u = component(d->tc, d->tc_type, i * d->tc_comps) * d->tc_scale + d->tc_bias[0];
            float v = component(d->tc, d->tc_type, i * d->tc_comps + 1) * d->tc_scale + d->tc_bias[1];
            if (d->flags & G3D_TEX_XFORM) {
                const float *t = d->tex_xf;
                float tu = t[0] * u + t[1] * v + t[3], tv = t[4] * u + t[5] * v + t[7];
                u = tu; v = tv;
            }
            o->u = u * d->tw; o->v = v * d->th;       /* texel units */
        } else { o->u = 0; o->v = 0; }
    }
}

/* ---- clipping against the view frustum (in clip space) ---- */

static inline float plane_dist(const Vtx *v, int plane)
{
    switch (plane) {
    case 0: return v->w + v->z;     /* near */
    case 1: return v->w - v->z;     /* far */
    case 2: return v->w + v->x;
    case 3: return v->w - v->x;
    case 4: return v->w + v->y;
    default: return v->w - v->y;
    }
}

static int clip_poly(Vtx *in, int n, Vtx *tmp)
{
    int plane, i;
    Vtx *src = in, *dst = tmp;
    for (plane = 0; plane < 6; plane++) {
        int m = 0;
        for (i = 0; i < n; i++) {
            const Vtx *a = &src[i], *b = &src[(i + 1) % n];
            float da = plane_dist(a, plane), db = plane_dist(b, plane);
            if (da >= 0) dst[m++] = *a;
            if ((da >= 0) != (db >= 0)) {
                float t = da / (da - db);
                Vtx *o = &dst[m++];
                o->x = a->x + (b->x - a->x) * t; o->y = a->y + (b->y - a->y) * t;
                o->z = a->z + (b->z - a->z) * t; o->w = a->w + (b->w - a->w) * t;
                o->r = a->r + (b->r - a->r) * t; o->g = a->g + (b->g - a->g) * t;
                o->b = a->b + (b->b - a->b) * t; o->a = a->a + (b->a - a->a) * t;
                o->u = a->u + (b->u - a->u) * t; o->v = a->v + (b->v - a->v) * t;
                o->f = a->f + (b->f - a->f) * t;
            }
        }
        n = m;
        if (n < 3) return 0;
        { Vtx *t = src; src = dst; dst = t; }
    }
    if (src != in) memcpy(in, src, sizeof(Vtx) * (size_t)n);
    return n;
}

/* ---- rasteriser ---- */

typedef struct { float x, y, z, iw, r, g, b, a, uw, vw, f; } SV;   /* screen vertex; uw = u/w, vw = v/w, iw = 1/w */

static const G3dDraw *s_d;

static inline void shade_pixel(uint16_t *dp, uint16_t *zp, uint32_t z, int r, int g, int b, int a, int tu, int tv, int fog)
{
    const G3dDraw *d = s_d;
    if (d->flags & G3D_DEPTH_TEST) { if ((z >> 16) > *zp) return; }
    if (d->tex) {
        int tr, tg, tb, ta = 255;
        uint16_t t;
        int ti;
        if (d->wrap_s) tu &= d->tw - 1; else tu = tu < 0 ? 0 : (tu >= d->tw ? d->tw - 1 : tu);
        if (d->wrap_t) tv &= d->th - 1; else tv = tv < 0 ? 0 : (tv >= d->th ? d->th - 1 : tv);
        ti = tv * d->tw + tu;
        t = d->tex[ti];
        tr = (t >> 8) & 0xF8; tg = (t >> 3) & 0xFC; tb = (t << 3) & 0xF8;
        tr |= tr >> 5; tg |= tg >> 6; tb |= tb >> 5;
        if (d->tex_alpha) ta = d->tex_alpha[ti];
        switch (d->tex_blend) {
        case G3D_TEX_REPLACE: r = tr; g = tg; b = tb; if (d->tex_alpha) a = ta; break;
        case G3D_TEX_MODULATE: r = (r * tr) >> 8; g = (g * tg) >> 8; b = (b * tb) >> 8; a = (a * (ta + 1)) >> 8; break;
        case G3D_TEX_DECAL: r += ((tr - r) * ta) >> 8; g += ((tg - g) * ta) >> 8; b += ((tb - b) * ta) >> 8; break;
        case G3D_TEX_ADD: r += tr; g += tg; b += tb; if (r > 255) r = 255; if (g > 255) g = 255; if (b > 255) b = 255;
                          a = (a * (ta + 1)) >> 8; break;
        default: {      /* BLEND with the texture's blend colour */
            int cr = (d->tex_blend_color >> 16) & 255, cg = (d->tex_blend_color >> 8) & 255, cb = d->tex_blend_color & 255;
            r += ((cr - r) * tr) >> 8; g += ((cg - g) * tg) >> 8; b += ((cb - b) * tb) >> 8; a = (a * (ta + 1)) >> 8; } break;
        }
    }
    if (a < d->alpha_threshold) return;
    if (d->fog_mode && fog < 255) {
        int fr = (d->fog_color >> 16) & 255, fg = (d->fog_color >> 8) & 255, fb = d->fog_color & 255;
        r = fr + (((r - fr) * fog) >> 8); g = fg + (((g - fg) * fog) >> 8); b = fb + (((b - fb) * fog) >> 8);
    }
    if (!(d->flags & G3D_NO_COLOR)) {
        if (d->blend != G3D_BLEND_REPLACE) {
            uint16_t o = *dp;
            int dr = (o >> 8) & 0xF8, dg = (o >> 3) & 0xFC, db = (o << 3) & 0xF8;
            switch (d->blend) {
            case G3D_BLEND_ALPHA: r = dr + (((r - dr) * a) >> 8); g = dg + (((g - dg) * a) >> 8); b = db + (((b - db) * a) >> 8); break;
            case G3D_BLEND_ALPHA_ADD: r = dr + ((r * a) >> 8); g = dg + ((g * a) >> 8); b = db + ((b * a) >> 8); break;
            case G3D_BLEND_SUB: r = dr - ((r * a) >> 8); g = dg - ((g * a) >> 8); b = db - ((b * a) >> 8);
                if (r < 0) r = 0;
                if (g < 0) g = 0;
                if (b < 0) b = 0;
                break;
            case G3D_BLEND_MODULATE: r = (r * dr) >> 8; g = (g * dg) >> 8; b = (b * db) >> 8; break;
            default: r = (r * dr) >> 7; g = (g * dg) >> 7; b = (b * db) >> 7; break;
            }
            if (r > 255) r = 255;
            if (g > 255) g = 255;
            if (b > 255) b = 255;
        }
        *dp = (uint16_t)(((r & 0xF8) << 8) | ((g & 0xFC) << 3) | (b >> 3));
    }
    if (d->flags & G3D_DEPTH_WRITE) *zp = (uint16_t)(z >> 16);
}

#define SPAN 16     /* pixels between exact perspective divisions */

static void tri(const SV *a, const SV *b, const SV *c)
{
    const SV *t;
    float area, inv;
    /* gradients of every interpolated quantity over the screen */
    float dzx, dzy, dix, diy, drx, dry, dgx, dgy, dbx, dby, dax, day, dux, duy, dvx, dvy, dfx, dfy;
    int y, y_start, y_end, x0b, y0b, x1b, y1b;
    int textured = s_d->tex != NULL, persp = textured && (s_d->flags & G3D_PERSPECTIVE);

    if (a->y > b->y) { t = a; a = b; b = t; }
    if (b->y > c->y) { t = b; b = c; c = t; }
    if (a->y > b->y) { t = a; a = b; b = t; }

    area = (b->x - a->x) * (c->y - a->y) - (c->x - a->x) * (b->y - a->y);
    /* slivers: their gradients are huge and they cover no pixel centre worth
       drawing (with the 68881's extended precision a degenerate triangle's
       area comes out tiny instead of exactly zero) */
    if (area > -1e-3f && area < 1e-3f) return;
    inv = 1.0f / area;
#define GRAD(f, gx, gy) \
    gx = ((b->f - a->f) * (c->y - a->y) - (c->f - a->f) * (b->y - a->y)) * inv; \
    gy = ((c->f - a->f) * (b->x - a->x) - (b->f - a->f) * (c->x - a->x)) * inv
    GRAD(z, dzx, dzy); GRAD(iw, dix, diy);
    GRAD(r, drx, dry); GRAD(g, dgx, dgy); GRAD(b, dbx, dby); GRAD(a, dax, day);
    GRAD(uw, dux, duy); GRAD(vw, dvx, dvy); GRAD(f, dfx, dfy);
#undef GRAD

    bounds(&x0b, &y0b, &x1b, &y1b);
    y_start = iceil(a->y - 0.5f); y_end = iceil(c->y - 0.5f);
    if (y_start < y0b) y_start = y0b;
    if (y_end > y1b) y_end = y1b;

    for (y = y_start; y < y_end; y++) {
        float py = y + 0.5f, xl, xr, px, dy, dx0;
        float z, iw, r, g, bl, al, uw, vw, fg;
        int xs, xe, n;
        uint16_t *dp, *zp;
        /* long edge a-c, short edges a-b / b-c */
        xl = a->x + (c->x - a->x) * (py - a->y) / (c->y - a->y);
        if (py < b->y) xr = (b->y == a->y) ? b->x : a->x + (b->x - a->x) * (py - a->y) / (b->y - a->y);
        else           xr = (c->y == b->y) ? b->x : b->x + (c->x - b->x) * (py - b->y) / (c->y - b->y);
        if (xl > xr) { float s = xl; xl = xr; xr = s; }
        xs = iceil(xl - 0.5f); xe = iceil(xr - 0.5f);
        if (xs < x0b) xs = x0b;
        if (xe > x1b) xe = x1b;
        n = xe - xs;
        if (n <= 0) continue;
        px = xs + 0.5f; dx0 = px - a->x; dy = py - a->y;
        z = a->z + dzx * dx0 + dzy * dy;  iw = a->iw + dix * dx0 + diy * dy;
        r = a->r + drx * dx0 + dry * dy;  g = a->g + dgx * dx0 + dgy * dy;
        bl = a->b + dbx * dx0 + dby * dy; al = a->a + dax * dx0 + day * dy;
        uw = a->uw + dux * dx0 + duy * dy; vw = a->vw + dvx * dx0 + dvy * dy;
        fg = a->f + dfx * dx0 + dfy * dy;
        dp = s_px + (size_t)y * s_w + xs; zp = s_z + (size_t)y * s_w + xs;
        {
            /* fixed point per pixel: z 16.16 (unsigned), colours and texels 16.16 */
            uint32_t zf = (uint32_t)(z < 0 ? 0 : (z > 65535.0f ? 65535.0f : z) * 65536.0f);
            int32_t dzf = (int32_t)(dzx * 65536.0f);
            int32_t rf = (int32_t)(r * 65536.0f), gf = (int32_t)(g * 65536.0f), bf = (int32_t)(bl * 65536.0f), af = (int32_t)(al * 65536.0f);
            int32_t drf = (int32_t)(drx * 65536.0f), dgf = (int32_t)(dgx * 65536.0f), dbf = (int32_t)(dbx * 65536.0f), daf = (int32_t)(dax * 65536.0f);
            int32_t uf = 0, vf = 0, duf = 0, dvf = 0;
            int32_t ff = (int32_t)(fg * 255.0f * 65536.0f), dff = (int32_t)(dfx * 255.0f * 65536.0f);
            int left = n;
            if (textured && !persp) {
                /* affine: u/w and v/w carry w = 1 (set by the caller) */
                uf = (int32_t)(uw * 65536.0f); vf = (int32_t)(vw * 65536.0f);
                duf = (int32_t)(dux * 65536.0f); dvf = (int32_t)(dvx * 65536.0f);
            }
            while (left > 0) {
                int span = left > SPAN ? SPAN : left, k;
                if (persp) {
                    float w0 = 1.0f / iw, iw1 = iw + dix * span, w1 = 1.0f / iw1;
                    float u0 = uw * w0, v0 = vw * w0;
                    float u1 = (uw + dux * span) * w1, v1 = (vw + dvx * span) * w1;
                    uf = (int32_t)(u0 * 65536.0f); vf = (int32_t)(v0 * 65536.0f);
                    duf = (int32_t)((u1 - u0) * 65536.0f) / span; dvf = (int32_t)((v1 - v0) * 65536.0f) / span;
                    uw += dux * span; vw += dvx * span; iw = iw1;
                }
                for (k = 0; k < span; k++) {
                    int ri = rf >> 16, gi = gf >> 16, bi = bf >> 16, ai = af >> 16;
                    if (ri < 0) ri = 0; else if (ri > 255) ri = 255;
                    if (gi < 0) gi = 0; else if (gi > 255) gi = 255;
                    if (bi < 0) bi = 0; else if (bi > 255) bi = 255;
                    if (ai < 0) ai = 0; else if (ai > 255) ai = 255;
                    {
                        int fi = ff >> 16;
                        if (fi < 0) fi = 0; else if (fi > 255) fi = 255;
                        shade_pixel(dp, zp, zf, ri, gi, bi, ai, uf >> 16, vf >> 16, fi);
                    }
                    dp++; zp++;
                    zf += (uint32_t)dzf; rf += drf; gf += dgf; bf += dbf; af += daf; uf += duf; vf += dvf; ff += dff;
                }
                left -= span;
            }
        }
    }
}

/* clips one triangle (poly[0..2], clip space) and draws what is left of it */
static void draw_poly(const G3dDraw *d, Vtx *poly)
{
    Vtx tmp[12];
    SV sv[12];
    int k, n;
    float ax, ay, bx, by, cx, cy, area;
    int front;
    n = clip_poly(poly, 3, tmp);
    if (n < 3) return;
    for (k = 0; k < n; k++) {
        float iw = 1.0f / poly[k].w;
        sv[k].x = s_vx + (poly[k].x * iw * 0.5f + 0.5f) * s_vw;
        sv[k].y = s_vy + (0.5f - poly[k].y * iw * 0.5f) * s_vh;
        sv[k].z = (poly[k].z * iw * 0.5f + 0.5f) * 65535.0f;
        sv[k].f = poly[k].f;
        sv[k].r = poly[k].r * 255.0f; sv[k].g = poly[k].g * 255.0f;
        sv[k].b = poly[k].b * 255.0f; sv[k].a = poly[k].a * 255.0f;
        if (d->flags & G3D_PERSPECTIVE) { sv[k].iw = iw; sv[k].uw = poly[k].u * iw; sv[k].vw = poly[k].v * iw; }
        else { sv[k].iw = 1.0f; sv[k].uw = poly[k].u; sv[k].vw = poly[k].v; }
    }
    /* facing, from the screen-space winding (y grows downwards) */
    ax = sv[0].x; ay = sv[0].y; bx = sv[1].x; by = sv[1].y; cx = sv[2].x; cy = sv[2].y;
    area = (bx - ax) * (cy - ay) - (cx - ax) * (by - ay);
    front = (area < 0.0f) != ((d->flags & G3D_WINDING_CW) != 0);    /* counter-clockwise = front by default */
    if ((d->flags & G3D_CULL_BACK) && !front) return;
    if ((d->flags & G3D_CULL_FRONT) && front) return;
    for (k = 1; k + 1 < n; k++) tri(&sv[0], &sv[k], &sv[k + 1]);
}

void g3d_draw(const G3dDraw *d)
{
    int i;
    Vtx poly[12];
    if (!s_px || !s_z || d->vertex_count <= 0) return;
    if (d->vertex_count > s_vcap) {
        free(s_vtx);
        s_vtx = (Vtx *)malloc(sizeof(Vtx) * (size_t)d->vertex_count);
        s_vcap = s_vtx ? d->vertex_count : 0;
        if (!s_vtx) return;
    }
    s_d = d;
    transform_vertices(d);
    for (i = 0; i + 2 < d->idx_count; i += 3) {
        int i0 = d->idx[i], i1 = d->idx[i + 1], i2 = d->idx[i + 2];
        if ((unsigned)i0 >= (unsigned)d->vertex_count || (unsigned)i1 >= (unsigned)d->vertex_count ||
            (unsigned)i2 >= (unsigned)d->vertex_count) continue;
        poly[0] = s_vtx[i0]; poly[1] = s_vtx[i1]; poly[2] = s_vtx[i2];
        if (d->flags & G3D_FLAT) {
            poly[0].r = poly[1].r = poly[2].r; poly[0].g = poly[1].g = poly[2].g;
            poly[0].b = poly[1].b = poly[2].b; poly[0].a = poly[1].a = poly[2].a;
        }
        draw_poly(d, poly);
    }
}

/* ---- Mascot Capsule figures ---- */

static float *s_fig_light;
static int s_fig_cap;

void g3d_figure(const G3dFigure *f)
{
    const float *p = f->params;
    G3dDraw d;
    Vtx poly[12];
    int i, t, persp = p[12] != 0.0f;
    int lit_ok = f->nrm && p[19] >= 0.0f;
    if (!s_px || !s_z || f->nverts <= 0 || s_vw <= 0 || s_vh <= 0) return;
    if (f->nverts > s_vcap) {
        free(s_vtx);
        s_vtx = (Vtx *)malloc(sizeof(Vtx) * (size_t)f->nverts);
        s_vcap = s_vtx ? f->nverts : 0;
        if (!s_vtx) return;
    }
    if (f->nverts > s_fig_cap) {
        free(s_fig_light);
        s_fig_light = (float *)malloc(sizeof(float) * (size_t)f->nverts);
        s_fig_cap = s_fig_light ? f->nverts : 0;
        if (!s_fig_light) return;
    }
    if (!f->reuse) {
        float iw = 1.0f / (float)s_vw, ih = 1.0f / (float)s_vh;
        float kx = 2.0f * p[15] * iw - 1.0f, ky = 1.0f - 2.0f * p[16] * ih;
        float fx = 2.0f * p[13] * iw, fy = 2.0f * p[14] * ih;
        float zn = p[17], zf = p[18], zs = 0.0f, zt = 0.0f;
        if (persp && zf > zn) { zs = (zf + zn) / (zf - zn); zt = -2.0f * zf * zn / (zf - zn); }
        for (i = 0; i < f->nverts; i++) {
            Vtx *o = &s_vtx[i];
            float x = f->pos[i * 3], y = f->pos[i * 3 + 1], z = f->pos[i * 3 + 2];
            float ex = p[0] * x + p[1] * y + p[2] * z + p[3];
            float ey = p[4] * x + p[5] * y + p[6] * z + p[7];
            float ez = p[8] * x + p[9] * y + p[10] * z + p[11];
            if (persp) {
                o->x = kx * ez + fx * ex; o->y = ky * ez - fy * ey;
                o->z = zs * ez + zt; o->w = ez;
            } else {
                o->x = kx + fx * ex; o->y = ky - fy * ey;
                o->z = ez * (1.0f / 65536.0f); o->w = 1.0f;
            }
            o->f = 1.0f;
            if (lit_ok) {
                float nx = f->nrm[i * 3], ny = f->nrm[i * 3 + 1], nz = f->nrm[i * 3 + 2];
                float tx = p[0] * nx + p[1] * ny + p[2] * nz;
                float ty = p[4] * nx + p[5] * ny + p[6] * nz;
                float tz = p[8] * nx + p[9] * ny + p[10] * nz;
                float len = (float)sqrt(tx * tx + ty * ty + tz * tz), l = 0.0f;
                if (len > 0.0f) l = (tx * p[21] + ty * p[22] + tz * p[23]) / len;
                if (l < 0.0f) l = 0.0f;
                l = p[19] + p[20] * l;
                if (l > 1.0f) l = 1.0f;
                if (p[24] >= 0.0f) l = l < p[24] ? p[26] : p[25];
                s_fig_light[i] = l;
            }
        }
    }
    memset(&d, 0, sizeof(d));
    d.tw = f->tw; d.th = f->th;
    d.tex_blend = G3D_TEX_MODULATE;
    s_d = &d;
    for (t = 0; t < f->ntris; t++) {
        const int32_t *r = f->tris + t * G3D_FIG_INTS;
        int flags = r[6], blend = f->blend_enabled ? (flags & G3D_FIG_BLEND_MASK) : 0;
        int textured = r[8] >= 0, k;
        if (r[8] != f->face || (r[7] & f->pattern_mask) != r[7]) continue;
        if ((blend != 0) != (f->pass != 0)) continue;
        if ((unsigned)r[0] >= (unsigned)f->nverts || (unsigned)r[1] >= (unsigned)f->nverts ||
            (unsigned)r[2] >= (unsigned)f->nverts) continue;
        if (textured && (!f->tex || f->tw <= 0 || f->th <= 0)) continue;
        for (k = 0; k < 3; k++) {
            Vtx *v = &poly[k];
            float l = (lit_ok && (flags & G3D_FIG_LIGHTING)) ? s_fig_light[r[k]] : 1.0f;
            *v = s_vtx[r[k]];
            if (textured) {
                v->r = v->g = v->b = l;
                v->u = (float)(r[3 + k] & 0xFFFF); v->v = (float)((r[3 + k] >> 16) & 0xFFFF);
            } else {
                v->r = ((r[3] >> 16) & 255) * (1.0f / 255) * l;
                v->g = ((r[3] >> 8) & 255) * (1.0f / 255) * l;
                v->b = (r[3] & 255) * (1.0f / 255) * l;
                v->u = v->v = 0.0f;
            }
            v->a = blend == 2 ? 0.5f : 1.0f;
        }
        d.tex = textured ? f->tex : NULL;
        d.tex_alpha = (textured && (flags & G3D_FIG_COLORKEY)) ? f->tex_alpha : NULL;
        d.alpha_threshold = d.tex_alpha ? 64 : 0;
        d.blend = blend == 2 ? G3D_BLEND_ALPHA : (blend == 4 ? G3D_BLEND_ALPHA_ADD : (blend == 6 ? G3D_BLEND_SUB : G3D_BLEND_REPLACE));
        d.flags = G3D_DEPTH_TEST | (blend ? 0 : G3D_DEPTH_WRITE) | ((textured && persp) ? G3D_PERSPECTIVE : 0)
                | ((flags & G3D_FIG_TWO_SIDED) ? 0 : (G3D_CULL_BACK | G3D_WINDING_CW));
        draw_poly(&d, poly);
    }
}
