package io.almadroid.server;

import android.annotation.SuppressLint;
import android.app.Activity;
import android.content.Intent;
import android.graphics.Color;
import android.os.Build;
import android.os.Bundle;
import android.view.View;
import android.view.ViewGroup;
import android.webkit.WebChromeClient;
import android.webkit.WebSettings;
import android.webkit.WebView;
import android.webkit.WebViewClient;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.window.OnBackInvokedCallback;
import android.window.OnBackInvokedDispatcher;

/** Linux 桌面入口：优先打开 Termux:X11 原生渲染窗口（com.termux.x11.MainActivity）；
 *  X server 未就绪时回退到 noVNC WebView（自动连接 23017/vnc）。 */
public class DesktopActivity extends Activity {
    private WebView web;
    private VncView vnc;
    private OnBackInvokedCallback backCb;
    private boolean usedX11;

    @SuppressLint("SetJavaScriptEnabled")
    @Override
    protected void onCreate(Bundle b) {
        super.onCreate(b);

        // Termux:X11 原生窗口暂关（AddExtensions failed，X server 未启动）。
        // 直接走 noVNC WebView，避免打开连不上 X 的黑屏 Activity。
        // X server 修好后恢复：
        // try {
        //     Intent x = new Intent();
        //     x.setClassName(this, "com.termux.x11.MainActivity");
        //     x.addFlags(Intent.FLAG_ACTIVITY_REORDER_TO_FRONT);
        //     startActivity(x);
        //     usedX11 = true;
        //     finish();
        //     return;
        // } catch (Throwable t) { }

        // 横屏：桌面 1280x720 在手机上铺满（tiny/aVNC 同款体验）
        setRequestedOrientation(android.content.pm.ActivityInfo.SCREEN_ORIENTATION_SENSOR_LANDSCAPE);

        final LinearLayout root = new LinearLayout(this);
        root.setOrientation(LinearLayout.VERTICAL);
        root.setBackgroundColor(0xFF000000);
        setContentView(root);

        // 主路径：原生 VNC 客户端（RFB over unix socket → TCP 兜底），tiny 同款零浏览器开销
        // 兜底：Xtigervnc/x11vnc 未就绪时退回 noVNC WebView（relay /vnc）
        final android.widget.FrameLayout holder = new android.widget.FrameLayout(this);
        root.addView(holder, new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, 0, 1f));

        final TextView bar = new TextView(this);
        bar.setText("Linux 桌面 · 触摸操作 · 返回键退出      ↻ 重连");
        bar.setTextSize(13);
        bar.setTextColor(0xFFF8B8A0);
        bar.setBackgroundColor(0xFF1E1714);
        bar.setPadding(dp(16), dp(10), dp(16), dp(10));
        root.addView(bar, new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT));

        bar.setOnClickListener(new View.OnClickListener() {
            public void onClick(View v) { mountVnc(holder); }
        });

        mountVnc(holder);

        if (Build.VERSION.SDK_INT >= 33) {
            backCb = new OnBackInvokedCallback() {
                @Override
                public void onBackInvoked() { finish(); }
            };
            getOnBackInvokedDispatcher().registerOnBackInvokedCallback(
                    OnBackInvokedDispatcher.PRIORITY_DEFAULT, backCb);
        }
    }

    /** 原生 VNC（tiny 同款）：Xtigervnc 在 TCP 5900 直供（无密码+localhost），
     *  失败退回 noVNC WebView 兜底。unix socket 通道预留给后续切换 rfbunixpath。 */
    private void mountVnc(android.widget.FrameLayout holder) {
        holder.removeAllViews();
        VncView v = new VncView(this, null, 5900, new VncView.Listener() {
            public void onConnected(int w, int h) { }
            public void onError(String m) {
                vnc = null;
                runOnUiThread(new Runnable() { public void run() { mountWeb(holder, m); } });
            }
        });
        vnc = v;
        holder.addView(v, new android.widget.FrameLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT));
        v.requestFocus();
        v.connect();
    }

    /** noVNC WebView 兜底（Xtigervnc/x11vnc 未就绪或连接失败时）。 */
    private void mountWeb(android.widget.FrameLayout holder, String why) {
        holder.removeAllViews();
        if (web == null) {
            web = new WebView(this);
            WebSettings s = web.getSettings();
            s.setJavaScriptEnabled(true);
            s.setDomStorageEnabled(true);
            web.setWebViewClient(new WebViewClient());
            web.setWebChromeClient(new WebChromeClient());
            web.setBackgroundColor(Color.BLACK);
        }
        holder.addView(web, new android.widget.FrameLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT));
        web.loadUrl("http://127.0.0.1:" + LanSettings.port(this) + "/vnc");
    }

    @Override
    public void onBackPressed() {
        finish();
    }

    @Override
    protected void onDestroy() {
        if (vnc != null) { vnc.shutdown(); vnc = null; }
        // 退出桌面后延时重新武装「电脑」跳转：
        // R2 页在返回瞬间会重组（armed=false 不跳），2s 后再武装供下次抽屉点击
        new Thread(new Runnable() { public void run() {
            try { Thread.sleep(2000); } catch (InterruptedException ignored) {}
            LocalBootstrap.rearmDesktopJump();
        }}).start();
        if (Build.VERSION.SDK_INT >= 33 && backCb != null) {
            try {
                getOnBackInvokedDispatcher().unregisterOnBackInvokedCallback(backCb);
            } catch (Throwable ignored) {}
        }
        if (web != null) {
            web.loadUrl("about:blank");
            web.destroy();
        }
        super.onDestroy();
    }

    private int dp(int v) {
        return Math.round(v * getResources().getDisplayMetrics().density);
    }
}
