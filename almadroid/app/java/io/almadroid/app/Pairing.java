package io.almadroid.app;

import android.util.Log;

import org.json.JSONObject;

import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.NetworkInterface;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * 生成指向「本机本地 relay」的官方 App 配对链接。
 *
 * 官方 App 的连接路径固定为 ws://host:port/c/&lt;routingHash&gt;/__alma_e2e__
 * 本机 local-relay.js 监听 23017 并桥接到 23019，所以配对链接写 23017 即可
 * 直连本机，完全不走云端中继。
 *
 * 链接格式（官方解析逻辑 见 p110p6.a.h）：
 *   alma://connect?u=<http(s) URL>&e2e=1&k=<code>
 * e2e=1 时必须带 k（配对码）；App 会据此做 E2E 握手。
 */
final class Pairing {
    private static final String TAG = "Pairing";

    interface Callback {
        void onLink(String link, String reason);
    }

    private Pairing() {}

    /** 本机服务的配对码（由 alma-server 的 mobileRelay 设置提供）。 */
    static String connectCode() {
        try {
            JSONObject s = settingsObject();
            JSONObject mr = s.optJSONObject("mobileRelay");
            if (mr != null) {
                String c = mr.optString("connectCode", "");
                if (!c.isEmpty()) return c;
            }
        } catch (Exception e) {
            Log.w(TAG, "connectCode failed: " + e.getMessage());
        }
        return null;
    }

    private static JSONObject settingsObject() throws Exception {
        String json = http(Env.SERVER_URL + "/api/settings", "GET", null);
        return new JSONObject(json);
    }

    /** 确保 mobileRelay 处于「E2E + 本机」状态，让 23019 E2E 监听器启动。 */
    static boolean ensureE2EEnabled() {
        try {
            JSONObject s = settingsObject();
            JSONObject mr = s.optJSONObject("mobileRelay");
            boolean ok = mr != null
                    && mr.optBoolean("enabled", false)
                    && mr.optBoolean("e2eEncryption", false)
                    && !mr.optString("connectCode", "").isEmpty();
            if (ok) return true;
            // 打开 E2E（若 enabled 已 true 只是 e2e 关，单独调 e2e 接口更轻）
            if (mr != null && mr.optBoolean("enabled", false) && !mr.optBoolean("e2eEncryption", false)) {
                http(Env.SERVER_URL + "/api/mobile-relay/e2e", "POST", "{\"enabled\":true}");
                return true;
            }
            // 全新开启：serverUrl 指向本机（local-relay 会代理 REST），E2E 开
            JSONObject body = new JSONObject();
            body.put("serverUrl", Env.LOCAL_RELAY_URL);
            body.put("e2eEncryption", true);
            http(Env.SERVER_URL + "/api/mobile-relay/enable", "POST", body.toString());
            return true;
        } catch (Exception e) {
            Log.w(TAG, "ensureE2EEnabled failed: " + e.getMessage());
            return false;
        }
    }

    /** 本机局域网 IPv4（官方 App 在手机上跑时用 127.0.0.1 其实也行）。 */
    static List<String> localIPv4() {
        List<String> out = new ArrayList<>();
        try {
            for (NetworkInterface ni : Collections.list(NetworkInterface.getNetworkInterfaces())) {
                if (!ni.isUp() || ni.isLoopback()) continue;
                String n = ni.getName();
                if (n != null && (n.startsWith("rmnet") || n.startsWith("dummy") || n.startsWith("tun"))) continue;
                for (java.net.InterfaceAddress ia : ni.getInterfaceAddresses()) {
                    java.net.InetAddress a = ia.getAddress();
                    if (a instanceof java.net.Inet4Address) {
                        out.add(a.getHostAddress());
                    }
                }
            }
        } catch (Exception ignored) {}
        return out;
    }

    /** 生成配对链接。优先 127.0.0.1（同设备），另附局域网地址供参考。 */
    static void buildLink(boolean preferLoopback, Callback cb) {
        new Thread(() -> {
            try {
                if (!AlmaApi.health()) {
                    cb.onLink(null, "服务未就绪");
                    return;
                }
                ensureE2EEnabled();
                String code = connectCode();
                if (code == null || code.isEmpty()) {
                    cb.onLink(null, "服务未提供配对码（mobileRelay 未初始化）");
                    return;
                }
                String host = "127.0.0.1";
                if (!preferLoopback) {
                    List<String> ips = localIPv4();
                    if (!ips.isEmpty()) host = ips.get(0);
                }
                String u = "http://" + host + ":" + Env.LOCAL_RELAY_PORT;
                String link = "alma://connect?u=" + enc(u) + "&e2e=1&k=" + enc(code);
                cb.onLink(link, null);
            } catch (Exception e) {
                cb.onLink(null, AlmaApi.describe(e));
            }
        }, "pairing").start();
    }

    private static String enc(String s) {
        try {
            return java.net.URLEncoder.encode(s, "UTF-8").replace("+", "%20");
        } catch (Exception e) {
            return s;
        }
    }

    private static String http(String urlStr, String method, String body) throws Exception {
        HttpURLConnection c = (HttpURLConnection) new URL(urlStr).openConnection();
        c.setConnectTimeout(6000);
        c.setReadTimeout(10000);
        c.setRequestMethod(method);
        c.setRequestProperty("Content-Type", "application/json");
        if (body != null) {
            c.setDoOutput(true);
            c.getOutputStream().write(body.getBytes(StandardCharsets.UTF_8));
        }
        int code = c.getResponseCode();
        InputStream in = code >= 400 ? c.getErrorStream() : c.getInputStream();
        StringBuilder sb = new StringBuilder();
        if (in != null) {
            byte[] b = new byte[8192];
            int n;
            while ((n = in.read(b)) > 0) sb.append(new String(b, 0, n, StandardCharsets.UTF_8));
            in.close();
        }
        String out = sb.toString();
        if (code >= 400) throw new Exception("HTTP " + code + ": " + out);
        return out;
    }
}