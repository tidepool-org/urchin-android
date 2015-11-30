package io.tidepool.urchin.util;

import io.tidepool.urchin.BuildConfig;

/**
 * Created by Brian King on 11/30/15.
 */
public class Log {
    public static void v(String tag, String msg) {
        if (BuildConfig.DEBUG) {
            android.util.Log.v(tag, msg);
        }
    }

    public static void d(String tag, String msg) {
        if (BuildConfig.DEBUG) {
            android.util.Log.d(tag, msg);
        }
    }

    // Always log
    public static void e(String tag, String msg) {
        android.util.Log.e(tag, msg);
    }

    public static void i(String tag, String msg) {
        android.util.Log.i(tag, msg);
    }

    public static void w(String tag, String msg) {
        android.util.Log.w(tag, msg);
    }

}
