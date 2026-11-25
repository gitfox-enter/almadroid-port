package io.almadroid.app;

import android.util.Log;

import org.json.JSONArray;
import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;

/**
 * alma-server 本机 API 客户端。
 * 协议见 memory/almadroid-chat-api.md：
 *   发消息 = POST /api/chat/completions {threadId, model, messages, stream} → SSE
 */
final class AlmaApi {
    private static final String TAG = "AlmaApi";
    private static final int TIMEOUT = 15000;

    // ---- 数据模型 ----

    static final class ChatThread {
        String id = "";
        String title = "新对话";
        String model = "";
        boolean generating;
        long lastAt;
    }

    static final class Msg {
        String id = "";
        String role = "user";   // user | assistant | system
        String text = "";
        long createdAt;
        boolean streaming;

        Msg() {}
        Msg(String role, String text) { this.role = role; this.text = text; }
    }

    static final class Model {
        String providerId = "";
        String id = "";
        String name = "";
        String fullId() { return providerId + ":" + id; }
    }

    interface StreamCb {
        void onDelta(String chunk);      // 文本增量
        void onDone(boolean ok, String err);
    }

    private AlmaApi() {}

    // ---- HTTP helpers ----

    private static String http(String path, String method, String body, int timeout) throws Exception {
        HttpURLConnection c = (HttpURLConnection) new URL(Env.SERVER_URL + path).openConnection();
        c.setConnectTimeout(timeout);
        c.setReadTimeout(timeout);
        c.setRequestMethod(method);
        c.setRequestProperty("Content-Type", "application/json");
        if (body != null) {
            c.setDoOutput(true);
            try (OutputStream os = c.getOutputStream()) {
                os.write(body.getBytes(StandardCharsets.UTF_8));
            }
        }
        int code = c.getResponseCode();
        InputStream in = code >= 400 ? c.getErrorStream() : c.getInputStream();
        StringBuilder sb = new StringBuilder();
        if (in != null) {
            try (BufferedReader br = new BufferedReader(new InputStreamReader(in, StandardCharsets.UTF_8))) {
                char[] buf = new char[4096];
                int n;
                while ((n = br.read(buf)) > 0) sb.append(buf, 0, n);
            }
        }
        String out = sb.toString();
        if (code >= 400) throw new Exception("HTTP " + code + ": " + trim(out, 200));
        return out;
    }

    private static String trim(String s, int n) {
        if (s == null) return "";
        return s.length() <= n ? s : s.substring(0, n) + "…";
    }

    // ---- health ----

    static boolean health() {
        try {
            http("/api/health", "GET", null, 2500);
            return true;
        } catch (Exception e) {
            return false;
        }
    }

    // ---- threads ----

    static List<ChatThread> threads() throws Exception {
        String json = http("/api/threads?limit=50", "GET", null, TIMEOUT);
        List<ChatThread> out = new ArrayList<>();
        JSONArray arr = new JSONArray(json);
        for (int i = 0; i < arr.length(); i++) {
            JSONObject o = arr.getJSONObject(i);
            ChatThread t = new ChatThread();
            t.id = o.optString("id");
            t.title = o.optString("title", "新对话");
            t.model = o.optString("model", "");
            t.generating = o.optBoolean("isGenerating", false);
            String ts = o.optString("updatedAt", o.optString("createdAt", ""));
            t.lastAt = parseTime(ts);
            out.add(t);
        }
        out.sort((a, b) -> Long.compare(b.lastAt, a.lastAt));
        return out;
    }

    private static long parseTime(String s) {
        if (s == null || s.isEmpty()) return 0;
        try {
            // 2026-11-17T07:16:52.291Z
            return java.time.Instant.parse(s).toEpochMilli();
        } catch (Throwable t) {
            return 0;
        }
    }

    static ChatThread createThread(String model) throws Exception {
        JSONObject body = new JSONObject();
        body.put("title", "新对话");
        body.put("model", model);
        String json = http("/api/threads", "POST", body.toString(), TIMEOUT);
        JSONObject o = new JSONObject(json);
        ChatThread t = new ChatThread();
        t.id = o.optString("id");
        t.title = o.optString("title", "新对话");
        t.model = o.optString("model", model);
        return t;
    }

