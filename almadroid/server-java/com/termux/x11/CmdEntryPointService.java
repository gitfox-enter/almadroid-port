package com.termux.x11;

import android.app.Service;
import android.content.Intent;
import android.os.IBinder;
import android.os.Looper;
import android.os.Process;
import android.system.Os;
import android.util.Log;

/** 替换 AAR 里的 CmdEntryPointService（原版 lambda 线程无 Looper，二次启动 NPE 崩进程）。
 *  行为对齐原版：读 extras → Os.setenv → 线程 { Looper.prepare(); CmdEntryPoint.main(args); } */
public class CmdEntryPointService extends Service {
    public static final String TAG = "CmdEntryPoint";
    public static final String ACTION_START = "com.termux.x11.action.START";
    public static final String ACTION_STOP = "com.termux.x11.action.STOP";
    public static final String EXTRA_ARGS = "args";
    public static final String EXTRA_ENV_KEYS = "env_keys";
    public static final String EXTRA_ENV_VALUES = "env_values";

    @Override
    public IBinder onBind(Intent intent) { return null; }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        if (intent == null) return START_NOT_STICKY;
        if (ACTION_STOP.equals(intent.getAction())) {
            Process.killProcess(Process.myPid());
            return START_NOT_STICKY;
        }
        String[] args = intent.getStringArrayExtra(EXTRA_ARGS);
        if (args == null) return START_NOT_STICKY;
        String[] keys = intent.getStringArrayExtra(EXTRA_ENV_KEYS);
        String[] vals = intent.getStringArrayExtra(EXTRA_ENV_VALUES);
        if (keys != null && vals != null) {
            for (int i = 0; i < keys.length && i < vals.length; i++) {
                try { Os.setenv(keys[i], vals[i], true); } catch (Throwable ignored) { }
            }
        }
        Thread t = new Thread(new Runnable() { public void run() {
            try {
                Looper.prepare();
                Class.forName("com.termux.x11.CmdEntryPoint")
                     .getMethod("main", String[].class)
                     .invoke(null, (Object) args);
            } catch (Throwable e) {
                Log.e(TAG, "X server main failed", e);
            }
        }}, "termux-x11-main");
        t.start();
        return START_NOT_STICKY;
    }
}
