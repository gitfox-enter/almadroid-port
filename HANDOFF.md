# AlmaDroid 桌面改造 —— 接手文档

> 最后状态：2026-11-22，v83 已装机（AlmaX-v83.apk，439MB），设备无线调试离线未能最终验收。
> 本文件是交接核心；详细坑位见记忆文件（文末列表）。

## 一、目标（用户需求原文）

1. 主页 → 左上角会话抽屉 → 点「电脑」→ 直接看到 Linux 桌面画面（不再「屏幕不可用」）
2. 桌面要像 **tiny_container**（用户手机上 `com.fct.tc4`）：**aVNC 和 X11 二选一原生显示，不是 noVNC**；预装浏览器 + OnlyOffice
3. 启动页只留图标+进度条；通知不打扰；全 App 中文
4. 用户明确评价：PIRT（noVNC 路线）不如 tiny，视频没声音；tiny 的原生客户端有声音

## 二、已完成（全部在 v83 内）

| 项 | 说明 |
|---|---|
| 启动页极简 | 图标+进度条，就绪 0.7s 自动进主界面（SplashActivity） |
| 通知最小化 | IMPORTANCE_MIN+静音+不可点（FGS 必须带通知，已压到最轻） |
| 「智能体」页修正 | 停发 pairSelf 的 `alma://connect` deep link（原来每次启动把官方 UI 顶到智能体页） |
| 汉化 | zh_cn4.py 补 29 处（No workspaces/New Workspace/Type something 等），三版累计 200+ 词条 |
| 抽屉「电脑」劫持 | R2.m smali 注入 `LocalBootstrap.openDesktopIfAbsent()`（j()==true 首次组合才跳，防重组循环）；DesktopActivity 退出 2s 后 rearm |
| **自研原生 VNC 客户端** | `VncView.java`（~430 行，零依赖）：RFB 3.8 + Raw/CopyRect/Hextile + 增量更新 + 触摸/键盘映射；DesktopActivity 主路径，失败兜底 noVNC WebView；横屏铺满 |
| **Xtigervnc 一体机** | 桌面脚本 tiger 分支：`Xtigervnc :1 -rfbport 5900 -SecurityTypes None -alwaysshared -localhost`；存在时替代 Xvfb+x11vnc 双进程（tiny 同款） |
| **LXQt 桌面预装** | 启动脚本幂等 `apt-get install lxqt-session ... falkon tigervnc-standalone-server`；dbus-run-session startlxqt，失败回退 openbox+xterm |
| **dpkg 中断修复** | 清 `/var/lib/dpkg/updates/*`+锁 → `timeout 90 dpkg --configure -a` → apt 恢复 |
| **dpkg-deb 缺失修复** | rootfs 裁剪版无 dpkg-deb（dpkg unpack 要 fork 它）→ `dpkgfix.tar.gz`（dpkg+5 依赖库）走 **logs push 通道**（adb push 到 files/logs = proot /alma-logs，启动脚本自动 tar 解包） |
| **rootfs 自愈** | `.desktop-ok` 标记（proot /opt/alma = app files/alma）未写 = 桌面安装没完成 → 启动时整树删除重建（防 apt 中断半写损坏 bash/libc） |
| OnlyOffice | 桌面终端 `install-office` 一键脚本（官网 deb+dpkg-deb 解包+patchelf --clear-execstack） |

## 三、关键文件

