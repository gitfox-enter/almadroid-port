#!/bin/bash
# Deploy AlmaDroid to a connected phone: install APK, push alma-server bundle, launch app.
# Usage: bash deploy.sh   (runs once if device present)
ADB=/d/Movies/Download/alma-port/work/sdk-home/platform-tools_x/platform-tools/adb.exe
APK=/d/Movies/Download/alma-port/almadroid/build/AlmaDroid-2.1.apk
TARBALL=/d/Movies/Download/alma-port/work/alma-server.tar.gz

dev=$("$ADB" devices | awk 'NR>1 && $2=="device" {print $1; exit}')
if [ -z "$dev" ]; then
  echo "NO_DEVICE"
  exit 1
fi
echo "device: $dev"

echo "== install APK =="
"$ADB" -s "$dev" install -r "$APK" || { echo INSTALL_FAILED; exit 1; }

echo "== push alma-server bundle to import dir =="
PKG=io.almadroid.app
"$ADB" -s "$dev" shell "mkdir -p /sdcard/Android/data/$PKG/files/import" 2>/dev/null
"$ADB" -s "$dev" push "$TARBALL" "/sdcard/Android/data/$PKG/files/import/alma-server.tar.gz" || echo "push failed (app can download instead)"

echo "== grant battery exemption hint (manual on HyperOS) =="
"$ADB" -s "$dev" shell "dumpsys deviceidle whitelist +$PKG" 2>/dev/null || true

echo "== launch app =="
"$ADB" -s "$dev" shell "monkey -p $PKG -c android.intent.category.LAUNCHER 1" 2>/dev/null

echo "== logcat (first 20 lines of app) =="
sleep 6
"$ADB" -s "$dev" logcat -d -t 40 | grep -iE "almadroid|proot|AlmaService|Bootstrap|TarGz|AndroidRuntime" | tail -30
echo "DEPLOY_OK"
