/* Platform abstraction used by the VM and the native layers.
 * Implementations: hal/amiga (AOS3 68060, WarpOS, MorphOS; over the platform
 * layer in amiga_platform.h) and hal/sdl-host (Linux, offscreen). */
#ifndef AJ_HAL_H
#define AJ_HAL_H
#include <stddef.h>
#include <stdint.h>

#ifdef __cplusplus
extern "C" {
#endif

int  hal_init(int argc, char **argv);   /* 0 = ok */
void hal_shutdown(void);
const char *hal_platform_name(void);

/* Run fn() on a stack of at least `size` bytes. (The Amiga builds get their
 * stack from the startup code, so this only calls fn.) */
int hal_run_with_stack(int (*fn)(void), size_t size);

/* ---- console / logging ---- */
void hal_log(const char *fmt, ...);
void hal_write_console(const char *s, int len);

/* ---- time ---- */
int64_t  hal_time_millis(void);         /* wall clock, ms since 1970 */
uint32_t hal_ticks_us(void);            /* monotonic, wraps */
uint64_t hal_monotonic_micros(void);    /* monotonic, never wraps */
void     hal_sleep_ms(uint32_t ms);

/* ---- memory ---- */
void *hal_alloc(size_t size);
void  hal_free(void *p);

/* ---- files ---- */
/* 1 = plain file, 2 = directory, 0 = does not exist. Use before fopen():
 * opening a directory as a file hangs in the WarpOS C library. */
int hal_path_type(const char *path);
void *hal_load_file(const char *path, size_t *out_size); /* free with hal_free */
/* Writes the whole file (replacing it). 0 = ok. */
int hal_save_file(const char *path, const void *data, size_t size);
int hal_delete_file(const char *path);      /* 0 = ok */
int hal_make_dir(const char *path);         /* 0 = ok or exists */
/* Calls cb(name, user) for every plain file in the directory. 0 = ok. */
int hal_list_dir(const char *path, void (*cb)(const char *name, void *user), void *user);

/* ---- video: one R5G6B5 picture (native-endian 16-bit words) ---- */
typedef struct HalRect { int32_t x, y, w, h; } HalRect;

/* Opens the display for a width x height picture. 0 = ok. */
int  hal_video_open(int width, int height);
void hal_video_close(void);
/* Copies the given rectangles of the picture (pitch in bytes) to the display. */
void hal_video_present(const uint16_t *pixels, int pitch, const HalRect *rects, int count);
/* Host only: writes the last presented picture as a PPM file. 0 = ok. */
int  hal_video_screenshot(const char *path);

/* ---- audio: 16-bit stereo, pulled from a fill callback ---- */
typedef void (*HalAudioFill)(int16_t *stereo, int frames, void *user);
/* Opens the output. Returns its rate in Hz, or 0 when there is none. */
int  hal_audio_open(void);
void hal_audio_close(void);
void hal_audio_set_fill(HalAudioFill fill, void *user);
/* Call often (every frame / event loop round): asks the callback for what
 * the output needs next. */
void hal_audio_update(void);

/* ---- input ---- */
enum { HAL_EV_NONE = 0, HAL_EV_KEY, HAL_EV_POINTER, HAL_EV_POINTER_MOVE, HAL_EV_QUIT };

/* Key codes are Amiga raw key codes on every platform. */
#define HAL_KEY_1        0x01   /* top row 1..9,0 = 0x01..0x0A */
#define HAL_KEY_0        0x0A
#define HAL_KEY_NP0      0x0F
#define HAL_KEY_NP1      0x1D   /* numpad 1,2,3 = 0x1D..0x1F */
#define HAL_KEY_NP4      0x2D   /* numpad 4,5,6 = 0x2D..0x2F */
#define HAL_KEY_NP7      0x3D   /* numpad 7,8,9 = 0x3D..0x3F */
#define HAL_KEY_SPACE    0x40
#define HAL_KEY_BACKSPACE 0x41
#define HAL_KEY_TAB      0x42
#define HAL_KEY_NPENTER  0x43
#define HAL_KEY_RETURN   0x44
#define HAL_KEY_ESC      0x45
#define HAL_KEY_DEL      0x46
#define HAL_KEY_UP       0x4C
#define HAL_KEY_DOWN     0x4D
#define HAL_KEY_RIGHT    0x4E
#define HAL_KEY_LEFT     0x4F
#define HAL_KEY_F1       0x50   /* F1..F10 = 0x50..0x59 */
#define HAL_KEY_NPSTAR   0x5D
#define HAL_KEY_HELP     0x5F

typedef struct HalEvent {
    int type;
    int code;       /* HAL_EV_KEY: raw key code; HAL_EV_POINTER: button (0 = left) */
    int down;       /* key or button pressed (1) / released (0) */
    int x, y;       /* pointer position in picture pixels */
    int ch;         /* HAL_EV_KEY: character in the current keymap, or 0 */
} HalEvent;

/* Next pending event; returns 0 when there is none. */
int  hal_poll_event(HalEvent *ev);
/* Waits until an event arrives or timeout_ms passes. */
void hal_wait_event(uint32_t timeout_ms);

#ifdef __cplusplus
}
#endif
#endif
