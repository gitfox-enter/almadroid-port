package io.almadroid.server;

import android.content.Context;
import android.content.SharedPreferences;

/**
 * 局域网访问设置。
 * 服务本身已经监听 0.0.0.0，所以同网段任何设备打开
 * http://<手机IP>:<port>/ 就能用网页版 Alma。
 */
public final class LanSettings {
    private static final String PREF = "alma_lan";

    public static final String KEY_PORT = "port";
    public static final String KEY_ENABLED = "enabled";

    /** 默认对外端口。23017 是 relay（同时代理 REST 和 E2E）。 */
    public static final int DEFAULT_PORT = 23017;
    public static final int MIN_PORT = 1024;
    public static final int MAX_PORT = 65535;

    private LanSettings() {}

    private static SharedPreferences sp(Context c) {
        return c.getApplicationContext().getSharedPreferences(PREF, Context.MODE_PRIVATE);
    }

    public static boolean enabled(Context c) {
        return sp(c).getBoolean(KEY_ENABLED, true);
    }

    public static void setEnabled(Context c, boolean on) {
        sp(c).edit().putBoolean(KEY_ENABLED, on).apply();
    }

    public static int port(Context c) {
        int p = sp(c).getInt(KEY_PORT, DEFAULT_PORT);
        return valid(p) ? p : DEFAULT_PORT;
    }

    public static boolean setPort(Context c, int p) {
        if (!valid(p)) return false;
        sp(c).edit().putInt(KEY_PORT, p).apply();
        return true;
    }

    public static final String KEY_VNC_PORT = "vncPort";
    public static final int DEFAULT_VNC_PORT = 5900;

    /** VNC（x11vnc）端口，局域网桌面用。 */
    public static int vncPort(Context c) {
        int p = sp(c).getInt(KEY_VNC_PORT, DEFAULT_VNC_PORT);
        return valid(p) ? p : DEFAULT_VNC_PORT;
    }

    public static boolean setVncPort(Context c, int p) {
        if (!valid(p)) return false;
        sp(c).edit().putInt(KEY_VNC_PORT, p).apply();
        return true;
    }

    /**
     * 桌面地址。本机自访问用回环（127.0.0.1 永不因 DHCP 变化失效）；
     * 给其他设备分享时用 lanUrl()。
     */
    public static String vncUrl(Context c) {
        return "http://127.0.0.1:" + port(c) + "/vnc";
    }

    public static boolean valid(int p) {
        return p >= MIN_PORT && p <= MAX_PORT;
    }

    /** 客户端配对用的本机回环地址（端口跟随设置）。 */
    public static String targetUrl(Context c) {
        return "http://127.0.0.1:" + port(c);
    }

    /**
     * 局域网里其他设备该打开的地址。
     * 手机通常有多个网卡（wlan0 / 热点 / VPN），优先返回私网 IPv4。
     */
    public static String lanUrl(Context c) {
        String ip = lanIpv4();
        return ip == null ? null : "http://" + ip + ":" + port(c);
    }

    /** 枚举本机网卡，挑一个私网 IPv4（非回环、非虚拟网卡）。 */
    public static String lanIpv4() {
        try {
            java.util.Enumeration<java.net.NetworkInterface> nis =
                    java.net.NetworkInterface.getNetworkInterfaces();
            String fallback = null;
            while (nis.hasMoreElements()) {
                java.net.NetworkInterface ni = nis.nextElement();
                if (!ni.isUp() || ni.isLoopback()) continue;
                String name = ni.getName();
                if (name != null && (name.startsWith("tun") || name.startsWith("tap")
                        || name.startsWith("ipsec") || name.startsWith("rmnet"))) continue;
                for (java.util.Enumeration<java.net.InetAddress> as = ni.getInetAddresses();
                     as.hasMoreElements(); ) {
                    java.net.InetAddress a = as.nextElement();
                    if (!(a instanceof java.net.Inet4Address)) continue;
                    String ip = a.getHostAddress();
                    if (ip == null || ip.startsWith("127.")) continue;
                    if (ip.startsWith("192.168.") || ip.startsWith("10.") || is172(ip)) return ip;
                    if (fallback == null) fallback = ip;
                }
            }
            return fallback;
        } catch (Throwable t) {
            return null;
        }
    }

    private static boolean is172(String ip) {
        if (!ip.startsWith("172.")) return false;
        int dot = ip.indexOf('.', 4);
        if (dot < 0) return false;
        try {
            int second = Integer.parseInt(ip.substring(4, dot));
            return second >= 16 && second <= 31;
        } catch (Throwable t) { return false; }
    }
}
