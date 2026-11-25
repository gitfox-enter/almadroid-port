import os, shutil

WORK = r'<PROJECT_ROOT>\work\runtime'
OUT = r'<PROJECT_ROOT>\almadroid\app\jniLibs\arm64-v8a'
os.makedirs(OUT, exist_ok=True)

proot = open(os.path.join(WORK, 'proot/data/data/com.termux/files/usr/bin/proot'), 'rb').read()

# patch DT_NEEDED names
p1 = b'libtalloc.so.2\x00'
r1 = b'libtalloc.so\x00\x00\x00'
assert proot.count(p1) >= 1, 'libtalloc string not found'
proot = proot.replace(p1, r1)
p2 = b'libandroid-shmem.so\x00'
r2 = b'libandroid_shmem.so\x00'
assert proot.count(p2) >= 1, 'shmem string not found'
proot = proot.replace(p2, r2)
open(os.path.join(OUT, 'libproot.so'), 'wb').write(proot)

shutil.copy(os.path.join(WORK, 'proot/data/data/com.termux/files/usr/libexec/proot/loader'),
            os.path.join(OUT, 'libproot_loader.so'))
shutil.copy(os.path.join(WORK, 'libtalloc/data/data/com.termux/files/usr/lib/libtalloc.so.2'),
            os.path.join(OUT, 'libtalloc.so'))
shutil.copy(os.path.join(WORK, 'libshmem/data/data/com.termux/files/usr/lib/libandroid-shmem.so'),
            os.path.join(OUT, 'libandroid_shmem.so'))

for f in os.listdir(OUT):
    print(f, os.path.getsize(os.path.join(OUT, f)))
