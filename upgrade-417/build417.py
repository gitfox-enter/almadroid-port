import zipfile, os, subprocess, shutil

os.chdir(r'<PROJECT_ROOT>\upgrade-417')
J17 = r'<PROJECT_ROOT>\work\jdk17\jdk-17.0.21+9\bin\java.exe'
BT = r'<PROJECT_ROOT>\work\sdk-ex\android-14'
KS = r'<PROJECT_ROOT>\almadroid.keystore'
PW = '<KS_PASSWORD>'

subprocess.run(['java','-jar',r'<TOOLS>\apktool\apktool_3.0.3.jar',
                'b','-f','alma-417','-o','base417.apk'], check=True)
shutil.copy('base417.apk','417.apk')
z=zipfile.ZipFile('417.apk','a',zipfile.ZIP_STORED)
zi=zipfile.ZipInfo('classes2.dex',(2026,1,1,0,0,0)); zi.compress_type=zipfile.ZIP_STORED
z.writestr(zi, open(r'<PROJECT_ROOT>\almadroid\server-build\classes2.dex','rb').read())
z.close()
z=zipfile.ZipFile('417.apk')
print('dex:',[n for n in z.namelist() if n.endswith('.dex')],'| test:',z.testzip() or 'OK'); z.close()
subprocess.run([BT+'\zipalign.exe','-f','-p','4','417.apk','417-al.apk'],check=True)
r=subprocess.run([J17,'-jar',BT+'\lib\apksigner.jar','sign','--ks',KS,
                '--ks-pass','pass:'+PW,'--key-pass','pass:'+PW,
                '--out','AlmaX.apk','417-al.apk'],capture_output=True,text=True)
print('sign rc:',r.returncode)
v=subprocess.run([J17,'-jar',BT+'\lib\apksigner.jar','verify','AlmaX.apk'],capture_output=True,text=True)
print('verify:',(v.stdout or '').strip()[:40] or 'see stderr')
print('size:', round(os.path.getsize('AlmaX.apk')/1024/1024,1),'MB')
