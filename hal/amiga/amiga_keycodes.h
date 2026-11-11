/*
 * The SDL key codes the game's key handling uses, for the Amiga builds.
 *
 * The SDL build hands SDL2 key codes to ui_HandleKeypress (SDLK_UP + 256 for an
 * arrow, SDLK_DELETE, ...) and aui_textfield.cpp compares against them. The
 * Amiga event pump (ui/aui_amiga/aui_amigaui.cpp) produces the same numbers,
 * so that code is shared unchanged. The values are SDL2's (SDL_keycode.h).
 */
#ifndef AJ_AMIGA_KEYCODES_H
#define AJ_AMIGA_KEYCODES_H 1

#define AJ_SDLK_SCANCODE_MASK (1 << 30)
#define AJ_SDLK_FROM_SCANCODE(x) ((x) | AJ_SDLK_SCANCODE_MASK)

#define SDLK_RETURN    '\r'
#define SDLK_ESCAPE    '\033'
#define SDLK_BACKSPACE '\b'
#define SDLK_TAB       '\t'
#define SDLK_SPACE     ' '
#define SDLK_DELETE    '\177'

#define SDLK_RIGHT     AJ_SDLK_FROM_SCANCODE(79)
#define SDLK_LEFT      AJ_SDLK_FROM_SCANCODE(80)
#define SDLK_DOWN      AJ_SDLK_FROM_SCANCODE(81)
#define SDLK_UP        AJ_SDLK_FROM_SCANCODE(82)
#define SDLK_SELECT    AJ_SDLK_FROM_SCANCODE(119)
#define SDLK_CUT       AJ_SDLK_FROM_SCANCODE(123)
#define SDLK_COPY      AJ_SDLK_FROM_SCANCODE(124)
#define SDLK_PASTE     AJ_SDLK_FROM_SCANCODE(125)

#endif
