/* amiga-j2me: timing probe. Runs a few code shapes the VM uses (register loop,
 * globals in memory, calls through a table, a big array, 64-bit division, the
 * clock) and prints microseconds for each, with where code, data, heap and
 * stack live. To compare machines and find what makes one of them slow. */
#include "hal.h"
#include <stdint.h>
#include <stdlib.h>
#include <stdio.h>
#include <string.h>
#if defined(__amigaos__) && !defined(WOS) && !defined(__MORPHOS__)
#include <exec/execbase.h>
#include <exec/memory.h>
#include <proto/exec.h>
#define HAVE_MEMLIST 1
#endif

static volatile uint32_t g_a, g_b;
static uint8_t *g_pc;
static uint32_t *g_sp;
static uint32_t g_stack[64];
typedef void (*Op)(void);
static Op s_table[256];
static volatile int s_stop;

static void op_push(void) { *g_sp++ = g_pc[1]; g_pc += 2; }
static void op_add(void)  { g_sp--; g_sp[-1] += g_sp[0]; g_pc += 1; }
static void op_pop(void)  { g_sp--; g_pc += 1; }
static void op_loop(void) { if (--g_a == 0) s_stop = 1; g_pc -= 6; }

static uint64_t now(void) { return hal_monotonic_micros(); }

int main(int argc, char **argv)
{
    static uint8_t code[] = { 0, 1, 0, 2, 1, 2, 3 };    /* push 1, push 2, add, pop, loop */
    uint64_t t0;
    uint32_t i, acc = 0;
    int local;
    uint8_t *big;
    if (hal_init(argc, argv) != 0) return 20;
    hal_log("code %p  data %p  stack %p\n", (void *)main, (void *)&g_a, (void *)&local);
#ifdef HAVE_MEMLIST
    {
        /* exec's memory regions in the order AllocMem searches them */
        struct { char name[24]; long pri; unsigned long attr, lo, hi, free, largest; } r[8];
        struct MemHeader *mh;
        int n = 0, k;
        Forbid();
        for (mh = (struct MemHeader *)SysBase->MemList.lh_Head; mh->mh_Node.ln_Succ && n < 8;
             mh = (struct MemHeader *)mh->mh_Node.ln_Succ, n++) {
            struct MemChunk *c;
            const char *nm = mh->mh_Node.ln_Name ? mh->mh_Node.ln_Name : "?";
            for (k = 0; k < 23 && nm[k]; k++) r[n].name[k] = nm[k];
            r[n].name[k] = 0;
            r[n].pri = mh->mh_Node.ln_Pri; r[n].attr = mh->mh_Attributes;
            r[n].lo = (unsigned long)mh->mh_Lower; r[n].hi = (unsigned long)mh->mh_Upper; r[n].free = mh->mh_Free;
            r[n].largest = 0;
            for (c = mh->mh_First; c; c = c->mc_Next) if (c->mc_Bytes > r[n].largest) r[n].largest = c->mc_Bytes;
        }
        Permit();
        for (k = 0; k < n; k++)
            hal_log("mem %d: pri %ld attr $%lx  $%08lx-$%08lx  free %lu K, largest %lu K  %s\n", k, r[k].pri, r[k].attr,
                    r[k].lo, r[k].hi, r[k].free >> 10, r[k].largest >> 10, r[k].name);
    }
#endif

#ifdef HAVE_MEMLIST
    if (argc > 1) {
        /* where does exec put blocks of several sizes, for several requirement flags? */
        static const unsigned long sizes[] = { 64, 12000, 70000, 700000, 9000000 };
        static const struct { unsigned long f; const char *n; } fl[] = {
            { MEMF_ANY, "ANY" }, { MEMF_PUBLIC, "PUBLIC" }, { MEMF_PUBLIC | MEMF_CLEAR, "PUBLIC|CLEAR" }, { MEMF_FAST, "FAST" },
            { MEMF_FAST | MEMF_KICK, "FAST|KICK" }, { MEMF_FAST | MEMF_LOCAL, "FAST|LOCAL" } };
        unsigned a, b;
        for (b = 0; b < sizeof(fl) / sizeof(fl[0]); b++) {
            char line[200]; int o = 0;
            for (a = 0; a < 5; a++) {
                void *m = AllocMem(sizes[a], fl[b].f);
                o += sprintf(line + o, " %08lx", (unsigned long)m);
                if (m) FreeMem(m, sizes[a]);
            }
            hal_log("%-13s%s\n", fl[b].n, line);
        }
        { void *m = malloc(64), *m2 = malloc(9000000); hal_log("malloc 64 %p, 9 MB %p\n", m, m2); free(m); free(m2); }
        hal_shutdown(); return 0;
    }
#endif
    t0 = now();
    for (i = 0; i < 1000000u; i++) acc += i ^ (acc >> 3);
    hal_log("A register loop 1M        %lu us (%lu)\n", (unsigned long)(now() - t0), (unsigned long)acc);

    t0 = now(); g_a = 0; g_b = 0;
    for (i = 0; i < 1000000u; i++) { g_a += i; g_b ^= g_a; }
    hal_log("B globals in memory 1M    %lu us\n", (unsigned long)(now() - t0));

    s_table[0] = op_push; s_table[1] = op_add; s_table[2] = op_pop; s_table[3] = op_loop;
    t0 = now(); g_a = 200000; g_pc = code; g_sp = g_stack; s_stop = 0;
    while (!s_stop) s_table[*g_pc]();
    hal_log("C table dispatch 1M ops   %lu us\n", (unsigned long)(now() - t0));

    big = (uint8_t *)malloc(4 << 20);
    hal_log("heap %p\n", (void *)big);
    if (big) {
        t0 = now(); memset(big, 1, 4 << 20);
        hal_log("D memset 4 MB             %lu us\n", (unsigned long)(now() - t0));
        t0 = now(); acc = 0;
        for (i = 0; i < (4u << 20); i += 16) acc += big[i];
        hal_log("E read 4 MB, every 16th   %lu us (%lu)\n", (unsigned long)(now() - t0), (unsigned long)acc);
        t0 = now(); acc = 0;
        for (i = 0; i < 1000000u; i++) acc += big[(i * 2654435761u) >> 10 & ((4u << 20) - 1)];
        hal_log("F 1M scattered reads      %lu us (%lu)\n", (unsigned long)(now() - t0), (unsigned long)acc);
        free(big);
    }
    {
        volatile uint64_t n = 123456789012345ull; volatile uint32_t d = 1000; uint64_t q = 0;
        t0 = now();
        for (i = 0; i < 10000u; i++) q += n / d;
        hal_log("G 10k 64-bit divisions    %lu us\n", (unsigned long)(now() - t0));
    }
    t0 = now();
    for (i = 0; i < 10000u; i++) acc += (uint32_t)now();
    hal_log("H 10k clock reads         %lu us\n", (unsigned long)(now() - t0));
    {
        volatile double x = 1.000001, y = 1.0;
        t0 = now();
        for (i = 0; i < 100000u; i++) y = y * x + 0.5;
        hal_log("I 100k double mul+add     %lu us\n", (unsigned long)(now() - t0));
    }
    hal_shutdown();
    return 0;
}
