/* Platform abstraction used by the VM and native layers. One implementation per target. */
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

/* Run fn() on a stack of at least `size` bytes (AOS3 CLI default is 4 KB).
 * Returns fn's result, or -1 if the stack could not be allocated. */
int hal_run_with_stack(int (*fn)(void), size_t size);

/* console / logging */
void hal_log(const char *fmt, ...);
void hal_write_console(const char *s, int len);  /* raw, unbuffered-ish */

/* time */
int64_t hal_time_millis(void);          /* wall clock, ms since 1970 */
uint32_t hal_ticks_us(void);            /* monotonic, wraps */
uint64_t hal_monotonic_micros(void);    /* monotonic, 64-bit, never wraps */
void hal_sleep_ms(uint32_t ms);

/* memory (Fast RAM preferred on Amiga) */
void *hal_alloc(size_t size);
void  hal_free(void *p);

/* files */
/* 1 = plain file, 2 = directory, 0 = does not exist. Use before fopen():
 * opening a directory as a file hangs in the WarpOS C library. */
int hal_path_type(const char *path);
void *hal_load_file(const char *path, size_t *out_size); /* free with hal_free */

#ifdef __cplusplus
}
#endif
#endif
