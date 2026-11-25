import re

data = open('lib/server.mjs', 'rb').read()
# WDe class region: find "WDe" as class and dump surrounding 4000 chars
joined = b'\n'.join(re.findall(rb'[\x20-\x7e]{4,}', data))
idx = joined.find(b'YJs="/__alma_e2e__"')
if idx < 0:
    idx = joined.find(b'/__alma_e2e__')
print(joined[idx-200:idx+3500].decode('ascii', 'ignore'))
