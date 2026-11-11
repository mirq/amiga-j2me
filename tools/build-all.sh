#!/bin/sh
# Build every available target into build/<target>.
set -e
cd "$(dirname "$0")/.."
build() { # name toolchain
  cmake -S . -B "build/$1" ${2:+-DCMAKE_TOOLCHAIN_FILE=cmake/toolchains/$2} >/dev/null
  cmake --build "build/$1" -j
}
build host ""
build aos3-m68k m68k-amigaos.cmake
if command -v ppc-morphos-gcc >/dev/null; then build morphos-ppc ppc-morphos.cmake; fi
if [ -x /home/mirek/ppc-amigaos-gcc/native-build/root-cross/bin/ppc-amigaos-gcc ]; then build wos-ppc ppc-warpos.cmake
else echo "skip warpos: ppc-amigaos-gcc not found"; fi
