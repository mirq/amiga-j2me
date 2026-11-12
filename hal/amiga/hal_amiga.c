/* amiga-j2me HAL for AmigaOS 3 (68060), WarpOS and MorphOS, over the Amiga
 * platform layer (amiga_platform.h; from the author's civctp2 port). */
#include "hal.h"
#include "amiga_os.h"
#include "amiga_platform.h"
#include <stdarg.h>
#include <stdio.h>
#include <stdlib.h>
#include <string.h>

/* Seconds between 1970-01-01 and the Amiga epoch 1978-01-01 */
#define AMIGA_EPOCH_OFFSET 252460800LL

static int64_t s_wall_base_ms;   /* wall clock at amiga_ticks_us() == 0 */
static int     s_video_w, s_video_h;

static void *s_pad;
static ULONG s_pad_size;

int hal_init(int argc, char **argv)
{
    struct DateStamp d;
    (void)argc; (void)argv;
    if (!amiga_platform_init()) return -1;
    {
        /* "setenv AJ_DISPLAY aga": the AGA screen for programs that do not ask (aj_cldc) */
        char env[8];
        if (GetVar((CONST_STRPTR)"AJ_DISPLAY", (STRPTR)env, sizeof(env), GVF_GLOBAL_ONLY) > 0
            && (env[0] == 'a' || env[0] == 'A'))
            hal_display_set(HAL_DISPLAY_AGA);
    }
#if defined(__mc68000__)
    /* Java doubles are 64-bit: round every FPU result to double, not to the
     * 68881's 80-bit default (FPCR precision bits 7-6 = 10). Per task. */
    __asm__ volatile ("fmove.l #0x80,%fpcr");
#endif
#if defined(__mc68000__)
    {
        /* diagnostics: AJ_PAD_MB=n takes n MB of Fast RAM first (never given back
         * before the exit), so that what the VM allocates afterwards lies at other
         * addresses. Used to check address-dependent faults in the emulator. */
        char env[8];
        if (GetVar((CONST_STRPTR)"AJ_PAD_MB", (STRPTR)env, sizeof(env), GVF_GLOBAL_ONLY) > 0) {
            s_pad_size = (ULONG)atoi(env) << 20;
            if (s_pad_size) s_pad = AllocMem(s_pad_size, MEMF_FAST);
        }
    }
#endif
    /* The date once from DOS; afterwards only the monotonic clock is read
     * (on WarpOS it never leaves the PPC). */
    DateStamp(&d);
    s_wall_base_ms = ((int64_t)d.ds_Days * 86400 + (int64_t)d.ds_Minute * 60
                      + AMIGA_EPOCH_OFFSET) * 1000 + (int64_t)d.ds_Tick * 20
                     - (int64_t)(amiga_ticks_us() / 1000u);
    return 0;
}

void hal_shutdown(void)
{
    hal_tls_shutdown();
    if (s_pad) { FreeMem(s_pad, s_pad_size); s_pad = NULL; }
    amiga_platform_exit();
}

const char *hal_platform_name(void)
{
#if defined(AJ_AMIGA_WOS)
    return "wos-ppc";
#elif defined(AJ_AMIGA_MOS)
    return "morphos-ppc";
#else
    return "aos3-m68k";
#endif
}

#if !defined(AJ_AMIGA_WOS) && !defined(AJ_AMIGA_MOS)
/* 68k: fn runs on a stack of its own, in Fast RAM asked for by name. The
 * stack libnix sets up at start comes from a plain MEMF_PUBLIC request, which
 * a machine with WarpOS can serve from "ppc memory", many times slower for
 * the 68060 (see amiga_malloc.c). Nothing between the two StackSwap calls may
 * live on the stack, hence the statics. */
static struct StackSwapStruct s_swap;
static int (*s_stack_fn)(void);
static int s_stack_rc;

int __attribute__((noinline)) hal_run_with_stack(int (*fn)(void), size_t size)
{
    void *mem;
    char env[8];
    /* diagnostics: AJ_NOFASTSTACK keeps the stack libnix set up */
    if (GetVar((CONST_STRPTR)"AJ_NOFASTSTACK", (STRPTR)env, sizeof(env), GVF_GLOBAL_ONLY) > 0) return fn();
    size = (size + 15) & ~(size_t)15;
    mem = AllocMem(size, MEMF_FAST);
    if (!mem) return fn();              /* no Fast RAM: stay on the stack we have */
#ifdef AJ_MALLOC_POISON
    memset(mem, 0xA5, size);            /* test builds: a used machine's stack memory holds junk */
#endif
    s_swap.stk_Lower = mem;
    s_swap.stk_Upper = (ULONG)mem + size;
    s_swap.stk_Pointer = (APTR)((ULONG)mem + size - 32);
    s_stack_fn = fn;
    StackSwap(&s_swap);
    s_stack_rc = s_stack_fn();
    StackSwap(&s_swap);
    FreeMem(mem, size);
    return s_stack_rc;
}
#else
int hal_run_with_stack(int (*fn)(void), size_t size) { (void)size; return fn(); }
#endif

