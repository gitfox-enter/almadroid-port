# -*- coding: utf-8 -*-
"""生成 Termux:X11 所需的 androidx 最小桩类。"""
import os

BASE = r'<PROJECT_ROOT>\work\stubs'

FILES = {
    'androidx/appcompat/app/AppCompatActivity.java': '''package androidx.appcompat.app;

import android.app.Activity;
import android.content.res.Configuration;
import android.os.Bundle;
import android.view.KeyEvent;
import android.view.MenuItem;

public class AppCompatActivity extends Activity {
    protected void onCreate(Bundle b) { super.onCreate(b); }
    protected void onResume() { super.onResume(); }
    protected void onPause() { super.onPause(); }
    protected void onDestroy() { super.onDestroy(); }
    public boolean dispatchKeyEvent(KeyEvent e) { return super.dispatchKeyEvent(e); }
    public void onConfigurationChanged(Configuration c) { super.onConfigurationChanged(c); }
    public void onPictureInPictureModeChanged(boolean inPip, Configuration c) { }
    public void onWindowFocusChanged(boolean hasFocus) { super.onWindowFocusChanged(hasFocus); }
    public boolean onOptionsItemSelected(MenuItem mi) { return super.onOptionsItemSelected(mi); }
    public Object getSupportActionBar() { return null; }
}
''',
    'androidx/appcompat/app/ActionBar.java': '''package androidx.appcompat.app;

public class ActionBar {
    public void setDisplayHomeAsUpEnabled(boolean b) { }
    public void setHomeButtonEnabled(boolean b) { }
    public void setTitle(CharSequence t) { }
}
''',
    'androidx/appcompat/app/AlertDialog.java': '''package androidx.appcompat.app;

import android.content.Context;
import android.content.DialogInterface;

public class AlertDialog extends android.app.Dialog {
    public AlertDialog(Context c) { super(c); }
    public void show() { }
    public static class Builder {
        Context c;
        public Builder(Context c) { this.c = c; }
        public Builder setTitle(CharSequence t) { return this; }
        public Builder setTitle(int r) { return this; }
        public Builder setMessage(CharSequence t) { return this; }
        public Builder setMessage(int r) { return this; }
        public Builder setNegativeButton(CharSequence t, DialogInterface.OnClickListener l) { return this; }
        public Builder setNegativeButton(int r, DialogInterface.OnClickListener l) { return this; }
        public AlertDialog create() { return new AlertDialog(c); }
        public AlertDialog show() { return create(); }
    }
}
''',
    'androidx/appcompat/widget/AppCompatEditText.java': '''package androidx.appcompat.widget;

import android.content.Context;
import android.util.AttributeSet;
import android.view.inputmethod.EditorInfo;
import android.view.inputmethod.InputConnection;

public class AppCompatEditText extends android.widget.EditText {
    public AppCompatEditText(Context c) { super(c); }
    public AppCompatEditText(Context c, AttributeSet a) { super(c, a); }
    public InputConnection onCreateInputConnection(EditorInfo ei) { return super.onCreateInputConnection(ei); }
}
''',
    'androidx/core/app/ActivityCompat.java': '''package androidx.core.app;

import android.app.Activity;

public class ActivityCompat {
    public static void requestPermissions(Activity a, String[] p, int c) {
        if (android.os.Build.VERSION.SDK_INT >= 23) a.requestPermissions(p, c);
    }
}
''',
    'androidx/core/app/NotificationCompat.java': '''package androidx.core.app;

import android.app.Notification;
import android.content.Context;
import android.graphics.drawable.Icon;

public class NotificationCompat {
    public static class Builder {
        Notification.Builder nb;
        Context c;
        public Builder(Context c, String ch) { this.c = c; nb = new Notification.Builder(c, ch); }
        public Builder setContentTitle(CharSequence t) { nb.setContentTitle(t); return this; }
        public Builder setContentText(CharSequence t) { nb.setContentText(t); return this; }
        public Builder setContentIntent(android.app.PendingIntent pi) { nb.setContentIntent(pi); return this; }
        public Builder setSmallIcon(int r) { nb.setSmallIcon(r); return this; }
        public Builder setPriority(int p) { return this; }
        public Builder setOngoing(boolean b) { nb.setOngoing(b); return this; }
        public Builder setShowWhen(boolean b) { nb.setShowWhen(b); return this; }
        public Builder setSilent(boolean b) { nb.setSilent(b); return this; }
        public Builder setColor(int col) { nb.setColor(col); return this; }
        public Builder addAction(int icon, CharSequence t, android.app.PendingIntent pi) {
            nb.addAction(new Notification.Action.Builder(Icon.createWithResource(c, icon), t, pi).build());
            return this;
        }
        public Notification build() { return nb.build(); }
    }
}
''',
    'androidx/core/content/ContextCompat.java': '''package androidx.core.content;

import android.content.Context;
import android.content.pm.PackageManager;

public class ContextCompat {
    public static int checkSelfPermission(Context c, String p) {
        if (android.os.Build.VERSION.SDK_INT >= 23) return c.checkSelfPermission(p);
        return PackageManager.PERMISSION_GRANTED;
    }
}
''',
    'androidx/core/math/MathUtils.java': '''package androidx.core.math;

public class MathUtils {
    public static float clamp(float v, float min, float max) {
        return v < min ? min : (v > max ? max : v);
    }
    public static int clamp(int v, int min, int max) {
        return v < min ? min : (v > max ? max : v);
    }
}
''',
    'androidx/core/graphics/Insets.java': '''package androidx.core.graphics;

public class Insets {
    public final int left, top, right, bottom;
    public Insets(int l, int t, int r, int b) { left = l; top = t; right = r; bottom = b; }
}
''',
    'androidx/core/view/ViewCompat.java': '''package androidx.core.view;

import android.view.View;

public class ViewCompat {
    public interface OnApplyWindowInsetsListener {
        WindowInsetsCompat onApplyWindowInsets(View v, WindowInsetsCompat insets);
    }
    public static void setOnApplyWindowInsetsListener(View v, OnApplyWindowInsetsListener l) {
        // 挂起：不回调（桌面默认布局，不做系统栏避让）
    }
}
''',
    'androidx/core/view/WindowInsetsCompat.java': '''package androidx.core.view;

import androidx.core.graphics.Insets;

public class WindowInsetsCompat {
    public static final WindowInsetsCompat CONSUMED = new WindowInsetsCompat();
    public WindowInsetsCompat() { }
    public Insets getInsets(int type) { return new Insets(0, 0, 0, 0); }
    public static class Type {
        public static int systemBars() { return 1; }
    }
}
''',
    'androidx/viewpager/widget/PagerAdapter.java': '''package androidx.viewpager.widget;

import android.view.View;
import android.view.ViewGroup;

public class PagerAdapter {
    public int getCount() { return 0; }
    public Object instantiateItem(ViewGroup c, int p) { return null; }
    public void destroyItem(ViewGroup c, int p, Object o) { }
    public boolean isViewFromObject(View v, Object o) { return false; }
    public CharSequence getPageTitle(int p) { return null; }
    public void notifyDataSetChanged() { }
}
''',
    'androidx/viewpager/widget/ViewPager.java': '''package androidx.viewpager.widget;

import android.content.Context;
import android.util.AttributeSet;
import android.view.ViewGroup;

public class ViewPager extends ViewGroup {
    public interface OnPageChangeListener {
        void onPageSelected(int p);
        void onPageScrolled(int p, float o, int o2);
        void onPageScrollStateChanged(int s);
    }
    public static class SimpleOnPageChangeListener implements OnPageChangeListener {
        public void onPageSelected(int p) { }
        public void onPageScrolled(int p, float o, int o2) { }
        public void onPageScrollStateChanged(int s) { }
    }
    public ViewPager(Context c) { super(c); }
    public ViewPager(Context c, AttributeSet a) { super(c, a); }
    public void setAdapter(PagerAdapter a) { }
    public void addOnPageChangeListener(OnPageChangeListener l) { }
    public void clearOnPageChangeListeners() { }
    public void setCurrentItem(int i, boolean s) { }
    protected void onLayout(boolean ch, int l, int t, int r, int b) { }
}
''',
}

def main():
    n = 0
    for path, content in FILES.items():
        full = os.path.join(BASE, path)
        os.makedirs(os.path.dirname(full), exist_ok=True)
        with open(full, 'w', encoding='utf-8') as f:
            f.write(content)
        n += 1
    print('stub files written:', n)

if __name__ == '__main__':
    main()
