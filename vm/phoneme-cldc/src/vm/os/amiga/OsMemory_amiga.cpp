/*
 * amiga-j2me: Amiga OS port of phoneME CLDC-HI (AOS3 m68k, MorphOS, WarpOS).
 * All OS access goes through the amiga-j2me HAL (hal/include/hal.h).
 * Licensed under GPLv2 only, like the rest of phoneME.
 */
#include "incls/_precompiled.incl"
#include "incls/_OsMemory_amiga.cpp.incl"
#include "hal.h"

extern "C" {
void *OsMemory_allocate(size_t size) { return hal_alloc(size); }
void OsMemory_free(void *p)          { hal_free(p); }
}
