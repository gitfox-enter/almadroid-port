package io.almadroid.app;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.ClipData;
import android.content.ClipboardManager;
import android.content.Context;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Color;
import android.graphics.Typeface;
import android.graphics.drawable.GradientDrawable;
import android.net.Uri;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.os.PowerManager;
import android.provider.Settings;
import android.text.TextUtils;
import android.view.Gravity;
import android.view.View;
import android.view.ViewGroup;
import android.webkit.CookieManager;
import android.webkit.WebResourceRequest;
import android.webkit.WebSettings;
import android.webkit.WebView;
import android.webkit.WebViewClient;
import android.widget.Button;
import android.widget.FrameLayout;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ProgressBar;
import android.widget.ScrollView;
import android.widget.TextView;
import android.widget.Toast;

import java.util.List;

/**
 * AlmaDroid：一个 App = Alma 的原生界面 + 本机后端。
 *   原生聊天（默认）← 直连 127.0.0.1:23001，不走任何外部服务器
 *   网页模式（可选）← 万一需要完整 Web 功能
 */
public class MainActivity extends Activity implements Sheet.Host, ChatView.Host {

    private static final int POLL_MS = 2500;

    private final Handler main = new Handler(Looper.getMainLooper());
    private int[] T;

    private FrameLayout root;
    private LinearLayout bootView;
    private TextView bootStatus;
    private ProgressBar bootBar;
    private TextView bootTip;

    private LinearLayout mainView;
    private FrameLayout contentBox;     // 原生聊天 or WebView
    private View statusDot;
    private TextView statusText;
    private TextView modelChip;
    private ChatView chat;
    private WebView web;
    private boolean webMode = false;
    private boolean serverUp = false;
    private boolean bootFailed = false;

