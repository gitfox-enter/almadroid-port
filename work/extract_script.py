# -*- coding: utf-8 -*-
"""从 LocalService.java 提取 desktop apps (变量 a) 的 sh 脚本并语法检查。"""
import re, subprocess

src = open(r'D:/Movies/Download/alma-port/almadroid/server-java/io/almadroid/server/LocalService.java',
           encoding='utf-8').read()
i = src.find('a.add("/bin/sh"); a.add("-c");')
j = src.find('+ "wait");', i)
seg = src[i:j + len('+ "wait");')]
parts = re.findall(r'"((?:[^"\\]|\\.)*)"', seg)
BS = chr(92)
script = ''.join(
    p.replace(BS + 'n', chr(10)).replace(BS + '"', '"').replace(BS + BS, BS)
    for p in parts)
out = r'D:/Movies/Download/alma-port/work/desktop-script.sh'
open(out, 'w', encoding='utf-8', newline='\n').write(script)
print('script len:', len(script))
r = subprocess.run(['sh', '-n', out], capture_output=True, text=True)
print('sh -n exit:', r.returncode)
print(r.stderr[:1000])
