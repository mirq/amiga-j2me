/* amiga-j2me 3D rasteriser for the M3G implementation: transform, per-vertex
 * lighting, frustum clipping and textured, Gouraud-shaded, depth-buffered
 * triangles on an R5G6B5 surface. Plain C (floats for setup, fixed point per
 * pixel), no OS calls. GPLv2. */
#ifndef AJ_G3D_H
#define AJ_G3D_H
#include <stddef.h>
#include <stdint.h>

#ifdef __cplusplus
extern "C" {
#endif

/* flags */
#define G3D_CULL_BACK    0x0001
#define G3D_CULL_FRONT   0x0002
#define G3D_WINDING_CW   0x0004
#define G3D_DEPTH_TEST   0x0008
#define G3D_DEPTH_WRITE  0x0010
#define G3D_LIGHTING     0x0020
#define G3D_FLAT         0x0040   /* flat shading: the last vertex's colour */
#define G3D_PERSPECTIVE  0x0080   /* perspective-correct texturing */
#define G3D_TWO_SIDED    0x0100
#define G3D_TEX_XFORM    0x0200   /* texture matrix given */
#define G3D_COLOR_TRACK  0x0400   /* vertex colours replace the material's diffuse (and ambient) */
#define G3D_NO_COLOR     0x0800   /* colour writes off (depth only) */

enum { G3D_BLEND_REPLACE = 0, G3D_BLEND_ALPHA, G3D_BLEND_ALPHA_ADD, G3D_BLEND_MODULATE, G3D_BLEND_MODULATE_X2, G3D_BLEND_SUB };
enum { G3D_TEX_REPLACE = 0, G3D_TEX_MODULATE, G3D_TEX_DECAL, G3D_TEX_ADD, G3D_TEX_BLEND };
enum { G3D_LIGHT_AMBIENT = 0, G3D_LIGHT_DIRECTIONAL, G3D_LIGHT_OMNI, G3D_LIGHT_SPOT };

#define G3D_LIGHT_FLOATS 12     /* type, r, g, b, x, y, z (eye space: position, or direction TO the light), att const, linear, quadratic, 2 spare */
#define G3D_MAX_LIGHTS   8

typedef struct G3dDraw {
    const float *mv, *proj;         /* 4x4, row-major (as M3G's Transform.get) */
    int flags;
    int vertex_count;
    /* positions: 3 components of byte (type 1) or short (type 2) */
    const void *pos; int pos_type; float pos_scale; const float *pos_bias;
    const void *nrm; int nrm_type;  /* normals, or NULL */
    const uint8_t *col; int col_comps;      /* vertex colours (3 or 4 bytes), or NULL */
    uint32_t def_color;             /* 0xAARRGGBB when there are no vertex colours */
    const void *tc; int tc_type, tc_comps; float tc_scale; const float *tc_bias;
    const float *tex_xf;            /* texture transform (4x4 row-major) */
    const int32_t *idx; int idx_count;      /* triangle list */
    /* texture: power-of-two size, R5G6B5 + optional alpha plane */
    const uint16_t *tex; const uint8_t *tex_alpha; int tw, th;
    int tex_blend; uint32_t tex_blend_color; int wrap_s, wrap_t;    /* wrap: 1 = repeat, 0 = clamp */
    int blend, alpha_threshold;
    const float *lights; int nlights;
    const float *material;          /* ambient rgb, diffuse rgba, emissive rgb, specular rgb, shininess (14 floats) */
    int fog_mode;                   /* 0 none, 1 linear (fog_a = near, fog_b = far), 2 exponential (fog_a = density) */
    uint32_t fog_color;
    float fog_a, fog_b;
} G3dDraw;

/* A Mascot Capsule figure: vertices already posed (floats, model space),
 * triangles as records of G3D_FIG_INTS ints: 3 vertex indices, 3 x (u | v << 16)
 * in texels (textured) or the 0xRRGGBB colour in the first of the three,
 * flags, pattern bit, texture number (-1 = coloured). */
#define G3D_FIG_INTS        9
#define G3D_FIG_COLORKEY    0x01
#define G3D_FIG_BLEND_MASK  0x06    /* 2 half, 4 add, 6 subtract */
#define G3D_FIG_TWO_SIDED   0x10
#define G3D_FIG_LIGHTING    0x20
#define G3D_FIG_SPHERE      0x40    /* add the sphere map, looked up by the eye space normal */
/* params: view 3x4 row-major (0..11), 12 = 1 for perspective, 13/14 = pixels per
 * unit (at depth 1 when perspective), 15/16 = centre in the viewport, 17/18 = near/far,
 * 19 = ambient (< 0: unlit), 20 = directional intensity, 21..23 = direction to
 * the light in eye space, 24 = toon threshold (< 0: off), 25/26 = toon high/low */
#define G3D_FIG_PARAMS      27
typedef struct G3dFigure {
    const float *pos, *nrm; int nverts;
    const float *params;
    const int32_t *tris; int ntris;
    int pattern_mask, face, blend_enabled, pass, reuse;     /* pass 0: opaque, 1: blended */
    const uint16_t *tex; const uint8_t *tex_alpha; int tw, th;
    const uint16_t *sphere; int sw, sh;                     /* sphere map, or NULL */
} G3dFigure;
void g3d_figure(const G3dFigure *f);

/* Target surface and scissor (clip rectangle); allocates the depth buffer. 0 = ok. */
int  g3d_bind(uint16_t *px, int w, int h, int clipx, int clipy, int clipw, int cliph);
void g3d_viewport(int x, int y, int w, int h);
void g3d_clear(uint32_t argb, int clear_color, int clear_depth);
void g3d_draw(const G3dDraw *d);

#ifdef __cplusplus
}
#endif
#endif