    static void renameThread(String id, String title) {
        try {
            JSONObject b = new JSONObject();
            b.put("title", title);
            http("/api/threads/" + id, "PUT", b.toString(), TIMEOUT);
        } catch (Exception e) {
            Log.w(TAG, "rename failed", e);
        }
    }

    static void deleteThread(String id) {
        try {
            http("/api/threads/" + id, "DELETE", null, TIMEOUT);
        } catch (Exception e) {
            Log.w(TAG, "delete failed", e);
        }
    }

    // ---- messages ----

    static List<Msg> messages(String threadId) throws Exception {
        String json = http("/api/threads/" + threadId + "/messages", "GET", null, TIMEOUT);
        List<Msg> out = new ArrayList<>();
        JSONArray arr = new JSONArray(json);
        for (int i = 0; i < arr.length(); i++) {
            JSONObject o = arr.getJSONObject(i);
            Msg m = new Msg();
            m.id = o.optString("id");
            m.role = o.optString("role", "assistant");
            m.createdAt = parseTime(o.optString("createdAt", ""));
            // content 可能是字符串，也可能在 parts 里
            String content = o.optString("content", "");
            if (content.isEmpty()) {
                JSONArray parts = o.optJSONArray("parts");
                if (parts != null) {
                    StringBuilder sb = new StringBuilder();
                    for (int k = 0; k < parts.length(); k++) {
                        JSONObject p = parts.optJSONObject(k);
                        if (p == null) continue;
                        if ("text".equals(p.optString("type"))) sb.append(p.optString("text"));
                    }
                    content = sb.toString();
                }
            }
            m.text = content;
            if (!m.text.isEmpty() || "user".equals(m.role)) out.add(m);
        }
        return out;
    }

    // ---- models ----

    static List<Model> models() throws Exception {
        String json = http("/api/models", "GET", null, TIMEOUT);
        List<Model> out = new ArrayList<>();
        JSONObject root = new JSONObject(json);
        JSONArray arr = root.optJSONArray("data");
        if (arr == null) return out;
        for (int i = 0; i < arr.length(); i++) {
            JSONObject o = arr.getJSONObject(i);
            Model m = new Model();
            m.providerId = o.optString("providerId");
            m.id = o.optString("id");
            m.name = o.optString("name", m.id);
            if (!m.providerId.isEmpty() && !m.id.isEmpty()) out.add(m);
        }
        return out;
    }

    /** 默认模型 id。GET /api/settings 返回扁平 JSON（chat.defaultModel 直接在顶层），
     *  也兼容旧版 {settingsData:"{...}"} 包一层的形态。 */
    static String defaultModel() {
        try {
            String json = http("/api/settings", "GET", null, TIMEOUT);
            JSONObject root = new JSONObject(json);
            // 形态 A：扁平
            JSONObject chat = root.optJSONObject("chat");
            if (chat != null) {
                String m = chat.optString("defaultModel", "");
                if (!m.isEmpty()) return m;
            }
            // 形态 B：settingsData 字符串/对象
            Object sd = root.opt("settingsData");
            JSONObject s = null;
            if (sd instanceof String) s = new JSONObject((String) sd);
            else if (sd instanceof JSONObject) s = (JSONObject) sd;
            if (s != null) {
                JSONObject c2 = s.optJSONObject("chat");
                if (c2 != null) {
                    String m = c2.optString("defaultModel", "");
                    if (!m.isEmpty()) return m;
                }
            }
        } catch (Exception e) {
            Log.w(TAG, "defaultModel failed", e);
        }
        return null;
    }

    /** 首个可用模型，兜底用。 */
    static String anyModel() {
        try {
            List<Model> ms = models();
            if (!ms.isEmpty()) return ms.get(0).fullId();
        } catch (Exception ignored) {}
        return null;
    }

    // ---- 发消息（SSE 流式）----

