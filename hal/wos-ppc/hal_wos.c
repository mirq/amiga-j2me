/* WarpOS PPC HAL (MOS2WOS SDK).
 * Landmine (BlitzQuake_src docs): timer.device ReadEClock/GetSysTime LP calls
 * crash from a WarpOS PPC task on the target stack. So:
 *   - wall clock: dos DateStamp() (1/50 s)
 *   - fine ticks: PPC time base (mftb), calibrated against DateStamp once.
 * Every PPC->68k call is expensive (ms) - never use these in per-frame paths
 * except hal_ticks_us(), which is pure PPC. */
#include "hal.h"
#include <exec/memory.h>
#include <dos/dos.h>
#include <proto/exec.h>
#include <proto/dos.h>
#include <stdarg.h>
#include <stdio.h>
#include <stdlib.h>

#define AMIGA_EPOCH_OFFSET 252460800LL

static uint32_t tb_per_us = 0; /* time-base ticks per microsecond */

static inline uint64_t read_tb(void)
{
    uint32_t hi, lo, hi2;
    do {
        __asm__ volatile ("mftbu %0" : "=r"(hi));
        __asm__ volatile ("mftb %0"  : "=r"(lo));
        __asm__ volatile ("mftbu %0" : "=r"(hi2));
    } while (hi != hi2);
    return ((uint64_t)hi << 32) | lo;
}

static int32_t ds_ticks(void)
{
    struct DateStamp d; DateStamp(&d);
    return (d.ds_Minute * 60 * 50) + d.ds_Tick; /* ignore day wrap for calibration */
}

static void calibrate_tb(void)
{
    int32_t t0, t1; uint64_t b0, b1;
    t0 = ds_ticks(); while ((t1 = ds_ticks()) == t0) {}       /* align to tick edge */
    b0 = read_tb();
    while (ds_ticks() - t1 < 10) {}                            /* 10 ticks = 200 ms */
    b1 = read_tb();
    tb_per_us = (uint32_t)((b1 - b0) / 200000u);
    if (!tb_per_us) tb_per_us = 1;
}

int hal_init(int argc, char **argv) { (void)argc; (void)argv; calibrate_tb(); return 0; }
void hal_shutdown(void) {}
const char *hal_platform_name(void) { return "wos-ppc"; }

void hal_log(const char *fmt, ...)
{
    va_list ap; va_start(ap, fmt); vprintf(fmt, ap); va_end(ap);
    fflush(stdout);
}

int64_t hal_time_millis(void)
{
    struct DateStamp d; DateStamp(&d);
    return ((int64_t)d.ds_Days * 86400 + (int64_t)d.ds_Minute * 60 + AMIGA_EPOCH_OFFSET) * 1000
           + (int64_t)d.ds_Tick * 20;
}

uint32_t hal_ticks_us(void) { return (uint32_t)(read_tb() / tb_per_us); }
uint64_t hal_monotonic_micros(void) { return read_tb() / tb_per_us; }

void hal_write_console(const char *s, int len)
{
    fwrite(s, 1, (size_t)len, stdout); fflush(stdout);
}

void hal_sleep_ms(uint32_t ms) { ULONG t = (ms + 19) / 20; Delay(t ? t : 1); }

/* PPC-side RAM; newlib malloc is backed by the SDK's PPC pool */
void *hal_alloc(size_t size) { return malloc(size); }
void  hal_free(void *p) { free(p); }

void *hal_load_file(const char *path, size_t *out_size)
{
    FILE *f = fopen(path, "rb"); long n; void *buf;
    if (!f) return NULL;
    fseek(f, 0, SEEK_END); n = ftell(f); fseek(f, 0, SEEK_SET);
    buf = malloc(n ? (size_t)n : 1);
    if (buf && fread(buf, 1, (size_t)n, f) != (size_t)n) { free(buf); buf = NULL; }
    fclose(f);
    if (buf && out_size) *out_size = (size_t)n;
    return buf;
}
