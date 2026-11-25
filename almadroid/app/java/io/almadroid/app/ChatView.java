package io.almadroid.app;

import android.app.Activity;
import android.graphics.Color;
import android.graphics.Typeface;
import android.graphics.drawable.GradientDrawable;
import android.text.InputType;
import android.text.TextUtils;
import android.view.Gravity;
import android.view.View;
import android.view.ViewGroup;
import android.widget.EditText;
import android.widget.FrameLayout;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ProgressBar;
import android.widget.ScrollView;
import android.widget.TextView;
import android.widget.Toast;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Locale;

/**
 * 原生聊天界面（Alma 风格）：对话列表 ⇄ 对话详情，全程对接本机 alma-server。
 * 不依赖 WebView，不需要官方 App。
 */
final class ChatView {

    interface Host {
        int[] tokens();
        int dp(int v);
        void onOpenSettings();
    }

    private final Activity act;
    private final Host host;
    private final FrameLayout root;

    private LinearLayout listScreen;
    private LinearLayout chatScreen;
    private LinearLayout threadContainer;
    private ScrollView threadScroll;
    private EditText input;
    private TextView sendBtn;
    private TextView titleText;
    private TextView emptyHint;

    private String currentThreadId;
    private String currentTitle;
    private String currentModel;
    private final List<AlmaApi.Msg> msgs = new ArrayList<>();
    private boolean generating;

    ChatView(Activity act, FrameLayout container, Host host) {
        this.act = act;
        this.host = host;
        this.root = container;
        container.setBackgroundColor(host.tokens()[M3.SURFACE]);
        buildList();
        buildChat();
        container.addView(listScreen);
        container.addView(chatScreen);
        chatScreen.setVisibility(View.GONE);
    }

    // ---- 对话列表 ----

    private void buildList() {
        int[] T = host.tokens();
        listScreen = new LinearLayout(act);
        listScreen.setOrientation(LinearLayout.VERTICAL);
        listScreen.setBackgroundColor(T[M3.SURFACE]);

        LinearLayout head = new LinearLayout(act);
        head.setOrientation(LinearLayout.HORIZONTAL);
        head.setGravity(Gravity.CENTER_VERTICAL);
        head.setPadding(host.dp(18), host.dp(14), host.dp(12), host.dp(10));

        TextView t = new TextView(act);
        t.setText("对话");
        t.setTextSize(26);
        t.setTypeface(null, Typeface.BOLD);
        t.setTextColor(T[M3.ON_SURFACE]);
        t.setLayoutParams(new LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f));
        head.addView(t);

        TextView newBtn = chip("＋ 新对话", T[M3.PRIMARY], T[M3.ON_PRIMARY]);
        newBtn.setOnClickListener(v -> newThread());
        head.addView(newBtn);
        listScreen.addView(head);

