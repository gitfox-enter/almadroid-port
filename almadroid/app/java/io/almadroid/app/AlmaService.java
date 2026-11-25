package io.almadroid.app;

import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.os.Handler;
import android.os.IBinder;
import android.os.Looper;
import android.os.PowerManager;
import android.util.Log;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;

/**
 * Foreground service that keeps alma-server (inside proot) alive.
 */
public class AlmaService extends Service {
    private static final String TAG = "AlmaService";
    private static final String CHANNEL = "alma_server";

    public static final String ACTION_START = "io.almadroid.app.START";
    public static final String ACTION_STOP = "io.almadroid.app.STOP";
    public static final String ACTION_STATUS = "io.almadroid.app.STATUS";

    /** Simple event bus for the UI. */
    public interface StatusListener {
        void onStatus(String state, String detail);
    }
    private static final java.util.concurrent.CopyOnWriteArrayList<StatusListener> LISTENERS =
            new java.util.concurrent.CopyOnWriteArrayList<>();

    public static void addListener(StatusListener l) { LISTENERS.add(l); }
    public static void removeListener(StatusListener l) { LISTENERS.remove(l); }

    private static volatile AlmaService sInstance;

    private Process serverProcess;
    private Thread watchThread;
    private Thread pumpThread;
    private PowerManager.WakeLock wakeLock;
    private final Handler main = new Handler(Looper.getMainLooper());
    private volatile String state = "stopped";
    private volatile String detail = "";
    private long restartCount = 0;

    public static boolean running() {
        AlmaService s = sInstance;
        return s != null && s.serverProcess != null && s.serverProcess.isAlive();
    }

    public static String currentState() {
        AlmaService s = sInstance;
        return s == null ? "stopped" : s.state;
    }

    public static void restartServer() {
        AlmaService s = sInstance;
        if (s != null) s.killProcess();
    }

    public static String logTail(Context c, int maxBytes) {
        try {
            File f = Env.serverLog(c);
            if (!f.isFile()) return "(暂无日志)";
            long len = f.length();
            FileInputStreamPlus p = new FileInputStreamPlus(f);
            long skip = Math.max(0, len - maxBytes);
            p.skipFully(skip);
            byte[] b = new byte[(int) Math.min(len - skip, maxBytes)];
            int n = p.read(b);
            p.close();
            return new String(b, 0, Math.max(0, n), java.nio.charset.StandardCharsets.UTF_8);
        } catch (Exception e) {
            return "(读取日志失败: " + e.getMessage() + ")";
        }
    }

    private static final class FileInputStreamPlus extends java.io.FileInputStream {
        FileInputStreamPlus(File f) throws java.io.FileNotFoundException { super(f); }
        void skipFully(long n) throws IOException {
            while (n > 0) {
                long k = skip(n);
                if (k <= 0) break;
                n -= k;
            }
        }
    }

    @Override
    public void onCreate() {
        super.onCreate();
        sInstance = this;
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        String action = intent == null ? ACTION_START : intent.getAction();
        if (ACTION_STOP.equals(action)) {
            killProcess();
            stopSelf();
            return START_NOT_STICKY;
        }
        startForegroundInternal();
        acquireWakeLock();
        if (serverProcess == null || !serverProcess.isAlive()) {
            startServerLoop();
        }
        return START_STICKY;
    }

    private void startForegroundInternal() {
        NotificationManager nm = (NotificationManager) getSystemService(NOTIFICATION_SERVICE);
        NotificationChannel ch = new NotificationChannel(CHANNEL, "Alma 服务",
                NotificationManager.IMPORTANCE_LOW);
        ch.setDescription("保持 Alma 本地服务运行");
        nm.createNotificationChannel(ch);
        Intent open = new Intent(this, MainActivity.class);
        PendingIntent pi = PendingIntent.getActivity(this, 1, open,
                PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE);
        Notification n = new Notification.Builder(this, CHANNEL)
                .setSmallIcon(android.R.drawable.stat_notify_sync)
                .setContentTitle("AlmaDroid 运行中")
                .setContentText("本地 Alma 服务 · 端口 " + Env.SERVER_PORT)
                .setContentIntent(pi)
                .setOngoing(true)
                .build();
        startForeground(1, n);
    }

    private void acquireWakeLock() {
        if (wakeLock != null && wakeLock.isHeld()) return;
        PowerManager pm = (PowerManager) getSystemService(POWER_SERVICE);
        wakeLock = pm.newWakeLock(PowerManager.PARTIAL_WAKE_LOCK, "AlmaDroid::server");
        wakeLock.setReferenceCounted(false);
        wakeLock.acquire();
    }

    private void setState(String s, String d) {
        state = s;
        detail = d == null ? "" : d;
        Log.i(TAG, "state=" + s + " detail=" + detail);
        for (StatusListener l : LISTENERS) {
            main.post(() -> l.onStatus(s, detail));
        }
    }

    // ---- server lifecycle ----

    private synchronized void killProcess() {
        Process p = serverProcess;
        serverProcess = null;
        if (p != null) {
            p.destroy();
            try { p.waitFor(); } catch (InterruptedException ignored) {}
        }
        Thread w = watchThread;
        if (w != null) w.interrupt();
    }

