#!/bin/sh
# Run a MIDlet JAR (or class directory) on the host build.
# usage: tools/run-host.sh game.jar [launcher args...]   env: AJ_KEYS, AJ_SHOT, HEAP (default 8M)
cd "$(dirname "$0")/.."
J="$1"; shift
exec build/host32/vm/aj_cldc =HeapCapacity${HEAP:-8M} -classpath "build/api/aj_classes.zip,$J" aj.Launcher "$@"
