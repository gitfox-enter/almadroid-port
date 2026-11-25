#!/bin/bash
ADB=/d/Movies/Download/alma-port/work/sdk-home/platform-tools_x/platform-tools/adb.exe
DEV='<DEVICE_SERIAL> (3)._adb-tls-connect._tcp'
echo "=== rootfs resolv.conf content (via tar-friendly path) ==="
MSYS_NO_PATHCONV=1 "$ADB" -s "$DEV" shell "su 2>/dev/null cat /data/data/io.almadroid.app/files/rootfs/etc/resolv.conf" 2>&1 | head -4
echo "=== IPv6 test from phone ==="
MSYS_NO_PATHCONV=1 "$ADB" -s "$DEV" shell "curl -s -o /dev/null -w '%{http_code}' --max-time 8 https://relay.alma.now -6" 2>&1
echo " <- ipv6 result (000=fail)"
MSYS_NO_PATHCONV=1 "$ADB" -s "$DEV" shell "curl -s -o /dev/null -w '%{http_code}' --max-time 8 https://relay.alma.now -4" 2>&1
echo " <- ipv4 result"
echo "=== what IP did relay resolve ==="
MSYS_NO_PATHCONV=1 "$ADB" -s "$DEV" shell "curl -sv --max-time 8 https://relay.alma.now 2>&1 | grep -E 'Connected to|Trying'" 2>&1 | head -4
