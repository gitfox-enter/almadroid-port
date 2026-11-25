package androidx.core.app;

import android.app.Activity;

public class ActivityCompat {
    public static void requestPermissions(Activity a, String[] p, int c) {
        if (android.os.Build.VERSION.SDK_INT >= 23) a.requestPermissions(p, c);
    }
}
