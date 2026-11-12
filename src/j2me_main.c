/* amiga-j2me: the launcher.
 *
 *   j2me [-w WIDTH] [-h HEIGHT] [-scale N] [-heap MB] [-rms DIR] [-v] game.jar|game.jad [MIDletClass]
 *
 * Starts the VM with the runtime's class library (aj_classes.zip, next to
 * this program) and the JAR on the class path and runs aj.Launcher, which
 * starts the MIDlet named in the JAR's manifest. Record stores go to
 * <game.jar>.rms unless -rms names another directory.
 *
 * Per-game settings: a text file <game.jar>.cfg with lines "name=value".
 * w, h, scale and heap are the options above (the command line wins); any
 * other name is a system property the game can read, such as
 * microedition.platform=Nokia6230 or microedition.locale=pl. */
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

#define MAX_PROPS 24
static char s_props[MAX_PROPS][160];
static int s_nprops;
static char s_cfg_w[16], s_cfg_h[16], s_cfg_scale[16], s_cfg_heap[16];

/* -Dname=value for the VM; a later one with the same name replaces an earlier one */
static void set_prop(const char *name, size_t nlen, const char *value, size_t vlen)
{
    int i;
    if (nlen + vlen + 4 > sizeof(s_props[0])) return;
    for (i = 0; i < s_nprops; i++)
        if (!strncmp(s_props[i] + 2, name, nlen) && s_props[i][2 + nlen] == '=') break;
    if (i == s_nprops) { if (s_nprops == MAX_PROPS) return; s_nprops++; }
    sprintf(s_props[i], "-D%.*s=%.*s", (int)nlen, name, (int)vlen, value);
}

static void copy_opt(char *dst, size_t cap, const char *v, size_t n)
{
    if (n >= cap) n = cap - 1;
    memcpy(dst, v, n); dst[n] = 0;
}

static void read_cfg(const char *path)
{
    size_t size = 0, i = 0;
    char *d;
    if (hal_path_type(path) != 1) return;
    d = (char *)hal_load_file(path, &size);
    if (!d) return;
    while (i < size) {
        size_t s = i, e, eq, ve;
        while (i < size && d[i] != '\n' && d[i] != '\r') i++;
        e = i;
        while (i < size && (d[i] == '\n' || d[i] == '\r')) i++;
        while (s < e && (d[s] == ' ' || d[s] == '\t')) s++;
        if (s == e || d[s] == '#' || d[s] == ';') continue;
        for (eq = s; eq < e && d[eq] != '='; eq++) {}
        if (eq == e) continue;
        ve = e;
        while (ve > eq + 1 && (d[ve - 1] == ' ' || d[ve - 1] == '\t')) ve--;
        {
            const char *name = d + s, *val = d + eq + 1;
            size_t nlen = eq - s, vlen = ve - eq - 1;
            while (nlen > 0 && (name[nlen - 1] == ' ' || name[nlen - 1] == '\t')) nlen--;
            while (vlen > 0 && (*val == ' ' || *val == '\t')) { val++; vlen--; }
            if (nlen == 1 && name[0] == 'w') copy_opt(s_cfg_w, sizeof(s_cfg_w), val, vlen);
            else if (nlen == 1 && name[0] == 'h') copy_opt(s_cfg_h, sizeof(s_cfg_h), val, vlen);
            else if (nlen == 5 && !strncmp(name, "scale", 5)) copy_opt(s_cfg_scale, sizeof(s_cfg_scale), val, vlen);
            else if (nlen == 4 && !strncmp(name, "heap", 4)) copy_opt(s_cfg_heap, sizeof(s_cfg_heap), val, vlen);
            else if (nlen > 0) set_prop(name, nlen, val, vlen);
        }
    }
    hal_free(d);
}

static void usage(void)
{
    hal_log("amiga-j2me - J2ME (MIDP 2.0 / CLDC 1.1) runtime\n"
            "usage: j2me [-w WIDTH] [-h HEIGHT] [-scale N] [-heap MB] [-rms DIR] [-v] game.jar|game.jad [MIDletClass]\n"
            "  -w, -h   screen size the game sees (default 240 x 320)\n"
            "  -scale   show each pixel N x N (1..4), for cards without small screen modes\n"
            "  -heap    Java heap in MB (default 8)\n"
            "  -rms     directory for the game's saved data (default: <game.jar>.rms)\n"
            "  -v       print start-up progress\n"
            "settings per game: <game.jar>.cfg with lines w=, h=, scale=, heap= or property=value\n"
            "keys: arrows, Return/Space = fire, F1/F2 = soft keys, 0-9, numpad * and /, Esc = quit\n");
}

