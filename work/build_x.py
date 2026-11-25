import zipfile, os, subprocess, shutil

os.chdir(r'<PROJECT_ROOT>\work\repack')
J17 = r'<PROJECT_ROOT>\work\jdk17\jdk-17.0.21+9\bin\java.exe'
BT = r'<PROJECT_ROOT>\work\sdk-ex\android-14'
KS = r'<PROJECT_ROOT>\almadroid.keystore'
PW = '<KS_PASSWORD>'

# 1) 用 apktool 重建（保持结构），含 assets+libs
subprocess.run(['java', '-jar', r'<TOOLS>\apktool\apktool_3.0.3.jar',
                'b', '-f', 'alma-final', '-o', 'baseX.apk'], check=True)

# 2) 追加模式加 classes2.dex（不重写已有条目 = 不破坏结构）
shutil.copy('baseX.apk', 'X.apk')
z = zipfile.ZipFile('X.apk', 'a', zipfile.ZIP_STORED)
zi = zipfile.ZipInfo('classes2.dex', (2026, 1, 1, 0, 0, 0))
zi.compress_type = zipfile.ZIP_STORED
z.writestr(zi, open(r'<PROJECT_ROOT>\almadroid\server-build\classes2.dex', 'rb').read())
z.close()

z = zipfile.ZipFile('X.apk')
print('dex:', [n for n in z.namelist() if n.endswith('.dex')], '| test:', z.testzip() or 'OK')
z.close()

# 3) align + sign
subprocess.run([BT + r'\zipalign.exe', '-f', '-p', '4', 'X.apk', 'X-al.apk'], check=True)
r = subprocess.run([J17, '-jar', BT + r'\lib\apksigner.jar', 'sign', '--ks', KS,
                    '--ks-pass', 'pass:' + PW, '--key-pass', 'pass:' + PW,
                    '--out', 'AlmaX.apk', 'X-al.apk'], capture_output=True, text=True)
print('sign rc:', r.returncode)
v = subprocess.run([J17, '-jar', BT + r'\lib\apksigner.jar', 'verify', 'AlmaX.apk'],
                   capture_output=True, text=True)
print('verify:', (v.stdout or '').strip()[:40] or 'see stderr')
print('size:', round(os.path.getsize('AlmaX.apk') / 1024 / 1024, 1), 'MB')