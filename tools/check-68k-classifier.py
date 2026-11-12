#!/usr/bin/env python3
"""Stop if a 68k program holds a byte sequence that makes the Sonnet
powerpc.library (PPC cards on Mediator/Prometheus PCI boards) take it for a
PowerPC program.

Its LoadSeg patch scans the loaded hunks for "RACE", "powerpc.library" and
"AmigaAMP". A program with any of them is marked PPC (and the mark is kept in
the file's protection bits 16/17): its hunks and every AllocMem of its task
then come from "ppc memory", which the 68060 reaches many times more slowly.
Seen with amiga-j2me on a real A4000: the VM ran 27 times slower, because of
the names AJ_TLS_TRACE and ENABLE_STACK_TRACE in its strings.
A file once marked stays marked: after replacing it, set the bits right, e.g.
with the bridge's protection setter: (bits & ~0x00030000) | 0x00020000.
"""
import sys

SIGNATURES = (b"RACE", b"powerpc.library", b"AmigaAMP")
bad = 0
for path in sys.argv[1:]:
    data = open(path, "rb").read()
    for sig in SIGNATURES:
        at = data.find(sig)
        while at >= 0:
            print("%s: %r at 0x%x: %r" % (path, sig, at, data[max(0, at - 24):at + 12]))
            bad = 1
            at = data.find(sig, at + 1)
if bad:
    sys.exit("a 68k program must not hold these bytes (see the top of this script)")
