import zipfile, os, subprocess, shutil

os.chdir(r'<PROJECT_ROOT>\work\repack')
J17 = r'<PROJECT_ROOT>\work\jdk17\jdk-17.0.21+9\bin\java.exe'
BT = r'<PROJECT_ROOT>\work\sdk-ex\android-14'
KS = r'<PROJECT_ROOT>\almadroid.keystore'

def rebuild(src, out_apk, drop_libs=(), subtitle=''):
    z = zipfile.ZipFile(src)
    with zipfile.ZipFile(out_apk, 'w') as o:
        for item in z.infolist():
            n = item.filename
            if n in drop_libs:
                continue
            data = z.read(n)
            zi = zipfile.ZipInfo(n, item.date_time)
            zi.compress_type = zipfile.ZIP_STORED if (n.startswith('lib/') or n.endswith('.dex')) else zipfile.ZIP_DEFLATED
            o.writestr(zi, data)
    z.close()
    # align + sign
    al = out_apk.replace('.apk', '-al.apk')
    subprocess.run([BT + r'\zipalign.exe', '-f', '-p', '4', out_apk, al], check=True)
    r = subprocess.run([J17, '-jar', BT + r'\lib\apksigner.jar', 'sign', '--ks', KS,
                        '--ks-pass', 'pass:<KS_PASSWORD>', '--key-pass', 'pass:<KS_PASSWORD>',
                        '--out', out_apk, al], capture_output=True, text=True)
    print(subtitle or out_apk, '| rc:', r.returncode,
          '| size:', round(os.path.getsize(out_apk) / 1024 / 1024, 1), 'MB')

# 对照 A：我的改动 + 去掉我加的 3 个 proot 库
rebuild('SlimTest.apk', 'T-no-proot.apk',
        drop_libs=('lib/arm64-v8a/libproot.so',
                   'lib/arm64-v8a/libproot_loader.so',
                   'lib/arm64-v8a/libtalloc.so',
                   'lib/arm64-v8a/libandroid_shmem.so'),
        subtitle='A: 去掉 proot 库')

# 对照 B：官方原包 + 我的 4 个 proot 库（验证库本身是否被拒）
z = zipfile.ZipFile(r'<PROJECT_ROOT>\work\Alma-4.9.apk')
with zipfile.ZipFile('B-official-plus-libs.apk', 'w') as o:
    for item in z.infolist():
        data = z.read(item.filename)
        zi = zipfile.ZipInfo(item.filename, item.date_time)
        zi.compress_type = zipfile.ZIP_STORED if (item.filename.startswith('lib/') or item.filename.endswith('.dex')) else zipfile.ZIP_DEFLATED
        o.writestr(zi, data)
    for lib in ['libproot.so', 'libproot_loader.so', 'libtalloc.so', 'libandroid_shmem.so']:
        p = rf'<PROJECT_ROOT>\almadroid\app\jniLibs\arm64-v8a\{lib}'
        zi = zipfile.ZipInfo('lib/arm64-v8a/' + lib, (2026, 1, 1, 0, 0, 0))
        zi.compress_type = zipfile.ZIP_STORED
        o.writestr(zi, open(p, 'rb').read())
z.close()
subprocess.run([BT + r'\zipalign.exe', '-f', '-p', '4', 'B-official-plus-libs.apk', 'B-al.apk'], check=True)
r = subprocess.run([J17, '-jar', BT + r'\lib\apksigner.jar', 'sign', '--ks', KS,
                    '--ks-pass', 'pass:<KS_PASSWORD>', '--key-pass', 'pass:<KS_PASSWORD>',
                    '--out', 'B-official-plus-libs.apk', 'B-al.apk'], capture_output=True, text=True)
print('B: 官方包+我的proot库 | rc:', r.returncode,
      '| size:', round(os.path.getsize('B-official-plus-libs.apk') / 1024 / 1024, 1), 'MB')