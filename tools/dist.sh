#!/bin/sh
# Collect what a user needs per target: dist/<target>/{j2me, aj_classes.zip, README.txt}
set -e
cd "$(dirname "$0")/.."
rm -rf dist; mkdir -p dist
readme() { cat > "$1/README.txt" <<'TXT'
amiga-j2me - J2ME (MIDP 2.0 / CLDC 1.1) runtime for the Amiga

  j2me [-w WIDTH] [-h HEIGHT] [-heap MB] [-rms DIR] [-v] game.jar

Needs: an RTG card with cybergraphics.library V41+ (CyberGraphX or
Picasso96) and a 16-bit screen mode; AHI for sound (optional).
Keep aj_classes.zip in the same drawer as j2me.

Keys: arrows, Return/Space = fire, F1/F2 = soft keys, 0-9, numpad * and /,
Esc = quit. -w/-h set the screen size the game sees (try 128x128, 176x208
or 240x320, whatever the game was made for).

Source and licence (GPLv2): https://github.com/mirq/amiga-j2me
TXT
}
if [ -f build/aos3-m68k-rel/vm/j2me ]; then
  mkdir -p dist/aos3-68060
  /opt/amiga/bin/m68k-amigaos-strip -o dist/aos3-68060/j2me build/aos3-m68k-rel/vm/j2me
  cp build/api/aj_classes.zip dist/aos3-68060/; readme dist/aos3-68060
fi
if [ -f build/wos-ppc/vm/j2me ]; then
  mkdir -p dist/warpos; cp build/wos-ppc/vm/j2me dist/warpos/j2me
  cp build/api/aj_classes.zip dist/warpos/; readme dist/warpos
fi
if [ -f build/morphos-ppc/vm/j2me ]; then
  mkdir -p dist/morphos; ppc-morphos-strip -o dist/morphos/j2me build/morphos-ppc/vm/j2me 2>/dev/null || cp build/morphos-ppc/vm/j2me dist/morphos/j2me
  cp build/api/aj_classes.zip dist/morphos/; readme dist/morphos
fi
ls -la dist/*/