    private void startServerLoop() {
        killProcess();
        watchThread = new Thread(() -> {
            long backoff = 2000;
            while (!Thread.currentThread().isInterrupted()) {
                try {
                    setState("starting", "启动 proot 环境…");
                    Process p = launchProot();
                    serverProcess = p;
                    pumpLogs(p);
                    setState("running", "PID " + p.hashCode());
                    backoff = 2000;
                    int exit = p.waitFor();
                    if (Thread.currentThread().isInterrupted() || serverProcess == null) break;
                    restartCount++;
                    setState("crashed", "退出码 " + exit + "，" + (backoff / 1000) + "s 后重启");
                    Thread.sleep(backoff);
                    backoff = Math.min(backoff * 2, 30000);
                } catch (InterruptedException e) {
                    break;
                } catch (Exception e) {
                    Log.e(TAG, "launch failed", e);
                    setState("error", e.getMessage());
                    try { Thread.sleep(Math.max(backoff, 5000)); } catch (InterruptedException ie) { break; }
                    backoff = Math.min(backoff * 2, 30000);
                }
            }
            setState("stopped", "");
        }, "alma-watchdog");
        watchThread.start();
    }

    private Process launchProot() throws IOException {
        Context c = this;
        File rootfs = Env.rootfsDir(c);
        File alma = Bootstrap.latestAlmaRoot(Env.almaDir(c));
        if (alma == null) throw new IOException("alma-server 未安装");
        String lib = Env.nativeLibDir(c);
        File tmp = Env.tmpDir(c);
        File home = Env.homeDir(c);
        File data = Env.dataDir(c);
        File logFile = Env.serverLog(c);

        Env.mkdirs(c);

        String boot =
                "set -e\n" +
                "mkdir -p /alma-data/home /alma-data/alma /alma-logs\n" +
                "cd /opt/alma\n" +
                "export HOME=/alma-data/home\n" +
                "export ALMA_HEADLESS=1\n" +
                "export ALMA_PORT=" + Env.SERVER_PORT + "\n" +
                // Android phones typically have no IPv6 route; force IPv4-first DNS
                // so the relay/mobile-relay outbound connects instead of timing out.
                // (Go has Happy Eyeballs by default; Node needs the flag below.)
                "export NODE_OPTIONS=--dns-result-order=ipv4first\n" +
                "export PATH=/usr/local/sbin:/usr/local/bin:/usr/sbin:/usr/bin:/sbin:/bin\n" +
                // 启动本地 relay 转发器：官方 App 直连本机（不经过云端中继）
                "if [ -f ./local-relay.js ]; then\n" +
                "  ( ./runtime/node ./local-relay.js " + Env.LOCAL_RELAY_PORT + " >>/alma-logs/relay.log 2>&1 & )\n" +
                "fi\n" +
                "exec ./bin/alma-server --port " + Env.SERVER_PORT + " --data-dir /alma-data/alma\n";

        ProcessBuilder pb = new ProcessBuilder(
                lib + "/libproot.so",
                "-r", rootfs.getAbsolutePath(),
                "-0",
                "-w", "/opt/alma",
                "--kill-on-exit",
                "-b", "/dev",
                "-b", "/proc",
                "-b", "/sys",
                "-b", tmp.getAbsolutePath() + ":/tmp",
                "-b", home.getAbsolutePath() + ":/alma-data/home",
                "-b", data.getAbsolutePath() + ":/alma-data/alma",
                "-b", logFile.getParentFile().getAbsolutePath() + ":/alma-logs",
                "-b", alma.getAbsolutePath() + ":/opt/alma",
                "/usr/bin/env",
                "-i",
                "HOME=/alma-data/home",
                "PATH=/usr/local/sbin:/usr/local/bin:/usr/sbin:/usr/bin:/sbin:/bin",
                "TERM=xterm-256color",
                "LANG=C.UTF-8",
                "TMPDIR=/tmp",
                "/bin/sh", "-c", boot
        );
        pb.environment().put("LD_LIBRARY_PATH", lib);
        pb.environment().put("PROOT_LOADER", lib + "/libproot_loader.so");
        pb.environment().put("PROOT_TMP_DIR", tmp.getAbsolutePath());
        pb.environment().put("PROOT_NO_SECCOMP", "1");
        pb.environment().put("HOME", home.getAbsolutePath());
        pb.redirectErrorStream(true);
        Log.i(TAG, "launching proot: " + String.join(" ", pb.command()));
        return pb.start();
    }

    private void rotateLogIfNeeded() {
        try {
            File f = Env.serverLog(this);
            if (f.isFile() && f.length() > 2L * 1024 * 1024) {
                File old = new File(f.getParentFile(), "server.log.1");
                old.delete();
                f.renameTo(old);
            }
        } catch (Exception ignored) {
        }
    }

    private void pumpLogs(Process p) {
        rotateLogIfNeeded();
        Thread old = pumpThread;
        if (old != null) old.interrupt();
        final Process fp = p;
        pumpThread = new Thread(() -> {
            try (InputStream in = fp.getInputStream();
                 OutputStream os = new FileOutputStream(Env.serverLog(this), true)) {
                byte[] buf = new byte[8192];
                int n;
                while (!Thread.currentThread().isInterrupted() && (n = in.read(buf)) > 0) {
                    os.write(buf, 0, n);
                    os.flush();
                }
            } catch (IOException ignored) {
            }
        }, "alma-logpump");
        pumpThread.setDaemon(true);
        pumpThread.start();
    }

    @Override
    public void onDestroy() {
        killProcess();
        if (wakeLock != null) {
            try { wakeLock.release(); } catch (Exception ignored) {}
            wakeLock = null;
        }
        if (sInstance == this) sInstance = null;
        super.onDestroy();
    }

    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }
}
