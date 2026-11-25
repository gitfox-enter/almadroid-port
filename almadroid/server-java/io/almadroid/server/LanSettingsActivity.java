package io.almadroid.server;

import android.app.Activity;
import android.content.ClipData;
import android.content.ClipboardManager;
import android.content.Intent;
import android.graphics.Color;
import android.graphics.Typeface;
import android.graphics.drawable.GradientDrawable;
import android.net.Uri;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.view.Gravity;
import android.view.ViewGroup;
import android.widget.CompoundButton;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.Switch;
import android.widget.TextView;
import android.widget.Toast;

/** 局域网访问设置：开关、端口、VNC 端口、地址展示与复制。 */
public class LanSettingsActivity extends Activity {
    private final Handler main = new Handler(Looper.getMainLooper());

    private Switch swEnable;
    private EditText etPort;
    private EditText etVncPort;
    private TextView tvUrl;
    private TextView tvVncUrl;
    private TextView tvStatus;

    @Override
    protected void onCreate(Bundle b) {
        super.onCreate(b);
        buildUi();
    }

    @Override
    protected void onResume() {
        super.onResume();
        refresh();
    }

    private int dp(int v) {
        return Math.round(v * getResources().getDisplayMetrics().density);
    }

    private LinearLayout card() {
        LinearLayout c = new LinearLayout(this);
        c.setOrientation(LinearLayout.VERTICAL);
        GradientDrawable bg = new GradientDrawable();
        bg.setColor(0xFF1E1714);
        bg.setCornerRadius(dp(16));
        bg.setStroke(dp(1), 0xFF2E2521);
        c.setBackground(bg);
        c.setPadding(dp(16), dp(14), dp(16), dp(14));
        return c;
    }

    private TextView label(String s, int color, float size, boolean bold) {
        TextView t = new TextView(this);
        t.setText(s);
        t.setTextSize(size);
        t.setTextColor(color);
        if (bold) t.setTypeface(null, Typeface.BOLD);
        return t;
    }

    private LinearLayout.LayoutParams mp(int top) {
        LinearLayout.LayoutParams p = new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        p.topMargin = dp(top);
        return p;
    }

