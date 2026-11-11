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
