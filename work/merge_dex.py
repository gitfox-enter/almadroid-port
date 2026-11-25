import zipfile, os, shutil

os.chdir(r'<PROJECT_ROOT>\work\repack')
DEX2 = r'<PROJECT_ROOT>\almadroid\server-build\classes2.dex'
src = 'base-v3.apk'
out = 'merged.apk'

shutil.copy(DEX2, 'classes2.dex')
src_z = zipfile.ZipFile(src)
with zipfile.ZipFile(out, 'w', zipfile.ZIP_DEFLATED) as out:
    for item in src_z.infolist():
        out.writestr(item, src_z.read(item.filename))
    with open('classes2.dex', 'rb') as f:
        out.writestr('classes2.dex', f.read())
src_z.close()
os.remove(src)
os.rename(out, src)
print('merged:', os.path.getsize(src))

JAVA = r'C:\Program Files\Microsoft\jdk-25.0.4.7-hotspot\bin\java.exe'
BT = r'<PROJECT_ROOT>\work\sdk-ex\android-14'
KS = r'<PROJECT_ROOT>\almadroid.keystore'

subprocess.run([BT + r'\zipalign.exe', '-f', '-p', '4', src, 'aligned.apk'], check=True)
r = subprocess.run([JAVA, '-jar', BT + r'\lib\apksigner.jar', 'sign',
                    '--ks', KS, '--ks-pass', 'pass:<KS_PASSWORD>', '--key-pass', 'pass:<KS_PASSWORD>',
                    '--out', 'AlmaOne-final.apk', 'aligned.apk'], capture_output=True, text=True)
print('sign rc:', r.returncode)
v = subprocess.run([JAVA, '-jar', BT + r'\lib\apksigner.jar', 'verify', 'AlmaOne-final.apk'],
                   capture_output=True, text=True)
print('verify:', v.stdout.strip() or v.returncode)
print('size:', os.path.getsize('AlmaOne-final.apk'))
