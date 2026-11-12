#!/usr/bin/env python3
"""Writes the Mascot Capsule test data for McTest: tests/midlets/res/mc.mbac (a
body and an arm on two bones, MBAC version 5 with packed vertices, normals and
polygons), mc_old.mbac (a version 3 file: plain 16-bit fields), mc.mtra (the
arm swinging, a pattern switching on) and mc.bmp (256 colours, entry 0 = see
through). The layout follows what the loader in
java/src/com/mascotcapsule/micro3d/v3 reads."""
import os, struct, sys

out = sys.argv[1] if len(sys.argv) > 1 else 'tests/midlets/res'

class Bits:
    """bit fields packed from the low bit of each byte upwards"""
    def __init__(self): self.b = bytearray(); self.acc = 0; self.n = 0
    def put(self, v, bits):
        self.acc |= (v & ((1 << bits) - 1)) << self.n; self.n += bits
        while self.n >= 8: self.b.append(self.acc & 255); self.acc >>= 8; self.n -= 8
    def align(self):
        if self.n: self.b.append(self.acc & 255)
        self.acc = 0; self.n = 0
    def raw(self, data): assert self.n == 0; self.b += data

def cross(a, b): return (a[1]*b[2]-a[2]*b[1], a[2]*b[0]-a[0]*b[2], a[0]*b[1]-a[1]*b[0])

AXIS = {(1,0,0): 0, (0,1,0): 1, (0,0,1): 2, (-1,0,0): 3, (0,-1,0): 4, (0,0,-1): 5}

