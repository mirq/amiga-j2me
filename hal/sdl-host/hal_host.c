/* Host (Linux) HAL: console-only for M0; SDL2 display added in M2. */
#define _POSIX_C_SOURCE 200809L
#include "hal.h"
#include <stdarg.h>
#include <stdio.h>
#include <stdlib.h>
#include <time.h>

int hal_init(int argc, char **argv) { (void)argc; (void)argv; return 0; }
void hal_shutdown(void) {}
const char *hal_platform_name(void) { return "host"; }

void hal_log(const char *fmt, ...)
{
    va_list ap; va_start(ap, fmt); vfprintf(stderr, fmt, ap); va_end(ap);
}

void hal_write_console(const char *s, int len)
{
    fwrite(s, 1, (size_t)len, stdout); fflush(stdout);
}

uint64_t hal_monotonic_micros(void)
{
    struct timespec ts; clock_gettime(CLOCK_MONOTONIC, &ts);
    return (uint64_t)ts.tv_sec * 1000000u + (uint64_t)ts.tv_nsec / 1000u;
}

int64_t hal_time_millis(void)
{
    struct timespec ts; clock_gettime(CLOCK_REALTIME, &ts);
    return (int64_t)ts.tv_sec * 1000 + ts.tv_nsec / 1000000;
}

uint32_t hal_ticks_us(void)
{
    struct timespec ts; clock_gettime(CLOCK_MONOTONIC, &ts);
    return (uint32_t)((uint64_t)ts.tv_sec * 1000000u + ts.tv_nsec / 1000);
}

void hal_sleep_ms(uint32_t ms)
{
    struct timespec ts = { ms / 1000, (long)(ms % 1000) * 1000000L };
    nanosleep(&ts, NULL);
}

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
