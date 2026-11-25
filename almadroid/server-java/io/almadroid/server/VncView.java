package io.almadroid.server;

import android.app.Activity;
import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Rect;
import android.net.LocalSocket;
import android.net.LocalSocketAddress;
import android.view.MotionEvent;
import android.view.KeyEvent;
import android.view.View;

import java.io.EOFException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.Socket;
import java.util.concurrent.atomic.AtomicBoolean;

/**
 * 极简原生 VNC 客户端 View（tiny_container 同款体验，零第三方依赖）。
 * RFB 3.8 + None 认证 + Raw/CopyRect/Hextile 解码 + 增量更新。
 * 连接顺序：unix socket（proot /tmp bind 共享）→ TCP 127.0.0.1:5900。
 * 桌面 1280x720 横屏缩放铺满，触摸坐标逆映射。
 */
public class VncView extends View {
    public interface Listener {
        void onConnected(int w, int h);
        void onError(String msg);
    }

    private final Listener listener;
    private final String unixPath;   // app 侧可见的 unix socket 路径（可空）
    private final int tcpPort;       // 兜底 TCP 端口

    private volatile Socket tcpSock;
    private volatile LocalSocket unixSock;
    private volatile InputStream in;
    private volatile OutputStream out;
    private final Object outLock = new Object();
    private final AtomicBoolean alive = new AtomicBoolean(false);
    private Thread reader;

    private Bitmap frame;            // 桌面帧
    private int fw, fh;              // 桌面尺寸
    private float scale = 1f;        // 显示缩放
    private final Paint paint = new Paint();
    private String errorMsg;

    public VncView(Context c, String unixPath, int tcpPort, Listener l) {
        super(c);
        this.unixPath = unixPath;
        this.tcpPort = tcpPort;
        this.listener = l;
        paint.setFilterBitmap(true);
        setFocusable(true);
        setFocusableInTouchMode(true);
    }

    public void connect() {
        alive.set(true);
        reader = new Thread(new Runnable() { public void run() { runLoop(); } }, "vnc-reader");
        reader.start();
    }

    public void shutdown() {
        alive.set(false);
        try { if (tcpSock != null) tcpSock.close(); } catch (Throwable ignored) {}
        try { if (unixSock != null) unixSock.close(); } catch (Throwable ignored) {}
    }

    /** 连接线程：unix → tcp 兜底，握手后循环读更新。 */
    private void runLoop() {
        try {
            if (unixPath != null) {
                try {
                    LocalSocket s = new LocalSocket();
                    s.connect(new LocalSocketAddress(unixPath, LocalSocketAddress.Namespace.FILESYSTEM), 4000);
                    unixSock = s;
                    handshake(s.getInputStream(), s.getOutputStream());
                    return;
                } catch (Throwable t) {
                    postError("unix: " + t.getMessage());
                }
            }
            Socket s = new Socket("127.0.0.1", tcpPort);
            s.setTcpNoDelay(true);
            tcpSock = s;
            handshake(s.getInputStream(), s.getOutputStream());
        } catch (Throwable t) {
            if (alive.get()) postError(t.getMessage() == null ? t.toString() : t.getMessage());
        }
    }

