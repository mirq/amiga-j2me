#!/bin/sh
# usage: tools/check-m68k-bittest.sh m68k-executable...
# bebbo's m68k gcc 6.5 can compile a test of one bit of a 32-bit word in memory
# into a btst/bset/bclr/bchg with a NEGATIVE bit number and no byte offset
# (seen: "btst #-24,(a0)" for bit 0), which acts on the wrong byte. This looks
# for such instructions in the code; tables that merely disassemble that way
# are told apart by their neighbours (real code has few of them in a row).
# Exit 1 when one is found.
bad=0
for f in "$@"; do
  /opt/amiga/bin/m68k-amigaos-objdump -d "$f" 2>/dev/null | awk -v file="$f" '
    /^[0-9a-f]+ [0-9a-f]+ _/ { fn = $3 }
    { line[NR] = $0; name[NR] = fn
      hit[NR] = ($0 ~ /\t(btst|bset|bclr|bchg) #-[0-9]+,[^d]/) }
    END {
      for (i = 1; i <= NR; i++) if (hit[i]) {
        n = 0
        for (k = i - 6; k <= i + 6; k++) if (k >= 1 && k <= NR && k != i && line[k] ~ /\t(btst|bset|bclr|bchg|ori\.b|andi\.b|\.short|\.long) /) n++
        if (n < 3) { print file ": suspicious bit instruction in " name[i] "\n  " line[i]; found = 1 }
      }
      exit found ? 1 : 0
    }' || bad=1
done
exit $bad
