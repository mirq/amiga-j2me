/* Host (Linux) HAL: console-only for M0; SDL2 display added in M2. */
#define _POSIX_C_SOURCE 200809L
#include "hal.h"
#include <stdarg.h>
#include <stdio.h>
#include <stdlib.h>
#include <string.h>
#include <time.h>
#include <sys/stat.h>
#include <dirent.h>
#include <unistd.h>

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

int hal_path_type(const char *path)
{
    struct stat st;
    if (stat(path, &st) != 0) return 0;
    return S_ISDIR(st.st_mode) ? 2 : 1;
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

/* the OS already provides a big enough stack here */
int hal_run_with_stack(int (*fn)(void), size_t size) { (void)size; return fn(); }

int hal_save_file(const char *path, const void *data, size_t size)
{
    FILE *f = fopen(path, "wb"); int ok;
    if (!f) return -1;
    ok = fwrite(data, 1, size, f) == size;
    return (fclose(f) == 0 && ok) ? 0 : -1;
}

int hal_delete_file(const char *path) { return unlink(path) == 0 ? 0 : -1; }

int hal_make_dir(const char *path)
{
    if (hal_path_type(path) == 2) return 0;
    return mkdir(path, 0777) == 0 ? 0 : -1;
}

int hal_list_dir(const char *path, void (*cb)(const char *name, void *user), void *user)
{
    DIR *d = opendir(path); struct dirent *e;
    if (!d) return -1;
    while ((e = readdir(d)) != NULL)
        if (e->d_name[0] != '.') cb(e->d_name, user);
    closedir(d);
    return 0;
}

/* ---- video: offscreen picture, dumped as PPM for tests ---- */
static uint16_t *s_fb;
static int s_fb_w, s_fb_h;

int hal_video_open(int width, int height)
{
    free(s_fb);
    s_fb = (uint16_t *)calloc((size_t)width * height, 2);
    s_fb_w = width; s_fb_h = height;
    return s_fb ? 0 : -1;
}

void hal_video_close(void)
{
    const char *shot = getenv("AJ_SHOT");   /* screenshot of the last frame */
    if (shot && s_fb) hal_video_screenshot(shot);
    free(s_fb); s_fb = NULL;
}

void hal_video_present(const uint16_t *pixels, int pitch, const HalRect *rects, int count)
{
    int i, y;
    if (!s_fb) return;
    for (i = 0; i < count; i++) {
        HalRect r = rects[i];
        if (r.x < 0) { r.w += r.x; r.x = 0; }
        if (r.y < 0) { r.h += r.y; r.y = 0; }
        if (r.x + r.w > s_fb_w) r.w = s_fb_w - r.x;
        if (r.y + r.h > s_fb_h) r.h = s_fb_h - r.y;
        for (y = r.y; y < r.y + r.h; y++)
            memcpy(s_fb + (size_t)y * s_fb_w + r.x,
                   (const uint8_t *)pixels + (size_t)y * pitch + (size_t)r.x * 2,
                   (size_t)(r.w > 0 ? r.w : 0) * 2);
    }
}

int hal_video_screenshot(const char *path)
{
    FILE *f; int i;
    if (!s_fb || !(f = fopen(path, "wb"))) return -1;
    fprintf(f, "P6\n%d %d\n255\n", s_fb_w, s_fb_h);
    for (i = 0; i < s_fb_w * s_fb_h; i++) {
        unsigned p = s_fb[i];
        unsigned r = (p >> 11) & 31, g = (p >> 5) & 63, b = p & 31;
        fputc((r << 3) | (r >> 2), f); fputc((g << 2) | (g >> 4), f); fputc((b << 3) | (b >> 2), f);
    }
    fclose(f);
    return 0;
}

/* ---- input: scripted through AJ_KEYS for tests ----
 * AJ_KEYS="500:4C 100:-4C 2000:Q" : after 500 ms press raw key 0x4C, 100 ms
 * later release it, 2000 ms later quit. */
static const char *s_script;
static uint64_t s_script_next_us;
static int s_script_started;

int hal_poll_event(HalEvent *ev)
{
    char *end; long delay;
    if (!s_script_started) {
        s_script = getenv("AJ_KEYS"); s_script_started = 1;
        s_script_next_us = 0;
    }
    if (!s_script || !*s_script) return 0;
    while (*s_script == ' ') s_script++;
    delay = strtol(s_script, &end, 10);
    if (*end != ':') { s_script = NULL; return 0; }
    if (s_script_next_us == 0) s_script_next_us = hal_monotonic_micros() + (uint64_t)delay * 1000u;
    if (hal_monotonic_micros() < s_script_next_us) return 0;
    s_script = end + 1; s_script_next_us = 0;
    memset(ev, 0, sizeof(*ev));
    if (*s_script == 'Q') { s_script++; ev->type = HAL_EV_QUIT; return 1; }
    ev->type = HAL_EV_KEY; ev->down = 1;
    if (*s_script == '-') { ev->down = 0; s_script++; }
    ev->code = (int)strtol(s_script, &end, 16);
    s_script = end;
    return 1;
}

void hal_wait_event(uint32_t timeout_ms) { hal_sleep_ms(timeout_ms > 5 ? 5 : timeout_ms); }
