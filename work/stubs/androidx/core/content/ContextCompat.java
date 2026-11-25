package androidx.core.content;

import android.content.Context;
import android.content.pm.PackageManager;

public class ContextCompat {
    public static int checkSelfPermission(Context c, String p) {
        if (android.os.Build.VERSION.SDK_INT >= 23) return c.checkSelfPermission(p);
        return PackageManager.PERMISSION_GRANTED;
    }
}
