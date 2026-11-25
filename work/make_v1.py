import os, subprocess, zipfile, hashlib, base64, shutil

os.chdir(r'<PROJECT_ROOT>\work\repack')
JDK = r'C:\Program Files\Microsoft\jdk-25.0.4.7-hotspot\bin'
JAVA = JDK + r'\java.exe'
KEYTOOL = JDK + r'\keytool.exe'
BT = r'<PROJECT_ROOT>\work\sdk-ex\android-14'
KS = r'<PROJECT_ROOT>\almadroid.keystore'
PW = '<KS_PASSWORD>'

base = 'final10.apk'
tmpdir = 'v1work'
shutil.rmtree(tmpdir, ignore_errors=True)
os.makedirs(tmpdir)

# 1) 解出所有条目到临时目录（保留原始顺序记录）
z = zipfile.ZipFile(base)
names = [n for n in z.namelist()]
for n in names:
    p = os.path.join(tmpdir, n.replace('/', os.sep))
    os.makedirs(os.path.dirname(p), exist_ok=True)
    with open(p, 'wb') as f:
        f.write(z.read(n))
z.close()
print('extracted:', len(names), 'entries')

# 2) 生成 MANIFEST.MF（标准 JAR 格式：SHA-256 摘要）
mf = []
mf.append('Manifest-Version: 1.0\r\n')
mf.append('Created-By: AlmaDroid\r\n\r\n')
for n in names:
    if n.startswith('META-INF/') and n.split('/')[-1].upper().endswith(('.SF', '.RSA', '.DSA', '.EC', 'MANIFEST.MF')):
        continue
    p = os.path.join(tmpdir, n.replace('/', os.sep))
    with open(p, 'rb') as f:
        data = f.read()
    digest = base64.b64encode(hashlib.sha256(data).digest()).decode()
    # 行长度限制 72 字节，Name 过长需折行
    mf.append('Name: %s\r\n' % n)
    mf.append('SHA-256-Digest: %s\r\n\r\n' % digest)
manifest_content = ''.join(mf).encode('utf-8')
mfpath = os.path.join(tmpdir, 'META-INF', 'MANIFEST.MF')
os.makedirs(os.path.dirname(mfpath), exist_ok=True)
with open(mfpath, 'wb') as f:
    f.write(manifest_content)
print('MANIFEST.MF:', len(manifest_content), 'bytes')

# 3) 用 jarsigner 对临时目录做 v1 签名（--altsigner 不行，改用直接签 APK）
# 重新打包成临时 APK（保留 STORED for lib/dex）
tmpapk = 'v1-base.apk'
z2 = zipfile.ZipFile(base)
with zipfile.ZipFile(tmpapk, 'w') as o:
    for item in z2.infolist():
        n = item.filename
        data = open(os.path.join(tmpdir, n.replace('/', os.sep)), 'rb').read() if n != 'META-INF/MANIFEST.MF' else manifest_content
        zi = zipfile.ZipInfo(n, item.date_time)
        zi.compress_type = zipfile.ZIP_STORED if (n.startswith('lib/') or n.endswith('.dex')) else zipfile.ZIP_DEFLATED
        o.writestr(zi, data)
    # 补上 MANIFEST
    zi = zipfile.ZipInfo('META-INF/MANIFEST.MF', (2026, 1, 1, 0, 0, 0))
    zi.compress_type = zipfile.ZIP_DEFLATED
    o.writestr(zi, manifest_content)
z2.close()
print('v1-base.apk:', os.path.getsize(tmpapk))

# 4) jarsigner 签这个 APK（会生成 .SF 和 .RSA）
r = subprocess.run([JDK + r'\jarsigner.exe', '-keystore', KS, '-storepass', PW, '-keypass', PW,
                    '-sigalg', 'SHA256withRSA', '-digestalg', 'SHA-256',
                    tmpapk, 'almadroid'], capture_output=True, text=True)
print('jarsigner rc:', r.returncode)
print(r.stdout[-400:] if r.stdout else '')
print(r.stderr[-400:] if r.stderr else '')

z3 = zipfile.ZipFile(tmpapk)
meta = [n for n in z3.namelist() if n.startswith('META-INF/') and ('MANIFEST' in n or n.endswith(('.SF', '.RSA')))]
print('sig files:', meta)