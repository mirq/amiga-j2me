#!/usr/bin/env python3
"""hunkmerge.py IN OUT : turns a stripped AmigaOS executable with code, data
and BSS hunks into one with a single CODE hunk holding all three.

Why: on a machine with WarpOS (powerpc.library) the loader can put a 68k
program's DATA and BSS hunks into "ppc memory", the PowerPC card's RAM, which
the 68060 reaches far more slowly than its own Fast RAM (the VM ran 25 times
slower on a real A4000 that way). CODE hunks stay in Fast RAM, so everything
goes into the CODE hunk. A few instructions put in front clear the BSS part,
which the loader does not promise to do for a CODE hunk."""
import struct, sys

HEADER, CODE, DATA, BSS, RELOC32, END, SYMBOL, DEBUG, DREL32, RELOC32SHORT = (
    0x3F3, 0x3E9, 0x3EA, 0x3EB, 0x3EC, 0x3F2, 0x3F0, 0x3F1, 0x3F7, 0x3FC)

def u32(d, p): return struct.unpack('>I', d[p:p+4])[0]

def parse(d):
    assert u32(d, 0) == HEADER, 'not an AmigaOS executable'
    p = 4
    assert u32(d, p) == 0, 'resident libraries are not supported'
    p += 4
    n, first, last = struct.unpack('>III', d[p:p+12]); p += 12
    sizes = [u32(d, p + 4*i) for i in range(n)]; p += 4*n
    assert all(s >> 30 == 0 for s in sizes), 'hunks with memory flags are not supported'
    hunks = []          # {type, data (bytearray), alloc (bytes), relocs: [(target, offset)]}
    for i in range(n):
        h = None
        while True:
            t = u32(d, p) & 0x3FFFFFFF; p += 4
            if t in (CODE, DATA):
                ln = u32(d, p) * 4; p += 4
                h = dict(type=t, data=bytearray(d[p:p+ln]), alloc=(sizes[i] & 0x3FFFFFFF) * 4, relocs=[]); p += ln
            elif t == BSS:
                p += 4
                h = dict(type=t, data=bytearray(), alloc=(sizes[i] & 0x3FFFFFFF) * 4, relocs=[])
            elif t == RELOC32:
                while True:
                    cnt = u32(d, p); p += 4
                    if cnt == 0: break
                    target = u32(d, p); p += 4
                    for k in range(cnt): h['relocs'].append((target, u32(d, p + 4*k)))
                    p += 4 * cnt
            elif t in (DREL32, RELOC32SHORT):
                while True:
                    cnt = struct.unpack('>H', d[p:p+2])[0]; p += 2
                    if cnt == 0: break
                    target = struct.unpack('>H', d[p:p+2])[0]; p += 2
                    for k in range(cnt): h['relocs'].append((target, struct.unpack('>H', d[p+2*k:p+2*k+2])[0]))
                    p += 2 * cnt
                if p & 3: p += 2
            elif t == SYMBOL:
                while True:
                    ln = u32(d, p) & 0xFFFFFF; p += 4
                    if ln == 0: break
                    p += 4 * ln + 4
            elif t == DEBUG:
                p += 4 + 4 * u32(d, p)
            elif t == END:
                break
            else:
                raise SystemExit('unsupported hunk type %x' % t)
        hunks.append(h)
    return hunks

def merge(hunks):
    assert hunks[0]['type'] == CODE, 'the first hunk must be code'
    # the stub: keep d0/a0 (the argument line), clear the BSS part, fall into the program
    #   movem.l d0/a0,-(sp) ; lea stub_end(pc),a0 ; adda.l #bss_offset,a0 ; move.l #longs,d0
    #   1: clr.l (a0)+ ; subq.l #1,d0 ; bne.s 1b ; movem.l (sp)+,d0/a0 ; nop
    STUB = 32
    base, off = [], STUB
    for h in hunks:
        base.append(off)
        off += h['alloc']
    stored_end = STUB
    for i, h in enumerate(hunks):
        if h['data']: stored_end = base[i] + len(h['data'])
    stored_end = (stored_end + 3) & ~3
    total = off
    clear_from, clear_longs = stored_end, (total - stored_end) // 4
    stub = bytearray()
    stub += bytes.fromhex('48e78080')                               # movem.l d0/a0,-(sp)
    stub += bytes.fromhex('41fa') + struct.pack('>h', STUB - 6)     # lea stub_end(pc),a0
    stub += bytes.fromhex('d1fc') + struct.pack('>I', clear_from - STUB)    # adda.l #..,a0
    if clear_longs:
        stub += bytes.fromhex('203c') + struct.pack('>I', clear_longs)      # move.l #..,d0
        stub += bytes.fromhex('4298')                               # clr.l (a0)+
        stub += bytes.fromhex('5380')                               # subq.l #1,d0
        stub += bytes.fromhex('66fa')                               # bne.s clr
    else:
        stub += bytes.fromhex('4e71' * 6)
    stub += bytes.fromhex('4cdf0101')                               # movem.l (sp)+,d0/a0
    stub += bytes.fromhex('4e71') * ((STUB - len(stub)) // 2)
    assert len(stub) == STUB
    image = bytearray(stored_end)
    image[:STUB] = stub
    relocs = []
    for i, h in enumerate(hunks):
        image[base[i]:base[i] + len(h['data'])] = h['data']
        for target, o in h['relocs']:
            at = base[i] + o
            image[at:at+4] = struct.pack('>I', (u32(image, at) + base[target]) & 0xFFFFFFFF)
            relocs.append(at)
    out = struct.pack('>IIIIII', HEADER, 0, 1, 0, 0, total // 4)
    out += struct.pack('>II', CODE, len(image) // 4) + bytes(image)
    relocs.sort()
    if relocs:
        out += struct.pack('>III', RELOC32, len(relocs), 0) + b''.join(struct.pack('>I', r) for r in relocs) + struct.pack('>I', 0)
    out += struct.pack('>I', END)
    return out, total, len(image)

if __name__ == '__main__':
    src, dst = sys.argv[1], sys.argv[2]
    hunks = parse(open(src, 'rb').read())
    out, total, stored = merge(hunks)
    open(dst, 'wb').write(out)
    print('%s: %d hunks -> 1 code hunk, %d bytes in memory (%d in the file)' % (dst, len(hunks), total, stored))
