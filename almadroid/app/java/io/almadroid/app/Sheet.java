package io.almadroid.app;

import android.app.Activity;
import android.app.Dialog;
import android.graphics.Bitmap;
import android.graphics.Typeface;
import android.graphics.drawable.GradientDrawable;
import android.view.Gravity;
import android.view.View;
import android.view.ViewGroup;
import android.view.Window;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;

/** Material 3 底部弹层菜单。 */
final class Sheet {

    interface Host {
        void onMenu(int id);
    }

    static final int ID_WEBMODE = 1;
    static final int ID_MODEL = 2;
    static final int ID_RELOAD = 3;
    static final int ID_COPY_ADDR = 4;
    static final int ID_RESTART = 5;
    static final int ID_STOP = 6;
    static final int ID_LOGS = 7;
    static final int ID_BATTERY = 8;
    static final int ID_PAIR = 9;

    private static int dp(float den, int v) {
        return Math.round(v * den);
    }

    static void show(Activity act, String addr, String extra, boolean serverUp, Host host) {
        int[] T = M3.current(act);
        float den = act.getResources().getDisplayMetrics().density;

        Dialog dialog = new Dialog(act);
        dialog.requestWindowFeature(Window.FEATURE_NO_TITLE);
        LinearLayout root = new LinearLayout(act);
        root.setOrientation(LinearLayout.VERTICAL);
        GradientDrawable bg = new GradientDrawable();
        bg.setColor(T[M3.SURFACE_CONTAINER_LOWEST]);
        bg.setCornerRadii(new float[]{dp(den, 28), dp(den, 28), dp(den, 28), dp(den, 28), 0, 0, 0, 0});
        root.setBackground(bg);
        root.setPadding(dp(den, 20), dp(den, 10), dp(den, 20), dp(den, 28));

        View handle = new View(act);
        GradientDrawable hg = new GradientDrawable();
        hg.setColor(T[M3.OUTLINE_VARIANT]);
        hg.setCornerRadius(dp(den, 2));
        LinearLayout.LayoutParams hlp = new LinearLayout.LayoutParams(dp(den, 36), dp(den, 4));
        hlp.gravity = Gravity.CENTER_HORIZONTAL;
        hlp.bottomMargin = dp(den, 14);
        handle.setLayoutParams(hlp);
        handle.setBackground(hg);
        root.addView(handle);

        TextView title = new TextView(act);
        title.setText("菜单");
        title.setTextSize(24);
        title.setTypeface(null, Typeface.BOLD);
        title.setTextColor(T[M3.ON_SURFACE]);
        root.addView(title);

        TextView sub = new TextView(act);
        sub.setText("本机服务 " + addr + (extra.isEmpty() ? "" : " · " + extra));
        sub.setTextSize(13);
        sub.setTextColor(T[M3.ON_SURFACE_VARIANT]);
        sub.setPadding(0, dp(den, 4), 0, dp(den, 8));
        root.addView(sub);

        boolean batteryIgnored = batteryIgnored(act);
        item(act, dialog, root, T, den, ID_WEBMODE, Icons.Kind.GLOBE, "切换网页模式",
                "原生界面 / 完整网页界面 互换", serverUp, host);
        item(act, dialog, root, T, den, ID_MODEL, Icons.Kind.INFO, "选择模型",
                "设置默认对话模型", serverUp, host);
        item(act, dialog, root, T, den, ID_PAIR, Icons.Kind.PAIR, "配对官方 Alma App",
                "生成链接，用官方 App 的原生界面直连本机", serverUp, host);
        item(act, dialog, root, T, den, ID_RELOAD, Icons.Kind.RELOAD, "刷新",
                "重新载入对话列表 / 网页", true, host);
        item(act, dialog, root, T, den, ID_COPY_ADDR, Icons.Kind.COPY, "复制服务地址",
                "把 " + addr + " 复制到剪贴板", true, host);
        item(act, dialog, root, T, den, ID_RESTART, Icons.Kind.RESTART, "重启服务",
                "先停掉再启动，改过配置后用得上", true, host);
        item(act, dialog, root, T, den, ID_STOP, Icons.Kind.STOP, "停止服务",
                "停止本机 Alma 引擎", serverUp, host);
        item(act, dialog, root, T, den, ID_LOGS, Icons.Kind.LOGS, "查看日志",
                "服务最后的输出", true, host);
        item(act, dialog, root, T, den, ID_BATTERY, Icons.Kind.BATTERY,
                batteryIgnored ? "已忽略电池优化" : "忽略电池优化",
                "让系统不要在熄屏后冻结服务", !batteryIgnored, host);

        dialog.setContentView(root);
        Window w = dialog.getWindow();
        if (w != null) {
            w.setBackgroundDrawableResource(android.R.color.transparent);
            w.setGravity(Gravity.BOTTOM | Gravity.CENTER_HORIZONTAL);
            w.setLayout(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
            root.post(() -> {
                int screenH = act.getResources().getDisplayMetrics().heightPixels;
                int want = Math.min(root.getHeight() + dp(den, 28), screenH * 3 / 4);
                w.setLayout(ViewGroup.LayoutParams.MATCH_PARENT, Math.max(want, dp(den, 320)));
            });
        }
        dialog.show();
    }

    private static boolean batteryIgnored(Activity act) {
        try {
            android.os.PowerManager pm = (android.os.PowerManager) act.getSystemService(Activity.POWER_SERVICE);
            return pm.isIgnoringBatteryOptimizations(act.getPackageName());
        } catch (Throwable t) {
            return false;
        }
    }

    private static void item(Activity act, Dialog dialog, LinearLayout root, int[] T, float den,
                             int id, Icons.Kind kind, String title, String desc,
                             boolean enabled, Host host) {
        LinearLayout row = new LinearLayout(act);
        row.setOrientation(LinearLayout.HORIZONTAL);
        row.setGravity(Gravity.CENTER_VERTICAL);
        row.setPadding(dp(den, 8), dp(den, 10), dp(den, 8), dp(den, 10));
        row.setLayoutParams(new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT));

        GradientDrawable tileBg = new GradientDrawable();
        tileBg.setColor(enabled ? M3.blend(T[M3.PRIMARY], T[M3.SURFACE_CONTAINER_LOWEST], 0.14f)
                : T[M3.SURFACE_CONTAINER]);
        tileBg.setCornerRadius(dp(den, 14));
        ImageView tile = new ImageView(act);
        int iconColor = enabled ? T[M3.PRIMARY] : T[M3.OUTLINE];
        Bitmap bm = Icons.make(den, kind, iconColor, 22);
        tile.setImageBitmap(bm);
        tile.setBackground(tileBg);
        tile.setPadding(dp(den, 11), dp(den, 11), dp(den, 11), dp(den, 11));
        LinearLayout.LayoutParams tlp = new LinearLayout.LayoutParams(dp(den, 44), dp(den, 44));
        tlp.rightMargin = dp(den, 16);
        tile.setLayoutParams(tlp);
        row.addView(tile);

        LinearLayout textCol = new LinearLayout(act);
        textCol.setOrientation(LinearLayout.VERTICAL);
        TextView tt = new TextView(act);
        tt.setText(title);
        tt.setTextSize(16);
        tt.setTypeface(null, Typeface.BOLD);
        tt.setTextColor(enabled ? T[M3.ON_SURFACE] : T[M3.OUTLINE]);
        textCol.addView(tt);
        TextView td = new TextView(act);
        td.setText(desc);
        td.setTextSize(13);
        td.setTextColor(enabled ? T[M3.ON_SURFACE_VARIANT] : T[M3.OUTLINE_VARIANT]);
        td.setPadding(0, dp(den, 1), 0, 0);
        textCol.addView(td);
        textCol.setLayoutParams(new LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f));
        row.addView(textCol);

        if (enabled) {
            row.setClickable(true);
            row.setFocusable(true);
            row.setOnClickListener(v -> {
                dialog.dismiss();
                host.onMenu(id);
            });
        } else {
            row.setAlpha(0.55f);
        }
        root.addView(row);
    }
}