p = r'<PROJECT_ROOT>\almadroid\build.sh'
lines = open(p, encoding='utf-8').read().split('\n')
out = []
for ln in lines:
    if 'JSCH_JAR' in ln:
        continue
    out.append(ln)
open(p, 'w', encoding='utf-8').write('\n'.join(out))
print('clean done')