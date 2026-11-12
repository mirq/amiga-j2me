/* KNI natives of aj.Native: the 3D rasteriser (native/g3d). GPLv2.
 * The index layout of the int[] and float[] parameters is in aj/Native.java. */
#include <stdlib.h>
#include <string.h>
#include "kni.h"
#include "sni.h"
#include "g3d.h"
#include "gfx.h"

#define RAW(h) (KNI_IsNullHandle(h) ? NULL : SNI_GetRawArrayPointer((jarray)(h)))

static int bind_target(jobject hdst, const jint *i)
{
    if (g3d_bind((uint16_t *)SNI_GetRawArrayPointer((jarray)hdst), i[0], i[1], i[2], i[3], i[4], i[5]) != 0) return 0;
    g3d_viewport(i[6], i[7], i[8], i[9]);
    return 1;
}

/* g3dClear(short[] dst, int[] i, int argb, int flags) */
KNIEXPORT KNI_RETURNTYPE_VOID Java_aj_Native_g3dClear(void)
{
    int flags = KNI_GetParameterAsInt(4);
    KNI_StartHandles(2);
    KNI_DeclareHandle(hdst);
    KNI_DeclareHandle(hi);
    KNI_GetParameterAsObject(1, hdst);
    KNI_GetParameterAsObject(2, hi);
    if (bind_target(hdst, (const jint *)SNI_GetRawArrayPointer((jarray)hi)))
        g3d_clear((uint32_t)KNI_GetParameterAsInt(3), flags & 1, flags & 2);
    KNI_EndHandles();
    KNI_ReturnVoid();
}

/* g3dDraw(dst, i, f, pos, nrm, col, tc, idx, tex, texAlpha) */
KNIEXPORT KNI_RETURNTYPE_VOID Java_aj_Native_g3dDraw(void)
{
    G3dDraw d;
    const jint *i;
    const jfloat *f;
    KNI_StartHandles(10);
    KNI_DeclareHandle(hdst); KNI_DeclareHandle(hi); KNI_DeclareHandle(hf);
    KNI_DeclareHandle(hpos); KNI_DeclareHandle(hnrm); KNI_DeclareHandle(hcol);
    KNI_DeclareHandle(htc); KNI_DeclareHandle(hidx); KNI_DeclareHandle(htex); KNI_DeclareHandle(hta);
    KNI_GetParameterAsObject(1, hdst); KNI_GetParameterAsObject(2, hi); KNI_GetParameterAsObject(3, hf);
    KNI_GetParameterAsObject(4, hpos); KNI_GetParameterAsObject(5, hnrm); KNI_GetParameterAsObject(6, hcol);
    KNI_GetParameterAsObject(7, htc); KNI_GetParameterAsObject(8, hidx); KNI_GetParameterAsObject(9, htex);
    KNI_GetParameterAsObject(10, hta);
    i = (const jint *)SNI_GetRawArrayPointer((jarray)hi);
    f = (const jfloat *)SNI_GetRawArrayPointer((jarray)hf);
    if (bind_target(hdst, i) && !KNI_IsNullHandle(hpos) && !KNI_IsNullHandle(hidx)) {
        memset(&d, 0, sizeof(d));
        d.mv = f; d.proj = f + 16;
        d.flags = i[10]; d.vertex_count = i[11];
        d.pos = RAW(hpos); d.pos_type = i[13]; d.pos_scale = f[32]; d.pos_bias = f + 33;
        d.nrm = RAW(hnrm); d.nrm_type = i[14];
        d.col = (const uint8_t *)RAW(hcol); d.col_comps = i[15]; d.def_color = (uint32_t)i[16];
        d.tc = RAW(htc); d.tc_type = i[17]; d.tc_comps = i[18]; d.tc_scale = f[36]; d.tc_bias = f + 37;
        d.tex_xf = f + 40;
        d.idx = (const int32_t *)RAW(hidx); d.idx_count = i[12];
        d.tex = (const uint16_t *)RAW(htex); d.tex_alpha = (const uint8_t *)RAW(hta);
        d.tw = i[19]; d.th = i[20]; d.tex_blend = i[21]; d.tex_blend_color = (uint32_t)i[22];
        d.wrap_s = i[23]; d.wrap_t = i[24];
        d.blend = i[25]; d.alpha_threshold = i[26];
        d.lights = f + 72; d.nlights = i[27] > G3D_MAX_LIGHTS ? G3D_MAX_LIGHTS : i[27];
        d.material = f + 56;
        d.fog_mode = i[28]; d.fog_color = (uint32_t)i[29]; d.fog_a = f[70]; d.fog_b = f[71];
        if (!d.tc || d.tw <= 0 || d.th <= 0) d.tex = NULL;
        g3d_draw(&d);
    }
    KNI_EndHandles();
    KNI_ReturnVoid();
}

