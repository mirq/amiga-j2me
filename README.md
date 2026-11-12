# amiga-j2me

A J2ME runtime (MIDP 2.0 / CLDC 1.1) for the Amiga with RTG graphics: it runs
mobile-phone Java games (`.jar`) on a 68060 or a PowerPC Amiga.

```
j2me [-w WIDTH] [-h HEIGHT] [-scale N] [-heap MB] [-rms DIR] [-v] game.jar|game.jad
```

Keys: arrows, Return/Space = fire, F1/F2 = soft keys, 0-9, numpad `*` and `/`,
Esc = quit. `-w`/`-h` set the screen size the game sees (128x128, 176x208,
240x320, ...: whatever the game was made for); `-scale 2` shows every pixel
2x2, for cards without small screen modes.

## Targets

| Target | Toolchain | State |
|---|---|---|
| AmigaOS 3.x, 68060 + FPU, RTG | bebbo `m68k-amigaos-gcc` 6.5 (`/opt/amiga`) | runs (tested in Amiberry) |
| WarpOS (PPC accelerator cards) | adtools `ppc-amigaos-gcc` 8.4 + MOS2WOS SDK | runs (tested in Amiberry, CyberStorm PPC) |
| MorphOS | `ppc-morphos-gcc` 11 (`/gg`) | builds; never run |
| Linux host (32-bit) | gcc -m32 | development and tests, offscreen |

Needs cybergraphics.library V41+ (CyberGraphX or Picasso96) with a 16-bit
screen mode; AHI for sound (optional).

## What is in it

- **VM**: phoneME Feature CLDC-HI with its portable C interpreter
  (`vm/phoneme-cldc`, GPLv2, see `vm/UPSTREAM.md`), ported to the Amiga
  (`src/vm/os/amiga`), with threaded bytecode dispatch.
- **MIDP** (`java/src`, written for this project): `lcdui` (Canvas, Graphics,
  Image, Font, Form and its items, List, TextBox, Alert, commands),
  `lcdui.game` (GameCanvas, Sprite, TiledLayer, LayerManager), `rms`
  (record stores as files), `midlet`, `java.util.Timer`.
- **Nokia UI API**: FullCanvas, DirectGraphics, DirectUtils, DeviceControl, Sound.
- **Sound** (JSR-135 subset): WAV, MIDI files, tone sequences and Nokia ringing
  tones. MIDI is played by a small built-in synthesiser (`native/audio`), no
  sound bank needed.
- **3D** (JSR-184, M3G): own implementation: scene graph, animation, `.m3g`
  loader in Java, software rasteriser in C (`native/g3d`): lighting with
  specular highlights, fog, two texture units with perspective correction,
  blending, depth buffer, sprites, skinning, morphing, picking.
- **Mascot Capsule micro3d v3** (`com.mascotcapsule.micro3d.v3`): own
  implementation on the same rasteriser: MBAC figures (versions 2-5) with
  bones, patterns and several textures, MTRA actions, BMP textures with a
  colour key, lighting and toon shading, blending, primitives and command
  lists, sphere-map reflections. The same engine serves the older API of
  Motorola, Vodafone and JBlend phones (`com.motorola.graphics.j3d`,
  `com.vodafone.v10.graphics.j3d`, `com.jblend.graphics.j3d`) and Motorola
  iDEN (`com.motorola.iden.micro3d`; its render-to-texture is not done).
  Tested with models made by `tools/make-test-mbac.py` only, not yet with a
  commercial game.
- **Vendor APIs**: Siemens (`com.siemens.mp.game`, `color_game`, `io.File`,
  `ui`/`lcdui` Image), Samsung (`com.samsung.util`), Motorola
  (`multimedia`, `funlight`, `midi`). Vibration and lights do nothing.