    private Bootstrap bootstrap;
    private final AlmaService.StatusListener svcListener = this::onServiceStatus;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        T = M3.current(this);
        buildUi();
        setContentView(root);
        AlmaService.addListener(svcListener);
        maybeIgnoreBatteryOptimization();
        startFlow();
    }

    @Override
    protected void onDestroy() {
        AlmaService.removeListener(svcListener);
        if (web != null) web.destroy();
        super.onDestroy();
    }

    // ---- ChatView.Host ----

    @Override
    public int[] tokens() { return T; }

    @Override
    public int dp(int v) { return Math.round(v * getResources().getDisplayMetrics().density); }

    @Override
    public void onOpenSettings() { showModelPicker(); }

    private void maybeIgnoreBatteryOptimization() {
        try {
            PowerManager pm = (PowerManager) getSystemService(POWER_SERVICE);
            if (!pm.isIgnoringBatteryOptimizations(getPackageName())) {
                Intent i = new Intent(Settings.ACTION_REQUEST_IGNORE_BATTERY_OPTIMIZATIONS);
                i.setData(Uri.parse("package:" + getPackageName()));
                startActivity(i);
            }
        } catch (Throwable ignored) {}
    }

    // ---- 引导 ----

    private void startFlow() {
        Env.mkdirs(this);
        bootstrap = new Bootstrap(this, new Bootstrap.Listener() {
            @Override public void onStage(String m) { main.post(() -> { if (bootStatus != null) bootStatus.setText(m); }); }

            @Override public void onProgress(long done, long total, String detail) {
                main.post(() -> {
                    if (bootBar == null) return;
                    bootBar.setIndeterminate(total <= 0);
                    if (total > 0) {
                        bootBar.setMax((int) Math.min(total, Integer.MAX_VALUE));
                        bootBar.setProgress((int) Math.min(done, Integer.MAX_VALUE));
                        if (detail != null && !detail.isEmpty()) bootStatus.setText(detail);
                    }
                });
            }

            @Override public void onDone(boolean ok, String err) {
                main.post(() -> {
                    if (ok) {
                        bootFailed = false;
                        showMain();
                        startService(new Intent(MainActivity.this, AlmaService.class)
                                .setAction(AlmaService.ACTION_START));
                    } else {
                        bootFailed = true;
                        bootStatus.setText("引导失败：" + err);
                        bootTip.setText("退出应用后重新打开即可重试。");
                        bootBar.setIndeterminate(false);
                        Button retry = new Button(MainActivity.this);
                        retry.setText("重试");
                        retry.setTextColor(T[M3.ON_PRIMARY]);
                        retry.setBackground(pill(T[M3.PRIMARY], 999));
                        retry.setOnClickListener(v -> { bootView.removeAllViews(); buildBootView(); startFlow(); });
                        LinearLayout.LayoutParams rlp = new LinearLayout.LayoutParams(
                                ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT);
                        rlp.topMargin = dp(20);
                        retry.setLayoutParams(rlp);
                        bootView.addView(retry);
                    }
                });
            }
        });
        if (bootstrap.isRootfsReady() && bootstrap.isAlmaReady()) {
            // 已就绪的装机路径也要确保 local-relay 存在（升级安装不会重跑引导）
            if (!bootstrap.isRelayReady()) bootstrap.installLocalRelay();
            showMain();
            startService(new Intent(this, AlmaService.class).setAction(AlmaService.ACTION_START));
        } else {
            showBoot();
            bootstrap.run();
        }
    }

    // ---- 服务状态 ----

    private void onServiceStatus(String state, String detail) {
        main.post(() -> renderHeader(state, detail));
    }

    private void renderHeader(String state, String detail) {
        int dotColor; String label;
        switch (state) {
            case "running": dotColor = 0xFF4ADE80; label = "运行中"; break;
            case "starting": dotColor = 0xFFFBBF24; label = "启动中…"; break;
            case "crashed": case "error": dotColor = 0xFFF97758; label = "异常：" + detail; break;
            default: dotColor = 0xFF84716B; label = "已停止";
        }
        if (statusDot != null) {
            GradientDrawable d = new GradientDrawable();
            d.setShape(GradientDrawable.OVAL);
            d.setColor(dotColor);
            statusDot.setBackground(d);
        }
        if (statusText != null) statusText.setText(label);
    }

    private final Runnable pollChain = this::healthTick;

    private void pollHealth() {
        main.removeCallbacks(pollChain);
        main.postDelayed(pollChain, POLL_MS);
    }

    private void healthTick() {
        new Thread(() -> {
            final boolean ok = AlmaApi.health();
            main.post(() -> {
                boolean was = serverUp;
                serverUp = ok;
                if (ok) {
                    renderHeader("running", null);
                    if (!was) {
                        // 服务刚就绪：确保 E2E 直连监听器(23019)与配对码可用
                        new Thread(() -> {
                            boolean e2e = Pairing.ensureE2EEnabled();
                            android.util.Log.i("MainActivity", "ensureE2EEnabled -> " + e2e);
                        }, "ensure-e2e").start();
                        if (chat != null) chat.refreshList();
                    }
                }
                pollHealth();
            });
        }, "health").start();
    }

    @Override protected void onPause() { super.onPause(); main.removeCallbacks(pollChain); }
    @Override protected void onResume() {
        super.onResume();
        if (mainView != null && mainView.getVisibility() == View.VISIBLE) pollHealth();
    }

    // ---- UI ----

    private GradientDrawable pill(int color, int r) {
        GradientDrawable g = new GradientDrawable();
        g.setColor(color);
        g.setCornerRadius(dp(r));
        return g;
    }

    private void buildUi() {
        root = new FrameLayout(this);
        root.setBackgroundColor(T[M3.SURFACE]);
        buildBootView();
        root.addView(bootView, 0);

        mainView = new LinearLayout(this);
        mainView.setOrientation(LinearLayout.VERTICAL);

        // 头部卡片
        LinearLayout card = new LinearLayout(this);
        card.setOrientation(LinearLayout.HORIZONTAL);
        card.setGravity(Gravity.CENTER_VERTICAL);
        GradientDrawable cardBg = new GradientDrawable();
        cardBg.setColor(T[M3.SURFACE_CONTAINER_LOW]);
        cardBg.setCornerRadius(dp(24));
        card.setBackground(cardBg);
        card.setPadding(dp(14), dp(12), dp(10), dp(12));
        LinearLayout.LayoutParams clp = new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        clp.setMargins(dp(10), dp(10), dp(10), dp(8));
        card.setLayoutParams(clp);

        ImageView appIcon = new ImageView(this);
        Bitmap ic = BitmapFactory.decodeResource(getResources(), R.mipmap.ic_launcher);
        if (ic != null) appIcon.setImageBitmap(ic);
        GradientDrawable iconBg = new GradientDrawable();
        iconBg.setColor(Color.WHITE);
        iconBg.setCornerRadius(dp(14));
        appIcon.setBackground(iconBg);
        appIcon.setClipToOutline(true);
        LinearLayout.LayoutParams ilp = new LinearLayout.LayoutParams(dp(46), dp(46));
        ilp.rightMargin = dp(14);
        appIcon.setLayoutParams(ilp);
        card.addView(appIcon);

        LinearLayout col = new LinearLayout(this);
        col.setOrientation(LinearLayout.VERTICAL);
        TextView title = new TextView(this);
        title.setText("Alma");
        title.setTextSize(19);
        title.setTypeface(null, Typeface.BOLD);
        title.setTextColor(T[M3.ON_SURFACE]);
        col.addView(title);

        LinearLayout statusRow = new LinearLayout(this);
        statusRow.setOrientation(LinearLayout.HORIZONTAL);
        statusRow.setGravity(Gravity.CENTER_VERTICAL);
        statusDot = new View(this);
        LinearLayout.LayoutParams dlp = new LinearLayout.LayoutParams(dp(9), dp(9));
        dlp.rightMargin = dp(6);
        statusDot.setLayoutParams(dlp);
        GradientDrawable dot0 = new GradientDrawable();
        dot0.setShape(GradientDrawable.OVAL);
        dot0.setColor(0xFF84716B);
        statusDot.setBackground(dot0);
        statusRow.addView(statusDot);

        statusText = new TextView(this);
        statusText.setText("启动中");
        statusText.setTextSize(13);
        statusText.setTextColor(T[M3.ON_SURFACE_VARIANT]);
        statusRow.addView(statusText);

        modelChip = new TextView(this);
        modelChip.setText("本机");
        modelChip.setTextSize(11.5f);
        modelChip.setTypeface(Typeface.MONOSPACE);
        modelChip.setTextColor(T[M3.PRIMARY]);
        modelChip.setBackground(pill(M3.blend(T[M3.PRIMARY], T[M3.SURFACE_CONTAINER_LOW], 0.12f), 999));
        modelChip.setPadding(dp(8), dp(3), dp(8), dp(3));
        modelChip.setClickable(true);
        modelChip.setOnClickListener(v -> showModelPicker());
        LinearLayout.LayoutParams alp = new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        alp.leftMargin = dp(8);
        modelChip.setLayoutParams(alp);
        statusRow.addView(modelChip);
        col.addView(statusRow);
        col.setLayoutParams(new LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f));
        card.addView(col);

        ImageView menu = new ImageView(this);
        menu.setImageBitmap(Icons.dots(getResources().getDisplayMetrics().density,
                T[M3.ON_SURFACE_VARIANT], 22));
        menu.setPadding(dp(8), dp(8), dp(8), dp(8));
        menu.setOnClickListener(v -> openMenu());
        menu.setLayoutParams(new LinearLayout.LayoutParams(dp(40), dp(40)));
        card.addView(menu);

        mainView.addView(card);

        // 内容区：原生聊天 / WebView
        contentBox = new FrameLayout(this);
        contentBox.setLayoutParams(new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, 0, 1f));
        mainView.addView(contentBox);

        chat = new ChatView(this, contentBox, this);

        root.addView(mainView);
        mainView.setVisibility(View.GONE);
    }

    private void buildBootView() {
        bootView = new LinearLayout(this);
        bootView.setOrientation(LinearLayout.VERTICAL);
        bootView.setPadding(dp(28), dp(56), dp(28), dp(24));
        bootView.setGravity(Gravity.CENTER_HORIZONTAL);

        ImageView logo = new ImageView(this);
        Bitmap ic = BitmapFactory.decodeResource(getResources(), R.mipmap.ic_launcher);
        if (ic != null) logo.setImageBitmap(ic);
        GradientDrawable iconBg = new GradientDrawable();
        iconBg.setColor(Color.WHITE);
        iconBg.setCornerRadius(dp(24));
        logo.setBackground(iconBg);
        logo.setClipToOutline(true);
        logo.setPadding(dp(10), dp(10), dp(10), dp(10));
        LinearLayout.LayoutParams llp = new LinearLayout.LayoutParams(dp(96), dp(96));
        llp.topMargin = dp(60);
        logo.setLayoutParams(llp);
        bootView.addView(logo);

        TextView title = new TextView(this);
        title.setText("Alma");
        title.setTextSize(26);
        title.setTypeface(null, Typeface.BOLD);
        title.setTextColor(T[M3.ON_SURFACE]);
        title.setGravity(Gravity.CENTER);
        LinearLayout.LayoutParams tlp = new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        tlp.topMargin = dp(20);
        title.setLayoutParams(tlp);
        bootView.addView(title);

        TextView sub = new TextView(this);
        sub.setText("本机运行 · 无需其他应用");
        sub.setTextSize(13);
        sub.setTextColor(T[M3.ON_SURFACE_VARIANT]);
        sub.setGravity(Gravity.CENTER);
        sub.setPadding(0, dp(4), 0, 0);
        bootView.addView(sub);

        bootStatus = new TextView(this);
        bootStatus.setTextSize(14);
        bootStatus.setTextColor(T[M3.ON_SURFACE]);
        bootStatus.setGravity(Gravity.CENTER);
        LinearLayout.LayoutParams slp = new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        slp.topMargin = dp(36);
        bootStatus.setLayoutParams(slp);
        bootView.addView(bootStatus);

        bootBar = new ProgressBar(this, null, android.R.attr.progressBarStyleHorizontal);
        bootBar.setIndeterminate(true);
        bootBar.getProgressDrawable().setColorFilter(T[M3.PRIMARY], android.graphics.PorterDuff.Mode.SRC_IN);
        bootBar.getIndeterminateDrawable().setColorFilter(T[M3.PRIMARY], android.graphics.PorterDuff.Mode.SRC_IN);
        LinearLayout.LayoutParams blp = new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        blp.topMargin = dp(14);
        bootBar.setLayoutParams(blp);
        bootView.addView(bootBar);

        bootTip = new TextView(this);
        bootTip.setTextSize(12);
        bootTip.setTextColor(T[M3.ON_SURFACE_VARIANT]);
        bootTip.setGravity(Gravity.CENTER);
        bootTip.setPadding(0, dp(16), 0, 0);
        bootView.addView(bootTip);
    }

    private void showBoot() {
        bootView.setVisibility(View.VISIBLE);
        mainView.setVisibility(View.GONE);
    }

    private void showMain() {
        bootView.setVisibility(View.GONE);
        mainView.setVisibility(View.VISIBLE);
        chat.refreshList();
        pollHealth();
    }

    // ---- 菜单 ----

    private void openMenu() {
        Sheet.show(this, "127.0.0.1:" + Env.SERVER_PORT,
                serverUp ? "服务已就绪" : "服务未就绪", serverUp, this);
    }

    @Override
    public void onMenu(int id) {
        switch (id) {
            case Sheet.ID_WEBMODE: toggleWebMode(); break;
            case Sheet.ID_MODEL: showModelPicker(); break;
            case Sheet.ID_RELOAD:
                if (webMode && web != null) web.reload();
                else if (chat != null) chat.refreshList();
                toast("已刷新");
                break;
            case Sheet.ID_COPY_ADDR:
                copy(Env.SERVER_URL);
                toast("已复制 " + Env.SERVER_URL);
                break;
            case Sheet.ID_RESTART:
                toast("重启服务…");
                AlmaService.restartServer();
                break;
            case Sheet.ID_STOP:
                startService(new Intent(this, AlmaService.class).setAction(AlmaService.ACTION_STOP));
                toast("服务已停止");
                break;
            case Sheet.ID_PAIR: doPairing(); break;
            case Sheet.ID_LOGS: showLogs(); break;
            case Sheet.ID_BATTERY:
                try {
                    PowerManager pm = (PowerManager) getSystemService(POWER_SERVICE);
                    if (!pm.isIgnoringBatteryOptimizations(getPackageName())) {
                        Intent i = new Intent(Settings.ACTION_REQUEST_IGNORE_BATTERY_OPTIMIZATIONS);
                        i.setData(Uri.parse("package:" + getPackageName()));
                        startActivity(i);
                    }
                } catch (Throwable ignored) {}
                break;
        }
    }

    // ---- 模型选择 ----

    private void showModelPicker() {
        new Thread(() -> {
            ListHolder h = new ListHolder();
            String err = null;
            try {
                h.models = AlmaApi.models();
                h.current = AlmaApi.defaultModel();
            } catch (Exception e) {
                err = AlmaApi.describe(e);
            }
            final String fe = err;
            main.post(() -> {
                if (fe != null) {
                    toast("读取模型失败：" + fe);
                    return;
                }
                if (h.models.isEmpty()) {
                    toast("没有可用模型：请先在网页模式里配置提供商");
                    return;
                }
                String[] names = new String[h.models.size()];
                int sel = -1;
                for (int i = 0; i < h.models.size(); i++) {
                    AlmaApi.Model m = h.models.get(i);
                    names[i] = m.name + "\n" + m.fullId();
                    if (m.fullId().equals(h.current)) sel = i;
                }
                new AlertDialog.Builder(this)
                        .setTitle("选择模型")
                        .setSingleChoiceItems(names, sel, (d, which) -> {
                            String id = h.models.get(which).fullId();
                            setDefaultModel(id);
                            d.dismiss();
                        })
                        .setNegativeButton("取消", null)
                        .show();
            });
        }, "models").start();
    }

    private static final class ListHolder {
        List<AlmaApi.Model> models = new java.util.ArrayList<>();
        String current;
    }

    private void setDefaultModel(String modelId) {
        new Thread(() -> {
            try {
                String json = httpRaw("/api/settings", "GET", null);
                org.json.JSONObject rootJ = new org.json.JSONObject(json);
                // GET 返回扁平 JSON；PUT 也提交扁平结构（服务端会合并）
                org.json.JSONObject put = new org.json.JSONObject();
                org.json.JSONObject chatJ = rootJ.optJSONObject("chat");
                if (chatJ == null) chatJ = new org.json.JSONObject();
                chatJ.put("defaultModel", modelId);
                put.put("chat", chatJ);
                httpRaw("/api/settings", "PUT", put.toString());
                main.post(() -> {
                    if (modelChip != null) modelChip.setText(shortModel(modelId));
                    toast("默认模型已设为 " + shortModel(modelId));
                });
            } catch (Exception e) {
                main.post(() -> toast("设置失败：" + AlmaApi.describe(e)));
            }
        }, "set-model").start();
    }

    private String shortModel(String id) {
        if (id == null) return "本机";
        int c = id.indexOf(':');
        String name = c >= 0 ? id.substring(c + 1) : id;
        int slash = name.lastIndexOf('/');
        if (slash >= 0) name = name.substring(slash + 1);
        return name.length() > 18 ? name.substring(0, 18) + "…" : name;
    }

    private static String httpRaw(String path, String method, String body) throws Exception {
        java.net.HttpURLConnection c = (java.net.HttpURLConnection)
                new java.net.URL(Env.SERVER_URL + path).openConnection();
        c.setConnectTimeout(8000);
        c.setReadTimeout(10000);
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

    // ---- 网页模式 ----

    private void toggleWebMode() {
        if (webMode) {
            webMode = false;
            if (web != null) {
                contentBox.removeView(web);
                web.destroy();
                web = null;
            }
            chat = new ChatView(this, contentBox, this);
            chat.refreshList();
            toast("已切回原生界面");
            return;
        }
        if (!serverUp) {
            toast("服务未就绪");
            return;
        }
        webMode = true;
        contentBox.removeAllViews();
        web = new WebView(this);
        WebSettings ws = web.getSettings();
        ws.setJavaScriptEnabled(true);
        ws.setDomStorageEnabled(true);
        ws.setDatabaseEnabled(true);
        ws.setUseWideViewPort(true);
        ws.setLoadWithOverviewMode(true);
        ws.setMixedContentMode(WebSettings.MIXED_CONTENT_COMPATIBILITY_MODE);
        CookieManager.getInstance().setAcceptCookie(true);
        CookieManager.getInstance().setAcceptThirdPartyCookies(web, true);
        web.setWebViewClient(new WebViewClient() {
            @Override
            public boolean shouldOverrideUrlLoading(WebView v, WebResourceRequest r) {
                Uri u = r.getUrl();
                String s = u.getScheme();
                if ("http".equals(s) || "https".equals(s)) {
                    String h = u.getHost();
                    if ("127.0.0.1".equals(h) || "localhost".equals(h)) return false;
                    try { startActivity(new Intent(Intent.ACTION_VIEW, u)); } catch (Exception ignored) {}
                    return true;
                }
                return false;
            }
        });
        web.setLayoutParams(new FrameLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT));
        contentBox.addView(web);
        web.loadUrl(Env.SERVER_URL);
        toast("已切到网页模式（完整功能）");
    }

    // ---- 日志 ----

    // ---- 配对官方 App（直连本机 local relay，不走云端中继） ----

    private void doPairing() {
        toast("生成配对链接…");
        Pairing.buildLink(true, (link, reason) -> main.post(() -> {
            if (link == null) {
                new AlertDialog.Builder(this)
                        .setTitle("配对官方 App")
                        .setMessage("生成失败：" + reason)
                        .setPositiveButton("好", null).show();
                return;
            }
            showPairingDialog(link);
        }));
    }

    private void showPairingDialog(String link) {
        LinearLayout box = new LinearLayout(this);
        box.setOrientation(LinearLayout.VERTICAL);
        box.setPadding(dp(22), dp(14), dp(22), dp(6));

        TextView tip = new TextView(this);
        tip.setTextSize(13);
        tip.setTextColor(T[M3.ON_SURFACE]);
        tip.setText("点「打开官方 App」直接跳转配对；\n"
                + "或在官方 App 的远程访问页粘贴此链接。\n"
                + "此链接指向本机 " + Env.LOCAL_RELAY_PORT + " 端口，不经过官方服务器。");
        tip.setPadding(0, 0, 0, dp(10));
        box.addView(tip);

        TextView tv = new TextView(this);
        tv.setTextSize(11);
        tv.setTypeface(Typeface.MONOSPACE);
        tv.setTextColor(T[M3.ON_SURFACE_VARIANT]);
        tv.setTextIsSelectable(true);
        tv.setText(link);
        tv.setBackground(pill(T[M3.SURFACE_CONTAINER], 12));
        tv.setPadding(dp(12), dp(10), dp(12), dp(10));
        box.addView(tv);

        new AlertDialog.Builder(this)
                .setTitle("配对官方 Alma App")
                .setView(box)
                .setPositiveButton("打开官方 App", (d, w) -> {
                    try {
                        Intent i = new Intent(Intent.ACTION_VIEW, Uri.parse(link));
                        i.setPackage(Env.OFFICIAL_PKG);
                        startActivity(i);
                    } catch (Exception e) {
                        copy(link);
                        toast("未装官方 App，链接已复制");
                    }
                })
                .setNeutralButton("复制链接", (d, w) -> { copy(link); toast("已复制"); })
                .setNegativeButton("关闭", null)
                .show();
    }

    private void showLogs() {
        String log = AlmaService.logTail(this, 60 * 1024);
        ScrollView sv = new ScrollView(this);
        TextView tv = new TextView(this);
        tv.setText(log);
        tv.setTextSize(11);
        tv.setTypeface(Typeface.MONOSPACE);
        tv.setTextColor(T[M3.ON_SURFACE]);
        tv.setPadding(dp(14), dp(12), dp(14), dp(12));
        sv.addView(tv);
        new AlertDialog.Builder(this)
                .setTitle("服务日志（尾部 60KB）")
                .setView(sv)
                .setPositiveButton("刷新", (d, w) -> showLogs())
                .setNegativeButton("关闭", null)
                .show();
    }

    private void copy(String s) {
        ClipboardManager cm = (ClipboardManager) getSystemService(Context.CLIPBOARD_SERVICE);
        cm.setPrimaryClip(ClipData.newPlainText("alma", s));
    }

    private void toast(String s) {
        Toast.makeText(this, s, Toast.LENGTH_SHORT).show();
    }

    @Override
    public void onBackPressed() {
        if (chat != null && !webMode && chat.onBack()) return;
        if (webMode && web != null && web.canGoBack()) {
            web.goBack();
            return;
        }
        moveTaskToBack(true);
    }
}