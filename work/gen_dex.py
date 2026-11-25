import os, subprocess

os.chdir(r'<PROJECT_ROOT>\almadroid\server-build')
JAVA = r'C:\Program Files\Microsoft\jdk-25.0.4.7-hotspot\bin\java.exe'
D8 = r'<PROJECT_ROOT>\work\runtime\sdk\bt35\android-VanillaIceCream\lib\d8.jar'

classes = []
for root, dirs, files in os.walk('classes'):
    for f in files:
        if f.endswith('.class'):
            classes.append(os.path.abspath(os.path.join(root, f)))

cmd = [JAVA, '-cp', D8, 'com.android.tools.r8.D8', '--release', '--min-api', '26',
       '--output', '.'] + classes
r = subprocess.run(cmd, capture_output=True, text=True)
print('rc:', r.returncode)
print(r.stdout[-2000:] if r.stdout else '')
print(r.stderr[-2000:] if r.stderr else '')
print('dex exists:', os.path.exists('classes2.dex'))