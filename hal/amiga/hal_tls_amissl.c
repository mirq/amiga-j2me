/* amiga-j2me HAL: TLS client connections through AmiSSL (amisslmaster.library
 * v5, OpenSSL 3), for https:// and ssl://. AOS3, MorphOS and WarpOS.
 *
 * No AmiSSL SDK is needed to build this: the few library calls are spelled out
 * below with their offsets and registers (from the AmiSSL 5 SDK's inline
 * headers). OpenSSL talks to memory buffers and this file moves the bytes
 * to and from the socket with hal_net_read/write, so it does not matter what
 * kind of descriptor the platform's socket layer hands out.
 * When AmiSSL is not installed, hal_tls_open fails and says so in the log. */
#include "hal.h"
#include <exec/types.h>
#include <exec/libraries.h>
#include <utility/tagitem.h>
#include <dos/var.h>
#include <proto/exec.h>
#include <proto/dos.h>
#include <string.h>

/* A call into a 68k library: the LPn macros of each SDK, which differ in what
 * follows the library base. */
#if defined(__MORPHOS__)
#include <ppcinline/macros.h>
#define AJ_X , 0, 0, 0, 0, 0, 0
#elif defined(AJ_AMIGA_WOS)
/* <proto/exec.h> has brought in the PPC-to-68k macros; the rest is how to treat the caches */
#define AJ_X , IF_CACHEFLUSHALL, NULL, 0, IF_CACHEFLUSHALL, NULL, 0
#else
#include <inline/macros.h>
#define AJ_X
#endif
#define AJ_CALL(m, ...) AJ_CALL2(m, (__VA_ARGS__ AJ_X))
#define AJ_CALL2(m, args) m args

extern struct Library *SocketBase;
static struct Library *s_master, *s_ssl;
static int s_errno;
static APTR s_ctx;
static int s_failed;

#define AMISSL_API_VERSION 21           /* AMISSL_V303: the first OpenSSL 3 API level */
#define AmiSSL_SocketBase (TAG_USER + 0x01)
#define AmiSSL_ErrNoPtr   (TAG_USER + 0x0b)

#define InitAmiSSLMaster(v, structs)  AJ_CALL(LP2, 0x1e, LONG, InitAmiSSLMaster, LONG, v, d0, LONG, structs, d1, , s_master)
#define OpenAmiSSL()                  AJ_CALL(LP0, 0x24, struct Library *, OpenAmiSSL, , s_master)
#define CloseAmiSSL()                 AJ_CALL(LP0NR, 0x2a, CloseAmiSSL, , s_master)
#define InitAmiSSLA(tags)             AJ_CALL(LP1, 0x24, long, InitAmiSSLA, struct TagItem *, tags, a0, , s_ssl)
#define CleanupAmiSSLA(tags)          AJ_CALL(LP1, 0x2a, long, CleanupAmiSSLA, struct TagItem *, tags, a0, , s_ssl)
#define BIO_new(type)                 AJ_CALL(LP1, 0x6c0, APTR, BIO_new, APTR, type, a0, , s_ssl)
#define BIO_read(b, d, n)             AJ_CALL(LP3, 0x6d8, int, BIO_read, APTR, b, a0, APTR, d, a1, int, n, d0, , s_ssl)
#define BIO_write(b, d, n)            AJ_CALL(LP3, 0x6e4, int, BIO_write, APTR, b, a0, APTR, d, a1, int, n, d0, , s_ssl)
#define BIO_ctrl(b, cmd, l, p)        AJ_CALL(LP4, 0x6f6, long, BIO_ctrl, APTR, b, a0, int, cmd, d0, long, l, d1, APTR, p, a1, , s_ssl)
#define BIO_s_mem()                   AJ_CALL(LP0, 0x75c, APTR, BIO_s_mem, , s_ssl)
#define ERR_get_error()               AJ_CALL(LP0, 0xf78, unsigned long, ERR_get_error, , s_ssl)
#define ERR_error_string_n(e, b, n)   AJ_CALL(LP3NR, 0xfba, ERR_error_string_n, unsigned long, e, d0, char *, b, a0, unsigned long, n, d1, , s_ssl)
#define SSL_CTX_new(m)                AJ_CALL(LP1, 0x2010, APTR, SSL_CTX_new, APTR, m, a0, , s_ssl)
#define SSL_CTX_free(c)               AJ_CALL(LP1NR, 0x2016, SSL_CTX_free, APTR, c, a0, , s_ssl)
#define SSL_pending(s)                AJ_CALL(LP1, 0x20a0, int, SSL_pending, APTR, s, a0, , s_ssl)
#define SSL_set_bio(s, r, w)          AJ_CALL(LP3NR, 0x20b8, SSL_set_bio, APTR, s, a0, APTR, r, a1, APTR, w, a2, , s_ssl)
#define SSL_CTX_set_verify(c, m, cb)  AJ_CALL(LP3NR, 0x21fc, SSL_CTX_set_verify, APTR, c, a0, int, m, d0, APTR, cb, a1, , s_ssl)
#define SSL_new(c)                    AJ_CALL(LP1, 0x2250, APTR, SSL_new, APTR, c, a0, , s_ssl)
#define SSL_free(s)                   AJ_CALL(LP1NR, 0x2274, SSL_free, APTR, s, a0, , s_ssl)
#define SSL_connect(s)                AJ_CALL(LP1, 0x2280, int, SSL_connect, APTR, s, a0, , s_ssl)
#define SSL_read(s, b, n)             AJ_CALL(LP3, 0x2286, int, SSL_read, APTR, s, a0, APTR, b, a1, int, n, d0, , s_ssl)
#define SSL_write(s, b, n)            AJ_CALL(LP3, 0x2292, int, SSL_write, APTR, s, a0, APTR, b, a1, int, n, d0, , s_ssl)
#define SSL_ctrl(s, cmd, l, p)        AJ_CALL(LP4, 0x2298, long, SSL_ctrl, APTR, s, a0, int, cmd, d0, long, l, d1, APTR, p, a1, , s_ssl)
#define SSL_get_error(s, r)           AJ_CALL(LP2, 0x22b0, int, SSL_get_error, APTR, s, a0, int, r, d0, , s_ssl)
#define SSL_shutdown(s)               AJ_CALL(LP1, 0x2322, int, SSL_shutdown, APTR, s, a0, , s_ssl)
#define SSL_CTX_set_default_verify_paths(c) AJ_CALL(LP1, 0x23d0, int, SSL_CTX_set_default_verify_paths, APTR, c, a0, , s_ssl)
#define SSL_set1_host(s, h)           AJ_CALL(LP2, 0x68e8, int, SSL_set1_host, APTR, s, a0, APTR, h, a1, , s_ssl)
#define TLS_client_method()           AJ_CALL(LP0, 0x6936, APTR, TLS_client_method, , s_ssl)

