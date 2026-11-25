package androidx.core.view;

import androidx.core.graphics.Insets;

public class WindowInsetsCompat {
    public static final WindowInsetsCompat CONSUMED = new WindowInsetsCompat();
    public WindowInsetsCompat() { }
    public Insets getInsets(int type) { return new Insets(0, 0, 0, 0); }
    public static class Type {
        public static int systemBars() { return 1; }
    }
}
