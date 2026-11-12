/*
 * The AGA screen: an 8-bit native chipset screen for machines (or users)
 * without an RTG card. amiga_video.c hands over to this file when the AGA
 * display was chosen (amiga_video_use_aga).
 *
 * The game draws R5G6B5. Each presented rectangle is turned into 8-bit
 * palette indices in a chunky buffer the size of the screen (a fixed palette:
 * 6 x 8 x 5 levels of red, green and blue, with a 2 x 2 ordered dither, four
 * 64 KB tables from R5G6B5 straight to the index), then Mikael Kalms'
 * c2p_rect (c2p/c2p_rect.s) writes the rectangle, widened to whole 32-pixel
 * columns, into the 8 bitplanes. The planes are one block of Chip RAM, equally
 * spaced as c2p_rect needs, given to Intuition with SA_BitMap (as in the
 * author's BobrHopper and OpenTTD ports).
 *
 * WarpOS: the PowerPC fills the chunky buffer, flushes it out of its cache and
 * has the 68k run c2p_rect (a copy of its bytes, c2p/c2p68k.h) through
 * Run68K: one call per rectangle, about half a millisecond on a real card.
 *
 * Modes: the smallest of PAL (else NTSC) lores, hires, lores laced and hires
 * laced that holds the picture.
 */
#include <stdlib.h>
#include <string.h>

#include "amiga_os.h"
#include "amiga_platform.h"

#pragma pack(push, 2)
#include <graphics/modeid.h>
#pragma pack(pop)

#if defined(AJ_AMIGA_WOS)
#include "c2p/c2p68k.h"
#endif

/* the c2p_rect arguments (c2p/c2p_glue.s, 68k) */
struct AjC2P
{
	UWORD x, y, w, h, cmod, bmod;
	ULONG bplsize;
	APTR  chunky, bpl;
};
#if !defined(AJ_AMIGA_WOS)
extern void aj_c2p_rect(const struct AjC2P *a);
#endif

#define LEVELS_R 6
#define LEVELS_G 8
#define LEVELS_B 5

static struct Screen * s_screen;
static struct Window * s_window;
static struct BitMap   s_bm;
static UBYTE *         s_planes;           /* Chip RAM, 8 planes back to back */
static ULONG           s_plsize;
static UBYTE *         s_chunky;           /* the screen, one byte a pixel */
static UBYTE *         s_lut;              /* 4 x 65536: R5G6B5 -> index, per dither cell */
static UWORD *         s_blankPointer;
static int             s_sw, s_sh;         /* the screen */
static int             s_width, s_height;  /* the game's picture */
static int             s_ox, s_oy;
#if defined(AJ_AMIGA_WOS)
static UBYTE *         s_c2pCode;          /* c2p_rect, where the 68k runs it */
#endif

struct Window *aga_window(void) { return s_window; }
void aga_offset(int *x, int *y) { *x = s_ox; *y = s_oy; }
int  aga_width(void)  { return s_width; }
int  aga_height(void) { return s_height; }

/* The mode for a picture of width x height: 0 = none fits. */
static ULONG choose_mode(int width, int height, int *sw, int *sh)
{
	static const ULONG keys[] = { LORES_KEY, HIRES_KEY, LORESLACE_KEY, HIRESLACE_KEY };
	static const ULONG monitors[] = { PAL_MONITOR_ID, NTSC_MONITOR_ID, DEFAULT_MONITOR_ID };
	int m, k;
	for (m = 0; m < 3; m++)
	{
		for (k = 0; k < 4; k++)
		{
			ULONG id = monitors[m] | keys[k];
			struct DimensionInfo dims;
			int w, h;
			if (ModeNotAvailable(id)) continue;
			if (!GetDisplayInfoData(NULL, (UBYTE *)&dims, sizeof(dims), DTAG_DIMS, id)) continue;
			if (dims.MaxDepth < 8) continue;
			w = dims.Nominal.MaxX - dims.Nominal.MinX + 1;
			h = dims.Nominal.MaxY - dims.Nominal.MinY + 1;
			w &= ~31;
			if (w >= width && h >= height)
			{
				*sw = w;
				*sh = h;
				return id;
			}
		}
	}
	return INVALID_ID;
}

/* index of palette entry (r, g, b levels) */
#define INDEX(r, g, b) (((r) * LEVELS_G + (g)) * LEVELS_B + (b))

static int build_lut(void)
{
	/* 2x2 ordered dither thresholds, in 1/4 steps */
	static const int bias[4] = { 0, 2, 3, 1 };
	int d;
	s_lut = (UBYTE *)malloc(4 * 65536);
	if (!s_lut) return 0;
	for (d = 0; d < 4; d++)
	{
		UBYTE *t = s_lut + d * 65536;
		int c;
		for (c = 0; c < 65536; c++)
		{
			int r8 = ((c >> 11) & 31) * 255 / 31;
			int g8 = ((c >> 5) & 63) * 255 / 63;
			int b8 = (c & 31) * 255 / 31;
			/* level = floor(v * (L-1) / 255 + threshold), threshold (bias+0.5)/4 */
			int r = (r8 * (LEVELS_R - 1) * 4 + 255 * bias[d] + 127) / (255 * 4);
			int g = (g8 * (LEVELS_G - 1) * 4 + 255 * bias[d] + 127) / (255 * 4);
			int b = (b8 * (LEVELS_B - 1) * 4 + 255 * bias[d] + 127) / (255 * 4);
			if (r > LEVELS_R - 1) r = LEVELS_R - 1;
			if (g > LEVELS_G - 1) g = LEVELS_G - 1;
			if (b > LEVELS_B - 1) b = LEVELS_B - 1;
			t[c] = (UBYTE)INDEX(r, g, b);
		}
	}
	return 1;
}

