# -*- coding: utf-8 -*-
"""两步打包：apktool 出资源 → aapt 读 ID → 生成 R 类 → 与 Termux:X11 类合并 d8 → classes3.dex。"""
import subprocess, os, re, shutil, zipfile, sys

BASE = r'<PROJECT_ROOT>\upgrade-417'
BT = r'<PROJECT_ROOT>\work\sdk-ex\android-14'
AJ = r'<PROJECT_ROOT>\work\sdk-home\platform-34_x\android-34\android.jar'
J17 = r'<PROJECT_ROOT>\work\jdk17\jdk-17.0.21+9\bin\java.exe'
JAVAC = r'<PROJECT_ROOT>\work\jdk17\jdk-17.0.21+9\bin\javac.exe'
AAPT = os.path.join(BT, 'aapt.exe')
D8 = r'<PROJECT_ROOT>\work\runtime\sdk\bt35\android-VanillaIceCream\lib\d8.jar'
X11JAR = r'<PROJECT_ROOT>\work\termux-x11.aar'
TMP = os.path.join(BASE, 'x11build')
os.chdir(BASE)

# 排除设置页类（其 res 已剔除）
EXCLUDE_PREFIX = ('com/termux/x11/LoriePreferences', 'com/termux/x11/Prefs')

# ---- 代码引用清单（javap 已确认） ----
REFS = {
    'array': ['displayFilteringVariants','displayFilteringVariantsEntries','displayResolution','displayResolutionVariants','displayResolutionVariantsEntries','forceOrientationVariants','forceOrientationVariantsEntries','touchscreenInputModesEntries','touchscreenInputModesValues','transformCapturedPointerEntries','transformCapturedPointerValues','userActionsMediaKeyEntries','userActionsMediaKeyValues','userActionsValues','userActionsValuesEntries','userActionsVolumeDownEntries','userActionsVolumeDownValues','userActionsVolumeUpEntries','userActionsVolumeUpValues'],
    'drawable': ['ic_x11_icon'],
    'id': ['exit_button','extra_keys_config','extra_keys_config_description','frame','help_button','lorieView','mouse_button_left_click','mouse_button_middle_click','mouse_button_right_click','mouse_buttons','mouse_buttons_position','mouse_buttons_secondary_layer','mouse_helper_visibility','preferences_button','terminal_toolbar_text_input','terminal_toolbar_view_pager'],
    'layout': ['extra_keys_config','main_activity'],
    'string': ['lorie_dialog_permission_denied_message','lorie_dialog_permission_denied_title','lorie_error_native_library_load','lorie_notification_content_text','lorie_pref_summary_requiresExactOrCustom','lorie_pref_summary_requiresIntercepting','lorie_pref_summary_requiresTrackpadAndNative','lorie_toast_copied_to_clipboard','lorie_toast_wrong_resolution_format'],
}

# ---- step1: apktool b ----
print('== apktool b ==')
r = subprocess.run(['java','-jar',r'<TOOLS>\apktool\apktool_3.0.3.jar','b','-f','alma-417','-o','base417.apk'],
                   capture_output=True, text=True)
if r.returncode != 0:
    print(r.stderr[-800:]); sys.exit(1)
print('apktool ok')

# ---- step2: 不在此步 append classes2（后面 d8 合并后再 append） ----
shutil.copy('base417.apk','417.apk')

# ---- step3: aapt dump 读 ID ----
print('== aapt dump ==')
r = subprocess.run([AAPT,'dump','resources','417.apk'], capture_output=True, text=True)
ids = {}  # (type,name) -> int id
for line in r.stdout.split('\n'):
    m = re.match(r'\s*spec resource 0x([0-9a-fA-F]+) io\.almadroid\.almashell:(\w+)/([\w.]+)', line)
    if m:
        ids[(m.group(2), m.group(3))] = int(m.group(1), 16)

missing = []
for t, names in REFS.items():
    for n in names:
        if (t, n) not in ids:
            missing.append(f'{t}/{n}')
if missing:
    print('MISSING RES:', missing); sys.exit(1)

# ---- step4: 生成 R.java ----
print('== gen R.java ==')
cls_lines = ['package com.termux.x11;', 'public final class R {']
for t, names in REFS.items():
    cls_lines.append(f'  public static final class {t} {{')
    for n in sorted(set(names)):
        cls_lines.append(f'    public static int {n} = 0x{ids[(t,n)]:08x};')
    cls_lines.append('  }')
