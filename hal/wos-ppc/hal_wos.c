/* WarpOS PPC HAL (MOS2WOS SDK).
 *
 * Rules (learned in the BlitzQuake and civctp2 ports):
 *  - Time and waits use powerpc.library's GetSysTimePPC / WaitTime, which
 *    never leave the PPC. timer.device calls from a PPC task crash on the
 *    target stack, and every PPC->68k call costs milliseconds.
 *  - A "waker" PPC task wakes every 2 ms. When no PPC task is ready WarpOS
 *    naps the PPC; in Amiberry's PowerPC emulation the task waiting for a
 *    68k call is then resumed only ~59 s later. With the waker the scheduler
 *    keeps running. On real hardware it costs a few hundred task switches/s.
 *  - OS structures use the 68k's 2-byte alignment: OS headers go in under
 *    #pragma pack(2).
 */
#include "hal.h"
#pragma pack(push, 2)
#include <exec/types.h>
#include <exec/memory.h>
#include <dos/dos.h>
#include <dos/dosextens.h>
#include <utility/tagitem.h>
#include <proto/exec.h>
#include <proto/dos.h>
#include <powerpc/powerpc.h>
#include <powerpc/powerpc_protos.h>
#include <powerpc/tasksPPC.h>
#pragma pack(pop)
#include <stdarg.h>
#include <stdio.h>
#include <stdlib.h>

#define AMIGA_EPOCH_OFFSET 252460800LL

static uint64_t s_base;
static int64_t  s_wall_base_ms;   /* wall clock (ms since 1970) at s_base */
static volatile int s_wakerQuit;
static volatile int s_wakerRunning;

static uint64_t now_us(void)
{
    struct timeval tv;
    GetSysTimePPC(&tv);
    return (uint64_t)tv.tv_secs * 1000000u + tv.tv_micro;
}

/* touches nothing but these flags: no C library, no heap */
static void waker(void)
{
    while (!s_wakerQuit) WaitTime(0, 2000);
    s_wakerRunning = 0;
}

static void waker_start(void)
{
    struct TagItem tags[] = {
        { TASKATTR_CODE,      (ULONG)waker },
        { TASKATTR_NAME,      (ULONG)"amiga-j2me waker" },
        { TASKATTR_PRI,       (ULONG)-100 },
        { TASKATTR_NICE,      20 },
        { TASKATTR_INHERITR2, TRUE },
        { TAG_DONE,           0 }
    };
    s_wakerQuit = 0;
    s_wakerRunning = 1;
    if (!CreateTaskPPC(tags)) s_wakerRunning = 0;
}

/* the task's code is in this program: it must be gone before we unload */
static void waker_stop(void)
{
    int i;
    s_wakerQuit = 1;
    for (i = 0; s_wakerRunning && i < 500; ++i) WaitTime(0, 2000);
    WaitTime(0, 2000);
}

int hal_init(int argc, char **argv)
{
    (void)argc; (void)argv;
    struct DateStamp d;
    s_base = now_us();
    waker_start();
    /* GetSysTimePPC is not wall-clock time: take the date once from DOS */
    DateStamp(&d);
    s_wall_base_ms = ((int64_t)d.ds_Days * 86400 + (int64_t)d.ds_Minute * 60
                      + AMIGA_EPOCH_OFFSET) * 1000 + (int64_t)d.ds_Tick * 20;
    return 0;
}

void hal_shutdown(void) { waker_stop(); }
const char *hal_platform_name(void) { return "wos-ppc"; }

void hal_log(const char *fmt, ...)
{
    va_list ap; va_start(ap, fmt); vprintf(fmt, ap); va_end(ap);
    fflush(stdout);
}

void hal_write_console(const char *s, int len)
{
    fwrite(s, 1, (size_t)len, stdout); fflush(stdout);
}

int64_t hal_time_millis(void)
{
    return s_wall_base_ms + (int64_t)((now_us() - s_base) / 1000u);
}

uint64_t hal_monotonic_micros(void) { return now_us() - s_base; }
uint32_t hal_ticks_us(void) { return (uint32_t)hal_monotonic_micros(); }

void hal_sleep_ms(uint32_t ms) { if (ms) WaitTime(0, (ULONG)ms * 1000u); }

/* PPC-side RAM: malloc is hal/amiga/amiga_malloc.c (AllocVecPPC slabs) */
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
    FILE *f = fopen(path, "rb"); long n; void *buf;
    if (!f) return NULL;
    fseek(f, 0, SEEK_END); n = ftell(f); fseek(f, 0, SEEK_SET);
    buf = malloc(n ? (size_t)n : 1);
    if (buf && fread(buf, 1, (size_t)n, f) != (size_t)n) { free(buf); buf = NULL; }
    fclose(f);
    if (buf && out_size) *out_size = (size_t)n;
    return buf;
}

/* the WarpOS executable carries its stack size (elf2warpos --stack) */
int hal_run_with_stack(int (*fn)(void), size_t size) { (void)size; return fn(); }
