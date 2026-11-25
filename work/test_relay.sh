#!/bin/bash
pkill -f local-relay.js 2>/dev/null
sleep 1
cd ~/alma-test/alma-server-0.4.713-linux-x64/lib
nohup ../runtime/node local-relay.js 23017 > /tmp/relay.log 2>&1 &
echo "pid=$!"
sleep 3
echo "--- log ---"
cat /tmp/relay.log
echo "--- port 23017 ---"
ss -tln | grep 23017 || echo NOT_LISTENING
echo "--- port 23019 ---"
ss -tln | grep 23019 || echo NOT_LISTENING
echo "--- WS bridge test ---"
../runtime/node -e '
const WebSocket = require("./ws");
const ws = new WebSocket("ws://127.0.0.1:23017/c/testHash123/__alma_e2e__");
ws.on("open", () => { console.log("WS BRIDGE OK"); ws.close(); process.exit(0); });
ws.on("error", (e) => { console.log("WS BRIDGE FAIL:", e.message); process.exit(1); });
setTimeout(() => { console.log("TIMEOUT"); process.exit(2); }, 8000);
'
echo "exit=$?"