    private void handshake(InputStream in, OutputStream out) throws Exception {
        this.in = in; this.out = out;
        byte[] banner = readN(12);
        if (!(new String(banner, "ISO-8859-1")).startsWith("RFB "))
            throw new IllegalStateException("not RFB: " + new String(banner));
        // 服务器最高 003.008，回同版本
        sSend("RFB 003.008\n".getBytes("ISO-8859-1"));
        int nsec = readN(1)[0] & 0xff;
        byte[] types = readN(nsec);
        boolean hasNone = false;
        for (byte t : types) if (t == 1) hasNone = true;
        if (!hasNone) throw new IllegalStateException("no None security");
        sSend(new byte[]{0, 0, 0, 1});  // 选 None
        byte[] res = readN(4);          // 3.8 安全结果
        if (res[0] != 0 || res[1] != 0 || res[2] != 0 || res[3] != 0)
            throw new IllegalStateException("security failed");
        sSend(new byte[]{1});           // ClientInit: shared
        byte[] si = readN(24);
        fw = ((si[0] & 0xff) << 8) | (si[1] & 0xff);
        fh = ((si[2] & 0xff) << 8) | (si[3] & 0xff);
        int nameLen = ((si[20] & 0xff) << 24) | ((si[21] & 0xff) << 16)
                | ((si[22] & 0xff) << 8) | (si[23] & 0xff);
        readN(nameLen);                 // 桌面名（丢弃）

        frame = Bitmap.createBitmap(fw, fh, Bitmap.Config.ARGB_8888);
        post(new Runnable() { public void run() {
            if (listener != null) listener.onConnected(fw, fh);
        } });

        // SetPixelFormat: 32bpp BGRX little-endian
        // RFB 消息格式：type(1) + padding(3) + pixel-format(16) = 20 字节
        // pixel-format(16B): bpp(1) depth(1) be(1) trueColor(1) redMax(2) greenMax(2) blueMax(2) redShift(1) greenShift(1) blueShift(1) pad(3)
        byte[] pf = new byte[16];
        pf[0] = 32; pf[1] = 24; pf[2] = 0; pf[3] = 1;   // bpp depth be trueColor
        pf[4] = 0; pf[5] = (byte) 255;                 // red-max
        pf[6] = 0; pf[7] = (byte) 255;                 // green-max
        pf[8] = 0; pf[9] = (byte) 255;                 // blue-max
        pf[10] = 16; pf[11] = 8; pf[12] = 0;          // red green blue shift
        sSend(concat(new byte[]{0}, new byte[3], pf));
        // SetEncodings: Raw(0) CopyRect(1) Hextile(5)
        sSend(concat(new byte[]{2, 0, 0, 3},
                new byte[]{0, 0, 0, 0, 0, 0, 0, 1, 0, 0, 0, 5}));
        // 首次全量请求
        fbRequest(false);

        // 读消息循环
        while (alive.get()) {
            int type = readU8();
            if (type == 0) {
                readN(1);
                int nrects = (readN(1)[0] & 0xff) << 8 | (readN(1)[0] & 0xff);
                for (int i = 0; i < nrects; i++) readRect();
                postInvalidate();
            } else if (type == 2) {          // Bell
            } else if (type == 3) {          // ServerCutText
                readN(4); int len = (int) readU32(); skipN(len);
            } else {
                throw new EOFException("msg type " + type);
            }
            // 增量请求：服务器只回变化区域，静止零流量
            fbRequest(true);
        }
    }

    private int curX, curY, curW, curH;

    private void readRect() throws Exception {
        int x = (readN(1)[0] & 0xff) << 8 | (readN(1)[0] & 0xff);
        int y = (readN(1)[0] & 0xff) << 8 | (readN(1)[0] & 0xff);
        int w = (readN(1)[0] & 0xff) << 8 | (readN(1)[0] & 0xff);
        int h = (readN(1)[0] & 0xff) << 8 | (readN(1)[0] & 0xff);
        long enc = readU32();
        if (enc == 0) {                                  // Raw
            byte[] d = readN(w * h * 4);
            blit(x, y, w, h, d, 0);
        } else if (enc == 1) {                           // CopyRect
            int sx = (readN(1)[0] & 0xff) << 8 | (readN(1)[0] & 0xff);
            int sy = (readN(1)[0] & 0xff) << 8 | (readN(1)[0] & 0xff);
            Bitmap region = Bitmap.createBitmap(frame, sx, sy, w, h);
            Canvas c = new Canvas(frame);
            c.drawBitmap(region, x - sx, y - sy, null);
            region.recycle();
        } else if (enc == 5) {                           // Hextile
            hextile(x, y, w, h);
        } else {
            throw new EOFException("encoding " + enc);
        }
    }

