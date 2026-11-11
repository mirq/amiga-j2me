/*
 * amiga-j2me: Amiga OS port of phoneME CLDC-HI (AOS3 m68k, MorphOS, WarpOS).
 * All OS access goes through the amiga-j2me HAL (hal/include/hal.h).
 * Licensed under GPLv2 only, like the rest of phoneME.
 */
#include "incls/_precompiled.incl"
#include "incls/_OS_amiga.cpp.incl"
#include "hal.h"

/*
 * Timer ticks drive Java thread preemption. Instead of a timer task or
 * interrupt (fragile on WarpOS, where 68k timer calls crash from PPC tasks),
 * the C interpreter's check_timer_tick() counts down aj_tick_poll_counter
 * and calls aj_tick_poll() every AJ_TICK_POLL_EVERY checks; we then read the
 * HAL monotonic clock and raise real_time_tick() once per TickInterval.
 */
#define AJ_TICK_POLL_EVERY 2048

extern "C" {
int aj_tick_poll_counter = AJ_TICK_POLL_EVERY;
static bool     ticks_enabled = false;
static uint64_t next_tick_us  = 0;

void aj_tick_poll(void) {
  aj_tick_poll_counter = AJ_TICK_POLL_EVERY;
  if (!ticks_enabled) return;
  uint64_t now = hal_monotonic_micros();
  if (now >= next_tick_us) {
    next_tick_us = now + (uint64_t)TickInterval * 1000u;
    real_time_tick(TickInterval);
  }
}
}

#if ENABLE_DYNAMIC_NATIVE_METHODS
void* Os::loadLibrary(const char* libName) { return 0; }
void* Os::getSymbol(void* handle, const char* name) { return 0; }
#endif

#if SUPPORTS_MONOTONIC_CLOCK
jlong Os::monotonic_time_millis() {
  return (jlong)(hal_monotonic_micros() / 1000u);
}
#endif

jlong Os::java_time_millis() { return (jlong)hal_time_millis(); }

void Os::sleep(jlong ms) {
  if (ms > 0) hal_sleep_ms((uint32_t)(ms > 0x7fffffff ? 0x7fffffff : ms));
}

bool Os::start_ticks() {
  next_tick_us = hal_monotonic_micros() + (uint64_t)TickInterval * 1000u;
  ticks_enabled = true;
  return true;
}
void Os::stop_ticks()    { ticks_enabled = false; }
void Os::suspend_ticks() { ticks_enabled = false; }
void Os::resume_ticks()  { start_ticks(); }

void Os::start_compiler_timer() {}
bool Os::check_compiler_timer() { return false; }

void Os::initialize() {}
void Os::dispose() { stop_ticks(); }

#ifndef PRODUCT
jlong Os::elapsed_counter()   { return (jlong)hal_monotonic_micros(); }
jlong Os::elapsed_frequency() { return 1000000; }
#endif