static void load_palette(void)
{
	static ULONG table[1 + 256 * 3 + 1];
	int r, g, b, i = 1;
	table[0] = (256UL << 16) | 0;
	memset(table + 1, 0, 256 * 3 * sizeof(ULONG));
	for (r = 0; r < LEVELS_R; r++)
		for (g = 0; g < LEVELS_G; g++)
			for (b = 0; b < LEVELS_B; b++)
			{
				int k = 1 + INDEX(r, g, b) * 3;
				table[k]     = (ULONG)(r * 255 / (LEVELS_R - 1)) * 0x01010101UL;
				table[k + 1] = (ULONG)(g * 255 / (LEVELS_G - 1)) * 0x01010101UL;
				table[k + 2] = (ULONG)(b * 255 / (LEVELS_B - 1)) * 0x01010101UL;
			}
	(void)i;
	table[1 + 256 * 3] = 0;
	LoadRGB32(&s_screen->ViewPort, table);
}

void aga_close(void)
{
	if (s_window)
	{
		ClearPointer(s_window);
		CloseWindow(s_window);
		s_window = NULL;
	}
	if (s_screen)
	{
		CloseScreen(s_screen);
		s_screen = NULL;
	}
	if (s_planes)
	{
		FreeMem(s_planes, s_plsize * 8);
		s_planes = NULL;
	}
	if (s_blankPointer)
	{
		FreeVec(s_blankPointer);
		s_blankPointer = NULL;
	}
#if defined(AJ_AMIGA_WOS)
	if (s_chunky) FreeVec(s_chunky);
	if (s_c2pCode) FreeVec(s_c2pCode);
	s_c2pCode = NULL;
#else
	free(s_chunky);
#endif
	s_chunky = NULL;
	free(s_lut);
	s_lut = NULL;
}

int aga_open(int width, int height)
{
	ULONG id;
	int   i;

	if (s_screen) return 1;
	id = choose_mode(width, height, &s_sw, &s_sh);
	if (id == INVALID_ID)
	{
		amiga_log("AGA: no 256-colour chipset mode holds %dx%d\n", width, height);
		return 0;
	}

	s_plsize = (ULONG)(s_sw / 8) * (ULONG)s_sh;
	s_planes = (UBYTE *)AllocMem(s_plsize * 8, MEMF_CHIP | MEMF_CLEAR);
#if defined(AJ_AMIGA_WOS)
	s_chunky = (UBYTE *)AllocVec((ULONG)s_sw * s_sh, MEMF_PUBLIC | MEMF_CLEAR);
	s_c2pCode = (UBYTE *)AllocVec(sizeof(c2p68k_code), MEMF_PUBLIC);
	if (s_c2pCode)
	{
		memcpy(s_c2pCode, c2p68k_code, sizeof(c2p68k_code));
		amiga_cache_flush(s_c2pCode, sizeof(c2p68k_code));
		CacheClearU();
	}
#else
	s_chunky = (UBYTE *)calloc((size_t)s_sw * s_sh, 1);
#endif
	if (!s_planes || !s_chunky || !build_lut()
#if defined(AJ_AMIGA_WOS)
	    || !s_c2pCode
#endif
	   )
	{
		amiga_log("AGA: not enough memory for a %dx%d screen\n", s_sw, s_sh);
		aga_close();
		return 0;
	}

	InitBitMap(&s_bm, 8, (UWORD)s_sw, (UWORD)s_sh);
	for (i = 0; i < 8; i++) s_bm.Planes[i] = (PLANEPTR)(s_planes + (ULONG)i * s_plsize);

	{
		struct TagItem tags[] =
		{
			{ SA_DisplayID, id },
			{ SA_Width,     (ULONG)s_sw },
			{ SA_Height,    (ULONG)s_sh },
			{ SA_Depth,     8 },
			{ SA_BitMap,    (ULONG)&s_bm },
			{ SA_Quiet,     TRUE },
			{ SA_ShowTitle, FALSE },
			{ SA_Draggable, FALSE },
			{ SA_Type,      CUSTOMSCREEN },
			{ SA_Title,     (ULONG)"amiga-j2me" },
			{ TAG_DONE,     0 }
		};
		s_screen = OpenScreenTagList(NULL, tags);
	}
	if (!s_screen)
	{
		amiga_log("AGA: OpenScreen of mode $%08lx failed\n", (unsigned long)id);
		aga_close();
		return 0;
	}
	load_palette();

	{
		struct TagItem tags[] =
		{
			{ WA_CustomScreen, (ULONG)s_screen },
			{ WA_Left,         0 },
			{ WA_Top,          0 },
			{ WA_Width,        (ULONG)s_sw },
			{ WA_Height,       (ULONG)s_sh },
			{ WA_Backdrop,     TRUE },
			{ WA_Borderless,   TRUE },
			{ WA_Activate,     TRUE },
			{ WA_RMBTrap,      TRUE },
			{ WA_ReportMouse,  TRUE },
			{ WA_SimpleRefresh,TRUE },
			{ WA_NoCareRefresh,TRUE },
			{ WA_BackFill,     (ULONG)LAYERS_NOBACKFILL },
			{ WA_IDCMP,        IDCMP_RAWKEY | IDCMP_MOUSEBUTTONS | IDCMP_MOUSEMOVE },
			{ TAG_DONE,        0 }
		};
		s_window = OpenWindowTagList(NULL, tags);
	}
	if (!s_window)
	{
		amiga_log("AGA: OpenWindow failed\n");
		aga_close();
		return 0;
	}

	s_width  = width;
	s_height = height;
	s_ox     = (s_sw - width) / 2;
	s_oy     = (s_sh - height) / 2;

	ScreenToFront(s_screen);
	ActivateWindow(s_window);
	amiga_log("Screen: AGA mode $%08lx, %dx%d, 256 colours, picture %dx%d at %d,%d\n",
	          (unsigned long)id, s_sw, s_sh, width, height, s_ox, s_oy);
	return 1;
}

