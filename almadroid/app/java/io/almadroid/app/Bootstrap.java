package io.almadroid.app;

import android.content.Context;
import android.util.Log;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;

/**
 * One-time bootstrap:
 *  1. extract bundled Ubuntu arm64 rootfs from assets
 *  2. obtain alma-server tarball (local import > bundled asset > download with mirrors)
 *  3. extract it, patch DNS + misc rootfs fixes
 */
public final class Bootstrap {
    private static final String TAG = "Bootstrap";
    private static final String FALLBACK_VERSION = "0.4.713";

    public interface Listener {
        void onStage(String message);
        void onProgress(long done, long total, String detail);
        void onDone(boolean ok, String error);
    }

    private final Context ctx;
    private final Listener listener;
    private volatile boolean cancelled = false;

    public Bootstrap(Context ctx, Listener listener) {
        this.ctx = ctx.getApplicationContext();
        this.listener = listener;
    }

    public void cancel() { cancelled = true; }

    public boolean isRootfsReady() {
        File bash = new File(Env.rootfsDir(ctx), "usr/bin/bash");
        File entry = new File(Env.rootfsDir(ctx), "etc/passwd");
        return bash.isFile() && bash.canExecute() && entry.isFile();
    }

    /** relay 是否与内置 assets 版本一致（大小比对，避免沿用旧脚本）。 */
    public boolean isRelayReady() {
        File root = latestAlmaRoot();
        if (root == null) return false;
        File f = new File(root, "local-relay.js");
        if (!f.isFile() || f.length() < 1000) return false;
        try (InputStream is = ctx.getAssets().open("local-relay.js")) {
            long n = 0;
            byte[] b = new byte[8192];
            int r;
            while ((r = is.read(b)) > 0) n += r;
            return n == f.length();
        } catch (IOException e) {
            return false;
        }
    }

    public boolean isAlmaReady() {
        File f = latestAlmaRoot();
        return f != null && new File(f, "bin/alma-server").isFile()
                && new File(f, "runtime/node").isFile()
                && new File(f, "go/alma-server").isFile();
    }

    /** The extracted alma-server directory (highest version wins). */
    public File latestAlmaRoot() {
        return latestAlmaRoot(Env.almaDir(ctx));
    }

    public static File latestAlmaRoot(File base) {
        File[] dirs = base.listFiles();
        if (dirs == null) return null;
        File best = null;
        String bestVer = null;
        for (File d : dirs) {
            if (!d.isDirectory()) continue;
            if (!new File(d, "bin/alma-server").isFile()) continue;
            String v = versionOf(d);
            if (best == null || compareVer(v, bestVer) > 0) { best = d; bestVer = v; }
        }
        return best;
    }

    public static String versionOf(File almaRoot) {
        File vf = new File(almaRoot, "VERSION");
        if (!vf.isFile()) return "0";
        try {
            byte[] b = readAll(vf, 64);
            return new String(b, StandardCharsets.UTF_8).trim();
        } catch (IOException e) {
            return "0";
        }
    }

    static int compareVer(String a, String b) {
        if (a == null) a = "0";
        if (b == null) b = "0";
        String[] pa = a.split("[.v-]+");
        String[] pb = b.split("[.v-]+");
        int n = Math.max(pa.length, pb.length);
        for (int i = 0; i < n; i++) {
            long x = i < pa.length ? parseNum(pa[i]) : 0;
            long y = i < pb.length ? parseNum(pb[i]) : 0;
            if (x != y) return x > y ? 1 : -1;
        }
        return 0;
    }

    private static long parseNum(String s) {
        try { return Long.parseLong(s.trim()); } catch (Exception e) { return 0; }
    }

    public void run() {
        new Thread(this::runInternal, "bootstrap").start();
    }

    private void runInternal() {
        try {
            Env.mkdirs(ctx);
            if (!isRootfsReady()) {
                stage("解包 Linux rootfs（Ubuntu 24.04 arm64）…");
                extractAsset("rootfs.tar.gz", Env.rootfsDir(ctx));
                fixRootfs();
            }
            if (cancelled) throw new InterruptedException();

            if (!isAlmaReady()) {
                File imported = findImportedTarball();
                if (imported != null) {
                    stage("从导入包安装 alma-server …");
                    extractTarGz(imported, Env.almaDir(ctx));
                    imported.delete();
                } else if (hasAsset("alma-server.tar.gz")) {
                    stage("解包内置 alma-server …");
                    extractAsset("alma-server.tar.gz", Env.almaDir(ctx));
                } else {
                    stage("下载 alma-server …");
                    downloadLatestAlma();
                }
            }
            if (!isAlmaReady()) {
                throw new IOException("alma-server 安装失败：未找到有效的安装包");
            }
            // 本地 relay 转发器：让官方 App 直连本机（不走云端中继）
            installLocalRelay();
            stage("就绪");
            listener.onDone(true, null);
        } catch (InterruptedException e) {
            listener.onDone(false, "已取消");
        } catch (Exception e) {
            Log.e(TAG, "bootstrap failed", e);
            listener.onDone(false, describe(e));
        }
    }

