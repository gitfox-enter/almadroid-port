import zipfile, os, subprocess

os.chdir(r'<PROJECT_ROOT>\work\repack')

# 1) 从 final10.apk 移除所有旧 META-INF 签名文件
src = 'final10.apk'
clean = 'final10-clean.apk'
z = zipfile.ZipFile(src)
removed = []
with zipfile.ZipFile(clean, 'w', zipfile.ZIP_STORED) as o:
    for item in z.infolist():
        n = item.filename
        if n.startswith('META-INF/') and (
                n.endswith(('.SF', '.RSA', '.DSA', '.EC')) or n == 'META-INF/MANIFEST.MF'):
            removed.append(n)
            continue
        data = z.read(n)
        zi = zipfile.ZipInfo(n, item.date_time)
        # native libs 和 dex 用 STORED
        zi.compress_type = zipfile.ZIP_STORED if (n.startswith('lib/') or n.endswith('.dex')) else zipfile.ZIP_DEFLATED
        o.writestr(zi, data)
z.close()
print('removed old sigs:', removed)
print('clean size:', os.path.getsize(clean))

# 2) zipalign
JAVA = r'C:\Program Files\Microsoft\jdk-25.0.4.7-hotspot\bin\java.exe'
BT = r'<PROJECT_ROOT>\work\sdk-ex\android-14'
KS = r'<PROJECT_ROOT>\almadroid.keystore'
subprocess.run([BT + r'\zipalign.exe', '-f', '-p', '4', clean, 'aligned-clean.apk'], check=True)

# 3) v1+v2+v3 全签
cmd = [JAVA, '-jar', BT + r'\lib\apksigner.jar', 'sign', '--ks', KS,
       '--ks-pass', 'pass:<KS_PASSWORD>', '--key-pass', 'pass:<KS_PASSWORD>',
       '--v1-signing-enabled', 'true', '--v2-signing-enabled', 'true', '--v3-signing-enabled', 'true',
       '--out', 'Alma.v1v2v3.apk', 'aligned-clean.apk']
r = subprocess.run(cmd, capture_output=True, text=True)
print('sign rc:', r.returncode, r.stderr[-300:] if r.returncode else '')

# 4) verify
v = subprocess.run([JAVA, '-jar', BT + r'\lib\apksigner.jar', 'verify', '--verbose', 'Alma.v1v2v3.apk'],
                   capture_output=True, text=True)
for line in v.stdout.split('\n'):
    if 'Verified using' in line:
        print(line.strip())
print('size:', os.path.getsize('Alma.v1v2v3.apk'))