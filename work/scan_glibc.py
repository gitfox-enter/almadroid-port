import tarfile, re

t = tarfile.open(r'<PROJECT_ROOT>\work\alma-server.tar.gz')
m = t.getmember('alma-server-0.4.713-linux-arm64/runtime/node')
f = t.extractfile(m)
vs = set()
while True:
    data = f.read(16 * 1024 * 1024)
    if not data:
        break
    vs.update(int(x) for x in re.findall(rb'GLIBC_2\.(\d+)', data))
print('node GLIBC max required:', max(vs))

targets = []
for name in t.getnames():
    if name.endswith('.node') and ('arm64' in name or 'Release' in name):
        mm = t.getmember(name)
        if mm.size <= 30_000_000:
            targets.append(name)
for name in targets:
    ff = t.extractfile(t.getmember(name))
    vs2 = set()
    while True:
        d = ff.read(16 * 1024 * 1024)
        if not d:
            break
        vs2.update(int(x) for x in re.findall(rb'GLIBC_2\.(\d+)', d))
    if vs2:
        print(name.split('node_modules/')[-1], '-> GLIBC', max(vs2))
