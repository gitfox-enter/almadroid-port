import zipfile, os, subprocess, shutil

os.chdir(r'<PROJECT_ROOT>\work\repack')

# 1. final8.apk 是最新构建（含 bridge smali + extractNativeLibs=true + assets + libs）
src = 'final9.apk'
print('final8 size:', os.path.getsize(src))

# 2. 注入 classes2.dex（io.almadroid.server.*）
DEX2 = r'<PROJECT_ROOT>\almadroid\server-build\classes2.dex'
shutil.copy(DEX2, 'classes2.dex')
src_z = zipfile.ZipFile(src)
with zipfile.ZipFile('merged-v7.apk', 'w', zipfile.ZIP_DEFLATED) as out:
    for item in src_z.infolist():
        out.writestr(item, src_z.read(item.filename))
    with open('classes2.dex', 'rb') as f:
        out.writestr('classes2.dex', f.read())
src_z.close()
print('merged size:', os.path.getsize('merged-v7.apk'))

# 3. zipalign + sign
JAVA = r'C:\Program Files\Microsoft\jdk-25.0.4.7-hotspot\bin\java.exe'
BT = r'<PROJECT_ROOT>\work\sdk-ex\android-14'
KS = r'<PROJECT_ROOT>\almadroid.keystore'

subprocess.run([BT + r'\zipalign.exe', '-f', '-p', '4', 'merged-v7.apk', 'aligned-v7.apk'], check=True)
r = subprocess.run([JAVA, '-jar', BT + r'\lib\apksigner.jar', 'sign',
                    '--ks', KS, '--ks-pass', 'pass:<KS_PASSWORD>', '--key-pass', 'pass:<KS_PASSWORD>',
                    '--out', 'AlmaOne.apk', 'aligned-v7.apk'], capture_output=True, text=True)
print('sign rc:', r.returncode, r.stderr[-300:] if r.stderr else '')

# 4. verify dex presence
z = zipfile.ZipFile('AlmaOne.apk')
print('classes.dex:', z.getinfo('classes.dex').file_size)
print('classes2.dex:', z.getinfo('classes2.dex').file_size)
print('lib:', [n for n in z.namelist() if n.startswith('lib/') and 'proot' in n])
print('assets:', [n for n in z.namelist() if n.startswith('assets/')])