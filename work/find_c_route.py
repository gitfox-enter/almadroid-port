import re

data = open(r'<PROJECT_ROOT>\work\alma-server-0.4.713-linux-arm64\lib\server.mjs', 'rb').read()
joined = b'\n'.join(re.findall(rb'[\x20-\x7e]{4,}', data))

pats = [
    rb'.{100}"/c/:code".{300}',
    rb'.{80}app\.get\("/c/.{300}',
    rb'.{60}\/c\/\$\{.{250}',
    rb'.{120}connectCode.{150}app\.(get|use).{200}',
]
for p in pats:
    ms = list(re.finditer(p, joined))
    print('###', p.decode()[:40], 'hits:', len(ms))
    for m in ms[:2]:
        print(m.group(0).decode('ascii', 'ignore')[:400])
        print('---')