    private void buildUi() {
        LinearLayout root = new LinearLayout(this);
        root.setOrientation(LinearLayout.VERTICAL);
        root.setBackgroundColor(0xFF130D0A);
        int p = dp(20);
        root.setPadding(p, dp(48), p, dp(24));
        setContentView(root);

        TextView title = label("局域网访问", 0xFFF9E0D8, 22, true);
        root.addView(title);
        root.addView(label("同一 WiFi 下的任何设备，用浏览器即可使用 Alma", 0xFF84716B, 12, false), mp(6));

        // ---- 卡片 1：开关与端口 ----
        LinearLayout c1 = card();
        root.addView(c1, mp(18));

        LinearLayout rowSw = new LinearLayout(this);
        rowSw.setOrientation(LinearLayout.HORIZONTAL);
        rowSw.setGravity(Gravity.CENTER_VERTICAL);
        swEnable = new Switch(this);
        swEnable.setText("启用局域网入口");
        swEnable.setTextColor(0xFFF9E0D8);
        swEnable.setTextSize(14);
        rowSw.addView(swEnable, new LinearLayout.LayoutParams(
                0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f));
        c1.addView(rowSw);
        c1.addView(label("关闭后仅本机可用（127.0.0.1）", 0xFF84716B, 11, false), mp(4));

        c1.addView(label("网页端口（默认 23017）", 0xFFBCA69F, 12, false), mp(14));
        etPort = new EditText(this);
        etPort.setInputType(android.text.InputType.TYPE_CLASS_NUMBER);
        etPort.setTextColor(Color.WHITE);
        etPort.setTextSize(14);
        GradientDrawable eb = new GradientDrawable();
        eb.setColor(0xFF241B17);
        eb.setCornerRadius(dp(10));
        eb.setStroke(dp(1), 0xFF3A2E28);
        etPort.setBackground(eb);
        etPort.setPadding(dp(12), dp(10), dp(12), dp(10));
        c1.addView(etPort, mp(6));

        c1.addView(label("桌面 VNC 端口（默认 5900）", 0xFFBCA69F, 12, false), mp(14));
        etVncPort = new EditText(this);
        etVncPort.setInputType(android.text.InputType.TYPE_CLASS_NUMBER);
        etVncPort.setTextColor(Color.WHITE);
        etVncPort.setTextSize(14);
        GradientDrawable eb2 = new GradientDrawable();
        eb2.setColor(0xFF241B17);
        eb2.setCornerRadius(dp(10));
        eb2.setStroke(dp(1), 0xFF3A2E28);
        etVncPort.setBackground(eb2);
        etVncPort.setPadding(dp(12), dp(10), dp(12), dp(10));
        c1.addView(etVncPort, mp(6));

        TextView save = label("保存（重启应用后生效）", 0xFF130D0A, 14, true);
        save.setGravity(Gravity.CENTER);
        GradientDrawable sb = new GradientDrawable();
        sb.setColor(0xFFF8B8A0);
        sb.setCornerRadius(dp(12));
        save.setBackground(sb);
        save.setPadding(0, dp(12), 0, dp(12));
        save.setOnClickListener(new android.view.View.OnClickListener() {
            public void onClick(android.view.View v) { save(); }
        });
        c1.addView(save, mp(16));

        // ---- 卡片 2：地址 ----
        LinearLayout c2 = card();
        root.addView(c2, mp(14));
        c2.addView(label("本机网页地址", 0xFFF9E0D8, 13, true));
        tvUrl = new TextView(this);
        tvUrl.setTextSize(13);
        tvUrl.setTextColor(0xFF7FD8B8);
        tvUrl.setTextIsSelectable(true);
        c2.addView(tvUrl, mp(6));
        TextView copy1 = label("复制网页地址", 0xFFF8B8A0, 12, false);
        copy1.setPadding(0, dp(8), 0, 0);
        copy1.setOnClickListener(new android.view.View.OnClickListener() {
            public void onClick(android.view.View v) { copy(tvUrl.getText().toString()); }
        });
        c2.addView(copy1);

        c2.addView(label("桌面（noVNC · 本机打开永不失效）", 0xFFF9E0D8, 13, true), mp(14));
        tvVncUrl = new TextView(this);
        tvVncUrl.setTextSize(13);
        tvVncUrl.setTextColor(0xFF7FD8B8);
        tvVncUrl.setTextIsSelectable(true);
        c2.addView(tvVncUrl, mp(6));
        LinearLayout rowBtn = new LinearLayout(this);
        rowBtn.setOrientation(LinearLayout.HORIZONTAL);
        TextView copy2 = label("复制桌面地址", 0xFFF8B8A0, 12, false);
        copy2.setPadding(0, dp(8), dp(24), 0);
        copy2.setOnClickListener(new android.view.View.OnClickListener() {
            public void onClick(android.view.View v) { copy(tvVncUrl.getText().toString()); }
        });
        rowBtn.addView(copy2);
        TextView open = label("打开桌面", 0xFFF8B8A0, 12, false);
        open.setPadding(0, dp(8), dp(24), 0);
        open.setOnClickListener(new android.view.View.OnClickListener() {
            public void onClick(android.view.View v) {
                try {
                    startActivity(new Intent(LanSettingsActivity.this, DesktopActivity.class));
                } catch (Throwable t) { toast("桌面启动失败"); }
            }
        });
        rowBtn.addView(open);
        // 管理网页：添加/编辑模型供应商、设置等完整功能都在这里
        TextView web = label("打开管理网页（模型供应商）", 0xFFF8B8A0, 12, false);
        web.setPadding(0, dp(8), 0, 0);
        web.setOnClickListener(new android.view.View.OnClickListener() {
            public void onClick(android.view.View v) {
                // 用回环地址：局域网 IP 会随 DHCP 变化而失效
                String u = "http://127.0.0.1:" + LanSettings.port(LanSettingsActivity.this) + "/";
                if (u.startsWith("http")) {
                    try {
                        startActivity(new Intent(Intent.ACTION_VIEW, Uri.parse(u))
                                .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK));
                    } catch (Throwable t) { toast("没有可用的浏览器"); }
                } else toast("暂无地址");
            }
        });
        rowBtn.addView(web);
        c2.addView(rowBtn);
        c2.addView(label("App 原生端不含供应商管理（官方设计：手机是遥控器）。添加/编辑模型供应商请用上面的管理网页，和 App 共用同一个本机服务，改完立即生效。", 0xFF84716B, 11, false), mp(8));
        c2.addView(label("桌面是本机 Linux 的 1280×720 虚拟屏；computer 页在纯手机部署下不可用（需桌面端），请用这里的桌面。", 0xFF84716B, 11, false), mp(8));