void aga_hide_pointer(void)
{
	if (!s_window) return;
	if (!s_blankPointer) s_blankPointer = (UWORD *)AllocVec(6 * sizeof(UWORD), MEMF_CHIP | MEMF_CLEAR);
	if (s_blankPointer) SetPointer(s_window, s_blankPointer, 1, 16, 0, 0);
}

static void c2p(int x, int y, int w, int h)
{
#if defined(AJ_AMIGA_WOS)
	struct PPCArgs args;
	amiga_cache_flush(s_chunky + (size_t)y * s_sw, (unsigned long)h * s_sw);
	memset(&args, 0, sizeof(args));
	args.PP_Code = (APTR)s_c2pCode;
	args.PP_Regs[PPREG_D0] = (ULONG)x;
	args.PP_Regs[PPREG_D1] = (ULONG)y;
	args.PP_Regs[PPREG_D2] = (ULONG)w;
	args.PP_Regs[PPREG_D3] = (ULONG)h;
	args.PP_Regs[PPREG_D4] = (ULONG)s_sw;
	args.PP_Regs[PPREG_D5] = (ULONG)(s_sw / 8);
	args.PP_Regs[PPREG_D6] = s_plsize;
	args.PP_Regs[PPREG_A0] = (ULONG)s_chunky;
	args.PP_Regs[PPREG_A1] = (ULONG)s_planes;
	Run68K(&args);
#else
	struct AjC2P a;
	a.x = (UWORD)x; a.y = (UWORD)y; a.w = (UWORD)w; a.h = (UWORD)h;
	a.cmod = (UWORD)s_sw; a.bmod = (UWORD)(s_sw / 8);
	a.bplsize = s_plsize;
	a.chunky = s_chunky; a.bpl = s_planes;
	aj_c2p_rect(&a);
#endif
}

void aga_present(const uint16_t *pixels, int pitch, const AmigaRect *rects, int count)
{
	int r;
	if (!s_screen) return;
	for (r = 0; r < count; ++r)
	{
		int x = rects[r].left, y = rects[r].top;
		int w = rects[r].right - x, h = rects[r].bottom - y;
		int row, x0, x1;
		if (x < 0) { w += x; x = 0; }
		if (y < 0) { h += y; y = 0; }
		if (x + w > s_width)  w = s_width - x;
		if (y + h > s_height) h = s_height - y;
		if (w <= 0 || h <= 0) continue;

		for (row = 0; row < h; ++row)
		{
			const uint16_t *s = (const uint16_t *)((const uint8_t *)pixels + (size_t)(y + row) * pitch) + x;
			int sy = s_oy + y + row, sx = s_ox + x, i;
			UBYTE *d = s_chunky + (size_t)sy * s_sw + sx;
			/* the dither cell of each pixel: (screen y & 1) * 2 + (screen x & 1) */
			const UBYTE *even = s_lut + ((sy & 1) * 2 + (sx & 1)) * 65536;
			const UBYTE *odd  = s_lut + ((sy & 1) * 2 + ((sx + 1) & 1)) * 65536;
			for (i = 0; i + 1 < w; i += 2)
			{
				d[i]     = even[s[i]];
				d[i + 1] = odd[s[i + 1]];
			}
			if (i < w) d[i] = even[s[i]];
		}

		x0 = (s_ox + x) & ~31;
		x1 = (s_ox + x + w + 31) & ~31;
		if (x1 > s_sw) x1 = s_sw;
		c2p(x0, s_oy + y, x1 - x0, h);
	}
}
