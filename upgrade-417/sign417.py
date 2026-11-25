import subprocess, os
os.chdir(r'<PROJECT_ROOT>\upgrade-417')
J17=r'<PROJECT_ROOT>\work\jdk17\jdk-17.0.21+9\bin\java.exe'
BT=r'<PROJECT_ROOT>\work\sdk-ex\android-14'
KS=r'<PROJECT_ROOT>\almadroid.keystore'
PW = '<KS_PASSWORD>'
r=subprocess.run([J17,'-jar',BT+r'\lib\apksigner.jar','sign','--ks',KS,
                 '--ks-pass','pass:'+PW,'--key-pass','pass:'+PW,
                 '--out','AlmaX-v68.apk','417-al.apk'],capture_output=True,text=True)
print('sign rc:',r.returncode, r.stderr[-300:] if r.returncode else '')
if r.returncode==0:
    v=subprocess.run([J17,'-jar',BT+r'\lib\apksigner.jar','verify','AlmaX-v68.apk'],capture_output=True,text=True)
    print('verify:',(v.stdout or '').strip()[:30] or 'stderr:'+v.stderr[:100])
    print('size:',round(os.path.getsize('AlmaX-v68.apk')/1024/1024,1),'MB')