        threadContainer = new LinearLayout(act);
        threadContainer.setOrientation(LinearLayout.VERTICAL);
        threadContainer.setPadding(host.dp(12), 0, host.dp(12), host.dp(12));
        ScrollView sc = new ScrollView(act);
        sc.addView(threadContainer);
        sc.setLayoutParams(new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, 0, 1f));
        listScreen.addView(sc);

        emptyHint = new TextView(act);
        emptyHint.setText("还没有对话\n点右上角「新对话」开始");
        emptyHint.setTextSize(14);
        emptyHint.setTextColor(T[M3.ON_SURFACE_VARIANT]);
        emptyHint.setGravity(Gravity.CENTER);
        emptyHint.setPadding(0, host.dp(80), 0, 0);
        threadContainer.addView(emptyHint);
    }

    void refreshList() {
        new Thread(() -> {
            List<AlmaApi.ChatThread> ts = null;
            String err = null;
            try {
                ts = AlmaApi.threads();
            } catch (Exception e) {
                err = AlmaApi.describe(e);
            }
            final List<AlmaApi.ChatThread> fts = ts;
            final String ferr = err;
            act.runOnUiThread(() -> renderList(fts, ferr));
        }, "threads").start();
    }

    private void renderList(List<AlmaApi.ChatThread> ts, String err) {
        int[] T = host.tokens();
        threadContainer.removeAllViews();
        if (err != null) {
            TextView tv = new TextView(act);
            tv.setText("读取对话失败：" + err);
            tv.setTextSize(13);
            tv.setTextColor(T[M3.ERROR]);
            tv.setPadding(host.dp(8), host.dp(20), host.dp(8), 0);
            threadContainer.addView(tv);
            return;
        }
        if (ts == null || ts.isEmpty()) {
            TextView tv = new TextView(act);
            tv.setText("还没有对话\n点右上角「新对话」开始");
            tv.setTextSize(14);
            tv.setTextColor(T[M3.ON_SURFACE_VARIANT]);
            tv.setGravity(Gravity.CENTER);
            tv.setPadding(0, host.dp(80), 0, 0);
            threadContainer.addView(tv);
            return;
        }
        for (AlmaApi.ChatThread th : ts) {
            threadContainer.addView(threadRow(th, T));
        }
    }

    private View threadRow(AlmaApi.ChatThread th, int[] T) {
        LinearLayout row = new LinearLayout(act);
        row.setOrientation(LinearLayout.HORIZONTAL);
        row.setGravity(Gravity.CENTER_VERTICAL);
        GradientDrawable bg = new GradientDrawable();
        bg.setColor(T[M3.SURFACE_CONTAINER_LOW]);
        bg.setCornerRadius(host.dp(16));
        row.setBackground(bg);
        row.setPadding(host.dp(14), host.dp(13), host.dp(14), host.dp(13));
        LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        lp.bottomMargin = host.dp(8);
        row.setLayoutParams(lp);

        LinearLayout col = new LinearLayout(act);
        col.setOrientation(LinearLayout.VERTICAL);
        TextView title = new TextView(act);
        title.setText(TextUtils.isEmpty(th.title) ? "新对话" : th.title);
        title.setTextSize(15);
        title.setTextColor(T[M3.ON_SURFACE]);
        title.setSingleLine(true);
        title.setEllipsize(TextUtils.TruncateAt.END);
        col.addView(title);

        TextView meta = new TextView(act);
        String when = th.lastAt > 0
                ? new SimpleDateFormat("MM-dd HH:mm", Locale.getDefault()).format(new Date(th.lastAt))
                : "";
        meta.setText(when + (th.generating ? "  ·  生成中…" : ""));
        meta.setTextSize(11);
        meta.setTextColor(T[M3.ON_SURFACE_VARIANT]);
        col.addView(meta);
        col.setLayoutParams(new LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f));
        row.addView(col);

        TextView del = new TextView(act);
        del.setText("✕");
        del.setTextSize(15);
        del.setTextColor(T[M3.OUTLINE]);
        del.setPadding(host.dp(10), host.dp(2), host.dp(4), host.dp(2));
        del.setOnClickListener(v -> confirmDelete(th));
        row.addView(del);

        row.setOnClickListener(v -> openThread(th.id, th.title, th.model));
        return row;
    }

    private void confirmDelete(AlmaApi.ChatThread th) {
        new android.app.AlertDialog.Builder(act)
                .setTitle("删除对话")
                .setMessage("删除「" + th.title + "」？此操作不可撤销。")
                .setPositiveButton("删除", (d, w) -> {
                    new Thread(() -> {
                        AlmaApi.deleteThread(th.id);
                        act.runOnUiThread(this::refreshList);
                    }).start();
                })
                .setNegativeButton("取消", null)
                .show();
    }

    // ---- 对话详情 ----

    private void buildChat() {
        int[] T = host.tokens();
        chatScreen = new LinearLayout(act);
        chatScreen.setOrientation(LinearLayout.VERTICAL);
        chatScreen.setBackgroundColor(T[M3.SURFACE]);

        LinearLayout head = new LinearLayout(act);
        head.setOrientation(LinearLayout.HORIZONTAL);
        head.setGravity(Gravity.CENTER_VERTICAL);
        head.setPadding(host.dp(8), host.dp(10), host.dp(14), host.dp(8));

        TextView back = new TextView(act);
        back.setText("‹");
        back.setTextSize(26);
        back.setTextColor(T[M3.ON_SURFACE]);
        back.setPadding(host.dp(10), 0, host.dp(10), host.dp(4));
        back.setOnClickListener(v -> showList());
        head.addView(back);

        titleText = new TextView(act);
        titleText.setText("对话");
        titleText.setTextSize(17);
        titleText.setTypeface(null, Typeface.BOLD);
        titleText.setTextColor(T[M3.ON_SURFACE]);
        titleText.setSingleLine(true);
        titleText.setEllipsize(TextUtils.TruncateAt.END);
        titleText.setLayoutParams(new LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f));
        head.addView(titleText);

        TextView cfg = new TextView(act);
        cfg.setText("模型");
        cfg.setTextSize(12);
        cfg.setTextColor(T[M3.PRIMARY]);
        cfg.setPadding(host.dp(10), host.dp(6), host.dp(6), host.dp(6));
        cfg.setOnClickListener(v -> host.onOpenSettings());
        head.addView(cfg);
        chatScreen.addView(head);

        threadScroll = new ScrollView(act);
        threadScroll.setLayoutParams(new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, 0, 1f));
        threadContainer = new LinearLayout(act);
        threadContainer.setOrientation(LinearLayout.VERTICAL);
        threadContainer.setPadding(host.dp(12), host.dp(6), host.dp(12), host.dp(10));
        threadScroll.addView(threadContainer);
        chatScreen.addView(threadScroll);

        // 输入区
        LinearLayout bar = new LinearLayout(act);
        bar.setOrientation(LinearLayout.HORIZONTAL);
        bar.setGravity(Gravity.BOTTOM);
        bar.setPadding(host.dp(10), host.dp(8), host.dp(10), host.dp(10));
        GradientDrawable barBg = new GradientDrawable();
        barBg.setColor(T[M3.SURFACE_CONTAINER_LOW]);
        bar.setBackground(barBg);

        input = new EditText(act);
        input.setHint("发消息…");
        input.setTextSize(15);
        input.setTextColor(T[M3.ON_SURFACE]);
        input.setHintTextColor(T[M3.ON_SURFACE_VARIANT]);
        input.setBackground(null);
        input.setInputType(InputType.TYPE_CLASS_TEXT | InputType.TYPE_TEXT_FLAG_MULTI_LINE
                | InputType.TYPE_TEXT_FLAG_CAP_SENTENCES);
        input.setMaxLines(5);
        input.setPadding(host.dp(10), host.dp(8), host.dp(10), host.dp(8));
        input.setLayoutParams(new LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f));
        bar.addView(input);

        sendBtn = chip("发送", T[M3.PRIMARY], T[M3.ON_PRIMARY]);
        sendBtn.setOnClickListener(v -> send());
        LinearLayout.LayoutParams slp = new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        slp.leftMargin = host.dp(8);
        sendBtn.setLayoutParams(slp);
        bar.addView(sendBtn);
        chatScreen.addView(bar);
    }

    private void openThread(String id, String title, String model) {
        currentThreadId = id;
        currentTitle = title;
        currentModel = model;
        titleText.setText(TextUtils.isEmpty(title) ? "对话" : title);
        msgs.clear();
        threadContainer.removeAllViews();
        showChat();
        new Thread(() -> {
            List<AlmaApi.Msg> loaded = null;
            String err = null;
            try {
                loaded = AlmaApi.messages(id);
            } catch (Exception e) {
                err = AlmaApi.describe(e);
            }
            final List<AlmaApi.Msg> fl = loaded;
            final String fe = err;
            act.runOnUiThread(() -> {
                msgs.clear();
                if (fl != null) msgs.addAll(fl);
                renderMessages();
                if (fe != null) toast("载入消息失败：" + fe);
            });
        }, "msgs").start();
    }

    private void renderMessages() {
        int[] T = host.tokens();
        threadContainer.removeAllViews();
        if (msgs.isEmpty()) {
            TextView tv = new TextView(act);
            tv.setText("开始聊天吧");
            tv.setTextSize(14);
            tv.setTextColor(T[M3.ON_SURFACE_VARIANT]);
            tv.setGravity(Gravity.CENTER);
            tv.setPadding(0, host.dp(60), 0, 0);
            threadContainer.addView(tv);
            return;
        }
        for (AlmaApi.Msg m : msgs) threadContainer.addView(bubble(m, T));
        scrollBottom();
    }

    private View bubble(AlmaApi.Msg m, int[] T) {
        boolean user = "user".equals(m.role);
        LinearLayout wrap = new LinearLayout(act);
        wrap.setGravity(user ? Gravity.END : Gravity.START);
        wrap.setLayoutParams(new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT));

        TextView tv = new TextView(act);
        tv.setText(m.text);
        tv.setTextSize(15);
        tv.setTextIsSelectable(true);
        tv.setLineSpacing(host.dp(3), 1f);

        GradientDrawable bg = new GradientDrawable();
        bg.setCornerRadius(host.dp(18));
        if (user) {
            bg.setColor(T[M3.PRIMARY]);
            tv.setTextColor(T[M3.ON_PRIMARY]);
        } else {
            bg.setColor(T[M3.SURFACE_CONTAINER]);
            tv.setTextColor(T[M3.ON_SURFACE]);
        }
        tv.setBackground(bg);
        tv.setPadding(host.dp(14), host.dp(10), host.dp(14), host.dp(10));

        LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        lp.bottomMargin = host.dp(8);
        int maxW = (int) (act.getResources().getDisplayMetrics().widthPixels * 0.82f);
        tv.setMaxWidth(maxW);
        tv.setLayoutParams(lp);
        wrap.addView(tv);
        return wrap;
    }

    private void scrollBottom() {
        threadScroll.post(() -> threadScroll.fullScroll(View.FOCUS_DOWN));
    }

    // ---- 发送 ----

    private void send() {
        if (generating) {
            toast("还在生成中，稍候");
            return;
        }
        String text = input.getText() == null ? "" : input.getText().toString().trim();
        if (text.isEmpty()) return;
        if (currentThreadId == null) {
            toast("请先新建对话");
            return;
        }
        input.setText("");

        final String model = resolveModel();
        if (model == null) {
            toast("没有可用模型：请到设置里配置提供商");
            return;
        }

        AlmaApi.Msg um = new AlmaApi.Msg("user", text);
        msgs.add(um);
        AlmaApi.Msg am = new AlmaApi.Msg("assistant", "");
        am.streaming = true;
        msgs.add(am);
        renderMessages();

        generating = true;
        sendBtn.setAlpha(0.5f);
        final TextView streamBubble = findStreamingBubble();

        // 发给服务端时不含空的 assistant 占位
        List<AlmaApi.Msg> sendHistory = new ArrayList<>();
        for (AlmaApi.Msg m : msgs) {
            if (!m.streaming) sendHistory.add(m);
        }
        // 最后一条就是刚加的 user，sendMessage 内部会再 append，去掉避免重复
        if (!sendHistory.isEmpty()) sendHistory.remove(sendHistory.size() - 1);

        AlmaApi.sendMessage(currentThreadId, model, text, sendHistory, new AlmaApi.StreamCb() {
            @Override
            public void onDelta(String chunk) {
                am.text += chunk;
                act.runOnUiThread(() -> {
                    if (streamBubble != null) streamBubble.setText(am.text);
                    scrollBottom();
                });
            }

            @Override
            public void onDone(boolean ok, String err) {
                am.streaming = false;
                generating = false;
                act.runOnUiThread(() -> {
                    sendBtn.setAlpha(1f);
                    if (!ok) {
                        am.text = am.text.isEmpty() ? ("（生成失败：" + err + "）") : am.text;
                        toast("生成失败：" + err);
                    }
                    renderMessages();
                    refreshList();
                });
            }
        });
    }

    private TextView findStreamingBubble() {
        // 最后添加的那个 bubble 里的 TextView
        View v = threadContainer.getChildAt(threadContainer.getChildCount() - 1);
        if (v instanceof LinearLayout) {
            View c = ((LinearLayout) v).getChildAt(0);
            if (c instanceof TextView) return (TextView) c;
        }
        return null;
    }

    private String resolveModel() {
        if (currentModel != null && !currentModel.isEmpty() && currentModel.contains(":")) {
            return currentModel;
        }
        String m = AlmaApi.defaultModel();
        if (m != null && m.contains(":")) return m;
        return AlmaApi.anyModel();
    }

    // ---- 新建 ----

    private void newThread() {
        new Thread(() -> {
            try {
                String model = resolveModel();
                AlmaApi.ChatThread t = AlmaApi.createThread(model == null ? "none" : model);
                act.runOnUiThread(() -> {
                    refreshList();
                    openThread(t.id, t.title, t.model);
                });
            } catch (Exception e) {
                act.runOnUiThread(() -> toast("新建失败：" + AlmaApi.describe(e)));
            }
        }, "new-thread").start();
    }

    // ---- 导航 ----

    void showList() {
        chatScreen.setVisibility(View.GONE);
        listScreen.setVisibility(View.VISIBLE);
        refreshList();
    }

    void showChat() {
        listScreen.setVisibility(View.GONE);
        chatScreen.setVisibility(View.VISIBLE);
    }

    boolean onBack() {
        if (chatScreen.getVisibility() == View.VISIBLE) {
            showList();
            return true;
        }
        return false;
    }

    // ---- 工具 ----

    private TextView chip(String text, int bgColor, int fgColor) {
        TextView v = new TextView(act);
        v.setText(text);
        v.setTextSize(13);
        v.setTextColor(fgColor);
        GradientDrawable g = new GradientDrawable();
        g.setColor(bgColor);
        g.setCornerRadius(host.dp(999));
        v.setBackground(g);
        v.setPadding(host.dp(14), host.dp(8), host.dp(14), host.dp(8));
        return v;
    }

    private void toast(String s) {
        Toast.makeText(act, s, Toast.LENGTH_SHORT).show();
    }
}