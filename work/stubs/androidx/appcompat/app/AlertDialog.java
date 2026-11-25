package androidx.appcompat.app;

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
