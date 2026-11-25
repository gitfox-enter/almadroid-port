import subprocess, os, zipfile, hashlib, base64, shutil

os.chdir(r'<PROJECT_ROOT>\work\repack')
JDK = r'C:\Program Files\Microsoft\jdk-25.0.4.7-hotspot\bin'
JAVA = JDK + r'\java.exe'
JARSIGNER = JDK + r'\jarsigner.exe'
BT = r'<PROJECT_ROOT>\work\sdk-ex\android-14'
KS = r'<PROJECT_ROOT>\almadroid.keystore'
PW = '<KS_PASSWORD>'

src = 'final10.apk'          # 纯净未签名
stepA = 'A-v23.apk'

# 1) 先 zipalign（v2 要求）
subprocess.run([BT + r'\zipalign.exe', '-f', '-p', '4', src, 'A-aligned.apk'], check=True)

# 2) apksigner 生产 v2+v3（此时会写 APK Signing Block）
r = subprocess.run([JAVA, '-jar', BT + r'\lib\apksigner.jar', 'sign',
                    '--ks', KS, '--ks-pass', 'pass:' + PW, '--key-pass', 'pass:' + PW,
                    '--v1-signing-enabled', 'false',
                    '--v2-signing-enabled', 'true',
                    '--v3-signing-enabled', 'true',
                    '--out', stepA, 'A-aligned.apk'], capture_output=True, text=True)
print('v2v3 rc:', r.returncode)

# 3) 手工补 v1：解包→建 MANIFEST.MF→重打→jarsigner（保留 v2/v3 块需要特殊处理）
# 关键：jarsigner 会重写整个 zip，可能丢失 v2/v3 block。
# 所以改为：手工算 v1 文件，再 zipalign 后 apksigner 只加 v2/v3（v1 文件保留在 zip 里）
z = zipfile.ZipFile(stepA)
entries = [(i.filename, i.date_time, i.compress_type, z.read(i.filename)) for i in z.infolist()]
z.close()

# 3a) 生成 MANIFEST.MF
mf = ['Manifest-Version: 1.0\r\nCreated-By: AlmaDroid\r\n\r\n']
for name, _, _, data in entries:
    if name.startswith('META-INF/'):
        continue
    d = base64.b64encode(hashlib.sha256(data).digest()).decode()
    mf.append('Name: %s\r\nSHA-256-Digest: %s\r\n\r\n' % (name, d))
manifest = ''.join(mf).encode('utf-8')

# 3b) 重打包（先包内容，MANIFEST 放最前，符合 JAR 惯例）
out = 'B-v1v2v3.apk'
with zipfile.ZipFile(out, 'w') as o:
    # MANIFEST 放第一个
    zi = zipfile.ZipInfo('META-INF/MANIFEST.MF', (2026, 1, 1, 0, 0, 0))
    zi.compress_type = zipfile.ZIP_DEFLATED
    o.writestr(zi, manifest)
    for name, dt, ct, data in entries:
        if name == 'META-INF/MANIFEST.MF':
            continue
        zi = zipfile.ZipInfo(name, dt)
        zi.compress_type = zipfile.ZIP_STORED if (name.startswith('lib/') or name.endswith('.dex')) else zipfile.ZIP_DEFLATED
        o.writestr(zi, data)
print('B apk:', os.path.getsize(out))

# 3c) jarsigner 生成 .SF/.RSA（保留已有 MANIFEST）
r2 = subprocess.run([JARSIGNER, '-keystore', KS, '-storepass', PW, '-keypass', PW,
                     '-sigalg', 'SHA256withRSA', '-digestalg', 'SHA-256',
                     out, 'almadroid'], capture_output=True, text=True)
print('jarsigner rc:', r2.returncode)

z2 = zipfile.ZipFile(out)
print('sig files:', [n for n in z2.namelist() if n.startswith('META-INF/') and ('MANIFEST' in n or n.endswith(('.SF', '.RSA')))])
z2.close()

# 4) 最后 zipalign + apksigner verify
subprocess.run([BT + r'\zipalign.exe', '-f', '-p', '4', out, 'C-aligned.apk'], check=True)
v = subprocess.run([JAVA, '-jar', BT + r'\lib\apksigner.jar', 'verify', '--verbose', 'C-aligned.apk'],
                   capture_output=True, text=True)
for line in v.stdout.split('\n'):
    if 'Verified using' in line:
        print(line.strip())