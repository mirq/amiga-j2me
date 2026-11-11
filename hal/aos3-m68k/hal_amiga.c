/* AmigaOS HAL shared by AOS3 m68k and MorphOS (both have exec/dos/timer.device). */
#include "hal.h"
#include <exec/memory.h>
#include <devices/timer.h>
#include <dos/dos.h>
#include <proto/exec.h>
#include <proto/dos.h>
#include <proto/timer.h>
#include <stdarg.h>
#include <stdio.h>

#if defined(__MORPHOS__)
struct Library *TimerBase;
#else
struct Device *TimerBase;
#endif
static struct timerequest tr;
static int timer_open;

/* Seconds between 1970-01-01 and Amiga epoch 1978-01-01 */
#define AMIGA_EPOCH_OFFSET 252460800LL

int hal_init(int argc, char **argv)
{
    (void)argc; (void)argv;
    if (OpenDevice((CONST_STRPTR)TIMERNAME, UNIT_MICROHZ, (struct IORequest *)&tr, 0) != 0)
        return -1;
    TimerBase = (void *)tr.tr_node.io_Device;
    timer_open = 1;
    return 0;
}

void hal_shutdown(void)
{
    if (timer_open) { CloseDevice((struct IORequest *)&tr); timer_open = 0; }
}

const char *hal_platform_name(void)
{
#if defined(__MORPHOS__)
    return "morphos-ppc";
#else
    return "aos3-m68k";
#endif
}

void hal_log(const char *fmt, ...)
{
    va_list ap; va_start(ap, fmt); vprintf(fmt, ap); va_end(ap);
    fflush(stdout);
}

void hal_write_console(const char *s, int len)
{
    fwrite(s, 1, (size_t)len, stdout); fflush(stdout);
}

uint64_t hal_monotonic_micros(void)
{
    struct timeval tv;
    GetSysTime(&tv);  /* not strictly monotonic if the clock is set; good enough */
    return (uint64_t)tv.tv_secs * 1000000u + tv.tv_micro;
}

int64_t hal_time_millis(void)
{
    struct timeval tv;
    GetSysTime(&tv);
    return ((int64_t)tv.tv_secs + AMIGA_EPOCH_OFFSET) * 1000 + tv.tv_micro / 1000;
}

uint32_t hal_ticks_us(void)
{
    struct timeval tv;
    GetSysTime(&tv);
    return (uint32_t)(tv.tv_secs * 1000000u + tv.tv_micro);
}

void hal_sleep_ms(uint32_t ms)
{
    ULONG ticks = (ms + 19) / 20;           /* dos Delay() is 50 Hz */
    Delay(ticks ? ticks : 1);
}

void *hal_alloc(size_t size)
{
    /* size-tracked AllocVec; MEMF_ANY lets Exec pick Fast RAM first */
    return AllocVec((ULONG)size, MEMF_ANY);
}

void hal_free(void *p) { if (p) FreeVec(p); }

void *hal_load_file(const char *path, size_t *out_size)
{
    BPTR fh = Open((CONST_STRPTR)path, MODE_OLDFILE);
    LONG n; void *buf;
    if (!fh) return NULL;
    Seek(fh, 0, OFFSET_END);
    n = Seek(fh, 0, OFFSET_BEGINNING);
    buf = hal_alloc(n ? (size_t)n : 1);
    if (buf && Read(fh, buf, n) != n) { hal_free(buf); buf = NULL; }
    Close(fh);
    if (buf && out_size) *out_size = (size_t)n;
    return buf;
}
