package com.wei.android.lib.fingerprintidentify.util;

import android.os.Build;
import android.security.keystore.KeyGenParameterSpec;
import android.security.keystore.KeyPermanentlyInvalidatedException;
import android.security.keystore.KeyProperties;
import android.util.Log;

import java.lang.reflect.Constructor;
import java.security.Key;
import java.security.KeyStore;

import javax.crypto.Cipher;
import javax.crypto.KeyGenerator;
import javax.crypto.spec.IvParameterSpec;

public class CryptoObjectHelper {

    private static final String TAG = "CryptoObjectHelper";

    // This can be key name you want. Should be unique for the app.
    static final String KEY_NAME = "com.wei.android.lib.fingerprintidentify";

    // We always use this keystore on Android.
    static final String KEYSTORE_NAME = "AndroidKeyStore";

    final KeyStore keystore;

    public CryptoObjectHelper() throws Exception {
        keystore = KeyStore.getInstance(KEYSTORE_NAME);
        keystore.load(null);
    }

    public <T> T createCryptoObject(Class<T> tClass, int opmode, byte[] iv) throws Exception {
        Cipher cipher = createCipher(opmode, iv, true);
        Constructor<T> tCon = tClass.getDeclaredConstructor(Cipher.class);
        return tCon.newInstance(cipher);
    }

    Cipher createCipher(int opmode, byte[] iv, boolean retry) throws Exception {
        Key key = getKey();
        Cipher cipher = Cipher.getInstance(
                KeyProperties.KEY_ALGORITHM_AES + "/"
                        + KeyProperties.BLOCK_MODE_CBC + "/"
                        + KeyProperties.ENCRYPTION_PADDING_PKCS7
        );
        try {
            if (opmode == Cipher.DECRYPT_MODE) {
                cipher.init(opmode, key, new IvParameterSpec(iv));
            } else {
                cipher.init(opmode, key);
            }
        } catch (KeyPermanentlyInvalidatedException e) {
            keystore.deleteEntry(KEY_NAME);
            if (retry) {
                createCipher(opmode, iv, false);
            } else {
                throw new Exception("Could not create the cipher for fingerprint authentication.", e);
            }
        }
        return cipher;
    }

    Key getKey() throws Exception {
        Key secretKey;
        if (!keystore.isKeyEntry(KEY_NAME)) {
            try {
                createKey(true);
            } catch (Exception e) {
                Log.e(TAG, "createKey", e);
                createKey(false);
            }
        }

        secretKey = keystore.getKey(KEY_NAME, null);
        return secretKey;
    }

    void createKey(boolean withValiditySeconds) throws Exception {
            KeyGenerator keyGen = KeyGenerator.getInstance(KeyProperties.KEY_ALGORITHM_AES, KEYSTORE_NAME);
            KeyGenParameterSpec.Builder builder = new KeyGenParameterSpec.Builder(KEY_NAME, KeyProperties.PURPOSE_ENCRYPT | KeyProperties.PURPOSE_DECRYPT)
                    .setBlockModes(KeyProperties.BLOCK_MODE_CBC)
                    .setEncryptionPaddings(KeyProperties.ENCRYPTION_PADDING_PKCS7)
                    .setRandomizedEncryptionRequired(false)
                    .setUserAuthenticationRequired(true);
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
                builder.setUserPresenceRequired(false);
                builder.setUserConfirmationRequired(false);
                builder.setIsStrongBoxBacked(false);
            }
//            builder.setInvalidatedByBiometricEnrollment(true);
            if (withValiditySeconds) {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
                    builder.setUserAuthenticationParameters(30, KeyProperties.AUTH_BIOMETRIC_STRONG);
                } else {
                    builder.setUserAuthenticationValidityDurationSeconds(30);
                }
            }
            KeyGenParameterSpec keyGenSpec = builder.build();
            keyGen.init(keyGenSpec);
            keyGen.generateKey();
    }
}