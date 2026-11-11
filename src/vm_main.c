/* amiga-j2me: console launcher for the CLDC-HI VM.
 * usage: aj_cldc [VM options] -classpath "<cldc_classes.zip>;<dir|jar>" MainClass
 * (quote the classpath in an AmigaDOS shell: ';' starts a comment there) */
#include "hal.h"

int aj_vm_main(int argc, char **argv);

#if defined(__MORPHOS__)
unsigned long __stack = 512 * 1024;   /* honoured by the MorphOS startup code */
#endif

static int s_argc;
static char **s_argv;
static int run_vm(void) { return aj_vm_main(s_argc, s_argv); }

int main(int argc, char **argv)
{
    int rc;
    if (hal_init(argc, argv) != 0) return 20;
    s_argc = argc; s_argv = argv;
    rc = hal_run_with_stack(run_vm, 512 * 1024);
    hal_shutdown();
    return rc < 0 ? 20 : rc;
}
