#!/bin/bash
# Manual APK build pipeline for AlmaDroid (no gradle/AGP).
set -e
BASE=/d/Movies/Download/alma-port
APP=$BASE/almadroid/app
WORK=$BASE/work
BUILD=$BASE/almadroid/build
SDK_BT=$WORK/sdk-ex/android-14
ANDROID_JAR=$WORK/sdk-home/platform-34_x/android-34/android.jar
JAVA_HOME_DIR="/c/Program Files/Microsoft/jdk-25.0.4.7-hotspot"
ALMA_KS_PW=<KS_PASSWORD>
JAVA="$JAVA_HOME_DIR/bin/java.exe"
KEYTOOL="$JAVA_HOME_DIR/bin/keytool.exe"

# Windows-friendly paths (D:/...)
WAPP=$(cygpath -m "$APP")
WWORK=$(cygpath -m "$WORK")
WBUILD=$(cygpath -m "$BUILD")
WBT=$(cygpath -m "$SDK_BT")
WJAR=$(cygpath -m "$ANDROID_JAR")

rm -rf "$BUILD"/*
mkdir -p "$BUILD/gen" "$BUILD/classes" "$BUILD/flats" "$BUILD/out"

echo "== 1. aapt2 compile =="
find "$APP/res" -type f | while read -r f; do
  "$SDK_BT/aapt2.exe" compile -o "$(cygpath -w "$BUILD/flats")" "$(cygpath -w "$f")"
done

echo "== 2. aapt2 link =="
"$SDK_BT/aapt2.exe" link \
  -o "$(cygpath -w "$BUILD/out/base.apk")" \
  -I "$(cygpath -w "$ANDROID_JAR")" \
  --manifest "$(cygpath -w "$APP/AndroidManifest.xml")" \
  -A "$(cygpath -w "$APP/assets")" \
  --java "$(cygpath -w "$BUILD/gen")" \
  --auto-add-overlay \
  $(ls "$BUILD"/flats/*.flat | while read -r f; do cygpath -w "$f"; done)

echo "== 3. javac =="
"$JAVA_HOME_DIR/bin/javac.exe" \
  --release 11 \
  -classpath "$WJAR" \
  -d "$WBUILD/classes" \
  $(find "$APP/java" "$BUILD/gen" -name "*.java" | while read -r f; do cygpath -m "$f"; done)

echo "== 4. d8 dex =="
D8JAR="$WWORK/runtime/sdk/bt35/android-VanillaIceCream/lib/d8.jar"
"$JAVA" -cp "$D8JAR" com.android.tools.r8.D8 \
  --release \
  --lib "$WJAR" \
  --min-api 26 \
  --output "$WBUILD/out" \
  $(find "$BUILD/classes" -name "*.class" | while read -r f; do cygpath -m "$f"; done)

echo "== 5. zip dex + jniLibs into apk =="
cd "$BUILD/out"
python -c "
import zipfile, os
JNILIB = r'D:/Movies/Download/alma-port/almadroid/app/jniLibs'
src = zipfile.ZipFile('base.apk')
with zipfile.ZipFile('base2.apk', 'w', zipfile.ZIP_DEFLATED) as out:
    for item in src.infolist():
        out.writestr(item, src.read(item.filename))
    with open('classes.dex','rb') as f:
        out.writestr('classes.dex', f.read())
    for root, dirs, files in os.walk(os.path.join(JNILIB, 'arm64-v8a')):
        for fn in files:
            p = os.path.join(root, fn)
            arc = 'lib/arm64-v8a/' + fn
            # native libs stored uncompressed (safe extraction)
            with open(p, 'rb') as f:
                out.writestr(zipfile.ZipInfo(arc, (2026,1,1,0,0,0)), f.read(), zipfile.ZIP_STORED)
src.close()
os.remove('base.apk')
os.rename('base2.apk', 'base.apk')
print('dex + jniLibs merged')
"

echo "== 6. zipalign =="
"$SDK_BT/zipalign.exe" -f 4 "$WBUILD/out/base.apk" "$WBUILD/out/aligned.apk"

echo "== 7. keystore =="
if [ ! -f "$BASE/almadroid.keystore" ]; then
  "$KEYTOOL" -genkeypair -keystore "$(cygpath -w "$BASE/almadroid.keystore")" \
    -storepass almadroid -keypass almadroid -alias almadroid \
    -keyalg RSA -keysize 2048 -validity 10000 \
    -dname "CN=AlmaDroid, OU=Local, O=Local, L=Local, ST=Local, C=CN" \
    >/dev/null 2>&1
fi

echo "== 8. sign =="
"$JAVA" -jar "$WBT/lib/apksigner.jar" sign \
  --ks "$(cygpath -w "$BASE/almadroid.keystore")" --ks-pass pass:<KS_PASSWORD> \
  --key-pass pass:<KS_PASSWORD> \
  --out "$WBUILD/AlmaDroid-3.8.apk" \
  "$WBUILD/out/aligned.apk"

"$JAVA" -jar "$WBT/lib/apksigner.jar" verify "$WBUILD/AlmaDroid-3.8.apk" && echo "verify OK"
ls -la "$BUILD/AlmaDroid-3.8.apk"
echo "BUILD DONE"
