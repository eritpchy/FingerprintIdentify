package com.wei.android.lib.fingerprintidentify.bean;

import android.hardware.fingerprint.FingerprintManager;
import android.util.Log;

public class FingerprintIdentifyFailInfo {
    public boolean deviceLocked;
    public int errorCode;
    public String errString;

    public Throwable throwable;

    public FingerprintIdentifyFailInfo(boolean deviceLocked) {
        this.deviceLocked = deviceLocked;
    }

    public FingerprintIdentifyFailInfo(boolean deviceLocked, int errorCode, String errString) {
        this.deviceLocked = deviceLocked;
        this.errorCode = errorCode;
        this.errString = errString;
    }

    public FingerprintIdentifyFailInfo(boolean deviceLocked, Throwable throwable) {
        this.deviceLocked = deviceLocked;
        this.throwable = throwable;
    }

    public boolean isCancel() {
        return this.errorCode == FingerprintManager.FINGERPRINT_ERROR_CANCELED
            || this.errorCode == FingerprintManager.FINGERPRINT_ERROR_USER_CANCELED;
    }

    @Override
    public String toString() {
        Throwable throwable = this.throwable;
        Throwable cause = throwable == null ? null : throwable.getCause();
        if (cause != null) {
            throwable = cause;
        }
        return "FingerprintIdentifyFailInfo{" +
                "deviceLocked=" + deviceLocked +
                ", errorCode=" + errorCode +
                ", errString='" + errString + '\'' +
                ", throwable=" + throwable +
                ", cause=" + cause +
                ", throwable.cause=" + Log.getStackTraceString(throwable) +
                '}';
    }
}
