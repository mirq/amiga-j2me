/*
 * amiga-j2me: Amiga OS port of phoneME CLDC-HI (AOS3 m68k, MorphOS, WarpOS).
 * TCP sockets for com.sun.cldc.io.j2me.socket.Protocol over the HAL
 * (bsdsocket.library on the Amiga), and TLS ones (AmiSSL) when open0 is
 * given mode bit 0x100; their handles carry TLS_FLAG. The calls block: while one waits for the
 * network, no other Java thread runs.
 * Licensed under GPLv2 only, like the rest of phoneME.
 */
#include "incls/_precompiled.incl"
#include "incls/_OsSocket_amiga.cpp.incl"
#include "hal.h"

void JVMSPI_CheckEvents(JVMSPI_BlockedThreadInfo * blocked_threads,
                        int blocked_threads_count, jlong timeout_ms) {
  // not used: the VM runs in master mode and sockets block
}

extern "C" {

#define TLS_FLAG 0x40000000
#define TLS_MODE 0x100
static int is_tls(int h) { return h >= 0 && (h & TLS_FLAG) != 0; }
static int sock_read(int h, void *b, int n) { return is_tls(h) ? hal_tls_read(h & ~TLS_FLAG, b, n) : hal_net_read(h, b, n); }
static int sock_write(int h, const void *b, int n) { return is_tls(h) ? hal_tls_write(h & ~TLS_FLAG, b, n) : hal_net_write(h, b, n); }

// static native int open0(byte hostname[], int port, int mode);
KNIEXPORT KNI_RETURNTYPE_INT
Java_com_sun_cldc_io_j2me_socket_Protocol_open0() {
  char host[256];
  int port = KNI_GetParameterAsInt(2), mode = KNI_GetParameterAsInt(3);
  int n;
  KNI_StartHandles(1);
  KNI_DeclareHandle(hname);
  KNI_GetParameterAsObject(1, hname);
  n = KNI_GetArrayLength((jarray)hname);
  if (n > (int)sizeof(host) - 1) n = sizeof(host) - 1;
  KNI_GetRawArrayRegion((jarray)hname, 0, n, (jbyte*)host);
  host[n] = 0;
  KNI_EndHandles();
  if (mode & TLS_MODE) {
    n = hal_tls_open(host, port);
    KNI_ReturnInt(n < 0 ? -1 : (n | TLS_FLAG));
  }
  KNI_ReturnInt(hal_net_open(host, port));
}

// static native int readByte(int handle);
KNIEXPORT KNI_RETURNTYPE_INT
Java_com_sun_cldc_io_j2me_socket_Protocol_readByte() {
  unsigned char c;
  int n = sock_read(KNI_GetParameterAsInt(1), &c, 1);
  KNI_ReturnInt(n == 1 ? (int)c : -1);
}

// static native int readBuf(int handle, byte b[], int off, int len);
KNIEXPORT KNI_RETURNTYPE_INT
Java_com_sun_cldc_io_j2me_socket_Protocol_readBuf() {
  char buf[1024];
  int handle = KNI_GetParameterAsInt(1), off = KNI_GetParameterAsInt(3);
  int len = KNI_GetParameterAsInt(4), n;
  if (len > (int)sizeof(buf)) len = sizeof(buf);
  n = sock_read(handle, buf, len);
  if (n <= 0) {
    KNI_ReturnInt(-1);          // closed by the peer, or an error
  }
  KNI_StartHandles(1);
  KNI_DeclareHandle(harr);
  KNI_GetParameterAsObject(2, harr);
  KNI_SetRawArrayRegion((jarray)harr, off, n, (jbyte*)buf);
  KNI_EndHandles();
  KNI_ReturnInt(n);
}

// static native int writeByte(int handle, int b);
KNIEXPORT KNI_RETURNTYPE_INT
Java_com_sun_cldc_io_j2me_socket_Protocol_writeByte() {
  unsigned char c = (unsigned char)KNI_GetParameterAsInt(2);
  KNI_ReturnInt(sock_write(KNI_GetParameterAsInt(1), &c, 1) == 1 ? 1 : -1);
}

// static native int writeBuf(int handle, byte b[], int off, int len);
KNIEXPORT KNI_RETURNTYPE_INT
Java_com_sun_cldc_io_j2me_socket_Protocol_writeBuf() {
  char buf[1024];
  int handle = KNI_GetParameterAsInt(1), off = KNI_GetParameterAsInt(3);
  int len = KNI_GetParameterAsInt(4), n;
  if (len > (int)sizeof(buf)) len = sizeof(buf);
  KNI_StartHandles(1);
  KNI_DeclareHandle(harr);
  KNI_GetParameterAsObject(2, harr);
  KNI_GetRawArrayRegion((jarray)harr, off, len, (jbyte*)buf);
  KNI_EndHandles();
  n = sock_write(handle, buf, len);
  KNI_ReturnInt(n > 0 ? n : -1);
}

// static native int available0(int handle);
KNIEXPORT KNI_RETURNTYPE_INT
Java_com_sun_cldc_io_j2me_socket_Protocol_available0() {
  int h = KNI_GetParameterAsInt(1);
  KNI_ReturnInt(is_tls(h) ? hal_tls_available(h & ~TLS_FLAG) : hal_net_available(h));
}

// static native void close0(int handle);
KNIEXPORT KNI_RETURNTYPE_VOID
Java_com_sun_cldc_io_j2me_socket_Protocol_close0() {
  int h = KNI_GetParameterAsInt(1);
  if (is_tls(h)) hal_tls_close(h & ~TLS_FLAG); else hal_net_close(h);
  KNI_ReturnVoid();
}

} // extern "C"
