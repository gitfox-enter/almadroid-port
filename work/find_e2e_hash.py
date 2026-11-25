import re

data = open(r'<PROJECT_ROOT>\work\alma-server-0.4.713-linux-arm64\lib\server.mjs', 'rb').read()
joined = b'\n'.join(re.findall(rb'[\x20-\x7e]{4,}', data))

# Find: how does the E2E tunnel decide which code a connection belongs to?
pats = [
    rb'alma-relay-e2e/v1/routing.{300}',
    rb'.{150}routing.{200}',
    rb'.{100}HKDF|hkdf.{200}',
    rb'.{80}createHash\(.sha256.\).{200}',
    rb'.{100}computeTokenHash.{300}',
]
for p in pats:
    ms = list(re.finditer(p, joined))
    print('###', p.decode()[:38], 'hits:', len(ms))
    for m in ms[:2]:
        print(m.group(0).decode('ascii', 'ignore')[:500])
        print('---')