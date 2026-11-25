import zipfile, re, sys

z = zipfile.ZipFile('Alma-4.9.apk')
dex = z.read('classes.dex')
strings = re.findall(rb'[\x20-\x7e]{5,}', dex)
strs = [s.decode('ascii', 'ignore') for s in strings]

# URLs and endpoints
urls = set()
url_re = re.compile(r'https?://[A-Za-z0-9._~:/?#\[\]@!$&()*+,;=%-]+')
for s in strs:
    for m in url_re.findall(s):
        urls.add(m.rstrip('/\\'))

print('=== URLS (%d) ===' % len(urls))
for u in sorted(urls):
    print(u)

print()
print('=== KEYWORD HITS ===')
keywords = ['termux', 'Termux', 'localhost', '127.0.0.1', '23001', 'herdr', 'socket', 'WebSocket', 'relay', 'pair', ' Pair', 'e2ee', 'proot', 'debian', 'ubuntu', 'rootfs', 'bootstrap', 'alma-server', 'almaServer']
for kw in keywords:
    hits = [s for s in strs if kw in s]
    uniq = sorted(set(hits))[:12]
    if uniq:
        print('--- %s (%d unique shown of %d) ---' % (kw, len(uniq), len(hits)))
        for h in uniq:
            h2 = h if len(h) < 220 else h[:220] + '...'
            print('   ', h2)
