import zipfile, re

z = zipfile.ZipFile('Alma-4.9.apk')
dex = z.read('classes.dex')
strings = re.findall(rb'[\x20-\x7e]{3,}', dex)
strs = [s.decode('ascii', 'ignore') for s in strings]

keywords = ['23999', '/api/', 'proot', 'deb ', '.deb', 'apt', 'pkg install', 'node', 'npm ', 'bun', 'claude', 'codex', 'opencode', 'agent cli', 'ALMA_DATA', 'alma-data', 'startServer', 'LocalServer', 'local server', 'Termux', 'bootstrap', 'downloadUrl', 'releases/download', 'matching-refs', 'ssh ', 'sshd', 'openssh', 'tailscale']
seen = set()
for kw in keywords:
    hits = [s for s in strs if kw.lower() in s.lower()]
    uniq = sorted(set(hits))
    if uniq:
        print('--- %s (%d unique) ---' % (kw, len(uniq)))
        for h in uniq[:25]:
            h2 = h if len(h) < 200 else h[:200] + '...'
            if h2 not in seen:
                print('   ', h2)
                seen.add(h2)
        print()
