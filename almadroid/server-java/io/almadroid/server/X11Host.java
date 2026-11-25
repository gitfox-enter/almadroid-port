package io.almadroid.server;

import android.content.Context;
import android.content.Intent;
import android.util.Log;
import java.io.File;

/** Termux:X11 宿主接线（移植自 tiny_container 的启动方式）。
 *  X server 跑在 com.termux.x11.CmdEntryPointService（独立 :xserver 进程），
 *  X socket 落在 app files/tmp/.X11-unix/Xn —— 正是 proot 容器 /tmp bind 的目录。 */
public class X11Host {
    private static final String TAG = "AlmaLocal";

    /** 启动 X server。display 形如 ":0"。返回是否成功发起。
     *  幂等：socket 可连接说明 X server 在跑，重复启动会重置 display 踢掉客户端。
     *  实现：不走 CmdEntryPointService（其 lambda 线程无 Looper，二次启动必 NPE 崩进程），
     *  而是在本进程专用线程 Looper.prepare() 后直接调 CmdEntryPoint.main —— tiny_container 同款。 */
    public static boolean start(Context ctx, String display) {
        File sock = socketFile(ctx, display);
        if (sock.exists()) {
            if (socketAlive(sock)) {
                Log.i(TAG, "X server already running (" + display + "), skip re-start");
                return true;
            }
            Log.w(TAG, "stale X socket (server dead), removing " + sock);
            sock.delete();
        }
        try {
            // 与 CmdEntryPointService 相同的 env 传递方式：android.system.Os.setenv（同进程生效，native 可读）
            File files = ctx.getFilesDir();
            android.system.Os.setenv("TMPDIR", files.getAbsolutePath() + "/tmp", true);
            android.system.Os.setenv("XKB_CONFIG_ROOT", files.getAbsolutePath() + "/rootfs/usr/share/X11/xkb", true);
            android.system.Os.setenv("TERMUX_X11_DEBUG", "1", true);
            android.system.Os.setenv("TERMUX_X11_OVERRIDE_PACKAGE", ctx.getPackageName(), true);
            Log.i(TAG, "setenv readback TMPDIR=" + android.system.Os.getenv("TMPDIR"));
            // 走 :xserver 独立进程（我们的 CmdEntryPointService 版本已修 Looper NPE）。
            // 独立进程的 EGL 环境与主进程（Compose）隔离，AddExtensions 才能成功。
            Class<?> svc = Class.forName("com.termux.x11.CmdEntryPointService");
            Intent i = new Intent(ctx, svc);
            i.setAction(field(svc, "ACTION_START"));
            i.putExtra(field(svc, "EXTRA_ARGS"), new String[]{display});
            i.putExtra(field(svc, "EXTRA_ENV_KEYS"), new String[]{"TMPDIR", "XKB_CONFIG_ROOT", "TERMUX_X11_DEBUG", "TERMUX_X11_OVERRIDE_PACKAGE"});
            i.putExtra(field(svc, "EXTRA_ENV_VALUES"), new String[]{
                    files.getAbsolutePath() + "/tmp",
                    files.getAbsolutePath() + "/rootfs/usr/share/X11/xkb",
                    "1", ctx.getPackageName()});
            ctx.startService(i);
            Log.i(TAG, "X server service started (" + display + ")");
            return true;
        } catch (Throwable t) {
            Log.e(TAG, "X server start failed", t);
            return false;
        }
    }

    public static void stop(Context ctx) {
        // CmdEntryPoint 无法优雅停：杀整个进程组的 X server 由 force-stop/进程退出兜底
    }

    /** X socket 文件（等待它的出现来确认 server 就绪）。 */
    public static File socketFile(Context ctx, String display) {
        String num = display.startsWith(":") ? display.substring(1) : display;
        return new File(ctx.getFilesDir(), "tmp/.X11-unix/X" + num);
    }

    /** X server 是否真的在跑（socket 存在且可连接）。 */
    public static boolean isRunning(Context ctx, String display) {
        File f = socketFile(ctx, display);
        return f.exists() && socketAlive(f);
    }

    /** 试连 unix socket 判断 X server 是否真的活着。 */
    private static boolean socketAlive(File sock) {
        try {
            android.net.LocalSocket s = new android.net.LocalSocket();
            s.connect(new android.net.LocalSocketAddress(
                    sock.getAbsolutePath(), android.net.LocalSocketAddress.Namespace.FILESYSTEM), 800);
            s.close();
            return true;
        } catch (Throwable t) {
            return false;
        }
    }

    /** 阻塞等待 socket 就绪（文件出现且可连接）。 */
    public static boolean waitSocket(Context ctx, String display, int timeoutMs) {
        File f = socketFile(ctx, display);
        long dl = System.currentTimeMillis() + timeoutMs;
        while (System.currentTimeMillis() < dl) {
            if (f.exists() && socketAlive(f)) return true;
            try { Thread.sleep(300); } catch (InterruptedException e) { return false; }
        }
        Log.w(TAG, "X socket timeout: " + f);
        return false;
    }

    private static String field(Class<?> c, String n) throws Exception {
        return (String) c.getField(n).get(null);
    }
}
