package com.util;

import android.util.Log;

/**
 * Created by Gmm on 17/7/19.
 * <p>
 * Describe:
 */
public class LogUtils {
    private static boolean DEBUG = true;

    private static final String TAG = "GMM";

    public static void e(String msg) {
        if (DEBUG)
            Log.e(TAG, msg);
    }
}
