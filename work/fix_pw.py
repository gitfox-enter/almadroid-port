import re
p = r'<PROJECT_ROOT>\almadroid\build.sh'
s = open(p, encoding='utf-8').read()
lines = s.split('\n')
out = []
KS = '  --ks-pass env:ALMA_KS_PW \\'
KP = '  --key-pass env:ALMA_KS_PW \\'
for ln in lines:
    if '--ks-pass' in ln and '{{' in ln:
        ln = KS
    elif '--key-pass' in ln and '{{' in ln:
        ln = KP
    out.append(ln)
s = '\n'.join(out)
if 'ALMA_KS_PW=' not in s.split('--ks-pass')[0]:
    s = s.replace('JAVA_HOME_DIR="/c/Program Files/Microsoft/jdk-25.0.4.7-hotspot"',
                  'JAVA_HOME_DIR="/c/Program Files/Microsoft/jdk-25.0.4.7-hotspot"\nALMA_KS_PW=<KS_PASSWORD>')
open(p, 'w', encoding='utf-8').write(s)
print('env-based password wired')
