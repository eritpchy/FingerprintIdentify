package com.wei.android.lib.fingerprintidentify;

import android.content.Context;

import com.wei.android.lib.fingerprintidentify.base.BaseFingerprint;
import com.wei.android.lib.fingerprintidentify.impl.AndroidFingerprint;
import com.wei.android.lib.fingerprintidentify.impl.BiometricImpl;
import com.wei.android.lib.fingerprintidentify.impl.MeiZuFingerprint;
import com.wei.android.lib.fingerprintidentify.impl.SamsungFingerprint;

import javax.crypto.Cipher;

/**
 * Copyright (c) 2017 Awei
 * <p>
 * Permission is hereby granted, free of charge, to any person obtaining a copy
 * of this software and associated documentation files (the "Software"), to deal
 * in the Software without restriction, including without limitation the rights
 * to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 * copies of the Software, and to permit persons to whom the Software is
 * furnished to do so, subject to the following conditions:
 * <p>
 * The above copyright notice and this permission notice shall be included in all
 * copies or substantial portions of the Software.
 * <p>
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE
 * SOFTWARE.
 * <p>
 * Created by Awei on 2017/2/8.
 */
public class FingerprintIdentify {

    protected Context mContext;
    protected BaseFingerprint.ExceptionListener mExceptionListener;

    protected boolean mIsSupportAndroidL = false;

    protected BaseFingerprint mFingerprint;
    protected BaseFingerprint mSubFingerprint;

    private int mMaxAvailableTimes = 5;

    private int mCipherMode = Cipher.ENCRYPT_MODE;

    private byte[] mCipherIV = null;

    private boolean mUseBiometricApi = false;

    public FingerprintIdentify(Context context) {
        mContext = context;
    }

    public void setMaxAvailableTimes(int v) {
        this.mMaxAvailableTimes = v;
    }

    public void setCipherMode(int cipherMode, byte[] cipherIV) {
        this.mCipherMode = cipherMode;
        this.mCipherIV = cipherIV;
    }

    public int getCipherMode() {
        return this.mCipherMode;
    }

    public void setUseBiometricApi(boolean on) {
        mUseBiometricApi = on;
    }

    public boolean isUsingBiometricApi() {
        return mFingerprint instanceof BiometricImpl;
    }

    public void setSupportAndroidL(boolean supportAndroidL) {
        mIsSupportAndroidL = supportAndroidL;
    }

    public void setExceptionListener(BaseFingerprint.ExceptionListener exceptionListener) {
        mExceptionListener = exceptionListener;
    }

    public void init() {

        if (mUseBiometricApi) {
            BiometricImpl biometricImpl = new BiometricImpl(mContext, mExceptionListener);
            if (biometricImpl.isHardwareEnable()) {
                mSubFingerprint = biometricImpl;
                if (biometricImpl.isRegisteredFingerprint()) {
                    mFingerprint = biometricImpl;
                    return;
                }
            }
        }

        AndroidFingerprint androidFingerprint = new AndroidFingerprint(mContext, mExceptionListener, mIsSupportAndroidL);
        if (androidFingerprint.isHardwareEnable()) {
            mSubFingerprint = androidFingerprint;
            if (androidFingerprint.isRegisteredFingerprint()) {
                mFingerprint = androidFingerprint;
                return;
            }
        }

        SamsungFingerprint samsungFingerprint = new SamsungFingerprint(mContext, mExceptionListener);
        if (samsungFingerprint.isHardwareEnable()) {
            mSubFingerprint = samsungFingerprint;
            if (samsungFingerprint.isRegisteredFingerprint()) {
                mFingerprint = samsungFingerprint;
                return;
            }
        }

        MeiZuFingerprint meiZuFingerprint = new MeiZuFingerprint(mContext, mExceptionListener);
        if (meiZuFingerprint.isHardwareEnable()) {
            mSubFingerprint = meiZuFingerprint;
            if (meiZuFingerprint.isRegisteredFingerprint()) {
                mFingerprint = meiZuFingerprint;
            }
        }
    }

    // DO
    public void startIdentify(BaseFingerprint.IdentifyListener listener) {
        if (!isFingerprintEnable()) {
            return;
        }

        mFingerprint.startIdentify(this.mMaxAvailableTimes,
                this.mCipherMode, this.mCipherIV, listener);
    }

    public void cancelIdentify() {
        if (mFingerprint != null) {
            mFingerprint.cancelIdentify();
        }
    }

    public void resumeIdentify() {
        if (!isFingerprintEnable()) {
            return;
        }

        mFingerprint.resumeIdentify();
    }

    // GET & SET
    public boolean isFingerprintEnable() {
        return mFingerprint != null && mFingerprint.isEnable();
    }

    public boolean isHardwareEnable() {
        return isFingerprintEnable() || (mSubFingerprint != null && mSubFingerprint.isHardwareEnable());
    }

    public boolean isRegisteredFingerprint() {
        return isFingerprintEnable() || (mSubFingerprint != null && mSubFingerprint.isRegisteredFingerprint());
    }
}
