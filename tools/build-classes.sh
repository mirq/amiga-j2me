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
# The older Mascot Capsule API of Motorola, Vodafone and JBlend phones: the same
# classes in other packages, drawn through lcdui.Graphics (which implements
# each package's Graphics3D interface).
for P in com.motorola.graphics.j3d com.vodafone.v10.graphics.j3d com.jblend.graphics.j3d; do
  G=$OUT/gen/$(echo $P | tr . /)
  mkdir -p $G
  for f in java/src/com/mascotcapsule/micro3d/v3/*.java; do
    n=$(basename $f .java)
    [ $n = Graphics3D ] && n=Render3D
    sed -e "s/^package com\.mascotcapsule\.micro3d\.v3;/package $P;/" \
        -e 's/\bGraphics3D\b/Render3D/g' $f > $G/$n.java
  done
  cat > $G/Graphics3D.java <<JAVA
package $P;
/** Implemented by javax.microedition.lcdui.Graphics. */
public interface Graphics3D {
    void drawFigure(Figure figure, int x, int y, FigureLayout layout, Effect3D effect);
}
JAVA
  cat > $G/RenderProxy.java <<JAVA
package $P;
import javax.microedition.lcdui.Graphics;
/** Draws a figure into a Graphics; not part of the phone API. */
public final class RenderProxy {
    private static final Render3D R = new Render3D();
    private RenderProxy() {}
    public static synchronized void drawFigure(Graphics g, Figure figure, int x, int y, FigureLayout layout, Effect3D effect) {
        R.bind(g);
        try {
            R.drawFigure(figure, x, y, layout, effect);
        } finally {
            R.release(g);
        }
    }
}
JAVA
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
