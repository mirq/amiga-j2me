#!/bin/sh
# Build the phoneME CLDC-HI VM (C interpreter loop) for the Linux host (32-bit).
# Output: build/cldc-linux_c/linux_c/dist/bin/cldc_vm
set -e
cd "$(dirname "$0")/.."
ROOT=$PWD
[ -f tools/ecj-4.6.1.jar ] || tools/fetch-ecj.sh
OUT=${VMBUILD:-build/cldc-linux_c}
mkdir -p $OUT
# Old (2007-2009) C++: no narrowing errors, no modern-GCC UB-based optimizations.
OLDCXX="-Wno-narrowing -fno-strict-aliasing -fno-delete-null-pointer-checks -fno-lifetime-dse -fpermissive"
exec make -C vm/phoneme-cldc/build/linux_c \
  host_arch=i386 \
  ENABLE_C_INTERPRETER=true \
  ENABLE_COMPILATION_WARNINGS=true \
  ENABLE_DEBUGGER=false \
  USER_CFLAGS="$OLDCXX" \
  JDK_DIR=$ROOT/tools/jdk-shim \
  JVMWorkSpace=$ROOT/vm/phoneme-cldc \
  JVMBuildSpace=$ROOT/$OUT "$@"
