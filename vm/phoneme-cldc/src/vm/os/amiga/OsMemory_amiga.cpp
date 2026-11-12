/*
 * amiga-j2me: Amiga OS port of phoneME CLDC-HI (AOS3 m68k, MorphOS, WarpOS).
 * All OS access goes through the amiga-j2me HAL (hal/include/hal.h).
 * Licensed under GPLv2 only, like the rest of phoneME.
 */
#include "incls/_precompiled.incl"
#include "incls/_OsMemory_amiga.cpp.incl"
#include "hal.h"
#include <string.h>

extern "C" {
/* Zeroed: on the systems this VM grew up on, a large malloc comes back as
 * fresh zero pages, and the collector's data relies on it (with AmigaOS'
 * leftover memory contents, stack map generation failed now and then). */
void *OsMemory_allocate(size_t size) {
  void *p = hal_alloc(size);
  if (p != NULL) {
    memset(p, 0, size);
  }
  return p;
}
void OsMemory_free(void *p)          { hal_free(p); }
}
