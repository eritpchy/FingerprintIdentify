package com.wei.android.lib.fingerprintidentify.util;

import android.content.Context;
import android.os.Vibrator;

public class NotifyUtils {
    public static void notifyFingerprintTapped(Context context) {
        Vibrator vibrator = (Vibrator) context.getSystemService(Context.VIBRATOR_SERVICE);
        vibrator.vibrate(100);
    }
}
