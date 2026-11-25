import subprocess, os

os.chdir(r'<PROJECT_ROOT>\work\repack')
JDK = r'C:\Program Files\Microsoft\jdk-25.0.4.7-hotspot\bin'
JAVA = JDK + r'\java.exe'
BT = r'<PROJECT_ROOT>\work\sdk-ex\android-14'
KS = r'<PROJECT_ROOT>\almadroid.keystore'
PW = '<KS_PASSWORD>'

# v1-base.apk 已含 v1 签名；zipalign 后再补 v2/v3
subprocess.run([BT + r'\zipalign.exe', '-f', '-p', '4', 'v1-base.apk', 'v1-aligned.apk'], check=True)

cmd = [JAVA, '-jar', BT + r'\lib\apksigner.jar', 'sign',
       '--ks', KS, '--ks-pass', 'pass:' + PW, '--key-pass', 'pass:' + PW,
       '--v1-signing-enabled', 'false',
       '--v2-signing-enabled', 'true',
       '--v3-signing-enabled', 'true',
       '--out', 'AlmaReady.apk', 'v1-aligned.apk']
r = subprocess.run(cmd, capture_output=True, text=True)
print('apksigner rc:', r.returncode, r.stderr[-300:] if r.returncode else '')

v = subprocess.run([JAVA, '-jar', BT + r'\lib\apksigner.jar', 'verify', '--verbose', 'AlmaReady.apk'],
                   capture_output=True, text=True)
for line in v.stdout.split('\n'):
    if 'Verified using' in line:
        print(line.strip())
print('size:', os.path.getsize('AlmaReady.apk'))

import zipfile
z = zipfile.ZipFile('AlmaReady.apk')
print('zip:', z.testzip() or 'OK')
print('sigs:', [n for n in z.namelist() if n.startswith('META-INF/') and ('MANIFEST' in n or n.endswith(('.SF', '.RSA')))])