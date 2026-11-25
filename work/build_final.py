import zipfile, os, subprocess, shutil

os.chdir(r'<PROJECT_ROOT>\work\repack')
src = 'final9.apk'
merged_dex = 'dexwork/merged/classes.dex'
out = 'final10.apk'

# 用单 dex 替换 APK（不产生 classes2）
src_z = zipfile.ZipFile(src)
with zipfile.ZipFile(out, 'w', zipfile.ZIP_STORED) as o:
    for item in src_z.infolist():
        data = src_z.read(item.filename)
        if item.filename == 'classes.dex':
            data = open(merged_dex, 'rb').read()
        # native libs 必须 STORED（不需解压即可 mmap），其他也可 DEFLATED
        if item.filename.startswith('lib/') or item.filename == 'classes.dex':
            zi = zipfile.ZipInfo(item.filename, item.date_time)
            zi.compress_type = zipfile.ZIP_STORED
            o.writestr(zi, data)
        else:
            zi = zipfile.ZipInfo(item.filename, item.date_time)
            zi.compress_type = zipfile.ZIP_DEFLATED
            o.writestr(zi, data)
src_z.close()

# 验证 zip
z = zipfile.ZipFile(out)
print('zip test:', z.testzip() or 'OK')
print('entries:', len(z.namelist()), '| dex:', z.getinfo('classes.dex').file_size)

# zipalign + sign
JAVA = r'C:\Program Files\Microsoft\jdk-25.0.4.7-hotspot\bin\java.exe'
BT = r'<PROJECT_ROOT>\work\sdk-ex\android-14'
KS = r'<PROJECT_ROOT>\almadroid.keystore'
subprocess.run([BT + r'\zipalign.exe', '-f', '-p', '4', out, 'al10.apk'], check=True)
r = subprocess.run([JAVA, '-jar', BT + r'\lib\apksigner.jar', 'sign',
                    '--ks', KS, '--ks-pass', 'pass:<KS_PASSWORD>', '--key-pass', 'pass:<KS_PASSWORD>',
                    '--out', 'AlmaFinal.apk', 'al10.apk'], capture_output=True, text=True)
print('sign rc:', r.returncode)
z2 = zipfile.ZipFile('AlmaFinal.apk')
print('final zip test:', z2.testzip() or 'OK')
print('final size:', os.path.getsize('AlmaFinal.apk'))