    static void sendMessage(String threadId, String model, String text, List<Msg> history, StreamCb cb) {
        new Thread(() -> {
            HttpURLConnection c = null;
            try {
                JSONObject body = new JSONObject();
                body.put("threadId", threadId);
                body.put("model", model);
                JSONArray msgs = new JSONArray();
                // 历史（服务端也会自己存，这里带上最新 user 消息即可）
                for (Msg m : history) {
                    if (m.streaming || m.text.isEmpty()) continue;
                    JSONObject mo = new JSONObject();
                    mo.put("id", m.id == null || m.id.isEmpty() ? ("m" + System.nanoTime()) : m.id);
                    mo.put("role", m.role);
                    JSONArray parts = new JSONArray();
                    JSONObject p = new JSONObject();
                    p.put("type", "text");
                    p.put("text", m.text);
                    parts.put(p);
                    mo.put("parts", parts);
                    msgs.put(mo);
                }
                JSONObject u = new JSONObject();
                u.put("id", "u" + System.currentTimeMillis());
                u.put("role", "user");
                JSONArray up = new JSONArray();
                JSONObject ut = new JSONObject();
                ut.put("type", "text");
                ut.put("text", text);
                up.put(ut);
                u.put("parts", up);
                msgs.put(u);
                body.put("messages", msgs);
                body.put("stream", true);

                c = (HttpURLConnection) new URL(Env.SERVER_URL + "/api/chat/completions").openConnection();
                c.setConnectTimeout(15000);
                c.setReadTimeout(180000);   // 长回答
                c.setRequestMethod("POST");
                c.setRequestProperty("Content-Type", "application/json");
                c.setRequestProperty("Accept", "text/plain");
                c.setDoOutput(true);
                try (OutputStream os = c.getOutputStream()) {
                    os.write(body.toString().getBytes(StandardCharsets.UTF_8));
                }
                int code = c.getResponseCode();
                if (code >= 400) {
                    InputStream es = c.getErrorStream();
                    String err = es == null ? "" : readAll(es);
                    cb.onDone(false, "HTTP " + code + ": " + trim(err, 300));
                    return;
                }
                // 逐行读 SSE
                try (BufferedReader br = new BufferedReader(
                        new InputStreamReader(c.getInputStream(), StandardCharsets.UTF_8))) {
                    String line;
                    StringBuilder acc = new StringBuilder();
                    while ((line = br.readLine()) != null) {
                        if (!line.startsWith("data:")) continue;
                        String payload = line.substring(5).trim();
                        if (payload.isEmpty()) continue;
                        if ("[DONE]".equals(payload)) break;
                        try {
                            JSONObject o = new JSONObject(payload);
                            JSONArray choices = o.optJSONArray("choices");
                            if (choices == null || choices.length() == 0) continue;
                            JSONObject ch = choices.getJSONObject(0);
                            JSONObject delta = ch.optJSONObject("delta");
                            if (delta != null) {
                                String piece = delta.optString("content", "");
                                if (!piece.isEmpty()) {
                                    acc.append(piece);
                                    cb.onDelta(piece);
                                }
                            }
                        } catch (Exception pe) {
                            Log.w(TAG, "sse parse: " + payload, pe);
                        }
                    }
                    cb.onDone(true, null);
                }
            } catch (Exception e) {
                Log.w(TAG, "send failed", e);
                cb.onDone(false, describe(e));
            } finally {
                if (c != null) c.disconnect();
            }
        }, "alma-send").start();
    }

    private static String readAll(InputStream in) throws Exception {
        StringBuilder sb = new StringBuilder();
        try (BufferedReader br = new BufferedReader(new InputStreamReader(in, StandardCharsets.UTF_8))) {
            char[] buf = new char[2048];
            int n;
            while ((n = br.read(buf)) > 0) sb.append(buf, 0, n);
        }
        return sb.toString();
    }

    static String describe(Throwable t) {
        String m = t.getMessage();
        return t.getClass().getSimpleName() + (m == null ? "" : (": " + m));
    }
}