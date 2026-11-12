/* amiga-j2me: the launcher.
 *
 *   j2me [-w WIDTH] [-h HEIGHT] [-heap MB] [-rms DIR] [-v] game.jar [MIDletClass]
 *
 * Starts the VM with the runtime's class library (aj_classes.zip, next to
 * this program) and the JAR on the class path and runs aj.Launcher, which
 * starts the MIDlet named in the JAR's manifest. Record stores go to
 * <game.jar>.rms unless -rms names another directory. */
#include "hal.h"
#include <stdio.h>
#include <stdlib.h>
#include <string.h>

int aj_vm_main(int argc, char **argv);

#if defined(__amigaos__) || defined(__MORPHOS__) || defined(AMIGA) || defined(WOS)
#define CLASSES_DEFAULT "PROGDIR:aj_classes.zip"
#else
#define CLASSES_DEFAULT "build/api/aj_classes.zip"
#endif

static int s_argc;
static char **s_argv;
static int run_vm(void) { return aj_vm_main(s_argc, s_argv); }

static void usage(void)
{
    hal_log("amiga-j2me - J2ME (MIDP 2.0 / CLDC 1.1) runtime\n"
            "usage: j2me [-w WIDTH] [-h HEIGHT] [-heap MB] [-rms DIR] [-v] game.jar [MIDletClass]\n"
            "  -w, -h   screen size the game sees (default 240 x 320)\n"
            "  -heap    Java heap in MB (default 8)\n"
            "  -rms     directory for the game's saved data (default: <game.jar>.rms)\n"
            "  -v       print start-up progress\n"
            "keys: arrows, Return/Space = fire, F1/F2 = soft keys, 0-9, numpad * and /, Esc = quit\n");
}

int main(int argc, char **argv)
{
    static char heap_opt[32], classpath[640], rms[320];
    static char *vm_argv[24];
    const char *jar = NULL, *cls = NULL, *w = NULL, *h = NULL, *rms_dir = NULL, *heap = "8";
    const char *classes = getenv("AJ_CLASSES");
    int verbose = 0, i, n = 0, rc;

    if (hal_init(argc, argv) != 0) return 20;
    for (i = 1; i < argc; i++) {
        if (!strcmp(argv[i], "-w") && i + 1 < argc) w = argv[++i];
        else if (!strcmp(argv[i], "-h") && i + 1 < argc) h = argv[++i];
        else if (!strcmp(argv[i], "-heap") && i + 1 < argc) heap = argv[++i];
        else if (!strcmp(argv[i], "-rms") && i + 1 < argc) rms_dir = argv[++i];
        else if (!strcmp(argv[i], "-v")) verbose = 1;
        else if (argv[i][0] == '-' || argv[i][0] == '?') { usage(); hal_shutdown(); return 5; }
        else if (!jar) jar = argv[i];
        else cls = argv[i];
    }
    if (!jar) { usage(); hal_shutdown(); return 5; }
    if (hal_path_type(jar) == 0) { hal_log("j2me: cannot find %s\n", jar); hal_shutdown(); return 10; }
    if (!classes || !*classes) classes = CLASSES_DEFAULT;
    if (hal_path_type(classes) != 1) {
        hal_log("j2me: the class library %s is missing (it belongs next to this program)\n", classes);
        hal_shutdown();
        return 10;
    }
    if (strlen(classes) + strlen(jar) + 2 > sizeof(classpath) || strlen(jar) + 5 > sizeof(rms)) {
        hal_log("j2me: path too long\n"); hal_shutdown(); return 10;
    }
    sprintf(heap_opt, "=HeapCapacity%dM", atoi(heap) > 0 ? atoi(heap) : 8);
    sprintf(classpath, "%s,%s", classes, jar);
    if (!rms_dir) { sprintf(rms, "%s.rms", jar); rms_dir = rms; }

    vm_argv[n++] = argv[0];
    vm_argv[n++] = heap_opt;
    vm_argv[n++] = (char *)"-classpath";
    vm_argv[n++] = classpath;
    vm_argv[n++] = (char *)"aj.Launcher";
    if (verbose) vm_argv[n++] = (char *)"-v";
    if (w) { vm_argv[n++] = (char *)"-w"; vm_argv[n++] = (char *)w; }
    if (h) { vm_argv[n++] = (char *)"-h"; vm_argv[n++] = (char *)h; }
    vm_argv[n++] = (char *)"-rms"; vm_argv[n++] = (char *)rms_dir;
    if (cls) vm_argv[n++] = (char *)cls;
    vm_argv[n] = NULL;

    s_argc = n; s_argv = vm_argv;
    rc = hal_run_with_stack(run_vm, 512 * 1024);
    hal_shutdown();
    return rc < 0 ? 20 : rc;
}