def box(cx, cy, cz, sx, sy, sz):
    """24 vertices (4 per face) with face normals, 6 quads as TL, TR, BL, BR seen from outside (y down)"""
    verts, norms, quads = [], [], []
    faces = [((0,0,-1), (1,0,0), (0,1,0)), ((0,0,1), (-1,0,0), (0,1,0)), ((1,0,0), (0,0,1), (0,1,0)),
             ((-1,0,0), (0,0,-1), (0,1,0)), ((0,-1,0), (1,0,0), (0,0,-1)), ((0,1,0), (1,0,0), (0,0,1))]
    for n, r, d in faces:
        assert cross(r, d) == tuple(-c for c in n)
        base = len(verts)
        for dv, rv in ((-1,-1), (-1,1), (1,-1), (1,1)):       # TL, TR, BL, BR
            p = [n[i] + r[i]*rv + d[i]*dv for i in range(3)]
            verts.append((cx + p[0]*sx//2, cy + p[1]*sy//2, cz + p[2]*sz//2))
            norms.append(n)
        quads.append((base, base+1, base+2, base+3))
    return verts, norms, quads

body_v, body_n, body_q = box(0, 0, 0, 60, 100, 40)
arm_v, arm_n, arm_q = box(0, 35, 0, 20, 70, 20)        # hangs from the arm bone's origin
hat_v = [(-20, -50, -15), (20, -50, -15), (0, -85, 0), (20, -50, 15), (-20, -50, 15)]   # coloured, on the body bone
hat_n = [(0, -1, 0)] * 5
verts = body_v + hat_v + arm_v
norms = body_n + hat_n + arm_n
nb = len(body_v) + len(hat_v)
tq = body_q + [tuple(i + nb for i in q) for q in arm_q]
hb = len(body_v)
# coloured triangles: the hat (pattern 0) and a badge that only pattern 1 shows
c3 = [(hb, hb+1, hb+2, 0, 0), (hb+1, hb+3, hb+2, 1, 0), (hb+3, hb+4, hb+2, 0, 0), (hb+4, hb, hb+2, 1, 0),
      (0, 1, 2, 2, 1)]
colors = [(255, 200, 0), (220, 60, 30), (255, 255, 255)]
LIT, TWO_SIDED, KEY, SPHERE = 0x20, 0x10, 0x01, 0x40

def bones_bytes():
    def bone(count, parent, tx, ty, tz):
        m = [4096, 0, 0, tx, 0, 4096, 0, ty, 0, 0, 4096, tz]
        return struct.pack('<Hh12h', count, parent, *m)
    return bone(nb, -1, 0, 0, 0) + bone(len(arm_v), 0, 42, -40, 0)

def uvs(face):
    # every face shows the whole 64x64 texture; the arm uses its left half
    return [(0, 0), (63, 0), (0, 63), (63, 63)] if face < 6 else [(0, 0), (31, 0), (0, 63), (31, 63)]

def mbac_v5():
    b = Bits()
    b.raw(struct.pack('<2sBB4B', b'MB', 5, 0, 2, 2, 3, 1))
    b.raw(struct.pack('<4H5H', len(verts), 0, len(tq), 2, len(c3), 0, 1, 2, len(colors)))
    # pattern 0: 4 coloured triangles, all the textured quads; pattern 1: the badge
    b.raw(struct.pack('<4H', 4, 0, 0, len(tq)))
    b.raw(struct.pack('<4H', 1, 0, 0, 0))
    for i in range(0, len(verts), 64):                  # vertices: runs of up to 64, 10 bits per coordinate
        run = verts[i:i+64]
        b.put((1 << 6) | (len(run) - 1), 8)
        for v in run:
            for c in v: b.put(c, 10)
    b.align()
    for n in norms:                                     # normals: all along an axis here
        b.put(64, 7); b.put(AXIS[n], 3)
    b.align()
    b.raw(bytes([7, 8, 8, 2, 0]))                       # coloured: material, index, colour and colour id bits
    for c in colors:
        for k in c: b.put(k, 8)
    for a, bb, c, col, pat in c3:
        b.put((LIT | TWO_SIDED) >> 1, 7); b.put(a, 8); b.put(bb, 8); b.put(c, 8); b.put(col, 2)
    for v in (7, 8, 7, 0): b.put(v, 8)                  # textured: material, index, uv bits
    for f, q in enumerate(tq):
        b.put(LIT | KEY if f < 6 else LIT | SPHERE, 7)
        for i in q: b.put(i, 8)
        for u, v in uvs(f): b.put(u, 7); b.put(v, 7)
    b.align()
    b.raw(bones_bytes())
    b.raw(bytes(20))
    return bytes(b.b)

def mbac_v3():
    d = struct.pack('<2sBB4H', b'MB', 3, 0, nb, 0, len(body_q), 1)
    for v in verts[:nb]: d += struct.pack('<3h', *v)
    for q in body_q:
        d += struct.pack('<H4H', 1 | 2, *q)             # quad + colour key
        for u, v in uvs(0): d += struct.pack('<2B', u, v)
    d += struct.pack('<Hh12h', nb, -1, 4096, 0, 0, 0, 0, 4096, 0, 0, 0, 0, 4096, 0)
    return d

def mtra():
    d = struct.pack('<2sBB2H', b'MT', 5, 0, 1, 2) + struct.pack('<8H', 0, 1, 0, 0, 0, 0, 1, 0) + struct.pack('<i', 0)
    d += struct.pack('<H', 16)                          # action 0: 16 key frames
    d += bytes([1])                                     # body: identity
    d += bytes([6])                                     # arm: translation, direction and roll tracks
    d += struct.pack('<H', 1) + struct.pack('<H3h', 0, 0, 0, 0)
    d += struct.pack('<H', 3) + struct.pack('<H3h', 0, 0, 0, 4096) + struct.pack('<H3h', 8, 0, -2896, 2896) \
       + struct.pack('<H3h', 16, 0, 0, 4096)            # the arm's z axis tips up and back again
    d += struct.pack('<H', 2) + struct.pack('<Hh', 0, 0) + struct.pack('<Hh', 16, 1024)    # and it rolls a quarter turn
    d += struct.pack('<H', 2) + struct.pack('<HI', 0, 0) + struct.pack('<HI', 8, 2)        # pattern 1 from frame 8
    d += bytes(20)
    return d

def bmp():
    w = h = 64
    pal = [(0, 0, 0)] * 256
    pal[1] = (40, 90, 200); pal[2] = (240, 240, 240); pal[3] = (30, 160, 60)
    rows = []
    for y in range(h):
        row = bytearray()
        for x in range(w):
            c = 1 if ((x // 8) + (y // 8)) % 2 == 0 else 2
            if (x - 32) ** 2 + (y - 32) ** 2 < 100: c = 0           # a hole in the middle
            if x < 3 or y < 3 or x > 60 or y > 60: c = 3
            row.append(c)
        rows.append(bytes(row))
    data = b''.join(reversed(rows))
    head = struct.pack('<2sIHHI', b'BM', 14 + 40 + 1024 + len(data), 0, 0, 14 + 40 + 1024)
    info = struct.pack('<IiiHHIIiiII', 40, w, h, 1, 8, 0, len(data), 2835, 2835, 256, 0)
    return head + info + b''.join(struct.pack('<4B', b, g, r, 0) for r, g, b in pal) + data

def sphere_bmp():
    """a soft highlight, upper left: what the arm 'reflects'"""
    w = h = 64
    rows = []
    for y in range(h):
        row = bytearray()
        for x in range(w):
            d = ((x - 22) ** 2 + (y - 22) ** 2) ** 0.5
            row.append(max(0, min(255, int(230 - d * 9))))
        rows.append(bytes(row))
    data = b''.join(reversed(rows))
    head = struct.pack('<2sIHHI', b'BM', 14 + 40 + 1024 + len(data), 0, 0, 14 + 40 + 1024)
    info = struct.pack('<IiiHHIIiiII', 40, w, h, 1, 8, 0, len(data), 2835, 2835, 256, 0)
    return head + info + b''.join(struct.pack('<4B', i, i, i, 0) for i in range(256)) + data

for name, data in (('mc.mbac', mbac_v5()), ('mc_old.mbac', mbac_v3()), ('mc.mtra', mtra()), ('mc.bmp', bmp()),
                   ('mc_sphere.bmp', sphere_bmp())):
    with open(os.path.join(out, name), 'wb') as f: f.write(data)
    print(name, len(data))
