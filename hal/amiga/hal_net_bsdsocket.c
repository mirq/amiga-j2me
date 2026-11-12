/* amiga-j2me HAL network for AmigaOS 3 (68k) and MorphOS: bsdsocket.library
 * (AmiTCP, Miami, Roadshow, ...), opened at the first connection. GPLv2. */
#include "hal.h"
#include <exec/types.h>
#include <exec/libraries.h>
#include <proto/exec.h>
#include <sys/types.h>
#include <sys/socket.h>
#include <netinet/in.h>
#include <netdb.h>
#include <string.h>
#if defined(__MORPHOS__)
#include <proto/socket.h>
#include <sys/filio.h>
#else
#include <proto/bsdsocket.h>
#include <sys/ioctl.h>
#endif

struct Library *SocketBase;

static int net_ready(void)
{
    if (!SocketBase) SocketBase = OpenLibrary((CONST_STRPTR)"bsdsocket.library", 4);
    return SocketBase != NULL;
}

int hal_net_open(const char *host, int port)
{
    struct hostent *he;
    struct sockaddr_in a;
    LONG s;
    if (!net_ready()) return -1;
    he = gethostbyname((STRPTR)host);
    if (!he || !he->h_addr_list || !he->h_addr_list[0]) return -1;
    s = socket(AF_INET, SOCK_STREAM, 0);
    if (s < 0) return -1;
    memset(&a, 0, sizeof(a));
    a.sin_family = AF_INET;
    a.sin_port = (unsigned short)port;      /* big-endian host: network order already */
    memcpy(&a.sin_addr, he->h_addr_list[0], sizeof(a.sin_addr));
    if (connect(s, (struct sockaddr *)&a, sizeof(a)) != 0) { CloseSocket(s); return -1; }
    return (int)s;
}

int hal_net_read(int handle, void *buf, int len)
{
    LONG n = recv(handle, buf, len, 0);
    return n < 0 ? -1 : (int)n;
}

int hal_net_write(int handle, const void *buf, int len)
{
    LONG n = send(handle, (APTR)buf, len, 0);
    return n < 0 ? -1 : (int)n;
}

int hal_net_available(int handle)
{
    LONG n = 0;
    if (IoctlSocket(handle, FIONREAD, (char *)&n) != 0) return 0;
    return (int)n;
}

void hal_net_close(int handle) { if (SocketBase && handle >= 0) CloseSocket(handle); }