int main(int argc, char **argv)
{
    static char heap_opt[32], classpath[640], rms[320], jar_from_jad[320], cfg[330];
    static char *vm_argv[64];
    const char *jar = NULL, *cls = NULL, *w = NULL, *h = NULL, *rms_dir = NULL, *heap = NULL;
    const char *scale = NULL, *jad = NULL;
    const char *classes = getenv("AJ_CLASSES");
    int verbose = 0, i, n = 0, rc;

    if (hal_init(argc, argv) != 0) return 20;
    for (i = 1; i < argc; i++) {
        if (!strcmp(argv[i], "-w") && i + 1 < argc) w = argv[++i];
        else if (!strcmp(argv[i], "-h") && i + 1 < argc) h = argv[++i];
        else if (!strcmp(argv[i], "-heap") && i + 1 < argc) heap = argv[++i];
        else if (!strcmp(argv[i], "-scale") && i + 1 < argc) scale = argv[++i];
        else if (!strcmp(argv[i], "-rms") && i + 1 < argc) rms_dir = argv[++i];
        else if (!strcmp(argv[i], "-v")) verbose = 1;
        else if (argv[i][0] == '-' || argv[i][0] == '?') { usage(); hal_shutdown(); return 5; }
        else if (!jar) jar = argv[i];
        else cls = argv[i];
    }
    if (!jar) { usage(); hal_shutdown(); return 5; }
    if (hal_path_type(jar) == 0) { hal_log("j2me: cannot find %s\n", jar); hal_shutdown(); return 10; }
    {
        /* a .jad descriptor: the JAR is its MIDlet-Jar-URL, looked for next to it */
        size_t n = strlen(jar);
        if (n > 4 && (!strcmp(jar + n - 4, ".jad") || !strcmp(jar + n - 4, ".JAD"))) {
            size_t size = 0, dir, k;
            char *d = (char *)hal_load_file(jar, &size), *u = NULL;
            const char *key = "MIDlet-Jar-URL:";
            jad = jar;
            for (k = 0; d && k + 15 < size; k++)
                if ((k == 0 || d[k - 1] == '\n') && !strncmp(d + k, key, 15)) { u = d + k + 15; break; }
            if (!u) { hal_log("j2me: no MIDlet-Jar-URL in %s\n", jar); hal_shutdown(); return 10; }
            while (u < d + size && (*u == ' ' || *u == '\t')) u++;
            for (k = 0; u + k < d + size && u[k] != '\r' && u[k] != '\n'; k++) {}
            while (k > 0 && (u[k - 1] == ' ')) k--;
            /* keep only the file name of the URL */
            { size_t s2 = k; while (s2 > 0 && u[s2 - 1] != '/' && u[s2 - 1] != ':') s2--; u += s2; k -= s2; }
            for (dir = n; dir > 0 && jar[dir - 1] != '/' && jar[dir - 1] != ':'; dir--) {}
            if (dir + k + 1 > sizeof(jar_from_jad)) { hal_log("j2me: path too long\n"); hal_shutdown(); return 10; }
            memcpy(jar_from_jad, jar, dir); memcpy(jar_from_jad + dir, u, k); jar_from_jad[dir + k] = 0;
            hal_free(d);
            jar = jar_from_jad;
            if (hal_path_type(jar) != 1) { hal_log("j2me: cannot find %s (named in the .jad)\n", jar); hal_shutdown(); return 10; }
        }
    }
    if (!classes || !*classes) classes = CLASSES_DEFAULT;
    if (hal_path_type(classes) != 1) {
        hal_log("j2me: the class library %s is missing (it belongs next to this program)\n", classes);
        hal_shutdown();
        return 10;
    }
    if (strlen(classes) + strlen(jar) + 2 > sizeof(classpath) || strlen(jar) + 5 > sizeof(rms)) {
        hal_log("j2me: path too long\n"); hal_shutdown(); return 10;
    }
    /* what a MIDP 2.0 phone says about itself; the game's .cfg may change any of it */
#define PROP(n, v) set_prop(n, strlen(n), v, strlen(v))
    PROP("microedition.profiles", "MIDP-2.0");
    PROP("microedition.platform", "amiga-j2me");
    PROP("microedition.locale", "en");
    PROP("microedition.m3g.version", "1.0");
    PROP("microedition.media.version", "1.1");
    PROP("supports.mixing", "true");
    PROP("supports.audio.capture", "false");
    PROP("supports.video.capture", "false");
    PROP("supports.recording", "false");
#undef PROP
    sprintf(cfg, "%s.cfg", jar);
    read_cfg(cfg);
    if (!w && s_cfg_w[0]) w = s_cfg_w;
    if (!h && s_cfg_h[0]) h = s_cfg_h;
    if (!scale && s_cfg_scale[0]) scale = s_cfg_scale;
    if (!heap) heap = s_cfg_heap[0] ? s_cfg_heap : "8";
    sprintf(heap_opt, "=HeapCapacity%dM", atoi(heap) > 0 ? atoi(heap) : 8);
    sprintf(classpath, "%s,%s", classes, jar);
    if (!rms_dir) { sprintf(rms, "%s.rms", jar); rms_dir = rms; }

    vm_argv[n++] = argv[0];
    vm_argv[n++] = heap_opt;
    for (i = 0; i < s_nprops; i++) vm_argv[n++] = s_props[i];
    vm_argv[n++] = (char *)"-classpath";
    vm_argv[n++] = classpath;
    vm_argv[n++] = (char *)"aj.Launcher";
    if (verbose) vm_argv[n++] = (char *)"-v";
    if (w) { vm_argv[n++] = (char *)"-w"; vm_argv[n++] = (char *)w; }
    if (h) { vm_argv[n++] = (char *)"-h"; vm_argv[n++] = (char *)h; }
    if (scale) { vm_argv[n++] = (char *)"-scale"; vm_argv[n++] = (char *)scale; }
    if (jad) { vm_argv[n++] = (char *)"-jad"; vm_argv[n++] = (char *)jad; }
    vm_argv[n++] = (char *)"-rms"; vm_argv[n++] = (char *)rms_dir;
    if (cls) vm_argv[n++] = (char *)cls;
    vm_argv[n] = NULL;

    s_argc = n; s_argv = vm_argv;
    rc = hal_run_with_stack(run_vm, 512 * 1024);
    hal_shutdown();
    return rc < 0 ? 20 : rc;
}
