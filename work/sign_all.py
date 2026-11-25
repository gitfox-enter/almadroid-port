import subprocess, os, zipfile

os.chdir(r'<PROJECT_ROOT>\work\repack')
JAVA = r'C:\Program Files\Microsoft\jdk-25.0.4.7-hotspot\bin\java.exe'
BT = r'<PROJECT_ROOT>\work\sdk-ex\android-14'
KS = r'<PROJECT_ROOT>\almadroid.keystore'
ALIGNED = 'al10.apk'

# 1) 先 zipalign（v2 签名要求对齐后再签）
subprocess.run([BT + r'\zipalign.exe', '-f', '-p', '4', 'final10.apk', ALIGNED], check=True)

# 2) 用 v1 + v2 + v3 全开签名（小米兼容性最好）
cmd = [JAVA, '-jar', BT + r'\lib\apksigner.jar', 'sign',
       '--ks', KS,
       '--ks-pass', 'pass:<KS_PASSWORD>',
       '--key-pass', 'pass:<KS_PASSWORD>',
       '--v1-signing-enabled', 'true',
       '--v2-signing-enabled', 'true',
       '--v3-signing-enabled', 'true',
       '--out', 'AlmaSigned.apk', ALIGNED]
r = subprocess.run(cmd, capture_output=True, text=True)
print('sign rc:', r.returncode)
if r.stderr:
    print(r.stderr[-500:])

# 3) 校验各版本签名
v = subprocess.run([JAVA, '-jar', BT + r'\lib\apksigner.jar', 'verify', '--verbose', 'AlmaSigned.apk'],
                   capture_output=True, text=True)
out = v.stdout
for line in out.split('\n'):
    if 'Verified using' in line or 'scheme' in line.lower():
        print(line.strip())

z = zipfile.ZipFile('AlmaSigned.apk')
print('zip:', z.testzip() or 'OK', '| size:', os.path.getsize('AlmaSigned.apk'))
print('META-INF:', [n for n in z.namelist() if n.startswith('META-INF') and n.endswith(('.SF', '.RSA', '.MF'))])