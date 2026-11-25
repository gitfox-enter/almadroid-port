#!/bin/bash
# Background watcher: when a phone appears on adb, deploy AlmaDroid automatically.
ADB=/d/Movies/Download/alma-port/work/sdk-home/platform-tools_x/platform-tools/adb.exe
LOG=/d/Movies/Download/alma-port/deploy-watch.log
echo "$(date) watcher started" >> "$LOG"
for i in $(seq 1 720); do
  dev=$("$ADB" devices 2>/dev/null | awk 'NR>1 && $2=="device" {print $1; exit}')
  if [ -n "$dev" ]; then
    echo "$(date) device found: $dev, deploying..." >> "$LOG"
    bash /d/Movies/Download/alma-port/deploy.sh >> "$LOG" 2>&1
    echo "$(date) deploy attempt finished" >> "$LOG"
    exit 0
  fi
  sleep 60
done
echo "$(date) watcher timed out after 12h" >> "$LOG"
