/*
 * amiga-j2me: Amiga OS port of phoneME CLDC-HI (AOS3 m68k, MorphOS, WarpOS).
 * All OS access goes through the amiga-j2me HAL (hal/include/hal.h).
 * Licensed under GPLv2 only, like the rest of phoneME.
 */
#include "incls/_precompiled.incl"
#include "incls/_OsMisc_amiga.cpp.incl"

extern "C" {
const JvmPathChar *OsMisc_get_classpath() { return NULL; }

/* The C interpreter never generates code, so there is nothing to flush. */
void OsMisc_flush_icache(address start, int size) { (void)start; (void)size; }

#if !defined(PRODUCT) || ENABLE_TTY_TRACE || USE_DEBUG_PRINTING
const char *OsMisc_jlong_format_specifier()  { return "%lld"; }
const char *OsMisc_julong_format_specifier() { return "%llu"; }
#endif

#if ENABLE_PAGE_PROTECTION
void OsMisc_page_protect()   { UNIMPLEMENTED(); }
void OsMisc_page_unprotect() { UNIMPLEMENTED(); }
#endif
}