    /** Hextile 解码（背景/前景/任意子矩形/原始，不含轻量 SubrectsTransparency）。 */
    private void hextile(int rx, int ry, int rw, int rh) throws Exception {
        byte[] bg = new byte[4], fg = new byte[4];
        int[] tile = new int[16 * 16];
        for (int ty = ry; ty < ry + rh; ty += 16) {
            int th = Math.min(16, ry + rh - ty);
            for (int tx = rx; tx < rx + rw; tx += 16) {
                int tw = Math.min(16, rx + rw - tx);
                int sub = readU8();
                if ((sub & 1) != 0) { byte[] b = readN(4); System.arraycopy(b, 0, bg, 0, 4); }  // background
                fillTile(tile, tw, th, bg);
                if ((sub & 2) != 0) { byte[] f = readN(4); System.arraycopy(f, 0, fg, 0, 4); }  // foreground
                if ((sub & 4) != 0) {                                  // anySubrects
                    int n = readU8();
                    for (int i = 0; i < n; i++) {
                        byte[] col = ((sub & 16) != 0) ? readN(4) : fg;
                        int b0 = readU8();
                        int sx = (b0 >> 4) & 0xF, sy = b0 & 0xF;
                        int b1 = readU8();
                        int sw = ((b1 >> 4) & 0xF) + 1, sh = (b1 & 0xF) + 1;
                        for (int yy = 0; yy < sh; yy++)
                            for (int xx = 0; xx < sw; xx++)
                                tile[(sy + yy) * 16 + sx + xx] = pixel(col);
                    }
                }
                if ((sub & 8) != 0) {                                  // raw tile
                    byte[] d = readN(tw * th * 4);
                    int[] px = new int[tw * th];
                    for (int i = 0; i < px.length; i++) {
                        int o = i * 4;
                        px[i] = 0xff000000 | ((d[o + 2] & 0xff) << 16)
                                | ((d[o + 1] & 0xff) << 8) | (d[o] & 0xff);
                    }
                    frame.setPixels(px, 0, tw, tx, ty, tw, th);
                } else {
                    frame.setPixels(tile, 0, 16, tx, ty, tw, th);
                }
            }
        }
    }

    private void fillTile(int[] tile, int tw, int th, byte[] col) {
        int p = pixel(col);
        for (int y = 0; y < th; y++)
            for (int x = 0; x < tw; x++) tile[y * 16 + x] = p;
    }

    private int pixel(byte[] bgrx) {
        return 0xff000000 | ((bgrx[2] & 0xff) << 16) | ((bgrx[1] & 0xff) << 8) | (bgrx[0] & 0xff);
    }

    private void blit(int x, int y, int w, int h, byte[] d, int off) {
        int[] px = new int[w * h];
        for (int i = 0; i < px.length; i++) {
            int o = off + i * 4;
            px[i] = 0xff000000 | ((d[o + 2] & 0xff) << 16) | ((d[o + 1] & 0xff) << 8) | (d[o] & 0xff);
        }
        frame.setPixels(px, 0, w, x, y, w, h);
    }

    private void fbRequest(boolean incremental) throws Exception {
        sSend(new byte[]{3, (byte) (incremental ? 1 : 0),
                0, 0, 0, 0, (byte) (fw >> 8), (byte) fw, (byte) (fh >> 8), (byte) fh});
    }

    // ---------- 输入 ----------
    public void sendPointer(int x, int y, int mask) {
        try {
            synchronized (outLock) {
                out.write(new byte[]{5, (byte) mask, (byte) (x >> 8), (byte) x, (byte) (y >> 8), (byte) y});
                out.flush();
            }
        } catch (Throwable ignored) {}
    }

    public void sendKey(int keysym, boolean down) {
        try {
            synchronized (outLock) {
                out.write(new byte[]{4, (byte) (down ? 1 : 0), 0, 0,
                        (byte) (keysym >> 24), (byte) (keysym >> 16), (byte) (keysym >> 8), (byte) keysym});
                out.flush();
            }
        } catch (Throwable ignored) {}
    }

    // ---------- 绘制与触摸 ----------
    @Override protected void onSizeChanged(int w, int h, int ow, int oh) {
        super.onSizeChanged(w, h, ow, oh);
        if (fw > 0 && w > 0) scale = Math.min((float) w / fw, (float) h / fh);
    }

    @Override protected void onDraw(Canvas canvas) {
        canvas.drawColor(Color.BLACK);
        if (frame == null) {
            paint.setColor(Color.WHITE);
            paint.setTextSize(40);
            canvas.drawText(errorMsg == null ? "连接桌面..." : errorMsg, 60, getHeight() / 2f, paint);
            return;
        }
        int dw = (int) (fw * scale), dh = (int) (fh * scale);
        int left = (getWidth() - dw) / 2, top = (getHeight() - dh) / 2;
        canvas.drawBitmap(frame, null, new Rect(left, top, left + dw, top + dh), paint);
    }