        // ---- 卡片 3：状态 ----
        LinearLayout c3 = card();
        root.addView(c3, mp(14));
        c3.addView(label("服务状态", 0xFFF9E0D8, 13, true));
        tvStatus = new TextView(this);
        tvStatus.setTextSize(12);
        tvStatus.setTextColor(0xFFBCA69F);
        c3.addView(tvStatus, mp(6));

        refresh();
    }

    private void refresh() {
        swEnable.setChecked(LanSettings.enabled(this));
        etPort.setText(String.valueOf(LanSettings.port(this)));
        etVncPort.setText(String.valueOf(LanSettings.vncPort(this)));

        String url = LanSettings.lanUrl(this);
        String vnc = "http://127.0.0.1:" + LanSettings.port(this) + "/vnc";
        tvUrl.setText(url != null ? url + "  （给其他设备用，IP 变了会失效）" : "未连接 WiFi");
        tvVncUrl.setText(vnc + "  （本机打开桌面）");

        final boolean up = LocalService.isPortOpenPublic(LanSettings.port(this));
        main.post(new Runnable() { public void run() {
            tvStatus.setText(up ? "● 服务运行中 · 局域网入口已监听"
                                : "○ 服务未运行（先打开 Alma 主界面）");
            tvStatus.setTextColor(up ? 0xFF7FD8B8 : 0xFF9A8A84);
        }});
    }

    private void save() {
        int port;
        int vnc;
        try { port = Integer.parseInt(etPort.getText().toString().trim()); }
        catch (Throwable t) { toast("网页端口必须是数字"); return; }
        try { vnc = Integer.parseInt(etVncPort.getText().toString().trim()); }
        catch (Throwable t) { toast("VNC 端口必须是数字"); return; }
        if (!LanSettings.valid(port)) { toast("网页端口需在 " + LanSettings.MIN_PORT + "–" + LanSettings.MAX_PORT); return; }
        if (!LanSettings.valid(vnc)) { toast("VNC 端口需在 " + LanSettings.MIN_PORT + "–" + LanSettings.MAX_PORT); return; }
        LanSettings.setEnabled(this, swEnable.isChecked());
        LanSettings.setPort(this, port);
        LanSettings.setVncPort(this, vnc);
        toast("已保存 · 重启应用后生效");
        refresh();
    }

    private void copy(String s) {
        if (s == null || !s.startsWith("http")) { toast("暂无地址"); return; }
        try {
            ClipboardManager cm = (ClipboardManager) getSystemService(CLIPBOARD_SERVICE);
            cm.setPrimaryClip(ClipData.newPlainText("Alma LAN", s));
            toast("已复制");
        } catch (Throwable t) { toast(s); }
    }

    private void toast(String s) {
        Toast.makeText(this, s, Toast.LENGTH_SHORT).show();
    }
}