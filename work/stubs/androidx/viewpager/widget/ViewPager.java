package androidx.viewpager.widget;

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