    private static String describe(Throwable t) {
        String m = t.getMessage();
        return t.getClass().getSimpleName() + (m == null ? "" : (": " + m));
    }

    /** import/alma-server-*.tar.gz placed via adb or file manager. */
    private File findImportedTarball() {
        // scan both private filesDir/import and external files dir
        // (adb push lands in /sdcard/Android/data/<pkg>/files/import = external)
        java.util.List<File> dirs = new ArrayList<>();
        dirs.add(Env.importDir(ctx));
        File ext = ctx.getExternalFilesDir(null);
        if (ext != null) {
            dirs.add(new File(ext, "import"));
            dirs.add(ext); // tolerate a tarball dropped straight into files/
        }
        for (File dir : dirs) {
            File[] fs = dir.listFiles();
            if (fs == null) continue;
            File best = null;
            long bestSize = -1;
            for (File f : fs) {
                String n = f.getName().toLowerCase();
                if (!n.endsWith(".tar.gz") && !n.endsWith(".tgz")) continue;
                if (f.length() < 10_000_000) continue; // sanity: alma-server is ~200MB
                if (best == null || f.length() > bestSize) { best = f; bestSize = f.length(); }
            }
            if (best != null) return best;
        }
        return null;
    }

    /** 安装本地 relay 转发器：放到 alma-server 根目录（proot 内 = /opt/alma/local-relay.js）。 */
    public void installLocalRelay() {
        File root = latestAlmaRoot();
        if (root == null) {
            Log.w(TAG, "installLocalRelay: alma root not found");
            return;
        }
        File dst = new File(root, "local-relay.js");
        // 与 assets 版本比对（避免升级安装沿用旧的坏脚本）
        long assetLen = -1;
        try (InputStream probe = ctx.getAssets().open("local-relay.js")) {
            long n = 0;
            byte[] b = new byte[8192];
            int r;
            while ((r = probe.read(b)) > 0) n += r;
            assetLen = n;
        } catch (IOException ignored) {}
        if (dst.isFile() && dst.length() > 1000 && assetLen == dst.length()) return;
        try (InputStream is = ctx.getAssets().open("local-relay.js")) {
            java.io.ByteArrayOutputStream bos = new java.io.ByteArrayOutputStream();
            byte[] buf = new byte[8192];
            int n;
            while ((n = is.read(buf)) > 0) bos.write(buf, 0, n);
            dst.getParentFile().mkdirs();
            try (java.io.FileOutputStream os = new java.io.FileOutputStream(dst)) {
                os.write(bos.toByteArray());
            }
            Log.i(TAG, "local-relay.js installed: " + dst.length() + " bytes");
        } catch (IOException e) {
            Log.w(TAG, "installLocalRelay failed: " + e.getMessage());
        }
    }

    private void downloadLatestAlma() throws Exception {
        String ver = fetchLatestVersion();
        String assetUrl = "https://github.com/yetone/alma-releases/releases/download/v" + ver
                + "/alma-server-" + ver + "-linux-arm64.tar.gz";
        stage("下载 alma-server " + ver);
        File dst = new File(Env.tmpDir(ctx), "alma-server.tar.gz");
        IOException last = null;
        for (String mirror : Env.MIRRORS) {
            if (cancelled) throw new InterruptedException();
            String url = mirror.contains("%s") ? String.format(mirror, assetUrl) : mirror + assetUrl;
            try {
                stage("尝试镜像 " + hostOf(url));
                download(url, dst, ver);
                stage("解包 alma-server …");
                extractTarGz(dst, Env.almaDir(ctx));
                dst.delete();
                return;
            } catch (IOException e) {
                Log.w(TAG, "mirror failed: " + url, e);
                last = e;
            }
        }
        throw (last != null) ? last : new IOException("所有下载源均失败");
    }

    private String fetchLatestVersion() throws Exception {
        IOException last = null;
        for (String mirror : Env.MIRRORS) {
            String url = mirror.contains("%s") ? String.format(mirror, Env.RELEASES_API) : mirror + Env.RELEASES_API;
            try {
                String json = httpGet(url, 15000);
                java.util.regex.Matcher m = java.util.regex.Pattern
                        .compile("\"tag_name\"\\s*:\\s*\"v?([0-9][0-9a-zA-Z.\\-_]*)\"").matcher(json);
                if (m.find()) return m.group(1);
            } catch (IOException e) {
                Log.w(TAG, "api mirror failed: " + url);
                last = e;
            }
        }
        // fallback: known-good version so the (mirrored) download can still work
        Log.w(TAG, "version lookup failed everywhere; using fallback version", last);
        return FALLBACK_VERSION;
    }

    private static String hostOf(String url) {
        try { return new URL(url).getHost(); } catch (Exception e) { return url; }
    }

