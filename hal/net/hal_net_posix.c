/* amiga-j2me HAL network: BSD sockets by their POSIX names. Used on the
 * Linux host and on WarpOS (where MOS2WOS's libglosswos provides them over
 * bsdsocket.library). GPLv2. */
#include "hal.h"
#include <sys/types.h>
#include <sys/socket.h>
#include <sys/ioctl.h>
#include <netinet/in.h>
#include <netdb.h>
#include <string.h>
#include <unistd.h>

int hal_net_open(const char *host, int port)
{
    struct hostent *he = gethostbyname((char *)host);
    struct sockaddr_in a;
    int s;
    if (!he || !he->h_addr_list || !he->h_addr_list[0]) return -1;
    s = socket(AF_INET, SOCK_STREAM, 0);
    if (s < 0) return -1;
    memset(&a, 0, sizeof(a));
    a.sin_family = AF_INET;
    a.sin_port = htons((unsigned short)port);
    memcpy(&a.sin_addr, he->h_addr_list[0], sizeof(a.sin_addr));
    if (connect(s, (struct sockaddr *)&a, sizeof(a)) != 0) { close(s); return -1; }
    return s;
}

int hal_net_read(int handle, void *buf, int len)
{
    int n = (int)recv(handle, buf, (size_t)len, 0);
    return n < 0 ? -1 : n;
}

int hal_net_write(int handle, const void *buf, int len)
{
    int n = (int)send(handle, buf, (size_t)len, 0);
    return n < 0 ? -1 : n;
}

int hal_net_available(int handle)
{
    long n = 0;
    if (ioctl(handle, FIONREAD, &n) != 0) return 0;
    return (int)n;
}

void hal_net_close(int handle) { if (handle >= 0) close(handle); }
