/*
 * amiga-j2me: Amiga OS port of phoneME CLDC-HI (AOS3 m68k, MorphOS, WarpOS).
 * All OS access goes through the amiga-j2me HAL (hal/include/hal.h).
 * Licensed under GPLv2 only, like the rest of phoneME.
 */
#include "incls/_precompiled.incl"
#include "incls/_JVM_amiga.cpp.incl"

static int executeVM(void) {
  const int result = JVM::start();
  Arguments::finalize();
  return result;
}

extern "C" int JVM_Start(const char *classpath, char *main_class, int argc,
                         char **argv) {
  JVM::set_arguments(classpath, main_class, argc, argv);
  return executeVM();
}

extern "C" int JVM_Start2(const char *classpath, char *main_class, int argc,
                          jchar **u_argv) {
  JVM::set_arguments2(classpath, main_class, argc, NULL, u_argv, true);
  return executeVM();
}
