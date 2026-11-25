package androidx.viewpager.widget;

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
