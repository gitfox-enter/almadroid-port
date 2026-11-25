import re
data = open('go/alma-server', 'rb').read()
joined = b'\n'.join(re.findall(rb'[\x20-\x7e]{4,}', data))
pats = [
    rb'"/c/[^"]{0,60}"',
    rb'.{50}mobile-relay-node.{100}',
    rb'ALMA_GO_ROUTES.{100}',
    rb'.{40}lan-direct.{120}',
    rb'.{40}lanDirect.{120}',
    rb'.{30}localPort.{120}',
]
for p in pats:
    print('### PATTERN', p.decode())
    seen = 0
    for m in re.finditer(p, joined, re.I):
        print(m.group(0).decode('ascii', 'ignore'))
        print('---')
        seen += 1
        if seen >= 5:
            break
    if seen == 0:
        print('(no match)')
