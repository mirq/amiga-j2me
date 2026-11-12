#!/usr/bin/env python3
"""Writes tests/midlets/res/scene.m3g: a small M3G 1.0 file for the Loader test.

A world with a camera, an ambient and a directional light, a background and a
textured cube that an animation track spins. The scene section is zlib
compressed, as exporters write it."""
import struct, zlib, math, sys

objs = []           # (type, data); index of objs[i] = i + 2 (the header is object 1)
def add(t, data):
    objs.append((t, data)); return len(objs) + 1
u8 = lambda v: struct.pack('<B', v & 255)
u16 = lambda v: struct.pack('<H', v & 0xFFFF)
u32 = lambda v: struct.pack('<I', v & 0xFFFFFFFF)
i32 = lambda v: struct.pack('<i', v)
f32 = lambda v: struct.pack('<f', v)
def obj3d(tracks=()): return u32(0) + u32(len(tracks)) + b''.join(u32(t) for t in tracks) + u32(0)
def transformable(tracks=(), translation=None):
    d = obj3d(tracks)
    if translation: d += u8(1) + b''.join(f32(v) for v in translation) + f32(1)*3 + f32(0) + f32(0)*3
    else: d += u8(0)
    return d + u8(0)
def node(tracks=(), translation=None): return transformable(tracks, translation) + u8(1) + u8(1) + u8(255) + i32(-1) + u8(0)

POS = [1,1,1,-1,1,1,1,-1,1,-1,-1,1, -1,1,-1,1,1,-1,-1,-1,-1,1,-1,-1, -1,1,1,-1,1,-1,-1,-1,1,-1,-1,-1,
       1,1,-1,1,1,1,1,-1,-1,1,-1,1, 1,1,-1,-1,1,-1,1,1,1,-1,1,1, 1,-1,1,-1,-1,1,1,-1,-1,-1,-1,-1]
NRM = sum(([x*127, y*127, z*127]*4 for x,y,z in [(0,0,1),(0,0,-1),(-1,0,0),(1,0,0),(0,1,0),(0,-1,0)]), [])
TEX = [1,0,0,0,1,1,0,1]*6

pos = add(20, obj3d() + u8(2) + u8(3) + u8(0) + u16(24) + b''.join(struct.pack('<h', v) for v in POS))
nrm = add(20, obj3d() + u8(1) + u8(3) + u8(0) + u16(24) + b''.join(struct.pack('<b', v) for v in NRM))
tex = add(20, obj3d() + u8(2) + u8(2) + u8(0) + u16(24) + b''.join(struct.pack('<h', v) for v in TEX))
vb = add(21, obj3d() + bytes([255,255,255,255]) + u32(pos) + f32(0)*3 + f32(1) + u32(nrm) + u32(0)
         + u32(1) + u32(tex) + f32(0)*3 + f32(1))
ib = add(11, obj3d() + u8(0) + u32(0) + u32(6) + u32(4)*6)
pix = bytearray()
for y in range(16):
    for x in range(16):
        on = ((x >> 2) + (y >> 2)) % 2 == 0
        pix += bytes([230, 60, 60]) if on else bytes([250, 250, 250])
img = add(10, obj3d() + u8(99) + u8(0) + u32(16) + u32(16) + u32(0) + u32(len(pix)) + bytes(pix))
tx = add(17, transformable() + u32(img) + bytes([0,0,0]) + u8(227) + u8(240) + u8(240) + u8(208) + u8(210))
mat = add(13, obj3d() + bytes([90,90,90]) + bytes([255,255,255,255]) + bytes([0,0,0]) + bytes([0,0,0]) + f32(0) + u8(0))
pm = add(8, obj3d() + u8(160) + u8(165) + u8(168) + u8(0) + u8(0) + u8(1))
ap = add(3, obj3d() + u8(0) + u32(0) + u32(0) + u32(pm) + u32(mat) + u32(1) + u32(tx))
# animation: a full turn about Y in 4 s, looping (SLERP between quaternions)
keys = b''
for k in range(4):
    a = k * math.pi / 2 / 2      # half the angle of k * 90 degrees
    keys += u32(k * 1000) + f32(0) + f32(math.sin(a)) + f32(0) + f32(math.cos(a))
seq = add(19, obj3d() + u8(177) + u8(193) + u8(0) + u32(4000) + u32(0) + u32(3) + u32(4) + u32(4) + keys)
ctl = add(1, obj3d() + f32(1) + f32(1) + i32(0) + i32(0) + f32(0) + i32(0))
trk = add(2, obj3d() + u32(seq) + u32(ctl) + u32(268))
mesh = add(14, node(tracks=(trk,)) + u32(vb) + u32(1) + u32(ib) + u32(ap))
cam = add(5, node(translation=(0, 1.5, 6)) + u8(50) + f32(50) + f32(1) + f32(0.5) + f32(50))
amb = add(12, node() + f32(1) + f32(0) + f32(0) + bytes([255,255,255]) + u8(128) + f32(0.35) + f32(45) + f32(0))
dirl = add(12, node() + f32(1) + f32(0) + f32(0) + bytes([255,255,255]) + u8(129) + f32(1.0) + f32(45) + f32(0))
bg = add(4, obj3d() + bytes([20, 60, 40, 255]) + u32(0) + u8(32) + u8(32) + i32(0)*4 + u8(1) + u8(1))
world = add(22, node() + u32(4) + u32(mesh) + u32(cam) + u32(amb) + u32(dirl) + u32(cam) + u32(bg))

def section(objects, compress):
    body = b''.join(u8(t) + u32(len(d)) + d for t, d in objects)
    payload = zlib.compress(body, 9) if compress else body
    sec = u8(1 if compress else 0) + u32(len(payload) + 13) + u32(len(body)) + payload
    return sec + u32(zlib.adler32(sec))

scene = section(objs, True)
ident = bytes([0xAB]) + b'JSR184' + bytes([0xBB, 0x0D, 0x0A, 0x1A, 0x0A])
def header(total): return section([(0, bytes([1, 0]) + u8(0) + u32(total) + u32(total) + b'amiga-j2me test\x00')], False)
total = len(ident) + len(header(0)) + len(scene)
out = sys.argv[1] if len(sys.argv) > 1 else 'tests/midlets/res/scene.m3g'
open(out, 'wb').write(ident + header(total) + scene)
print('wrote', out, total, 'bytes,', len(objs) + 1, 'objects')
