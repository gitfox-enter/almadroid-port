import subprocess, os, zipfile, shutil

os.chdir(r'<PROJECT_ROOT>\work\repack')
JDK = r'C:\Program Files\Microsoft\jdk-25.0.4.7-hotspot\bin'
JAVA = JDK + r'\java.exe'
JARSIGNER = JDK + r'\jarsigner.exe'
BT = r'<PROJECT_ROOT>\work\sdk-ex\android-14'
KS = r'<PROJECT_ROOT>\almadroid.keystore'
PW = '<KS_PASSWORD>'

base = 'final10.apk'   # 无签名的原始构建
step1 = 'step1-jar.apk'

# 1) jarsigner 做 v1（JAR signing）
shutil.copy(base, step1)
r = subprocess.run([JARSIGNER, '-keystore', KS, '-storepass', PW, '-keypass', PW,
                    '-sigalg', 'SHA256withRSA', '-digestalg', 'SHA-256',
                    step1, 'almadroid'], capture_output=True, text=True)
print('jarsigner rc:', r.returncode)
if r.returncode != 0:
    print(r.stdout[-600:], r.stderr[-600:])

# 2) zipalign
subprocess.run([BT + r'\zipalign.exe', '-f', '-p', '4', step1, 'step2-aligned.apk'], check=True)

# 3) apksigner 补 v2/v3（保留已有 v1）
cmd = [JAVA, '-jar', BT + r'\lib\apksigner.jar', 'sign',
       '--ks', KS, '--ks-pass', 'pass:' + PW, '--key-pass', 'pass:' + PW,
       '--v1-signing-enabled', 'false',   # 已有 v1，不重做
       '--v2-signing-enabled', 'true', '--v3-signing-enabled', 'true',
       '--out', 'AlmaAllSchemes.apk', 'step2-aligned.apk']
r2 = subprocess.run(cmd, capture_output=True, text=True)
print('apksigner rc:', r2.returncode)

# 4) verify
v = subprocess.run([JAVA, '-jar', BT + r'\lib\apksigner.jar', 'verify', '--verbose', 'AlmaAllSchemes.apk'],
                   capture_output=True, text=True)
for line in v.stdout.split('\n'):
    if 'Verified using' in line:
        print(line.strip())
print('size:', os.path.getsize('AlmaAllSchemes.apk'))