    private int lastMask = 0;

    @Override public boolean onTouchEvent(MotionEvent e) {
        if (frame == null) return false;
        int dw = (int) (fw * scale), dh = (int) (fh * scale);
        int left = (getWidth() - dw) / 2, top = (getHeight() - dh) / 2;
        int x = (int) ((e.getX() - left) / scale);
        int y = (int) ((e.getY() - top) / scale);
        if (x < 0) x = 0; if (y < 0) y = 0;
        if (x >= fw) x = fw - 1; if (y >= fh) y = fh - 1;
        int mask = (e.getAction() == MotionEvent.ACTION_DOWN
                || e.getAction() == MotionEvent.ACTION_MOVE) ? 1 : 0;
        if (e.getAction() == MotionEvent.ACTION_UP) mask = 0;
        if (mask != lastMask || e.getAction() == MotionEvent.ACTION_MOVE) {
            sendPointer(x, y, mask);
            lastMask = mask;
        }
        return true;
    }

    // 物理键盘 → keysym（拉丁字符基础映射；中英文输入后续加软键盘）
    @Override public boolean onKeyDown(int keyCode, KeyEvent e) {
        int ks = latinKeysym(e);
        if (ks > 0) { sendKey(ks, true); return true; }
        return super.onKeyDown(keyCode, e);
    }

    @Override public boolean onKeyUp(int keyCode, KeyEvent e) {
        int ks = latinKeysym(e);
        if (ks > 0) { sendKey(ks, false); return true; }
        return super.onKeyUp(keyCode, e);
    }

    private static int latinKeysym(KeyEvent e) {
        int u = e.getUnicodeChar();
        if (u >= 32 && u < 127) return u;                   // ASCII 直接映射
        switch (keyCode(e)) {
            case KeyEvent.KEYCODE_ENTER: return 0xff0d;
            case KeyEvent.KEYCODE_DEL: return 0xff08;
            case KeyEvent.KEYCODE_ESCAPE: return 0xff1b;
            case KeyEvent.KEYCODE_TAB: return 0xff09;
            case KeyEvent.KEYCODE_DPAD_UP: return 0xff52;
            case KeyEvent.KEYCODE_DPAD_DOWN: return 0xff54;
            case KeyEvent.KEYCODE_DPAD_LEFT: return 0xff51;
            case KeyEvent.KEYCODE_DPAD_RIGHT: return 0xff53;
        }
        return -1;
    }

    private static int keyCode(KeyEvent e) { return e.getKeyCode(); }

    // ---------- 工具 ----------
    private void postError(final String m) {
        errorMsg = m;
        postInvalidate();
        post(new Runnable() { public void run() {
            if (listener != null) listener.onError(m);
        } });
    }

    private static void skipN(InputStream in, int n) throws Exception {
        long left = n;
        while (left > 0) left -= in.skip(left);
    }

    private void skipN(int n) throws Exception { skipN(in, n); }

    private byte[] readN(int n) throws Exception {
        byte[] buf = new byte[n];
        int off = 0;
        while (off < n) {
            int r = in.read(buf, off, n - off);
            if (r < 0) throw new EOFException("eof at " + off + "/" + n);
            off += r;
        }
        return buf;
    }

    private int readU8() throws Exception { return readN(1)[0] & 0xff; }

    private long readU32() throws Exception {
        byte[] b = readN(4);
        return ((b[0] & 0xffL) << 24) | ((b[1] & 0xffL) << 16) | ((b[2] & 0xffL) << 8) | (b[3] & 0xffL);
    }

    private void sSend(byte[] d) throws Exception {
        synchronized (outLock) { out.write(d); out.flush(); }
    }

    private static byte[] concat(byte[]... arrs) {
        int n = 0; for (byte[] a : arrs) n += a.length;
        byte[] r = new byte[n]; int o = 0;
        for (byte[] a : arrs) { System.arraycopy(a, 0, r, o, a.length); o += a.length; }
        return r;
    }
}