    private void download(String url, File dst, String verLabel) throws IOException {
        File part = new File(dst.getAbsolutePath() + ".part");
        long total = -1, done = 0;
        InputStream in = null;
        OutputStream out = null;
        try {
            URL u = new URL(url);
            HttpURLConnection conn = (HttpURLConnection) u.openConnection();
            conn.setConnectTimeout(20000);
            conn.setReadTimeout(30000);
            conn.setInstanceFollowRedirects(true);
            conn.setRequestProperty("User-Agent", "AlmaDroid/1.0");
            conn.connect();
            int code = conn.getResponseCode();
            if (code < 200 || code >= 300) throw new IOException("HTTP " + code + " from " + hostOf(url));
            total = conn.getContentLength();
            in = conn.getInputStream();
            out = new FileOutputStream(part);
            byte[] buf = new byte[128 * 1024];
            long lastReport = 0;
            int n;
            while ((n = in.read(buf)) > 0) {
                out.write(buf, 0, n);
                done += n;
                if (System.currentTimeMillis() - lastReport > 400) {
                    lastReport = System.currentTimeMillis();
                    listener.onProgress(done, total, null);
                }
            }
            out.flush();
            if (total > 0 && done < total * 95 / 100) throw new IOException("下载不完整 " + done + "/" + total);
            if (!part.renameTo(dst)) {
                copyFile(part, dst);
                part.delete();
            }
        } finally {
            close(in); close(out);
        }
    }

    private String httpGet(String url, int timeoutMs) throws IOException {
        HttpURLConnection conn = (HttpURLConnection) new URL(url).openConnection();
        conn.setConnectTimeout(timeoutMs);
        conn.setReadTimeout(timeoutMs);
        conn.setRequestProperty("User-Agent", "AlmaDroid/1.0");
        conn.setRequestProperty("Accept", "application/vnd.github+json");
        int code = conn.getResponseCode();
        if (code != 200) throw new IOException("HTTP " + code + " for " + url);
        try (InputStream is = conn.getInputStream()) {
            return new String(readAll(is, 4 * 1024 * 1024), StandardCharsets.UTF_8);
        }
    }

    private boolean hasAsset(String name) {
        try (InputStream is = ctx.getAssets().open(name)) {
            return is.read() >= 0;
        } catch (IOException e) {
            return false;
        }
    }

    private void extractAsset(String name, File dest) throws IOException {
        try (InputStream is = ctx.getAssets().open(name)) {
            TarGz.extract(is, dest, (n, i, t) -> listener.onProgress(i, t, n));
        }
    }

    private void extractTarGz(File tarball, File dest) throws IOException {
        try (InputStream is = new FileInputStream(tarball)) {
            TarGz.extract(is, dest, (n, i, t) -> listener.onProgress(i, t, n));
        }
    }

    private void fixRootfs() throws IOException {
        // DNS: Android host has no /etc/resolv.conf to bind; write our own.
        File etc = new File(Env.rootfsDir(ctx), "etc");
        etc.mkdirs();
        write(new File(etc, "resolv.conf"),
                "nameserver 223.5.5.5\nnameserver 119.29.29.29\nnameserver 8.8.8.8\noptions timeout:2 attempts:2\n");
        write(new File(etc, "hosts"), "127.0.0.1 localhost\n::1 localhost\n");
        // dirs the server needs
        for (String p : new String[]{"root", "opt", "tmp", "dev/shm", "run", "var/tmp"}) {
            new File(Env.rootfsDir(ctx), p).mkdirs();
        }
        new File(Env.rootfsDir(ctx), "tmp").setExecutable(true, false);
        // ensure /bin/sh exists (usrmerge layouts already symlink)
        File sh = new File(Env.rootfsDir(ctx), "bin/sh");
        if (!sh.exists()) {
            try {
                android.system.Os.symlink("/usr/bin/dash", sh.getAbsolutePath());
            } catch (Throwable t) {
                Log.w(TAG, "bin/sh symlink failed: " + t.getMessage());
            }
        }
    }

    private static void write(File f, String content) throws IOException {
        try (FileOutputStream os = new FileOutputStream(f)) {
            os.write(content.getBytes(StandardCharsets.UTF_8));
        }
    }

    private static byte[] readAll(File f, int cap) throws IOException {
        try (FileInputStream is = new FileInputStream(f)) {
            return readAll(is, cap);
        }
    }

    private static byte[] readAll(InputStream is, int cap) throws IOException {
        java.io.ByteArrayOutputStream bos = new java.io.ByteArrayOutputStream();
        byte[] buf = new byte[64 * 1024];
        int n;
        while ((n = is.read(buf)) > 0) {
            bos.write(buf, 0, n);
            if (bos.size() > cap) break;
        }
        return bos.toByteArray();
    }

    private static void close(java.io.Closeable c) {
        if (c != null) try { c.close(); } catch (IOException ignored) {}
    }

    private static void copyFile(File src, File dst) throws IOException {
        try (FileInputStream is = new FileInputStream(src); FileOutputStream os = new FileOutputStream(dst)) {
            byte[] buf = new byte[128 * 1024];
            int n;
            while ((n = is.read(buf)) > 0) os.write(buf, 0, n);
        }
    }

    private void stage(String s) {
        Log.i(TAG, s);
        listener.onStage(s);
    }
}
