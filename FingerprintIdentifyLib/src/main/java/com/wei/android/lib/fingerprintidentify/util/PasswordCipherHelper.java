package com.wei.android.lib.fingerprintidentify.util;

import android.util.Log;

import java.io.UnsupportedEncodingException;

import javax.crypto.Cipher;
import javax.crypto.spec.SecretKeySpec;

public class PasswordCipherHelper {

    private static final String TAG = "PasswordCipherHelper";
    private static final String CIPHER_MODE = "AES/ECB/PKCS5Padding";

    public static Cipher createCipher(int cipherMode, String password) {
        try {
            SecretKeySpec key = createKey(password);
            Cipher cipher = Cipher.getInstance(CIPHER_MODE);
            cipher.init(cipherMode, key);
            return cipher;
        } catch (Exception e) {
            Log.e(TAG, "", e);
        }
        return null;
    }

    private static SecretKeySpec createKey(String password) {
        byte[] data = null;
        if (password == null) {
            password = "";
        }
        StringBuffer sb = new StringBuffer(32);
        sb.append(password);
        while (sb.length() < 32) {
            sb.append("0");
        }
        if (sb.length() > 32) {
            sb.setLength(32);
        }

        try {
            data = sb.toString().getBytes("UTF-8");
        } catch (UnsupportedEncodingException e) {
            e.printStackTrace();
        }
        return new SecretKeySpec(data, "AES");
    }
}
