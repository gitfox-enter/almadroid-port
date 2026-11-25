package io.almadroid.server;

import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.os.Build;
import android.os.Handler;
import android.os.IBinder;
import android.os.Looper;
import android.util.Log;

import java.io.File;

/** 前台服务：跑 proot + alma-server + local-relay，全流程进度通知。 */
public class LocalService extends Service {
    private static final String TAG = "AlmaLocal";
    private static final String CH = "alma_local";
    private final Handler main = new Handler(Looper.getMainLooper());
    private int notifyId = 10;

    @Override
    public IBinder onBind(Intent i) { return null; }

    @Override
    public void onCreate() {
        super.onCreate();
        NotificationManager nm = (NotificationManager) getSystemService(NOTIFICATION_SERVICE);
        NotificationChannel ch = new NotificationChannel(CH, "Alma 本机服务", NotificationManager.IMPORTANCE_MIN);
        ch.setDescription("保持本机 Alma 服务与桌面运行");
        ch.setShowBadge(false);
        ch.setSound(null, null);
        ch.enableVibration(false);
        nm.createNotificationChannel(ch);

        acquireWakeLock();
        requestIgnoreBatteryOptimizations();
    }

    private android.os.PowerManager.WakeLock wakeLock;

    /** 持有 CPU 唤醒锁，避免息屏后 proot/Node/X11 被挂起。 */
    private void acquireWakeLock() {
        try {
            android.os.PowerManager pm =
                    (android.os.PowerManager) getSystemService(POWER_SERVICE);
            if (pm == null) return;
            wakeLock = pm.newWakeLock(
                    android.os.PowerManager.PARTIAL_WAKE_LOCK, "AlmaDroid::service");
            wakeLock.setReferenceCounted(false);
            wakeLock.acquire();
            Log.i(TAG, "wakelock acquired");
        } catch (Throwable t) {
            Log.w(TAG, "wakelock failed", t);
        }
    }

    /** 引导用户把本应用加入电池优化白名单（HyperOS/MIUI 必须）。 */
    private void requestIgnoreBatteryOptimizations() {
        try {
            android.os.PowerManager pm =
                    (android.os.PowerManager) getSystemService(POWER_SERVICE);
            if (pm == null) return;
            if (pm.isIgnoringBatteryOptimizations(getPackageName())) {
                Log.i(TAG, "already ignoring battery optimizations");
                return;
            }
            Intent i = new Intent(
                    android.provider.Settings.ACTION_REQUEST_IGNORE_BATTERY_OPTIMIZATIONS);
            i.setData(android.net.Uri.parse("package:" + getPackageName()));
            i.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
            startActivity(i);
            Log.i(TAG, "requested battery optimization exemption");
        } catch (Throwable t) {
            Log.w(TAG, "battery optimization request failed", t);
        }
    }

