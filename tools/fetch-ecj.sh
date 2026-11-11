#!/bin/sh
# Fetch the Eclipse Java compiler (ECJ 4.6.1): unlike modern javac it still
# emits -target 1.4 class files, which CLDC-HI and its preverifier need.
set -e
cd "$(dirname "$0")"
J=ecj-4.6.1.jar
[ -f "$J" ] || curl -fsSL -o "$J" https://repo1.maven.org/maven2/org/eclipse/jdt/core/compiler/ecj/4.6.1/ecj-4.6.1.jar
echo "ok: tools/$J"
