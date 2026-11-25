#!/bin/bash
ADB=/d/Movies/Download/alma-port/work/sdk-home/platform-tools_x/platform-tools/adb.exe
DEV='<DEVICE_SERIAL> (3)._adb-tls-connect._tcp'
MSYS_NO_PATHCONV=1 "$ADB" -s "$DEV" shell "curl -s -i --max-time 8 -H 'Connection: Upgrade' -H 'Upgrade: websocket' -H 'Sec-WebSocket-Version: 13' -H 'Sec-WebSocket-Key: dGhlIHNhbXBsZSBub25jZQ==' 'https://relay.alma.now/relay'" 2>&1 | head -10
echo "=== relay root ==="
MSYS_NO_PATHCONV=1 "$ADB" -s "$DEV" shell "curl -s -i --max-time 8 'https://relay.alma.now/'" 2>&1 | head -6