- `upgrade-417/build_x11.py` —— 两步打包脚本（apktool b → d8 合并 X11/我方类/R 类/桩 → append classes2.dex → zipalign+apksigner），输出 `upgrade-417/AlmaX-v8X.apk`；每次打包改末尾版本号
- `almadroid/server-java/io/almadroid/server/` —— 我方全部 Java（VncView/DesktopActivity/LocalService/LocalBootstrap/SplashActivity）
- `upgrade-417/alma-417/` —— apktool 解码树（smali 注入在 `k6.1/R2.smali`、`j6.1/H.smali`）
- `work/zh_cn4.py` —— 汉化补丁（跑完重打包）；`work/extract_script.py` —— 从 LocalService 提取桌面脚本做 sh -n 校验
- `work/rfb_grab.py` —— mini RFB 客户端，`adb forward tcp:15900 tcp:5900` 后抓 Xvfb 帧存 PNG（绕截图花屏）
- `work/dpkg-fix/dpkgfix.tar.gz` —— dpkg-deb+libbz2/liblzma5/libmd0/libzstd1/zlib1g（noble arm64），**装机后需 adb push 到 `files/logs/`**
- `work/tiny-ref/tiny.apk` + `tiny-dec/` —— tiny_container 逆向（Xtigervnc 配方/aVNC 集成）
- `work/pirt-tmp/` —— PIRT 源码（XTigervnc+websockify+noVNC 参考）

## 四、装机 / 验证命令

```bash
# 设备：手机开「无线调试」，电脑同 WiFi
adb mdns services | grep adb            # 找 mDNS 序列名（含空格括号，引号包裹）
adb -s "<序列名>" install -r upgrade-417/AlmaX-v83.apk
# 每次装机后补 dpkgfix（logs push 通道）：
adb -s "<序列名>" push work/dpkg-fix/dpkgfix.tar.gz /sdcard/Android/data/io.almadroid.almashell/files/logs/
adb -s "<序列名>" shell am start -n io.almadroid.almashell/io.almadroid.server.SplashActivity
# 观察：
adb -s "<序列名>" shell cat /sdcard/Android/data/io.almadroid.almashell/files/logs/apt.log    # 桌面安装
adb -s "<序列名>" shell cat /sdcard/Android/data/io.almadroid.almashell/files/logs/diag.log  # 文件状态诊断
adb -s "<序列名>" shell ps -A | grep -E 'Xtigervnc|lxqt|falkon'                              # 桌面栈
```

验证链路：启动页(图标+进度条) → 自动进主界面(非智能体页) → 抽屉「电脑」→ 原生 VNC 显示 LXQt
桌面（横屏铺满）→ Falkon 上网 → `install-office` 装 OnlyOffice。

## 五、遗留风险（明早第一件事）

1. **rootfs 半损坏重建**：若上次 apt 在熄屏(Dozing)中被打断，v83 首次启动会触发自愈删 rootfs 重解
   （300MB 解包 + 桌面重装，首次较久）。日志看 `AlmaLocal: rootfs self-heal`。
2. **dpkgfix push**：装机后必须 push（只存本机，不在 APK 里）。
3. **VncView 未真机验证**：TCP 5900 连接路径已按协议实现，若连接失败会闪退回 noVNC（可用兜底），
   把 DesktopActivity 的现象（黑屏/报错文案）截图即可定位。
4. **Dozing 冻结**：HyperOS 熄屏后冻结后台，apt/下载类长任务必须亮屏（或设不锁屏）。
5. **智能体页**：若冷启动仍落智能体页（官方自己记住了路由），手动回主页一次即可；如复现再查。

## 六、后续可做（未做，勿展开）

- VncView 软键盘/中英文输入（现在只有物理键盘 ASCII 映射）
- unix socket 通道（Xtigervnc `-rfbunixpath`，VncView 已留 unixPath 参数）
- Termux:X11 原生窗口修复（`AddExtensions failed` 卡 native EGL，tiny 的 libXlorie 能跑，可对照 tiny-dec）
- 音频（noVNC 无声音是方案级；原生 X11 才有）

## 七、项目记忆（本会话已写入，全部保留）

- `tiny-container-graphics-recipe.md` —— tiny 配方（Xtigervnc+unix socket+内嵌aVNC）
- `almadroid-desktop-lxqt-pitfalls.md` —— 连环坑：pkill -f 自匹配两连杀、dpkg-deb 缺失、
  dpkg interrupted、tiny 进程混淆误判成功、Dozing 冻结、logs push 通道
- `almadroid-linux-desktop.md` / `almadroid-port-project.md` / `almadroid-local-plaintext-mode.md`
  —— 历史架构与链路