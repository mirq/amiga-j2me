# amiga-j2me

A J2ME (MIDP 2.0 / CLDC 1.1) runtime for the Amiga with RTG graphics, including
JSR-184 M3G 3D and MIDI audio.

Targets:

| Target | Toolchain | Notes |
|---|---|---|
| AmigaOS 3.x, 68060 + FPU | bebbo `m68k-amigaos-gcc` 6.5 (`/opt/amiga/bin`) | CyberGraphX / Picasso96 RTG |
| MorphOS (PPC) | `ppc-morphos-gcc` 11 (`/gg`) | build-only for now |
| WarpOS (classic PPC cards) | adtools `ppc-amigaos-gcc` + MOS2WOS SDK | ET_REL `.warpelf`, convert on the Amiga with `Elf2Exe2` |
| Linux host | gcc + SDL2 | development and testing |

VM: phoneME Feature CLDC-HI with its portable C interpreter loop
(`vm/phoneme-cldc`, see `vm/UPSTREAM.md`).

## Status

M0 (toolchains and build skeleton) is done: `aj_hello` builds for all targets
and runs on a real 68060. M1 (VM port) is in progress.

## Build

```sh
PATH=/opt/amiga/bin:$PATH ./tools/build-all.sh
```

Outputs go to `build/<target>/`. For WarpOS, on the Amiga:

```
Stack 100000
Elf2Exe2 aj_hello.warpelf aj_hello
```

## License

GPLv2 (see `COPYING`), because the VM is phoneME (GPLv2-only).
Third-party components with incompatible licenses (Nokia M3G core, EPL-1.0;
Sonivox EAS, Apache-2.0) will be built as separate Amiga shared libraries.