cls_lines.append('}')
os.makedirs(TMP, exist_ok=True)
open(os.path.join(TMP,'R.java'),'w',encoding='utf-8').write('\n'.join(cls_lines))

# ---- step5: javac R ----
r = subprocess.run([JAVAC,'-source','8','-target','8','-nowarn','-bootclasspath',AJ,
                    '-d',TMP, os.path.join(TMP,'R.java')], capture_output=True, text=True)
if r.returncode != 0:
    print(r.stderr[-500:]); sys.exit(1)

# ---- step6: 解 classes.jar（排除设置页） ----
print('== extract x11 classes ==')
zx = zipfile.ZipFile(X11JAR)
cj = zipfile.ZipFile(__import__('io').BytesIO(zx.read('classes.jar')))
cj.extractall(TMP)
# 收集要 d8 的 class 文件
class_files = []
for dp,_,fns in os.walk(TMP):
    for fn in fns:
        if fn.endswith('.class'):
            full = os.path.join(dp,fn)
            rel = os.path.relpath(full, TMP).replace('\\','/')
            if rel.startswith(EXCLUDE_PREFIX): continue
            if rel.startswith('com/termux/x11/CmdEntryPointService'): continue
            class_files.append(full)
print('d8 inputs:', len(class_files))

# ---- step7: d8 —— 我们的 Java 层 + X11 类 + R 类 + androidx 桩 合并成单个 classes2.dex ----
OUR = r'<PROJECT_ROOT>\almadroid\server-build\classes'
STUBS = r'<PROJECT_ROOT>\work\stubs\classes'
our_files = []
for dp,_,fns in os.walk(OUR):
    for fn in fns:
        if fn.endswith('.class'):
            our_files.append(os.path.join(dp,fn))
for dp,_,fns in os.walk(STUBS):
    for fn in fns:
        if fn.endswith('.class'):
            our_files.append(os.path.join(dp,fn))
print('our classes (incl stubs):', len(our_files))
outd = os.path.join(TMP,'out')
if os.path.isdir(outd): shutil.rmtree(outd)
os.makedirs(outd, exist_ok=True)
r = subprocess.run([J17,'-cp',D8,'com.android.tools.r8.D8','--min-api','28','--release',
                    '--lib',AJ,'--output',outd] + class_files + our_files, capture_output=True, text=True)
if r.returncode != 0:
    print('D8 ERR:', r.stderr[-800:]); sys.exit(1)
dexs = [f for f in os.listdir(outd) if f.endswith('.dex')]
print('merged dex count:', len(dexs), dexs)

# ---- step8: append 全部合并 dex ----
z = zipfile.ZipFile('417.apk','a', zipfile.ZIP_STORED)
for i, d in enumerate(dexs):
    name = 'classes2.dex' if i == 0 else f'classes{ i+2 }.dex'
    zi = zipfile.ZipInfo(name,(2026,1,1,0,0,0)); zi.compress_type = zipfile.ZIP_STORED
    z.writestr(zi, open(os.path.join(outd,d),'rb').read())
    print('appended', name)
z.close()

# ---- step9: zipalign + sign ----
print('== align+sign ==')
r = subprocess.run([os.path.join(BT,'zipalign.exe'),'-f','-p','4','417.apk','417-al.apk'], capture_output=True, text=True)
if r.returncode != 0: print(r.stderr[-300:]); sys.exit(1)
KS = r'<PROJECT_ROOT>\almadroid.keystore'
r = subprocess.run([J17,'-jar',os.path.join(BT,'lib','apksigner.jar'),'sign','--ks',KS,
                    '--ks-pass','pass:<KS_PASSWORD>','--key-pass','pass:<KS_PASSWORD>',
                     '--out','AlmaX-v84.apk','417-al.apk'], capture_output=True, text=True)
if r.returncode != 0: print(r.stderr[-300:]); sys.exit(1)
v = subprocess.run([J17,'-jar',os.path.join(BT,'lib','apksigner.jar'),'verify','AlmaX-v84.apk'], capture_output=True, text=True)
print('verify:', v.returncode)
print('size:', round(os.path.getsize('AlmaX-v84.apk')/1024/1024,1),'MB')
