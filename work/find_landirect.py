import re

data = open(r'<PROJECT_ROOT>\work\alma-server-0.4.713-linux-arm64\lib\server.mjs', 'rb').read()
joined = b'\n'.join(re.findall(rb'[\x20-\x7e]{4,}', data))

# where is startLanDirect called?
for m in re.finditer(rb'.{300}startLanDirect\(e\.connectCode\).{100}', joined):
    print(m.group(0).decode('ascii', 'ignore')[:700])
    print('===')
    break

print('##### 完整 syncMobileRelay 函数 #####')
idx = joined.rfind(b'syncMobileRelayFromSettings(){')
print(joined[idx:idx + 1400].decode('ascii', 'ignore'))