#define BIO_CTRL_PENDING 10
#define SSL_CTRL_SET_TLSEXT_HOSTNAME 55
#define SSL_ERROR_WANT_READ 2
#define SSL_ERROR_WANT_WRITE 3
#define SSL_VERIFY_PEER 1

#define MAX_TLS 8
typedef struct { APTR ssl, rbio, wbio; int sock, used; } Tls;
static Tls s_tls[MAX_TLS];

static void log_ssl_error(const char *what)
{
    char msg[160];
    unsigned long e = ERR_get_error();
    msg[0] = 0;
    if (e) ERR_error_string_n(e, msg, sizeof(msg));
    hal_log("TLS: %s%s%s\n", what, msg[0] ? ": " : "", msg);
    while (ERR_get_error()) {}
}

static int tls_init(void)
{
    struct TagItem tags[3];
    char env[8];
    if (s_ctx) return 1;
    if (s_failed) return 0;
    s_failed = 1;
    s_master = OpenLibrary((CONST_STRPTR)"amisslmaster.library", 5);
    if (!s_master) { hal_log("TLS: https needs AmiSSL 5 (amisslmaster.library), which is not installed\n"); return 0; }
    if (!InitAmiSSLMaster(AMISSL_API_VERSION, TRUE) || !(s_ssl = OpenAmiSSL())) {
        hal_log("TLS: this AmiSSL is too old (version 5 is needed)\n");
        CloseLibrary(s_master); s_master = NULL;
        return 0;
    }
    tags[0].ti_Tag = AmiSSL_ErrNoPtr; tags[0].ti_Data = (ULONG)&s_errno;
    tags[1].ti_Tag = AmiSSL_SocketBase; tags[1].ti_Data = (ULONG)SocketBase;
    tags[2].ti_Tag = TAG_DONE; tags[2].ti_Data = 0;
    if (InitAmiSSLA(tags) != 0) {
        hal_log("TLS: AmiSSL did not start\n");
        CloseAmiSSL(); CloseLibrary(s_master); s_master = NULL; s_ssl = NULL;
        return 0;
    }
    s_ctx = SSL_CTX_new(TLS_client_method());
    if (!s_ctx) { log_ssl_error("no context"); return 0; }
    /* certificates are checked against AmiSSL's store unless the user turns that off */
    if (GetVar((CONST_STRPTR)"AJ_TLS_NOVERIFY", (STRPTR)env, sizeof(env), GVF_GLOBAL_ONLY) > 0) {
        hal_log("TLS: certificates are not checked (AJ_TLS_NOVERIFY is set)\n");
    } else {
        SSL_CTX_set_default_verify_paths(s_ctx);
        SSL_CTX_set_verify(s_ctx, SSL_VERIFY_PEER, NULL);
    }
    s_failed = 0;
    return 1;
}

