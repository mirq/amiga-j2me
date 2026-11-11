/* M0 smoke test: HAL init, timing, endianness. */
#include "hal.h"

static int big_endian(void) { const uint16_t v = 1; return *(const uint8_t *)&v == 0; }

int main(int argc, char **argv)
{
    uint32_t t0, t1;
    volatile uint32_t acc = 0;
    uint32_t i;
    if (hal_init(argc, argv) != 0) return 20;
    hal_log("amiga-j2me hello: platform=%s endian=%s ptr=%d\n",
            hal_platform_name(), big_endian() ? "big" : "little", (int)sizeof(void *));
    hal_log("time_millis=%ld\n", (long)(hal_time_millis() / 1000));
    t0 = hal_ticks_us();
    for (i = 0; i < 1000000u; i++) acc += i ^ (acc >> 3);
    t1 = hal_ticks_us();
    hal_log("1M loop iters: %lu us (acc=%lu)\n", (unsigned long)(t1 - t0), (unsigned long)acc);
    hal_shutdown();
    return 0;
}
