import struct, os, sys, lzma, zlib, tarfile, io

def read_ar(path):
    data = open(path, 'rb').read()
    assert data[:8] == b'!<arch>\n', path
    off = 8
    members = {}
    while off < len(data):
        hdr = data[off:off+60]
        if len(hdr) < 60: break
        name = hdr[0:16].decode('ascii').strip()
        size = int(hdr[48:58].decode().strip())
        body = data[off+60:off+60+size]
        members[name] = body
        off += 60 + size + (size % 2)
    return members

def extract_deb(deb, outdir):
    m = read_ar(deb)
    # find data tar (xz or zst or gz)
    for name, body in m.items():
        if name.startswith('data.tar'):
            if name.endswith('.xz'):
                tar_data = lzma.decompress(body)
            elif name.endswith('.gz'):
                tar_data = zlib.decompress(body, 16+zlib.MAX_WBITS)
            elif name.endswith('.zst'):
                raise SystemExit('zstd not supported for ' + deb)
            else:
                tar_data = body
            tf = tarfile.open(fileobj=io.BytesIO(tar_data))
            os.makedirs(outdir, exist_ok=True)
            tf.extractall(outdir)
            return name
    raise SystemExit('no data.tar in ' + deb)

if __name__ == '__main__':
    import sys
    for arg in sys.argv[1:]:
        deb, outdir = arg.split('=')
        extract_deb(deb, outdir)
        print('extracted', deb)
    print('done')
    for d in ['proot', 'libtalloc', 'libshmem']:
        for root, dirs, files in os.walk(d):
            for f in files:
                p = os.path.join(root, f)
                print(p, os.path.getsize(p))
