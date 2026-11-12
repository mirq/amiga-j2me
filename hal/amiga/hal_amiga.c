/* amiga-j2me HAL for AmigaOS 3 (68060), WarpOS and MorphOS, over the Amiga
 * platform layer (amiga_platform.h; from the author's civctp2 port). */
#include "hal.h"
#include "amiga_os.h"
#include "amiga_platform.h"
#include <stdarg.h>
#include <stdio.h>
#include <stdlib.h>

/* Seconds between 1970-01-01 and the Amiga epoch 1978-01-01 */
#define AMIGA_EPOCH_OFFSET 252460800LL

static int64_t s_wall_base_ms;   /* wall clock at amiga_ticks_us() == 0 */
static int     s_video_w, s_video_h;

int hal_init(int argc, char **argv)
{
    struct DateStamp d;
    (void)argc; (void)argv;
    if (!amiga_platform_init()) return -1;
#if defined(__mc68000__)
    /* Java doubles are 64-bit: round every FPU result to double, not to the
     * 68881's 80-bit default (FPCR precision bits 7-6 = 10). Per task. */
    __asm__ volatile ("fmove.l #0x80,%fpcr");
#endif
    /* The date once from DOS; afterwards only the monotonic clock is read
     * (on WarpOS it never leaves the PPC). */
    DateStamp(&d);
    s_wall_base_ms = ((int64_t)d.ds_Days * 86400 + (int64_t)d.ds_Minute * 60
                      + AMIGA_EPOCH_OFFSET) * 1000 + (int64_t)d.ds_Tick * 20
                     - (int64_t)(amiga_ticks_us() / 1000u);
    return 0;
}

void hal_shutdown(void) { hal_tls_shutdown(); amiga_platform_exit(); }

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

int hal_run_with_stack(int (*fn)(void), size_t size) { (void)size; return fn(); }

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
