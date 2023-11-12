package com.wei.android.lib.fingerprintidentify.util;

import android.security.keystore.KeyGenParameterSpec;
import android.security.keystore.KeyPermanentlyInvalidatedException;
import android.security.keystore.KeyProperties;

import java.lang.reflect.Constructor;
import java.security.Key;
import java.security.KeyStore;

import javax.crypto.Cipher;
import javax.crypto.KeyGenerator;
import javax.crypto.spec.IvParameterSpec;

public class CryptoObjectHelper {
    // This can be key name you want. Should be unique for the app.
    static final String KEY_NAME = "com.wei.android.lib.fingerprintidentify";

    // We always use this keystore on Android.
    static final String KEYSTORE_NAME = "AndroidKeyStore";

    // Should be no need to change these values.
    static final String KEY_ALGORITHM = KeyProperties.KEY_ALGORITHM_AES;
    static final String BLOCK_MODE = KeyProperties.BLOCK_MODE_CBC;
    static final String ENCRYPTION_PADDING = KeyProperties.ENCRYPTION_PADDING_PKCS7;
    static final String TRANSFORMATION = KEY_ALGORITHM + "/" +
            BLOCK_MODE + "/" +
            ENCRYPTION_PADDING;
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
        Key key = GetKey();
        Cipher cipher = Cipher.getInstance(TRANSFORMATION);
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

    Key GetKey() throws Exception {
        Key secretKey;
        if (!keystore.isKeyEntry(KEY_NAME)) {
            CreateKey();
        }

        secretKey = keystore.getKey(KEY_NAME, null);
        return secretKey;
    }

    void CreateKey() throws Exception {
        KeyGenerator keyGen = KeyGenerator.getInstance(KEY_ALGORITHM, KEYSTORE_NAME);
        KeyGenParameterSpec keyGenSpec =
                new KeyGenParameterSpec.Builder(KEY_NAME, KeyProperties.PURPOSE_ENCRYPT | KeyProperties.PURPOSE_DECRYPT)
                        .setBlockModes(BLOCK_MODE)
                        .setEncryptionPaddings(ENCRYPTION_PADDING)
                        .setUserAuthenticationRequired(true)
                        .build();
        keyGen.init(keyGenSpec);
        keyGen.generateKey();
    }
}