- **Networking**: `socket://` and `http://` (HTTP/1.0) through
  bsdsocket.library, `ssl://` and `https://` through AmiSSL 5 when it is
  installed (certificates are checked against AmiSSL's store; `setenv
  AJ_TLS_NOVERIFY 1` turns that off). The calls block. No datagrams or server
  sockets.
- **Platform layer** (`hal/amiga`, from the author's civctp2 port): RTG screen,
  input, timers, AHI, a fast `malloc`.

## Settings per game

A text file named like the game plus `.cfg` (`game.jar.cfg`) is read at start:

```
w=176
h=208
scale=2
heap=16
microedition.platform=Nokia6230
microedition.locale=pl
```

`w`, `h`, `scale` and `heap` are the command line options (which win); every
other line is a system property for `System.getProperty`.

## Build

```sh
tools/fetch-ecj.sh          # once: the Java compiler used for 1.4 class files
tools/gen-vm-amiga.sh       # host tools + generated VM sources      -> build/vmgen
tools/build-classes.sh      # class library + natives table           -> build/api
for t in m68k-amigaos:aos3-m68k-rel ppc-warpos:wos-ppc ppc-morphos:morphos-ppc host32:host32; do
  PATH=/opt/amiga/bin:$PATH cmake -S . -B build/${t#*:} -DCMAKE_TOOLCHAIN_FILE=cmake/toolchains/${t%%:*}.cmake
  PATH=/opt/amiga/bin:$PATH cmake --build build/${t#*:} -j4
done
tools/dist.sh               # dist/<target>/{j2me, aj_classes.zip, README.txt}
```

After changing Java sources or adding native methods: `tools/build-classes.sh`,
then rebuild the targets (the natives table is compiled in).

## Tests

`tools/build-tests.sh` builds `tests/java` (plain CLDC programs) and
`tests/midlets`. On the host:

```sh
build/host32/vm/aj_cldc -classpath build/api/aj_classes.zip,build/tests/classes VmTest
AJ_SHOT=out.ppm AJ_KEYS="500:4C 100:-4C 1000:Q" build/host32/vm/j2me build/tests/classes GfxTest
```

`AJ_KEYS` scripts input (delay in ms, raw Amiga key code, `S` = numbered
screenshot, `Q` = quit); `AJ_SHOT` and `AJ_WAV` capture the picture and the
sound. `VmTest` prints checksums that must be the same on every target.

## Notes for porters

- bebbo gcc 6.5: its own `-fbbb` "s" pass miscompiled the VM (a dropped load,
  an uninitialised register stored: the first full GC hung). The toolchain
  file turns all of those passes off (`-fbbb=-`), at no measurable cost.
- The VM expects the memory for its heap to be zeroed (true of a big `malloc`
  on Linux, not of AmigaOS memory): `OsMemory_allocate` clears it. Before
  that, stack map generation failed now and then, differently on each run.
- WarpOS executables are made on the build host (`tools/elf2warpos`).
- On WarpOS every 68k call from the PPC is expensive, and in Amiberry a PPC
  task that waits for one can stall without the "waker" task in
  `hal/amiga/amiga_clock.c`.
- PPC cards run by the Sonnet `powerpc.library` (Mediator/Prometheus PCI):
  its loader takes any program whose file holds the bytes `RACE`,
  `powerpc.library` or `AmigaAMP` for a PowerPC one, remembers that in the
  file's protection bits 16/17, and from then on gives the program "ppc
  memory" for its hunks and for every `AllocMem`. A 68060 reaches that memory
  many times more slowly: the 68k VM ran 27 times slower on a real A4000
  because of names like `AJ_TLS_TRACE` in its strings. `tools/dist.sh` stops
  if the 68k program holds one of the three (`tools/check-68k-classifier.py`).
  A file that was once marked stays marked when it is overwritten: copy the
  new one under a new name, or clear the bits.
- For the same reason a WarpOS program's own 68k side, and any process it
  creates, allocates from ppc memory. AmiSSL run that way needed 14 s for a
  TLS handshake; so on such a card the WarpOS build has AmiSSL run by a 68k
  helper process that is started from a plain exec task placed in Fast RAM
  (`hal/amiga/stub/tls68k.s`), and the handshake takes 1.9 s.

## Licence

GPLv2 (see `COPYING`): the VM is phoneME (GPLv2 only). `third_party/stb` is
public domain.
