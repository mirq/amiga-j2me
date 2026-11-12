/* KNI natives of aj.Native: whole-file access for record stores. GPLv2. */
#include <stdlib.h>
#include <string.h>
#include "kni.h"
#include "sni.h"
#include "hal.h"

#define PATH_MAX_CHARS 300

/* Java String parameter -> C path (Latin-1; other characters become '_'). */
static void get_path(int index, char *out)
{
    jchar buf[PATH_MAX_CHARS];
    int n, i;
    KNI_StartHandles(1);
    KNI_DeclareHandle(hs);
    KNI_GetParameterAsObject(index, hs);
    n = KNI_IsNullHandle(hs) ? 0 : KNI_GetStringLength((jstring)hs);
    if (n > PATH_MAX_CHARS - 1) n = PATH_MAX_CHARS - 1;
    if (n > 0) KNI_GetStringRegion((jstring)hs, 0, n, buf);
    for (i = 0; i < n; i++) out[i] = buf[i] < 256 ? (char)buf[i] : '_';
    out[n] = 0;
    KNI_EndHandles();
}

/* byte[] readFile(String path): null when the file does not exist */
KNIEXPORT KNI_RETURNTYPE_OBJECT Java_aj_Native_readFile(void)
{
    char path[PATH_MAX_CHARS];
    size_t size = 0;
    void *data;
    get_path(1, path);
    data = hal_load_file(path, &size);
    KNI_StartHandles(1);
    KNI_DeclareHandle(harr);
    if (data) {
        SNI_NewArray(SNI_BYTE_ARRAY, (jint)size, harr);
        if (!KNI_IsNullHandle(harr) && size > 0)
            memcpy(SNI_GetRawArrayPointer((jarray)harr), data, size);
        hal_free(data);
    }
    KNI_EndHandlesAndReturnObject(harr);
}

/* int writeFile(String path, byte[] data, int len): 0 = ok */
KNIEXPORT KNI_RETURNTYPE_INT Java_aj_Native_writeFile(void)
{
    char path[PATH_MAX_CHARS];
    int rc;
    get_path(1, path);
    KNI_StartHandles(1);
    KNI_DeclareHandle(hdata);
    KNI_GetParameterAsObject(2, hdata);
    rc = hal_save_file(path, SNI_GetRawArrayPointer((jarray)hdata), (size_t)KNI_GetParameterAsInt(3));
    KNI_EndHandles();
    KNI_ReturnInt(rc);
}

KNIEXPORT KNI_RETURNTYPE_INT Java_aj_Native_deleteFile(void)
{
    char path[PATH_MAX_CHARS];
    get_path(1, path);
    KNI_ReturnInt(hal_delete_file(path));
}

KNIEXPORT KNI_RETURNTYPE_INT Java_aj_Native_makeDir(void)
{
    char path[PATH_MAX_CHARS];
    get_path(1, path);
    KNI_ReturnInt(hal_make_dir(path));
}

/* String listFiles(String dir): file names separated by '\n', "" when none */
struct list_ctx { char *buf; size_t len, cap; };

static void list_cb(const char *name, void *user)
{
    struct list_ctx *c = (struct list_ctx *)user;
    size_t n = strlen(name);
    if (c->len + n + 2 > c->cap) {
        size_t cap = (c->cap ? c->cap * 2 : 1024) + n;
        char *nb = (char *)realloc(c->buf, cap);
        if (!nb) return;
        c->buf = nb; c->cap = cap;
    }
    memcpy(c->buf + c->len, name, n);
    c->len += n;
    c->buf[c->len++] = '\n';
    c->buf[c->len] = 0;
}

KNIEXPORT KNI_RETURNTYPE_OBJECT Java_aj_Native_listFiles(void)
{
    char path[PATH_MAX_CHARS];
    struct list_ctx c;
    memset(&c, 0, sizeof(c));
    get_path(1, path);
    hal_list_dir(path, list_cb, &c);
    KNI_StartHandles(1);
    KNI_DeclareHandle(hs);
    KNI_NewStringUTF(c.buf ? c.buf : "", (jstring)hs);
    free(c.buf);
    KNI_EndHandlesAndReturnObject(hs);
}


