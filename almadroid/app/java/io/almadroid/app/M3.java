package io.almadroid.app;

import android.content.Context;
import android.content.res.Configuration;
import android.graphics.Color;

/**
 * Material 3 色彩令牌（暗色为主）。
 * Android 12+ 优先莫奈动态取色；取不到时退回内置「陶土」种子色板。
 * 令牌布局参考 Material Color Utilities SchemeTonalSpot。
 */
final class M3 {
    static final int PRIMARY = 0, ON_PRIMARY = 1, PRIMARY_CONTAINER = 2, ON_PRIMARY_CONTAINER = 3;
    static final int SURFACE = 4, ON_SURFACE = 5, SURFACE_VARIANT = 6, ON_SURFACE_VARIANT = 7;
    static final int OUTLINE = 8, OUTLINE_VARIANT = 9;
    static final int SURFACE_CONTAINER_LOW = 10, SURFACE_CONTAINER = 11,
            SURFACE_CONTAINER_HIGH = 12, SURFACE_CONTAINER_HIGHEST = 13, SURFACE_CONTAINER_LOWEST = 14;
    static final int ERROR = 15;
    static final int COUNT = 16;

    /** 内置「陶土」暗色板（与 magpie-android 同源）。 */
    private static final int[] DARK_TERRACOTTA = {
            0xFFF8B8A0, // primary
            0xFF603524, // onPrimary
            0xFF764634, // primaryContainer
            0xFFFFDCD0, // onPrimaryContainer
            0xFF130D0A, // surface
            0xFFF9E0D8, // onSurface
            0xFF30231E, // surfaceVariant
            0xFFBCA69F, // onSurfaceVariant
            0xFF84716B, // outline
            0xFF55443F, // outlineVariant
            0xFF1A110E, // surfaceContainerLow
            0xFF221713, // surfaceContainer
            0xFF281D19, // surfaceContainerHigh
            0xFF30231E, // surfaceContainerHighest
            0xFF000000, // surfaceContainerLowest
            0xFFF97758, // error
    };

    private static volatile int[] sCached;
    private static boolean sCachedMonet = false;

    static boolean dark(Context ctx) {
        return (ctx.getResources().getConfiguration().uiMode
                & Configuration.UI_MODE_NIGHT_MASK) == Configuration.UI_MODE_NIGHT_YES;
    }

    static int[] current(Context ctx) {
        int[] s = fromSystem(ctx);
        if (s != null) {
            synchronized (M3.class) {
                if (!sCachedMonet) sCached = null;
                sCachedMonet = true;
            }
            return s;
        }
        synchronized (M3.class) {
            sCachedMonet = false;
            if (sCached == null) sCached = DARK_TERRACOTTA.clone();
            return sCached;
        }
    }

    /** 莫奈取色：Android 12+ 系统色阶直接映射成 M3 令牌。 */
    private static int[] fromSystem(Context ctx) {
        if (android.os.Build.VERSION.SDK_INT < 31) return null;
        int[] o = new int[COUNT];
        boolean dk = dark(ctx);
        int p = sys(ctx, dk ? "system_accent1_80" : "system_accent1_40");
        int pc = sys(ctx, dk ? "system_accent1_30" : "system_accent1_90");
        if (p == 0 || pc == 0) return null;
        o[PRIMARY] = p;
        o[ON_PRIMARY] = sys(ctx, dk ? "system_accent1_20" : "system_accent1_100");
        o[PRIMARY_CONTAINER] = pc;
        o[ON_PRIMARY_CONTAINER] = sys(ctx, dk ? "system_accent1_90" : "system_accent1_10");
        int n1 = dk ? 6 : 99, n2 = dk ? 90 : 10;
        o[SURFACE] = sys(ctx, "system_neutral1_" + n1);
        o[ON_SURFACE] = sys(ctx, "system_neutral1_" + (dk ? 90 : 10));
        o[SURFACE_VARIANT] = sys(ctx, "system_neutral2_" + (dk ? 30 : 50));
        o[ON_SURFACE_VARIANT] = sys(ctx, "system_neutral2_" + (dk ? 80 : 30));
        o[OUTLINE] = sys(ctx, "system_neutral2_" + (dk ? 60 : 50));
        o[OUTLINE_VARIANT] = sys(ctx, "system_neutral2_" + (dk ? 30 : 80));
        o[SURFACE_CONTAINER_LOW] = sys(ctx, "system_neutral1_" + (dk ? 12 : 94));
        o[SURFACE_CONTAINER] = sys(ctx, "system_neutral1_" + (dk ? 17 : 92));
        o[SURFACE_CONTAINER_HIGH] = sys(ctx, "system_neutral1_" + (dk ? 22 : 90));
        o[SURFACE_CONTAINER_HIGHEST] = sys(ctx, "system_neutral1_" + (dk ? 24 : 87));
        o[SURFACE_CONTAINER_LOWEST] = sys(ctx, "system_neutral1_" + (dk ? 4 : 100));
        int e = sys(ctx, dk ? "system_error_80" : "system_error_40");
        o[ERROR] = e != 0 ? e : 0xFFF97758;
        for (int c : o) if (c == 0) return null;
        return o;
    }

    private static int sys(Context ctx, String name) {
        int id = ctx.getResources().getIdentifier(name, "color", "android");
        if (id == 0) return 0;
        try {
            return ctx.getResources().getColor(id, null == null ? ctx.getTheme() : ctx.getTheme());
        } catch (Throwable t) {
            try {
                return ctx.getResources().getColor(id);
            } catch (Throwable t2) {
                return 0;
            }
        }
    }

    /** 在底色上混入一定比例的前景色（模拟 alpha 叠加，返回不透明色）。 */
    static int blend(int fg, int bg, float alpha) {
        float a = Math.max(0f, Math.min(1f, alpha));
        int r = Math.round(Color.red(fg) * a + Color.red(bg) * (1 - a));
        int g = Math.round(Color.green(fg) * a + Color.green(bg) * (1 - a));
        int b = Math.round(Color.blue(fg) * a + Color.blue(bg) * (1 - a));
        return Color.rgb(r, g, b);
    }
}
