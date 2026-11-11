#!/bin/sh
# Elf2Exe2 input contract (mirrors WOS_VALIDATE_ETREL from MiniGL_WOS_V19).
# usage: validate-etrel.sh <toolchain-bin-dir> <file.warpelf>
B="$1/ppc-amigaos-"; F="$2"; rc=0
u=$(${B}nm --undefined-only "$F"); [ -z "$u" ] || { echo "Undefined symbols:"; echo "$u"; rc=1; }
${B}nm --defined-only --format=posix "$F" | awk '$1=="_start" && $2=="T" && $3=="00000000"{f=1} END{exit !f}' \
  || { echo "_start not at .text+0"; rc=1; }
c=$(${B}nm --format=posix "$F" | awk '$2=="C"{print $1}'); [ -z "$c" ] || { echo "COMMON symbols: $c"; rc=1; }
r=$(${B}readelf --relocs --wide "$F" | awk '$3 ~ /^R_PPC_/ && $3 !~ /^R_PPC_(ADDR32|ADDR16_LO|ADDR16_HA|REL24)$/ {print $3}' | sort -u)
[ -z "$r" ] || { echo "Unsupported relocations: $r"; rc=1; }
[ $rc = 0 ] && echo "ET_REL OK: $F"
exit $rc