void hal_log(const char *fmt, ...)
{
    va_list ap; va_start(ap, fmt); vprintf(fmt, ap); va_end(ap);
    fflush(stdout);
}

void hal_write_console(const char *s, int len)
{
    fwrite(s, 1, (size_t)len, stdout); fflush(stdout);
}

int64_t  hal_time_millis(void) { return s_wall_base_ms + (int64_t)(amiga_ticks_us() / 1000u); }
uint64_t hal_monotonic_micros(void) { return amiga_ticks_us(); }
uint32_t hal_ticks_us(void) { return (uint32_t)amiga_ticks_us(); }
void     hal_sleep_ms(uint32_t ms) { amiga_delay_ms(ms); }

/* malloc is hal/amiga/amiga_malloc.c on the 68060 and WarpOS */
void *hal_alloc(size_t size) { return malloc(size); }
void  hal_free(void *p) { free(p); }

int hal_path_type(const char *path)
{
    int type = 0;
    BPTR lock = Lock((CONST_STRPTR)path, ACCESS_READ);
    if (lock) {
        struct FileInfoBlock *fib = (struct FileInfoBlock *)AllocDosObject(DOS_FIB, NULL);
        if (fib) {
            if (Examine(lock, fib)) type = (fib->fib_DirEntryType < 0) ? 1 : 2;
            FreeDosObject(DOS_FIB, fib);
        }
        UnLock(lock);
    }
    return type;
}

void *hal_load_file(const char *path, size_t *out_size)
{
    FILE *f; long n; void *buf;
    if (hal_path_type(path) != 1) return NULL;
    f = fopen(path, "rb");
    if (!f) return NULL;
    fseek(f, 0, SEEK_END); n = ftell(f); fseek(f, 0, SEEK_SET);
    buf = malloc(n ? (size_t)n : 1);
    if (buf && fread(buf, 1, (size_t)n, f) != (size_t)n) { free(buf); buf = NULL; }
    fclose(f);
    if (buf && out_size) *out_size = (size_t)n;
    return buf;
}

int hal_save_file(const char *path, const void *data, size_t size)
{
    FILE *f = fopen(path, "wb"); int ok;
    if (!f) return -1;
    ok = fwrite(data, 1, size, f) == size;
    return (fclose(f) == 0 && ok) ? 0 : -1;
}

int hal_delete_file(const char *path) { return DeleteFile((CONST_STRPTR)path) ? 0 : -1; }

int hal_make_dir(const char *path)
{
    BPTR lock;
    if (hal_path_type(path) == 2) return 0;
    lock = CreateDir((CONST_STRPTR)path);
    if (!lock) return -1;
    UnLock(lock);
    return 0;
}

int hal_list_dir(const char *path, void (*cb)(const char *name, void *user), void *user)
{
    int rc = -1;
    BPTR lock = Lock((CONST_STRPTR)path, ACCESS_READ);
    if (lock) {
        struct FileInfoBlock *fib = (struct FileInfoBlock *)AllocDosObject(DOS_FIB, NULL);
        if (fib) {
            if (Examine(lock, fib)) {
                rc = 0;
                while (ExNext(lock, fib))
                    if (fib->fib_DirEntryType < 0) cb((const char *)fib->fib_FileName, user);
            }
            FreeDosObject(DOS_FIB, fib);
        }
        UnLock(lock);
    }
    return rc;
}

/* ---- video ---- */

int hal_display_ask(const char *game)
{
    int widths[4], heights[4];
    int rtg = CyberGfxBase && amiga_video_modes(widths, heights, 4) > 0;
    int aga = amiga_video_have_aga();
    LONG r;
    if (!aga) return HAL_DISPLAY_RTG;
    if (!rtg) return HAL_DISPLAY_AGA;
    {
        struct EasyStruct es;
        ULONG args[1];
        memset(&es, 0, sizeof(es));
        es.es_StructSize = sizeof(es);
        es.es_Title = (UBYTE *)"amiga-j2me";
        es.es_TextFormat = (UBYTE *)"%s\n\nShow the game on the RTG card or on the AGA chipset?";
        es.es_GadgetFormat = (UBYTE *)"RTG|AGA|Quit";
        args[0] = (ULONG)game;
        r = EasyRequestArgs(NULL, &es, NULL, args);
    }
    return r == 1 ? HAL_DISPLAY_RTG : r == 2 ? HAL_DISPLAY_AGA : -1;
}

void hal_display_set(int display)
{
    amiga_video_use_aga(display == HAL_DISPLAY_AGA && amiga_video_have_aga());
}

int hal_video_open(int width, int height)
{
    if (!amiga_video_open(width, height)) return -1;
    amiga_video_hide_pointer();
    s_video_w = width; s_video_h = height;
    return 0;
}

