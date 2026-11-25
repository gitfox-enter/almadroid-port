import zipfile, os, subprocess

os.chdir(r'<PROJECT_ROOT>\work\repack')

# 从 final10.apk（我的完整修改，未签名）去掉两个大 assets
src = 'final10.apk'
out = 'slim-my-mod.apk'

z = zipfile.ZipFile(src)
kept = 0
skipped = []
with zipfile.ZipFile(out, 'w') as o:
    for item in z.infolist():
        n = item.filename
        if n in ('assets/rootfs.tar.gz', 'assets/alma-server.tar.gz'):
            skipped.append(n)
            continue
        data = z.read(n)
        zi = zipfile.ZipInfo(n, item.date_time)
        zi.compress_type = zipfile.ZIP_STORED if (n.startswith('lib/') or n.endswith('.dex')) else zipfile.ZIP_DEFLATED
        o.writestr(zi, data)
        kept += 1
z.close()
print('skipped:', skipped)
print('kept entries:', kept)
print('slim size:', round(os.path.getsize(out) / 1024 / 1024, 1), 'MB')

# 检查 lib 目录内容
z2 = zipfile.ZipFile(out)
libs = [n for n in z2.namelist() if n.startswith('lib/')]
print('libs:', libs)
print('dex:', [n for n in z2.namelist() if n.endswith('.dex')])

# 签名
J17 = r'<PROJECT_ROOT>\work\jdk17\jdk-17.0.21+9\bin\java.exe'
BT = r'<PROJECT_ROOT>\work\sdk-ex\android-14'
KS = r'<PROJECT_ROOT>\almadroid.keystore'
subprocess.run([BT + r'\zipalign.exe', '-f', '-p', '4', out, 'slim-al.apk'], check=True)
r = subprocess.run([J17, '-jar', BT + r'\lib\apksigner.jar', 'sign', '--ks', KS,
                    '--ks-pass', 'pass:<KS_PASSWORD>', '--key-pass', 'pass:<KS_PASSWORD>',
                    '--out', 'SlimTest.apk', 'slim-al.apk'], capture_output=True, text=True)
print('sign rc:', r.returncode)
print('final:', round(os.path.getsize('SlimTest.apk') / 1024 / 1024, 1), 'MB')