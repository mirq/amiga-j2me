/* amiga-j2me HAL: no TLS on this platform (the Linux test build). */
#include "hal.h"

int  hal_tls_open(const char *host, int port) { (void)host; (void)port; hal_log("TLS: not available in this build\n"); return -1; }
int  hal_tls_read(int handle, void *buf, int len) { (void)handle; (void)buf; (void)len; return -1; }
int  hal_tls_write(int handle, const void *buf, int len) { (void)handle; (void)buf; (void)len; return -1; }
int  hal_tls_available(int handle) { (void)handle; return 0; }
void hal_tls_close(int handle) { (void)handle; }
void hal_tls_shutdown(void) {}