void hal_tls_shutdown(void)
{
    int i;
    for (i = 0; i < MAX_TLS; i++) if (s_tls[i].used) hal_tls_close(i);
    if (s_ctx) { SSL_CTX_free(s_ctx); s_ctx = NULL; }
    if (s_ssl) {
        struct TagItem done; done.ti_Tag = TAG_DONE; done.ti_Data = 0;
        CleanupAmiSSLA(&done);
        CloseAmiSSL(); s_ssl = NULL;
    }
    if (s_master) { CloseLibrary(s_master); s_master = NULL; }
}

/* what OpenSSL wants to send goes out to the socket */
static int flush_out(Tls *t)
{
    char buf[1024];
    while (BIO_ctrl(t->wbio, BIO_CTRL_PENDING, 0, NULL) > 0) {
        int n = BIO_read(t->wbio, buf, sizeof(buf)), off = 0;
        if (n <= 0) break;
        while (off < n) {
            int w = hal_net_write(t->sock, buf + off, n - off);
            if (w <= 0) return 0;
            off += w;
        }
    }
    return 1;
}

/* one read from the socket handed to OpenSSL; 0 when the peer is gone */
static int feed_in(Tls *t)
{
    char buf[1024];
    int n = hal_net_read(t->sock, buf, sizeof(buf));
    if (n <= 0) return 0;
    return BIO_write(t->rbio, buf, n) == n;
}

int hal_tls_open(const char *host, int port)
{
    int i, sock;
    Tls *t = NULL;
    sock = hal_net_open(host, port);
    if (sock < 0) return -1;
    if (!tls_init()) { hal_net_close(sock); return -1; }
    for (i = 0; i < MAX_TLS; i++) if (!s_tls[i].used) { t = &s_tls[i]; break; }
    if (!t) { hal_net_close(sock); return -1; }
    t->sock = sock;
    t->ssl = SSL_new(s_ctx);
    t->rbio = t->ssl ? BIO_new(BIO_s_mem()) : NULL;
    t->wbio = t->rbio ? BIO_new(BIO_s_mem()) : NULL;
    if (!t->wbio) {
        log_ssl_error("out of memory");
        if (t->ssl) SSL_free(t->ssl);
        hal_net_close(sock);
        return -1;
    }
    SSL_set_bio(t->ssl, t->rbio, t->wbio);      /* the SSL owns them now */
    SSL_ctrl(t->ssl, SSL_CTRL_SET_TLSEXT_HOSTNAME, 0, (APTR)host);
    SSL_set1_host(t->ssl, (APTR)host);
    for (;;) {
        int r = SSL_connect(t->ssl), err;
        if (!flush_out(t)) { r = -1; err = 0; }
        else if (r == 1) break;
        else err = SSL_get_error(t->ssl, r);
        if (err == SSL_ERROR_WANT_READ && feed_in(t)) continue;
        if (err == SSL_ERROR_WANT_WRITE) continue;
        log_ssl_error("the handshake failed");
        SSL_free(t->ssl);
        hal_net_close(sock);
        return -1;
    }
    t->used = 1;
    return i;
}

int hal_tls_read(int handle, void *buf, int len)
{
    Tls *t;
    if ((unsigned)handle >= MAX_TLS || !s_tls[handle].used) return -1;
    t = &s_tls[handle];
    for (;;) {
        int n = SSL_read(t->ssl, buf, len), err;
        if (n > 0) return n;
        err = SSL_get_error(t->ssl, n);
        if (!flush_out(t)) return -1;
        if (err == SSL_ERROR_WANT_READ) { if (!feed_in(t)) return 0; continue; }
        while (ERR_get_error()) {}
        return 0;       /* closed, cleanly or not */
    }
}

int hal_tls_write(int handle, const void *buf, int len)
{
    Tls *t;
    int n;
    if ((unsigned)handle >= MAX_TLS || !s_tls[handle].used) return -1;
    t = &s_tls[handle];
    n = SSL_write(t->ssl, (APTR)buf, len);
    if (n <= 0) {
        int err = SSL_get_error(t->ssl, n);
        hal_log("TLS: write of %d bytes returned %d, error %d\n", len, n, err);
        log_ssl_error("write failed");
        return -1;
    }
    if (!flush_out(t)) { hal_log("TLS: the socket did not take the data\n"); return -1; }
    return n;
}

int hal_tls_available(int handle)
{
    if ((unsigned)handle >= MAX_TLS || !s_tls[handle].used) return 0;
    return SSL_pending(s_tls[handle].ssl);
}

void hal_tls_close(int handle)
{
    Tls *t;
    if ((unsigned)handle >= MAX_TLS || !s_tls[handle].used) return;
    t = &s_tls[handle];
    SSL_shutdown(t->ssl);
    flush_out(t);
    SSL_free(t->ssl);
    hal_net_close(t->sock);
    t->used = 0;
}
