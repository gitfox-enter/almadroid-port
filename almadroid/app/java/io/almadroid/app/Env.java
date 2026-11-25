package io.almadroid.app;

import android.content.Context;

import java.io.File;

/** Central paths & constants. */
public final class Env {
    public static final String SERVER_PORT = "23001";
    public static final String SERVER_URL = "http://127.0.0.1:" + SERVER_PORT;
    /** 本地 relay 转发器端口：官方 App 连这个端口，shim 转发到 23019 E2E。 */
    public static final String LOCAL_RELAY_PORT = "23017";
    public static final String LOCAL_RELAY_URL = "http://127.0.0.1:" + LOCAL_RELAY_PORT;
    public static final String OFFICIAL_PKG = "me.yetone.alma";

    public static int portInt() {
        return Integer.parseInt(SERVER_PORT);
    }

    /** GitHub release repo for alma-server downloads. */
    public static final String RELEASES_API =
            "https://api.github.com/repos/yetone/alma-releases/releases/latest";

    /** Mirrors tried in order; %s replaced by full URL. */
    public static final String[] MIRRORS = {
            "%s",                                                        // direct
            "https://ghfast.top/%s",                                     // known-fast mirror
            "https://gh-proxy.com/%s",
            "https://ghproxy.net/%s",
    };

    public static File filesDir(Context c)      { return c.getFilesDir(); }
    public static File rootfsDir(Context c)     { return new File(filesDir(c), "rootfs"); }
    public static File almaDir(Context c)       { return new File(filesDir(c), "alma"); }
    public static File dataDir(Context c)       { return new File(filesDir(c), "alma-data"); }
    public static File homeDir(Context c)       { return new File(filesDir(c), "guest-home"); }
    public static File tmpDir(Context c)        { return new File(filesDir(c), "tmp"); }
    public static File logsDir(Context c)       { return new File(filesDir(c), "logs"); }
    public static File serverLog(Context c)     { return new File(logsDir(c), "server.log"); }
    public static File importDir(Context c)     { return new File(filesDir(c), "import"); }
    public static File stateDir(Context c)      { return new File(filesDir(c), ".state"); }

    public static File stateFile(Context c, String name) { return new File(stateDir(c), name); }

    public static void mkdirs(Context c) {
        for (File f : new File[]{rootfsDir(c), almaDir(c), dataDir(c), homeDir(c),
                tmpDir(c), logsDir(c), importDir(c), stateDir(c)}) {
            f.mkdirs();
        }
    }

    public static String nativeLibDir(Context c) {
        return c.getApplicationInfo().nativeLibraryDir;
    }
}
