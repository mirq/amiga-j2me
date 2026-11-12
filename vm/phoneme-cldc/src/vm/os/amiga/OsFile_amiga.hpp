/*
 * amiga-j2me: Amiga OS port of phoneME CLDC-HI (AOS3 m68k, MorphOS, WarpOS).
 * All OS access goes through the amiga-j2me HAL (hal/include/hal.h).
 * Licensed under GPLv2 only, like the rest of phoneME.
 */
#include <stdio.h>
#ifdef __cplusplus
extern "C" {
#endif
/* A file: either a stdio stream, or (JAR and zip files opened for reading)
 * a view of the whole file kept in memory, see OsFile_amiga.cpp */
struct AjFile;
typedef struct AjFile *OsFile_Handle;
const char OsFile_separator_char      = '/';
/* ':' is the Amiga volume separator and ';' starts a comment in AmigaDOS
 * scripts, so classpath entries are separated by ',' */
const char OsFile_path_separator_char = ',';
#ifdef __cplusplus
}
#endif
