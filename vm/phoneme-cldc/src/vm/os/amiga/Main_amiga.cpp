/*
 * amiga-j2me: Amiga OS port of phoneME CLDC-HI (AOS3 m68k, MorphOS, WarpOS).
 * All OS access goes through the amiga-j2me HAL (hal/include/hal.h).
 * Licensed under GPLv2 only, like the rest of phoneME.
 */
#include "incls/_precompiled.incl"
#include "incls/_Main_amiga.cpp.incl"
#include "hal.h"
#include <setjmp.h>
#include <stdlib.h>

/* Startup trace: "setenv AJ_TRACE 1" (porting aid, see AJ_TRACE() uses). */
extern "C" void aj_trace(const char *what) {
  static int on = -1;
  if (on < 0) { const char *e = getenv("AJ_TRACE"); on = (e && *e == '1'); }
  if (on) { hal_log("[aj] %s\n", what); }
}

void JVMSPI_PrintRaw(const char* s, int length) {
  hal_write_console(s, length);
}

/* JVMSPI_Exit must not return: jump back into aj_vm_main(), which returns
 * the code to the platform main() (so the HAL can restore the stack and
 * close what it opened; a plain exit() would skip that). */
static jmp_buf aj_exit_jmp;
static int     aj_exit_code;

void JVMSPI_Exit(int code) {
  aj_exit_code = code;
  longjmp(aj_exit_jmp, 1);
}

/* A failed VM assertion (BREAKPOINT): the error text is out already; stop
 * the VM where it stands and return to main() with a failure code. */
extern "C" void aj_vm_fatal(void) {
  static int once;
  if (!once) {
    once = 1;
    hal_log("amiga-j2me: the VM stopped after an internal error\n");
  }
  aj_exit_code = 20;
  longjmp(aj_exit_jmp, 1);
}

jboolean JVMSPI_IsRestrictedPackage(const char* pkg_name, int name_length) {
  (void)pkg_name; (void)name_length;
  return KNI_FALSE;
}

/* Entry point called from the platform main() (src/vm_main.c) after
 * hal_init(). argv[0] is the program name. */
extern "C" int aj_vm_main(int argc, char **argv) {
  int code = 0;
  if (setjmp(aj_exit_jmp)) {
    return aj_exit_code;
  }
  JVM_Initialize();
  argc--; argv++;
  while (true) {
    int n = JVM_ParseOneArg(argc, argv);
    if (n < 0) { JVMSPI_DisplayUsage(NULL); return -1; }
    if (n == 0) break;
    argc -= n; argv += n;
  }
  if (JVM_GetConfig(JVM_CONFIG_SLAVE_MODE) == KNI_FALSE) {
    code = JVM_Start(NULL, NULL, argc, argv);
  } else {
    JVM_Start(NULL, NULL, argc, argv);
    for (;;) {
      jlong timeout = JVM_TimeSlice();
      if (timeout <= -2) break;
      int blocked_threads_count;
      JVMSPI_BlockedThreadInfo *blocked_threads =
          SNI_GetBlockedThreads(&blocked_threads_count);
      JVMSPI_CheckEvents(blocked_threads, blocked_threads_count, timeout);
    }
    code = JVM_CleanUp();
  }
  return code;
}
