#!/bin/sh
# Compile + preverify the Java tests (tests/java: plain CLDC programs,
# tests/midlets: MIDlets) against the runtime's class library.
# Output: build/tests/classes (also copied to $AJ_DEPLOY/tests if set)
set -e
cd "$(dirname "$0")/.."
Z=build/api/aj_classes.zip
PV=build/vmgen/amiga_c/dist/bin/preverify
rm -rf build/tests/tmp build/tests/classes; mkdir -p build/tests/tmp build/tests/classes
java -jar tools/ecj-4.6.1.jar -source 1.4 -target 1.4 -nowarn -bootclasspath $Z -d build/tests/tmp \
     tests/java/src/*.java tests/midlets/src/*.java
$PV -classpath $Z -d build/tests/classes build/tests/tmp
cp tests/midlets/res/* build/tests/classes/ 2>/dev/null || true
if [ -n "$AJ_DEPLOY" ]; then
  mkdir -p "$AJ_DEPLOY/tests"; cp build/tests/classes/* "$AJ_DEPLOY/tests/"; cp $Z "$AJ_DEPLOY/"
fi
echo "tests built"
