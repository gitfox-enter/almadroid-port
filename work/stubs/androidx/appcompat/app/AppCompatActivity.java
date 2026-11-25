package androidx.appcompat.app;

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
