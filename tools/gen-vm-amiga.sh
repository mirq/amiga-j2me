#!/bin/sh
# Generate the CLDC-HI target tree for the Amiga platform (amiga_c):
# host tools + romgen run on Linux, then the target "dependencies" step
# writes jvmconfig.h, incls/, merged sources, NativesTable.cpp, OopMaps.cpp.
# Output: build/vmgen/amiga_c/target/{generated,release,debug}
set -e
cd "$(dirname "$0")/.."
ROOT=$PWD
[ -f tools/ecj-4.6.1.jar ] || tools/fetch-ecj.sh
OUT=build/vmgen
mkdir -p $OUT
OLDCXX="-Wno-narrowing -fno-strict-aliasing -fno-delete-null-pointer-checks -fno-lifetime-dse -fpermissive"
M="make -C vm/phoneme-cldc/build/amiga_c \
  ENABLE_COMPILATION_WARNINGS=true USER_CFLAGS=\"$OLDCXX\" \
  JDK_DIR=$ROOT/tools/jdk-shim JVMWorkSpace=$ROOT/vm/phoneme-cldc JVMBuildSpace=$ROOT/$OUT"
eval $M tools romgen
for b in ${BUILDS:-release debug}; do
  eval $M $ROOT/$OUT/amiga_c/target/$b/Makefile
  T=$OUT/amiga_c/target/$b
  eval make -C $T dependencies JDK_DIR=$ROOT/tools/jdk-shim JVMWorkSpace=$ROOT/vm/phoneme-cldc JVMBuildSpace=$ROOT/$OUT
  eval make -C $T NativesTable.cpp OopMaps.cpp JDK_DIR=$ROOT/tools/jdk-shim JVMWorkSpace=$ROOT/vm/phoneme-cldc JVMBuildSpace=$ROOT/$OUT
done
echo "generated: $OUT/amiga_c/target"
