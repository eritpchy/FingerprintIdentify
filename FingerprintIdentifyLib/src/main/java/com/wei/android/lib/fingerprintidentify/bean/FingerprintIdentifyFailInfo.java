package com.wei.android.lib.fingerprintidentify.bean;

public class FingerprintIdentifyFailInfo {
    public boolean deviceLocked;
    public int errMsgId;
    public String errString;

    public Throwable throwable;

    public FingerprintIdentifyFailInfo(boolean deviceLocked) {
        this.deviceLocked = deviceLocked;
    }

    public FingerprintIdentifyFailInfo(boolean deviceLocked, int errMsgId, String errString) {
        this.deviceLocked = deviceLocked;
        this.errMsgId = errMsgId;
        this.errString = errString;
    }

    public FingerprintIdentifyFailInfo(boolean deviceLocked, Throwable throwable) {
        this.deviceLocked = deviceLocked;
        this.throwable = throwable;
    }

    @Override
    public String toString() {
        return "FingerprintIdentifyFailInfo{" +
                "deviceLocked=" + deviceLocked +
                ", errMsgId=" + errMsgId +
                ", errString='" + errString + '\'' +
                ", throwable=" + throwable +
                '}';
    }
}
