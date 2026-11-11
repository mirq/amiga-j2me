/*
 * amiga-j2me: Amiga OS port of phoneME CLDC-HI (AOS3 m68k, MorphOS, WarpOS).
 * All OS access goes through the amiga-j2me HAL (hal/include/hal.h).
 * Licensed under GPLv2 only, like the rest of phoneME.
 */
#include "incls/_precompiled.incl"
#include "incls/_OsFile_amiga.cpp.incl"

extern "C" {
OsFile_Handle OsFile_open(const char *filename, const char *mode) {
  return fopen(filename, mode);
}
int OsFile_close(OsFile_Handle h)  { return fclose(h); }
int OsFile_flush(OsFile_Handle h)  { return fflush(h); }
size_t OsFile_read(OsFile_Handle h, void *buf, size_t size, size_t count) {
  return fread(buf, size, count, h);
}
size_t OsFile_write(OsFile_Handle h, const void *buf, size_t size, size_t count) {
  return fwrite(buf, size, count, h);
}
long OsFile_length(OsFile_Handle h) {
  long pos = ftell(h), len;
  fseek(h, 0, SEEK_END);
  len = ftell(h);
  fseek(h, pos, SEEK_SET);
  return len;
}
bool OsFile_exists(const char *name) {
  FILE *f = fopen(name, "rb");
  if (f) { fclose(f); return true; }
  return false;
}
long OsFile_seek(OsFile_Handle h, long offset, int origin) {
  return fseek(h, offset, origin);
}
int OsFile_error(OsFile_Handle h) { return ferror(h); }
int OsFile_eof(OsFile_Handle h)   { return feof(h); }
bool OsFile_rename(const char *from, const char *to) {
  return rename(from, to) == 0;
}
int OsFile_remove(const char *filename) { return remove(filename); }
} // extern "C"