/* ---- timing probe (diagnostics): int probe(int what) = microseconds ----
 * 0: 1M register loop, 1: 1M ops through a call table with state in globals
 * (the shape of the interpreter), 2: 1M reads/writes of two globals */
static volatile uint32_t pr_a, pr_b;
static uint8_t *pr_pc;
static uint32_t *pr_sp;
static uint32_t pr_stack[64];
static void (*pr_table[256])(void);
static volatile int pr_stop;
static void pr_push(void) { *pr_sp++ = pr_pc[1]; pr_pc += 2; }
static void pr_add(void)  { pr_sp--; pr_sp[-1] += pr_sp[0]; pr_pc += 1; }
static void pr_pop(void)  { pr_sp--; pr_pc += 1; }
static void pr_loop(void) { if (--pr_a == 0) pr_stop = 1; pr_pc -= 6; }

KNIEXPORT KNI_RETURNTYPE_INT Java_aj_Native_probe(void)
{
    static uint8_t code[] = { 0, 1, 0, 2, 1, 2, 3 };
    int what = KNI_GetParameterAsInt(1);
    uint64_t t0 = hal_monotonic_micros();
    uint32_t i, acc = 0;
    if (what == 0) {
        for (i = 0; i < 1000000u; i++) acc += i ^ (acc >> 3);
        pr_b = acc;
    } else if (what == 1) {
        pr_table[0] = pr_push; pr_table[1] = pr_add; pr_table[2] = pr_pop; pr_table[3] = pr_loop;
        pr_a = 200000; pr_pc = code; pr_sp = pr_stack; pr_stop = 0;
        while (!pr_stop) pr_table[*pr_pc]();
    } else if (what == 2) {
        pr_a = 0; pr_b = 0;
        for (i = 0; i < 1000000u; i++) { pr_a += i; pr_b ^= pr_a; }
    } else if (what == 3) {                 /* the same two cells, on the C stack */
        volatile uint32_t sa = 0, sb = 0;
        for (i = 0; i < 1000000u; i++) { sa += i; sb ^= sa; }
    } else if (what == 4) {                 /* and in malloc memory */
        volatile uint32_t *m = (volatile uint32_t *)malloc(64);
        if (m) { m[0] = 0; m[1] = 0; for (i = 0; i < 1000000u; i++) { m[0] += i; m[1] ^= m[0]; } free((void *)m); }
#if defined(__mc68000__)
    } else if (what >= 20 && what < 30) {   /* where does exec put 70000 bytes for these flags, in this process? */
        static const unsigned long fl[] = { 0, 1, 4, 4 | 0x400, 4 | 0x100, 0x400, 0x100, 1 | 0x10000, 4 | 0x400 | 0x100 | 1 };
        extern void *aj_probe_alloc(unsigned long size, unsigned long flags);
        KNI_ReturnInt((jint)(size_t)aj_probe_alloc(what == 29 ? 9000000 : 70000, fl[what - 20 > 8 ? 0 : what - 20]));
#endif
#if defined(WOS)
    } else if (what == 30 || what == 31) {
        extern int aj_probe_coherency(int flush);
        KNI_ReturnInt(aj_probe_coherency(what == 31));
#endif
    } else if (what == 10) { KNI_ReturnInt((jint)(size_t)&pr_a);
    } else if (what == 11) { KNI_ReturnInt((jint)(size_t)&Java_aj_Native_probe);
    } else if (what == 12) { volatile int on_stack = 0; KNI_ReturnInt((jint)(size_t)&on_stack);
    } else if (what == 13) { void *m = malloc(64); free(m); KNI_ReturnInt((jint)(size_t)m);
    } else if (what == 14) { KNI_ReturnInt((jint)(size_t)code);
    }
    KNI_ReturnInt((jint)(hal_monotonic_micros() - t0));
}
