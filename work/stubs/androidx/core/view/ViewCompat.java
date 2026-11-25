package androidx.core.view;

import android.view.View;

public class ViewCompat {
    public interface OnApplyWindowInsetsListener {
        WindowInsetsCompat onApplyWindowInsets(View v, WindowInsetsCompat insets);
    }
    public static void setOnApplyWindowInsetsListener(View v, OnApplyWindowInsetsListener l) {
        // 挂起：不回调（桌面默认布局，不做系统栏避让）
    }
}
