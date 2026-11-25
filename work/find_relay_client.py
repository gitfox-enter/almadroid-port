import re

data = open(r'<PROJECT_ROOT>\work\alma-server-0.4.713-linux-arm64\lib\server.mjs', 'rb').read()
joined = b'\n'.join(re.findall(rb'[\x20-\x7e]{4,}', data))

# find the relay client class that connects (constructor with serverUrl, connectCode)
pats = [
    rb'.{100}registrationKey.{200}connect\(.{300}',
    rb'.{150}connectCode:.{100}registrationKey.{300}',
    rb'class \w+\{[^{]{0,100}constructor\(\{serverUrl.{500}',
    rb'.{80}new WebSocket\(.{200}',
]
for p in pats:
    ms = list(re.finditer(p, joined))
    print('###', p.decode()[:40], 'hits:', len(ms))
    for m in ms[:2]:
        print(m.group(0).decode('ascii', 'ignore')[:600])
        print('---')