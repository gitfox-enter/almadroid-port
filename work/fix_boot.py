p = r'<PROJECT_ROOT>\almadroid\server-java\io\almadroid\server\LocalService.java'
s = open(p, encoding='utf-8').read()

start = s.find('        String boot =')
end = s.find('        ProcessBuilder pb = new ProcessBuilder(')
assert start > 0 and end > start

new_block = '''        // 主机侧预建目录（proot 内对应 /alma-data/home、/alma-data/alma、/alma-logs）
        new File(home, "").mkdirs();
        new File(data, "").mkdirs();
        new File(logs, "").mkdirs();
        // 直接用绝对路径启动，避免依赖 cd/mkdir/sh 内建命令
        String boot =
                "export HOME=/alma-data/home; "
                + "export ALMA_HEADLESS=1; "
                + "export ALMA_PORT=23001; "
                + "export NODE_OPTIONS=--dns-result-order=ipv4first; "
                + "export PATH=/usr/local/sbin:/usr/local/bin:/usr/sbin:/usr/bin:/sbin:/bin; "
                + "if [ -f /opt/alma/local-relay.js ]; then "
                + "( /opt/alma/runtime/node /opt/alma/local-relay.js 23017 >>/alma-logs/relay.log 2>&1 & ); "
                + "fi; "
                + "exec /bin/sh /opt/alma/bin/alma-server --port 23001 --data-dir /alma-data/alma";

'''
s = s[:start] + new_block + s[end:]
open(p, 'w', encoding='utf-8').write(s)
print('boot script replaced')