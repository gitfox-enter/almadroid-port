import subprocess, os

os.chdir(r'<PROJECT_ROOT>\work\repack')
J17 = r'<PROJECT_ROOT>\work\jdk17\jdk-17.0.21+9\bin\java.exe'
BT = r'<PROJECT_ROOT>\work\sdk-ex\android-14'
KS = r'<PROJECT_ROOT>\almadroid.keystore'
PW = '<KS_PASSWORD>'

# 用未修改官方 APK 做对照：JDK17 能否正确签 v1+v2
subprocess.run([BT + r'\zipalign.exe', '-f', '-p', '4',
                r'<PROJECT_ROOT>\work\Alma-4.9.apk', 'ctrl-aligned.apk'], check=True)

r = subprocess.run([J17, '-jar', BT + r'\lib\apksigner.jar', 'sign',
                    '--ks', KS, '--ks-pass', 'pass:' + PW, '--key-pass', 'pass:' + PW,
                    '--v1-signing-enabled', 'true',
                    '--v2-signing-enabled', 'true',
                    '--v3-signing-enabled', 'true',
                    '--out', 'ctrl-jdk17.apk', 'ctrl-aligned.apk'],
                   capture_output=True, text=True)
print('sign rc:', r.returncode, r.stderr[-300:] if r.returncode else '')

v = subprocess.run([J17, '-jar', BT + r'\lib\apksigner.jar', 'verify', '--verbose', 'ctrl-jdk17.apk'],
                   capture_output=True, text=True)
for line in v.stdout.split('\n'):
    if 'Verified using' in line:
        print(line.strip())
print('size:', os.path.getsize('ctrl-jdk17.apk'))