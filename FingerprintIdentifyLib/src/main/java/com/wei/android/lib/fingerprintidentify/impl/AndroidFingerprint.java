package com.wei.android.lib.fingerprintidentify.impl;

import android.content.Context;
import android.hardware.fingerprint.FingerprintManager;
import android.os.Build;
import android.util.Log;

import androidx.core.os.CancellationSignal;

import com.wei.android.lib.fingerprintidentify.aosp.FingerprintManagerCompat;
import com.wei.android.lib.fingerprintidentify.base.BaseFingerprint;
import com.wei.android.lib.fingerprintidentify.bean.FingerprintIdentifyFailInfo;
import com.wei.android.lib.fingerprintidentify.util.NotifyUtils;

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
 * Created by Awei on 2017/2/9.
 */
public class AndroidFingerprint extends BaseFingerprint {

    private static final String TAG = "AndroidFingerprint";
    private CancellationSignal mCancellationSignal;
    private FingerprintManagerCompat mFingerprintManagerCompat;

    public AndroidFingerprint(Context context, ExceptionListener exceptionListener, boolean iSupportAndroidL) {
        super(context, exceptionListener);

        if (!iSupportAndroidL) {
            if (Build.VERSION.SDK_INT < Build.VERSION_CODES.M) {
                return;
            }
        }

        try {
            mFingerprintManagerCompat = FingerprintManagerCompat.from(mContext);
            setHardwareEnable(mFingerprintManagerCompat.isHardwareDetected());
            setRegisteredFingerprint(mFingerprintManagerCompat.hasEnrolledFingerprints());
        } catch (Throwable e) {
            onCatchException(e);
        }
    }

    @Override
    protected void doIdentify() {
        FingerprintManagerCompat.CryptoObject cryptoObject = createCryptoObject(FingerprintManagerCompat.CryptoObject.class);
        if (cryptoObject == null) {
            Log.e(TAG, "Unable to auth with CryptoObject, retry authenticate.");
        }
        IAuthCallback callback = result -> {
            FingerprintManagerCompat.CryptoObject crypto = result.getCryptoObject();
            if (crypto != null) {
                onSucceed(crypto.getCipher());
            } else {
                onSucceed(null);
            }
        };
        if (cryptoObject != null) {
            authenticate(cryptoObject, callback);
        } else {
            /**
             * android.security.keystore.UserNotAuthenticatedException: User not authenticated
             * 通常是用户使用了不够安全的方式解锁手机 或上一次验证时间已过30s + setUserAuthenticationValidityDurationSeconds(30)
             * 先强制要求用户不用CryptoObject认证一遍先. 在走原先的认证逻辑
             */
            authenticate(null, result -> {
                onNotMatch();
                NotifyUtils.notifyFingerprintTapped(mContext);
                FingerprintManagerCompat.CryptoObject crypto = createCryptoObject(FingerprintManagerCompat.CryptoObject.class);
                if (crypto == null) {
                    Log.e(TAG, "Unable to auth with CryptoObject, use fallback instead.");
                }
                authenticate(crypto, callback);
            });
        }
    }

    private void authenticate(FingerprintManagerCompat.CryptoObject cryptoObject, IAuthCallback callback) {
        try {
            mCancellationSignal = new CancellationSignal();
            mFingerprintManagerCompat.authenticate(cryptoObject, 0, mCancellationSignal, new FingerprintManagerCompat.AuthenticationCallback() {
                @Override
                public void onAuthenticationSucceeded(FingerprintManagerCompat.AuthenticationResult result) {
                    super.onAuthenticationSucceeded(result);
                    callback.onAuthenticationSucceeded(result);
                }

                @Override
                public void onAuthenticationFailed() {
                    super.onAuthenticationFailed();
                    onNotMatch();
                }

                @Override
                public void onAuthenticationError(int errMsgId, CharSequence errString) {
                    super.onAuthenticationError(errMsgId, errString);
                    boolean deviceLocked = errMsgId == FingerprintManager.FINGERPRINT_ERROR_LOCKOUT ||
                            errMsgId == FingerprintManager.FINGERPRINT_ERROR_LOCKOUT_PERMANENT;
                    onFailed(new FingerprintIdentifyFailInfo(deviceLocked, errMsgId, errString.toString()));
                }
            }, null);
        } catch (Throwable e) {
            onCatchException(e);
            onFailed(new FingerprintIdentifyFailInfo(false, e));
        }
    }

    private interface IAuthCallback {
        void onAuthenticationSucceeded(FingerprintManagerCompat.AuthenticationResult result);
    }

    @Override
    protected void doCancelIdentify() {
        try {
            if (mCancellationSignal != null) {
                mCancellationSignal.cancel();
            }
        } catch (Throwable e) {
            onCatchException(e);
        }
    }

    @Override
    protected boolean needToCallDoIdentifyAgainAfterNotMatch() {
        return false;
    }
}