/* g3dFigure(dst, i, pos, nrm, params, tris, opts, tex, texAlpha)
   opts: vertices, triangles, pattern mask, face, blending on, pass, reuse, texture w, h */
KNIEXPORT KNI_RETURNTYPE_VOID Java_aj_Native_g3dFigure(void)
{
    G3dFigure f;
    const jint *i, *o;
    KNI_StartHandles(9);
    KNI_DeclareHandle(hdst); KNI_DeclareHandle(hi); KNI_DeclareHandle(hpos); KNI_DeclareHandle(hnrm);
    KNI_DeclareHandle(hpar); KNI_DeclareHandle(htris); KNI_DeclareHandle(hopts); KNI_DeclareHandle(htex);
    KNI_DeclareHandle(hta);
    KNI_GetParameterAsObject(1, hdst); KNI_GetParameterAsObject(2, hi); KNI_GetParameterAsObject(3, hpos);
    KNI_GetParameterAsObject(4, hnrm); KNI_GetParameterAsObject(5, hpar); KNI_GetParameterAsObject(6, htris);
    KNI_GetParameterAsObject(7, hopts); KNI_GetParameterAsObject(8, htex); KNI_GetParameterAsObject(9, hta);
    i = (const jint *)SNI_GetRawArrayPointer((jarray)hi);
    if (bind_target(hdst, i) && !KNI_IsNullHandle(hpos) && !KNI_IsNullHandle(htris) && !KNI_IsNullHandle(hpar)
        && !KNI_IsNullHandle(hopts) && KNI_GetArrayLength((jarray)hopts) >= 9
        && KNI_GetArrayLength((jarray)hpar) >= G3D_FIG_PARAMS) {
        o = (const jint *)SNI_GetRawArrayPointer((jarray)hopts);
        memset(&f, 0, sizeof(f));
        f.pos = (const float *)RAW(hpos); f.nrm = (const float *)RAW(hnrm);
        f.params = (const float *)RAW(hpar);
        f.tris = (const int32_t *)RAW(htris);
        f.nverts = o[0]; f.ntris = o[1]; f.pattern_mask = o[2]; f.face = o[3];
        f.blend_enabled = o[4]; f.pass = o[5]; f.reuse = o[6]; f.tw = o[7]; f.th = o[8];
        f.tex = (const uint16_t *)RAW(htex); f.tex_alpha = (const uint8_t *)RAW(hta);
        if (f.nverts * 3 <= KNI_GetArrayLength((jarray)hpos)
            && (!f.nrm || f.nverts * 3 <= KNI_GetArrayLength((jarray)hnrm))
            && f.ntris * G3D_FIG_INTS <= KNI_GetArrayLength((jarray)htris)
            && (!f.tex || f.tw * f.th <= KNI_GetArrayLength((jarray)htex))
            && (!f.tex_alpha || f.tw * f.th <= KNI_GetArrayLength((jarray)hta)))
            g3d_figure(&f);
    }
    KNI_EndHandles();
    KNI_ReturnVoid();
}

/* byte[] inflate(byte[] src, int off, int len, int outLen) */
KNIEXPORT KNI_RETURNTYPE_OBJECT Java_aj_Native_inflate(void)
{
    int off = KNI_GetParameterAsInt(2), len = KNI_GetParameterAsInt(3), out_len = KNI_GetParameterAsInt(4);
    uint8_t *tmp = (out_len >= 0 && out_len < (64 << 20)) ? (uint8_t *)malloc((size_t)(out_len ? out_len : 1)) : NULL;
    int got = -1;
    KNI_StartHandles(2);
    KNI_DeclareHandle(hsrc);
    KNI_DeclareHandle(hout);
    KNI_GetParameterAsObject(1, hsrc);
    if (tmp) got = gfx_inflate((const uint8_t *)SNI_GetRawArrayPointer((jarray)hsrc) + off, len, tmp, out_len);
    if (got == out_len) {
        SNI_NewArray(SNI_BYTE_ARRAY, out_len, hout);      /* may move src: not used after this */
        if (!KNI_IsNullHandle(hout) && out_len > 0) memcpy(SNI_GetRawArrayPointer((jarray)hout), tmp, (size_t)out_len);
    }
    free(tmp);
    KNI_EndHandlesAndReturnObject(hout);
}
