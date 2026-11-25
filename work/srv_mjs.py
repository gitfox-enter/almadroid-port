import re

data = open('lib/server.mjs', 'rb').read()
joined = b'\n'.join(re.findall(rb'[\x20-\x7e]{4,}', data))

pats = [
    rb'.{100}localPort.{150}',
    rb'.{80}new SW\(.{150}',
    rb'.{80}phoneSockets.{100}',
    rb'.{60}lanDirect\s*=.{150}',
    rb'.{80}startLan.{120}',
    rb'.{80}mobilePresence.{120}',
]
for p in pats:
    print('### PATTERN', p.decode())
    seen = 0
    for m in re.finditer(p, joined):
        print(m.group(0).decode('ascii', 'ignore'))
        print('---')
        seen += 1
        if seen >= 4:
            break
    if seen == 0:
        print('(no match)')
