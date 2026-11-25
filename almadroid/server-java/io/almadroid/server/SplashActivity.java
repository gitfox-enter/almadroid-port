package io.almadroid.server;

import android.app.Activity;
import android.content.Intent;
import android.graphics.Color;
import android.graphics.Typeface;
import android.graphics.drawable.GradientDrawable;
import android.net.Uri;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.util.Log;
import android.util.TypedValue;
import android.view.Gravity;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ProgressBar;
import android.widget.TextView;

import java.io.File;
import java.io.FileOutputStream;
import java.io.InputStream;

/**
 * 启动页：首次/每次启动都先在这里完成「解包 → 启动服务 → 就绪」，
 * 全部完成后自动跳转到官方 Alma 主界面（带配对 deep link）。
 * 用户看到的是明确的进度，而不是干等的扫码页。
 */
public class SplashActivity extends Activity {
    private static final String TAG = "AlmaSplash";

    private final Handler main = new Handler(Looper.getMainLooper());
    private TextView status;
    private ProgressBar bar;
    private TextView hint;
    private volatile String pairCode;
    private int retryCount;

    @Override
    protected void onCreate(Bundle b) {
        super.onCreate(b);
        buildUi();
        new Thread(new Runnable() { public void run() { runFlow(); } }, "splash-flow").start();
    }

    // ---------- UI ----------

    private void buildUi() {
        int bg = 0xFF130D0A;
        LinearLayout root = new LinearLayout(this);
        root.setOrientation(LinearLayout.VERTICAL);
        root.setGravity(Gravity.CENTER_HORIZONTAL);
        root.setBackgroundColor(bg);
        int p = dp(28);
        root.setPadding(p, dp(80), p, p);

        ImageView logo = new ImageView(this);
        try {
            logo.setImageResource(getResources().getIdentifier(
                    "mipmap/ic_launcher", null, getPackageName()));
        } catch (Throwable ignored) {}
        GradientDrawable ib = new GradientDrawable();
        ib.setColor(Color.WHITE);
        ib.setCornerRadius(dp(24));
        logo.setBackground(ib);
        logo.setClipToOutline(true);
        logo.setPadding(dp(10), dp(10), dp(10), dp(10));
        root.addView(logo, new LinearLayout.LayoutParams(dp(96), dp(96)));

        // 启动页只留：图标 + 进度条（标题/状态文字/局域网卡片全部移除）。
        // status/hint 仍创建但不挂到界面：setStatus/showRetry 的引用不断，文字只进 log。
        status = new TextView(this);
        hint = new TextView(this);

        bar = new ProgressBar(this, null, android.R.attr.progressBarStyleHorizontal);
        bar.setIndeterminate(true);
        bar.getProgressDrawable().setColorFilter(0xFFF8B8A0, android.graphics.PorterDuff.Mode.SRC_IN);
        bar.getIndeterminateDrawable().setColorFilter(0xFFF8B8A0, android.graphics.PorterDuff.Mode.SRC_IN);
        LinearLayout.LayoutParams blp = new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        blp.topMargin = dp(14);
        root.addView(bar, blp);

        setContentView(root);
    }

