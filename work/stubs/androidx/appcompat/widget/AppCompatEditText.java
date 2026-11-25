package androidx.appcompat.widget;

import android.content.Context;
import android.util.AttributeSet;
import android.view.inputmethod.EditorInfo;
import android.view.inputmethod.InputConnection;

public class AppCompatEditText extends android.widget.EditText {
    public AppCompatEditText(Context c) { super(c); }
    public AppCompatEditText(Context c, AttributeSet a) { super(c, a); }
    public InputConnection onCreateInputConnection(EditorInfo ei) { return super.onCreateInputConnection(ei); }
}