    /** 防重复启动：Application.onCreate 和 SplashActivity 都会触发 startService。 */
    private static final java.util.concurrent.atomic.AtomicBoolean BOOTING =
            new java.util.concurrent.atomic.AtomicBoolean(false);
    private static volatile Process sProc;

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        startFg("准备环境…");
        if (!BOOTING.compareAndSet(false, true)) {
            Log.i(TAG, "boot already in progress, skip duplicate start");
            return START_STICKY;
        }
        new Thread(new Runnable() { public void run() {
            try {
                // rootfs 自愈：桌面安装（apt unpack）被熄屏/系统中断会让 bash/libc 半写损坏，
                // 之后所有 proot 的 /bin/sh 都 exec 失败。.desktop-ok 标记未写 = 安装未完成 → 重建。
                File rootfsDir = new File(getFilesDir(), "rootfs");
                File deskOk = new File(new File(getFilesDir(), "alma"), ".desktop-ok");
                if (!deskOk.isFile() && rootfsDir.isDirectory()) {
                    Log.w(TAG, "rootfs self-heal: desktop install not completed, rebuilding rootfs");
                    setProgress("修复 Linux 环境…");
                    deleteRecursive(rootfsDir);
                }
                setProgress("解包 Linux rootfs…");
                if (!extractIfNeeded("rootfs.tar.gz", new File(getFilesDir(), "rootfs"))) {
                    fail("rootfs 解包失败");
                    return;
                }
                setProgress("安装开发工具…");
                installDevtoolsLayer();
                setProgress("修复系统权限…");
                fixRootfsPerms(new File(getFilesDir(), "rootfs"));
                setProgress("安装桌面组件…");
                installDesktopLayer();
                setProgress("安装桌面应用…");
                installDesktopAppsLayer();
                installHerdrCore(almaDir());
                setProgress("解包 alma-server…");
                if (!extractIfNeeded("alma-server.tar.gz", new File(getFilesDir(), "alma"))) {
                    fail("alma-server 解包失败");
                    return;
                }
                setProgress("放置本地 relay…");
                extractIfNeeded("local-relay.js", new File(getFilesDir(), "alma/local-relay.js"), false);
                fixRootfs();
                setProgress("启动服务…");
                launchProot();

                // 关键：relay 是客户端唯一的接入点(23017)。
                // 必须先确认它在听，再宣布"服务就绪"，否则客户端会一直 Reconnecting。
                final int lanPort = LanSettings.enabled(LocalService.this)
                        ? LanSettings.port(LocalService.this) : LanSettings.DEFAULT_PORT;
                setProgress("启动局域网入口…");
                launchRelayAsync();
                launchDesktopAsync();   // 并行：X11 不必等 relay
                boolean relayUp = waitPort(lanPort, 45000);
                Log.i(TAG, "relay port " + lanPort + " up=" + relayUp);

                if (!relayUp) {
                    // 再试一次（可能端口被旧进程占着还没释放）
                    Log.w(TAG, "relay not up, retry after 3s");
                    try { Thread.sleep(3000); } catch (InterruptedException e) {}
                    launchRelayAsync();
                    relayUp = waitPort(lanPort, 20000);
                    Log.i(TAG, "relay retry up=" + relayUp);
                }

                setFg(relayUp ? "Alma 服务运行中 · 本机就绪"
                              : "Alma 服务运行中 · 局域网入口未就绪");
                Log.i(TAG, "service started");

                // 服务就绪 → 触发配对（发给官方界面自己）
                waitHealthyThenPair();
                // 之后常驻看护：proot 挂掉或 health 连续失败就重启
                startWatchdog();
            } catch (Throwable t) {
                Log.e(TAG, "service failed", t);
                BOOTING.set(false);
                fail("服务失败：" + t.getMessage());
            }
        }}, "alma-local").start();
        return START_STICKY;
    }

    private void waitHealthyThenPair() {
        // 最多等 5 分钟，每 5 秒探一次；就绪后触发官方界面的配对
        for (int i = 0; i < 60; i++) {
            if (LocalBootstrap.health()) {
                main.post(new Runnable() { public void run() { setFg("服务就绪 · 配对本机界面…"); } });
                LocalBootstrap.pairSelf(this);
                return;
            }
            try { Thread.sleep(5000); } catch (InterruptedException e) { return; }
        }
        main.post(new Runnable() { public void run() { setFg("服务启动超时，重启服务重试"); } });
    }

    private void fail(String msg) {
        try { setFg("失败：" + msg); } catch (Throwable ignored) {}
        Log.e(TAG, msg);
    }

    // ---- 解包 ----

    private boolean extractIfNeeded(String asset, File destDir) {
        return extractIfNeeded(asset, destDir, true);
    }

    private boolean extractIfNeeded(String asset, File dest, boolean isDir) {
        try {
            long assetLen = assetSize(asset);
            if (isDir) {
                File marker = new File(dest, ".extracted-" + assetLen);
                boolean hit = marker.isFile();
                Log.i(TAG, "extract " + asset + " assetLen=" + assetLen
                        + " marker=" + marker.getName() + " hit=" + hit);
                if (hit) return true;
            } else if (dest.isFile() && dest.length() == assetLen) {
                return true;
            }
            try (java.io.InputStream is = getAssets().open(asset)) {
                if (asset.endsWith(".tar.gz")) {
                    final long fTotal = assetLen;
                    TarGz.extract(is, assetLen, dest, new TarGz.Progress() {
                        public void onBytes(long read, long total) {
                            long t = total > 0 ? total : fTotal;
                            if (t > 0) {
                                int pct = (int) (read * 100 / t);
                                setProgress("解包 " + asset + " " + Math.min(pct, 100) + "%");
                            } else {
                                setProgress("解包 " + asset + " …");
                            }
                        }
                    });
                } else {
                    File tmp = new File(dest.getAbsolutePath() + ".tmp");
                    java.io.FileOutputStream os = new java.io.FileOutputStream(tmp);
                    byte[] buf = new byte[65536];
                    int n;
                    while ((n = is.read(buf)) > 0) os.write(buf, 0, n);
                    os.close();
                    if (!tmp.renameTo(dest)) tmp.delete();
                }
            }
            if (isDir) {
                File marker = new File(dest, ".extracted-" + assetLen);
                try (java.io.FileOutputStream os = new java.io.FileOutputStream(marker)) {
                    os.write(1);
                }
            }
            return true;
        } catch (Throwable t) {
            Log.e(TAG, "extract " + asset + " failed", t);
            return false;
        }
    }

    private long assetSize(String asset) throws Exception {
        try (java.io.InputStream is = getAssets().open(asset)) {
            long n = 0;
            byte[] b = new byte[65536];
            int r;
            while ((r = is.read(b)) > 0) n += r;
            return n;
        }
    }

    /**
     * 修复 rootfs 权限树。
     * 原因：tar 里保留了原始 555/444 权限位，而 proot 的 -0 是"假 root"，
     * 内核仍按真实 uid 校验 —— 属主无写权限时 apt/dpkg 会 Permission denied。
     * 这里把整棵树的目录设为 0755（可写可进）、文件补上属主写位。
     */
    private void fixRootfsPerms(File root) {
        long t0 = System.currentTimeMillis();
        int dirs = 0, files = 0;
        java.util.ArrayDeque<File> q = new java.util.ArrayDeque<>();
        q.push(root);
        while (!q.isEmpty()) {
            File d = q.pop();
            File[] kids = d.listFiles();
            if (kids == null) continue;
            for (File f : kids) {
                if (f.isDirectory()) {
                    f.setReadable(true, false);
                    f.setWritable(true, false);
                    f.setExecutable(true, false);
                    dirs++;
                    q.push(f);
                } else {
                    if (!f.canWrite()) f.setWritable(true, false);
                    if (f.canExecute()) f.setExecutable(true, false);
                    files++;
                }
            }
        }
        Log.i(TAG, "fixRootfsPerms done: dirs=" + dirs + " files=" + files
                + " in " + (System.currentTimeMillis() - t0) + "ms");
    }

    private void fixRootfs() {
        File etc = new File(getFilesDir(), "rootfs/etc");
        etc.mkdirs();
        writeFile(new File(etc, "resolv.conf"),
                "nameserver 223.5.5.5\nnameserver 119.29.29.29\nnameserver 8.8.8.8\n");
        writeFile(new File(etc, "hosts"), "127.0.0.1 localhost\n");
        for (String p : new String[]{"root", "opt", "tmp", "dev/shm", "run", "var/tmp"})
            new File(getFilesDir(), "rootfs/" + p).mkdirs();
    }

    private void writeFile(File f, String s) {
        try (java.io.FileOutputStream os = new java.io.FileOutputStream(f)) {
            os.write(s.getBytes(java.nio.charset.StandardCharsets.UTF_8));
        } catch (Exception ignored) {}
    }

    // ---- proot ----

    private void launchProot() throws Exception {
        if (sProc != null && isAlive(sProc)) {
            Log.i(TAG, "proot already running, skip relaunch");
            return;
        }
        Context c = this;
        String lib = getApplicationInfo().nativeLibraryDir;
        File rootfs = new File(getFilesDir(), "rootfs");
        File almaRoot = new File(getFilesDir(), "alma");
        // alma-server.tar.gz 解包出的是 alma-server-<ver>-linux-arm64/ 子目录
        File alma = almaRoot;
        File[] subs = almaRoot.listFiles();
        if (subs != null) {
            for (File d : subs) {
                if (d.isDirectory() && new File(d, "bin/alma-server").isFile()) {
                    alma = d;
                    break;
                }
            }
        }
        Log.i(TAG, "alma dir = " + alma.getAbsolutePath());
        File tmp = new File(getFilesDir(), "tmp");
        File home = new File(getFilesDir(), "home");
        File data = new File(getFilesDir(), "data");
        File logs = getExternalFilesDir("logs");
        if (logs == null) logs = new File(getFilesDir(), "logs");
        for (File f : new File[]{tmp, home, data, logs}) f.mkdirs();

        // rootfs 内的工作目录与挂载点必须预先存在
        new File(rootfs, "opt/alma").mkdirs();
        new File(rootfs, "alma-data/home").mkdirs();
        new File(rootfs, "alma-data/alma").mkdirs();
        new File(rootfs, "alma-logs").mkdirs();
        // 主机侧预建目录（proot 内对应 /alma-data/home、/alma-data/alma、/alma-logs）
        new File(home, "").mkdirs();
        new File(data, "").mkdirs();
        new File(logs, "").mkdirs();
        // 直接用绝对路径启动，避免依赖 cd/mkdir/sh 内建命令
        // 局域网端口可配置（LanSettings）；relay 绑 0.0.0.0，是局域网唯一入口
        final int lanPort = LanSettings.enabled(this) ? LanSettings.port(this) : 23017;
        // 自检：relay 脚本与 node 是否就位（缺失时局域网入口会消失）
        File relayJs = new File(alma, "local-relay.js");
        File relayNode = new File(alma, "runtime/node");
        Log.i(TAG, "relay present=" + relayJs.isFile() + " (" + relayJs.length()
                + "B) node=" + relayNode.isFile());
        // 资产随版本更新：以资产长度做标记，长度变了就重新拷贝
        long wantLen = -1;
        try { wantLen = getAssets().openFd("local-relay.js").getLength(); } catch (Throwable ignored) {}
        boolean needCopy = !relayJs.isFile() || (wantLen > 0 && relayJs.length() != wantLen);
        if (needCopy) {
            try {
                copyAsset("local-relay.js", relayJs);
                Log.i(TAG, "relay refreshed to " + relayJs.length() + "B (want " + wantLen + ")");
            } catch (Throwable t) {
                Log.e(TAG, "relay copy failed", t);
            }
        }

        String boot =
                "export HOME=/alma-data/home; "
                + "export ALMA_HEADLESS=1; "
                + "export ALMA_PORT=23001; "
                + "export NODE_OPTIONS=--dns-result-order=ipv4first; "
                + "export PATH=/usr/local/sbin:/usr/local/bin:/usr/sbin:/usr/bin:/sbin:/bin:/alma-data/home/.local/bin; "
                // 中文 locale（否则命令行工具输出中文乱码）
                + "export LANG=zh_CN.UTF-8; "
                + "export LC_ALL=zh_CN.UTF-8; "
                + "export LANGUAGE=zh_CN:en; "
                // 时区（UTC+8）
                + "export TZ=Asia/Shanghai; "
                // CA 证书（否则所有 HTTPS 客户端验不过）
                + "export SSL_CERT_FILE=/etc/ssl/certs/ca-certificates.crt; "
                + "export CURL_CA_BUNDLE=/etc/ssl/certs/ca-certificates.crt; "
                + "export SSL_CERT_DIR=/etc/ssl/certs; "
                // pip/npm 走的证书
                + "export PIP_CERT=/etc/ssl/certs/ca-certificates.crt; "
                + "export NODE_EXTRA_CA_CERTS=/etc/ssl/certs/ca-certificates.crt; "
                // herdr-core 放进 PATH，服务端 binary() 才能找到并自行启动。
                // 官方 install.sh 依赖 curl（rootfs 没有），所以从 assets 预置。
                + "cp /opt/alma/herdr-core /usr/local/bin/herdr-core 2>/dev/null; "
                // node 放进 PATH（报告第1条要 node/npm/npx）
                + "ln -sf /opt/alma/runtime/node /usr/local/bin/node 2>/dev/null; "
                + "ln -sf /opt/alma/npm/bin/npm /usr/local/bin/npm 2>/dev/null; "
                + "ln -sf /opt/alma/npm/bin/npx /usr/local/bin/npx 2>/dev/null; "
                + "ln -sf /usr/bin/python3.12 /usr/local/bin/python3 2>/dev/null; "
                // pip：devtools 层预置了 wheel，无网络幂等安装（不再依赖 ensurepip，Ubuntu 会拒绝装系统 pip）
                + "if ! python3 -c 'import pip' 2>/dev/null; then "
                + "python3 -c \"import zipfile,sysconfig;zipfile.ZipFile('/opt/alma/pip.whl').extractall(sysconfig.get_paths()['purelib'])\" 2>/dev/null; fi; "
                + "chmod +x /usr/local/bin/pip3 2>/dev/null; "
                // 生成 zh_CN.UTF-8 locale（locale-gen 若不可用则跳过）
                + "if [ -f /usr/sbin/locale-gen ]; then locale-gen zh_CN.UTF-8 2>/dev/null; fi; "
                + "chmod +x /usr/local/bin/herdr-core 2>/dev/null; "
                // 补 Android 附加组名，否则每个新 shell 都刷 groups: cannot find name for group ID
                + "for l in inet:x:3003: everybody:x:9997: aid-inet:x:20721: aid-everybody:x:50721: ext_obb_rw:x:99909997:; do g=$(echo $l|cut -d: -f3); grep -q \":x:${g}:\" /etc/group || echo \"$l\" >> /etc/group; done; "
                + "exec /bin/sh /opt/alma/bin/alma-server --port 23001 --data-dir /alma-data/alma";

        ProcessBuilder pb = new ProcessBuilder(
                lib + "/libproot.so",
                "-r", rootfs.getAbsolutePath(),
                "-0", "-w", "/opt/alma", "--kill-on-exit",
                "-b", "/dev", "-b", "/proc", "-b", "/sys",
                "-b", tmp.getAbsolutePath() + ":/tmp",
                "-b", home.getAbsolutePath() + ":/alma-data/home",
                "-b", data.getAbsolutePath() + ":/alma-data/alma",
                "-b", logs.getAbsolutePath() + ":/alma-logs",
                "-b", alma.getAbsolutePath() + ":/opt/alma",
                "/bin/sh", "-c", boot);
        pb.environment().put("LD_LIBRARY_PATH", lib);
        pb.environment().put("PROOT_LOADER", lib + "/libproot_loader.so");
        pb.environment().put("PROOT_TMP_DIR", tmp.getAbsolutePath());
        pb.environment().put("PROOT_NO_SECCOMP", "1");
        pb.environment().put("HOME", home.getAbsolutePath());
        pb.redirectErrorStream(true);

        Log.i(TAG, "launch proot (cmd=" + pb.command() + ")");
        sProc = pb.start();
        launchRelayAsync();
        // 输出直接进 logcat（便于远程诊断）
        new Thread(new Runnable() { public void run() {
            try (java.io.InputStream in = sProc.getInputStream()) {
                byte[] b = new byte[4096];
                int n;
                while ((n = in.read(b)) > 0) {
                    String line = new String(b, 0, n, "UTF-8");
                    Log.i(TAG, "proot: " + line.trim());
                }
                Log.w(TAG, "proot stdout EOF, exit=" + sProc.exitValue());
            } catch (Exception e) {
                Log.w(TAG, "logpump: " + e);
            }
        }}, "logpump").start();
    }

    // ---- 通知 ----

    private void startFg(String text) { notify(1, text, true); }
    private void setFg(String text) { notify(1, text, true); }
    private void setProgress(String text) { notify(1, text, true); }
    private void notify(int id, String text, boolean ongoing) {
        NotificationManager nm = (NotificationManager) getSystemService(NOTIFICATION_SERVICE);
        // 最小化通知：不给点击意图、低优先级（系统要求 FGS 必须有通知，但尽量不打扰）
        Notification.Builder b = new Notification.Builder(this, CH)
                .setSmallIcon(android.R.drawable.stat_notify_sync)
                .setContentTitle("Alma 本机服务")
                .setOngoing(ongoing);
        // 显式声明 specialUse 类型：Android 15 对 dataSync 有 6h/24h 配额，
        // 耗尽后 startForeground 会抛 ForegroundServiceStartNotAllowedException。
        // specialUse 无此配额限制。
        try {
            startForeground(id, b.build(),
                    android.content.pm.ServiceInfo.FOREGROUND_SERVICE_TYPE_SPECIAL_USE);
        } catch (Throwable t) {
            // 老系统或类型不被接受时退回普通调用
            Log.w(TAG, "startForeground(specialUse) failed, fallback", t);
            startForeground(id, b.build());
        }
    }

    /** 判断已托管的 proot 是否还活着。 */
    private static boolean isAlive(Process p) {
        try {
            p.exitValue();
            return false;                       // 已退出
        } catch (IllegalThreadStateException e) {
            return true;                        // 仍在运行
        } catch (Throwable t) {
            return false;
        }
    }

    @Override
    public void onDestroy() {
        watchRunning = false;
        if (wakeLock != null && wakeLock.isHeld()) {
            try { wakeLock.release(); } catch (Throwable ignored) {}
        }
        BOOTING.set(false);
        Process p = sProc;
        if (p != null) p.destroy();
        super.onDestroy();
    }

    private String http(String url, String method, String body, int timeout) throws Exception {
        java.net.HttpURLConnection c = (java.net.HttpURLConnection) new java.net.URL(url).openConnection();
        c.setConnectTimeout(timeout);
        c.setReadTimeout(timeout);
        c.setRequestMethod(method);
        c.setRequestProperty("Content-Type", "application/json");
        if (body != null) {
            c.setDoOutput(true);
            c.getOutputStream().write(body.getBytes(java.nio.charset.StandardCharsets.UTF_8));
        }
        int code = c.getResponseCode();
        java.io.InputStream in = code >= 400 ? c.getErrorStream() : c.getInputStream();
        java.io.ByteArrayOutputStream bos = new java.io.ByteArrayOutputStream();
        if (in != null) {
            byte[] b = new byte[8192];
            int n;
            while ((n = in.read(b)) > 0) bos.write(b, 0, n);
            in.close();
        }
        String out = new String(bos.toByteArray(), java.nio.charset.StandardCharsets.UTF_8);
        if (code >= 400) throw new Exception("HTTP " + code + ": " + out);
        return out;
    }

    // ---- 看护：切后台被系统清理 / Node bus error 后自动恢复 ----

    private volatile boolean watchRunning;

    private void startWatchdog() {
        if (watchRunning) return;
        watchRunning = true;
        new Thread(new Runnable() { public void run() {
            int fails = 0;
            while (watchRunning) {
                try { Thread.sleep(15000); } catch (InterruptedException e) { return; }
                if (!watchRunning) return;

                // relay 用端口探活（Process 对象跨重启不可靠）
                final int lp2 = LanSettings.enabled(LocalService.this)
                        ? LanSettings.port(LocalService.this) : LanSettings.DEFAULT_PORT;
                if (!portOpen(lp2)) {
                    Log.w(TAG, "watchdog: relay port closed, relaunching");
                    launchRelayAsync();
                    if (waitPort(lp2, 20000)) Log.i(TAG, "watchdog: relay back up");
                    else Log.e(TAG, "watchdog: relay still down");
                }
                // 桌面用端口探活：x11vnc 用 -bg 会 fork，Process 对象必然已退出，
                // 用 isAlive 会误判并反复重启，把正常工作的实例杀掉。
                final int vp = LanSettings.vncPort(LocalService.this);
                if (!portOpen(vp)) {
                    Log.w(TAG, "watchdog: vnc port " + vp + " closed, relaunching desktop");
                    launchDesktopAsync();
                    if (waitPort(vp, 25000)) Log.i(TAG, "watchdog: vnc back up");
                }
                boolean alive = sProc != null && isAlive(sProc) && LocalBootstrap.health();
                if (alive) {
                    if (fails > 0) Log.i(TAG, "watchdog: service recovered");
                    fails = 0;
                    continue;
                }
                fails++;
                Log.w(TAG, "watchdog: unhealthy (" + fails + "/3)");
                if (fails < 3) continue;
                fails = 0;
                restartProot();
            }
        }}, "alma-watchdog").start();
    }

    private void restartProot() {
        try {
            Log.w(TAG, "watchdog: restarting proot");
            if (sProc != null) { try { sProc.destroy(); } catch (Throwable ignored) {} sProc = null; }
            Thread.sleep(1500);
            launchProot();
            int lp = LanSettings.enabled(LocalService.this)
                    ? LanSettings.port(LocalService.this) : LanSettings.DEFAULT_PORT;
            if (!portOpen(lp)) {
                launchRelayAsync();
                waitPort(lp, 30000);
            }
            setFg("Alma 服务运行中 · 本机就绪");
            for (int i = 0; i < 40; i++) {
                if (LocalBootstrap.health()) {
                    Log.i(TAG, "watchdog: service back online");
                    setFg("Alma 服务运行中 · 本机就绪");
                    LocalBootstrap.pairSelf(this);
                    return;
                }
                try { Thread.sleep(2000); } catch (InterruptedException e) { return; }
            }
            Log.e(TAG, "watchdog: restart timed out");
        } catch (Throwable t) {
            Log.e(TAG, "watchdog restart failed", t);
        }
    }

    private void copyAsset(String asset, File dest) throws Exception {
        dest.getParentFile().mkdirs();
        try (java.io.InputStream is = getAssets().open(asset);
             java.io.FileOutputStream os = new java.io.FileOutputStream(dest)) {
            byte[] b = new byte[65536];
            int n;
            while ((n = is.read(b)) > 0) os.write(b, 0, n);
        }
    }

    // ---- 局域网 relay：独立 proot 实例，避免随主服务一起被清理 ----

    private static volatile Process sRelay;

    private void launchRelayAsync() {
        final File almaFinal;
        try {
            almaFinal = almaDir();
        } catch (Throwable t) { Log.w(TAG, "relay: alma dir resolve failed", t); return; }
        File relay = new File(almaFinal, "local-relay.js");
        File node = new File(almaFinal, "runtime/node");
        // 脚本随版本更新：每次覆盖（几 KB，无成本）
        try {
            java.io.InputStream in = getAssets().open("local-relay.js");
            java.io.ByteArrayOutputStream bo = new java.io.ByteArrayOutputStream();
            byte[] bb = new byte[8192];
            int nn;
            while ((nn = in.read(bb)) > 0) bo.write(bb, 0, nn);
            in.close();
            byte[] want = bo.toByteArray();
            boolean same = relay.isFile() && relay.length() == want.length;
            if (!same) {
                try (java.io.FileOutputStream os = new java.io.FileOutputStream(relay)) {
                    os.write(want);
                }
                Log.i(TAG, "relay script refreshed to " + want.length + "B");
            }
        } catch (Throwable t) {
            Log.w(TAG, "relay script refresh failed", t);
        }
        if (!relay.isFile() || !node.isFile()) {
            Log.w(TAG, "relay: missing js=" + relay.isFile() + " node=" + node.isFile());
            return;
        }
        final int port = LanSettings.enabled(this)
                ? LanSettings.port(this) : LanSettings.DEFAULT_PORT;
        if (portOpen(port)) {
            Log.i(TAG, "relay port " + port + " already open, skip launch");
            return;
        }
        new Thread(new Runnable() { public void run() {
            try {
                File rootfs = new File(getFilesDir(), "rootfs");
                File logs = getExternalFilesDir("logs");
                if (logs == null) logs = new File(getFilesDir(), "logs");
                String lib = getApplicationInfo().nativeLibraryDir;
                // 独立的 proot 实例：不带 --kill-on-exit，主服务退出时它不受影响
                java.util.List<String> cmd = new java.util.ArrayList<>();
                cmd.add(lib + "/libproot.so");
                cmd.add("-r"); cmd.add(rootfs.getAbsolutePath());
                cmd.add("-0"); cmd.add("-w"); cmd.add("/opt/alma");
                for (String[] b : new String[][]{{"/dev", "/dev"}, {"/proc", "/proc"}, {"/sys", "/sys"}}) {
                    cmd.add("-b"); cmd.add(b[0]);
                }
                cmd.add("-b"); cmd.add(almaFinal.getAbsolutePath() + ":/opt/alma");
                cmd.add("-b"); cmd.add(logs.getAbsolutePath() + ":/alma-logs");
                cmd.add("/bin/sh"); cmd.add("-c");
                cmd.add("/opt/alma/runtime/node /opt/alma/local-relay.js " + port
                        + " >>/alma-logs/relay.log 2>&1");

                ProcessBuilder pb = new ProcessBuilder(cmd);
                pb.redirectErrorStream(true);
                String libPath = lib + ":" + lib + "/libproot";
                pb.environment().put("LD_LIBRARY_PATH", libPath);
                pb.environment().put("PROOT_LOADER", lib + "/libproot_loader.so");
                pb.environment().put("PROOT_NO_SECCOMP", "1");
                File tmp = new File(getFilesDir(), "tmp");
                tmp.mkdirs();
                pb.environment().put("PROOT_TMP_DIR", tmp.getAbsolutePath());

                sRelay = pb.start();
                Log.i(TAG, "relay proot launched on port " + port);
                // 吃掉输出，防止缓冲区填满阻塞
                try (java.io.InputStream in = sRelay.getInputStream()) {
                    byte[] b = new byte[4096];
                    while (in.read(b) > 0) { /* drain */ }
                }
                Log.w(TAG, "relay proot exited");
            } catch (Throwable t) {
                Log.e(TAG, "relay launch failed", t);
            }
        }}, "alma-relay").start();
    }

    private static File sAlmaDir;

    private File almaDir() {
        if (sAlmaDir != null && sAlmaDir.isDirectory()) return sAlmaDir;
        File root = new File(getFilesDir(), "alma");
        File[] subs = root.listFiles();
        if (subs != null) {
            for (File d : subs) {
                if (d.isDirectory() && new File(d, "bin/alma-server").isFile()) {
                    sAlmaDir = d;
                    return d;
                }
            }
        }
        sAlmaDir = root;
        return root;
    }

    // ---------- 桌面层（Xvfb + x11vnc + noVNC）----------

    private static final String DESKTOP_MARK_PREFIX = ".alma-desktop-";
    private static volatile Process sXvfb, sX11vnc, sDesktopApps;

    /** 安装桌面应用层（openbox WM + xterm 库 + 中文字体），幂等。
     *  desktop.tar.gz 是 117 个 arm64 deb 解压后的文件，绕开 proot 里卡住的 dpkg configure。 */
    private static final String DESKTOP_APPS_MARK = ".desktop-apps-v1";
    private void installDesktopAppsLayer() {
        File rootfs = new File(getFilesDir(), "rootfs");
        File mark = new File(rootfs, DESKTOP_APPS_MARK);
        if (mark.isFile()) { Log.i(TAG, "desktop apps layer present"); return; }
        Log.i(TAG, "installing desktop apps layer");
        try (java.io.InputStream is = getAssets().open("desktop.tar.gz")) {
            TarGz.extract(is, -1, rootfs, null);
            mark.createNewFile();
            Log.i(TAG, "desktop apps layer installed");
        } catch (Throwable t) {
            Log.e(TAG, "desktop apps layer install failed", t);
        }
    }

    /** 把 x11layer.tar.gz 解压进 rootfs，把 noVNC 放到 alma 目录。幂等。 */
    private void installDesktopLayer() {
        File rootfs = new File(getFilesDir(), "rootfs");
        long xLen = -1;
        try { xLen = getAssets().openFd("x11layer.tar.gz").getLength(); } catch (Throwable ignored) {}
        // 用资产长度做标记：只有 APK 内资产变化时才重新解压。
        // 之前每次解压 17MB 要 1-2 秒，是启动慢的主要来源之一。
        File mark = new File(rootfs, DESKTOP_MARK_PREFIX + xLen);
        boolean needX = !mark.isFile();
        if (needX) {
            File[] old = rootfs.listFiles();
            if (old != null) for (File f : old) {
                if (f.getName().startsWith(DESKTOP_MARK_PREFIX)) f.delete();
            }
        }
        if (needX) {
            Log.i(TAG, "installing x11 layer into rootfs");
            try (java.io.InputStream is = getAssets().open("x11layer.tar.gz")) {
                TarGz.extract(is, -1, rootfs, null);
                mark.createNewFile();
                Log.i(TAG, "x11 layer installed");
                // 自检：关键可执行文件是否就位，并写入可读的档
                StringBuilder sb = new StringBuilder();
                String nl = System.getProperty("line.separator");
                for (String bin : new String[]{"usr/bin/Xvfb", "usr/bin/x11vnc", "usr/bin/xterm"}) {
                    File f = new File(rootfs, bin);
                    sb.append(bin).append(" exists=").append(f.isFile())
                      .append(" len=").append(f.length())
                      .append(" canExec=").append(f.canExecute()).append(nl);
                }
                for (String dir : new String[]{"usr/bin", "usr/lib/aarch64-linux-gnu"}) {
                    File d = new File(rootfs, dir);
                    String[] list = d.list();
                    int cnt = (list == null) ? -1 : list.length;
                    sb.append(dir).append(" entries=").append(cnt).append(nl);
                }
                Log.i(TAG, "desktop selfcheck: " + sb);
                File diag = new File(getExternalFilesDir("logs") != null
                        ? getExternalFilesDir("logs") : getFilesDir(), "desktop-check.txt");
                try (java.io.FileOutputStream os = new java.io.FileOutputStream(diag)) {
                    os.write(sb.toString().getBytes("UTF-8"));
                } catch (Throwable ignored) {}
            } catch (Throwable t) {
                Log.e(TAG, "x11 layer install failed", t);
            }
        }

        File novnc = new File(almaDir(), "novnc");
        File vncHtml = new File(novnc, "vnc.html");
        if (!vncHtml.isFile()) {
            Log.i(TAG, "installing novnc");
            try (java.io.InputStream is = getAssets().open("novnc.tar.gz")) {
                novnc.mkdirs();
                TarGz.extract(is, -1, novnc, null);
                Log.i(TAG, "novnc installed, vnc.html=" + vncHtml.isFile()
                        + " size=" + vncHtml.length());
            } catch (Throwable t) {
                Log.e(TAG, "novnc install failed", t);
            }
        }
    }

    /** 拉起 Xvfb + x11vnc（独立 proot，不随主服务退出而消失）。 */
    private void launchDesktopAsync() {
        File rootfs = new File(getFilesDir(), "rootfs");
        // 直接看二进制在不在，不依赖标记文件。
        // Xtigervnc（X server + VNC 一体，tiny_container 同款）存在时替代 Xvfb+x11vnc 双进程
        final boolean tiger = new File(rootfs, "usr/bin/Xtigervnc").isFile();
        if (!tiger && !new File(rootfs, "usr/bin/Xvfb").isFile()) {
            Log.w(TAG, "desktop binaries missing, skip X");
            return;
        }
        final File alma = almaDir();
        final File logs = getExternalFilesDir("logs");
        final int vncPort = LanSettings.vncPort(this);

        new Thread(new Runnable() { public void run() {
            try {
                String lib = getApplicationInfo().nativeLibraryDir;
                File tmp = new File(getFilesDir(), "tmp");
                tmp.mkdirs();

                java.util.List<String> base = new java.util.ArrayList<>();
                base.add(lib + "/libproot.so");
                base.add("-r"); base.add(rootfs.getAbsolutePath());
                base.add("-0");
                base.add("--kill-on-exit");
                base.add("-b"); base.add("/dev");
                base.add("-b"); base.add("/proc");
                base.add("-b"); base.add("/sys");
                // 共享 /tmp：Termux:X11 的 X socket 就落在 files/tmp/.X11-unix
                base.add("-b"); base.add(tmp.getAbsolutePath() + ":/tmp");
                base.add("-b"); base.add(alma.getAbsolutePath() + ":/opt/alma");
                if (logs != null) { base.add("-b"); base.add(logs.getAbsolutePath() + ":/alma-logs"); }

                // Termux:X11 原生窗口：AddExtensions failed 待查（native EGL 层），暂用 Xvfb+noVNC
                // X11Host.start(LocalService.this, ":0");

                // Xvfb：1280x720x24 虚拟屏（:1）——仅 Xtigervnc 未装时使用（tiger 走一体化路径）
                if (!tiger) {
                java.util.List<String> x = new java.util.ArrayList<>(base);
                x.add("/bin/sh"); x.add("-c");
                x.add("export PATH=/usr/local/sbin:/usr/local/bin:/usr/sbin:/usr/bin:/sbin:/bin; "
                        + "export LD_LIBRARY_PATH=/usr/lib/aarch64-linux-gnu:/lib/aarch64-linux-gnu:"
                        + "/usr/lib:/lib; "
                        + "export XDG_RUNTIME_DIR=/tmp; "
                        + "export XKB_CONFIG_ROOT=/usr/share/X11/xkb; "
                        + "Xvfb :1 -screen 0 1280x720x24 -nolisten tcp -nolock -ac "
                        + ">/alma-logs/xvfb.log 2>&1");
                ProcessBuilder pbx = new ProcessBuilder(x);
                pbx.redirectErrorStream(true);
                applyProotEnv(pbx, lib, tmp);
                sXvfb = pbx.start();
                Log.i(TAG, "Xvfb launched");
                drain(sXvfb, "xvfb");

                Thread.sleep(2500);

                // x11vnc：桥接 :1 到 5900
                java.util.List<String> v = new java.util.ArrayList<>(base);
                v.add("/bin/sh"); v.add("-c");
                v.add("export PATH=/usr/local/sbin:/usr/local/bin:/usr/sbin:/usr/bin:/sbin:/bin; "
                        + "export LD_LIBRARY_PATH=/usr/lib/aarch64-linux-gnu:/lib/aarch64-linux-gnu:"
                        + "/usr/lib:/lib; "
                        + "export HOME=/root; "
                        // -forever 常驻；-shared 多客户端；-nopw 无密码；
                        // -noxdamage 避免 Xdamage 缺失；-repeat 允许按键连发
                        + "x11vnc -display :1 -forever -shared -nopw -rfbport " + vncPort
                        + " -noxdamage -noshm -repeat -norc -o /alma-logs/x11vnc.log "
                        + ">/alma-logs/x11vnc.out 2>&1");
                ProcessBuilder pbv = new ProcessBuilder(v);
                pbv.redirectErrorStream(true);
                applyProotEnv(pbv, lib, tmp);
                sX11vnc = pbv.start();
                Log.i(TAG, "x11vnc launched on " + vncPort);
                drain(sX11vnc, "x11vnc");
                } // end !tiger

                java.util.List<String> a = new java.util.ArrayList<>(base);
                a.add("/bin/sh"); a.add("-c");
                a.add("export PATH=/usr/local/sbin:/usr/local/bin:/usr/sbin:/usr/bin:/sbin:/bin; "
                        + "export LD_LIBRARY_PATH=/usr/lib/aarch64-linux-gnu:/lib/aarch64-linux-gnu:/usr/lib:/lib; "
                        + "export DISPLAY=:1; export HOME=/root; "
                        + "export LANG=zh_CN.UTF-8; export LC_ALL=zh_CN.UTF-8; "
                        + "for l in inet:x:3003: everybody:x:9997: aid-inet:x:20721: aid-everybody:x:50721: ext_obb_rw:x:99909997:; do g=$(echo $l|cut -d: -f3); grep -q \":x:${g}:\" /etc/group || echo \"$l\" >> /etc/group; done; "
                        // rootfs 裁剪版缺 dpkg-deb（dpkg unpack 内部 fork 它）；
                        // 电脑侧预置 dpkgfix.tar.gz（dpkg-deb+libbz2/lzma/md/z/zstd）push 到 logs 目录，
                        // 这里解包补齐——adb/电脑随时可更新此 tar 升级工具链
                        + "if [ ! -x /usr/bin/dpkg-deb ] && [ -f /alma-logs/dpkgfix.tar.gz ]; then "
                        + "tar xzf /alma-logs/dpkgfix.tar.gz -C / 2>>/alma-logs/apt.log && chmod +x /usr/bin/dpkg-deb /usr/bin/dpkg 2>/dev/null; "
                        + "echo '[dpkgfix] dpkg-deb installed' >>/alma-logs/apt.log 2>&1; fi; "
                        // LXQt 桌面 + 浏览器一次性安装（幂等：有 startlxqt/falkon 就跳过）。
                        // apt-get install -d 只下载不解包，绕开 proot 里 dpkg configure 卡死；
                        // deb 用 dpkg-deb -x 纯解包到 /（不写 dpkg status）。
                        + "if [ ! -x /usr/bin/startlxqt ] || [ ! -x /usr/bin/falkon ]; then "
                        + "echo '[desktop] installing lxqt+falkon...' >>/alma-logs/apt.log 2>&1; "
                        // dpkg interrupted（updates 残留事务）会让 apt 全部报错；先清锁和事务
                        + "rm -f /var/lib/dpkg/updates/* /var/lib/dpkg/lock* /var/cache/apt/archives/lock 2>/dev/null; "
                        + "timeout 90 dpkg --configure -a >>/alma-logs/apt.log 2>&1 || true; "
                        + "rm -f /var/lib/dpkg/updates/* /var/lib/dpkg/lock* 2>/dev/null; "
                        + "apt-get update >>/alma-logs/apt.log 2>&1; "
                        // 真安装：unpack 阶段完成即文件就位；configure 卡死（EPERM）由 timeout 断掉，不影响文件
                        + "timeout 600 apt-get install -y --no-install-recommends lxqt-session lxqt-panel lxqt-config lxqt-runner lxqt-notificationd lxqt-about pcmanfm-qt qterminal lxqt-archiver lxqt-themes lxqt-core openbox dbus falkon fonts-wqy-zenhei tigervnc-standalone-server >>/alma-logs/apt.log 2>&1 || true; "
                        + "rm -f /var/lib/dpkg/updates/* /var/lib/dpkg/lock* /var/lib/dpkg/lock-frontend 2>/dev/null; "
                        + "ldconfig 2>/dev/null; "
                        + "echo '[desktop] done' >>/alma-logs/apt.log 2>&1; "
                        // 安装完成标记（app 侧 files/alma = /opt/alma）：自愈逻辑据此判断 rootfs 是否健康
                        + "if [ -x /usr/bin/startlxqt ] && [ -x /usr/bin/falkon ] && [ -x /usr/bin/Xtigervnc ]; then touch /opt/alma/.desktop-ok; fi; fi; "
                        + "pkill -x openbox 2>/dev/null; pkill -x xterm 2>/dev/null; "
                        // 清掉 force-stop 后可能残留的旧 LXQt 进程（挂在已死的旧 Xvfb 上）。
                        // 全部用 -x 精确进程名：脚本文本本身是 sh -c 的命令行参数（含 lxqt 字样），
                        // 任何 pkill -f 模式都会连父进程一起杀，导致桌面栈静默死亡
                        + "for p in lxqt-session lxqt-panel lxqt-runner lxqt-notificationd lxqt-globalkeysd lxqt-config lxqt-policykit-agent pcmanfm-qt qterminal dbus-daemon; do pkill -x \"$p\" 2>/dev/null; done; sleep 1; "
                        // 诊断：每次桌面栈启动记录 LXQt 关键文件状态
                        + "ls -la /usr/bin/startlxqt /usr/bin/dbus-run-session /usr/bin/falkon /usr/bin/pcmanfm-qt >>/alma-logs/diag.log 2>&1; "
                        + "echo \"usrbin_total=$(ls /usr/bin 2>/dev/null | wc -l) qtfiles=$(ls /usr/bin 2>/dev/null | grep -cE 'lxqt|falkon|pcmanfm|qterminal') debs=$(ls /var/cache/apt/archives/*.deb 2>/dev/null | wc -l)\" >>/alma-logs/diag.log 2>&1; "
                        + "ls /var/cache/apt/archives/ 2>/dev/null | head -5 >>/alma-logs/diag.log 2>&1; "
                        + "df -h / >>/alma-logs/diag.log 2>&1; "
                        + "echo \"---- diag $(date) ----\" >>/alma-logs/diag.log 2>&1; "
                        // tiger 路径：Xtigervnc 一体机（X server + VNC 同进程，tiny 同款）。
                        // VNC 走 TCP 5900（localhost），X11 客户端走 /tmp/.X11-unix unix socket
                        + "if [ -x /usr/bin/Xtigervnc ]; then "
                        + "export XDG_RUNTIME_DIR=/tmp; export XKB_CONFIG_ROOT=/usr/share/X11/xkb; "
                        + "mkdir -p /tmp/.X11-unix /tmp/xdg; chmod 700 /tmp/xdg; "
                        + "rm -f /tmp/.X1-lock /tmp/.X11-unix/X1; "
                        + "Xtigervnc :1 -rfbport 5900 -geometry 1280x720 -depth 24 "
                        + "-SecurityTypes None -alwaysshared -localhost -nolisten tcp "
                        + ">/alma-logs/tigervnc.log 2>&1 & "
                        + "for i in $(seq 1 100); do [ -S /tmp/.X11-unix/X1 ] && break; sleep 0.2; done; "
                        + "echo '[tiger] Xtigervnc up' >>/alma-logs/diag.log 2>&1; fi; "
                        // LXQt 主桌面（dbus 会话包裹；失败回退 openbox，保证桌面非空）
                        + "if command -v dbus-run-session >/dev/null 2>&1 && [ -x /usr/bin/startlxqt ]; then "
                        + "(while true; do dbus-run-session -- startlxqt >/alma-logs/lxqt.log 2>&1; echo \"lxqt exit=$?\" >>/alma-logs/lxqt.log; sleep 1; done) & "
                        + "else "
                        + "(while true; do [ -x /usr/bin/startlxqt ] && startlxqt >/alma-logs/lxqt.log 2>&1 || openbox >/alma-logs/openbox.log 2>&1; sleep 1; done) & "
                        + "fi; "
                        + "sleep 4; "
                        // 桌面里放一个终端 + OnlyOffice 一键安装脚本入口
                        + "cat > /usr/local/bin/install-office <<'EOFX'\n"
                        + "#!/bin/sh\n"
                        + "set -e\n"
                        + "echo '下载 OnlyOffice (~260MB)...'\n"
                        + "wget -q https://download.onlyoffice.com/install/desktop/editors/linux/onlyoffice-desktopeditors_arm64.deb -O /tmp/office.deb\n"
                        + "dpkg-deb -x /tmp/office.deb / 2>/dev/null || apt-get install -y --no-install-recommends -d patchelf >>/alma-logs/apt.log 2>&1 && for f in /var/cache/apt/archives/*.deb; do dpkg-deb -x \"$f\" / 2>/dev/null; done\n"
                        + "for f in /var/cache/apt/archives/patchelf*.deb /var/cache/apt/archives/*patchelf*.deb; do [ -f \"$f\" ] && dpkg-deb -x \"$f\" / 2>/dev/null; done\n"
                        + "command -v patchelf >/dev/null 2>&1 && patchelf --clear-execstack /opt/onlyoffice/desktopeditors/libQt5Gui.so.5 2>/dev/null\n"
                        + "rm -f /tmp/office.deb\n"
                        + "echo 'OnlyOffice 安装完成'\n"
                        + "EOFX\n"
                        + "chmod +x /usr/local/bin/install-office; "
                        + "(while true; do qterminal >/alma-logs/xterm.log 2>&1 || xterm -geometry 90x24+80+60 -bg '#1e1e1e' -fg '#d0d0d0' "
                        + "-fa 'WenQuanYi Zen Hei' -fs 14 >/alma-logs/xterm.log 2>&1; sleep 2; done) & "
                        + "wait");
                ProcessBuilder pba = new ProcessBuilder(a);
                pba.redirectErrorStream(true);
                applyProotEnv(pba, lib, tmp);
                sDesktopApps = pba.start();
                Log.i(TAG, "desktop apps (openbox+xterm) launched");
                drain(sDesktopApps, "desktop-apps");
            } catch (Throwable t) {
                Log.e(TAG, "desktop launch failed", t);
            }
        }}, "alma-desktop").start();
    }

    private void applyProotEnv(ProcessBuilder pb, String lib, File tmp) {
        String libPath = lib + ":" + lib + "/libproot";
        pb.environment().put("LD_LIBRARY_PATH", libPath);
        pb.environment().put("PROOT_LOADER", lib + "/libproot_loader.so");
        pb.environment().put("PROOT_NO_SECCOMP", "1");
        pb.environment().put("PROOT_TMP_DIR", tmp.getAbsolutePath());
    }

    /** 递归删除目录（rootfs 自愈用）。 */
    private static void deleteRecursive(File f) {
        if (f.isDirectory()) {
            File[] c = f.listFiles();
            if (c != null) for (File x : c) deleteRecursive(x);
        }
        f.delete();
    }

    private void drain(final Process p, final String tag) {
        new Thread(new Runnable() { public void run() {
            try (java.io.InputStream in = p.getInputStream()) {
                byte[] b = new byte[4096];
                while (in.read(b) > 0) { /* discard */ }
            } catch (Throwable ignored) {}
        }}, tag + "-drain").start();
    }

    /** 探测某端口是否有服务在听（真连接，比 Process 对象可靠）。 */
    /** 供设置页等服务外部模块探活。 */
    public static boolean isPortOpenPublic(int port) { return portOpen(port); }

    private static boolean portOpen(int port) {
        java.net.Socket s = null;
        try {
            s = new java.net.Socket();
            s.connect(new java.net.InetSocketAddress("127.0.0.1", port), 600);
            return true;
        } catch (Throwable t) {
            return false;
        } finally {
            if (s != null) try { s.close(); } catch (Throwable ignored) {}
        }
    }

    /** 等待端口起来，最多 timeoutMs。 */
    private static boolean waitPort(int port, long timeoutMs) {
        long end = System.currentTimeMillis() + timeoutMs;
        while (System.currentTimeMillis() < end) {
            if (portOpen(port)) return true;
            try { Thread.sleep(300); } catch (InterruptedException e) { return false; }
        }
        return false;
    }

    /** 部署 herdr-core（静态 aarch64 二进制，agent 本机运行核心）。 */
    private void installHerdrCore(File alma) {
        File hc = new File(alma, "herdr-core");
        try {
            long want = -1;
            try { want = getAssets().openFd("herdr-core").getLength(); } catch (Throwable ignored) {}
            if (!hc.isFile() || (want > 0 && hc.length() != want)) {
                try (java.io.InputStream is = getAssets().open("herdr-core");
                     java.io.FileOutputStream os = new java.io.FileOutputStream(hc)) {
                    byte[] b = new byte[65536];
                    int n;
                    while ((n = is.read(b)) > 0) os.write(b, 0, n);
                }
                hc.setExecutable(true, false);
                Log.i(TAG, "herdr-core deployed " + hc.length() + "B");
            } else {
                hc.setExecutable(true, false);
                Log.i(TAG, "herdr-core present " + hc.length() + "B");
            }
        } catch (Throwable t) {
            Log.e(TAG, "herdr-core deploy failed", t);
        }
    }

    /** 安装开发工具层（git/curl/node 依赖/openssl/ssh/jq/中文 locale/时区/CA/pip/npm）。
     *  标记用固定版本号：AssetManager.openFd().getLength() 对压缩 asset 返回 -1，
     *  旧代码据此命名 .devtools--1，升级后永远跳过重装。版本升级只需改这里的常量。 */
    private static final String DEVTOOLS_MARK = ".devtools-v4";
    private void installDevtoolsLayer() {
        File rootfs = new File(getFilesDir(), "rootfs");
        File mark = new File(rootfs, DEVTOOLS_MARK);
        if (mark.isFile()) {
            Log.i(TAG, "devtools layer present (" + DEVTOOLS_MARK + ")");
            return;
        }
        Log.i(TAG, "installing devtools layer (" + DEVTOOLS_MARK + ")");
        try (java.io.InputStream is = getAssets().open("devtools.tar.gz")) {
            TarGz.extract(is, -1, rootfs, null);
            mark.createNewFile();
            // 清理旧的 devtools 标记
            File[] kids = rootfs.listFiles();
            if (kids != null) for (File f : kids) {
                if (f.getName().startsWith(".devtools-") && !f.equals(mark)) f.delete();
            }
            Log.i(TAG, "devtools layer installed");
        } catch (Throwable t) {
            Log.e(TAG, "devtools install failed", t);
        }
    }
}
