import zipfile, os, subprocess, shutil

os.chdir(r'<PROJECT_ROOT>\work\repack')
J17 = r'<PROJECT_ROOT>\work\jdk17\jdk-17.0.21+9\bin\java.exe'
BT = r'<PROJECT_ROOT>\work\sdk-ex\android-14'
KS = r'<PROJECT_ROOT>\almadroid\keystore.keystore'
if not os.path.exists(KS):
    KS = r'<PROJECT_ROOT>\almadroid.keystore'

# apktool 构建的 final9.apk 结构完好（含 bridge smali + assets + libs）
# 用「追加」模式加 classes2.dex —— 不重写任何现有条目
src = 'final9.apk'
work = 'append-test.apk'
shutil.copy(src, work)

# 先看现有条目
z = zipfile.ZipFile(work)
print('before:', len(z.namelist()), 'entries | dex:', [n for n in z.namelist() if n.endswith('.dex')])
z.close()

# 追加 classes2.dex
dex2 = r'<PROJECT_ROOT>\almadroid\server-build\classes2.dex'
z = zipfile.ZipFile(work, 'a', zipfile.ZIP_STORED)
zi = zipfile.ZipInfo('classes2.dex', (2026, 1, 1, 0, 0, 0))
zi.compress_type = zipfile.ZIP_STORED
z.writestr(zi, open(dex2, 'rb').read())
z.close()

z = zipfile.ZipFile(work)
print('after:', len(z.namelist()), 'entries | dex:', [n for n in z.namelist() if n.endswith('.dex')])
print('zip test:', z.testzip() or 'OK')
z.close()

# 去掉两个大 assets（用 apktool 重建更干净，这里先测试带 assets 的）
# zipalign + sign
subprocess.run([BT + r'\zipalign.exe', '-f', '-p', '4', work, 'append-al.apk'], check=True)
r = subprocess.run([J17, '-jar', BT + r'\lib\apksigner.jar', 'sign', '--ks', KS,
                    '--ks-pass', 'pass:<KS_PASSWORD>', '--key-pass', 'pass:<KS_PASSWORD>',
                    '--v1-signing-enabled', 'true', '--v2-signing-enabled', 'true', '--v3-signing-enabled', 'true',
                    '--out', 'AlmaAppend.apk', 'append-al.apk'], capture_output=True, text=True)
print('sign rc:', r.returncode, r.stderr[-200:] if r.returncode else '')

v = subprocess.run([J17, '-jar', BT + r'\lib\apksigner.jar', 'verify', 'AlmaAppend.apk'],
                   capture_output=True, text=True)
print('verify:', (v.stdout or '').strip()[:50])
print('size:', round(os.path.getsize('AlmaAppend.apk') / 1024 / 1024, 1), 'MB')