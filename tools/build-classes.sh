#!/bin/sh
# Build the runtime's class library: CLDC (from the phoneME build) + our MIDP
# classes (java/src), and the natives table for all of it.
# Output: build/api/aj_classes.zip, build/api/NativesTable.cpp
set -e
cd "$(dirname "$0")/.."
GEN=build/vmgen/amiga_c
CLDC=$GEN/dist/lib/cldc_classes.zip
[ -f $CLDC ] || { echo "run tools/gen-vm-amiga.sh first"; exit 1; }
OUT=build/api
rm -rf $OUT/tmp $OUT/classes; mkdir -p $OUT/tmp $OUT/classes
# com.siemens.mp.color_game is the MIDP 2 game API under another package name
G=$OUT/gen/com/siemens/mp/color_game
rm -rf $OUT/gen; mkdir -p $G
for f in java/src/javax/microedition/lcdui/game/*.java; do
  sed -e 's/^package javax\.microedition\.lcdui\.game;/package com.siemens.mp.color_game;/' \
      -e 's/javax\.microedition\.lcdui\.game\./com.siemens.mp.color_game./g' $f > $G/$(basename $f)
done
find java/src $OUT/gen -name '*.java' > $OUT/sources.txt
java -jar tools/ecj-4.6.1.jar -source 1.4 -target 1.4 -nowarn -bootclasspath $CLDC \
     -d $OUT/tmp @$OUT/sources.txt
$GEN/dist/bin/preverify -classpath $CLDC -d $OUT/classes $OUT/tmp
(cd $OUT/classes && unzip -q -o ../../../$CLDC)          # CLDC classes are already preverified
rm -rf $OUT/classes/META-INF     # a manifest here would hide the MIDlet JAR's
rm -f $OUT/aj_classes.zip
(cd $OUT/classes && zip -q -r -0 ../aj_classes.zip .)
java -jar $GEN/tools/jcc.jar -writer CLDC_HI_Natives -o $OUT/NativesTable.cpp $OUT/aj_classes.zip
echo "built $OUT/aj_classes.zip ($(unzip -l $OUT/aj_classes.zip | tail -1 | awk '{print $2}') files), $(grep -c JVM_NATIVE $OUT/NativesTable.cpp) natives"
