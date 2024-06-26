package com.wei.android.lib.fingerprintidentify.impl;

import android.annotation.TargetApi;
import android.content.Context;
import android.hardware.biometrics.BiometricPrompt;
import android.hardware.fingerprint.FingerprintManager;
import android.os.Build;
import android.os.CancellationSignal;
import android.os.Handler;
import android.os.Looper;
import android.util.Log;

import androidx.annotation.NonNull;
import androidx.biometric.BiometricManager;

import com.wei.android.lib.fingerprintidentify.base.BaseFingerprint;
import com.wei.android.lib.fingerprintidentify.bean.FingerprintIdentifyFailInfo;
import com.wei.android.lib.fingerprintidentify.util.NotifyUtils;

import java.util.Locale;
import java.util.concurrent.Executor;

@TargetApi(Build.VERSION_CODES.P)
public class BiometricImpl extends BaseFingerprint {

    private static final String TAG = "BiometricImpl";
    private CancellationSignal mCancellationSignal;

    public BiometricImpl(Context context, ExceptionListener exceptionListener) {
        super(context, exceptionListener);

        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.P) {
            return;
        }

        try {
            BiometricManager biometricManager = BiometricManager.from(context);
            setHardwareEnable(false);
            int v = biometricManager.canAuthenticate(BiometricManager.Authenticators.BIOMETRIC_STRONG);
            switch (v) {
                case BiometricManager.BIOMETRIC_SUCCESS:
                    setHardwareEnable(true);
                    setRegisteredFingerprint(true);
                    break;
                case BiometricManager.BIOMETRIC_ERROR_NO_HARDWARE:
                    Log.e(TAG, "No biometric features available on this device.");
                    break;
                case BiometricManager.BIOMETRIC_ERROR_HW_UNAVAILABLE:
                    Log.e(TAG, "Biometric features are currently unavailable.");
                    break;
                case BiometricManager.BIOMETRIC_ERROR_NONE_ENROLLED:
                    Log.e(TAG, "The user hasn't associated " +
                            "any biometric credentials with their account.");
                    break;
                default:
                    Log.e(TAG, "Error Biometric canAuthenticate: " + v);
                    break;
            }
        } catch (Throwable e) {
            onCatchException(e);
        }
    }

    @Override
    protected void doIdentify() {
        BiometricPrompt.CryptoObject cryptoObject = createCryptoObject(BiometricPrompt.CryptoObject.class);
        if (cryptoObject == null) {
            Log.e(TAG, "Unable to auth with CryptoObject, retry authenticate.");
        }
        IAuthCallback callback = result -> {
            BiometricPrompt.CryptoObject crypto = result.getCryptoObject();
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
             * 先强制要求用户不用CryptoObject认证一遍先. 再走原先的认证逻辑
             */
            authenticate(null, result -> {
                NotifyUtils.notifyFingerprintTapped(mContext);
                BiometricPrompt.CryptoObject crypto = createCryptoObject(BiometricPrompt.CryptoObject.class);
                if (crypto == null) {
                    Log.e(TAG, "Unable to auth with CryptoObject, retry authenticate.");
                }
                authenticate(crypto, callback);
            });
        }


    }
    private void authenticate(BiometricPrompt.CryptoObject cryptoObject, IAuthCallback authCallback) {
        try {
            mCancellationSignal = new CancellationSignal();
            BiometricPrompt.Builder builder = new BiometricPrompt.Builder(this.mContext);
            builder.setTitle(" ");
            String cancelText = Locale.getDefault().getLanguage().toLowerCase().contains("zh") ? "取消" : "Cancel";
            builder.setNegativeButton(cancelText, new PromptExecutor(), (dialog, which) -> {
                onFailed(new FingerprintIdentifyFailInfo(false,
                        FingerprintManager.FINGERPRINT_ERROR_USER_CANCELED, "user cancel"));
            });

            builder.setConfirmationRequired(false);
            int authenticators = BiometricManager.Authenticators.BIOMETRIC_STRONG;
            builder.setAllowedAuthenticators(authenticators);
            builder.setDeviceCredentialAllowed(isDeviceCredentialAllowed(authenticators));
            BiometricPrompt prompt = builder.build();
            BiometricPrompt.AuthenticationCallback callback = new BiometricPrompt.AuthenticationCallback() {

                @Override
                public void onAuthenticationSucceeded(BiometricPrompt.AuthenticationResult result) {
                    super.onAuthenticationSucceeded(result);
                    authCallback.onAuthenticationSucceeded(result);
                }

                @Override
                public void onAuthenticationFailed() {
                    super.onAuthenticationFailed();
                    onNotMatch();
                }

                @Override
                public void onAuthenticationError(int errorCode, CharSequence errString) {
                    super.onAuthenticationError(errorCode, errString);
                    boolean deviceLocked = errorCode == FingerprintManager.FINGERPRINT_ERROR_LOCKOUT ||
                            errorCode == FingerprintManager.FINGERPRINT_ERROR_LOCKOUT_PERMANENT;
                    onFailed(new FingerprintIdentifyFailInfo(deviceLocked, errorCode, errString.toString()));
                }
            };
            if (cryptoObject != null) {
                prompt.authenticate(cryptoObject, this.mCancellationSignal, new PromptExecutor(), callback);
            } else {
                prompt.authenticate(this.mCancellationSignal, new PromptExecutor(), callback);
            }
        } catch (Throwable e) {
            onCatchException(e);
            onFailed(new FingerprintIdentifyFailInfo(false, e));
        }
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

    private static class PromptExecutor implements Executor {
        private final Handler mPromptHandler = new Handler(Looper.getMainLooper());

        @SuppressWarnings("WeakerAccess") /* synthetic access */
        PromptExecutor() {}

        @Override
        public void execute(@NonNull Runnable runnable) {
            mPromptHandler.post(runnable);
        }
    }

    static boolean isDeviceCredentialAllowed(int authenticators) {
        return (authenticators & BiometricManager.Authenticators.DEVICE_CREDENTIAL) != 0;
    }


    private interface IAuthCallback {
        void onAuthenticationSucceeded(BiometricPrompt.AuthenticationResult result);
    }

}