void hal_video_close(void) { amiga_video_close(); }

void hal_video_present(const uint16_t *pixels, int pitch, const HalRect *rects, int count)
{
    AmigaRect r[16];
    int i;
    while (count > 0) {
        int n = count > 16 ? 16 : count;
        for (i = 0; i < n; i++) {
            r[i].left = rects[i].x; r[i].top = rects[i].y;
            r[i].right = rects[i].x + rects[i].w; r[i].bottom = rects[i].y + rects[i].h;
        }
        amiga_video_present(pixels, pitch, r, n);
        rects += n; count -= n;
    }
}

int hal_video_screenshot(const char *path) { (void)path; return -1; }

/* ---- audio (AHI ring buffer, amiga_audio.c) ---- */

int  hal_audio_open(void) { return amiga_audio_open(); }
void hal_audio_close(void) { amiga_audio_close(); }
void hal_audio_set_fill(HalAudioFill fill, void *user) { amiga_audio_set_fill(fill, user); }
void hal_audio_update(void) { amiga_audio_update(); }

/* ---- input ---- */

int hal_poll_event(HalEvent *ev)
{
    AmigaEvent a;
    if (amiga_break_requested()) { ev->type = HAL_EV_QUIT; return 1; }
    while (amiga_input_poll(&a)) {
        /* amiga_input.c already gives positions relative to the picture */
        ev->code = a.code; ev->down = a.down; ev->ch = (unsigned char)a.ch;
        ev->x = a.x; ev->y = a.y;
        switch (a.type) {
        case AMIGA_EV_KEY:         ev->type = HAL_EV_KEY; return 1;
        case AMIGA_EV_MOUSEBUTTON: ev->type = HAL_EV_POINTER; return 1;
        case AMIGA_EV_MOUSEMOVE:   ev->type = HAL_EV_POINTER_MOVE; return 1;
        case AMIGA_EV_QUIT:        ev->type = HAL_EV_QUIT; return 1;
        default: break;            /* wheel etc.: skip */
        }
    }
    return 0;
}

void hal_wait_event(uint32_t timeout_ms) { amiga_input_wait(timeout_ms); }

/* diagnostics: where exec places a block for the given requirement flags (freed at once) */
void *aj_probe_alloc(unsigned long size, unsigned long flags)
{
    void *m = AllocMem(size, flags);
    if (m) FreeMem(m, size);
    return m;
}

#if defined(AJ_AMIGA_WOS)
/* diagnostics, real PowerPC cards: do the PPC and the 68k see each other's
 * writes right after a call? exec's CopyMem runs on the 68k. Returns
 * (mismatches seen by the PPC after the 68k wrote) * 1000 + (mismatches in what
 * the 68k read after the PPC wrote), over 200 rounds; flush != 0 uses dcbf on
 * the buffers before each call, as the audio and input code do. */
int aj_probe_coherency(int flush)
{
    static unsigned char a[1024], b[1024], c[1024];
    int round, i, bad_in = 0, bad_out = 0;
    for (round = 0; round < 200; round++) {
        for (i = 0; i < 1024; i++) a[i] = (unsigned char)(i * 7 + round * 13);    /* the PPC writes a */
        if (flush) { amiga_cache_flush(a, sizeof(a)); amiga_cache_flush(b, sizeof(b)); }
        CopyMem(a, b, sizeof(a));                                                 /* the 68k copies a to b */
        for (i = 0; i < 1024; i++) if (b[i] != (unsigned char)(i * 7 + round * 13)) { bad_in++; break; }
        /* and once more through the 68k alone: c is what the 68k has in b */
        if (flush) { amiga_cache_flush(b, sizeof(b)); amiga_cache_flush(c, sizeof(c)); }
        CopyMem(b, c, sizeof(b));
        for (i = 0; i < 1024; i++) if (c[i] != (unsigned char)(i * 7 + round * 13)) { bad_out++; break; }
    }
    return bad_in * 1000 + bad_out;
}
#endif

#if !defined(AJ_AMIGA_MOS)
/* diagnostics: milliseconds the 68k needs for 8 exec CopyMem calls of 2 MB.
 * From the WarpOS build the calls go through Run68K; compared with the same
 * figure from the 68k build it shows how fast 68k code runs under a PPC task. */
int aj_probe_68k_speed(void)
{
    unsigned long n = 2UL << 20;
    char *a = (char *)AllocMem(n, MEMF_FAST), *b = (char *)AllocMem(n, MEMF_FAST);
    uint64_t t0;
    int i, ms = -1;
    if (a && b) {
        CopyMem(a, b, n);
        t0 = hal_monotonic_micros();
        for (i = 0; i < 8; i++) CopyMem(a, b, n);
        ms = (int)((hal_monotonic_micros() - t0) / 1000);
    }
    if (a) FreeMem(a, n);
    if (b) FreeMem(b, n);
    return ms;
}
#endif
