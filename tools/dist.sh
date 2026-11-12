#!/bin/sh
# Collect what a user needs per target: dist/<target>/{j2me, aj_classes.zip, README.txt}
set -e
cd "$(dirname "$0")/.."
rm -rf dist; mkdir -p dist
readme() { cat > "$1/README.txt" <<'TXT'
amiga-j2me - J2ME (MIDP 2.0 / CLDC 1.1) runtime for the Amiga

  j2me [-w WIDTH] [-h HEIGHT] [-scale N] [-heap MB] [-rms DIR] [-v] game.jar|game.jad

Needs: an RTG card with cybergraphics.library V41+ (CyberGraphX or
Picasso96) and a 16-bit screen mode; AHI for sound (optional); a TCP/IP
stack for games that go online, and AmiSSL 5 for https (optional).
Keep aj_classes.zip in the same drawer as j2me.

Keys: arrows, Return = fire, Space = 0, Ctrl = 5, F1/F2 = soft keys, 0-9,
numpad * and /, Esc = quit. Two arrows together send the diagonal key
(up+right = 3, up+left = 1, down+left = 7, down+right = 9). -w/-h set the screen size the game sees (try 128x128, 176x208
or 240x320, whatever the game was made for); -scale 2 or 3 enlarges it.

Settings per game: a text file game.jar.cfg next to the game, with lines
w=176, h=208, scale=2, heap=16, or name=value for a system property the
game asks for (microedition.platform=Nokia6230, microedition.locale=pl).
Keys per game: aj.keys=space:fire,ctrl:none (amiga key:phone key, see the
README on GitHub) and aj.diagonals=off (or four keys, default 1379).

Source and licence (GPLv2): https://github.com/mirq/amiga-j2me
TXT
}
if [ -f build/aos3-m68k-rel/vm/j2me ]; then
  mkdir -p dist/aos3-68060
  tools/check-m68k-bittest.sh build/aos3-m68k-rel/vm/j2me   # a known miscompile of bebbo's gcc: stop if it is back
  # stripped, then code, data and BSS in one hunk: keeps the data out of "ppc memory"
  # on machines with WarpOS (see tools/hunkmerge.py)
  /opt/amiga/bin/m68k-amigaos-strip -o dist/aos3-68060/j2me.strip build/aos3-m68k-rel/vm/j2me
  python3 tools/hunkmerge.py dist/aos3-68060/j2me.strip dist/aos3-68060/j2me
  rm -f dist/aos3-68060/j2me.strip
  tools/check-68k-classifier.py dist/aos3-68060/j2me   # or a Sonnet PPC card treats it as a PowerPC program
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
