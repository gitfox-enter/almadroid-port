# -*- coding: utf-8 -*-
"""极简 RFB 客户端：连 x11vnc 抓一帧原始桌面，存 PNG（无 PIL 依赖，手写 PNG 容器）。"""
import socket, struct, zlib, sys

host, port, out = '127.0.0.1', 15900, sys.argv[1] if len(sys.argv) > 1 else 'grab.png'
s = socket.create_connection((host, port), 8); s.settimeout(10)

def rd(n):
    buf = b''
    while len(buf) < n:
        c = s.recv(n - len(buf))
        if not c: raise EOFError('closed at %d/%d' % (len(buf), n))
        buf += c
    return buf

banner = rd(12)                      # RFB 003.008\n
s.sendall(b'RFB 003.008\n')
sec = rd(1); nsec = sec[0]           # 安全类型数量
types = rd(nsec)
assert 1 in types, 'None security not offered: %s' % types
s.sendall(struct.pack('>I', 1))      # 选 None
sec_result = rd(4)                   # 3.8 有安全结果
assert sec_result == b'\x00\x00\x00\x00', 'sec fail %s' % sec_result
s.sendall(b'\x01')                   # ClientInit: shared
w, h, _bn, name_len = struct.unpack('>HHHH', rd(12))
name = rd(name_len).decode('utf-8', 'replace')
print('server:', w, 'x', h, name)

# SetPixelFormat: 32bpp true-color little-endian (BGRX)
pf = struct.pack('>B3sHHBBBHHHBBB33s', 0, b'\x00'*3, 32, 24, 1, 0, 255, 255, 255, 16, 8, 0, b'\x00'*33)
s.sendall(b'\x00' + pf)
# SetEncodings: Raw
s.sendall(struct.pack('>BxH', 2, 1) + struct.pack('>i', 0))
# FramebufferUpdateRequest incremental=0 全屏
s.sendall(struct.pack('>BBHHH', 3, 0, 0, 0, w, h))

# 读消息直到拿到 FramebufferUpdate
pixels = None
while pixels is None:
    t = rd(1)[0]
    if t == 0:
        rd(1)  # padding
        rects = struct.unpack('>H', rd(2))[0]
        for _ in range(rects):
            x, y, rw, rh = struct.unpack('>HHHH', rd(8))
            enc = struct.unpack('>i', rd(4))[0]
            size = rw * rh * 4
            if enc == 0:
                data = rd(size)
                if pixels is None:
                    pixels = bytearray(b'\x00' * (w * h * 4))
                for row in range(rh):
                    off = ((y + row) * w + x) * 4
                    pixels[off:off + rw*4] = data[row*rw*4:(row+1)*rw*4]
            else:
                # 其他编码：跳过会破坏画面，直接请求 Raw-only 已保证
                raise SystemExit('unexpected encoding %d' % enc)
    elif t == 1:  # SetColourMapEntries
        rd(5); n = struct.unpack('>H', rd(2))[0]; rd(n*6)
    elif t == 2:  # Bell
        pass
    else:
        raise SystemExit('msg type %d' % t)

# BGRX -> RGB 行数据
rows = bytearray()
for y in range(h):
    row = bytearray()
    base = y * w * 4
    for x in range(w):
        o = base + x*4
        row += bytes((pixels[o+2], pixels[o+1], pixels[o]))
    rows += b'\x00' + row

def chunk(tag, data):
    c = struct.pack('>I', len(data)) + tag + data
    return c + struct.pack('>I', zlib.crc32(tag + data) & 0xffffffff)

sig = b'\x89PNG\r\n\x1a\n'
ihdr = chunk(b'IHDR', struct.pack('>IIBBBBB', w, h, 8, 2, 0, 0, 0))
idat = chunk(b'IDAT', zlib.compress(bytes(rows), 6))
png = sig + ihdr + idat + chunk(b'IEND', b'')
open(out, 'wb').write(png)
print('saved', out, len(png), 'bytes')
s.close()
