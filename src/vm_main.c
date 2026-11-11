/* amiga-j2me: console launcher for the CLDC-HI VM.
 * usage: aj_cldc [VM options] -classpath <cldc_classes.zip>;<dir|jar> MainClass */
#include "hal.h"

int aj_vm_main(int argc, char **argv);

int main(int argc, char **argv)
{
    int rc;
    if (hal_init(argc, argv) != 0) return 20;
    rc = aj_vm_main(argc, argv);
    hal_shutdown();
    return rc < 0 ? 20 : rc;
}
