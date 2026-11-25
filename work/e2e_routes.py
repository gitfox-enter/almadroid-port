import re

data = open('lib/server.mjs', 'rb').read()
joined = b'\n'.join(re.findall(rb'[\x20-\x7e]{4,}', data))

# find WDe class definition and its route strings
pats = [
    rb'class WDe[^{]{0,40}\{.{800}',
    rb'.{60}"/__alma_e2e[^"]{0,40}".{100}',
    rb'.{50}(alma-e2e|e2e-handshake|pair-handshake|/pair).{120}',
    rb'.{80}(candidates|candidate)\(\).{100}',
]
for p in pats:
    print('### PATTERN', p.decode())
    seen = 0
    for m in re.finditer(p, joined, re.S):
        txt = m.group(0).decode('ascii', 'ignore')
        print(txt[:900])
        print('---')
        seen += 1
        if seen >= 3:
            break
    if seen == 0:
        print('(no match)')
