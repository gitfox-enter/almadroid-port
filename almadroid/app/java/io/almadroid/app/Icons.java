package io.almadroid.app;

import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.Path;
import android.graphics.RectF;

/** 程序化绘制的 Material 风格小图标（24dp 逻辑尺寸，按密度放大）。 */
final class Icons {

    static Bitmap make(float density, Kind kind, int color, int dpSize) {
        int px = Math.round(dpSize * density);
        Bitmap b = Bitmap.createBitmap(px, px, Bitmap.Config.ARGB_8888);
        Canvas cv = new Canvas(b);
        Paint p = new Paint(Paint.ANTI_ALIAS_FLAG);
        p.setColor(color);
        p.setStyle(Paint.Style.STROKE);
        p.setStrokeWidth(px * 0.085f);
        p.setStrokeCap(Paint.Cap.ROUND);
        p.setStrokeJoin(Paint.Join.ROUND);
        float s = px / 24f; // logical unit
        Path path = new Path();
        switch (kind) {
            case RELOAD: {
                RectF arc = new RectF(4.5f * s, 4.5f * s, 19.5f * s, 19.5f * s);
                cv.drawArc(arc, -60, 300, false, p);
                arrowHead(cv, p, path, 19.9f * s, 3.6f * s, 3.2f * s, 7.2f * s, 6.6f * s, 3.4f * s);
                break;
            }
            case RESTART: {
                RectF arc = new RectF(4.5f * s, 4.5f * s, 19.5f * s, 19.5f * s);
                cv.drawArc(arc, 110, 250, false, p);
                arrowHead(cv, p, path, 4.1f * s, 3.9f * s, 4.0f * s, 7.6f * s, 7.6f * s, 4.2f * s);
                break;
            }
            case OPEN: { // 外链：方框 + 箭头
                path.moveTo(12f * s, 4.5f * s);
                path.lineTo(5f * s, 4.5f * s);
                path.quadTo(4f * s, 4.5f * s, 4f * s, 5.5f * s);
                path.lineTo(4f * s, 18.5f * s);
                path.quadTo(4f * s, 19.5f * s, 5f * s, 19.5f * s);
                path.lineTo(18f * s, 19.5f * s);
                path.quadTo(19f * s, 19.5f * s, 19f * s, 18.5f * s);
                path.lineTo(19f * s, 11.5f * s);
                cv.drawPath(path, p);
                path.reset();
                path.moveTo(13.5f * s, 4.5f * s);
                path.lineTo(19.5f * s, 4.5f * s);
                path.lineTo(19.5f * s, 10.5f * s);
                cv.drawPath(path, p);
                path.reset();
                path.moveTo(19.5f * s, 4.5f * s);
                path.lineTo(11f * s, 13f * s);
                cv.drawPath(path, p);
                break;
            }
            case COPY: {
                path.moveTo(8.5f * s, 8.5f * s);
                path.lineTo(8.5f * s, 5.5f * s);
                path.quadTo(8.5f * s, 4.5f * s, 9.5f * s, 4.5f * s);
                path.lineTo(18.5f * s, 4.5f * s);
                path.quadTo(19.5f * s, 4.5f * s, 19.5f * s, 5.5f * s);
                path.lineTo(19.5f * s, 14.5f * s);
                path.quadTo(19.5f * s, 15.5f * s, 18.5f * s, 15.5f * s);
                path.lineTo(15.5f * s, 15.5f * s);
                cv.drawPath(path, p);
                RectF r2 = new RectF(4.5f * s, 8.5f * s, 15.5f * s, 19.5f * s);
                cv.drawRoundRect(r2, 1.2f * s, 1.2f * s, p);
                break;
            }
            case PAIR: { // 链环
                RectF r1 = new RectF(3.5f * s, 8.5f * s, 14.5f * s, 15.5f * s);
                RectF r2 = new RectF(9.5f * s, 8.5f * s, 20.5f * s, 15.5f * s);
                cv.drawRoundRect(r1, 3.5f * s, 3.5f * s, p);
                cv.drawRoundRect(r2, 3.5f * s, 3.5f * s, p);
                break;
            }
            case STOP: { // 电源
                RectF arc = new RectF(5f * s, 5f * s, 19f * s, 19f * s);
                cv.drawArc(arc, 300, 240, false, p);
                path.reset();
                path.moveTo(12f * s, 3.5f * s);
                path.lineTo(12f * s, 11f * s);
                cv.drawPath(path, p);
                break;
            }
            case LOGS: { // 终端 >_
                RectF r = new RectF(3.5f * s, 4.5f * s, 20.5f * s, 19.5f * s);
                cv.drawRoundRect(r, 2f * s, 2f * s, p);
                path.reset();
                path.moveTo(7f * s, 9f * s);
                path.lineTo(10.5f * s, 12f * s);
                path.lineTo(7f * s, 15f * s);
                cv.drawPath(path, p);
                path.reset();
                path.moveTo(12.5f * s, 15f * s);
                path.lineTo(16.5f * s, 15f * s);
                cv.drawPath(path, p);
                break;
            }
            case BATTERY: { // 闪电
                p.setStyle(Paint.Style.FILL);
                path.moveTo(13.5f * s, 3f * s);
                path.lineTo(6.5f * s, 13.5f * s);
                path.lineTo(11f * s, 13.5f * s);
                path.lineTo(10f * s, 21f * s);
                path.lineTo(17.5f * s, 10f * s);
                path.lineTo(12.8f * s, 10f * s);
                path.close();
                cv.drawPath(path, p);
                break;
            }
            case SWITCH: { // 双向箭头（主机切换）
                path.moveTo(5.5f * s, 9f * s);
                path.lineTo(18.5f * s, 9f * s);
                cv.drawPath(path, p);
                path.reset();
                path.moveTo(18.5f * s, 15f * s);
                path.lineTo(5.5f * s, 15f * s);
                cv.drawPath(path, p);
                path.reset();
                path.moveTo(15.5f * s, 5.5f * s);
                path.lineTo(18.7f * s, 9f * s);
                path.lineTo(15.5f * s, 12.2f * s);
                cv.drawPath(path, p);
                path.reset();
                path.moveTo(8.5f * s, 11.8f * s);
                path.lineTo(5.3f * s, 15f * s);
                path.lineTo(8.5f * s, 18.5f * s);
                cv.drawPath(path, p);
                break;
            }
            case GLOBE: { // 网页
                cv.drawCircle(12f * s, 12f * s, 8f * s, p);
                path.reset();
                path.moveTo(4f * s, 12f * s);
                path.lineTo(20f * s, 12f * s);
                cv.drawPath(path, p);
                path.reset();
                path.moveTo(12f * s, 4f * s);
                path.quadTo(15.5f * s, 12f * s, 12f * s, 20f * s);
                path.moveTo(12f * s, 4f * s);
                path.quadTo(8.5f * s, 12f * s, 12f * s, 20f * s);
                cv.drawPath(path, p);
                break;
            }
            case INFO: {
                cv.drawCircle(12f * s, 12f * s, 8.5f * s, p);
                p.setStyle(Paint.Style.FILL);
                cv.drawCircle(12f * s, 8.2f * s, 1.15f * s, p);
                path.reset();
                path.moveTo(12f * s, 11f * s);
                path.lineTo(12f * s, 16.5f * s);
                p.setStyle(Paint.Style.STROKE);
                p.setStrokeWidth(px * 0.13f);
                cv.drawPath(path, p);
                break;
            }
        }
        return b;
    }

    private static void arrowHead(Canvas cv, Paint p, Path path,
                                  float tipX, float tipY, float ax, float ay, float bx, float by) {
        path.reset();
        path.moveTo(tipX, tipY);
        path.lineTo(ax, ay);
        path.lineTo(bx, by);
        cv.drawPath(path, p);
    }

    /** ⋮ 竖三点（溢出菜单）。 */
    static Bitmap dots(float density, int color, int dpSize) {
        int px = Math.round(dpSize * density);
        Bitmap b = Bitmap.createBitmap(px, px, Bitmap.Config.ARGB_8888);
        Canvas cv = new Canvas(b);
        Paint p = new Paint(Paint.ANTI_ALIAS_FLAG);
        p.setColor(color);
        p.setStyle(Paint.Style.FILL);
        float r = px * 0.055f;
        float[] ys = {0.30f, 0.50f, 0.70f};
        for (float yf : ys) {
            cv.drawCircle(px / 2f, px * yf, r, p);
        }
        return b;
    }

    enum Kind { RELOAD, RESTART, OPEN, COPY, PAIR, STOP, LOGS, BATTERY, GLOBE, INFO, SWITCH }
}
