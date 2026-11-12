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
#include "amiga_os.h"
#include <exec/types.h>
#include <exec/libraries.h>
#include <utility/tagitem.h>
#include <dos/var.h>
#include <proto/exec.h>
#include <proto/dos.h>
#include <string.h>
#include <stdio.h>
#include <stdint.h>

/* A call into a 68k library. On the 68060 and under MorphOS: the LPn macros of
 * each SDK, which differ in what follows the library base. Under WarpOS the
 * call is made by a 68k helper process instead (see helper_start below). */
#if defined(__MORPHOS__)
#include <ppcinline/macros.h>
#define AJ_X , 0, 0, 0, 0, 0, 0
#define AJ_CALL(m, ...) AJ_CALL2(m, (__VA_ARGS__ AJ_X))
#define AJ_CALL2(m, args) m args
#define LIB_OPEN(name, ver) OpenLibrary((CONST_STRPTR)(name), (ver))
#define LIB_CLOSE(base)     CloseLibrary((struct Library *)(base))
#elif defined(AJ_AMIGA_WOS)
#include <stdarg.h>
#include <dos/dostags.h>
#include "stub/tls68k.h"
#include <powerpc/powerpc.h>
#include <powerpc/powerpc_protos.h>
enum { R_d0, R_d1, R_a0, R_a1, R_a2, R_a3 };
static ULONG rpc(ULONG base, ULONG offset, int nargs, ...);
#define AJ_CALL(m, ...) AJ_R##m(__VA_ARGS__)
#define AJ_RLP0(o, rt, name, bt, bn)                        ((rt)rpc((ULONG)(bn), o, 0))
#define AJ_RLP0NR(o, name, bt, bn)                          ((void)rpc((ULONG)(bn), o, 0))
#define AJ_RLP1(o, rt, name, t1, v1, r1, bt, bn)            ((rt)rpc((ULONG)(bn), o, 1, R_##r1, (ULONG)(v1)))
#define AJ_RLP1NR(o, name, t1, v1, r1, bt, bn)              ((void)rpc((ULONG)(bn), o, 1, R_##r1, (ULONG)(v1)))
#define AJ_RLP2(o, rt, name, t1, v1, r1, t2, v2, r2, bt, bn) \
    ((rt)rpc((ULONG)(bn), o, 2, R_##r1, (ULONG)(v1), R_##r2, (ULONG)(v2)))
#define AJ_RLP3(o, rt, name, t1, v1, r1, t2, v2, r2, t3, v3, r3, bt, bn) \
    ((rt)rpc((ULONG)(bn), o, 3, R_##r1, (ULONG)(v1), R_##r2, (ULONG)(v2), R_##r3, (ULONG)(v3)))
#define AJ_RLP3NR(o, name, t1, v1, r1, t2, v2, r2, t3, v3, r3, bt, bn) \
    ((void)rpc((ULONG)(bn), o, 3, R_##r1, (ULONG)(v1), R_##r2, (ULONG)(v2), R_##r3, (ULONG)(v3)))
#define AJ_RLP4(o, rt, name, t1, v1, r1, t2, v2, r2, t3, v3, r3, t4, v4, r4, bt, bn) \
    ((rt)rpc((ULONG)(bn), o, 4, R_##r1, (ULONG)(v1), R_##r2, (ULONG)(v2), R_##r3, (ULONG)(v3), R_##r4, (ULONG)(v4)))
#define LIB_OPEN(name, ver) ((struct Library *)rpc((ULONG)SysBase, 0x228, 2, R_a1, (ULONG)(name), R_d0, (ULONG)(ver)))
#define LIB_CLOSE(base)     ((void)rpc((ULONG)SysBase, 0x19e, 1, R_a1, (ULONG)(base)))
#else
#include <inline/macros.h>
#define AJ_X
#define AJ_CALL(m, ...) AJ_CALL2(m, (__VA_ARGS__ AJ_X))
#define AJ_CALL2(m, args) m args
#define LIB_OPEN(name, ver) OpenLibrary((CONST_STRPTR)(name), (ver))
#define LIB_CLOSE(base)     CloseLibrary((struct Library *)(base))
#endif

/* On a real PowerPC card the two CPUs have their own data caches. Before the
 * 68k (AmiSSL) reads memory the PPC wrote, or writes memory the PPC will read,
 * the PPC's copy of those lines has to go: written back and dropped. A no-op
 * on the 68060 and under MorphOS. */
#define SHARE(p, n) amiga_cache_flush((p), (unsigned long)(n))

extern struct Library *SocketBase;

static int s_trace_steps;      /* AJ_TLS_LOG=2: every step of the start-up is reported */
#define STEP(text) do { if (s_trace_steps) hal_log("TLS step: %s\n", text); } while (0)

#if defined(AJ_AMIGA_WOS)
/* ---- the 68k helper process (WarpOS) ----
 * The Sonnet powerpc.library serves a PPC task's 68k side from "ppc memory",
 * which a 68060 reaches several times more slowly than its Fast RAM (measured
 * on a real card: AmiSSL's handshake took 3.5 times as long as from a 68k
 * program), and AmiSSL allocates a lot. So every AmiSSL call is carried out by
 * a 68k process of its own, stub/tls68k.s: the registers go there in a
 * message, the result comes back in the reply. That process must not be made
 * by this task (it would be marked PPC as well): see the stub for how it is
 * started clean. */
typedef struct { struct Message msg; ULONG fn, a6, d0, d1, a0, a1, a2, a3, result, quit; } Req;
typedef struct { ULONG port, gone; } Shared;
static Shared *s_shared;
static Req *s_req;
static UBYTE *s_stub;                  /* the stub's bytes, as the 68k runs boot() from them */
static APTR s_block;                   /* the Fast RAM block the helper lives in */
#define STUB_BYTES ((sizeof(tls68k_code) + 7u) & ~7u)
static struct MsgPort *s_reply;
static struct MsgPort *s_helper;       /* the helper's port */
static struct Library *s_hsocket;      /* bsdsocket.library as opened by the helper, for AmiSSL */

static ULONG peek68k(const ULONG *where)
{
    ULONG v = 0;
    CopyMem((APTR)where, &v, sizeof(v));        /* read by the 68k: what it wrote is certainly seen */
    return v;
}

static int s_use_helper;       /* calls go through the helper process (Sonnet cards); else straight to this task's 68k side */

static ULONG rpc(ULONG base, ULONG offset, int nargs, ...)
{
    va_list ap;
    ULONG *regs = &s_req->d0;
    int i;
    if (!s_use_helper) {
        /* the plain way: this task's 68k side runs the function */
        static const int where[] = { PPREG_D0, PPREG_D1, PPREG_A0, PPREG_A1, PPREG_A2, PPREG_A3 };
        struct PPCArgs args;
        memset(&args, 0, sizeof(args));
        va_start(ap, nargs);
        for (i = 0; i < nargs; i++) { int r = va_arg(ap, int); args.PP_Regs[where[r]] = va_arg(ap, ULONG); }
        va_end(ap);
        args.PP_Code = (APTR)base;
        args.PP_Offset = -(LONG)offset;
        args.PP_Regs[PPREG_A6] = base;
        Run68K(&args);
        return args.PP_Regs[PPREG_D0];
    }
    if (!s_helper) return 0;
    memset(s_req, 0, sizeof(*s_req));
    va_start(ap, nargs);
    for (i = 0; i < nargs; i++) { int r = va_arg(ap, int); regs[r] = va_arg(ap, ULONG); }
    va_end(ap);
    s_req->fn = base - offset;
    s_req->a6 = base;
    s_req->msg.mn_Node.ln_Type = NT_MESSAGE;
    s_req->msg.mn_ReplyPort = s_reply;
    s_req->msg.mn_Length = sizeof(Req);
    PutMsg(s_helper, &s_req->msg);
    WaitPort(s_reply);
    GetMsg(s_reply);
    return s_req->result;
}

static void helper_stop(void)
{
    int i;
    if (s_helper) {
        memset(s_req, 0, sizeof(*s_req));
        s_req->quit = 1;
        s_req->msg.mn_Node.ln_Type = NT_MESSAGE;
        s_req->msg.mn_ReplyPort = s_reply;
        s_req->msg.mn_Length = sizeof(Req);
        PutMsg(s_helper, &s_req->msg);
        WaitPort(s_reply);
        GetMsg(s_reply);
        s_helper = NULL;
        for (i = 0; i < 250 && !peek68k(&s_shared->gone); i++) Delay(1);    /* its code must be left before it is freed */
    }
    if (s_reply) { DeleteMsgPort(s_reply); s_reply = NULL; }
    if (s_block) { FreeMem(s_block, TLS68K_HEAD + STUB_BYTES); s_block = NULL; }
    if (s_stub) { FreeVec(s_stub); s_stub = NULL; }
    if (s_req) { FreeVec(s_req); s_req = NULL; }
    if (s_shared) { FreeVec(s_shared); s_shared = NULL; }
}

static int helper_start(void)
{
    struct PPCArgs args;
    ULONG shared_addr;
    int i;
    if (s_helper) return 1;
    STEP("helper: memory");
    s_shared = (Shared *)AllocVec(sizeof(Shared), MEMF_PUBLIC | MEMF_CLEAR);
    s_req = (Req *)AllocVec(sizeof(Req), MEMF_PUBLIC | MEMF_CLEAR);
    s_stub = (UBYTE *)AllocVec(STUB_BYTES, MEMF_PUBLIC | MEMF_CLEAR);
    s_reply = CreateMsgPort();
    if (!s_shared || !s_req || !s_stub || !s_reply) { helper_stop(); return 0; }
    memcpy(s_stub, tls68k_code, sizeof(tls68k_code));
    shared_addr = (ULONG)s_shared;
    memcpy(s_stub + TLS68K_PATCH_OFFSET, &shared_addr, 4);
    amiga_cache_flush(s_stub, STUB_BYTES);
    CacheClearU();
    STEP("helper: starting it");
    memset(&args, 0, sizeof(args));
    args.PP_Code = (APTR)(s_stub + TLS68K_BOOT_OFFSET);
    args.PP_Regs[PPREG_A0] = (ULONG)s_stub;
    args.PP_Regs[PPREG_D0] = STUB_BYTES;
    Run68K(&args);
    s_block = (APTR)args.PP_Regs[PPREG_D0];
    if (!s_block) { hal_log("TLS: no Fast RAM for the helper process\n"); helper_stop(); return 0; }
    if (s_trace_steps) hal_log("TLS step: helper block at %08lx\n", (unsigned long)s_block);
    for (i = 0; i < 250; i++) {
        ULONG port = peek68k(&s_shared->port);
        if (port) { s_helper = (struct MsgPort *)port; STEP("helper: running"); return 1; }
        if (peek68k(&s_shared->gone)) { hal_log("TLS: no helper process\n"); Delay(5); helper_stop(); return 0; }
        Delay(1);
    }
    hal_log("TLS: the helper process did not start\n");
    /* it may still come up: its memory is left alone */
    s_block = NULL; s_shared = NULL;
    helper_stop();
    return 0;
}
#endif
static struct Library *s_master, *s_ssl;
/* AmiSSL's errno cell, with room on both sides in case it writes more than a long there */
static struct { ULONG before[8]; int value; ULONG after[8]; } s_errno_box;
#define s_errno (s_errno_box.value)
static APTR s_ctx;
static APTR s_store;         /* the trusted certificates; NULL: no checking (AJ_TLS_NOVERIFY) */
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
#define SSL_SESSION_free(x)            AJ_CALL(LP1NR, 0x21a8, SSL_SESSION_free, APTR, x, a0, , s_ssl)
#define SSL_set_session(s, x)          AJ_CALL(LP2, 0x21b4, int, SSL_set_session, APTR, s, a0, APTR, x, a1, , s_ssl)
#define SSL_get1_session(s)            AJ_CALL(LP1, 0x23e2, APTR, SSL_get1_session, APTR, s, a0, , s_ssl)
#define SSL_session_reused(s)          AJ_CALL(LP1, 0x68dc, int, SSL_session_reused, APTR, s, a0, , s_ssl)
#define SSL_get1_peer_certificate(s)   AJ_CALL(LP1, 0x21de, APTR, SSL_get1_peer_certificate, APTR, s, a0, , s_ssl)
#define SSL_get_peer_cert_chain(s)     AJ_CALL(LP1, 0x21e4, APTR, SSL_get_peer_cert_chain, APTR, s, a0, , s_ssl)
#define X509_verify_cert_error_string(n) AJ_CALL(LP1, 0x2664, const char *, X509_verify_cert_error_string, long, n, d0, , s_ssl)
#define X509_free(x)                   AJ_CALL(LP1NR, 0x297c, X509_free, APTR, x, a0, , s_ssl)
#define X509_verify_cert(c)            AJ_CALL(LP1, 0x2e44, int, X509_verify_cert, APTR, c, a0, , s_ssl)
#define X509_STORE_new()               AJ_CALL(LP0, 0x2f46, APTR, X509_STORE_new, , s_ssl)
#define X509_STORE_free(x)             AJ_CALL(LP1NR, 0x2f4c, X509_STORE_free, APTR, x, a0, , s_ssl)
#define X509_STORE_CTX_new()           AJ_CALL(LP0, 0x2f64, APTR, X509_STORE_CTX_new, , s_ssl)
#define X509_STORE_CTX_free(c)         AJ_CALL(LP1NR, 0x2f70, X509_STORE_CTX_free, APTR, c, a0, , s_ssl)
#define X509_STORE_CTX_init(c, st, x, ch) AJ_CALL(LP4, 0x2f76, int, X509_STORE_CTX_init, APTR, c, a0, APTR, st, a1, APTR, x, a2, APTR, ch, a3, , s_ssl)
#define X509_STORE_set_default_paths(x) AJ_CALL(LP1, 0x2ffa, int, X509_STORE_set_default_paths, APTR, x, a0, , s_ssl)
#define X509_STORE_CTX_get_error(c)    AJ_CALL(LP1, 0x3012, int, X509_STORE_CTX_get_error, APTR, c, a0, , s_ssl)
#define X509_STORE_CTX_get0_param(c)   AJ_CALL(LP1, 0x4dee, APTR, X509_STORE_CTX_get0_param, APTR, c, a0, , s_ssl)
#define X509_STORE_CTX_set_default(c, n) AJ_CALL(LP2, 0x4e66, int, X509_STORE_CTX_set_default, APTR, c, a0, APTR, n, a1, , s_ssl)
#define X509_VERIFY_PARAM_set1_host(p, h, n) AJ_CALL(LP3, 0x5dc6, int, X509_VERIFY_PARAM_set1_host, APTR, p, a0, APTR, h, a1, unsigned long, n, d0, , s_ssl)
#define X509_VERIFY_PARAM_set1_ip_asc(p, h) AJ_CALL(LP2, 0x5dd2, int, X509_VERIFY_PARAM_set1_ip_asc, APTR, p, a0, APTR, h, a1, , s_ssl)
#define X509_digest(x, md, out, len)   AJ_CALL(LP4, 0x26c4, int, X509_digest, APTR, x, a0, APTR, md, a1, APTR, out, a2, APTR, len, a3, , s_ssl)
#define EVP_sha256()                   AJ_CALL(LP0, 0x3b58, APTR, EVP_sha256, , s_ssl)
#define TLS_client_method()           AJ_CALL(LP0, 0x6936, APTR, TLS_client_method, , s_ssl)

#define BIO_CTRL_PENDING 10
#define SSL_CTRL_SET_TLSEXT_HOSTNAME 55
#define SSL_ERROR_WANT_READ 2
#define SSL_ERROR_WANT_WRITE 3
#define SSL_VERIFY_PEER 1

#define MAX_TLS 8
typedef struct { APTR ssl, rbio, wbio; int sock, used, port; char host[64]; } Tls;
static Tls s_tls[MAX_TLS];

/* Sessions of the servers talked to last: a second connection to one of them
 * resumes its session and skips the certificates, the slow part of a
 * handshake on a 68060 (10 s and more). */
#define MAX_SESSIONS 4
static struct { char host[64]; int port; APTR session; unsigned long used; } s_sess[MAX_SESSIONS];
static unsigned long s_sess_clock;

static APTR session_for(const char *host, int port)
{
    int i;
    for (i = 0; i < MAX_SESSIONS; i++)
        if (s_sess[i].session && s_sess[i].port == port && !strcmp(s_sess[i].host, host)) { s_sess[i].used = ++s_sess_clock; return s_sess[i].session; }
    return NULL;
}

static void session_keep(const char *host, int port, APTR session)
{
    int i, slot = 0;
    if (!session || !host[0]) { if (session) SSL_SESSION_free(session); return; }
    for (i = 0; i < MAX_SESSIONS; i++) {
        if (s_sess[i].session && s_sess[i].port == port && !strcmp(s_sess[i].host, host)) { slot = i; break; }
        if (s_sess[i].used < s_sess[slot].used) slot = i;
    }
    if (s_sess[slot].session) SSL_SESSION_free(s_sess[slot].session);
    strcpy(s_sess[slot].host, host);
    s_sess[slot].port = port; s_sess[slot].session = session; s_sess[slot].used = ++s_sess_clock;
}

static void log_ssl_error(const char *what)
{
    char msg[160];
    unsigned long e = ERR_get_error();
    msg[0] = 0;
    SHARE(msg, sizeof(msg));
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
    s_trace_steps = GetVar((CONST_STRPTR)"AJ_TLS_LOG", (STRPTR)env, sizeof(env), GVF_GLOBAL_ONLY) > 0 && env[0] == '2';
#if defined(AJ_AMIGA_WOS)
    /* The helper process is for machines with the Sonnet powerpc.library,
     * known by their "ppc memory" pool (memory attribute bit 13): there this
     * task's own 68k side would run AmiSSL out of that slow memory. Elsewhere
     * the 68k side does it directly (and under Amiberry's PowerPC emulation
     * the helper stalls). AJ_TLS_HELPER set to 0 or 1 overrides the choice. */
    s_use_helper = AvailMem(1UL << 13) > 0;
    if (GetVar((CONST_STRPTR)"AJ_TLS_HELPER", (STRPTR)env, sizeof(env), GVF_GLOBAL_ONLY) > 0) s_use_helper = env[0] != '0';
    if (s_use_helper) {
        if (!helper_start()) return 0;
        s_hsocket = LIB_OPEN("bsdsocket.library", 4);
        STEP("bsdsocket opened by the helper");
    } else {
        s_hsocket = SocketBase;
    }
#endif
    s_master = LIB_OPEN("amisslmaster.library", 5);
    STEP("amisslmaster opened");
    if (!s_master) { hal_log("TLS: https needs AmiSSL 5 (amisslmaster.library), which is not installed\n"); return 0; }
    if (!InitAmiSSLMaster(AMISSL_API_VERSION, TRUE) || !(s_ssl = OpenAmiSSL())) {
        hal_log("TLS: this AmiSSL is too old (version 5 is needed)\n");
        LIB_CLOSE(s_master); s_master = NULL;
        return 0;
    }
    tags[0].ti_Tag = AmiSSL_ErrNoPtr; tags[0].ti_Data = (ULONG)&s_errno;
#if defined(AJ_AMIGA_WOS)
    /* experiment: AJ_TLS_NOSOCK leaves the socket library out (the data goes through memory buffers anyway) */
    tags[1].ti_Tag = AmiSSL_SocketBase; tags[1].ti_Data = (ULONG)s_hsocket;
    if (GetVar((CONST_STRPTR)"AJ_TLS_NOSOCK", (STRPTR)env, sizeof(env), GVF_GLOBAL_ONLY) > 0) { tags[1].ti_Tag = TAG_DONE; tags[1].ti_Data = 0; }
#else
    tags[1].ti_Tag = AmiSSL_SocketBase; tags[1].ti_Data = (ULONG)SocketBase;
#endif
    tags[2].ti_Tag = TAG_DONE; tags[2].ti_Data = 0;
    SHARE(tags, sizeof(tags)); SHARE(&s_errno, sizeof(s_errno));
    STEP("AmiSSL opened");
    if (InitAmiSSLA(tags) != 0) {
        hal_log("TLS: AmiSSL did not start\n");
        CloseAmiSSL(); LIB_CLOSE(s_master); s_master = NULL; s_ssl = NULL;
        return 0;
    }
    STEP("AmiSSL initialised");
    {
        char e2[8];
        if (GetVar((CONST_STRPTR)"AJ_TLS_LOG", (STRPTR)e2, sizeof(e2), GVF_GLOBAL_ONLY) > 0)
            hal_log("TLS: amisslmaster at %lx, amissl at %lx, context memory follows\n", (unsigned long)s_master, (unsigned long)s_ssl);
    }
    if (s_trace_steps) {
        int k, touched = 0;
        for (k = 0; k < 8; k++) if (s_errno_box.before[k] || s_errno_box.after[k]) touched = 1;
        hal_log("TLS step: memory round the errno cell %s\n", touched ? "WAS WRITTEN TO" : "untouched");
    }
    {
#if defined(AJ_AMIGA_WOS)
        if (s_trace_steps) {
            ULONG task = rpc((ULONG)SysBase, 0x126, 1, R_a1, 0UL);      /* FindTask(NULL) in the helper */
            hal_log("TLS step: helper task %lx still answers\n", (unsigned long)task);
        }
#endif
        APTR method = TLS_client_method();
        STEP("have the method");
        s_ctx = SSL_CTX_new(method);
        STEP("have the context");
    }
    if (!s_ctx) { log_ssl_error("no context"); return 0; }
    {
        char e2[8];
        if (GetVar((CONST_STRPTR)"AJ_TLS_LOG", (STRPTR)e2, sizeof(e2), GVF_GLOBAL_ONLY) > 0)
            hal_log("TLS: context at %lx\n", (unsigned long)s_ctx);
    }
    /* certificates are checked against AmiSSL's store unless the user turns that off */
    if (GetVar((CONST_STRPTR)"AJ_TLS_NOVERIFY", (STRPTR)env, sizeof(env), GVF_GLOBAL_ONLY) > 0) {
        hal_log("TLS: certificates are not checked (AJ_TLS_NOVERIFY is set)\n");
    } else {
        /* The context itself stays without trusted certificates and does not
         * verify: checking a chain costs a 68060 some 10 s, and servers give up
         * on a handshake that long. The check is made right after the handshake,
         * before anything is sent (verify_peer), against this store. */
        s_store = X509_STORE_new();
        if (!s_store || !X509_STORE_set_default_paths(s_store)) {
            log_ssl_error("no certificate store");
            return 0;
        }
    }
    s_failed = 0;
    return 1;
}

/* Certificates that passed the check, by SHA-256 fingerprint and host, kept
 * for a week in a small file next to the program. Checking a chain costs a
 * 68060 7 to 12 s of arithmetic, longer than many servers wait: with this it
 * is paid once per server, not once per connection or per start. */
#define MAX_KNOWN 16
#define KNOWN_SECONDS (7UL * 24 * 3600)
typedef struct { char host[64]; unsigned char sha[32]; unsigned long until; } Known;
static Known s_known[MAX_KNOWN];
static int s_known_loaded;
static const char *s_known_files[] = { "PROGDIR:aj-tls.cache", "T:aj-tls.cache" };

static unsigned long now_seconds(void) { return (unsigned long)(hal_time_millis() / 1000); }

static void known_load(void)
{
    int f;
    if (s_known_loaded) return;
    s_known_loaded = 1;
    for (f = 0; f < 2; f++) {
        FILE *fp = fopen(s_known_files[f], "rb");
        if (fp) {
            if (fread(s_known, sizeof(Known), MAX_KNOWN, fp) != MAX_KNOWN) memset(s_known, 0, sizeof(s_known));
            fclose(fp);
            return;
        }
    }
}

static int known_has(const char *host, const unsigned char *sha)
{
    int i;
    unsigned long now = now_seconds();
    known_load();
    for (i = 0; i < MAX_KNOWN; i++)
        if (s_known[i].until > now && s_known[i].until - now <= KNOWN_SECONDS &&
            !strncmp(s_known[i].host, host, sizeof(s_known[i].host)) && !memcmp(s_known[i].sha, sha, 32)) return 1;
    return 0;
}

static void known_add(const char *host, const unsigned char *sha)
{
    int i, slot = 0, f;
    if (strlen(host) >= sizeof(s_known[0].host)) return;
    known_load();
    for (i = 0; i < MAX_KNOWN; i++) {
        if (!strcmp(s_known[i].host, host)) { slot = i; break; }
        if (s_known[i].until < s_known[slot].until) slot = i;
    }
    memset(&s_known[slot], 0, sizeof(Known));
    strcpy(s_known[slot].host, host);
    memcpy(s_known[slot].sha, sha, 32);
    s_known[slot].until = now_seconds() + KNOWN_SECONDS;
    for (f = 0; f < 2; f++) {
        FILE *fp = fopen(s_known_files[f], "wb");
        if (fp) {
            int ok = fwrite(s_known, sizeof(Known), MAX_KNOWN, fp) == MAX_KNOWN;
            fclose(fp);
            if (ok) return;
        }
    }
}

/* The server's certificate chain against the trusted store, and its name
 * against the host we asked for. 1 = good. */
static int verify_peer(Tls *t, const char *host, int *checked_chain)
{
    static const char purpose[] = "ssl_server";
    APTR cert = SSL_get1_peer_certificate(t->ssl), chain, vc;
    unsigned char sha[32];
    unsigned int sha_len = 0;
    int ok = 0, numeric = 1, i, have_sha;
    *checked_chain = 0;
    if (!cert) { hal_log("TLS: the server sent no certificate\n"); return 0; }
    SHARE(sha, sizeof(sha)); SHARE(&sha_len, sizeof(sha_len));
    have_sha = X509_digest(cert, EVP_sha256(), sha, &sha_len) == 1 && sha_len == 32;
    if (have_sha && known_has(host, sha)) { X509_free(cert); return 1; }
    *checked_chain = 1;
    chain = SSL_get_peer_cert_chain(t->ssl);
    vc = X509_STORE_CTX_new();
    if (vc && X509_STORE_CTX_init(vc, s_store, cert, chain)) {
        APTR param = X509_STORE_CTX_get0_param(vc);
        for (i = 0; host[i]; i++) if (!((host[i] >= '0' && host[i] <= '9') || host[i] == '.')) numeric = 0;
        SHARE(host, strlen(host) + 1);
        X509_STORE_CTX_set_default(vc, (APTR)purpose);
        if (numeric ? X509_VERIFY_PARAM_set1_ip_asc(param, (APTR)host)
                    : X509_VERIFY_PARAM_set1_host(param, (APTR)host, strlen(host))) {
            ok = X509_verify_cert(vc) == 1;
            if (!ok) {
                const char *why = X509_verify_cert_error_string((long)X509_STORE_CTX_get_error(vc));
                hal_log("TLS: the certificate of %s was not accepted: %s\n", host, why ? why : "?");
            }
        } else {
            hal_log("TLS: cannot check the certificate against the name %s\n", host);
        }
    }
    if (vc) X509_STORE_CTX_free(vc);
    X509_free(cert);
    while (ERR_get_error()) {}
    if (ok && have_sha) known_add(host, sha);
    return ok;
}

void hal_tls_shutdown(void)
{
    int i;
    for (i = 0; i < MAX_TLS; i++) if (s_tls[i].used) hal_tls_close(i);
    for (i = 0; i < MAX_SESSIONS; i++) if (s_sess[i].session) { SSL_SESSION_free(s_sess[i].session); s_sess[i].session = NULL; }
    if (s_store) { X509_STORE_free(s_store); s_store = NULL; }
    if (s_ctx) { SSL_CTX_free(s_ctx); s_ctx = NULL; }
    if (s_ssl) {
        struct TagItem done; done.ti_Tag = TAG_DONE; done.ti_Data = 0;
        SHARE(&done, sizeof(done));
        CleanupAmiSSLA(&done);
        CloseAmiSSL(); s_ssl = NULL;
    }
    if (s_master) { LIB_CLOSE(s_master); s_master = NULL; }
#if defined(AJ_AMIGA_WOS)
    if (s_use_helper && s_hsocket) LIB_CLOSE(s_hsocket);
    s_hsocket = NULL;
    helper_stop();
#endif
}

/* what OpenSSL wants to send goes out to the socket */
static int flush_out(Tls *t)
{
    char buf[1024];
    while (BIO_ctrl(t->wbio, BIO_CTRL_PENDING, 0, NULL) > 0) {
        int n, off = 0;
        SHARE(buf, sizeof(buf));
        n = BIO_read(t->wbio, buf, sizeof(buf));
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
    SHARE(buf, n);
    return BIO_write(t->rbio, buf, n) == n;
}

static int tls_open_once(const char *host, int port, int *slow_check)
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
    t->port = port;
    t->host[0] = 0;
    if (strlen(host) < sizeof(t->host)) strcpy(t->host, host);
    { APTR old = session_for(host, port); if (old) SSL_set_session(t->ssl, old); }
    SHARE(host, strlen(host) + 1);
    SSL_ctrl(t->ssl, SSL_CTRL_SET_TLSEXT_HOSTNAME, 0, (APTR)host);
    {
    /* AJ_TLS_LOG: where the handshake's time goes (in the library, sending, waiting for the peer) */
    char env[8];
    int trace = GetVar((CONST_STRPTR)"AJ_TLS_LOG", (STRPTR)env, sizeof(env), GVF_GLOBAL_ONLY) > 0;
    uint64_t t_start = hal_monotonic_micros(), t_ssl = 0, t_out = 0, t_in = 0, t0;
    int steps = 0;
    for (;;) {
        int r, err, fed;
        t0 = hal_monotonic_micros();
        r = SSL_connect(t->ssl);
        t_ssl += hal_monotonic_micros() - t0; steps++;
        t0 = hal_monotonic_micros();
        fed = flush_out(t);
        t_out += hal_monotonic_micros() - t0;
        if (!fed) { r = -1; err = 0; }
        else if (r == 1) {
            if (trace) hal_log("TLS: %s handshake %lu ms in %d steps: library %lu, sending %lu, receiving %lu\n",
                               SSL_session_reused(t->ssl) ? "resumed" : "full",
                               (unsigned long)((hal_monotonic_micros() - t_start) / 1000), steps,
                               (unsigned long)(t_ssl / 1000), (unsigned long)(t_out / 1000), (unsigned long)(t_in / 1000));
            break;
        }
        else err = SSL_get_error(t->ssl, r);
        if (err == SSL_ERROR_WANT_READ) {
            t0 = hal_monotonic_micros();
            fed = feed_in(t);
            t_in += hal_monotonic_micros() - t0;
            if (fed) continue;
        }
        if (err == SSL_ERROR_WANT_WRITE) continue;
        log_ssl_error("the handshake failed");
        SSL_free(t->ssl);
        hal_net_close(sock);
        return -1;
    }
    }
    /* a resumed session was checked when it was made; a new one is checked now */
    if (s_store && !SSL_session_reused(t->ssl)) {
        uint64_t t0 = hal_monotonic_micros();
        int checked = 0;
        int good = verify_peer(t, host, &checked);
        unsigned long ms = (unsigned long)((hal_monotonic_micros() - t0) / 1000);
        char env[8];
        if (GetVar((CONST_STRPTR)"AJ_TLS_LOG", (STRPTR)env, sizeof(env), GVF_GLOBAL_ONLY) > 0)
            hal_log("TLS: certificate %s, %lu ms\n", checked ? "chain checked" : "known", ms);
        if (good && checked && ms > 3000) *slow_check = 1;
        if (!good) {
            SSL_free(t->ssl);
            hal_net_close(sock);
            return -1;
        }
    }
    t->used = 1;
    return i;
}

int hal_tls_open(const char *host, int port)
{
    int slow = 0;
    int h = tls_open_once(host, port, &slow);
    if (h >= 0 && slow) {
        /* The check kept the server waiting for seconds and it has most likely
         * hung up by now. The certificate is known from here on: start over. */
        Tls *t = &s_tls[h];
        SSL_free(t->ssl);
        hal_net_close(t->sock);
        t->used = 0;
        slow = 0;
        h = tls_open_once(host, port, &slow);
    }
    return h;
}

int hal_tls_read(int handle, void *buf, int len)
{
    Tls *t;
    if ((unsigned)handle >= MAX_TLS || !s_tls[handle].used) return -1;
    t = &s_tls[handle];
    for (;;) {
        int n, err;
        SHARE(buf, len);
        n = SSL_read(t->ssl, buf, len);
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
    SHARE(buf, len);
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
    session_keep(t->host, t->port, SSL_get1_session(t->ssl));
    SSL_free(t->ssl);
    hal_net_close(t->sock);
    t->used = 0;
}
