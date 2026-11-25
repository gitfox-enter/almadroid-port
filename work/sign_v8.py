import subprocess, os

os.chdir(r'<PROJECT_ROOT>\work\repack')
JAVA = r'C:\Program Files\Microsoft\jdk-25.0.4.7-hotspot\bin\java.exe'
BT = r'<PROJECT_ROOT>\work\sdk-ex\android-14'
KS = r'<PROJECT_ROOT>\almadroid.keystore'

subprocess.run([BT + r'\zipalign.exe', '-f', '-p', '4', 'merged.apk', 'aligned-final.apk'], check=True)
r = subprocess.run([JAVA, '-jar', BT + r'\lib\apksigner.jar', 'sign',
                    '--ks', KS, '--ks-pass', 'pass:<KS_PASSWORD>', '--key-pass', 'pass:<KS_PASSWORD>',
                    '--out', 'AlmaLocal-v8.apk', 'aligned-final.apk'], capture_output=True, text=True)
print('sign rc:', r.returncode, r.stderr[-200:] if r.stderr else '')
v = subprocess.run([JAVA, '-jar', BT + r'\lib\apksigner.jar', 'verify', 'AlmaLocal-v8.apk'],
                   capture_output=True, text=True)
print('verify:', v.stdout.strip()[:60])
import zipfile
z = zipfile.ZipFile('AlmaLocal-v8.apk')
print('dex:', [n for n in z.namelist() if n.startswith('classes')])
print('size:', os.path.getsize('AlmaLocal-v8.apk'))