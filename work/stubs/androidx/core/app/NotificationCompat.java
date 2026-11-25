package androidx.core.app;

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
        public Builder setSilent(boolean b) { return this; }
        public Builder setColor(int col) { nb.setColor(col); return this; }
        public Builder addAction(int icon, CharSequence t, android.app.PendingIntent pi) {
            nb.addAction(new Notification.Action.Builder(Icon.createWithResource(c, icon), t, pi).build());
            return this;
        }
        public Notification build() { return nb.build(); }
    }
}