    private LinearLayout.LayoutParams lp(int top, int h) {
        LinearLayout.LayoutParams p = new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.WRAP_CONTENT,
                h == 0 ? ViewGroup.LayoutParams.WRAP_CONTENT : h);
        p.topMargin = top;
        return p;
    }

    private int dp(int v) {
        return Math.round(v * getResources().getDisplayMetrics().density);
    }

    private void setStatus(String s) {
        main.post(new Runnable() { public void run() { if (status != null) status.setText(s); } });
        Log.i(TAG, s);
    }

    private void updateProgress(int percent) {
        main.post(new Runnable() { public void run() {
            if (bar == null) return;
            if (percent < 0) {
                bar.setIndeterminate(true);
            } else {
                bar.setIndeterminate(false);
                bar.setMax(100);
                bar.setProgress(percent);
            }
        } });
    }

    // ---------- flow ----------

    private void runFlow() {
        try {
            File rootfs = new File(getFilesDir(), "rootfs");
            File alma = new File(getFilesDir(), "alma");

            // 服务已在跑（上次没被杀）：直接进，无需任何解包/启动
            if (LocalBootstrap.health()) {
                Log.i(TAG, "service already healthy, fast path");
                setStatus("服务已在运行");
                updateProgress(100);
                if (!LocalBootstrap.isPaired(this)) {
                    pairCode = LocalBootstrap.ensureLocalPairing(this);
                }
                // 快速路径：进度打满后 0.7s 直接进主界面（无卡片、无等待）
                main.post(new Runnable() { public void run() { showReadyWithLan(); }});
                return;
            }

            // Splash 不再自己解包——LocalService 会做（带标记，幂等）。
            // 之前两侧都做一次检查/解包，白等 5 秒。
            setStatus("检查环境…");
            updateProgress(45);

            // local-relay.js 放到实际的 alma-server 子目录里
            File almaReal = alma;
            File[] subs = alma.listFiles();
            if (subs != null) {
                for (File d : subs) {
                    if (d.isDirectory() && new File(d, "bin/alma-server").isFile()) {
                        almaReal = d;
                        break;
                    }
                }
            }
            File relay = new File(almaReal, "local-relay.js");
            // 每次启动都覆盖：APK 更新后 relay 逻辑（如 noVNC 参数）随之生效
            copyAsset("local-relay.js", relay);
            Log.i(TAG, "alma real dir = " + almaReal.getAbsolutePath());

            setStatus("启动服务…");
            updateProgress(-1);
            LocalBootstrap.startServiceFromForeground(this);

            setStatus("等待服务就绪…");
            // 只要 23001 能连上就进界面；客户端自己会重连，不必等完整 health。
            // 之前等 health 要多花 4-5 秒。
            boolean up = false;
            for (int i = 0; i < 200; i++) {
                if (LocalService.isPortOpenPublic(23001)) { up = true; break; }
                try { Thread.sleep(300); } catch (InterruptedException e) { return; }
            }

            if (!up) {
                setStatus("服务启动超时");
                updateProgress(0);
                main.post(new Runnable() { public void run() { showRetry(); } });
                return;
            }

            // 配对只需做一次：prefs 已存在就跳过（省 3 秒）。
            setStatus("检查配对…");
            if (!LocalBootstrap.isPaired(this)) {
                setStatus("配置本机配对…");
                String code = LocalBootstrap.ensureLocalPairing(this);
                pairCode = code;
                if (code == null || code.length() != 43) {
                    setStatus("配对初始化失败");
                    updateProgress(0);
                    main.post(new Runnable() { public void run() { showRetry(); } });
                    return;
                }
            }
            updateProgress(100);
            setStatus("已就绪");
            main.post(new Runnable() { public void run() { showReadyWithLan(); } });
        } catch (Throwable t) {
            Log.e(TAG, "flow failed", t);
            setStatus("启动失败：" + t.getMessage());
            main.post(new Runnable() { public void run() { showRetry(); } });
        }
    }

    private void showRetry() {
        // 启动页没有可点提示：3 秒后自动重跑（最多连续 3 次，防死循环）
        if (retryCount < 3) {
            retryCount++;
            updateProgress(-1);
            main.postDelayed(new Runnable() {
                public void run() { new Thread(new Runnable() { public void run() { runFlow(); } }, "splash-retry").start(); }
            }, 3000);
            return;
        }
        if (hint != null) {
            hint.setText("启动失败，点击屏幕重试");
            hint.setTextColor(0xFFF97758);
            hint.setOnClickListener(new android.view.View.OnClickListener() {
                public void onClick(android.view.View v) {
                    hint.setOnClickListener(null);
                    hint.setText("重试中…");
                    final SplashActivity self = SplashActivity.this;
                    new Thread(new Runnable() { public void run() { self.runFlow(); } }, "splash-retry").start();
                }
            });
        }
    }

    private void launchOfficial() {
        try {
            // 不发 deep link：deep link 会被官方 App 当成"新的配对请求"，
            // 从而弹出 Connect/Cancel 确认框。配对 prefs 已经写好，
            // 直接启动 MainActivity 它会自己读 prefs 并连接。
            Intent i = new Intent(Intent.ACTION_MAIN);
            i.setClassName(getPackageName(), "me.yetone.alma.app.MainActivity");
            i.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TOP);
            startActivity(i);
            Log.i(TAG, "-> official UI (prefs-based, no deep link)");
        } catch (Throwable t) {
            Log.w(TAG, "launch official failed, fallback MAIN", t);
            try {
                Intent i = new Intent(Intent.ACTION_MAIN);
                i.setClassName(getPackageName(), "me.yetone.alma.app.MainActivity");
                startActivity(i);
            } catch (Throwable ignored) {}
        }
        finish();
    }

    // ---------- helpers ----------

    private boolean isDone(File dir) {
        if (dir.isFile()) return dir.length() > 1000;
        String[] names = dir.list();
        return names != null && names.length > 3;
    }

    private void extract(String asset, File dest, int from, int to) throws Exception {
        dest.mkdirs();
        long total = assetSize(asset);
        final int lo = from, hi = to;
        final int[] last = {-1};
        try (InputStream is = getAssets().open(asset)) {
            TarGz.extract(is, total, dest, new TarGz.Progress() {
                public void onBytes(long read, long len) {
                    if (len <= 0) return;
                    int pct = lo + (int) ((hi - lo) * Math.min(read, len) / (double) len);
                    if (pct > hi) pct = hi;
                    if (pct != last[0]) {
                        last[0] = pct;
                        updateProgress(pct);
                    }
                }
            });
        }
        updateProgress(to);
    }

    private long assetSize(String asset) {
        try {
            return getAssets().openFd(asset).getLength();
        } catch (Throwable t) {
            // 资产被压缩时 openFd 不可用，退回读流长度的近似
            return -1;
        }
    }

    private void copyAsset(String asset, File dest) {
        try (InputStream is = getAssets().open(asset);
             FileOutputStream os = new FileOutputStream(dest)) {
            byte[] b = new byte[65536];
            int n;
            while ((n = is.read(b)) > 0) os.write(b, 0, n);
        } catch (Throwable t) {
            Log.w(TAG, "copyAsset " + asset + " failed", t);
        }
    }

    private void fixRootfs() {
        File etc = new File(getFilesDir(), "rootfs/etc");
        etc.mkdirs();
        write(new File(etc, "resolv.conf"),
                "nameserver 223.5.5.5\nnameserver 119.29.29.29\nnameserver 8.8.8.8\n");
        write(new File(etc, "hosts"), "127.0.0.1 localhost\n");
        for (String p : new String[]{"root", "opt", "tmp", "dev/shm", "run", "var/tmp"}) {
            new File(getFilesDir(), "rootfs/" + p).mkdirs();
        }
    }

    private void write(File f, String s) {
        try (FileOutputStream os = new FileOutputStream(f)) {
            os.write(s.getBytes("UTF-8"));
        } catch (Throwable ignored) {}
    }

    private static String enc(String s) {
        try {
            return java.net.URLEncoder.encode(s, "UTF-8").replace("+", "%20");
        } catch (Exception e) {
            return s;
        }
    }

    // ---------- 局域网卡片 ----------

    private LinearLayout lanCard;
    private TextView lanUrlView;
    private TextView lanPortView;

    private android.view.View buildLanCard() {
        lanCard = new LinearLayout(this);
        lanCard.setOrientation(LinearLayout.VERTICAL);
        GradientDrawable bgd = new GradientDrawable();
        bgd.setColor(0xFF1E1714);
        bgd.setCornerRadius(dp(16));
        bgd.setStroke(dp(1), 0xFF2E2521);
        lanCard.setBackground(bgd);
        int cp = dp(16);
        lanCard.setPadding(cp, dp(14), cp, dp(14));

        TextView t = new TextView(this);
        t.setText("局域网访问");
        t.setTextSize(13);
        t.setTextColor(0xFFF9E0D8);
        t.setTypeface(null, Typeface.BOLD);
        lanCard.addView(t);

        lanUrlView = new TextView(this);
        lanUrlView.setTextSize(12);
        lanUrlView.setTextColor(0xFF7FD8B8);
        lanUrlView.setPadding(0, dp(6), 0, 0);
        lanUrlView.setTextIsSelectable(true);
        lanCard.addView(lanUrlView);

        TextView sub = new TextView(this);
        sub.setText("同一 WiFi 下的任何设备，用浏览器打开上面地址即可使用");
        sub.setTextSize(10);
        sub.setTextColor(0xFF84716B);
        sub.setPadding(0, dp(4), 0, 0);
        lanCard.addView(sub);

        LinearLayout row = new LinearLayout(this);
        row.setOrientation(LinearLayout.HORIZONTAL);
        row.setGravity(Gravity.CENTER_VERTICAL);
        row.setPadding(0, dp(12), 0, 0);

        lanPortView = new TextView(this);
        lanPortView.setTextSize(12);
        lanPortView.setTextColor(0xFFBCA69F);
        row.addView(lanPortView, new LinearLayout.LayoutParams(
                0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f));

        TextView edit = new TextView(this);
        edit.setText("修改端口");
        edit.setTextSize(12);
        edit.setTextColor(0xFFF8B8A0);
        edit.setPadding(dp(10), dp(6), dp(10), dp(6));
        edit.setOnClickListener(new android.view.View.OnClickListener() {
            public void onClick(android.view.View v) {
                startActivity(new Intent(SplashActivity.this, LanSettingsActivity.class));
            }
        });
        row.addView(edit);

        TextView copy = new TextView(this);
        copy.setText("复制");
        copy.setTextSize(12);
        copy.setTextColor(0xFFF8B8A0);
        copy.setPadding(dp(10), dp(6), dp(10), dp(6));
        copy.setOnClickListener(new android.view.View.OnClickListener() {
            public void onClick(android.view.View v) { copyLanUrl(); }
        });
        row.addView(copy);

        TextView mgmt = new TextView(this);
        mgmt.setText("管理网页");
        mgmt.setTextSize(12);
        mgmt.setTextColor(0xFFF8B8A0);
        mgmt.setPadding(dp(10), dp(6), dp(10), dp(6));
        mgmt.setOnClickListener(new android.view.View.OnClickListener() {
            public void onClick(android.view.View v) {
                String u = LanSettings.lanUrl(SplashActivity.this);
                if (u == null) { toast("未连接 WiFi"); return; }
                try {
                    startActivity(new Intent(Intent.ACTION_VIEW, Uri.parse(u))
                            .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK));
                } catch (Throwable t) { toast(u); }
            }
        });
        row.addView(mgmt);

        lanCard.addView(row);

        // 桌面入口：App 内全屏打开（不跳系统浏览器）
        TextView desk = new TextView(this);
        desk.setText("打开桌面");
        desk.setTextSize(12);
        desk.setTextColor(0xFFF8B8A0);
        desk.setPadding(0, dp(10), 0, 0);
        desk.setOnClickListener(new android.view.View.OnClickListener() {
            public void onClick(android.view.View v) {
                try {
                    startActivity(new Intent(SplashActivity.this, DesktopActivity.class));
                } catch (Throwable t) { toast("桌面启动失败"); }
            }
        });
        lanCard.addView(desk);

        refreshLan();
        return lanCard;
    }

    private LinearLayout.LayoutParams lpWithTop(int top) {
        LinearLayout.LayoutParams p = new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        p.topMargin = top;
        return p;
    }

    private void refreshLan() {
        final String url = LanSettings.lanUrl(this);
        main.post(new Runnable() { public void run() {
            if (lanUrlView != null) {
                lanUrlView.setText(url != null ? url : "未连接 WiFi，暂无局域网地址");
                lanUrlView.setTextColor(url != null ? 0xFF7FD8B8 : 0xFF9A8A84);
            }
            if (lanPortView != null) {
                lanPortView.setText("端口 " + LanSettings.port(SplashActivity.this));
            }
        } });
    }

    private void showPortDialog() {
        final android.widget.EditText input = new android.widget.EditText(this);
        input.setInputType(android.text.InputType.TYPE_CLASS_NUMBER);
        input.setText(String.valueOf(LanSettings.port(this)));
        input.setSelection(input.getText().length());
        new android.app.AlertDialog.Builder(this)
                .setTitle("局域网端口")
                .setMessage("范围 " + LanSettings.MIN_PORT + "–" + LanSettings.MAX_PORT
                        + "。修改后需重启应用生效。")
                .setView(input)
                .setPositiveButton("保存", new android.content.DialogInterface.OnClickListener() {
                    public void onClick(android.content.DialogInterface d, int w) {
                        int p;
                        try {
                            p = Integer.parseInt(input.getText().toString().trim());
                        } catch (Throwable t) {
                            toast("端口必须是数字");
                            return;
                        }
                        if (!LanSettings.setPort(SplashActivity.this, p)) {
                            toast("端口需在 " + LanSettings.MIN_PORT + "–" + LanSettings.MAX_PORT + " 之间");
                            return;
                        }
                        refreshLan();
                        toast("已保存 · 重启应用后生效");
                    }
                })
                .setNegativeButton("取消", null)
                .show();
    }

    private void copyLanUrl() {
        String url = LanSettings.lanUrl(this);
        if (url == null) { toast("暂无局域网地址"); return; }
        try {
            android.content.ClipboardManager cm = (android.content.ClipboardManager)
                    getSystemService(CLIPBOARD_SERVICE);
            cm.setPrimaryClip(android.content.ClipData.newPlainText("Alma LAN", url));
            toast("已复制");
        } catch (Throwable t) {
            toast(url);
        }
    }

    private void toast(String s) {
        main.post(new Runnable() { public void run() {
            android.widget.Toast.makeText(SplashActivity.this, s, android.widget.Toast.LENGTH_SHORT).show();
        } });
    }

    /** 就绪：进度条打满，短暂停留后自动进主界面（启动页不再放任何按钮/卡片）。 */
    private void showReadyWithLan() {
        setStatus("服务已就绪");
        updateProgress(100);
        main.postDelayed(new Runnable() { public void run() {
            try { launchOfficial(); } catch (Throwable ignored) {}
        } }, 700);
    }
}
