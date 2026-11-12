# amiga-j2me

A J2ME runtime (MIDP 2.0 / CLDC 1.1) for the Amiga with RTG graphics: it runs
mobile-phone Java games (`.jar`) on a 68060 or a PowerPC Amiga.

```
j2me [-w WIDTH] [-h HEIGHT] [-heap MB] [-rms DIR] [-v] game.jar
```

Keys: arrows, Return/Space = fire, F1/F2 = soft keys, 0-9, numpad `*` and `/`,
Esc = quit. `-w`/`-h` set the screen size the game sees (128x128, 176x208,
240x320, ...: whatever the game was made for).

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
  loader in Java, software rasteriser in C (`native/g3d`): lighting, textures
  with perspective correction, blending, depth buffer, sprites, skinning,
  morphing.
- **Platform layer** (`hal/amiga`, from the author's civctp2 port): RTG screen,
  input, timers, AHI, a fast `malloc`.

Not there yet: networking (`Connector.open` of http/socket fails), M3G fog,
specular highlights, picking and the second texture unit, Siemens / Samsung /
Motorola vendor APIs, Mascot Capsule 3D, a JAD reader, per-game settings.

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

- bebbo gcc 6.5: the `-fbbb` "s" pass miscompiles (a dropped load, an
  uninitialised register stored); the toolchain file leaves it out.
- WarpOS executables are made on the build host (`tools/elf2warpos`).
- On WarpOS every 68k call from the PPC is expensive, and in Amiberry a PPC
  task that waits for one can stall without the "waker" task in
  `hal/amiga/amiga_clock.c`.

## Licence

GPLv2 (see `COPYING`): the VM is phoneME (GPLv2 only). `third_party/stb` is
public domain.
