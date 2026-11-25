package io.almadroid.server;

import android.content.Context;
import android.content.Intent;
import android.net.Uri;
import android.util.Log;

import java.lang.reflect.Method;

/** 入口：官方 Application.onCreate 调用 start(ctx)。负责解包→启动服务→触发配对界面。 */
public final class LocalBootstrap {
    private static final String TAG = "AlmaLocal";
    private static volatile boolean started;

    private LocalBootstrap() {}

    private static volatile Context appCtx;

    /** 供 smali 层（官方电脑页注入）获取 context。 */
    public static Context getCtx() {
        return appCtx;
    }

    private static volatile long lastDesktopJumpAt;
    /** 桌面跳转武装位：抽屉「电脑」导航到 R2 页时消费一次；
     *  DesktopActivity 退出时置回 false，避免从桌面返回 R2 页又被立即弹回桌面（死循环）。 */
    private static volatile boolean desktopJumpArmed = true;

    /** smali 注入点：R2.m（官方"电脑"页）入口调用 → 打开 Linux 桌面（noVNC）。
     *  官方电脑页等的是 /ws/screen 远程串流，本机部署永远「屏幕不可用」，
     *  所以点开它就直接跳到本机 DesktopActivity。
     *  armed + 1.5s 防抖：每次从抽屉进只跳一次；从桌面返回 R2 时不再跳。 */
    public static void openDesktopIfAbsent() {
        Context c = appCtx;
        if (c == null || !desktopJumpArmed) return;
        long now = System.currentTimeMillis();
        if (now - lastDesktopJumpAt < 1500) return;
        lastDesktopJumpAt = now;
        desktopJumpArmed = false;   // 消费：从桌面返回 R2 时不会再跳
        try {
            Intent i = new Intent(c, Class.forName("io.almadroid.server.DesktopActivity"));
            i.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_SINGLE_TOP);
            c.startActivity(i);
            Log.i(TAG, "openDesktopIfAbsent -> DesktopActivity");
        } catch (Throwable t) {
            Log.w(TAG, "openDesktopIfAbsent failed", t);
        }
    }

    /** DesktopActivity 退出时调用（smali/Java 均可）：重新武装，
     *  下一次从抽屉点「电脑」仍然直达桌面。 */
    public static void rearmDesktopJump() {
        desktopJumpArmed = true;
        Log.i(TAG, "rearmDesktopJump");
    }

    /**
     * 仅记录上下文，不在此处启动前台服务。
     * Android 12+ 从 Application.onCreate 启动 FGS 会抛
     * ForegroundServiceStartNotAllowedException（应用还在后台）。
     * 真正的启动由 SplashActivity 在其前台时执行。
     */
    public static void start(Context ctx) {
        appCtx = ctx.getApplicationContext();
        Log.i(TAG, "start: context cached (service will start from foreground UI)");
        // 诊断：X11 类在运行时是否真的可加载
        try {
            Class<?> c = Class.forName("com.termux.x11.CmdEntryPoint");
            Log.i(TAG, "X11 diag: CmdEntryPoint loadable, loader=" + c.getClassLoader());
        } catch (Throwable t) {
            Log.w(TAG, "X11 diag: CmdEntryPoint NOT loadable: " + t);
        }
        try {
            Class<?> c = Class.forName("com.termux.x11.MainActivity");
            Log.i(TAG, "X11 diag: MainActivity loadable, loader=" + c.getClassLoader()
                    + " super=" + c.getSuperclass());
        } catch (Throwable t) {
            Log.w(TAG, "X11 diag: MainActivity NOT loadable: " + t);
        }
        try {
            Class<?> c = Class.forName("com.termux.x11.LorieView");
            Log.i(TAG, "X11 diag: LorieView loadable");
        } catch (Throwable t) {
            Log.w(TAG, "X11 diag: LorieView NOT loadable: " + t);
        }
        try {
            Class<?> c = Class.forName("io.almadroid.server.LocalBootstrap");
            Log.i(TAG, "X11 diag: LocalBootstrap loadable, loader=" + c.getClassLoader());
        } catch (Throwable t) {
            Log.w(TAG, "X11 diag: LocalBootstrap NOT loadable: " + t);
        }
    }

    /** 由前台 Activity 调用，安全启动前台服务。 */
    public static void startServiceFromForeground(Context ctx) {
        try {
            Intent i = new Intent(ctx, LocalService.class);
            ctx.startForegroundService(i);
            Log.i(TAG, "foreground service start requested");
        } catch (Throwable t) {
            Log.w(TAG, "startForegroundService failed, trying startService", t);
            try { ctx.startService(new Intent(ctx, LocalService.class)); } catch (Throwable ignored) {}
        }
    }

    private static Intent i(Context c) { return new Intent(c, LocalService.class); }

    /** 服务就绪后由 LocalService 调用：只写配对 prefs。
     *  不再 fire alma://connect deep link——官方收到后会打开「智能体」页，
     *  用户每次启动都落到错误界面。配对 prefs 已写好，MainActivity 自己会连。 */
    public static void pairSelf(Context ctx) {
        try {
            seedPairing(ctx);
        } catch (Throwable t) {
            Log.w(TAG, "seedPairing failed", t);
        }
        Log.i(TAG, "pairSelf done (no deep link)");
    }

    private static String enc(String s) {
        try {
            return java.net.URLEncoder.encode(s, "UTF-8").replace("+", "%20");
        } catch (Exception e) { return s; }
    }

    /** 从 alma-server 读当前配对码（mobileRelay.connectCode）。 */
    private static String connectCode(Context c) {
        try {
            String json = http("http://127.0.0.1:23001/api/settings", "GET", null, 6000);
            org.json.JSONObject root = new org.json.JSONObject(json);
            org.json.JSONObject mr = root.optJSONObject("mobileRelay");
            if (mr != null) return mr.optString("connectCode", "");
        } catch (Throwable t) { Log.w(TAG, "connectCode read failed", t); }
        return "";
    }

    /**
     * 等价于用户点了弹窗里的 Connect：
     * 调官方 p6.a.i(String) 完成配对档案写入，然后覆盖 pairing.target/e2e
     * 为「本机明文直连」模式（target=23001, e2e=false）。
     * p6.a.f() 在 e2e=false 时不需要 connectCode，配对必定成立，
     * 客户端将对 23001 直接走 REST/WS（Go 原生支持 WS /ws/threads 等）。
     */
    private static boolean activatePairing(Context ctx, String rawLink) {
        try {
            Class<?> a = Class.forName("p6.a");
            Method i = a.getMethod("i", String.class);
            i.invoke(null, rawLink.trim());
            Log.i(TAG, "pairing activated via p6.a.i()");

            // 覆盖为本机明文模式
            Class<?> d = Class.forName("t6.d");
            Method f = d.getMethod("f", String.class, String.class);
            f.invoke(null, "pairing.target", "http://127.0.0.1:23001");
            Method g = d.getMethod("g", String.class, boolean.class);
            g.invoke(null, "pairing.e2e", false);
            Log.i(TAG, "pairing switched to local plaintext (target=23001, e2e=false)");
            return true;
        } catch (Throwable t) {
            Log.w(TAG, "activatePairing failed", t);
            return false;
        }
    }

    /** 用官方 App 自己的 prefs API 写配对状态（反射调 t6.d）。 */
    private static void seedPairing(Context ctx) {
        // 本机明文模式：客户端直连 Go 服务(23001) 的 REST/WS，
        // 不走 E2E 隧道。E2E 是跨设备加密用的，本机场景不需要，
        // 且其握手协议（/agent 注册帧 vs /__alma_e2e__ E2E 帧）在本地无法对齐。
        // p6.a.f() 在 e2e=false 时不要求 connectCode，配对必定成立。
        String payload = "[{\"id\":\"local\",\"target\":\"http://127.0.0.1:23001\","
                + "\"e2e\":false,\"name\":\"AlmaLocal\"}]";
        Class<?> d;
        try { d = Class.forName("t6.d"); }
        catch (Throwable t) { Log.w(TAG, "t6.d not found", t); return; }
        try {
            Method f = d.getMethod("f", String.class, String.class);
            f.invoke(null, "pairings.v2", payload);
            f.invoke(null, "pairings.active", "local");

            // p6.a.f() 读取的是加密存储里的 "connectCode"（无后缀）。
            // 若 pairings.v2 里 e2e=true 而 connectCode 为空，该项会被直接丢弃，
            // 于是界面仍认为未配对。必须用 t6.F.d(key, value) 写入。
            String code = readConnectCode();
            if (code != null && code.length() == 43) {
                try {
                    Class<?> fcl = cryptoPrefsClass();
                    Method setter = fcl.getMethod("d", String.class, String.class);
                    // 注意签名是 d(value, account)，参数顺序不能反
                    setter.invoke(null, code, "connectCode");
                    Log.i(TAG, "connectCode (encrypted) written");

                    // p6.a.f() 也看 pairing.target / pairing.e2e（本机明文模式）
                    Method g = d.getMethod("g", String.class, boolean.class);
                    g.invoke(null, "pairing.e2e", false);
                    Method ff = d.getMethod("f", String.class, String.class);
                    ff.invoke(null, "pairing.target", "http://127.0.0.1:23001");
                    Log.i(TAG, "pairing.target/e2e written (local plaintext)");
            try {
                Class<?> fcl2 = cryptoPrefsClass();
                Method getter = fcl2.getMethod("a", String.class);
                Object v = getter.invoke(null, "connectCode");
                Log.i(TAG, "connectCode readback = " + v);
            } catch (Throwable t) {
                Log.w(TAG, "connectCode readback failed", t);
            }
                } catch (Throwable t) {
                    Log.w(TAG, "connectCode write failed", t);
                }
            }
            Log.i(TAG, "pairings seeded via t6.d");
            // 自检：立刻回读，确认官方解析器认可我们写的内容
            Log.i(TAG, "verify pairings -> " + pairingDiag());
            try {
                Class<?> pa = Class.forName("p6.a");
                Method listF = pa.getMethod("f");
                Object list = listF.invoke(null);
                int n = -1;
                if (list instanceof java.util.List) n = ((java.util.List<?>) list).size();
                Log.i(TAG, "p6.a.f() size = " + n);
            } catch (Throwable t) {
                Log.w(TAG, "p6.a.f() probe failed", t);
            }
        } catch (Throwable t) {
            Log.w(TAG, "seed via t6.d failed: " + t);
        }
    }

    /** 确保 mobileRelay E2E 打开（23019 监听器启动条件）。 */
    static void ensureE2E() {
        try {
            String json = http("http://127.0.0.1:23001/api/mobile-relay/e2e",
                    "POST", "{\"enabled\":true}", 8000);
            Log.i(TAG, "e2e ensured");
        } catch (Throwable t) {
            Log.w(TAG, "ensureE2E failed", t);
        }
    }

    private static String http(String url, String method, String body, int timeout) throws Exception {
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

    static boolean health() {
        try {
            String json = http("http://127.0.0.1:23001/api/health", "GET", null, 3000);
            return json.contains("\"ok\"");
        } catch (Throwable t) { return false; }
    }

    /** 只写配对 prefs，不发 deep link（SplashActivity 用它）。 */
    public static void seedPairingOnly(Context ctx) {
        try { seedPairing(ctx); } catch (Throwable t) { Log.w(TAG, "seed failed", t); }
        try { ensureE2E(); } catch (Throwable ignored) {}
    }

    /** 读配对码（供 SplashActivity 构造 deep link）。 */
    public static String readConnectCode() {
        try {
            String json = http("http://127.0.0.1:23001/api/settings", "GET", null, 6000);
            org.json.JSONObject root = new org.json.JSONObject(json);
            org.json.JSONObject mr = root.optJSONObject("mobileRelay");
            if (mr != null) return mr.optString("connectCode", "");
        } catch (Throwable t) { Log.w(TAG, "readConnectCode failed", t); }
        return "";
    }

    /**
     * 让本机服务具备「可直连」条件并返回配对码。
     * 服务端只有在 enabled && serverUrl && connectCode 三者齐备时才会监听 23019
     * （见 server.mjs 的 syncMobileRelayFromSettings → startLanDirect）。
     * 我们不连云端：serverUrl 只是占位，E2E 直连完全走 127.0.0.1。
     */
    public static String ensureLocalPairing(Context ctx) {
        // 0) serverUrl 必须是占位（见 enable 处注释）：指回 relay.invalid，
        //    清掉之前指向本机造成的自连环。
        String cur = readServerUrl();
        if (!"https://relay.invalid".equals(cur)) {
            Log.i(TAG, "serverUrl cur=" + cur + " -> resetting to placeholder");
            try {
                http("http://127.0.0.1:23001/api/mobile-relay/enable", "POST",
                     "{\"serverUrl\":\"https://relay.invalid\",\"e2eEncryption\":true}", 15000);
            } catch (Throwable t) {
                Log.w(TAG, "reset serverUrl failed", t);
            }
        }

        // 1) 先看是否已经有 43 位配对码
        String code = readConnectCode();
        if (isValidCode(code)) {
            seedPairing(ctx);
            ensureE2E();
            // e2e=false 模式不需要 23019（E2E 直连），跳过等待
            activatePairing(ctx, "alma://connect?u=" + enc(LanSettings.targetUrl(ctx))
                    + "&e2e=1&k=" + code);
            return code;
        }

        // 2) 触发一次 enable（幂等）：拿到稳定 connectCode 并启动 lanDirect
        // serverUrl 只是服务端 lanDirect(23019) 的启动开关条件之一。
        // 绝不能指向本机 relay：服务端里的 MobileRelay 客户端也读这个值，
        // 指向本机会形成自连接环（服务端连自己 → /agent 注册帧 → 23019 拒收）。
        // 用不可达占位，让手机客户端走 direct 路线直连 23019（已验证可握手成功）。
        try {
            http("http://127.0.0.1:23001/api/mobile-relay/enable", "POST",
                 "{\"serverUrl\":\"https://relay.invalid\",\"e2eEncryption\":true}", 15000);
            Log.i(TAG, "mobile-relay enabled with placeholder serverUrl (direct route)");
        } catch (Throwable t) {
            Log.w(TAG, "enable failed", t);
        }
        code = readConnectCode();
        if (!isValidCode(code)) {
            Log.e(TAG, "no valid connect code, got: " + code);
            return null;
        }
        seedPairing(ctx);
        ensureE2E();
        // e2e=false 模式不需要 23019，跳过等待
        activatePairing(ctx, "alma://connect?u=" + enc(LanSettings.targetUrl(ctx))
                + "&e2e=1&k=" + code);
        return code;
    }

    /** 43 位 base64url（服务端 rbn() 的校验规则）。 */
    private static boolean isValidCode(String code) {
        if (code == null || code.length() != 43) return false;
        for (int i = 0; i < code.length(); i++) {
            char ch = code.charAt(i);
            boolean ok = (ch >= 'A' && ch <= 'Z') || (ch >= 'a' && ch <= 'z')
                    || (ch >= '0' && ch <= '9') || ch == '-' || ch == '_';
            if (!ok) return false;
        }
        return true;
    }

    /** 等 23019 的 LAN 直连监听器真正起来（最多 ~10s）。 */
    private static void waitLanDirect() {
        for (int i = 0; i < 20; i++) {
            try {
                java.net.Socket s = new java.net.Socket();
                s.connect(new java.net.InetSocketAddress("127.0.0.1", 23019), 400);
                s.close();
                Log.i(TAG, "lanDirect 23019 up");
                return;
            } catch (Throwable ignored) {
                try { Thread.sleep(500); } catch (InterruptedException e) { return; }
            }
        }
        Log.w(TAG, "lanDirect 23019 did not come up");
    }


    /** 自诊断：把官方 prefs 的配对状态读出来（不依赖 root）。 */
    public static String pairingDiag() {
        StringBuilder sb = new StringBuilder();
        try {
            Class<?> d = Class.forName("t6.d");
            Method i = d.getMethod("i", String.class);
            Object v2 = i.invoke(null, "pairings.v2");
            Object act = i.invoke(null, "pairings.active");
            sb.append("v2=").append(v2 == null ? "null" : String.valueOf(v2));
            sb.append(" | active=").append(act == null ? "null" : String.valueOf(act));
        } catch (Throwable t) {
            sb.append("diag err: ").append(t);
        }
        return sb.toString();
    }

    /** 读服务端当前记录的 relay serverUrl。 */
    private static String readServerUrl() {
        try {
            String json = http("http://127.0.0.1:23001/api/settings", "GET", null, 6000);
            org.json.JSONObject root = new org.json.JSONObject(json);
            org.json.JSONObject mr = root.optJSONObject("mobileRelay");
            if (mr != null) return mr.optString("serverUrl", "");
        } catch (Throwable t) { Log.w(TAG, "readServerUrl failed", t); }
        return "";
    }

    /**
     * 加密 prefs 存储类。官方在 4.9 叫 t6.F，4.17 改名为 t6.G，签名一致。
     * 运行时探测，两个版本都能用。
     */
    private static Class<?> cryptoPrefsClass() throws ClassNotFoundException {
        try { return Class.forName("t6.F"); }
        catch (ClassNotFoundException e) { return Class.forName("t6.G"); }
    }

    /** 配对是否已完成（pairings.v2 有条目 + target 指向本机）。 */
    public static boolean isPaired(Context ctx) {
        try {
            String diag = pairingDiag();
            return diag != null
                    && diag.contains("http://127.0.0.1:23001")
                    && !diag.contains("v2=null");
        } catch (Throwable t) {
            return false;
        }
    }
}
