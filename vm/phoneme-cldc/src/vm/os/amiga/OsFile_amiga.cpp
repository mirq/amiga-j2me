/*
 * amiga-j2me: Amiga OS port of phoneME CLDC-HI (AOS3 m68k, MorphOS, WarpOS).
 * All OS access goes through the amiga-j2me HAL (hal/include/hal.h).
 * Licensed under GPLv2 only, like the rest of phoneME.
 */
#include "incls/_precompiled.incl"
#include "incls/_OsFile_amiga.cpp.incl"
#include "hal.h"
#include <stdlib.h>
#include <string.h>

/* JAR and zip files opened for reading are read into memory once and kept
 * there until the program ends. Looking up one resource or class makes the
 * VM's JarFileParser seek a couple of hundred times in each archive on the
 * class path; on a real Amiga every seek is a DOS Seek() on the disk, and
 * Skoki 2's 15 small images took 4 to 9 seconds to load on a 68060 (the game
 * measures real time across that pause and its jumper skipped half the
 * inrun). From memory, a seek costs nothing. */
struct AjFile {
  FILE *f;                       /* a stdio stream, or NULL: */
  const unsigned char *mem;      /* the whole file */
  long len, pos;
  int eof;
};

struct AjCached {
  struct AjCached *next;
  char *name;
  unsigned char *data;
  long len;
};
static struct AjCached *aj_cache;

static int aj_is_archive(const char *name, const char *mode) {
  size_t n = strlen(name);
  if (mode[0] != 'r' || strchr(mode, '+') || n < 4) return 0;
  const char *e = name + n - 4;
  return !strcmp(e, ".jar") || !strcmp(e, ".zip") || !strcmp(e, ".JAR") || !strcmp(e, ".ZIP");
}

static struct AjCached *aj_load(const char *name) {
  struct AjCached *c;
  for (c = aj_cache; c; c = c->next) if (!strcmp(c->name, name)) return c;
  FILE *f = fopen(name, "rb");
  if (!f) return NULL;
  long len = -1;
  if (fseek(f, 0, SEEK_END) == 0) len = ftell(f);
  fseek(f, 0, SEEK_SET);
  if (len < 0 || len > 32L * 1024 * 1024) { fclose(f); return NULL; }
  c = (struct AjCached *)malloc(sizeof(*c));
  unsigned char *data = (unsigned char *)malloc(len > 0 ? len : 1);
  char *copy = (char *)malloc(strlen(name) + 1);
  if (!c || !data || !copy || (len > 0 && fread(data, 1, len, f) != (size_t)len)) {
    free(c); free(data); free(copy); fclose(f); return NULL;
  }
  fclose(f);
  strcpy(copy, name);
  c->name = copy; c->data = data; c->len = len;
  c->next = aj_cache; aj_cache = c;
  return c;
}

extern "C" {
OsFile_Handle OsFile_open(const char *filename, const char *mode) {
  /* never fopen() a directory: that hangs in the WarpOS C library */
  if (hal_path_type(filename) == 2) return NULL;
  struct AjFile *h = (struct AjFile *)calloc(1, sizeof(struct AjFile));
  if (!h) return NULL;
  struct AjCached *c = aj_is_archive(filename, mode) ? aj_load(filename) : NULL;
  if (c) {
    h->mem = c->data; h->len = c->len;
    return h;
  }
  h->f = fopen(filename, mode);
  if (!h->f) { free(h); return NULL; }
  return h;
}
int OsFile_close(OsFile_Handle h) {
  int r = h->f ? fclose(h->f) : 0;
  free(h);
  return r;
}
int OsFile_flush(OsFile_Handle h)  { return h->f ? fflush(h->f) : 0; }
size_t OsFile_read(OsFile_Handle h, void *buf, size_t size, size_t count) {
  if (h->f) return fread(buf, size, count, h->f);
  if (size == 0 || count == 0) return 0;
  long left = h->len - h->pos;
  if (left < 0) left = 0;
  size_t items = (size_t)left / size;
  if (items > count) items = count;
  else h->eof = 1;
  memcpy(buf, h->mem + h->pos, items * size);
  h->pos += (long)(items * size);
  return items;
}
size_t OsFile_write(OsFile_Handle h, const void *buf, size_t size, size_t count) {
  return h->f ? fwrite(buf, size, count, h->f) : 0;
}
long OsFile_length(OsFile_Handle h) {
  if (!h->f) return h->len;
  long pos = ftell(h->f), len;
  fseek(h->f, 0, SEEK_END);
  len = ftell(h->f);
  fseek(h->f, pos, SEEK_SET);
  return len;
}
bool OsFile_exists(const char *name) {
  return hal_path_type(name) != 0;
}
long OsFile_seek(OsFile_Handle h, long offset, int origin) {
  if (h->f) return fseek(h->f, offset, origin);
  long to = origin == SEEK_SET ? offset : origin == SEEK_CUR ? h->pos + offset : h->len + offset;
  if (to < 0) return -1;
  h->pos = to;
  h->eof = 0;
  return 0;
}
int OsFile_error(OsFile_Handle h) { return h->f ? ferror(h->f) : 0; }
int OsFile_eof(OsFile_Handle h)   { return h->f ? feof(h->f) : h->eof; }
bool OsFile_rename(const char *from, const char *to) {
  return rename(from, to) == 0;
}
int OsFile_remove(const char *filename) { return remove(filename); }
} // extern "C"
